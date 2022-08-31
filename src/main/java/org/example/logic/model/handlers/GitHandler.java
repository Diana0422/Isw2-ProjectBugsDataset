package org.example.logic.model.handlers;

import org.example.logic.model.exceptions.CommandException;
import org.example.logic.model.keyabstractions.Commit;
import org.example.logic.model.keyabstractions.JFile;
import org.example.logic.model.keyabstractions.Project;
import org.example.logic.model.keyabstractions.Release;
import org.example.logic.model.utils.Parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class GitHandler {
    private final Project project;

    public GitHandler(Project project) {
        this.project = project;
    }

    public Process runGitCommand(String [] cmdArgs, File workDir) throws IOException {
        ProcessBuilder build = new ProcessBuilder(cmdArgs);
        String command = Parser.getInstance().listToString(build.command());
        build.directory(workDir);
        try {
            return build.start();
        } catch (IOException e) {
            throw new IOException("Something occurred while running shell command:"+command, e);
        }
    }

    public BufferedReader getCommandOutput(String[] args) throws CommandException {
        BufferedReader input;
        try {
            File dir = new File(project.getProjDir());
            Process inputProcess = runGitCommand(args, dir);
            input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));
            return input;
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
        throw new CommandException("Command malformed");
    }

    /**
     * Uses git to retrieve commits for the project specified in class field @project
     * @return the list of the commits retrieved
     */
    public List<Commit> lookupForCommits() {
        String [] args = {"git", "log", "--full-history", "--date-order", "--reverse",
                "--pretty=format:\"%h<%as<MES=%s<AUTHOR=%an\"",
                "--numstat"};
        List<Commit> cs = new ArrayList<>();
        try {
            BufferedReader input = getCommandOutput(args);

            String[] lines = input.lines()
                    .collect(Collectors.joining(System.lineSeparator()))
                    .replace("\r", "")
                    .split("\n");
            List<String> linesList = Arrays.stream(lines)
                    .filter(s -> !s.equals(" "))
                    .toList();

            cs = linesList.parallelStream()
                    .filter(line -> line.contains("<"))
                    .map(line -> {
                        StringTokenizer st = new StringTokenizer(line, "<");
                        String sha = st.nextToken();
                        LocalDateTime date = Parser.getInstance().parseDateToLocalDateTime(st.nextToken());
                        String ticketRef = st.nextToken();
                        String author = st.nextToken();

                        /* create a new commit instance with the parameters retrieved */
                        Commit commit = new Commit(project, sha, ticketRef, date, author);
                        /* assign a version to the new commit */
                        Release commitRelease = Release.findVersionByDate(project.getVersions(), commit.getDate());
                        commit.setVersion(commitRelease);
                        return commit;
                    }).collect(Collectors.toList());

            /* Order commits retrieved by date */
            cs.sort(Comparator.comparing(Commit::getDate));
        }catch (CommandException e) {
            e.printStackTrace();
        }

        return cs;
    }

    /**
     * Search using git command "git show".This command takes the sha of a commit and lists all the files modified
     * (added, deleted, modified) by the specified commit.
     * @param commit the commit that modified the file
     * @return the list of unique files touched by the commit, also calculating file features
     * like additions and deletions
     */
    public List<JFile> lookupForFiles(Commit commit) {
        String [] args = {"git", "show", commit.getShaId(),
                "--pretty=format:\"%h<%as<MES=%s<AUTHOR=%an\"",
                "--numstat"};
        List<JFile> files = new ArrayList<>();
        try {
            BufferedReader input = getCommandOutput(args);
            String[] lines = input.lines()
                    .collect(Collectors.joining(System.lineSeparator()))
                    .replace("\r", "")
                    .split("\n");
            List<String> linesJavaList = Arrays.stream(lines).toList();

            /* renamed files */
            files = linesJavaList.stream()
                    .filter(line -> line.contains(".java") && !line.contains("<"))
                    .map(line -> {
                        StringTokenizer stringTokenizer = new StringTokenizer(line, "\t");
                        int addition = Integer.parseInt(stringTokenizer.nextToken());
                        int deletion = Integer.parseInt(stringTokenizer.nextToken());
                        String file = stringTokenizer.nextToken();
                        if (line.contains("=>")) {
                            /* this branch implements the case in which the file is renamed by the commit:
                               this case is handled adding a new file, that takes the characteristics of the
                               old file.
                             */
                            String filepath = Parser.getInstance().parseFilePathFromLine(file, false);
                            String filename = Parser.getInstance().parseFilenameFromFilepath(filepath);
                            String filepathOld = Parser.getInstance().parseFilePathFromLine(file, true);
                            String filenameOld = Parser.getInstance().parseFilenameFromFilepath(filepathOld);
                            // cerca il vecchio file, oppure creane uno nuovo
                            JFile oldFile = project.getFile(commit, filenameOld, filepathOld);
                            // cerca il nuovo file, oppure creane uno nuovo
                            JFile newFile = project.getFile(commit, filename, filepath);
                            // aggiungi la release del commit al file nuovo
                            newFile.addAndFillRelease(commit.getVersion());
                            // elimina la release del commit dal file vecchio
                            oldFile.getReleases().remove(commit.getVersion());
                            // aggiungi il commit alla lista delle revisioni del file
                            oldFile.addRevision(commit);
                            // aggiungi il commit alla lista delle revisioni del file
                            newFile.addRevision(commit);
                            // aggiungi il vecchio e il nuovo file nella hashmap contenente le rinominazioni dei file per release
                            project.addRenomination(commit.getVersion().getIndex(), oldFile, newFile);
                            // aggiorna le caratteristiche del file
                            updateFileFeatures(oldFile, commit, addition, deletion);
                            // aggiorna le caratteristiche del file
                            updateFileFeatures(newFile, commit, addition, deletion);
                            // aggiungi il nuovo file alla lista dei file committati
                            commit.addTouchedFile(newFile);
                            return newFile;
                        } else if (deletion != 0 && addition == 0) {
                            /* file is deleted */
                            String filepath = Parser.getInstance().parseFilePathFromLine(file, false);
                            String filename = Parser.getInstance().parseFilenameFromFilepath(file);
                            // cerca il file, altrimenti creane uno nuovo
                            JFile f = project.getFile(commit, filename, filepath);
                            // aggiungi il commit alla lista delle revisioni del file
                            f.addRevision(commit);
                            // aggiorna le caratteristiche del file
                            updateFileFeatures(f, commit, addition, deletion);
                            // aggiungi il nuovo file alla lista dei file committati
                            commit.addTouchedFile(f);
                            return f;
                        } else {
                            /* file is added or modified */
                            String filepath = Parser.getInstance().parseFilePathFromLine(file, false);
                            String filename = Parser.getInstance().parseFilenameFromFilepath(file);

                            // cerca il file, altrimenti creane uno nuovo
                            JFile f = project.getFile(commit, filename, filepath);
                            // aggiungi il commit alla lista delle revisioni del file
                            f.addRevision(commit);
                            //aggiungi la release del commit al file
                            f.addAndFillRelease(commit.getVersion());
                            // aggiorna le caratteristiche del file
                            updateFileFeatures(f, commit, addition, deletion);
                            commit.addTouchedFile(f);
                            return f;
                        }
                            }).toList();
        } catch (CommandException e) {
            e.printStackTrace();
        }
        return files;
    }

    private void updateFileFeatures(JFile file, Commit commit, int addition, int deletion) {
        int version = commit.getVersion().getIndex();
        int changes = addition + deletion;
        try {
            /* update file stats */
            file.updateAdditions(version, addition);
            file.updateDeletions(version, deletion);
            file.updateChanges(version, changes);
            file.updateContent(version, getFileContent(file.getRelPath(), commit));
            file.updateAge(version);
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
    }

    /***
     * Get the content of a java file in a specific commit.
     * @param filepath: the complete filepath of the file
     * @param commit: the commit to consider
     * @return String[] file content
     */
    public String[] getFileContent(String filepath, Commit commit) throws IOException {
        String sha = commit.getShaId();

        String [] args = {"git", "show", sha+":"+filepath};
        File dir = new File(project.getProjDir());
        Process inputProcess = runGitCommand(args, dir);
        BufferedReader input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));

        List<String> temp = new ArrayList<>();
        String line;
        try {
            while ((line = input.readLine()) != null && !line.contains("diff --git a/")) {
                temp.add(line);
            }
        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }

        // put elements in simple array
        String[] content = new String[temp.size()];
        for (int i=0; i<temp.size();i++) {
            content[i] = temp.get(i);
        }
        inputProcess.destroyForcibly();
        return content;
    }
}
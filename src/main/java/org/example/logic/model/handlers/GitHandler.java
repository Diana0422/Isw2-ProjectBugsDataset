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

/**
 * Class that interacts with Git
 */
public class GitHandler {

    private final Project project;

    public GitHandler(Project project) {
        this.project = project;
    }

    /**
     * Runs a git command
     * @param cmdArgs command arguments array
     * @param workDir git working directory
     * @return a Process instance
     * @throws IOException
     */
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

    /**
     * Gets the output of a Git command
     * @param args the command args
     * @return a BufferedReader to the output
     * @throws CommandException error during Git command
     */
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
     * Commits on side branches are excluded, because may affect "git show" output filenames
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

                            /* OLD FILE
                                - search old file from existing files
                                - remove the commit release from the list of releases of the file
                            */
                            JFile oldFile = project.getFile(commit, filenameOld, filepathOld);

                            /* NEW FILE
                                - create new file inheriting old file stats
                                - add commit release to the list of the releases in file
                                - add the commit to the list of revisions
                                - update file stats (additions, deletions...)
                                - add file to list of files touched by the commit
                             */
                            JFile newFile = new JFile(commit.getVersion(), filename, filepath, oldFile);
                            newFile.addAndFillRelease(commit.getVersion());
                            newFile.addRevision(commit);
                            updateFileFeatures(newFile, commit, addition, deletion);
                            commit.addTouchedFile(newFile);

                            // set renaming of the file
                            oldFile.setRenamed(commit.getVersion(), newFile);
                            project.removeFile(commit.getVersion().getIndex(), oldFile);
                            return newFile;
                        }

                        if (deletion != 0 && addition == 0) {
                            /* file is candidate to be deleted */
                            String filepath = Parser.getInstance().parseFilePathFromLine(file, false);
                            String filename = Parser.getInstance().parseFilenameFromFilepath(file);

                            if (checkGitIfDeleted(commit.getShaId(), filepath)) {
                                JFile f = project.getFile(commit, filename, filepath);
                                // aggiungi il commit alla lista delle revisioni del file
                                f.addRevision(commit);
                                // aggiorna le caratteristiche del file
                                updateFileFeatures(f, commit, addition, deletion);
                                // aggiungi il nuovo file alla lista dei file committati
                                commit.addTouchedFile(f);
                                // set deletion release of file
                                f.setDeleted(commit.getVersion());
                                project.removeFile(commit.getVersion().getIndex(), f);
                                return f;
                            }
                        }
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
                            }).toList();
        } catch (CommandException e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     * Checks if a file is really deleated with a certain commit
     * @param shaId the commit id
     * @param filepath the complete filepath
     * @return a boolean
     */
    private boolean checkGitIfDeleted(String shaId, String filepath) {
        String [] args = {"git", "show", "--name-status", "--diff-filter=D", shaId};
        try {
            BufferedReader commandOutput = getCommandOutput(args);
            List<String> strings = commandOutput.lines().filter(s -> s.contains(filepath)).toList();
            return !strings.isEmpty();
        } catch (CommandException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Updates the features collected for a certain commit
     * @param file the file to modify
     * @param commit the commit that updated the file
     * @param addition number of LOC added with commit
     * @param deletion number of LOC deleted with commit
     */
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
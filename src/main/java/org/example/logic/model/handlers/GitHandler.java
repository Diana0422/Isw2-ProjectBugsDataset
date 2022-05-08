package org.example.logic.model.handlers;

import org.example.logic.control.InspectionController;
import org.example.logic.model.features.FeatureCalculator;
import org.example.logic.model.keyabstractions.Commit;
import org.example.logic.model.keyabstractions.JFile;
import org.example.logic.model.keyabstractions.Project;
import org.example.logic.model.keyabstractions.Record;
import org.example.logic.model.keyabstractions.Release;
import org.example.logic.model.utils.Parser;

import javax.sound.sampled.Line;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;


public class GitHandler {

    private Project project;

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

    public void lookupForCommits() throws IOException {
        /***
         * Use Git commands to retrieve all project commits from the first release to the last measurable one
         * git command: git log --all --pretty=format: "%h>%as>MES=%s>AUTHOR=%an"
         * --all : specifies that all the commits from all the branches must be retrieved
         * --pretty=format : formats the output to the form specified
         */
        String [] args = {"git", "log", "--full-history", "--date-order", "--reverse", "--name-status","--pretty=format:\"%h>%as>MES=%s>AUTHOR=%an\"", "--stat", "HEAD"};
        File dir = new File(project.getProjDir());
        Process inputProcess = runGitCommand(args, dir);
        BufferedReader input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));
        List<String> tokenizedLine = new ArrayList<>();
        String sha;
        String author;
        LocalDateTime date;
        String ticketRef;

        /* test */
        List<Commit> zookeeper = new ArrayList<>();

        String line = "";
        boolean reset = false;
        while (line != null) {
            /* iterazione su tutti i commit */
            if (!reset) line = input.readLine();
            StringTokenizer st = new StringTokenizer(line, ">");

            /* informazioni del commit */
            sha = st.nextToken();
            date = Parser.getInstance().parseDateToLocalDateTime(st.nextToken());
            ticketRef = st.nextToken();
            author = st.nextToken();
            Commit commit = new Commit(sha, ticketRef, date, author);
            Release commitRelease = Release.findVersionByDate(project.getVersions(), commit.getDate());
            commit.setVersion(commitRelease);
            /* test */
            if (ticketRef.contains("ZOOKEEPER")) zookeeper.add(commit);

            /* iterazione su tutti i file modificati e toccati dal commit */
            List<JFile> touchedFiles = new ArrayList<>();
            while (!line.equals("") && (line = input.readLine())!= null) {
                if (line.contains(">")) {
                    reset = true;
                    break;
                } else {
                    reset = false;
                }
                if (line.contains(".java")) {
                    if (line.startsWith("A") || line.startsWith("M")) {
                        String filepath = Parser.getInstance().parseFilepathFromLogLine(line, false);
                        String filename = Parser.getInstance().parseFilenameFromLogLine(line, false);
                        // cerca il file, altrimenti creane uno nuovo
                        JFile file = JFile.getSpecificJFile(project.getJavaFiles(), filepath);
                        if (file == null) file = new JFile(project, filename, filepath);
                        //aggiungi la release del commit al file
                        file.addRelease(commitRelease);
                        // aggiungi il file alla lista dei file committati
                        touchedFiles.add(file);
                        // aggiungi il commit alla lista delle revisioni del file
                        file.addRevision(commit);
                        // aggiungi il file alla lista dei file del progetto
                        project.addJavaFile(file);
                        // aggiungi il commit alla lista dei commit added o modified del file vecchio
                        if (line.startsWith("A")) file.addAddedCommits(commit);
                        if (line.startsWith("M")) file.addModifiedCommits(commit);
                    } else if (line.startsWith("D")) {
                        String filepath = Parser.getInstance().parseFilepathFromLogLine(line, false);
                        String filename = Parser.getInstance().parseFilenameFromLogLine(line, false);
                        // cerca il file, altrimenti creane uno nuovo
                        JFile file = JFile.getSpecificJFile(project.getJavaFiles(), filepath);
                        if (file == null) file = new JFile(project, filename, filepath);
                        // aggiungi il file alla lista dei file committati
                        touchedFiles.add(file);
                        // aggiungi il commit alla lista delle revisioni del file
                        file.addRevision(commit);
                        // aggiungi il file alla lista dei file del progetto
                        project.addJavaFile(file);
                        // aggiungi il commit alla lista dei commit delete del file vecchio
                        file.addDeletedCommits(commit);
                    } else if (line.startsWith("R")) {
                        String filename = Parser.getInstance().parseFilenameFromLogLine(line, true);
                        String filepath = Parser.getInstance().parseFilepathFromLogLine(line, true);
                        String filenameOld = Parser.getInstance().parseFilenameFromLogLine(line, false);
                        String filepathOld = Parser.getInstance().parseFilepathFromLogLine(line, false);
                        // cerca il vecchio file, oppure creane uno nuovo
                        JFile oldFile = JFile.getSpecificJFile(project.getJavaFiles(), filepathOld);
                        if (oldFile == null) oldFile = new JFile(project, filenameOld, filepathOld);
                        // cerca il nuovo file, oppure creane uno nuovo
                        JFile newFile = JFile.getSpecificJFile(project.getJavaFiles(), filepath);
                        if (newFile == null) newFile = new JFile(project, filename, filepath);
                        // aggiungi la release del commit al file nuovo
                        newFile.addRelease(commitRelease);
                        // elimina la release del commit dal file vecchio
                        oldFile.getReleases().remove(commitRelease);
                        // aggiungi il nuovo file alla lista dei file committati
                        touchedFiles.add(newFile);
                        // aggiungi il commit alla lista delle revisioni del file
                        oldFile.addRevision(commit);
                        // aggiungi il commit alla lista delle revisioni del file
                        newFile.addRevision(commit);
                        // aggiungi nuovo file alla lista dei files del progetto
                        project.addJavaFile(newFile);
                        // aggiungi vecchio file alla lista dei files del progetto
                        project.addJavaFile(oldFile);
                        // aggiungi il vecchio e il nuovo file nella hashmap contenente le rinominazioni dei file per release
                        project.addRenomination(commitRelease.getIndex(), oldFile, newFile);
                        // aggiungi il commit alla lista dei commit rename del file vecchio
                        oldFile.addRenamedCommits(commit);

                    }
                }
            }
            /* set files touched by the commit */
            commit.setCommittedFiles(touchedFiles);

            if (ticketRef.contains(this.project.getProjName()) || ticketRef.contains("ISSUE") || ticketRef.contains("#")) {
                project.getRefCommits().add(commit);
            }

            /* add commit to the list of project commits */
            project.getCommits().add(commit);
            System.out.println("Commit :"+commit.getShaId()+"-"+commit.getMessage()+"-"+commit.getDate());

            tokenizedLine.clear();

        }
        /* sort the commits by date */
        project.getCommits().sort(Comparator.comparing(Commit::getDate));
        project.getRefCommits().sort(Comparator.comparing(Commit::getDate));
        inputProcess.destroyForcibly();
    }

    public String[] getFileContent(String filepath, Commit commit) throws IOException {
        /***
         * Get the content of a java file in a specific commit.
         * @param filepath: the complete filepath of the file
         * @param commit: the commit to consider
         * @return String[] file content
         */
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
            // TODO Auto-generated catch block
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

    public void updateCommitLinesChanges(Commit commit, HashMap<String, Record> records, BufferedReader input) throws IOException {
        if (input == null) {
            String[] args = {"git", "show", commit.getShaId(), "--numstat"};
            File dir = new File(project.getProjDir());
            Process inputProcess = runGitCommand(args, dir);
            input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));
        }
        int addition;
        int deletion;
        Record record;
        String filepath;
        String recordKey;
        String line;
        char firstChar = ' ';
        while ((line = input.readLine()) != null) {
            if (line.length() != 0) firstChar = line.charAt(0);
            if (line.contains("git-svn-id") || Character.isDigit(firstChar)) break;
        }

        while ((line = input.readLine()) != null) {
            if (line.contains(".java")) {
                int releaseIdx = commit.getVersion().getIndex();
                StringTokenizer tokenizer = new StringTokenizer(line, "\t");
                addition = Integer.parseInt(tokenizer.nextToken());
                deletion = Integer.parseInt(tokenizer.nextToken());
                filepath = tokenizer.nextToken();
                if (filepath.contains("{")) {
                    // il file è stato rinominato e dunque è necessario fare il parsing del nome attuale del file
                    filepath = Parser.getInstance().parseFilepathFromChangesLine(filepath);
                }
                JFile file = JFile.getSpecificJFile(project.getJavaFiles(), filepath);
                if (project.checkRenomination(releaseIdx, file)) {
                    // preleva dall'hashmap la nuova istanza del file
                    JFile newFile = project.getRenomination(releaseIdx, file);
                    // recupera il contenuto del file usando il nome della nuova istanza del file
                    recordKey = newFile.getRelPath();
                } else {
                    recordKey = filepath;
                }
                if (records.get(recordKey) != null) {
                    record = records.get(recordKey);
                    FeatureCalculator.getInstance().updateAdditions(addition, record);
                    FeatureCalculator.getInstance().updateDeletions(deletion, record);
                } else {
                    /* TEST */
                    System.out.println(filepath);
                }
            }
        }
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getRenamingFilepath(Commit commit, String originalFilepath) throws IOException {
        String[] args = {"git", "show", commit.getShaId()};
        File dir = new File(project.getProjDir());
        Process inputProcess = runGitCommand(args, dir);
        BufferedReader input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));
        String line;
        String newFilepath;

        while ((line = input.readLine()) != null) {
            if (line.equals("--- a/"+originalFilepath)){
                break;
            }
        }

        line = input.readLine();
        newFilepath = JFile.getFilepathFromLine(line);
        return newFilepath;
    }
}
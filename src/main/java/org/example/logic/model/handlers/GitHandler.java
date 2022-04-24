package org.example.logic.model.handlers;

import org.example.logic.control.InspectionController;
import org.example.logic.model.features.FeatureCalculator;
import org.example.logic.model.keyabstractions.Commit;
import org.example.logic.model.keyabstractions.JFile;
import org.example.logic.model.keyabstractions.Project;
import org.example.logic.model.utils.Parser;

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
         */
        //TODO implement with JGit
        String [] args = {"git", "log", "--all", "--pretty=format:\"%h>%as>MES=%s>AUTHOR=%an\""}; //command to call
        File dir = new File(this.project.getProjDir());
        Process inputProcess = runGitCommand(args, dir);
        BufferedReader input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));
        List<String> tokenizedLine = new ArrayList<>();

        String line = "";
        String sha = "";
        String author = "";
        LocalDateTime date = null;
        String ticketRef = "";

        while ((line = input.readLine()) != null) {
            if (InspectionController.verbose) System.out.println("Lookup4Commits:"+line);
            StringTokenizer st = new StringTokenizer(line, ">");

            sha = st.nextToken();
            date = Parser.getInstance().parseDateToLocalDateTime(st.nextToken());
            ticketRef = st.nextToken();
            author = st.nextToken();

            /* create a new commit object to add to the commit list of the project */
            Commit commit = new Commit(sha, ticketRef, date, author);

            /* add commit to the list of project commits */
            project.getCommits().add(commit);

            /* if the commit references an issue, add it to the list of the commits referencing a ticket */
            if (ticketRef.contains(this.project.getProjName()) || ticketRef.contains("ISSUE") || ticketRef.contains("#")) {
                this.project.getRefCommits().add(commit);
            }
            lookupTouchedFiles(commit);

            tokenizedLine.clear();
        }

        /* sort the commits by date */
        Collections.sort(this.project.getCommits(), Comparator.comparing(Commit::getDate));
        inputProcess.destroyForcibly();
    }

    private void lookupTouchedFiles(Commit commit) throws IOException {
        String line = null;
        String filename = "";
        String filepath = "";
        JFile touched = null;
        int additions = 0;
        int deletions = 0;
        int changes = 0;
        int size = 0;
        String[] content = null;

        String [] args = {"git", "show", commit.getShaId()};
        File dir = new File(this.project.getProjDir());
        Process inputProcess = runGitCommand(args, dir);
        BufferedReader input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));

        while ((line = input.readLine()) != null) {
            if (line.contains("+++ b/")){

                filepath = JFile.getFilepathFromLine(line);
                filename = JFile.getJFileNameFromLine(line);
                if (InspectionController.verbose) System.out.println(filename);
                touched = JFile.getSpecificFile(commit.getCommittedFiles(), filename);

                if (touched != null) {
                    String [] lines = readLines(input, false);
                    content = getFileContent(filepath, commit);
                    if (InspectionController.verbose) System.out.println(content);
                    additions = FeatureCalculator.getInstance().getAdditions(lines);
                    deletions = FeatureCalculator.getInstance().getDeletions(lines);
                    changes = additions + deletions;
                    size = FeatureCalculator.getInstance().getLOC(content);

                    touched.setSize(size);
                    touched.setAdditions(additions);
                    touched.setDeletions(deletions);
                    touched.setChanges(changes);
                    touched.setRelPath(filepath);
                } else if (touched==null && filename.contains(".java")){
                    touched = new JFile(this.project, filename, filepath);
                    touched.setRelPath(filepath);
                }

                //if (InspectionController.verbose) System.out.println(touched);
                if (touched!=null) commit.getCommittedFiles().add(touched);
            }
        }

        inputProcess.destroyForcibly();
    }

    public void lookupForReleases(JFile jfile) throws IOException {
        /***
         * looks for the release in which the file is present (a java file is present in a release if there is a commit that modifies
         * the file in that release.
         * param jfile: the file java to locate
         * */
        //TODO modify with JGit
        String [] args = {"git", "blame", jfile.getName()};
        File dir = new File(jfile.getDirPath());
        Process inputProcess = runGitCommand(args, dir);
        BufferedReader input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));
        List<String> tokenizedLine = new ArrayList<>();
        String line = null;

        if (InspectionController.verbose) System.out.println("\n\n\n\n\nLooking for the releases in which file "+jfile.getName()+" is present.");

        try {
            while ((line = input.readLine()) != null) {
                if (InspectionController.verbose) System.out.println(line);
                StringTokenizer st = new StringTokenizer(line);
                while(st.hasMoreTokens()) {
                    tokenizedLine.add(st.nextToken());
                }
                for (String s: tokenizedLine) {
                    if (s.startsWith("20") && s.contains("-")) {
                        LocalDateTime commitDate = Parser.getInstance().parseDateToLocalDateTime(s);
                        /* get the date from the git blame to search for the corresponding release */
                        jfile.locateInRelease(commitDate);
                        break;
                    }
                }
            }
            tokenizedLine.clear();
            inputProcess.destroyForcibly();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String[] getFileContent(String filepath, Commit commit) throws IOException {
        String sha = commit.getShaId();

        String [] args = {"git", "show", sha+":"+filepath};
        File dir = new File(this.project.getProjDir());
        Process inputProcess = runGitCommand(args, dir);
        BufferedReader input = new BufferedReader(new InputStreamReader(inputProcess.getInputStream()));


        String[] content = readLines(input, true);
        inputProcess.destroyForcibly();
        return content;
    }


    private String [] readLines(BufferedReader input, Boolean full) {
        List<String> temp = new ArrayList<>();
        String line = null;

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
        return content;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
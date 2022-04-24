package org.example.logic.model.utils;

import org.example.logic.control.InspectionController;
import org.example.logic.model.handlers.GitHandler;
import org.example.logic.model.handlers.JIRAHandler;
import org.example.logic.model.keyabstractions.*;
import org.example.logic.model.keyabstractions.Record;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

public class ProjectInspector {

    private Project project;
    private GitHandler git;
    private JIRAHandler jira;
    private List<Record> datasetRecords;

    public ProjectInspector(Project project) {
        this.setProject(project);
        this.git = new GitHandler(project);
        this.jira = new JIRAHandler(project);
        this.datasetRecords = new ArrayList<>();
    }

    private void setDatasetRecords(JFile file) {
        /***
         * set the dataset records with release index and file name, based on the releases in which the file is present
         * param file: the file that identifies a row of the dataset, together with the releases in which the file is present
         */
        for (Release r: file.getReleases()) {
            Record rec = new Record(r.getIndex(), file.getName());
            this.datasetRecords.add(rec);
        }
    }

    public List<JFile> inspectProjectFiles() {
        /***
         * retrieve all the files from the repository selected in the config file
         */
        List<JFile> javaFiles = new ArrayList<>();

        /* inspects the selected project reading all java files. */
        File folder = new File(this.project.getProjDir());
        List<File> files = Arrays.asList(Objects.requireNonNull(folder.listFiles()));
        List<File> listFiles = new ArrayList<>(files);

        while(!listFiles.isEmpty()) {
            File file = listFiles.get(0);
            if (file.isDirectory()) {
                listFiles.addAll(Arrays.asList(Objects.requireNonNull(file.listFiles())));
                listFiles.remove(file);

                /* select only java files (with .java extension) and exclude Test files */
            } else if (file.getName().contains(".java") && !file.getName().contains("Test")) {
                JFile jfile = new JFile(project, file.getName(), file.getParent());
                try {
                    /* search for the releases in which the file is present */
                    git.lookupForReleases(jfile);
                } catch (IOException e) {
                    //TODO handle exception
                    e.printStackTrace();
                }
                setDatasetRecords(jfile);
                javaFiles.add(jfile);
                listFiles.remove(file);

                if (InspectionController.verbose) {
                    System.out.println("Filename: "+jfile.getName());
                    System.out.println("Relative Path: "+jfile.getRelPath());
                }
            } else {
                listFiles.remove(file);
            }
        }
        project.setJavaFiles(javaFiles);
        return javaFiles;
    }

    public List<Commit> inspectProjectCommits() {
        /***
         * retrieve all the commits of the project selected in the config file
         */
        List<Commit> commits = null;
        try {
            git.lookupForCommits();
            commits = project.getCommits();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return commits;
    }

    public List<Issue> inspectProjectIssues() {
        /***
         * Retrieve project issues (bug type) from Jira
         */
        List<Issue> bugs = null;
        try {
            bugs = jira.retrieveProjectIssues("Bug");
            project.setBugIssues(bugs);
            for (Issue i: bugs) {
                Issue.prepareIssue(i);
                /* retrieve the fixed commits of the issue */
                i.setFixedCommits(findLinkedToIssueCommits(project, i.getKey(), i.getNumber()));
            }

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return bugs;
    }

    private List<Commit> findLinkedToIssueCommits(Project project, String issueKey, String issueNum) {
        /***
         * filter the commits of the project to those which have the issue key referenced in the commit message
         * param project: the project to inspect
         * param issueKey: the key of the issue to filter the commits
         */
        List<Commit> fixedCommits = new ArrayList<>();

        for (Commit c: project.getCommits()) {
            if (c.getMessage().contains(issueKey+":") || c.getMessage().contains(issueKey+"]") || c.getMessage().contains(issueKey+" ")){
                fixedCommits.add(c);
            }
        }
        return fixedCommits;
    }
/*
    private double calculateP(Issue issue, int openV, int fixedV) {
        List<Issue> window = new ArrayList<>();
        List<Issue> prevBugs = new ArrayList<>();
        List<Issue> bugs = this.project.getBugIssues();

        for (Issue bug: bugs) {
            if (bug.getId().equals(issue.getId())) break;
            prevBugs.add(bug);
        }

        // get last 1% of the fixed bugs
        int total = prevBugs.size();
        int percentage = (int) Math.ceil(total*0.01);

        int start = total - percentage;
        int end = total;

        // put the last 1% of the fixed bugs in the window
        for (int i=start; i<end; i++) {
            window.add(prevBugs.get(i));
        }

        // calculate p as the average of P in last 1% issues fixed
        double avg;
        double sum = 0;

        for (int i=0; i<window.size(); i++) {
            window.get(i).calculateProportion(openV, fixedV);
            sum += window.get(i).getProportion();
        }

        avg = sum/window.size();
//		System.out.println("Proportion for window: "+window+" ="+avg);
        return avg;
    }


    private void applyProportion(Issue issue) {
        int fixedV;
        int openV;
        int injV;
        double p;

        openV = issue.getOpVersion().getIndex();
//    	System.out.println("opening ver: "+openV);

        //If the fixed version isn't found it's set equal to the opening version
        if(issue.getFixedVersions().isEmpty()) {
            fixedV = openV;
        } else {
            System.out.println(issue.getFixedVersions());
            fixedV = issue.getFixedVersions().get(0).getIndex();
        }

//    	System.out.println("fixed ver: "+fixedV);

        //If the opening version is greater the the fixed version they are inverted
        if(openV > fixedV) {
            int temp = fixedV;
            fixedV = openV;
            openV = temp;
        }

        p = calculateP(issue, openV, fixedV);
        injV = (int) (fixedV -(fixedV - openV)*p);

//    	System.out.println("injected ver: "+injV);

        //Checks if the value is negative
        if(injV <= 0) {
            injV = 1;
        }

        //Checks if the value is greater than the opening version
        if(injV > openV) {
            injV = openV;
        }

        //Adds all the new affected versions to the ticket
        for(int i=injV; i<=fixedV; i++) {
            Release ver = this.project.getVersions().get(i-1);
            issue.addAffected(ver);
        }

        issue.setFixVersion(Release.findVersionByIndex(this.project.getVersions(), fixedV));
        issue.setInjVersion(Release.findVersionByIndex(this.project.getVersions(), injV));

        // update the issue with corresponding p
        issue.setProportion(p);

        System.out.println("calculated affected versions: ");
        for (Release rel: issue.getAffectedVersions()) {
            System.out.println(rel.getIndex()+" - "+rel.getName());
        }
    } */

    public List<Record> produceDataset() {
        /***
         * produce dataset using project information about issues and commits that fix the issues.
         * The sample unit in the data is of type Record.
         */
        List<Record> records = new ArrayList<>();

        List<Release> releases = project.getViewedVersions(); // consider only the selected percentage of the versions to create the dataset
        int releaseNum = releases.size(); // we consider only a portion of the releases

        List<Commit> commits = new ArrayList<>();
        for (Commit c: project.getCommits()) {
            commits.add(c);
        }

        List<HashMap<String, Record>> hashMaps = new ArrayList<>();
        List<JFile> committedFiles = null;
        Commit commit;

        for (int i=0; i<releaseNum; i++) {
            hashMaps.add(new HashMap<>()); // create new hash map for the release to store records
            LocalDateTime limitDate = releases.get(i).getDate();

            while (!commits.isEmpty() && commits.get(0).getDate().compareTo(limitDate) < 0) {
                commit = commits.get(0);	//takes the first commit
                committedFiles = commit.getCommittedFiles();

                updateRecords(commit, committedFiles, hashMaps, records, i+1);

                commits.remove(commit);
            }

        }
        updateBugginess(hashMaps, records);
        return records;
    }

    private void updateRecords(Commit commit, List<JFile> committedFiles, List<HashMap<String, org.example.logic.model.keyabstractions.Record>> hashMaps,
                               List<Record> records, int release) {

        org.example.logic.model.keyabstractions.Record r = null;
        int idx = release-1;
        for (int i=0; i<committedFiles.size(); i++) {
            JFile file = committedFiles.get(i);
            String filename = file.getRelPath()+file.getName();

            r = hashMaps.get(idx).get(filename);

            //if it's a new file in the release add it to the hash map and to the list
            if (r == null) {
                r = new Record(release, filename);
                hashMaps.get(idx).put(filename, r);
                records.add(r);
            }

            //set record info (dataset features)
            r.setSize(file.getSize());
            r.addLocTouched(file.getChanges());
            r.addLocAdded(file.getAdditions());
            r.addRevision();
            r.addChgSetSize(committedFiles.size()-1);
            r.addAuthor(commit.getAuthor());
        }

    }

    public void updateBugginess(List<HashMap<String, Record>> hashmaps, List<Record> records) {
        /***
         * Update the bugginess status of each file of the project.
         */
        List<Issue> bugs = project.getBugIssues();
        Release fixedVer = null;
        Commit fixedCommit = null;
        List<Release> affected; //added
        List<Release> fixed;

        for (Issue bug: bugs) {
            if (!bug.getFixedCommits().isEmpty()) {

                fixedCommit = bug.getFixedCommits().get(0);
                fixedVer = bug.getFixVersion();
                affected = bug.getAffectedVersions();
                fixed = bug.getFixedVersions();

                if (fixedVer == null) fixedVer = fixedCommit.getVersion();
                if (fixedVer.getIndex() > this.project.getLastViewedVersion().getIndex()) continue;

                List<JFile> committedFiles = fixedCommit.getCommittedFiles();

                for (JFile file: committedFiles) {
                    String filename = file.getRelPath()+file.getName();

                    for (Release rel: affected) {
                        if (rel.getIndex() > this.project.getVersions().size()/2) continue;
                        Record r = hashmaps.get(rel.getIndex()-1).get(filename);

                        if (r != null) {
                            for (Record rec: records) {
                                if (rec.getFileName().equals(r.getFileName()) && rec.getVersion() == r.getVersion()) {
                                    rec.setBuggy("Yes");
                                }
                            }
                        }
                    }

                    for (Release rel: fixed) {
                        if (rel.getIndex() > this.project.getVersions().size()/2) continue;
                        Record r = hashmaps.get(rel.getIndex()-1).get(filename);

                        if (r != null) {
                            for (Record rec: records) {
                                if (rec.getFileName().equals(r.getFileName()) && rec.getVersion() == r.getVersion()) {
                                    rec.setBuggy("Yes");
                                    rec.addFix();
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    public List<Record> getDatasetRecords() {
        return datasetRecords;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

}
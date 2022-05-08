package org.example.logic.model.utils;

import org.example.logic.control.InspectionController;
import org.example.logic.model.features.FeatureCalculator;
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
        this.project = project;
        this.git = new GitHandler(project);
        this.jira = new JIRAHandler(project);
        this.datasetRecords = new ArrayList<>();
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
            for (Issue i: bugs) {
                Issue.prepareIssue(i, project.isExternal());
                /* retrieve the fixed commits of the issue */
                if (!project.isExternal()) i.setFixedCommits(findLinkedToIssueCommits(project, i.getKey()));
            }
            project.setBugIssues(bugs);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return bugs;
    }

    private List<Commit> findLinkedToIssueCommits(Project project, String issueKey) {
        /***
         * filter the commits of the project to those which have the issue key referenced in the commit message
         * param project: the project to inspect
         * param issueKey: the key of the issue to filter the commits
         */
        List<Commit> fixedCommits = new ArrayList<>();
        System.out.println(project.getCommits());
        System.out.println(project.getCommits().size());

        for (Commit c: project.getCommits()) {
            if (c.getMessage().contains(issueKey+":") || c.getMessage().contains(issueKey+"]") || c.getMessage().contains(issueKey+" ")){
                fixedCommits.add(c);
            }
        }
        return fixedCommits;
    }

    public void updateBugginess(List<Issue> issues) {
        /***
         * updates the bugginess of the files in the afflicted version of the issues fixed by commits.
         */
        List<JFile> touchedFiles;
        List<Release> av;

        /* for all the issues, get the fixed commits that have a link in the commits message to the issue */
        for (Issue issue: issues) {
            for (Commit commit: issue.getFixedCommits()) {
                touchedFiles = commit.getCommittedFiles();
                av = issue.getAffectedVersions();
                if (av.isEmpty()) System.out.println(issue.getKey());
                if (InspectionController.fulldebug) {
                    System.out.println("Update Bugginess -affected versions-: " + av);
                    System.out.println("Update Bugginess -touched files-: " + touchedFiles);
                }
                for (JFile file: touchedFiles) {
                    /* file is buggy in AV */
                    file.addBuggyReleases(av);
                }
            }
        }
    }

    public List<Record> produceRecords() {
        /***
         * produce dataset using project information about issues and commits that fix the issues.
         * The sample unit in the data is of type Record.
         */
        List<HashMap<String, Record>> hashMaps = new ArrayList<>(); //list of hashmaps for release: String (filename), Record (dataset record)
        List<Record> observations = new ArrayList<>();

        List<Release> releases = project.getVersions(); // consider only the selected percentage of the versions to create the dataset
        int releaseNum = releases.size(); // we consider only a portion of the releases

        /* create new hash map for each release to store records */
        for (int i = 0; i < releaseNum; i++) {
            hashMaps.add(new HashMap<>());
        }

//        /*test */
//        for (int i=0; i<releaseNum; i++) {
//            System.out.println("test hashmap n°"+i+":"+hashMaps.get(i));
//        }
        /* get project commits */
        List<Commit> commits = project.getCommits();

        for (Commit commit: commits) {
            /* get touched files from commit */
            Release commitRelease = commit.getVersion();
            int releaseIdx = commitRelease.getIndex();
            List<JFile> touchedFiles = commit.getCommittedFiles();
            for (JFile file : touchedFiles) {
                if (file.getRelPath().equals("bookkeeper-benchmark/src/main/java/org/apache/bookkeeper/benchmark/TestClient.java")) {
                    System.out.println(file);
                }
                // check if the file is present in the release of the commit, else the file must not be included in the dataset
                if (file.getReleases().contains(commitRelease)) {
                    setRecordsBugginess(file, hashMaps);
                }
            }
        }

        for (HashMap<String, Record> hashmap: hashMaps) {
            for (JFile file: project.getJavaFiles()) {
                Record record = hashmap.get(file.getRelPath());
                if (record != null) observations.add(record);
            }
        }
        /* Set the hashmap into the FeatureCalculator class */
        FeatureCalculator.getInstance().setHashMaps(hashMaps);
        return observations;
    }

    private void setRecordsBugginess(JFile file, List<HashMap<String, Record>> hashMaps) {
        /***
         * Sets the bugginess of the entries in the dataset
         * @param file:
         * @param hashMaps:
         */
        List<Release> av; //affected versions
        List<Release> pv; //present versions
        String filepath; //complete file name

        /* for each file get versions in which the file is present */
        pv = file.getReleases();
        filepath = file.getRelPath();
        System.out.println("filename: " + filepath);

        String rels = "releases: ";
        for (Release presRel : pv) {
            rels += presRel.getName() + "-" + presRel.getIndex() + ";";
        }
        System.out.println(rels);
        System.out.println("num releases: " + pv.size() + "\n");

        /* by default the file is not buggy in the releases in which it's present. */
        for (Release presRel : pv) {
            int index = presRel.getIndex();
            int i = index - 1;

            if (InspectionController.fulldebug) {
                System.out.println("Index: " + index);
                System.out.println("Index-1: " + i);
            }
            if (hashMaps.get(i).containsKey(filepath)) continue; //file already inserted as a record in the dataset
            Record record = new Record(index, filepath);
            record.setBuggy("No");
            /* add a new record to the release hashmap (acceding the list with the version index) */
            hashMaps.get(i).put(filepath, record);
            System.out.println("Put record in bucket hashmap n°" + i);
        }

        av = file.getBuggyReleases();

        if (!av.isEmpty()) {
            /* file is buggy in those releases specified in affected versions (av) - need to modify the record in the hashmap corresponding the version */
            for (Release bugRel : av) {
                int index = bugRel.getIndex();
                int i = index-1;
                Record record = hashMaps.get(i).get(filepath);
                if (record == null) continue; // file is buggy in a release in which is not present
                if (record.getBuggy().equals("Yes")) continue; // file is already labeled as buggy in that version!
                record.setBuggy("Yes");
            }
        }

    }

    public void calculateFeatures() {
        // TODO something's wrong
        /***
         * Calculate the features of the dataset using the FeatureCalculator class
         */

        List<HashMap<String, Record>> hashMaps = FeatureCalculator.getInstance().getHashMaps();
        List<Commit> commits = project.getCommits();
        int i = 0;
        String filepath;
        String[] content;
        String recordKey;

        for (Commit commit: commits) {
            i++;
            System.out.println("calculating commit "+i+"/"+commits.size()+": sha="+commit.getShaId()+" "+commit.getMessage()+" #committed: "+commit.getCommittedFiles().size());
            List<JFile> touchedFiles = commit.getCommittedFiles();
            Release commitRelease = commit.getVersion();
            String author = commit.getAuthor();
            int chgSetSize = touchedFiles.size()-1;
            HashMap<String, Record> releaseRecords = hashMaps.get(commitRelease.getIndex()-1);
            try {
                git.updateCommitLinesChanges(commit, releaseRecords, null);

                for (JFile file: touchedFiles) {
                    /**
                     * considerando la release del commit, vado a vedere da project.oldFiles se in quella release il file è stato ridenominato
                     * se il file ha una precedente ridenominazione in quella release allora significa che devo considerare la versione precedente
                     * del file per il calcolo delle features, ma recuprando il record dalla attuale nominazione.
                     */
                    // accedo alla lista di hashmap oldFiles in project per vedere se nella release del commit è presente una ridenominazione del file
                    int releaseIdx = commitRelease.getIndex();
//                    System.out.println(file.getRelPath()+"-"+project.checkRenomination(releaseIdx, file)+"-"+releaseRecords.containsKey(file.getRelPath()));
                    if (project.checkRenomination(releaseIdx, file)) {
                        // preleva dall'hashmap la nuova istanza del file
                        JFile newFile = project.getRenomination(releaseIdx, file);
                        // recupera il contenuto del file usando il nome della nuova istanza del file
                        recordKey = newFile.getRelPath();
//                        System.out.println(filepath+"-"+releaseRecords.containsKey(filepath));
                    } else {
                        // non ci sono ridenominazioni del file in release e si procede usando l'attuale istanza del file
                        recordKey = file.getRelPath();
                    }

                    filepath = file.getRelPath();
                    content = git.getFileContent(filepath, commit);

                    // accedo ai record usando il nome dell'istanza attuale del file
                    if (releaseRecords.containsKey(recordKey)) {
                        Record record = releaseRecords.get(recordKey);
                        FeatureCalculator.getInstance().updateLOC(content, record);
                        FeatureCalculator.getInstance().calculateLOCTouched(record);
                        FeatureCalculator.getInstance().updateChurn(record);
                        FeatureCalculator.getInstance().updateNR(record);
                        FeatureCalculator.getInstance().updateNAuth(author, record);
                        FeatureCalculator.getInstance().updateChgSetSize(chgSetSize, record);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
//    public List<Record> produceDataset() {
//        /***
//         * produce dataset using project information about issues and commits that fix the issues.
//         * The sample unit in the data is of type Record.
//         */
//        List<Record> records = new ArrayList<>();
//
//        List<Release> releases = project.getViewedVersions(); // consider only the selected percentage of the versions to create the dataset
//        int releaseNum = releases.size(); // we consider only a portion of the releases
//
//        List<Commit> commits = new ArrayList<>();
//        for (Commit c: project.getCommits()) {
//            commits.add(c);
//        }
//
//        List<HashMap<String, Record>> hashMaps = new ArrayList<>();
//        List<JFile> committedFiles = null;
//        Commit commit;
//
//        for (int i=0; i<releaseNum; i++) {
//            hashMaps.add(new HashMap<>()); // create new hash map for the release to store records
//            LocalDateTime limitDate = releases.get(i).getDate();
//
//            while (!commits.isEmpty() && commits.get(0).getDate().compareTo(limitDate) < 0) {
//                commit = commits.get(0);	//takes the first commit
//                committedFiles = commit.getCommittedFiles();
//
//                updateRecords(commit, committedFiles, hashMaps, records, i+1);
//
//                commits.remove(commit);
//            }
//
//        }
//        updateBugginess(hashMaps, records);
//        return records;
//    }
//
//    private void updateRecords(Commit commit, List<JFile> committedFiles, List<HashMap<String, org.example.logic.model.keyabstractions.Record>> hashMaps,
//                               List<Record> records, int release) {
//
//        org.example.logic.model.keyabstractions.Record r = null;
//        int idx = release-1;
//        for (int i=0; i<committedFiles.size(); i++) {
//            JFile file = committedFiles.get(i);
//            String filename = file.getRelPath()+file.getName();
//
//            r = hashMaps.get(idx).get(filename);
//
//            //if it's a new file in the release add it to the hash map and to the list
//            if (r == null) {
//                r = new Record(release, filename);
//                hashMaps.get(idx).put(filename, r);
//                records.add(r);
//            }
//
//            //set record info (dataset features)
//            r.setSize(file.getSize());
//            r.addLocTouched(file.getChanges());
//            r.addLocAdded(file.getAdditions());
//            r.addRevision();
//            r.addChgSetSize(committedFiles.size()-1);
//            r.addAuthor(commit.getAuthor());
//        }
//
//    }

//    public void updateBugginess(List<HashMap<String, Record>> hashmaps, List<Record> records) {
//        /***
//         * Update the bugginess status of each file of the project.
//         */
//        List<Issue> bugs = project.getBugIssues();
//        Release fixedVer = null;
//        Commit fixedCommit = null;
//        List<Release> affected; //added
//        List<Release> fixed;
//
//        for (Issue bug: bugs) {
//            if (!bug.getFixedCommits().isEmpty()) {
//
//                fixedCommit = bug.getFixedCommits().get(0);
//                fixedVer = bug.getFixVersion();
//                affected = bug.getAffectedVersions();
//                fixed = bug.getFixedVersions();
//
//                if (fixedVer == null) fixedVer = fixedCommit.getVersion();
//                if (fixedVer.getIndex() > this.project.getLastViewedVersion().getIndex()) continue;
//
//                List<JFile> committedFiles = fixedCommit.getCommittedFiles();
//
//                for (JFile file: committedFiles) {
//                    String filename = file.getRelPath()+file.getName();
//
//                    for (Release rel: affected) {
//                        if (rel.getIndex() > this.project.getVersions().size()/2) continue;
//                        Record r = hashmaps.get(rel.getIndex()-1).get(filename);
//
//                        if (r != null) {
//                            for (Record rec: records) {
//                                if (rec.getFileName().equals(r.getFileName()) && rec.getVersion() == r.getVersion()) {
//                                    rec.setBuggy("Yes");
//                                }
//                            }
//                        }
//                    }
//
//                    for (Release rel: fixed) {
//                        if (rel.getIndex() > this.project.getVersions().size()/2) continue;
//                        Record r = hashmaps.get(rel.getIndex()-1).get(filename);
//
//                        if (r != null) {
//                            for (Record rec: records) {
//                                if (rec.getFileName().equals(r.getFileName()) && rec.getVersion() == r.getVersion()) {
//                                    rec.setBuggy("Yes");
//                                    rec.addFix();
//                                }
//                            }
//                        }
//                    }
//
//                }
//            }
//        }
//    }

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
package org.example.logic.model.utils;

import org.example.logic.control.InspectionController;
import org.example.logic.model.features.FeatureCalculator;
import org.example.logic.model.handlers.GitHandler;
import org.example.logic.model.handlers.JIRAHandler;
import org.example.logic.model.keyabstractions.*;
import org.example.logic.model.keyabstractions.Record;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ProjectInspector {

    private final Project project;
    private final GitHandler git;
    private final JIRAHandler jira;
    private final List<Record> datasetRecords;

    public ProjectInspector(Project project) {
        this.project = project;
        this.git = new GitHandler(project);
        this.jira = new JIRAHandler(project);
        this.datasetRecords = new ArrayList<>();
    }

    /***
     * retrieve all the commits of the project selected in the config file
     */
    public List<Commit> inspectProjectCommits() {
        List<Commit> commits = git.lookupForCommits();
        List<Commit> refCommits = commits.parallelStream()
                .filter(commit -> commit.getMessage().contains(project.getProjName() + "-"))
                .toList();
        project.setCommits(commits);
        project.setRefCommits(refCommits);
        return commits;
    }

    /***
     * retrieve all the files of the project selected in the config file
     */
    public List<JFile> inspectProjectFiles() {
        List<Commit> commitList = project.getCommits();
        List<JFile> files = commitList.parallelStream()
                .map(git::lookupForFiles)
                .flatMap(List::stream).distinct().toList();

        project.setJavaFiles(files.stream().distinct().toList());
        return files;
    }

    /***
     * Retrieve project issues (bug type) from Jira
     */
    public List<Issue> inspectProjectIssues() {
        List<Issue> bugs = null;
        try {
            bugs = jira.retrieveProjectIssues("Bug");
            for (Issue i: bugs) {
                Issue.prepareIssue(i);
                /* retrieve the fixed commits of the issue */
                if (!project.isExternal()) i.setFixedCommits(findLinkedToIssueCommits(project, i.getKey()));
            }
            /* TODO order defects by their fix date */
            project.setBugIssues(bugs);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return bugs;
    }

    /***
     * filter the commits of the project to those which have the issue key referenced in the commit message
     * param project: the project to inspect
     * param issueKey: the key of the issue to filter the commits
     */
    private List<Commit> findLinkedToIssueCommits(Project project, String issueKey) {
        List<Commit> fixedCommits = new ArrayList<>();

        project.getCommits().parallelStream().filter(commit -> commit.getMessage().contains(issueKey+":") ||
                commit.getMessage().contains(issueKey+"]") ||
                commit.getMessage().contains(issueKey+ " "))
                .forEach(fixedCommits::add);
        return fixedCommits;
    }

    /***
     * updates the bugginess of the files in the afflicted version of the issues fixed by commits.
     */
    public void updateBugginess(List<Issue> issues) {
        List<JFile> touchedFiles;
        List<Release> av;

        /* for all the issues, get the fixed commits that have a link in the commits message to the issue */
        for (Issue issue: issues) {
            for (Commit commit: issue.getFixedCommits()) {
                touchedFiles = commit.getCommittedFiles();
                av = issue.getAffectedVersions();
                if (InspectionController.FULL_DEBUG) {
                    String log = "Update Bugginess -affected versions-: " + av;
                    Logger.getGlobal().log(Level.WARNING, log);
                    log = "Update Bugginess -touched files-: " + touchedFiles;
                    Logger.getGlobal().log(Level.WARNING, log);
                }
                for (JFile file: touchedFiles) {
                    /* file is buggy in AV */
                    file.addAndFillAffectedRelease(av);
                }
            }
        }
    }

    /***
     * produce dataset using project information about issues and commits that fix the issues.
     * The sample unit in the data is of type Record.
     */
    public List<Record> produceRecords() {
        List<HashMap<String, Record>> hashMaps = new ArrayList<>(); //list of hashmaps for release: String (filename), Record (dataset record)
        List<Record> observations = new ArrayList<>();

        List<Release> releases = project.getVersions(); // consider only the selected percentage of the versions to create the dataset
        int releaseNum = releases.size(); // we consider only a portion of the releases

        /* create new hash map for each release to store records */
        for (int i = 0; i < releaseNum; i++) {
            hashMaps.add(new HashMap<>());
        }

        /* get project commits */
        List<Commit> commits = project.getCommits();

        for (Commit commit: commits) {
            /* get touched files from commit */
            Release commitRelease = commit.getVersion();
            List<JFile> touchedFiles = commit.getCommittedFiles();
            touchedFiles.parallelStream()
                    .filter(file -> file.getReleases().contains(commitRelease))
                    .forEach(file -> setRecordsBugginess(file, hashMaps));
        }

        for (HashMap<String, Record> hashmap: hashMaps) {
            hashmap.forEach((s, r) -> observations.add(r));
        }
        /* Set the hashmap into the FeatureCalculator class */
        FeatureCalculator.setHashMaps(hashMaps);
        return observations;
    }

    /***
     * Sets the bugginess of the entries in the dataset
     * @param file:
     * @param hashMaps:
     */
    private void setRecordsBugginess(JFile file, List<HashMap<String, Record>> hashMaps) {
        List<Release> av; //affected versions
        List<Release> pv; //present versions
        String filepath; //complete file name

        /* for each file get versions in which the file is present */
        pv = file.getReleases();
        filepath = file.getRelPath();
        /* by default the file is not buggy in the releases in which it's present. */
        for (Release presRel : pv) {
            int index = presRel.getIndex();
            int i = index - 1;

            if (InspectionController.FULL_DEBUG) {
                String log = "Index: " + index;
                Logger.getGlobal().log(Level.WARNING, log);
                log = "Index-1: " + i;
                Logger.getGlobal().log(Level.WARNING, log);
            }
            if (hashMaps.get(i).containsKey(filepath)) continue; //file already inserted as a record in the dataset
            Record rec = new Record(index, filepath);
            rec.setBuggy("No");
            /* add a new record to the release hashmap (acceding the list with the version index) */
            hashMaps.get(i).put(filepath, rec);
        }

        av = file.getBuggyReleases();

        if (!av.isEmpty()) {
            /* file is buggy in those releases specified in affected versions (av) - need to modify the record in the hashmap corresponding the version */
            for (Release bugRel : av) {
                int index = bugRel.getIndex();
                int i = index-1;
                Record rec = hashMaps.get(i).get(filepath);
                if (rec == null || rec.getBuggy().equals("Yes")) continue; // file is already labeled as buggy in that version! or // file is buggy in a release in which is not present
                rec.setBuggy("Yes");
            }
        }

    }

    /***
     * Calculate the features of the dataset using the FeatureCalculator class
     */
    public List<Record> calculateFeatures() {
        List<Commit> commits = project.getCommits();
        List<List<Record>> recordsLists = new ArrayList<>();
        for (Commit commit : commits) {
            List<JFile> touchedFiles = commit.getCommittedFiles();
            List<Record> records = touchedFiles.parallelStream()
                    .map(file -> updateRecord(file, commit))
                    .filter(Objects::nonNull).toList();
            recordsLists.add(records);
        }
        return recordsLists.parallelStream()
                .flatMap(Collection::stream).distinct().toList();
    }

    private Record updateRecord(JFile file, Commit commit) {
        Release commitRelease = commit.getVersion();
        int releaseIdx = commitRelease.getIndex();
        HashMap<String, Record> releaseRecords = FeatureCalculator.getHashMaps()
                .get(commitRelease.getIndex()-1);
        String recordKey = file.getRelPath();

        String[] content = file.getContent().get(releaseIdx);
        int additions = file.getAdditions().get(releaseIdx);
        int maxAdditions = file.getMaxAdditions().get(releaseIdx);
        int deletions = file.getDeletions().get(releaseIdx);
        int age = file.getAges().get(releaseIdx);
        String author = commit.getAuthor();
        int chgSetSize = commit.getCommittedFiles().size() - 1;

        if (content.length == 0) {
            /* last commit in release deleted the file */
            releaseRecords.remove(recordKey);
        } else {
            /* update the record of the file in release */
            if (releaseRecords.containsKey(recordKey)) {
                Record rec = releaseRecords.get(recordKey);
                FeatureCalculator.setAdditions(rec, additions);
                FeatureCalculator.setMaxLocAdded(rec, maxAdditions);
                FeatureCalculator.setDeletions(rec, deletions);
                FeatureCalculator.updateLOC(content, rec);
                FeatureCalculator.calculateLOCTouched(rec);
                FeatureCalculator.updateChurn(rec);
                FeatureCalculator.updateNR(rec);
                FeatureCalculator.updateNAuth(author, rec);
                FeatureCalculator.updateChgSetSize(chgSetSize, rec);
                if (project.getRefCommits().contains(commit)) FeatureCalculator.updateNFix(rec, commit.getTicketTag());
                FeatureCalculator.calculateAge(age, rec);
                FeatureCalculator.calculateWeightedAge(rec);
                releaseRecords.put(recordKey, rec);
                return rec;
            }
        }

        return null;
    }

    /***
     * Select first percentage of records as specified by percentage size
     * @return listo of records selected
     */
    public List<Record> selectRecords(List<Record> instances) {
        return instances.parallelStream().filter(rec -> {
            Release recRelease = Release.findVersionByIndex(project.getVersions(), rec.getVersion());
            return project.getViewedVersions().contains(recRelease);
        }).toList();
    }

    public List<Record> getDatasetRecords() {
        return datasetRecords;
    }

}
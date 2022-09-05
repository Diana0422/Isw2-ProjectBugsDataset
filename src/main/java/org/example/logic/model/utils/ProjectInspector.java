package org.example.logic.model.utils;

import org.example.logic.control.InspectionController;
import org.example.logic.model.features.FeatureCalculator;
import org.example.logic.model.handlers.GitHandler;
import org.example.logic.model.handlers.JIRAHandler;
import org.example.logic.model.keyabstractions.*;
import org.example.logic.model.keyabstractions.Record;

import java.io.IOException;
import java.time.LocalDateTime;
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
        /* filter only commits that reference an issue (contains in the message something like:
            "[PROJECTNAME-#]" or PROJECTNAME-
         */

        List<Commit> refCommits = commits.parallelStream()
                .filter(commit -> commit.getMessage().contains(project.getProjName() + "-"))
                .toList();
        /* set project commits and referenced commits */
        project.setCommits(commits);
        project.setRefCommits(refCommits);
        return commits;
    }

    /***
     * Retrieves all the files of the project selected in the config file
     */
    public void inspectProjectFiles() {
        /* retrieve files from git */
        List<Commit> commitList = project.getCommits();
        commitList.forEach(git::lookupForFiles);
    }

    public void fixReleaseGaps() {
        /* fix release gaps */
        int numReleases = project.getVersions().size();
        List<HashMap<String, JFile>> files = project.getFiles();
        for (int i = 0; i < numReleases; i++) {
            HashMap<String, JFile> fileHashMap = files.get(i);
            fileHashMap.forEach((s, file) -> file.fillReleases());
        }
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

            /* Order defects by their fix date */
            List<Issue> sortedBugs = bugs
                    .stream()
                    .sorted((o1, o2) -> {
                        List<Release> o1FixedVersions = o1.getFixedVersions();
                        List<Release> o2FixedVersions = o2.getFixedVersions();
                        if (!o2FixedVersions.isEmpty() && !o1FixedVersions.isEmpty()) {
                            Release fixedRelease1 = o1FixedVersions.get(0);
                            Release fixedRelease2 = o2FixedVersions.get(0);
                            LocalDateTime date1 = fixedRelease1.getDate();
                            LocalDateTime date2 = fixedRelease2.getDate();
                            return date1.compareTo(date2);
                        } else if (!o1FixedVersions.isEmpty()) {
                            return -1;
                        } else if (!o2FixedVersions.isEmpty()) {
                            return 1;
                        } else {
                            return 0;
                        }
            }).toList();
            project.setBugIssues(sortedBugs);

        } catch (IOException e) {
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
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
                if (InspectionController.isFullDebug()) {
                    String log = "Update Bugginess -affected versions-: " + av;
                    Logger.getGlobal().log(Level.WARNING, log);
                    log = "Update Bugginess -touched files-: " + touchedFiles;
                    Logger.getGlobal().log(Level.WARNING, log);
                }
                for (JFile file: touchedFiles) {
                    /* file is buggy in AV */
                    file.addAffectedRelease(av);
                }
            }
        }
    }

    /***
     * produce dataset using project information about issues and commits that fix the issues.
     * The sample unit in the data is of type Record.
     */
    public void produceRecords() {
        List<HashMap<String, Record>> hashMaps = new ArrayList<>(); //list of hashmaps for release: String (filename), Record (dataset record)
        List<HashMap<String, JFile>> files = project.getFiles();
        List<Release> releases = project.getVersions(); // consider only the selected percentage of the versions to create the dataset
        int releaseNum = releases.size(); // we consider only a portion of the releases

        /* Initialize the dataset */
        for (int i = 0; i < releaseNum; i++) {
            int releaseIdx = i + 1;
            // create new hash map for each release to store records
            hashMaps.add(new HashMap<>());
            // add releases and filenames in the dataset as new records
            HashMap<String, JFile> fileHashMap = files.get(i);
            fileHashMap.forEach((s, file) -> addMissingRecord(releaseIdx, file, hashMaps));
        }

        for (int i = 0; i < releaseNum; i++) {
            HashMap<String, JFile> fileHashMap = files.get(i);
            fileHashMap.forEach((s, file) -> setRecordsBugginess(file, hashMaps));
        }

        /* Set the hashmap into the FeatureCalculator class */
        FeatureCalculator.setHashMaps(hashMaps);
    }

    private void addMissingRecord(int releaseIdx, JFile file, List<HashMap<String, Record>> hashMaps) {
        String filepath = file.getRelPath(); //complete file name
        int i = releaseIdx - 1;
        /* if the file was renamed in a previous release, then do not include in the dataset */
        if (file.checkRenamed()) {
            int renameIdx = file.getRenamedRelease().getIndex();
            if (releaseIdx >= renameIdx) return; // fixme metti l'uguale anche nella condizione
        }
        /* by default the file is not buggy in the releases in which it's present. */
        if (!hashMaps.get(i).containsKey(filepath)) {
            /* record with release index and filename specified is not already present in dataset */
            Record rec = new Record(releaseIdx, filepath);
            rec.setBuggy("No");
            /* add a new record to the release hashmap (acceding the list with the version index) */
            hashMaps.get(i).put(filepath, rec);
        }
    }

    /***
     * Sets the bugginess of the entries in the dataset
     * @param file :
     * @param hashMaps :
     */
    private void setRecordsBugginess(JFile file, List<HashMap<String, Record>> hashMaps) {
        List<Release> av; //affected versions
        String filepath = file.getRelPath(); //complete file name

        /* Set Yes if file is buggy in the version */
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
        List<Record> recordsLists = new ArrayList<>();
        /* update record based on commit */
        for (Commit commit : commits) {
            List<JFile> touchedFiles = commit.getCommittedFiles();
            touchedFiles.forEach(file -> {
                Record rec = updateRecord(file, commit);
                if (rec != null) recordsLists.add(rec);
            });
        }

        /* update record with non-commit based features */
        List<HashMap<String, JFile>> files = project.getFiles();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            int releaseIdx = i + 1;
            HashMap<String, JFile> fileHashMap = files.get(i);
            fileHashMap.forEach((s, file) -> {
                Record rec = updateRecord(releaseIdx, file);
                if (rec != null) recordsLists.add(rec);
            });
        }

        List<Record> instances = recordsLists.stream().distinct().collect(Collectors.toList());

        /* order records by version index */
        return instances.stream().sorted((o1, o2) -> {
            int version1 = o1.getVersion();
            int version2 = o2.getVersion();
            return Integer.compare(version1, version2);
        }).toList();

    }

    /**
     * Non commit-based update record to update features
     * @param releaseIdx
     * @param file
     * @return
     */
    private Record updateRecord(int releaseIdx, JFile file) {
        HashMap<String, Record> releaseRecords = FeatureCalculator.getHashMaps()
                .get(releaseIdx - 1);
        String recordKey = file.getRelPath();

        String[] content = file.getContent().get(releaseIdx-1);
        int additions = file.getAdditions().get(releaseIdx-1);
        int maxAdditions = file.getMaxAdditions().get(releaseIdx-1);
        int deletions = file.getDeletions().get(releaseIdx-1);
        int age = file.getAges().get(releaseIdx-1);

        if (content.length == 0) {
            /* last commit in release deleted the file */
            releaseRecords.remove(recordKey);
        } else {
            /* update the record of the file in release */
            return releaseRecords.computeIfPresent(recordKey, (s, rec) -> {
                FeatureCalculator.setAdditions(rec, additions);
                FeatureCalculator.setMaxLocAdded(rec, maxAdditions);
                FeatureCalculator.setDeletions(rec, deletions);
                FeatureCalculator.updateLOC(content, rec);
                FeatureCalculator.calculateLOCTouched(rec);
                FeatureCalculator.updateChurn(rec);
                FeatureCalculator.calculateAge(age, rec);
                FeatureCalculator.calculateWeightedAge(rec);
                releaseRecords.put(recordKey, rec);
                return rec;
            });
        }

        return null;
    }

    private Record updateRecord(JFile file, Commit commit) {
        Release commitRelease = commit.getVersion();
        int releaseIdx = commitRelease.getIndex();
        HashMap<String, Record> releaseRecords = FeatureCalculator.getHashMaps()
                .get(releaseIdx-1);
        String recordKey = file.getRelPath();

        String author = commit.getAuthor();
        int chgSetSize = commit.getCommittedFiles().size() - 1;
        /* update the record of the file in release */
        if (releaseRecords.containsKey(recordKey)) {
            Record rec = releaseRecords.get(recordKey);
            FeatureCalculator.updateNAuth(author, rec);
            FeatureCalculator.updateChgSetSize(chgSetSize, rec);
            if (project.getRefCommits().contains(commit)) FeatureCalculator.updateNFix(rec);
            FeatureCalculator.updateNR(rec);
            releaseRecords.put(recordKey, rec);
            return rec;
        }

        return null;
    }

    /***
     * Select first percentage of records as specified by percentage size
     * @return listo of records selected
     */
    public List<Record> selectRecords(List<Record> records) {
        /* select record if belongs to selected releases (e.g first 50%) */
        return records.stream().filter(rec -> {
            Release recRelease = Release.findVersionByIndex(project.getVersions(), rec.getVersion());
            return project.getViewedVersions().contains(recRelease);
        }).toList();
    }

    public List<Record> getDatasetRecords() {
        return datasetRecords;
    }

}
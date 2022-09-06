package org.example.logic.model.keyabstractions;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

public class JFile {

    private final Project project;
    private String name;
    private final String relpath;
    private final Release created;
    private Release deleted;
    private final HashMap<Release, JFile> renamed;
    private HashMap<Integer,Integer> additions; // (version, addition)
    private HashMap<Integer,Integer> maxAdditions;
    private HashMap<Integer,Integer> deletions; // (version, deletion)
    private HashMap<Integer,Integer> changes; // (version, changes)
    private HashMap<Integer, String[]> content; // (version, content)
    private HashMap<Integer, Integer> ages; // (version, age)
    private HashMap<Integer, List<Commit>> revisions;
    private List<Release> releases;
    private final List<Release> buggyReleases;


    /**
     * Constructor used for file replication (doesn't inherit previous stats)
     * @param releaseIndex
     * @param prevInstance
     */
    public JFile(int releaseIndex, JFile prevInstance) {
        /* Inherit prev instance file stats */
        this.project = prevInstance.project;
        this.name = prevInstance.name;
        this.relpath = prevInstance.relpath;
        this.created = prevInstance.created;
        this.renamed = prevInstance.renamed;
        this.releases = prevInstance.releases;
        this.buggyReleases = prevInstance.buggyReleases;
        /* Inherit prev instance features */
        this.additions = prevInstance.additions;
        this.maxAdditions = prevInstance.maxAdditions;
        this.deletions = prevInstance.deletions;
        this.changes = prevInstance.changes;
        this.content = prevInstance.content;
        this.ages = prevInstance.ages;
        this.revisions = prevInstance.revisions;
        /* Update features */
        updateAdditions(releaseIndex, 0);
        updateDeletions(releaseIndex, 0);
        updateChanges(releaseIndex, 0);
        updateContent(releaseIndex, content.get(releaseIndex-1));
        updateAge(releaseIndex);
    }

    /* when the file gets renamed */
    public JFile(Release created, String filename, String filepath, JFile oldFile) {
        this.project = oldFile.project;
        this.name = filename;
        this.relpath = filepath;
        this.created = created;
        this.renamed = new HashMap<>(1);
        /* Inherit old file stats */
        this.additions = oldFile.additions;
        this.maxAdditions = oldFile.maxAdditions;
        this.deletions = oldFile.deletions;
        this.changes = oldFile.changes;
        this.content = oldFile.content;
        this.ages = oldFile.ages;
        this.revisions = oldFile.revisions;
        /* Init releases as brand new */
        this.releases = new ArrayList<>();
        this.buggyReleases = new ArrayList<>();
        /* add file to the list of files */
        project.addFile(created.getIndex(), this);
        /* remove old file from the list of files */
        project.removeFile(created.getIndex(), oldFile);
    }

    public JFile(Project project, String name, String relpath, Release created) {
        this.name = name;
        this.project = project;
        this.relpath = relpath;
        this.created = created;
        this.renamed = new HashMap<>(1);
        this.additions = initAdditions();
        this.maxAdditions = initMaxAdditions();
        this.deletions = initDeletions();
        this.changes = initChanges();
        this.ages = initAges();
        this.revisions = initRevisions();
        this.content = initContent();
        this.releases = new ArrayList<>();
        this.buggyReleases = new ArrayList<>();
    }

    private HashMap<Integer, String[]> initContent() {
        this.content = new HashMap<>();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            this.content.put(i, new String[]{});
        }
        return content;
    }

    private HashMap<Integer, List<Commit>> initRevisions() {
        this.revisions = new HashMap<>();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            this.revisions.put(i, new ArrayList<>());
        }
        return revisions;
    }

    private HashMap<Integer, Integer> initAges() {
        this.ages = new HashMap<>();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            this.ages.put(i, 0);
        }
        return ages;
    }

    private HashMap<Integer, Integer> initChanges() {
        this.changes = new HashMap<>();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            this.changes.put(i, 0);
        }
        return changes;
    }

    private HashMap<Integer, Integer> initDeletions() {
        this.deletions = new HashMap<>();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            this.deletions.put(i, 0);
        }
        return deletions;
    }

    private HashMap<Integer, Integer> initMaxAdditions() {
        this.maxAdditions = new HashMap<>();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            this.maxAdditions.put(i, 0);
        }
        return maxAdditions;
    }

    private HashMap<Integer, Integer> initAdditions() {
        this.additions = new HashMap<>();
        int numReleases = project.getVersions().size();
        for (int i = 0; i < numReleases; i++) {
            this.additions.put(i, 0);
        }
        return additions;
    }

    public void setRenamed(Release release, JFile newFile) {
        this.renamed.put(release, newFile);
        /* mark file as deleted in that release */
        this.deleted = release;
    }

    public boolean checkRenamed() {
        return renamed.size() != 0;
    }

    public Release getRenamedRelease() {
        Release release = null;
        for (Map.Entry<Release, JFile> entry : renamed.entrySet()) {
            release = entry.getKey();
        }
        return release;
    }

    public void setDeleted(Release release) {
        this.deleted = release;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Release> getReleases() {
        return releases;
    }

    public void setReleases(List<Release> releases) {
        this.releases = releases;
    }


    /**
     * Fills gaps between releases in which a file is present
     */
    public void fillReleases() {
        /* add file instance to missing releases in files hashmap */
        List<HashMap<String, JFile>> files = project.getFiles();
        JFile prevInstance = this;
        int deletedIdx;
        if (deleted != null) {
            deletedIdx = this.deleted.getIndex();
        } else {
            deletedIdx = project.getVersions().size();
        }
        int createdIdx = this.created.getIndex();
        for (int i = createdIdx-1; i < deletedIdx; i++) {
            HashMap<String, JFile> fileHashMap = files.get(i);
            if (fileHashMap.containsKey(relpath)) {
                String[] text = fileHashMap.get(relpath).getContent().get(i);
                if (text.length == 0) {
                    project.replicateContent(i+1, prevInstance);
                    project.replicateRelease(i+1, prevInstance);
                }
                prevInstance = fileHashMap.get(relpath);
                continue;
            }
            project.replicateMissingFile(i+1, i+2, prevInstance);
        }
    }

    public String getRelPath() {
        return relpath;
    }

    public Map<Integer, Integer> getAdditions() {
        return additions;
    }

    public void updateAdditions(Integer version, Integer deltaAddition) {
        Integer addition = this.additions.get(version-1);
        this.additions.put(version-1, addition+deltaAddition);
        updateMaxAdditions(version, deltaAddition);
    }

    private void updateMaxAdditions(Integer version, Integer deltaAddition) {
        Integer max = this.maxAdditions.get(version-1);
        if (deltaAddition >= max) this.maxAdditions.put(version-1, deltaAddition);
    }

    public Map<Integer, Integer> getMaxAdditions() {
        return maxAdditions;
    }

    public Map<Integer, Integer> getDeletions() { return deletions; }

    public void updateDeletions(Integer version, Integer deltaDeletions) {
        Integer del = this.deletions.get(version-1);
        this.deletions.put(version-1, del+deltaDeletions);
    }

    public Map<Integer, Integer> getChanges() {
        return changes;
    }

    public void updateChanges(Integer version, Integer deltaChanges) {
        Integer ch = this.changes.get(version-1);
        this.changes.put(version-1, ch+deltaChanges);
    }

    public Map<Integer, String[]> getContent() { return content;}

    public void updateContent(Integer version, String[] content) {
        this.content.put(version-1, content);
    }

    public List<Release> getBuggyReleases() {
        return buggyReleases;
    }

    /***
     * adds a new release to the list of releases in which the file is buggy
     */
    public void addBuggyReleases(List<Release> buggyReleases) {
        for (Release release: buggyReleases) {
            /* add the release if not already in the list */
            if (!this.getBuggyReleases().contains(release)) {
                this.getBuggyReleases().add(release);
            }
            /* add to the releases list if not already present */
            if (!this.getReleases().contains(release)) {
                this.getReleases().add(release);
            }
        }
    }

    /***
     * adds a new release to the list of releases in which the file is present
     * and fills the gaps between releases
     */
    public void addAffectedRelease(List<Release> affRelease) {
        addBuggyReleases(affRelease);
        this.buggyReleases.sort(Comparator.comparing(Release::getDate));
    }

    /***
     * adds a new release to the list of releases in which the file is present
     * and fills the gaps between releases
     */
    public void addAndFillRelease(Release release) {
        addRelease(release);
        this.releases.sort(Comparator.comparing(Release::getDate));
    }

    /***
     * adds a new release to the list of releases in which the file is present
     */
    public void addRelease(Release release) {
        if (!getReleases().contains(release)) getReleases().add(release);
    }

    public void addRevision(Commit commit) {
        int releaseIdx = commit.getVersion().getIndex();
        List<Commit> commits = revisions.get(releaseIdx-1);
        commits.add(commit);
        revisions.put(releaseIdx-1, commits);
    }

    /***
     * updates file age given a commit
     * @param version the release index to consider to update age
     */
    public void updateAge(int version) {
        LocalDate releaseDate = Release.findVersionByIndex(project.getVersions(), version).getDate().toLocalDate();
        LocalDate creation = this.created.getDate().toLocalDate();
        Period period = Period.between(creation, releaseDate);
        int daysAge = period.getDays();
        int months = period.getMonths();
        int years = period.getYears();
        int weeksAge = (int) Math.ceil(((daysAge+30*months+365*years)/ 7.0));
        this.ages.put(version-1, weeksAge);
    }

    public Map<Integer, Integer> getAges() {
        return ages;
    }

    public Release getDeleted() {
        return deleted;
    }

    public Release getCreated() {return created;}
}
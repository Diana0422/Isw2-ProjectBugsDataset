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
    private final HashMap<Integer,Integer> additions; // (version, addition)
    private final HashMap<Integer,Integer> maxAdditions;
    private final HashMap<Integer,Integer> deletions; // (version, deletion)
    private final HashMap<Integer,Integer> changes; // (version, changes)
    private final HashMap<Integer, String[]> content; // (version, content)
    private final HashMap<Integer, Integer> ages; // (version, age)
    private final HashMap<Integer, List<Commit>> revisions;
    private List<Release> releases;
    private List<Release> buggyReleases;


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

    public JFile(JFile prevInstance) {
        /* Inherit prev instance file stats */
        this.project = prevInstance.project;
        this.name = prevInstance.name;
        this.relpath = prevInstance.relpath;
        this.created = prevInstance.created;
        this.renamed = prevInstance.renamed;
        this.releases = prevInstance.releases;
        this.buggyReleases = prevInstance.buggyReleases;
        this.revisions = prevInstance.revisions;
        /* Inherit prev instance features */
        this.additions = prevInstance.additions;
        this.maxAdditions = prevInstance.maxAdditions;
        this.deletions = prevInstance.deletions;
        this.changes = prevInstance.changes;
        this.content = prevInstance.content;
        this.ages = prevInstance.ages;
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
        this.additions = new HashMap<>();
        this.maxAdditions = new HashMap<>();
        this.deletions = new HashMap<>();
        this.changes = new HashMap<>();
        this.content = new HashMap<>();
        this.ages = new HashMap<>();
        this.revisions = new HashMap<>();
        this.releases = new ArrayList<>();
        this.buggyReleases = new ArrayList<>();
    }

    public void setRenamed(Release release, JFile newFile) {
        this.renamed.put(release, newFile);
        /* mark file as deleted in that release */
        this.deleted = release;
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
     * @param releases the releases of the file
     */
    public void fill(List<Release> releases) {
        int i;
        Release currentRelease;
        Release nextRelease;
        for (i=0; i<releases.size()-1; i++) {
            /* iterate on list of releases */
            currentRelease = releases.get(i);
            nextRelease = releases.get(i+1);
            int currentReleaseIndex = currentRelease.getIndex();
            int nextReleaseIndex = nextRelease.getIndex();
            /* check the offset between the two indices of the releases */
            int offset = nextReleaseIndex - currentReleaseIndex;
            if (offset>1) {
                /* fill the gap replicating the missing releases */
                replicate(currentRelease, nextRelease);
            }
        }
        this.getReleases().sort(Comparator.comparing(Release::getIndex));
    }

    private void replicate(Release currentRelease, Release nextRelease) {
        int currentReleaseIdx = currentRelease.getIndex();
        int nextReleaseIdx = nextRelease.getIndex();

        for (int idx=currentReleaseIdx+1; idx<nextReleaseIdx; idx++) {
            Release rel = Release.findVersionByIndex(project.getVersions(), idx);
            addRelease(rel);
            if (additions.containsKey(currentReleaseIdx)) updateAdditions(rel.getIndex(), additions.get(currentReleaseIdx));
            if (deletions.containsKey(currentReleaseIdx)) updateDeletions(rel.getIndex(), deletions.get(currentReleaseIdx));
            if (changes.containsKey(currentReleaseIdx)) updateChanges(rel.getIndex(), changes.get(currentReleaseIdx));
            if (content.containsKey(currentReleaseIdx)) updateContent(rel.getIndex(), content.get(currentReleaseIdx));
        }
    }


    /**
     *
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
                prevInstance = fileHashMap.get(relpath); // project.replicateRelease(inserire sopra)
                continue;
            }
            project.replicateMissingFile(i+1, i+2, prevInstance);
            project.replicateRelease(i+1, prevInstance);
        }
    }

    public String getRelPath() {
        return relpath;
    }

    public Map<Integer, Integer> getAdditions() {
        return additions;
    }

    public void updateAdditions(Integer version, Integer deltaAddition) {
        if (this.additions.containsKey(version)) {
            Integer addition = this.additions.get(version);
            this.additions.put(version, addition+deltaAddition);
        } else {
            this.additions.put(version, deltaAddition);
        }
        updateMaxAdditions(version, deltaAddition);
    }

    private void updateMaxAdditions(Integer version, Integer deltaAddition) {
        if (this.maxAdditions.containsKey(version)) {
            Integer max = this.maxAdditions.get(version);
            if (deltaAddition >= max) this.maxAdditions.put(version, deltaAddition);
        } else {
            this.maxAdditions.put(version, deltaAddition);
        }
    }

    public Map<Integer, Integer> getMaxAdditions() {
        return maxAdditions;
    }

    public Map<Integer, Integer> getDeletions() { return deletions; }

    public void updateDeletions(Integer version, Integer deltaDeletions) {
        if (this.deletions.containsKey(version)) {
            Integer del = this.deletions.get(version);
            this.deletions.put(version, del+deltaDeletions);
        } else {
            this.deletions.put(version, deltaDeletions);
        }
    }

    public Map<Integer, Integer> getChanges() {
        return changes;
    }

    public void updateChanges(Integer version, Integer deltaChanges) {
        if (this.changes.containsKey(version)) {
            Integer ch = this.changes.get(version);
            this.changes.put(version, ch+deltaChanges);
        } else {
            this.changes.put(version, deltaChanges);
        }
    }

    public Map<Integer, String[]> getContent() { return content;}

    public void updateContent(Integer version, String[] content) {
        this.content.put(version, content);
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
        fill(this.releases);
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
        if (commits == null) commits = new ArrayList<>();
        commits.add(commit);
        revisions.put(releaseIdx-1, commits);
    }

    public void setBuggyReleases(List<Release> buggyReleases) {
        this.buggyReleases = buggyReleases;
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
        this.ages.put(version, weeksAge);
    }

    public Map<Integer, Integer> getAges() {
        return ages;
    }

    public Release getDeleted() {
        return deleted;
    }
}
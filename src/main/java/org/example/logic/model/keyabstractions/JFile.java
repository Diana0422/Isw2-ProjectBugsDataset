package org.example.logic.model.keyabstractions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.*;

public class JFile {

    private final Project project;
    private String name;
    private final String relpath;
    private final String oldPath;
    private final LocalDateTime creation;
    private final HashMap<Integer,Integer> additions; // (version, addition)
    private final HashMap<Integer,Integer> maxAdditions;
    private final HashMap<Integer,Integer> deletions; // (version, deletion)
    private final HashMap<Integer,Integer> changes; // (version, changes)
    private final HashMap<Integer, String[]> content; // (version, content)
    private final HashMap<Integer, Integer> ages; // (version, age)
    private final List<Commit> revisions;
    private List<Release> releases;
    private List<Release> buggyReleases;


    public JFile(Project project, String name, String relpath, LocalDateTime creation) {
        this.name = name;
        this.project = project;
        this.relpath = relpath;
        this.creation = creation;
        this.additions = new HashMap<>();
        this.maxAdditions = new HashMap<>();
        this.deletions = new HashMap<>();
        this.changes = new HashMap<>();
        this.content = new HashMap<>();
        this.ages = new HashMap<>();
        this.revisions = new ArrayList<>();
        this.releases = new ArrayList<>();
        this.buggyReleases = new ArrayList<>();
        this.oldPath = relpath;
    }

    public String getOldPath() {
        return oldPath;
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
    public void addAndFillAffectedRelease(List<Release> affRelease) {
        addBuggyReleases(affRelease);
        this.buggyReleases.sort(Comparator.comparing(Release::getDate));
        fill(this.buggyReleases);
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

    public List<Commit> getRevisions() {
        return revisions;
    }

    public void addRevision(Commit commit) {this.revisions.add(commit);}

    public void setBuggyReleases(List<Release> buggyReleases) {
        this.buggyReleases = buggyReleases;
    }

    /***
     * updates file age given a commit
     * @param version the release index to consider to update age
     */
    public void updateAge(int version) {
        LocalDate releaseDate = Release.findVersionByIndex(project.getVersions(), version).getDate().toLocalDate();
        LocalDate created = this.creation.toLocalDate();
        int daysAge = Period.between(created, releaseDate).getDays();
        int weeksAge = (int) Math.ceil((daysAge / 7.0));
        this.ages.put(version, weeksAge);
    }

    public Map<Integer, Integer> getAges() {
        return ages;
    }
}
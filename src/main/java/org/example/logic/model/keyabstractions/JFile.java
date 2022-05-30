package org.example.logic.model.keyabstractions;

import java.util.*;

public class JFile {

    private final Project project;
    private String name;
    private final String relpath;
    private final String oldPath;
    private final HashMap<Integer,Integer> additions; // (version, addition)
    private final HashMap<Integer,Integer> deletions; // (version, deletion)
    private final HashMap<Integer,Integer> changes; // (version, changes)
    private final HashMap<Integer, String[]> content; // (version, content)
    private final List<Commit> revisions;
    private List<Release> releases;
    private List<Release> buggyReleases;


    public JFile(Project project, String name, String relpath) {
        this.name = name;
        this.project = project;
        this.relpath = relpath;
        this.additions = new HashMap<>();
        this.deletions = new HashMap<>();
        this.changes = new HashMap<>();
        this.content = new HashMap<>();
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

    public void fillReleaseGaps(List<Release> releases) {
        // FIXME check
        Release lastRelease = releases.get(0);
        Release currentRelease;
        for (int i=lastRelease.getIndex(); i<releases.size(); i++) {
            currentRelease = releases.get(i);
            int currentReleaseIdx = currentRelease.getIndex();
            int lastReleaseIdx = lastRelease.getIndex();
            if (currentReleaseIdx == lastReleaseIdx+1) {
                lastRelease = currentRelease;
            } else {
                /* fill the gap replicating the missing releases */
                this.getReleases().sort(Comparator.comparing(Release::getDate));
                replicateRelease(lastRelease, currentRelease);
            }
        }
    }

    private void replicateRelease(Release lastRelease, Release currentRelease) {
        // FIXME check
        int lastReleaseIdx = lastRelease.getIndex();
        int currentReleaseIdx = currentRelease.getIndex();

        for (int idx=lastReleaseIdx+1; idx<currentReleaseIdx; idx++) {
            Release rel = Release.findVersionByIndex(project.getVersions(), idx);
            addRelease(rel);
            if (additions.containsKey(lastReleaseIdx)) updateAdditions(rel.getIndex(), additions.get(lastReleaseIdx));
            if (deletions.containsKey(lastReleaseIdx)) updateDeletions(rel.getIndex(), deletions.get(lastReleaseIdx));
            if (changes.containsKey(lastReleaseIdx)) updateChanges(rel.getIndex(), changes.get(lastReleaseIdx));
            if (content.containsKey(lastReleaseIdx)) updateContent(rel.getIndex(), content.get(lastReleaseIdx));
            lastRelease = currentRelease;
            lastReleaseIdx = lastRelease.getIndex();
            currentReleaseIdx = currentRelease.getIndex();
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
        }
    }

    /***
     * adds a new release to the list of releases in which the file is present
     * and fills the gaps between releases
     */
    public void addAndFillAffectedRelease(List<Release> affRelease) {
        addBuggyReleases(affRelease);
        this.buggyReleases.sort(Comparator.comparing(Release::getDate));
        fillReleaseGaps(this.buggyReleases);
    }

    /***
     * adds a new release to the list of releases in which the file is present
     * and fills the gaps between releases
     */
    public void addAndFillRelease(Release release) {
        addRelease(release);
        this.releases.sort(Comparator.comparing(Release::getDate));
        fillReleaseGaps(this.releases);
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

    public void addRevision(Commit commit) {
        this.revisions.add(commit);
    }

    public void setBuggyReleases(List<Release> buggyReleases) {
        this.buggyReleases = buggyReleases;
    }
}
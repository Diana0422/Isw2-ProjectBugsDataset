package org.example.logic.model.keyabstractions;

import org.example.logic.control.InspectionController;
import org.example.logic.model.utils.Parser;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JFile {

    private Project project;
    private String name;
    private String relpath;
    private String oldPath;
    private String oldName;
//    private LocalDateTime lastCommitDate;
    private Integer additions;
    private Integer deletions;
    private Integer changes;
    private List<Commit> revisions;
    private Integer size;
    private List<Release> releases;
    private List<Release> buggyReleases;
    /* test */
    private List<Commit> addedCommits;
    private List<Commit> modifiedCommits;
    private List<Commit> deletedCommits;
    private List<Commit> renamedCommits;


    public JFile(Project project, String name, String relpath) {
        this.name = name;
        this.project = project;
        this.relpath = relpath;
        this.additions = 0;
        this.deletions = 0;
        this.changes = 0;
        this.revisions = new ArrayList<>();
        this.size = 0;
        this.releases = new ArrayList<>();
        this.buggyReleases = new ArrayList<>();
        this.oldPath = relpath;
        this.oldName = name;
        /* test */
        this.addedCommits = new ArrayList<>();
        this.renamedCommits = new ArrayList<>();
        this.deletedCommits = new ArrayList<>();
        this.modifiedCommits = new ArrayList<>();
    }

    public List<Commit> getAddedCommits() {
        return addedCommits;
    }

    public void addAddedCommits(Commit commit) {
        this.addedCommits.add(commit);
    }

    public List<Commit> getModifiedCommits() {
        return modifiedCommits;
    }

    public void addModifiedCommits(Commit commit) {
        this.modifiedCommits.add(commit);
    }

    public List<Commit> getDeletedCommits() {
        return deletedCommits;
    }

    public void addDeletedCommits(Commit commit) {
        this.deletedCommits.add(commit);
    }

    public List<Commit> getRenamedCommits() {
        return renamedCommits;
    }

    public void addRenamedCommits(Commit commit) {
        this.renamedCommits.add(commit);
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

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public List<Release> getReleases() {
        return releases;
    }

    public void setReleases(List<Release> releases) {
        this.releases = releases;
    }

    public void locateInRelease(LocalDateTime commitDate) {
        /***
         * checks for all the project releases if the file is present in the release or not. If the file in present in
         * the release, the release is added to the list of releases in which the file is present.
         */
        if (InspectionController.fulldebug) System.out.println("Filename (locate in release): "+this.getRelPath()+this.getName());
        /* it is necessary to use all the available releases retrieved previously! */
        for (Release release: project.getVersions()) {
            if (InspectionController.fulldebug) {
                System.out.println("commit date: "+commitDate);
                System.out.println("release date: "+release.getDate());
                System.out.println("commit < release: "+commitDate.compareTo(release.getDate()));
            }
            /* the file is present in the release if there is a commit that touched the file before release date */
            if (commitDate.compareTo(release.getDate()) < 0) {
                if (!getReleases().contains(release)) getReleases().add(release);
                if (InspectionController.fulldebug) System.out.println("Release added index: "+release.getIndex());
            }
        }
    }

    public static JFile getSpecificJFile(List<JFile> files, String filename) {
        /***
         * get a specific java file from the list of files whose filepath corresponds to the filepath given in input
         * param files: the list of java files
         * param filepath: the filepath to retrieve
         */
        for (JFile file: files) {
            /* check if file has an old path */
            if (filename.equals(file.getOldPath())) {
                return file;
            }

            /* check by filepath */
            if (filename.equals(file.getRelPath())) {
                return file;
            }

            if (InspectionController.fulldebug) {
                System.out.println("Get specific file -filename-: "+file.getName());
                System.out.println("Get specific file -relative path-: "+file.getRelPath());
                System.out.println("Get specific file -complete name-: "+file.getRelPath());
            }
        }
        return null;
    }

    public static JFile getSpecificJFileFromName(List<JFile> files, String name) {
        /***
         * get a specific java file from the list of files whose filepath corresponds to the filename given in input
         * param files: the list of java files
         * param filepath: the filename to retrieve
         */
        for (JFile file: files) {
            if (InspectionController.fulldebug) {
                System.out.println("Get specific file -filename-: "+file.getName());
                System.out.println("Get specific file -relative path-: "+file.getRelPath());
                System.out.println("Get specific file -complete name-: "+file.getRelPath());
            }
            String filename = file.getName();
            if (name.equals(filename)) {
                return file;
            }
        }
        return null;
    }

    public static String getJFileNameFromLine(String line) {
        String tok = "";
        StringTokenizer tokenizer = new StringTokenizer(line, "/");
        while (tokenizer.hasMoreTokens()) {
            String nexttok = tokenizer.nextToken();
            if (nexttok.contains(".java")) return nexttok;
        }

        return tok;
    }

    public static String getFilepathFromLine(String line) {
        StringBuilder builder = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(line, "/");
        if (line.contains("+++")) tokenizer.nextToken();

        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            builder.append(tok);
            if (tokenizer.countTokens() != 0) builder.append("/");
        }

        System.out.println(builder.toString());
        return builder.toString();
    }

    public static String getFilenameFromFilepath(String completeName) {
        StringTokenizer tokenizer = new StringTokenizer(completeName, "/");
        String tok = "";
        while (tokenizer.hasMoreTokens()) {
            tok = tokenizer.nextToken();
            if (tok.contains(".")) break; // end of the file path (encountered filename with extension)
        }
        return tok;
    }

    public static String getPathFromFilepath(String completeName) {
        StringTokenizer tokenizer = new StringTokenizer(completeName, "/");
        StringBuilder builder = new StringBuilder();
        String tok = "";
        while (tokenizer.hasMoreTokens()) {
            tok = tokenizer.nextToken();
            builder.append(tok);
            if (tokenizer.countTokens() != 0) builder.append("/");
            if (tok.contains(".")) break; // end of the file path (encountered filename with extension)
        }
        System.out.println(builder.toString());
        return builder.toString();
    }

    public String getRelPath() {
        return relpath;
    }

    public void setRelPath(String relpath) {
        this.relpath = relpath;
    }

    public Integer getAdditions() {
        return additions;
    }

    public void setAdditions(Integer additions) {
        this.additions = additions;
    }

    public Integer getChanges() {
        return changes;
    }

    public void setChanges(Integer changes) {
        this.changes = changes;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getDeletions() {
        return deletions;
    }

    public void setDeletions(Integer deletions) {
        this.deletions = deletions;
    }

    public List<Release> getBuggyReleases() {
        return buggyReleases;
    }

    public void addBuggyReleases(List<Release> buggyReleases) {
        /***
         * adds a new release to the list of releases in which the file is buggy
         */
        for (Release release: buggyReleases) {
            /* add the release if not already in the list */
            if (!this.getBuggyReleases().contains(release)) {
                this.getBuggyReleases().add(release);
            }
        }
    }

    public void addRelease(Release release) {
        /***
         * adds a new release to the list of releases in which the file is present
         */
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
package org.example.logic.model.keyabstractions;

import javax.print.attribute.standard.MediaSize;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Commit {

    private String shaId;
    private String message;
    private LocalDateTime date;
    private String author;
    private Release version;
    private List<JFile> committedFiles;
    private List<JFile> addedFiles;
    private List<JFile> deletedFiles;
    private List<JFile> modifiedFiles;
    private HashMap<String, List<String>> renamedFiles;

    public Commit(String sha, String mess, LocalDateTime date, String author) {
        this.shaId = sha;
        this.message = mess;
        this.date = date;
        this.author = author;
        this.committedFiles = new ArrayList<>();
        this.addedFiles = new ArrayList<>();
        this.deletedFiles = new ArrayList<>();
        this.modifiedFiles = new ArrayList<>();
        this.renamedFiles = new HashMap<>();
    }

    public static void addCommit(List<Commit> commits, Commit commit) {
        /***
         * add a commit in the list of commits
         */
        for (Commit c: commits) {
            if (c.getShaId().equals(commit.getShaId())) return;
            commits.add(commit);
        }

    }

    public String getShaId() {
        return shaId;
    }

    public void setShaId(String shaId) {
        this.shaId = shaId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public List<JFile> getCommittedFiles() {
        return committedFiles;
    }

    public void setCommittedFiles(List<JFile> committedFiles) {
        this.committedFiles = committedFiles;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Release getVersion() {
        return version;
    }

    public void setVersion(Release version) {
        this.version = version;
    }

    public void addAddedFiles(JFile file) {
        /***
         * Adds a file to the list of file added by the commit
         */
        if (this.addedFiles.contains(file)) return;
        this.addedFiles.add(file);
    }

    public void addModifiedFiles(JFile file) {
        /***
         * Adds a file to the list of files modified by the commit
         */
        if (this.modifiedFiles.contains(file)) return;
        this.modifiedFiles.add(file);
    }

    public void addRenamedFiles(JFile file) {
        /***
         * Adds a file to the list of files renamed by the commit
         */
        if (this.renamedFiles.containsKey(file.getRelPath())) {
            List<String> oldNames = this.renamedFiles.get(file.getRelPath());
            oldNames.add(file.getOldPath());
            this.renamedFiles.put(file.getRelPath(), oldNames);
        } else {
            List<String> oldNames = new ArrayList<>();
            oldNames.add(file.getOldPath());
            this.renamedFiles.put(file.getRelPath(), oldNames);
        }
    }

    public void addDeletedFiles(JFile file) {
        /***
         * Adds a file to the list of files deleted by the commit
         */
        if (this.deletedFiles.contains(file)) return;
        this.deletedFiles.add(file);
    }

    public boolean checkInAddedFiles(JFile file) {
        /***
         * Checks if a file is added in the commit
         */
        boolean ret = false;
        if (this.addedFiles.contains(file)) ret = true;
        return ret;
    }

    public boolean checkInModifiedFiles(JFile file) {
        /***
         * Checks if a file is modified in the commit
         */
        boolean ret = false;
        if (this.modifiedFiles.contains(file)) ret = true;
        return ret;
    }

    public boolean checkInDeletedFiles(JFile file) {
        /***
         * Check if a file is deleted in a commit
         */
        boolean ret = false;
        if (this.deletedFiles.contains(file)) ret = true;
        return ret;
    }

    public boolean checkInRenamedFiles(JFile file) {
        /***
         * Check if a file is renamed in a commit
         */
        boolean ret = false;
        if (this.renamedFiles.containsKey(file.getRelPath())) ret = true;
        return ret;
    }

//    public static Commit getCommitForSha(String sha, Project project) {
//        String log;
//        for (Commit c: project.getCommits()) {
//            if (c.getShaId().equals(sha)) return c;
//        }
//
//        for (Commit c: project.getRefCommits()) {
//            if (c.getShaId().equals(sha)) return c;
//        }
//        log = "No commit for sha: "+sha;
//        Logger.getGlobal().log(Level.WARNING, log);
//        return null;
//    }



}
package org.example.logic.model.keyabstractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Commit {

    private String shaId;
    private String message;
    private LocalDateTime date;
    private String author;
    private Release version;
    private List<JFile> committedFiles;
    private Integer filecount; //for debug

    public Commit(String sha, String mess, LocalDateTime date, String author) {
        this.shaId = sha;
        this.message = mess;
        this.date = date;
        this.author = author;
        this.filecount = 0; // for debug
        this.committedFiles = new ArrayList<>();
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

    public static Commit getCommitForSha(String sha, Project project) {
        String log;
        for (Commit c: project.getCommits()) {
            if (c.getShaId().equals(sha)) return c;
        }

        for (Commit c: project.getRefCommits()) {
            if (c.getShaId().equals(sha)) return c;
        }
        log = "No commit for sha: "+sha;
        Logger.getGlobal().log(Level.WARNING, log);
        return null;
    }

    public JFile getCommittedFileByPath(String relPath) {
        String filename = null;
        JFile file = null;
        StringTokenizer st = new StringTokenizer(relPath, "/");
        if (st.countTokens() != 0) {
            while (st.hasMoreTokens()) {
                filename = st.nextToken();
            }
            file = JFile.getSpecificFile(this.committedFiles, filename);
        }

        return file;
    }

    public Integer getFilecount() {
        return filecount;
    }

    public void setFilecount(Integer filecount) {
        this.filecount = filecount;
    }
}
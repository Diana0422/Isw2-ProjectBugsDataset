package org.example.logic.model.keyabstractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Commit {

    private final String shaId;
    private String message;
    private LocalDateTime date;
    private String author;
    private Release version;
    private List<JFile> committedFiles;

    public Commit() {
        this.shaId = "head";
    }

    public Commit(String sha, String mess, LocalDateTime date, String author) {
        this.shaId = sha;
        this.message = mess;
        this.date = date;
        this.author = author;
        this.committedFiles = new ArrayList<>();
    }

    public String getShaId() {
        return shaId;
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

    public String getAuthor() {
        return author;
    }

    public Release getVersion() {
        return version;
    }

    public void setVersion(Release version) {
        this.version = version;
    }

    public void addTouchedFile(JFile newFile) {
        if (!this.committedFiles.contains(newFile)) this.committedFiles.add(newFile);
    }
}
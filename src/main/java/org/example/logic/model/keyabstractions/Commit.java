package org.example.logic.model.keyabstractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Commit {

    private Project project;
    private final String shaId;
    private String message;
    private String ticketTag;
    private LocalDateTime date;
    private String author;
    private Release version;
    private List<JFile> committedFiles;

    public Commit() {
        this.shaId = "head";
    }

    public Commit(Project project, String sha, String mess, LocalDateTime date, String author) {
        this.project = project;
        this.shaId = sha;
        this.message = mess;
        this.date = date;
        this.author = author;
        this.committedFiles = new ArrayList<>();
        this.ticketTag = getTicketReference(message);
    }

    /**
     * Lookup for a ticket reference in the commit message
     * @param message commit message
     * @return ticket tag extracted
     */
    private String getTicketReference(String message) {
        String proj = project.getProjName();
        String m = message.substring(4);
        String[] list = m.split(" ");
        return Arrays.stream(list).parallel()
                .filter(s -> s.contains(proj + "-"))
                .collect(Collectors.joining());
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

    public String getTicketTag() {
        return ticketTag;
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
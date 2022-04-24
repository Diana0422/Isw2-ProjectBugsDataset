package org.example.logic.model.keyabstractions;

import org.example.logic.control.InspectionController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JFile {

    private Project project;
    private String name;
    private String dirpath;
    private String relpath;
    private LocalDateTime lastCommitDate;
    private Integer additions;
    private Integer deletions;
    private Integer changes;
    private Integer size;
    private List<Release> releases;

    public JFile(Project project, String name, String dirpath) {
        this.name = name;
        this.project = project;
        this.dirpath = dirpath;
        this.additions = 0;
        this.deletions = 0;
        this.changes = 0;
        this.size = 0;
        this.relpath = findRelPath(dirpath);
        this.lastCommitDate = null;
        this.releases = new ArrayList<>();
    }

    public JFile(Project project, String name, String dirpath, LocalDateTime lastCommitDate) {
        this.name = name;
        this.project = project;
        this.dirpath = dirpath;
        this.relpath = findRelPath(dirpath);
        this.lastCommitDate = lastCommitDate;
        this.additions = 0;
        this.deletions = 0;
        this.changes = 0;
        this.size = 0;
        this.releases = new ArrayList<>();
    }

    private String findRelPath(String path) {
        StringTokenizer tokenizer = new StringTokenizer(path, "\\");
        while (tokenizer.hasMoreTokens()) {
            String nextdir = tokenizer.nextToken();
            if (nextdir.equalsIgnoreCase(project.getProjName())) break;
        }

        StringBuilder sb = new StringBuilder("");
        while (tokenizer.hasMoreTokens()) {
            sb.append(tokenizer.nextToken());
            sb.append("/");
        }

        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirPath() {
        return dirpath;
    }

    public void setDirPath(String path) {
        this.dirpath = path;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public LocalDateTime getLastCommitDate() {
        return lastCommitDate;
    }

    public void setLastCommitDate(LocalDateTime lastDate) {
        this.lastCommitDate = lastDate;
    }

    public List<Release> getReleases() {
        return releases;
    }

    public void locateInRelease(LocalDateTime commitDate) {
        /***
         * checks for all the project releases if the file is present in the release or not. If the file in present in
         * the release, the release is added to the list of releases in which the file is present.
         */
        /* it is necessary to use all the available releases retrieved previously! */
        for (Release release: this.project.getVersions()) {
            /* the file is present in the release if there is a commit that touched the file before release date */
            if (commitDate.compareTo(release.getDate()) < 0) {
                this.releases.add(release);
            }
        }
    }

    public static JFile getJFileFromName(String name, Project project) {
        for (JFile file: project.getJavaFiles()) {
            if (file.getName().contains(name)) return file;
        }
        String msg = "No file with name "+name;
        Logger.getGlobal().log(Level.WARNING, msg);
        return null;
    }

    public static JFile getSpecificFile(List<JFile> files, String filename) {
        if (InspectionController.verbose) System.out.println(files);
        for (JFile file: files) {
            if (InspectionController.verbose) System.out.println(file);
            if (filename.equals(file.getName())) {
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
        tokenizer.nextToken();

        while (tokenizer.hasMoreTokens()) {
            builder.append(tokenizer.nextToken());
            if (tokenizer.countTokens() != 0) builder.append("/");
        }

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
}
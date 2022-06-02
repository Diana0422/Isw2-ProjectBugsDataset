package org.example.logic.model.keyabstractions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.example.logic.control.InspectionController;
import org.example.logic.model.utils.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Project {

    private final String projName;
    private String projDir;
    private boolean external; //the project is an external project and needed only to calculate average P for Cold Start Proportion
    private List<JFile> javaFiles;
    private final List<Release> versions;
    private final List<Release> viewedVersions;
    private final int viewedPercentage;
    private List<Issue> bugIssues;
    private List<Commit> commits;
    private List<Commit> refCommits;
    private List<HashMap<JFile, JFile>> renames;
    private List<HashMap<String, JFile>> files;

    public Project(String name, int percent, String projpath, Properties prop) {
        /* main project */
        this.projName = name;
        this.versions = new ArrayList<>();
        this.viewedVersions = new ArrayList<>();
        this.viewedPercentage = percent;
        this.projDir = projpath;
        this.bugIssues = new ArrayList<>();
        this.commits = new ArrayList<>();
        this.refCommits = new ArrayList<>();
        this.javaFiles = new ArrayList<>();
        this.external = false;
        this.renames = new ArrayList<>();
        this.files = new ArrayList<>();

        setVersions(prop);
        setViewedVersions();

        for (int i=0; i<this.versions.size(); i++) {
            this.renames.add(new HashMap<>());
            this.files.add(new HashMap<>());
        }
    }

    public Project(String name, Properties prop) {
        /* external project */
        this.projName = name;
        this.bugIssues = new ArrayList<>();
        this.versions = new ArrayList<>();
        this.viewedVersions = new ArrayList<>();
        this.viewedPercentage = 100;
        this.external = true;
        setVersions(prop);
        setViewedVersions();
    }

    private void setVersions(Properties prop) {
        //Fills the arraylist with releases dates and orders them
        //Ignores releases with missing dates
        int i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json;

        int skipped = 0;

        try (FileOutputStream fileOutputStream = new FileOutputStream(
                Objects.requireNonNull(
                        Project.class.getClassLoader().getResource("config.properties"))
                        .getPath()))
        {
            json = Parser.getInstance().readJsonFromUrl(url);
            JSONArray releases = json.getJSONArray("versions");
            for (i = 0; i < releases.length(); i++ ) {
                String relName = "";
                String relId = "";

                /* Select only the releases with a date */
                if(releases.getJSONObject(i).has("releaseDate")) {
                    /* Select only the releases with a name */
                    if (releases.getJSONObject(i).has("name")) {
                        relName = releases.getJSONObject(i).get("name").toString();
                    } else {
                        skipped++;
                    }
                    /* Select only the releases with a id */
                    if (releases.getJSONObject(i).has("id")) {
                        relId = releases.getJSONObject(i).get("id").toString();
                    } else {
                        skipped++;
                    }
                    /* Add release to the list of project releases */
                    addRelease(releases.getJSONObject(i).get("releaseDate").toString(),relName,relId);
                } else {
                    skipped++;
                }
            }
            if (InspectionController.FULL_DEBUG) {
                String log = "Skipped versions: "+skipped;
                Logger.getGlobal().log(Level.WARNING, log);
            }
            /* order releases by date */
            versions.sort(Comparator.comparing(Release::getDate));

            /* set index numbers to the releases */
            Release.setIndexNumbers(versions);

            // store on properties to commit changes
            prop.store(fileOutputStream, "");
        } catch (JSONException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setViewedVersions() {
        int total = this.versions.size();
        int partial = (int) ((this.getViewedPercentage()/100.0)*total);
        /* select some versions as "viewed" (default first 50% of the releases) */
        for (Release r: this.versions) {
            if (partial != 0) {
                this.viewedVersions.add(r);
                partial--;
            }
        }
        if (InspectionController.FULL_DEBUG) {
            String log = "Partial num of releases (" + getViewedPercentage() + "%): " + getViewedVersions().size();
            Logger.getGlobal().log(Level.WARNING, log);
        }
    }

    private void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        Release release = new Release(id, name, dateTime);

        /* set the index of the release counting the number of already explored releases */
        this.versions.add(release);
    }

    public void setBugIssues(List<Issue> issues) {
        this.bugIssues = issues;
    }

    public String getProjName() {
        return projName;
    }

    public int getViewedPercentage() {
        return viewedPercentage;
    }

    public List<Release> getVersions() {
        return versions;
    }

    public List<Release> getViewedVersions() {
        return viewedVersions;
    }

    public String getProjDir() {
        return projDir;
    }

    public List<Issue> getBugIssues() {
        return bugIssues;
    }

    public List<Commit> getCommits() {
        return commits;
    }

    public void setCommits(List<Commit> commits) {
        this.commits = commits;
    }

    public List<JFile> getJavaFiles() {
        return javaFiles;
    }

    public void setJavaFiles(List<JFile> javaFiles) {
        this.javaFiles = javaFiles;
    }

    public List<Commit> getRefCommits() {
        return refCommits;
    }

    public void setRefCommits(List<Commit> refCommits) {
        this.refCommits = refCommits;
    }
    public void addRenomination(int releaseIdx, JFile oldFile, JFile newFile) {
        int hashIdx = releaseIdx-1;
        HashMap<JFile, JFile> renomination = this.renames.get(hashIdx);
        renomination.put(newFile, oldFile);
    }

    public boolean checkFile(Integer releaseIdx, String filepath) {
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        return relFiles.containsKey(filepath);
    }

    public void addFile(Integer releaseIdx, JFile file) {
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        if (relFiles.containsKey(file.getRelPath())) return;
        relFiles.put(file.getRelPath(), file);
    }

    public JFile getFile(Commit commit, String filename, String filepath) {
        int releaseIdx = commit.getVersion().getIndex();
        LocalDateTime creation = commit.getDate();
        if (checkFile(releaseIdx, filepath)) {
            /* the file is already created - take the existing instance */
            HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
            return relFiles.get(filepath);
        } else {
            /* the file is new - create a new instance */
            JFile file = new JFile(this, filename, filepath, creation);
            addFile(releaseIdx, file);
            return file;
        }
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }
}
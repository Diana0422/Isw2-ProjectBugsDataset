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
    private final List<Release> versions;
    private final List<Release> viewedVersions;
    private final int viewedPercentage;
    private List<Issue> bugIssues;
    private List<Commit> commits;
    private List<Commit> refCommits;
    private List<HashMap<JFile, JFile>> renames;
    private List<HashMap<String, JFile>> files;
    private HashMap<String, List<Integer>> filesReleases;

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
        this.external = false;
        this.renames = new ArrayList<>();
        this.files = new ArrayList<>();

        setVersions(prop);
        setViewedVersions();

        for (int i=0; i<this.versions.size(); i++) {
            this.renames.add(new HashMap<>());
            this.files.add(new HashMap<>());
        }

        this.filesReleases = new HashMap<>();
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
                    }
                    /* Select only the releases with a id */
                    if (releases.getJSONObject(i).has("id")) {
                        relId = releases.getJSONObject(i).get("id").toString();
                    }

                    /* Select only RELEASED and NOT ARCHIVED */
                    boolean released = Boolean.parseBoolean(releases.getJSONObject(i).get("released").toString());
                    boolean archived = Boolean.parseBoolean(releases.getJSONObject(i).get("archived").toString());
                    if (released && !archived) {
                        /* Add release to the list of project releases */
                        addRelease(releases.getJSONObject(i).get("releaseDate").toString(),relName,relId);
                    }
                }
            }
            /* order releases by date */
            versions.sort(Comparator.comparing(Release::getDate));

            /* set index numbers to the releases */
            Release.setIndexNumbers(versions);

            // store on properties to commit changes
            prop.store(fileOutputStream, "");
        } catch (JSONException | IOException e) {
            Logger.getGlobal().log(Level.SEVERE, e.getMessage());
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
        if (InspectionController.isFullDebug()) {
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

    public List<Commit> getRefCommits() {
        return refCommits;
    }

    public void setRefCommits(List<Commit> refCommits) {
        this.refCommits = refCommits;
    }

    public boolean checkFile(Integer releaseIdx, String filepath) {
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        return relFiles.containsKey(filepath);
    }

    public void addFile(Integer releaseIdx, JFile file) {
        /* Add the file to the release specified - current */
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        if (relFiles.containsKey(file.getRelPath())) return;
        relFiles.put(file.getRelPath(), file);

        /* Add release to list of release indexes of file */
        addFileRelease(file, releaseIdx);
    }

    public void removeFile(Integer releaseIdx, JFile file) {
        /* Remove the file from the release specified - current */
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        relFiles.remove(file.getRelPath());
        /* remove releases from release */
        file.getReleases().remove(Release.findVersionByIndex(versions,releaseIdx));
    }

    /**
     * @param commit
     * @param filename
     * @param filepath
     * @return
     */
    public JFile getFile(Commit commit, String filename, String filepath) {
        int releaseIdx = commit.getVersion().getIndex();
        Release creation = commit.getVersion();

        if (checkFile(releaseIdx, filepath)) {
            /* the file is already created in the current release - take the existing instance */
            HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
            return relFiles.get(filepath);
        }

        for (int index = releaseIdx-1; index >= 1; index--) {
            if (checkFile(index, filepath)) {
                /* the file was already created in a previous release - take the existing instance */
                HashMap<String, JFile> relFiles = files.get(index-1);
                JFile prevInstance = relFiles.get(filepath);
                JFile file = new JFile(prevInstance);
                addFile(releaseIdx, file);

                /* replicate file in intermediate releases */
                replicateMissingFile(index, releaseIdx, prevInstance);
                return file;
            }
        }

        /* the file is new - create a new instance */
        JFile file = new JFile(this, filename, filepath, creation);
        addFile(releaseIdx, file);
        return file;
    }

    /**
     * @param startingIndex
     * @param currentReleaseIdx
     * @param prevInstance
     */
    public void replicateMissingFile(int startingIndex, int currentReleaseIdx, JFile prevInstance) {
        String filepath = prevInstance.getRelPath();
        /* replicate file in intermediate releases */
        for (int i = startingIndex; i < currentReleaseIdx; i++) {
            if (!checkFile(i, filepath)) {
                JFile file = new JFile(i, prevInstance);
                addFile(i, file);
            }
        }
    }

    /**
     * @param releaseIdx
     * @param prevInstance
     */
    public void replicateRelease(int releaseIdx, JFile prevInstance) {
        Release versionByIndex = Release.findVersionByIndex(versions, releaseIdx);
        prevInstance.addRelease(versionByIndex);
    }

    private void addFileRelease(JFile file, int releaseIdx) {
        String filepath = file.getRelPath();
        if (filesReleases.containsKey(filepath)) {
            List<Integer> fileRels = filesReleases.get(filepath);
            fileRels.add(releaseIdx);
            filesReleases.put(filepath, fileRels);
            if (fileRels.size() > 1) {
                int currRel = fileRels.get(fileRels.size()-1);
                int prevRel = fileRels.get(fileRels.size()-2);

                /* Replicate last file for all the previous missing releases */
                if (checkGap(currRel, prevRel)) replicatePrevFile(currRel, prevRel, filepath);
            }
        } else {
            filesReleases.put(filepath, new ArrayList<>());
        }
    }

    public List<HashMap<String, JFile>> getFiles() {
        return files;
    }

    private boolean checkGap(int currRel, int prevRel) {
        return currRel-prevRel == 1;
    }

    private void replicatePrevFile(int currRel, int prevRel, String filepath) {
        HashMap<String, JFile> fileList = files.get(prevRel - 1);
        JFile file = fileList.get(filepath);
        for (int i = prevRel+1; i < currRel-1; i++) {
            addFile(i, file);
        }
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }
}
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

/**
 * Class that represent a Project to analyse
 */
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

    /**
     * Main project analysed
     * @param name project name
     * @param percent the percent of the dataset to consider at the end
     * @param projpath the path to the local project repository
     * @param prop properties
     */
    public Project(String name, int percent, String projpath, Properties prop) {
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

    /**
     * External project
     * @param name project name
     * @param prop properties
     */
    public Project(String name, Properties prop) {
        this.projName = name;
        this.bugIssues = new ArrayList<>();
        this.versions = new ArrayList<>();
        this.viewedVersions = new ArrayList<>();
        this.viewedPercentage = 100;
        this.external = true;
        setVersions(prop);
        setViewedVersions();
    }

    /**
     * Sets project releases retrieved from Jira
     * @param prop properties
     */
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

    /**
     * Sets versions to consider (these can be used only at the end for verification)
     */
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

    /**
     * Add a release to the list of project releases
     * @param strDate date string
     * @param name release name
     * @param id the release id
     */
    private void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        Release release = new Release(id, name, dateTime);

        /* set the index of the release counting the number of already explored releases */
        this.versions.add(release);
    }

    /**
     * Check if a file is present in a certain release
     * @param releaseIdx the release index
     * @param filepath the complete filepath in the project
     * @return boolean
     */
    public boolean checkFile(Integer releaseIdx, String filepath) {
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        return relFiles.containsKey(filepath);
    }

    /**
     * Adds file to a certain release
     * @param releaseIdx the release index
     * @param file the file to add
     */
    public void addFile(Integer releaseIdx, JFile file) {
        /* Add the file to the release specified - current */
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        if (relFiles.containsKey(file.getRelPath())) return;
        relFiles.put(file.getRelPath(), file);

        /* Add release to list of release indexes of file */
        addFileRelease(file, releaseIdx);
    }

    /**
     * Removes a file from a certain release
     * @param releaseIdx the release index
     * @param file the file to remove
     */
    public void removeFile(Integer releaseIdx, JFile file) {
        /* Remove the file from the release specified - current */
        HashMap<String, JFile> relFiles = files.get(releaseIdx - 1);
        relFiles.remove(file.getRelPath());
        /* remove releases from release */
        file.getReleases().remove(Release.findVersionByIndex(versions,releaseIdx));
    }

    /**
     * Take a file from a certain release. If the file is not present in the current release, check if the file was
     * created in a previous release, otherwise creates a new file instance.
     * @param commit the commit (belongs to a release)
     * @param filename the name of the file
     * @param filepath the complete filepath
     * @return a file instance
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
                JFile file = new JFile(index + 1, prevInstance);
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
     * Replicates a previous file instance if not present in the releases interval selected
     * @param startingIndex the index of the start release
     * @param currentReleaseIdx the index of the current release
     * @param prevInstance the file instance to replicate
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
     * Replicate release if not present in the list of releases of a file instance
     * @param releaseIdx the release index
     * @param prevInstance the file instance
     */
    public void replicateRelease(int releaseIdx, JFile prevInstance) {
        Release versionByIndex = Release.findVersionByIndex(versions, releaseIdx);
        prevInstance.addRelease(versionByIndex);
        prevInstance.getReleases().sort(Comparator.comparingInt(Release::getIndex));
    }

    /**
     * Replicate file content from a previous release if the file was not present in the current release
     * @param releaseIdx the release index
     * @param file the file to modify
     */
    public void replicateContent(int releaseIdx, JFile file) {
        int created = file.getCreated().getIndex();
        for (int i = created-1; i < releaseIdx; i++) {
            if (created == 1) continue;
            String[] currentContent = file.getContent().get(i);
            String[] prevContent = file.getContent().get(i - 1);
            if (currentContent.length == 0) {
                file.getContent().put(i, prevContent);
            }
        }

    }

    /**
     * Adds a file in a release and replicate the file for all the missing releases if it should be present instead.
     * @param file the file considered
     * @param releaseIdx the release index
     */
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

    private void replicatePrevFile(int currRel, int prevRel, String filepath) {
        HashMap<String, JFile> fileList = files.get(prevRel - 1);
        JFile file = fileList.get(filepath);
        for (int i = prevRel+1; i < currRel-1; i++) {
            addFile(i, file);
        }
    }

    /* GETTERS AND SETTERS */
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

    public List<HashMap<String, JFile>> getFiles() {
        return files;
    }

    private boolean checkGap(int currRel, int prevRel) {
        return currRel-prevRel == 1;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }
}
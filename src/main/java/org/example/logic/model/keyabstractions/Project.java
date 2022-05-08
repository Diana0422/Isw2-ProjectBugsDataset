package org.example.logic.model.keyabstractions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import org.example.logic.control.InspectionController;
import org.example.logic.model.utils.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Project {

    private String projName;
    private String projDir;
    private boolean external; //the project is an external project and needed only to calculate average P for Cold Start Proportion
    private List<JFile> javaFiles;
    private final List<Release> versions;
    private final List<Release> viewedVersions;
    private Release lastViewedVersion;
    private int viewedPercentage;
    private List<Issue> bugIssues;
    private List<Commit> commits;
    private List<Commit> refCommits;
    private List<HashMap<JFile, JFile>> renames;
    private HashMap<Integer, List<JFile>> deletions;

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
        this.deletions = new HashMap<>();

        setVersions(prop);
        setViewedVersions();

        for (int i=0; i<this.versions.size(); i++) {
            this.renames.add(new HashMap<>());
        }

        for (int i=1; i<=this.versions.size(); i++) {
            this.deletions.put(i, new ArrayList<>());
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
        Integer i;
        String url = "https://issues.apache.org/jira/rest/api/2/project/" + projName;
        JSONObject json;

        int skipped = 0;

        try (FileOutputStream fileOutputStream = new FileOutputStream(Project.class.getClassLoader().getResource("config.properties").getPath())) {
            json = Parser.getInstance().readJsonFromUrl(url);
            JSONArray releases = json.getJSONArray("versions");
            System.out.println("Num. releases:"+releases.length());
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
            if (InspectionController.fulldebug) System.out.println("Skipped versions: "+skipped);
            /* order releases by date */
            versions.sort(Comparator.comparing(Release::getDate));

            /* set index numbers to the releases */
            for (i = 0; i< this.versions.size(); i++) {
                Release rel = this.versions.get(i);
                rel.setIndex(i+1);
                System.out.println("Index set n°"+i+": "+rel.getIndex());
                System.out.println("Release name: "+rel.getName());
                if (InspectionController.fulldebug) System.out.println(rel.getName()+" - "+rel.getIndex()+" - "+rel.getDate());
            }

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
        if (InspectionController.fulldebug) System.out.println("Partial num of releases ("+Integer.toString(getViewedPercentage())+"%): "+getViewedVersions().size());

        // pointer to the last viewed version
        setLastViewedVersion(getViewedVersions().get(getViewedVersions().size()-1));
    }

    private void addRelease(String strDate, String name, String id) {
        LocalDate date = LocalDate.parse(strDate);
        LocalDateTime dateTime = date.atStartOfDay();
        Release release = new Release(id, name, dateTime);

        /* set the index of the release counting the number of already explored releases */
        this.versions.add(release);
    }

    public void setBugIssues(List<Issue> issues) throws IOException {
        this.bugIssues = issues;
    }

    public String getProjName() {
        return projName;
    }

    public void setProjName(String projName) {
        this.projName = projName;
    }

    public int getViewedPercentage() {
        return viewedPercentage;
    }

    public void setViewedPercentage(int viewedPercentage) {
        this.viewedPercentage = viewedPercentage;
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

    public void setProjDir(String projDir) {
        this.projDir = projDir;
    }

    public List<Issue> getBugIssues() {
        return bugIssues;
    }

    public Release getLastViewedVersion() {
        return lastViewedVersion;
    }

    public void setLastViewedVersion(Release lastViewedVersion) {
        this.lastViewedVersion = lastViewedVersion;
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

    public void addJavaFile(JFile javaFile) {
        /***
         * adds a new java file to the list of java files
         */
        for (JFile file: getJavaFiles()) {
            if (file.getRelPath().equals(javaFile.getRelPath())) return;
        }
        getJavaFiles().add(javaFile);
    }

    public void addRenomination(int releaseIdx, JFile oldFile, JFile newFile) {
        int hashIdx = releaseIdx-1;
        HashMap<JFile, JFile> renomination = this.renames.get(hashIdx);
        renomination.put(oldFile, newFile);
    }

    public boolean checkRenomination(int releaseIdx, JFile file) {
        int hashIdx = releaseIdx-1;
        boolean ret = false;
        HashMap<JFile, JFile> renomination = this.renames.get(hashIdx);
        if (renomination.containsKey(file)) ret = true;
        return ret;
    }

    public JFile getRenomination(int releaseIdx, JFile file) {
        int hashIdx = releaseIdx-1;
        HashMap<JFile, JFile> renomination = this.renames.get(hashIdx);
        return renomination.get(file);
    }

    public boolean checkDeletions(int releaseIdx, JFile file) {
        boolean ret = false;
        List<JFile> deletedFiles = this.deletions.get(releaseIdx);
        if (deletedFiles.contains(file)) ret = true;
        return ret;
    }

    public JFile getDeletion(int releaseIdx, JFile file) {

        return null;
    }

    public boolean isExternal() {
        return external;
    }

    public void setExternal(boolean external) {
        this.external = external;
    }
}
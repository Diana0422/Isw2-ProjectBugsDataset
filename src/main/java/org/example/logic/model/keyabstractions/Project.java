package org.example.logic.model.keyabstractions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;

import org.example.logic.control.InspectionController;
import org.example.logic.model.handlers.JIRAHandler;
import org.example.logic.model.utils.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Project {

    private String projName;
    private String projDir;
    private List<JFile> javaFiles;
    private final List<Release> versions;
    private final List<Release> viewedVersions;
    private Release lastViewedVersion;
    private int viewedPercentage;
    private List<Issue> bugIssues;
    private List<Commit> commits;
    private List<Commit> refCommits;

    public Project(String name, int percent, String projpath, Properties prop) {
        this.projName = name;
        this.versions = new ArrayList<>();
        this.viewedVersions = new ArrayList<>();
        this.viewedPercentage = percent;
        this.projDir = projpath;
        this.bugIssues = new ArrayList<>();
        this.commits = new ArrayList<>();
        this.refCommits = new ArrayList<>();
        this.javaFiles = new ArrayList<>();

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
            if (InspectionController.verbose) System.out.println("Skipped versions: "+skipped);
            /* order releases by date */
            versions.sort(Comparator.comparing(Release::getDate));

            /* set index numbers to the releases */
            for (i = 0; i< this.versions.size(); i++) {
                Release rel = this.versions.get(i);
                rel.setIndex(i+1);
                if (InspectionController.verbose) System.out.println(rel.getName()+" - "+rel.getIndex());
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
        if (InspectionController.verbose) System.out.println("Partial num of releases ("+Integer.toString(getViewedPercentage())+"%): "+getViewedVersions().size());

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

}
package org.example.logic.model.keyabstractions;

import org.example.logic.control.InspectionController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Issue {
    /***
     * class that represents a Jira Issue
     */

    private Project project; // project of the issue
    private String key; // issue name with number
    private String id; // issue id
    private String number; // issue number (no name)
    private List<Release> affectedVersions; // releases in which the issue is present
    private List<Release> fixedVersions; // releases that fixed the issue
    private Release opVersion;
    private Release fixVersion;
    private Release injVersion;
    private double proportion;
    private LocalDateTime resolution;
    private LocalDateTime created; // date of the creation of the issue (determines the op. version)
    private List<Commit> fixedCommits; // list of commits that fixed the issue
    private List<JFile> touchedFiles; // list of java files touched by the commits that fixed the issue (those files are buggy in the releases specified by the affected versions
    private Commit fixCommit;

    public Issue(Project project, String key, String id, LocalDateTime res, LocalDateTime created) {
        this.project = project;
        this.key = key;
        this.number = key.substring(project.getProjName().length(), key.length()-1);
        this.id = id;
        this.resolution = res;
        this.created = created;
        this.affectedVersions = new ArrayList<>();
        this.fixedVersions = new ArrayList<>();
        this.fixedCommits = new ArrayList<>();
        this.fixCommit = null;
        this.opVersion = Release.findVersionByDate(affectedVersions, created);
        this.fixVersion = Release.findVersionByDate(affectedVersions, created);
        this.injVersion = null;
        this.proportion = 0;
    }

    public void addAffected(Release release) {
        List<Release> affected = this.affectedVersions;

        for (Release av: affected) {
            if (av.getIndex() == release.getIndex()) return;
        }
        this.affectedVersions.add(release);

        Collections.sort(this.affectedVersions, Comparator.comparing(Release::getDate));
    }

    public static void prepareIssue(Issue issue) {
        /***
         * complete the issue information with IV (calculating IV using proportion method if necessary)
         * param issue: the issue to prepare
         */
        /* if there is more than one fixed version, consider the most recent one */
        List<Release> fixed = issue.getFixedVersions();
        if (InspectionController.verbose) System.out.println(fixed);

        while(fixed.size() > 1) {
            if(fixed.get(0).getIndex() >= fixed.get(1).getIndex()) {
                issue.addAffected(fixed.get(1));	//Adds it to the affected versions
                issue.removeFixed(1);
            }
            else {
                issue.addAffected(fixed.get(0));
                issue.removeFixed(0);
            }
        }

        /* get issue's affected versions retrieved from JIRA */
        List<Release> affected = issue.getAffectedVersions();
        if (InspectionController.verbose) System.out.println("after fixed merge: "+affected);
        int fixedIdx = 0;

        /* get fixed version index */
        if (fixed.isEmpty()) {
            if (InspectionController.verbose) System.out.println("fixed is empty.");
            fixedIdx = -1;
        } else {
            if (InspectionController.verbose) System.out.println("fixed is not empty.");
            fixedIdx = fixed.get(0).getIndex();
        }

        /* check if the affected versions are consistent with fixed versions */
        for (int i=0; i<affected.size(); i++) {

            if (affected.get(i).getIndex() > fixedIdx) {
                // affected versions not consistent (affected version > fixed version): discard data
                issue.setAffectedVersions(new ArrayList<>());
                affected = issue.getAffectedVersions(); // affected now is empty
                break;
            }
        }

        /* check if there are no affected versions declared: if so use proportion method */
        if (affected.isEmpty()) {
            applyProportion(issue);
        } else {
            System.out.println("affected versions: ");
            for (Release rel: affected) {
                System.out.println(rel.getIndex()+" - "+rel.getName());
            }
        }

        System.out.println("\n");

    }

    public void calculateProportion(int openV, int fixedV) {
        int numAffected = this.affectedVersions.size();

        if (numAffected == 0) {
            this.proportion = 1;
            return;
        }

        int fixIdx = fixedV;
        int injIdx = this.affectedVersions.get(0).getIndex();
        int opIdx = openV;

        if (fixIdx == opIdx) {
            this.proportion = 1;
            return;
        }

        this.proportion = (double)(fixIdx-injIdx)/(fixIdx-opIdx);
    }

    private static double calculateP(Issue issue, int openV, int fixedV) {
        List<Issue> window = new ArrayList<>();
        List<Issue> prevBugs = new ArrayList<>();
        List<Issue> bugs = issue.project.getBugIssues();

        for (Issue bug: bugs) {
            if (bug.getId().equals(issue.getId())) break;
            prevBugs.add(bug);
        }

        // get last 1% of the fixed bugs
        int total = prevBugs.size();
        int percentage = (int) Math.ceil(total*0.01);

        int start = total - percentage;
        int end = total;

        // put the last 1% of the fixed bugs in the window
        for (int i=start; i<end; i++) {
            window.add(prevBugs.get(i));
        }

        // calculate p as the average of P in last 1% issues fixed
        double avg;
        double sum = 0;

        for (int i=0; i<window.size(); i++) {
            window.get(i).calculateProportion(openV, fixedV);
            sum += window.get(i).getProportion();
        }

        avg = sum/window.size();
//		System.out.println("Proportion for window: "+window+" ="+avg);
        return avg;
    }

    private static void applyProportion(Issue issue) {
        int fixedV;
        int openV;
        int injV;
        double p;
        Project project = issue.project;

        openV = issue.getOpVersion().getIndex();
//    	System.out.println("opening ver: "+openV);

        //If the fixed version isn't found it's set equal to the opening version
        if (issue.getFixedVersions().isEmpty()) {
            fixedV = openV;
        } else {
            System.out.println(issue.getFixedVersions());
            fixedV = issue.getFixedVersions().get(0).getIndex();
        }

//    	System.out.println("fixed ver: "+fixedV);

        //If the opening version is greater the the fixed version they are inverted
        if (openV > fixedV) {
            int temp = fixedV;
            fixedV = openV;
            openV = temp;
        }

        p = calculateP(issue, openV, fixedV);
        injV = (int) (fixedV - (fixedV - openV) * p);

//    	System.out.println("injected ver: "+injV);

        //Checks if the value is negative
        if (injV <= 0) {
            injV = 1;
        }

        //Checks if the value is greater than the opening version
        if (injV > openV) {
            injV = openV;
        }

        //Adds all the new affected versions to the ticket
        for (int i = injV; i <= fixedV; i++) {
            Release ver = project.getVersions().get(i - 1);
            issue.addAffected(ver);
        }

        issue.setFixVersion(Release.findVersionByIndex(project.getVersions(), fixedV));
        issue.setInjVersion(Release.findVersionByIndex(project.getVersions(), injV));

        // update the issue with corresponding p
        issue.setProportion(p);

        System.out.println("calculated affected versions: ");
        for (Release rel : issue.getAffectedVersions()) {
            System.out.println(rel.getIndex() + " - " + rel.getName());
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Release> getAffectedVersions() {
        return affectedVersions;
    }

    public void setAffectedVersions(List<Release> afflictedVersions) {
        this.affectedVersions = afflictedVersions;
    }

    public List<Commit> getFixedCommits() {
        return fixedCommits;
    }

    public void setFixedCommits(List<Commit> fixedCommits) {
        this.fixedCommits = fixedCommits;
    }

    public Commit getFixCommit() {
        return fixCommit;
    }

    public void setFixCommit(Commit fixCommit) {
        this.fixCommit = fixCommit;
    }

    public List<Release> getFixedVersions() {
        return fixedVersions;
    }

    public void setFixedVersions(List<Release> fixedVersions) {
        this.fixedVersions = fixedVersions;
    }

    public void removeFixed(int i) {
        this.fixedVersions.remove(i);
    }

    public void setOpVersion(Release opVersion) {
        this.opVersion = opVersion;
    }

    public void setFixVersion(Release fixVersion) {
        this.fixVersion = fixVersion;
    }

    public Release getFixVersion() {
        return fixVersion;
    }

    public Release getOpVersion() {
        return opVersion;
    }

        public LocalDateTime getResolution() {
        return resolution;
    }

    public void setResolution(LocalDateTime resolution) {
        this.resolution = resolution;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public Release getInjVersion() {
        return injVersion;
    }

    public void setInjVersion(Release injVersion) {
        this.injVersion = injVersion;
    }

    public double getProportion() {
        return proportion;
    }

    public void setProportion(double proportion) {
        this.proportion = proportion;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }
}
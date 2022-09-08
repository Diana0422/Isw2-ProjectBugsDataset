package org.example.logic.model.keyabstractions;

import org.example.logic.control.InspectionController;
import org.example.logic.model.utils.ProjectInspector;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/***
 * class that represents a Jira Issue
 */
public class Issue {

    protected static final String[] names = // names of the other apache projects
            {"AVRO", "BOOKKEEPER",
            "CHUKWA", "CONNECTORS",
            "CRUNCH", "FALCON",
            "IVY", "OPENJPA",
            "PROTON", "SSHD",
            "STORM", "SYNCOPE",
            "TAJO", "TEZ",
            "THRIFT", "TOMEE",
            "WHIRR", "ZEPPELIN",
            "ZOOKEEPER"};

    private final Project project; // project of the issue
    private final String key; // issue name with number
    private String id; // issue id
    private List<Release> affectedVersions; // releases in which the issue is present
    private List<Release> fixedVersions; // releases that fixed the issue
    private Release opVersion;
    private double p;
    private LocalDateTime resolution;
    private LocalDateTime created; // date of the creation of the issue (determines the op. version)
    private List<Commit> fixedCommits; // list of commits that fixed the issue

    public Issue(Project project, String key, String id, LocalDateTime res, LocalDateTime created) {
        this.project = project;
        this.key = key;
        this.id = id;
        this.resolution = res;
        this.created = created;
        this.affectedVersions = new ArrayList<>();
        this.fixedVersions = new ArrayList<>();
        this.fixedCommits = new ArrayList<>();
        this.opVersion = Release.findVersionByDate(affectedVersions, created);
        this.p = 0;
    }

    /**
     * Adds a release to the affected versions (AV).
     * @param release the release to add
     */
    public void addAffected(Release release) {
        List<Release> affected = this.affectedVersions;

        for (Release av: affected) {
            if (av.getIndex() == release.getIndex()) return;
        }
        this.affectedVersions.add(release);
        this.affectedVersions.sort(Comparator.comparing(Release::getDate));
    }

    /***
     * Complete the issue information with IV (calculating IV using proportion method if necessary)
     * @param issue: the issue to prepare
     */
    public static void prepareIssue(Issue issue) {
        /* if there is more than one fixed version, consider the most recent one */
        List<Release> fixed = issue.getFixedVersions();
        while(fixed.size() > 1) {
            /* add the fixed versions to the affected version */
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
        if (InspectionController.isFullDebug()) {
            String log = "after fixed merge: "+affected;
            Logger.getGlobal().log(Level.WARNING, log);
        }
        int fixedIdx;

        /* get fixed version index */
        if (fixed.isEmpty()) {
            if (InspectionController.isFullDebug()) Logger.getGlobal().log(Level.WARNING, "fixed is empty.");
            fixedIdx = -1;
        } else {
            if (InspectionController.isFullDebug()) Logger.getGlobal().log(Level.WARNING, "fixed is not empty.");
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
            /* apply proportion method to find injected version only if the issue belongs to the project analysed */
            applyProportion(issue);
        }
    }

    /***
     * Apply Proportion method to find the Injected Version of an Issue whose Affected Version
     * on JIRA are not consistent or unreliable.
     * @param issue: the issue on which is to be applied Proportion method.
     */
    private static void applyProportion(Issue issue) {
        int fixedV;
        int openV;
        int injV;
        double p;
        Project project = issue.project;
        openV = issue.getOpVersion().getIndex();
        /* If the fixed version isn't found it's set equal to the opening version - at least we know that the issue affects the opening version */
        if (issue.getFixedVersions().isEmpty()) {
            fixedV = openV;
        } else {
            fixedV = issue.getFixedVersions().get(0).getIndex();
        }

        //If the opening version is greater the fixed version they are inverted
        if (openV > fixedV) {
            int temp = fixedV;
            fixedV = openV;
            openV = temp;
        }

        p = calculateProportionMovingWindow(issue, openV, fixedV);
        /* update the issue with corresponding P value */
        issue.setP(p);
        /* calculate the index of the Injected Version */
        injV = (int) (fixedV - (fixedV - openV) * p);

        /* Checks if the value is negative - if so, set injected version at index 1 */
        if (injV <= 0) {
            injV = 1;
        }

        /* Checks if the value is greater than the opening version - if so, set injected version equal as openVersion */
        if (injV > openV) {
            injV = openV;
        }

        /* Add all the new affected versions to the issue */
        for (int i = injV; i <= fixedV; i++) {
            issue.addAffected(Release.findVersionByIndex(project.getVersions(), i));
        }
    }

    /***
     * calculates the proportion factor using the issue opening version and fixed version,
     * considering that IV / FV = IV / FV = P
     * The method used to calculate P is Proportion with Moving Window approach, using the last 1% of bug issues.
     * @param issue: the issue to consider.
     * @param openV: the index of the opening version
     * @param fixedV: the index of the fixed version
     * @return double
     */
    private static double calculateProportionMovingWindow(Issue issue, int openV, int fixedV) {
        List<Issue> window = new ArrayList<>();
        List<Issue> prevBugs = new ArrayList<>();
        List<Issue> bugs = issue.project.getBugIssues();

        /* take all the bugs issues (fixed) that happened before the issue that we are considering */
        for (Issue bug: bugs) {
            if (bug.getId().equals(issue.getId())) break;
            prevBugs.add(bug);
        }

        /* get last 1% of the fixed bugs */
        int numBugs = prevBugs.size();
        int percentage = (int) Math.ceil(numBugs*0.01);

        int start = numBugs - percentage; // start index of the bugs window

        /* put the last 1% of the fixed bugs in the window */
        for (int i = start; i< numBugs; i++) {
            window.add(prevBugs.get(i));
        }

        // calculate p as the average of P in last 1% issues fixed
        double avg;
        double sum = 0;

        /* is no previous P values are present, calculate P from other projects data */
        if (window.isEmpty()) {
            issue.calculatePValue(openV, fixedV);
        } else {
            /* use previous P values of the project to get the value of P for the current issue */
            for (Issue value : window) {
                value.calculatePValue(openV, fixedV);
                sum += value.getP();
            }
        }

        if (window.isEmpty()) {
            avg = 0.0;
        } else {
            avg = sum / window.size();
        }
        return avg;
    }

    /***
     * Use the average P values of the other projects to calculate Proportion value for this project
     * @param openV: index of the opening version
     * @param fixedV: index of the fixed version
     */
    public void calculatePValue(int openV, int fixedV) {
        int numAffected = affectedVersions.size();

        if (numAffected == 0) {
            /* no affected values to work on - take the average P value of the other projects */
            p = InspectionController.getColdProportion(); // this will be 0.0 if the issue belong to a project that is not analysed
        } else {
            /* there are affected versions for the issue */
            int injIdx = affectedVersions.get(0).getIndex();

            /* if OV = FV, then P= (FV-IV)/(OV-IV) = 1 */
            if (fixedV == openV) {
                p = 1;
            } else {
                p = (double) (fixedV - injIdx) / (fixedV - openV);
            }
        }
    }

    /***
     * Calculate the average of the P values among the other 75 apache projects
     * @param mainProjectName: the name of the main project to analyse
     * */
    public static double calculateColdStartProportion(String mainProjectName, Properties prop) {
        double p;
        double sum = 0;
        double avg;
        List<Double> averages = new ArrayList<>();
        double totalAvg;
        double totalSum = 0;

        /* iterate on all projects */
        for (String name : names) {
            if (name.equals(mainProjectName)) continue;
            Project project = new Project(name, prop);
            ProjectInspector inspector = new ProjectInspector(project);
            /* get each project's issues */
            List<Issue> projIssues = inspector.inspectProjectIssues();
            for (Issue issue : projIssues) {
                Issue.prepareIssue(issue);
                p = issue.getP();
                sum += p;
            }

            avg = sum / projIssues.size();
            averages.add(avg);
            sum = 0;
        }

        /* iterate on all averages of P of the external projects */
        for (Double averageP: averages) {
            totalSum += averageP;
        }
        totalAvg = totalSum / averages.size();
        return totalAvg;
    }

    public String getKey() {
        return key;
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

    public double getP() {
        return p;
    }

    public void setP(double proportion) {
        this.p = proportion;
    }
}
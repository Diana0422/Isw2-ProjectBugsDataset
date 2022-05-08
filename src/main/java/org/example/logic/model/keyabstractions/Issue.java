package org.example.logic.model.keyabstractions;

import org.example.logic.control.InspectionController;
import org.example.logic.model.utils.ProjectInspector;

import java.time.LocalDateTime;
import java.util.*;

public class Issue {
    /***
     * class that represents a Jira Issue
     */
    public static String[] names = {"AVRO", "BOOKKEEPER", "CHUKWA", "CONNECTORS", "CRUNCH", "FALCON", "IVY", "OPENJPA", "PROTON", "SSHD", "STORM", "SYNCOPE", "TAJO", "TEZ", "THRIFT", "TOMEE", "WHIRR", "ZEPPELIN", "ZOOKEEPER"};

    private Project project; // project of the issue
    private String key; // issue name with number
    private String id; // issue id
    private String number; // issue number (no name)
    private List<Release> affectedVersions; // releases in which the issue is present
    private List<Release> fixedVersions; // releases that fixed the issue
    private Release opVersion;
    private Release fixVersion;
    private Release injVersion;
    private double P;
    private LocalDateTime resolution;
    private LocalDateTime created; // date of the creation of the issue (determines the op. version)
    private List<Commit> fixedCommits; // list of commits that fixed the issue
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
        this.P = 0;
    }

    public void addAffected(Release release) {
        List<Release> affected = this.affectedVersions;

        for (Release av: affected) {
            if (av.getIndex() == release.getIndex()) return;
        }
        this.affectedVersions.add(release);

        Collections.sort(this.affectedVersions, Comparator.comparing(Release::getDate));
    }

    public static void prepareIssue(Issue issue, boolean externalProject) {
        /***
         * Complete the issue information with IV (calculating IV using proportion method if necessary)
         * @param issue: the issue to prepare
         * @param externalProject: the issue belongs to a project that is different from the project that is to be analysed
         */
        /* if there is more than one fixed version, consider the most recent one */
        List<Release> fixed = issue.getFixedVersions();
        if (InspectionController.fulldebug) System.out.println(fixed);

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
        if (InspectionController.fulldebug) System.out.println("after fixed merge: "+affected);
        int fixedIdx = 0;

        /* get fixed version index */
        if (fixed.isEmpty()) {
            if (InspectionController.fulldebug) System.out.println("fixed is empty.");
            fixedIdx = -1;
        } else {
            if (InspectionController.fulldebug) System.out.println("fixed is not empty.");
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
        } else {
            System.out.println("affected versions: ");
            for (Release rel: affected) {
                System.out.println(rel.getIndex()+" - "+rel.getName());
            }
        }

        System.out.println("\n");

    }

    private static void applyProportion(Issue issue) {
        /***
         * Apply Proportion method to find the Injected Version of an Issue whose Affected Version on JIRA are not consistent or unreliable
         * @param issue: the issue on which is to be applied Proportion method.
         */
        int fixedV;
        int openV;
        int injV;
        double P;
        Project project = issue.project;

        openV = issue.getOpVersion().getIndex();
    	System.out.println("issue: "+issue.getKey()+", opening ver: "+openV);

        /* If the fixed version isn't found it's set equal to the opening version - at least we know that the issue affects the opening version */
        if (issue.getFixedVersions().isEmpty()) {
            fixedV = openV;
        } else {
            System.out.println(issue.getFixedVersions());
            fixedV = issue.getFixedVersions().get(0).getIndex();
        }
    	System.out.println("issue: "+issue.getKey()+", fixed ver: "+fixedV);

        //If the opening version is greater the the fixed version they are inverted
        // TODO probably useless part
//        if (openV > fixedV) {
//            int temp = fixedV;
//            fixedV = openV;
//            openV = temp;
//        }

        P = calculateProportionMovingWindow(issue, openV, fixedV);
        /* update the issue with corresponding P value */
        issue.setP(P);
        /* calculate the index of the Injected Version */
        injV = (int) (fixedV - (fixedV - openV) * P);

    	System.out.println("issue: "+issue.getKey()+", injected ver: "+injV);

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
//            Release ver = project.getVersions().get(i - 1);
            issue.addAffected(Release.findVersionByIndex(project.getVersions(), i));
//            issue.addAffected(ver);
        }

        /* Update the Fixed Version and the Injected Version of the issue */
        issue.setFixVersion(Release.findVersionByIndex(project.getVersions(), fixedV));
        issue.setInjVersion(Release.findVersionByIndex(project.getVersions(), injV));

        System.out.println("calculated affected versions: ");
        for (Release rel : issue.getAffectedVersions()) {
            System.out.println(rel.getIndex() + " - " + rel.getName());
        }
    }

    private static double calculateProportionMovingWindow(Issue issue, int openV, int fixedV) {
        /***
         * calculates the proportion factor using the issue opening version and fixed version, considering that IV / FV = IV / FV = P
         * The method used to calculate P is Proportion with Moving Window approach.
         * @param issue: the issue to consider.
         * @param openV: the index of the opening version
         * @param fixedV: the index of the fixed version
         * @return double
         */
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
        int end = numBugs; // end index of the bugs window

        /* put the last 1% of the fixed bugs in the window */
        for (int i=start; i<end; i++) {
            window.add(prevBugs.get(i));
        }

        // calculate p as the average of P in last 1% issues fixed
        double avg;
        double sum = 0;

        /* is no previous P values are present, calculate P from other projects data */
        if (window.size() == 0) {
            issue.calculatePValue(openV, fixedV, true);
        } else {
            /* use previous P values of the project to get the value of P for the current issue */
            for (int i=0; i<window.size(); i++) {
                window.get(i).calculatePValue(openV, fixedV, false);
                sum += window.get(i).getP();
            }
        }

        if (window.isEmpty()) {
            avg = 0.0;
        } else {
            avg = sum / window.size();
        }
		System.out.println("Proportion for window: "+window+" ="+avg);
        return avg;
    }

    public void calculatePValue(int openV, int fixedV, boolean coldstart) {
        /***
         * Use the average P values of the other projects to calculate Proportion value for this project
         * @param openV: index of the opening version
         * @param fixedV: index of the fixed version
         * @param coldstart: boolean that indicates that it is necessary to retrieve the average of the P values of the other 75 projects
         */
        int numAffected = affectedVersions.size();

        if (numAffected == 0) {
            /* no affected values to work on - take the average P value of the other projects */
            P = InspectionController.coldproportion; // this will be 0.0 if the issue belong to a project that is not analysed
            return;
        } else {
            /* there are affected versions for the issue */
            int fixIdx = fixedV;
            int injIdx = affectedVersions.get(0).getIndex();
            int opIdx = openV;

            /* if OV = FV, then P= (FV-IV)/(OV-IV) = 1 */
            if (fixIdx == opIdx) {
                P = 1;
                return;
            } else {
                P = (double) (fixIdx-injIdx)/(fixIdx-opIdx);
            }

        }
    }

    public static double calculateColdStartProportion(String mainProjectName, Properties prop) {
        /***
         * Calculate the average of the P values among the other 75 apache projects
         * @param mainProjectName: the name of the main project to analyse
         * */
        double sum = 0;
        double P = 0;
        double avg = 0;
        List<Double> averages = new ArrayList<>();
        double totalAvg = 0;
        double totalSum = 0;

        /* iterate on all projects */
        for (int i=0; i<names.length; i++) {
            if (names[i].equals(mainProjectName)) continue;
            String projectName = names[i];
            Project project = new Project(projectName, prop);
            ProjectInspector inspector = new ProjectInspector(project);
            List<Issue> projIssues = inspector.inspectProjectIssues();
            for (Issue issue: projIssues) {
                Issue.prepareIssue(issue, project.isExternal());
                P = issue.getP();
                sum += P;
            }

            avg = (double) sum / projIssues.size();
            averages.add(avg);
            sum = 0;
            avg = 0;
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

    public double getP() {
        return P;
    }

    public void setP(double proportion) {
        this.P = proportion;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }
}
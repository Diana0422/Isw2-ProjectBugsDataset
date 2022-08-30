package org.example.logic.control;

import org.example.logic.model.keyabstractions.*;
import org.example.logic.model.keyabstractions.Record;
import org.example.logic.model.utils.ARFFWriter;
import org.example.logic.model.utils.CSVWriter;
import org.example.logic.model.utils.ProjectInspector;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InspectionController {

    public static boolean DEBUG = false;
    public static boolean FULL_DEBUG = false;
    public static double COLD_PROPORTION; // proportion average from the other 75 apache projects

    public static void main(String [] args) throws IOException {
//		main flow of the application
        Properties prop = new Properties();
        String propFileName = "config.properties";
        String projName = "";
        String projDir = "";
        String log = "";
        int percent = 0;

        try (InputStream inputStream = InspectionController.class.getClassLoader().getResourceAsStream(propFileName)) {
            prop.load(inputStream);

            // get the project properties
            projName = prop.getProperty("project_name");
            projDir = prop.getProperty("project_dir");
            percent = Integer.parseInt(prop.getProperty("percent"));
            DEBUG = Boolean.parseBoolean(prop.getProperty("debug"));
            FULL_DEBUG = Boolean.parseBoolean(prop.getProperty("full_debug"));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /* instantiate project */
        Project proj = new Project(projName, percent, projDir, prop);

        /* calculate the average P value of the other 75 project apache:
        * warning: this is valid just as a starting value not having analyzed any issue */
        COLD_PROPORTION = Issue.calculateColdStartProportion(projName, prop);

        /* inspect project */
        if (DEBUG) {
            log = "--> Inspecting project: " + proj.getProjName() +
                    "\n" + "--> Versions selected: " + proj.getViewedPercentage() + "%\n" + "...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        ProjectInspector inspector = new ProjectInspector(proj);

        /* retrieve all the project commits */
        if (DEBUG) {
            log = "--> Looking for Commits...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Commit> commits = inspector.inspectProjectCommits();

        if (DEBUG) {
            int i = 0;
            for (Commit commit: commits) {
                i++;
                System.out.println("\n");
                System.out.println("Commit n°"+ i+":\n");
                System.out.println("    commit id: "+commit.getShaId());
                System.out.println("    author: "+commit.getAuthor());
                System.out.println("    message: "+commit.getMessage());
                System.out.println("    date: "+commit.getDate());
                for (JFile file: commit.getCommittedFiles()) {
                    System.out.println("- "+file.getName());
                }
                // TODO System.out.println("    version: "+commit.getVersion().getName());
            }
        }

        List<JFile> files = inspector.inspectProjectFiles();

        System.out.println(files);

        /* get project issues on JIRA */
        if (DEBUG) {
            log = "--> Setting bug issues...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Issue> issues = inspector.inspectProjectIssues();

        if (DEBUG) {
            int i = 0;
            for (Issue issue: issues) {
                i++;
                System.out.println("\n");
                System.out.println("Issue n°"+ i+":\n");
                System.out.println("    issue id: "+issue.getId());
                System.out.println("    issue key: "+issue.getKey());
                System.out.println("    resolution: "+issue.getResolution());
                for (Commit commit: issue.getFixedCommits()) {
                    System.out.println("Fixed Commits: ");
                    System.out.println("    commit id: "+commit.getDate());
                    System.out.println("    message: "+commit.getMessage());
                }
            }
        }

        if (DEBUG) {
            log = "--> Update bugginess...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        inspector.updateBugginess(issues);

        if (DEBUG) {
            int i = 0;
            for (JFile file: proj.getJavaFiles()) {
                i++;
                if (file.getBuggyReleases().isEmpty()) continue;
                System.out.println("File n°"+i+":\n");
                System.out.println("    filename: "+file.getName());
                System.out.println("    filepath: "+file.getRelPath());
                for (Release release: file.getReleases()) {
                    System.out.println("Release: ");
                    System.out.println("    release name: "+ release.getName());
                    System.out.println("    release date: "+ release.getDate());
                    System.out.println("    release index: "+release.getIndex());
                }
                for (Release release: file.getBuggyReleases()) {
                    System.out.println("Buggy Release: ");
                    System.out.println("    release name: "+ release.getName());
                    System.out.println("    release date: "+ release.getDate());
                    System.out.println("    release index: "+release.getIndex());
                }
            }
        }


        if (DEBUG) {
            log = "--> Producing dataset...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Record> obs = inspector.produceRecords();

        if (DEBUG) {
            log = "--> Calculating features...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        obs = inspector.calculateFeatures();

        if (DEBUG) {
            log = "--> Selecting first "+percent+"% of instances...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        obs = inspector.selectRecords(obs);

        if (DEBUG) {
            log = "--> Writing version information to file: "+proj.getProjName()+"Versions.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeReleaseInfo(proj.getVersions(), "Versions", proj.getProjName());

        if (DEBUG) {
            log = "--> Writing tickets information to file: "+proj.getProjName()+"Tickets.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeTicketsInfo(proj.getBugIssues(), "Tickets", proj.getProjName());

        if (DEBUG) {
            log = "--> Writing commits information to file: "+proj.getProjName()+"Commits.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeCommitsInfo(proj.getRefCommits(), "Commits", proj.getProjName());

        if (DEBUG) {
            log = "--> Writing dataset: "+proj.getProjName()+"dataset.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeDataset(obs, proj.getProjName());

        if (DEBUG) {
            log = "--> Converting CSV file to ARFF file";
            Logger.getGlobal().log(Level.INFO, log);
        }
        ARFFWriter.getInstance().convertCvsToArff(proj.getProjName()+"dataset.csv");
    }
}

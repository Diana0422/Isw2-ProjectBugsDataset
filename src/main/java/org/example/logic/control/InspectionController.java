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


    public static boolean isFullDebug() {
        return fullDebug;
    }

    public static double getColdProportion() {
        return coldProportion;
    }

    private static boolean debug = false;
    private static boolean fullDebug = false;
    private static double coldProportion; // proportion average from the other 75 apache projects

    public static void simpleDebug(String msg) {
        if (debug) {
            StringBuilder log;
            log = new StringBuilder(msg);
            String s = log.toString();
            Logger.getGlobal().log(Level.FINE, s);
        }
    }

    public static void debugFiles(List<JFile> files) {
        if (debug) {
            int i = 0;
            StringBuilder log = new StringBuilder();
            for (JFile file: files) {
                i++;
                if (file.getBuggyReleases().isEmpty()) continue;
                log = new StringBuilder("File n°" + i + ":\n" +
                        "    filename: " + file.getName() +
                        "    filepath: " + file.getRelPath());
                for (Release release: file.getReleases()) {
                    log.append("Release: ")
                            .append("    release name: ")
                            .append(release.getName())
                            .append("    release date: ")
                            .append(release.getDate())
                            .append("    release index: ")
                            .append(release.getIndex());
                }
                for (Release release: file.getBuggyReleases()) {
                    log.append("Buggy Release: ")
                            .append("    release name: ")
                            .append(release.getName())
                            .append("    release date: ")
                            .append(release.getDate())
                            .append("    release index: ")
                            .append(release.getIndex());
                }
            }
            String s = log.toString();
            Logger.getGlobal().log(Level.FINE, s);
        }
    }

    public static void debugIssues(List<Issue> issues) {
        int i = 0;
        StringBuilder log = new StringBuilder();
        if (debug) {
            for (Issue issue: issues) {
                i++;
                log = new StringBuilder("\n" +
                        "Issue n°" + i + ":\n" +
                        "    issue id: " + issue.getId() +
                        "    issue key: " + issue.getKey() +
                        "    resolution: " + issue.getResolution());
                for (Commit commit: issue.getFixedCommits()) {
                    log.append("Fixed Commits: ").append("    commit id: ").append(commit.getDate()).append("    message: ").append(commit.getMessage());
                }
            }
            String s = log.toString();
            Logger.getGlobal().log(Level.FINE, s);
        }
    }

    public static void debugCommits(List<Commit> commits) {
        int i = 0;
        StringBuilder log = new StringBuilder();
        if (debug) {
            for (Commit commit: commits) {
                i++;
                log = new StringBuilder("\n" +
                        "Commit n°" + i + ":\n" +
                        "    commit id: " + commit.getShaId() +
                        "    author: " + commit.getAuthor() +
                        "    message: " + commit.getMessage() +
                        "    date: " + commit.getDate());

                for (JFile file: commit.getCommittedFiles()) {
                    log.append("- ").append(file.getName());
                }
            }
            String s = log.toString();
            Logger.getGlobal().log(Level.FINE, s);
        }
    }

    public static void main(String [] args) throws IOException {
//		main flow of the application
        Properties prop = new Properties();
        String propFileName = "config.properties";
        String projName = "";
        String projDir = "";
        StringBuilder log = new StringBuilder();
        int percent = 0;

        try (InputStream inputStream = InspectionController.class.getClassLoader().getResourceAsStream(propFileName)) {
            prop.load(inputStream);

            // get the project properties
            projName = prop.getProperty("project_name");
            projDir = prop.getProperty("project_dir");
            percent = Integer.parseInt(prop.getProperty("percentage"));
            debug = Boolean.parseBoolean(prop.getProperty("debug"));
            fullDebug = Boolean.parseBoolean(prop.getProperty("full_debug"));

        } catch (IOException e) {
            log = new StringBuilder("Error collecting project properties");
            Logger.getGlobal().log(Level.SEVERE, String.valueOf(log));
        }

        /* instantiate project */
        Project proj = new Project(projName, percent, projDir, prop);

        /* calculate the average P value of the other 75 project apache:
        * warning: this is valid just as a starting value not having analyzed any issue */
        coldProportion = Issue.calculateColdStartProportion(projName, prop);

        /* inspect project */
        simpleDebug("--> Inspecting project: " + proj.getProjName() +
                    "\n" + "--> Versions selected: " + proj.getViewedPercentage() + "%\n" + "...");
        ProjectInspector inspector = new ProjectInspector(proj);

        /* retrieve all the project commits */
        simpleDebug("--> Looking for Commits...");
        List<Commit> commits = inspector.inspectProjectCommits();
        debugCommits(commits);

        /* Retrieve all project files */
        simpleDebug("--> OK Retrieving project files...");
        inspector.inspectProjectFiles();

        /* get project issues on JIRA */
        simpleDebug("--> OK Setting bug issues...");
        List<Issue> issues = inspector.inspectProjectIssues();
        debugIssues(issues);

        simpleDebug("--> OK Update bugginess...");
        inspector.updateBugginess(issues);

        simpleDebug("--> Fixing release gaps");
        inspector.fixReleaseGaps();

        simpleDebug("--> Producing dataset...");
        inspector.produceRecords();

        simpleDebug("--> Calculating features...");
        List<Record> obs = inspector.calculateFeatures();

        simpleDebug("--> OK Selecting first " + percent + "% of instances...");
        obs = inspector.selectRecords(obs);

        simpleDebug("--> Writing version information to file: " + proj.getProjName() + "Versions.csv");
        CSVWriter.getInstance().writeReleaseInfo(proj.getVersions(), "Versions", proj.getProjName());

        simpleDebug("--> Writing tickets information to file: " + proj.getProjName() + "Tickets.csv");
        CSVWriter.getInstance().writeTicketsInfo(proj.getBugIssues(), "Tickets", proj.getProjName());

        simpleDebug("--> Writing commits information to file: " + proj.getProjName() + "Commits.csv");
        CSVWriter.getInstance().writeCommitsInfo(proj.getRefCommits(), "Commits", proj.getProjName());

        simpleDebug("--> Writing dataset: " + proj.getProjName() + "dataset.csv");
        CSVWriter.getInstance().writeDataset(obs, proj.getProjName());

        simpleDebug("--> Converting CSV file to ARFF file");
        ARFFWriter.getInstance().convertCvsToArff(proj.getProjName()+"dataset.csv");
    }
}

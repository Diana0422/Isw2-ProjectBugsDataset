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

    public static boolean debug = false;
    public static boolean fulldebug = false;
    public static double coldproportion; // proportion average from the other 75 apache projects

    public static void main(String [] args) throws IOException {
//		main flow of the application
        Properties prop = new Properties();
        String propFileName = "config.properties";
        String projName = "";
        String projDir = "";
        String log = "";
        Integer percent = 0;

        try (InputStream inputStream = InspectionController.class.getClassLoader().getResourceAsStream(propFileName)) {
            prop.load(inputStream);

            // get the project properties
            projName = prop.getProperty("project_name");
            System.out.println(projName);
            projDir = prop.getProperty("project_dir");
            System.out.println(projDir);
            percent = Integer.parseInt(prop.getProperty("percent"));
            debug = Boolean.parseBoolean(prop.getProperty("debug"));
            fulldebug = Boolean.parseBoolean(prop.getProperty("full_debug"));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /* instantiate project */
        Project proj = new Project(projName, percent, projDir, prop);

        /* calculate the average P value of the other 75 project apache */
        coldproportion = Issue.calculateColdStartProportion(projName, prop);
        System.out.println(coldproportion);

        /* inspect project */
        if (debug) {
            log = "--> Inspecting project: " + proj.getProjName() +
                    "\n" + "--> Versions selected: " + proj.getViewedPercentage() + "%\n" + "...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        ProjectInspector inspector = new ProjectInspector(proj);

        /* retrieve all the project commits */
        if (debug) {
            log = "--> Looking for Commits...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Commit> commits = inspector.inspectProjectCommits();

        if (debug) {
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


        /* get project issues on JIRA */
        if (debug) {
            log = "--> Setting bug issues...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Issue> issues = inspector.inspectProjectIssues();

        if (debug) {
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

        if (debug) {
            log = "--> Update bugginess...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        inspector.updateBugginess(issues);

        if (debug) {
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


        if (debug) {
            log = "--> Producing dataset...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Record> obs = inspector.produceRecords();

        if (debug) {
            log = "--> Calculating features...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        inspector.calculateFeatures();

        if (debug) {
            log = "--> Writing version information to file: "+proj.getProjName()+"Versions.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeReleaseInfo(proj.getVersions(), "Versions", proj.getProjName());

        if (debug) {
            log = "--> Writing tickets information to file: "+proj.getProjName()+"Tickets.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeTicketsInfo(proj.getBugIssues(), "Tickets", proj.getProjName());

        if (debug) {
            log = "--> Writing commits information to file: "+proj.getProjName()+"Commits.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeCommitsInfo(proj.getCommits(), proj.getRefCommits(), "Commits", proj.getProjName());

        if (debug) {
            log = "--> Writing dataset: "+proj.getProjName()+"dataset.csv";
            Logger.getGlobal().log(Level.INFO, log);
        }
        CSVWriter.getInstance().writeDataset(obs, proj.getProjName());

        if (debug) {
            log = "--> Converting CSV file to ARFF file";
            Logger.getGlobal().log(Level.INFO, log);
        }
        ARFFWriter.getInstance().convertCvsToArff(proj.getProjName()+"dataset.csv");
    }
}

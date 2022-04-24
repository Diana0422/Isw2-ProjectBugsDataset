package org.example.logic.control;

import org.example.logic.model.keyabstractions.*;
import org.example.logic.model.keyabstractions.Record;
import org.example.logic.model.utils.ARFFWriter;
import org.example.logic.model.utils.CSVWriter;
import org.example.logic.model.utils.ProjectInspector;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InspectionController {

    public static boolean verbose = false;

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
            verbose = Boolean.parseBoolean(prop.getProperty("verbose"));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /* instantiate project */
        Project proj = new Project(projName, percent, projDir, prop);

        /* inspect project */
        if (verbose) {
            log = "--> Inspecting project: " + proj.getProjName() +
                    "\n" + "--> Versions selected: " + proj.getViewedPercentage() + "%\n" + "...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        ProjectInspector inspector = new ProjectInspector(proj);

        if (verbose) {
            log = "--> Inspecting project files from directory...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<JFile> files = inspector.inspectProjectFiles();

        if (verbose) {
            int i = 0;
            for (JFile file: files) {
                i++;
                System.out.println("\n");
                System.out.println("File n°"+ i+":\n");
                System.out.println("    name: "+file.getName());
                System.out.println("    rel. path: "+file.getRelPath());
                String s = "    releases: ";
                for (int j=0; j<file.getReleases().size(); j++) {
                    s += file.getReleases().get(j).getName();
                    s += ",";
                }
            }
        }

        /* retrieve all the project commits */
        if (verbose) {
            log = "--> Looking for Commits...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Commit> commits = inspector.inspectProjectCommits();

        if (verbose) {
            int i = 0;
            for (Commit commit: commits) {
                i++;
                System.out.println("\n");
                System.out.println("Commit n°"+ i+":\n");
                System.out.println("    commit id: "+commit.getShaId());
                System.out.println("    author: "+commit.getAuthor());
                System.out.println("    message: "+commit.getMessage());
                System.out.println("    date: "+commit.getDate());
                // TODO System.out.println("    version: "+commit.getVersion().getName());
            }
        }

        /* get project issues on JIRA */
        if (verbose) {
            log = "--> Setting bug issues...";
            Logger.getGlobal().log(Level.INFO, log);
        }
        List<Issue> issues = inspector.inspectProjectIssues();

        if (verbose) {
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

//        if (verbose) {
//            log = "--> Update bugginess...";
//            Logger.getGlobal().log(Level.INFO, log);
//        }
//        inspector.
//
//        if (verbose) {
//            log = "--> Producing dataset...";
//            Logger.getGlobal().log(Level.INFO, log);
//        }
//        List<Record> obs = inspector.produceDataset();
    }
}

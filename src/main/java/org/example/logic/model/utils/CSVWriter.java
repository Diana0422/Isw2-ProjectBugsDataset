package org.example.logic.model.utils;

import org.example.logic.model.keyabstractions.Commit;
import org.example.logic.model.keyabstractions.Issue;
import org.example.logic.model.keyabstractions.Release;
import org.example.logic.model.keyabstractions.Record;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CSVWriter {

    private static CSVWriter instance;

    private CSVWriter() {}

    public static CSVWriter getInstance() {
        if (instance == null) {
            instance = new CSVWriter();
        }
        return instance;
    }

    public void writeTicketsInfo(List<Issue> issues, String filename, String projName) {
        // iterate on array of tickets and writes them as csv entries on file

        String outname = projName + filename + ".csv";
        try (FileWriter fileWriter = new FileWriter(outname)) {
            fileWriter.append("Ticket ID;Date;Fixed Commits;Affected Versions");
            fileWriter.append("\n");
            for (Issue i: issues) {
                fileWriter.append(i.getKey());
                fileWriter.append(";");
                fileWriter.append(Parser.getInstance().parseLocalDateToString(i.getCreated()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(i.getFixedCommits().size()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(i.getAffectedVersions().size()));
                fileWriter.append("\n");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeReleaseInfo(List<Release> releases, String filename, String projName) {
        // iterate on array of releases and writes as csv entries on file

        String outname = projName + filename + ".csv";
        try (FileWriter fileWriter = new FileWriter(outname)) {
            fileWriter.append("Index;Version ID;Version Name;Date");
            fileWriter.append("\n");

            int numEntries = releases.size();
            for (int i=0; i<numEntries; i++) {
                Release r = releases.get(i);
                if (r != null) {
                    Integer index = i + 1;
                    fileWriter.append(index.toString());
                    fileWriter.append(";");
                    fileWriter.append(r.getId());
                    fileWriter.append(";");
                    fileWriter.append(r.getName());
                    fileWriter.append(";");
                    fileWriter.append(Parser.getInstance().parseLocalDateToString(r.getDate()));
                    fileWriter.append("\n");
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeCommitsInfo(List<Commit> commits, List<Commit> refCommits, String filename, String projName) {
        // iterate on array of commits and write as entries on file csv

        String outname = projName + filename + ".csv";
        try (FileWriter fileWriter = new FileWriter(outname)) {
            fileWriter.append("Commit Sha;Date;Author;Referenced Ticket;Version;Committed Files");
            fileWriter.append("\n");
            for (Commit c: commits) {
                fileWriter.append(c.getShaId());
                fileWriter.append(";");
                fileWriter.append(Parser.getInstance().parseLocalDateToString(c.getDate()));
                fileWriter.append(";");
                fileWriter.append(c.getAuthor());
                fileWriter.append(";");
                fileWriter.append(c.getMessage());
                fileWriter.append(";");
                if (c.getVersion()!= null) {
                    fileWriter.append(Integer.toString(c.getVersion().getIndex()));
                } else {
                    fileWriter.append("N/D");
                }
                fileWriter.append(";");
                fileWriter.append(Integer.toString(c.getCommittedFiles().size()));
                fileWriter.append("\n");
            }


            for (Commit c: refCommits) {
                fileWriter.append(c.getShaId());
                fileWriter.append(";");
                fileWriter.append(Parser.getInstance().parseLocalDateToString(c.getDate()));
                fileWriter.append(";");
                fileWriter.append(c.getAuthor());
                fileWriter.append(";");
                fileWriter.append(c.getMessage());
                fileWriter.append(";");
                if (c.getVersion()!= null) {
                    fileWriter.append(Integer.toString(c.getVersion().getIndex()));
                } else {
                    fileWriter.append("N/D");
                }
                fileWriter.append(";");
                fileWriter.append(Integer.toString(c.getCommittedFiles().size()));
                fileWriter.append("\n");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void writeDataset(List<Record> obs, String projName) {

        String outname = projName+"dataset.csv";
        try (FileWriter fileWriter = new FileWriter(outname)) {
            fileWriter.append("Version;Class Name;Size;LOC_added;MAX_LOC_added;LOC_touched;AVG_LOC_added;NR;ChgSetSize;NFix;NAuth;Buggy");
            fileWriter.append("\n");

            for (Record r: obs) {
                fileWriter.append(Integer.toString(r.getVersion()));
                fileWriter.append(";");
                fileWriter.append(r.getFileName());
                fileWriter.append(";");
                fileWriter.append(Integer.toString(r.getSize()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(r.getLocAdded()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(r.getMaxLocAdded()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(r.getLocTouched()));
                fileWriter.append(";");
                fileWriter.append(Float.toString(r.getAvgLoc()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(r.getNumRevisions()));
                fileWriter.append(";");
                fileWriter.append(Float.toString(r.getChgSetSize()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(r.getNumFix()));
                fileWriter.append(";");
                fileWriter.append(Integer.toString(r.getAuthors().size()));
                fileWriter.append(";");
                fileWriter.append(r.getBuggy());
                fileWriter.append("\n");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
}
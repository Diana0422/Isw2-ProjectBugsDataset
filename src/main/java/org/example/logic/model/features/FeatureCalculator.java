package org.example.logic.model.features;

import org.example.logic.model.keyabstractions.Record;

import java.util.HashMap;
import java.util.List;

public class FeatureCalculator {

    private FeatureCalculator() {}

    private static List<HashMap<String, Record>> hashMaps;

    public static List<HashMap<String, Record>> getHashMaps() {
        return hashMaps;
    }

    public static void setHashMaps(List<HashMap<String, Record>> hashMaps) {
        FeatureCalculator.hashMaps = hashMaps;
    }

    /***
     * Update additions of a record (LOC Added)
     * (the value is already calculated as cumulative in release)
     * */
    public static void setAdditions(Record rec, int additions) {
        rec.setLocAdded(additions);
    }

    public static void setMaxLocAdded(Record rec, int maxAdd) {
        rec.setMaxLocAdded(maxAdd);
    }

    /**
     * Update deletions of a record.
     * (the value is already calculated as cumulative in release)
     * */
    public static void setDeletions(Record rec, int deletions) {
        rec.setLocDeleted(deletions);
    }

    /**
     * Updates LOC of a record (Size).
     * (the value is already calculated as cumulative in release)
     * */
    public static void updateLOC(String[] lines, Record rec) {
        boolean multi = false;

        int loc = lines.length;
        if (loc == 0) {
            return;
        }

        String line;
        for (String s : lines) {

            line = s;

            if (multi) {
                loc--;

                if (line.contains("*/")) {
                    multi = false;
                }
            } else if (line.contains("//")) {
                loc--;
            } else if (line.contains("/*")) {
                loc--;
                multi = true;
            }

        }

        rec.setSize(loc);
    }

    /***
     * update the value of the sum over revisions of LOC added+deleted (LOC Touched)
     * (the value is already calculated as cumulative in release)
     */
    public static void calculateLOCTouched(Record rec) {
        int added = rec.getLocAdded();
        int deleted = rec.getLocDeleted();
        int locTouched = added+deleted;
        rec.setLocTouched(locTouched);
    }

    /***
     * increments the number of revisions for the record.
     */
    public static void updateNR(Record rec) {
        int nr = rec.getNumRevisions();
        rec.setNumRevisions(nr+1);
    }

    /***
     * update the list of authors for the record specified (and thus the number of authors).
     */
    public static void updateNAuth(String authorName, Record rec) {
        if (rec.getAuthors().contains(authorName)) return;
        rec.getAuthors().add(authorName);
    }

    public static void updateNFix(Record rec) {
        int fix = rec.getNFix();
        rec.setNFix(fix + 1);
    }

    /***
     * Update the value of the sum over revisions of added - deleted LOC in a record.
     */
    public static void updateChurn(Record rec) {
        int added = rec.getLocAdded();
        int deleted = rec.getLocDeleted();
        int churn = rec.getChurn()+(added-deleted);
        rec.setChurn(churn);
        rec.setMaxChurn(added-deleted);
    }

    /***
     * update the value of the sum over revisions of the number of classes touched together with the file specified in the record
     */
    public static void updateChgSetSize(int chgSetSize, Record rec) {
        rec.addChgSetSize(chgSetSize);
        rec.setMaxChgSetSize(chgSetSize);
    }

    /**
     * update the value of the age of a class
     * (cross-release)
     * @param age
     * @param rec
     */
    public static void calculateAge(int age, Record rec) {
        rec.setAge(age);
    }

    /**
     * Calculate the age of a file weighted on the total locTouched over releases
     * (cross-release)
     * @param rec
     */
    public static void calculateWeightedAge(Record rec) {
        int age = rec.getAge();
        int locTouched = rec.getTotalLocTouched();
        if (locTouched == 0) {
            rec.setWeightedAge(0);
        } else {
            double weighted = age/(double)locTouched;
            rec.setWeightedAge(weighted);
        }
    }
}
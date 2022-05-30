package org.example.logic.model.features;
import org.example.logic.model.keyabstractions.Record;
import java.util.HashMap;
import java.util.List;

public class FeatureCalculator {

    private static List<HashMap<String, Record>> hashMaps;

    public static List<HashMap<String, Record>> getHashMaps() {
        return hashMaps;
    }

    public static void setHashMaps(List<HashMap<String, Record>> hashMaps) {
        FeatureCalculator.hashMaps = hashMaps;
    }

    /***
     * Update additions of a record (LOC Added)
     *
     * */
    public static void setAdditions(Record rec, int additions) {
        rec.setLocAdded(additions);
        rec.setMaxLocAdded(additions);
    }

    /**
     * Update deletions of a record.
     * */
    public static void setDeletions(Record rec, int deletions) {
        rec.setLocDeleted(deletions);
    }

    /**
     * Updates LOC of a record (Size).
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
     */
    public static void calculateLOCTouched(Record rec) {
        int added = rec.getLocAdded();
        int deleted = rec.getLocDeleted();
        int locTouched = rec.getLocTouched()+added+deleted;
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

    public static void calculateNFix(Record rec) {
        int fix = rec.getNFix();
        rec.setNFix(fix+1);
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

    public void calculateAge() {
        // TODO
    }

    public void calculateWeightedAge() {
        // TODO
    }
}
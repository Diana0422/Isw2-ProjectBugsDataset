package org.example.logic.model.features;
import org.example.logic.model.keyabstractions.Record;

import java.util.HashMap;
import java.util.List;

public class FeatureCalculator {

    private static FeatureCalculator instance;
    private static List<HashMap<String, Record>> hashMaps;

    private FeatureCalculator() {}

    public static FeatureCalculator getInstance() {
        if (instance == null) {
            instance = new FeatureCalculator();
        }

        return instance;
    }

    public void setHashMaps(List<HashMap<String, Record>> hashMaps) {
        this.hashMaps = hashMaps;
    }

    public List<HashMap<String, Record>> getHashMaps() {
        return hashMaps;
    }

    public void updateAdditions(int additions, Record record) {
        /***
         * Update additions of a record (LOC Added)
         *
         * */
//        int additions = 0;
//        String line;
//        String beginChar;
//
//        for (int i=0; i<lines.length; i++) {
//            line = lines[i];
//            beginChar = line.substring(0, 1);
//
//            if (beginChar.contains("+")) additions++;
//        }

        int locAdded = record.getLocAdded()+additions;
        record.setLocAdded(locAdded);
        record.setMaxLocAdded(additions);
    }

    public void updateDeletions(int deletions, Record record) {
        /**
         * Update deletions of a record.
         * */
//
//        int deletions = 0;
//        String line = null;
//        String beginChar = null;
//
//        for (int i=0; i<lines.length; i++) {
//            line = lines[i];
//            beginChar = line.substring(0, 1);
//
//            if (beginChar.contains("-")) deletions++;
//        }

        int locDeleted = record.getLocDeleted()+deletions;
        record.setLocDeleted(locDeleted);
    }


    public void updateLOC(String[] lines, Record record) {
        /**
         * Updates LOC of a record (Size).
         * */

        boolean multi = false;

        int loc = lines.length;
        if (loc == 0) {
            return;
        }

        String line = null;

        for(int i=0; i<lines.length; i++) {

            line = lines[i];

            if(multi){
                loc--;

                if(line.contains("*/")) {
                    multi = false;
                }
            }
            else if(line.contains("//")) {
                loc--;
            }
            else if(line.contains("/*")) {
                loc--;
                multi = true;
            }

        }

        record.setSize(loc);
    }

    public void calculateLOCTouched(Record record) {
        /***
         * update the value of the sum over revisions of LOC added+deleted (LOC Touched)
         */
        int added = record.getLocAdded();
        int deleted = record.getLocDeleted();
        int locTouched = record.getLocTouched()+added+deleted;
        record.setLocTouched(locTouched);
    }

    public void updateNR(Record record) {
        /***
         * increments the number of revisions for the record.
         */
        int NR = record.getNumRevisions();
        record.setNumRevisions(NR+1);
    }

    public void updateNAuth(String authorName, Record record) {
        /***
         * update the list of authors for the record specified (and thus the number of authors).
         */
        if (record.getAuthors().contains(authorName)) return;
        record.getAuthors().add(authorName);
    }

    public void calculateNFix() {
        // TODO
    }

    public void updateChurn(Record record) {
        /***
         * Update the value of the sum over revisions of added - deleted LOC in a record.
         */
        int added = record.getLocAdded();
        int deleted = record.getLocDeleted();
        int churn = record.getChurn()+(added-deleted);
        record.setChurn(churn);
        record.setMaxChurn(added-deleted);
    }

    public void updateChgSetSize(int chgSetSize, Record record) {
        /***
         * update the value of the sum over revisions of the number of classes touched together with the file specified in the record
         */
        record.addChgSetSize(chgSetSize);
        record.setMaxChgSetSize(chgSetSize);
    }

    public void calculateAge() {
        // TODO
    }

    public void calculateWeightedAge() {
        // TODO
    }

    public void calculateNSmells() {
        // TODO
    }
}
package org.example.logic.model.features;

public class FeatureCalculator {

    private static FeatureCalculator instance;

    private FeatureCalculator() {}

    public static FeatureCalculator getInstance() {
        if (instance == null) {
            instance = new FeatureCalculator();
        }

        return instance;
    }

    public int getAdditions(String[] lines) {
        int additions = 0;
        String line = null;
        String beginChar = null;

        for (int i=0; i<lines.length; i++) {
            line = lines[i];
            beginChar = line.substring(0, 1);

            if (beginChar.contains("+")) additions++;
        }

        return additions;
    }

    public int getDeletions(String[] lines) {
        int deletions = 0;
        String line = null;
        String beginChar = null;

        for (int i=0; i<lines.length; i++) {
            line = lines[i];
            beginChar = line.substring(0, 1);

            if (beginChar.contains("-")) deletions++;
        }

        return deletions;
    }


    public int getLOC(String[] lines) {

        int loc = 0;
        boolean multi = false;

        loc = lines.length;

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

        return loc;
    }
}
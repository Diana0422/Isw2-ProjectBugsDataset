package org.example.logic.model.keyabstractions;

import java.util.ArrayList;
import java.util.List;

public class Record {

    private int version;
    private String fileName;
    private int numRevisions;
    private String buggy;
    private int size;
    private int locTouched;
    private int locAdded;
    private int maxLocAdded;
    private int chgSetSize;
    private int numFix;
    private List<String> authors;


    public Record(int version, String filename) {
        this.version = version;
        this.fileName = filename;
        this.numRevisions = 0;
        this.size = 0;
        this.locTouched = 0;
        this.locAdded = 0;
        this.maxLocAdded = 0;
        this.chgSetSize = 0;
        this.numFix = 0;
        this.buggy = "No";
        this.authors = new ArrayList<>();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getBuggy() {
        return buggy;
    }

    public void setBuggy(String buggy) {
        this.buggy = buggy;
    }

    /*
     * LOC : size
     */

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /*
     * LOC added : sum of LOC added over revisions
     */

    public int getLocAdded() {
        return locAdded;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public void addLocAdded(int locAdded) {
        this.locAdded += locAdded;

        // calculate max loc added
        if (locAdded > this.maxLocAdded) this.maxLocAdded = locAdded;
    }


    /*
     * LOC touched : sum of LOC added+deleted over revisions (LOC changed)
     */
    public int getLocTouched() {
        return locTouched;
    }

    public void setLocTouched(int locTouched) {
        this.locTouched = locTouched;
    }

    public void addLocTouched(int locTouched) {
        this.locTouched += locTouched;
    }

    /*
     * MAX LOC added : max of the LOC added
     */

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setMaxLocAdded(int maxLoc) {
        this.maxLocAdded = maxLoc;
    }

    /*
     * AVG LOC added : avg of the LOC added
     */

    public float getAvgLoc() {

        float avg;

        if (this.locAdded == 0) {
            avg = 0;
        } else {
            avg = (float)this.locAdded/this.numRevisions;
        }

        return (float) (Math.floor(avg * 100) / 100);
    }

    /*
     * NUM revision : number of revisions
     */

    public int getNumRevisions() {
        return numRevisions;
    }

    public void setNumRevisions(int numRevisions) {
        this.numRevisions = numRevisions;
    }

    public void addRevision() {
        this.numRevisions++;
    }

    /*
     * ChgSetSize : number of files committed together with file
     */

    public int getChgSetSize() {
        return chgSetSize;
    }

    public void setChgSetSize(int chgSetSize) {
        this.chgSetSize = chgSetSize;
    }

    public void addChgSetSize(int num) {
        this.chgSetSize+=num;
    }

    /*
     * NAuth : num authors
     */

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void addAuthor(String auth) {
        for (String author: this.authors) {
            if (author.equals(auth)) {
                return;
            }
        }
        this.authors.add(auth);
    }

    /*
     * NFix : number of fixes
     *
     */

    public void addFix() {
        this.numFix++;
    }

    public int getNumFix() {
        return numFix;
    }

    public void setNumFix(int numFix) {
        this.numFix = numFix;
    }
}

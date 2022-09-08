package org.example.logic.model.keyabstractions;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that represents the final dataset record metadata
 */
public class Record {

    private int version;
    private String fileName;
    private int numRevisions;
    private String buggy;
    private int size;
    private int locTouched;
    private int totalLocTouched;
    private int locAdded;
    private int maxLocAdded;
    private int locDeleted;
    private int churn;
    private int maxChurn;
    private int chgSetSize;
    private int maxChgSetSize;
    private int numFix;
    private List<String> authors;
    private final List<String> tickets;
    private int age;
    private double weightedAge;


    public Record(int version, String filename) {
        this.version = version;
        this.fileName = filename;
        this.numRevisions = 0;
        this.size = 0;
        this.locTouched = 0;
        this.totalLocTouched = 0;
        this.locAdded = 0;
        this.locDeleted = 0;
        this.maxLocAdded = 0;
        this.churn = 0;
        this.maxChurn = 0;
        this.chgSetSize = 0;
        this.maxChgSetSize = 0;
        this.numFix = 0;
        this.age = 0;
        this.weightedAge = 0;
        this.buggy = "No";
        this.authors = new ArrayList<>();
        this.tickets = new ArrayList<>();
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

    public String getBuggy() {
        return buggy;
    }

    public void setBuggy(String buggy) {
        this.buggy = buggy;
    }

    /**
     * LOC : size of the file
     */

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    /**
     * LOC added: lines of code added
     */

    public int getLocAdded() {
        return locAdded;
    }

    public void setLocAdded(int locAdded) {
        this.locAdded = locAdded;
    }

    public int getMaxLocAdded() {
        return maxLocAdded;
    }

    public void setMaxLocAdded(int maxLoc) {
         this.maxLocAdded = maxLoc;
    }

    public float getAvgLoc() {

        float avg;

        if (this.locAdded == 0 || this.numRevisions == 0) {
            avg = 0;
        } else {
            avg = (float)this.locAdded/this.numRevisions;
        }

        return (float) (Math.floor(avg * 100) / 100);
    }


    /**
     * LOC touched : sum of LOC added+deleted over revisions (LOC changed)
     */
    public int getLocTouched() {
        return locTouched;
    }

    public int getTotalLocTouched() {return totalLocTouched; }

    public void setLocTouched(int locTouched) {
        this.locTouched = locTouched;
        totalLocTouched += locTouched;
    }

    /**
     * NUM revision : number of revisions
     */

    public int getNumRevisions() {
        return numRevisions;
    }

    public void setNumRevisions(int numRevisions) {
        this.numRevisions = numRevisions;
    }

    /***
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

    public float getAvgChgSetSize() {
        float avg;

        if (this.chgSetSize == 0 || this.numRevisions == 0) {
            avg = 0;
        } else {
            avg = (float)this.chgSetSize/this.numRevisions;
        }

        return (float) (Math.floor(avg * 100) / 100);
    }

    public void setMaxChgSetSize(int chgSetSize) {
        if (chgSetSize >= this.maxChgSetSize) this.maxChgSetSize = chgSetSize;
    }

    public int getMaxChgSetSize() {
        return this.maxChgSetSize;
    }

    /**
     * NAuth: number of authors
     */

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    /**
     * LOC deleted: lines deleted
     */

    public int getLocDeleted() {
        return locDeleted;
    }

    public void setLocDeleted(int locDeleted) {
        this.locDeleted = locDeleted;
    }

    /**
     * Churn: sum of added-deleted LOC in class
     */
    public int getChurn() {
        return churn;
    }

    public void setChurn(int churn) {
        this.churn = churn;
    }

    public float getAvgChurn() {
        float avg;

        if (this.churn == 0 || this.numRevisions == 0) {
            avg = 0;
        } else {
            avg = (float)this.churn/this.numRevisions;
        }

        return (float) (Math.floor(avg * 100) / 100);
    }

    public int getMaxChurn() {
        return this.maxChurn;
    }

    public void setMaxChurn(int churn) {
        if (churn >= this.maxChurn) this.maxChurn = churn;
    }


    /**
     * Age: age of the class in weeks
     */
    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getWeightedAge() {
        return weightedAge;
    }

    public void setWeightedAge(double weightedAge) {
        this.weightedAge = weightedAge;
    }

    /**
     * NFix: number of commits fix
     */
    public int getNFix() {
        return numFix;
    }

    public void setNFix(int i) {
        this.numFix = i;
    }

    public List<String> getTickets() {
        return tickets;
    }
}

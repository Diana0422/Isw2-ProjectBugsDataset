package org.example.logic.model.keyabstractions;

import org.example.logic.control.InspectionController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Release {

    private int index;
    private String id;
    private String name;
    private String tag;
    private LocalDateTime date;

    public Release(String id, String releaseName, LocalDateTime releaseDate) {
        this.id = id;
        this.name = releaseName;
        this.date = releaseDate;
        this.tag = "";
    }

    public static void setIndexNumbers(List<Release> versions) {
        /* set index numbers to the releases */
        for (int i = 0; i< versions.size(); i++) {
            Release rel = versions.get(i);
            rel.setIndex(i+1);
            if (InspectionController.isFullDebug()) {
                String log = rel.getName()+" - "+rel.getIndex()+" - "+rel.getDate();
                Logger.getGlobal().log(Level.WARNING, log);
            }
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime releaseDate) {
        this.date = releaseDate;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public static Release findVersionByDate(List<Release> versions, LocalDateTime created) {
        Release version = null;

        for (Release release : versions) {
            version = release;
            if (version.getDate().compareTo(created) >= 0) {
                return version;
            }
        }

        return version;
    }

    public static Release findVersionByIndex(List<Release> versions, int idx) {
        Release version = null;

        for (Release release : versions) {
            version = release;

            if (version.getIndex() == idx) {
                return version;
            }
        }

        return version;
    }

}
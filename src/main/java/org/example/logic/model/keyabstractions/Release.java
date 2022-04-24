package org.example.logic.model.keyabstractions;

import java.time.LocalDateTime;
import java.util.List;

public class Release {

    private int index;
    private String id;
    private String name;
    private LocalDateTime date;

    public Release(String id, String releaseName, LocalDateTime releaseDate) {
        this.id = id;
        this.name = releaseName;
        this.date = releaseDate;
    }

    public Release(int index, String id, String releaseName, LocalDateTime releaseDate) {
        this.index = index;
        this.id = id;
        this.name = releaseName;
        this.date = releaseDate;
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

    public static Release findVersionByDate(List<Release> versions, LocalDateTime created) {
        Release version = null;

        for (int i=0; i<versions.size(); i++) {
            version = versions.get(i);
            if (version.getDate().compareTo(created) >= 0) {
                version = versions.get(i);
                return version;
            }
        }

        return version;
    }

    public static Release findVersionByIndex(List<Release> versions, int idx) {
        Release version = null;

        for (int i=0; i<versions.size(); i++) {
            version = versions.get(i);

            if (version.getIndex() == idx) {
                return version;
            }
        }

        return version;
    }

    public static Release findReleaseFromId(String verId, List<Release> versions) {
        for (Release ver: versions) {
            if (ver.getId().equals(verId)) return ver;
        }
        return null;
    }

    public static int findIndexFromId(String verId, List<Release> versions) {
        for (Release ver: versions) {
            if (ver.getId().equals(verId)) {
                return ver.getIndex();
            }
        }

        return 0;
    }
}
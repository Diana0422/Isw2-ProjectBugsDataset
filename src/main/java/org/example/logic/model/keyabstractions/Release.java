package org.example.logic.model.keyabstractions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

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

    public Release(int index, String id, String releaseName, LocalDateTime releaseDate) {
        this.index = index;
        this.id = id;
        this.name = releaseName;
        this.date = releaseDate;
        this.tag = "";
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

    public static Release findVersionByTag(List<Release> versions, String tag) {
        /* get only the numeric name of the version from name */
        StringTokenizer st = new StringTokenizer(tag, "-");
        st.nextToken();
        String name = st.nextToken();

        for (Release rel: versions) {
            if (rel.getName().equals(name)) return rel;
        }
        return null;
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
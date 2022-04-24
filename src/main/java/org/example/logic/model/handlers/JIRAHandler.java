package org.example.logic.model.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.example.logic.model.keyabstractions.Commit;
import org.example.logic.model.keyabstractions.Issue;
import org.example.logic.model.keyabstractions.Project;
import org.example.logic.model.keyabstractions.Release;
import org.example.logic.model.utils.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JIRAHandler {

    private Project project;

    public JIRAHandler(Project project) {
        this.project = project;
    }


    public List<Issue> retrieveProjectIssues(String type) throws IOException {
        /***
         * retrieves project issue selecting a type
         * param type: the issue type selected
         */
        Integer i = 0;
        Integer j = 0;
        Integer total = 1;

        List<Issue> tickets = new ArrayList<>();

        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if >1000
            j = i + 1000;

            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + this.project.getProjName() + "%22AND%22issueType%22=%22"+type+"%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
                    + i.toString() + "&maxResults=" + j.toString();
            JSONObject json = readJsonFromUrl(url);

            JSONArray issues = json.getJSONArray("issues");
            total = json.getInt("total");

            for (; i < total && i < j; i++) {

                JSONObject fields = issues.getJSONObject(i%1000).getJSONObject("fields");

                /* get issue basic information */
                String key = issues.getJSONObject(i%1000).get("key").toString();
                String id = issues.getJSONObject(i%1000).getString("id");
                String opening = fields.getString("created").substring(0,10);
                String resolution = fields.getString("resolutiondate").substring(0,10);

                Issue issue = new Issue(project, key, id, Parser.getInstance().parseDateToLocalDateTime(resolution), Parser.getInstance().parseDateToLocalDateTime(opening));

                /* retrieve the affected versions of the issue */
                issue.setAffectedVersions(findVersions(fields, "versions"));
                /* retrieve the fixed versions of the issue */
                issue.setFixedVersions(findVersions(fields, "fixVersions"));
                /* set the opening version */
                issue.setOpVersion(Release.findVersionByDate(project.getVersions(), Parser.getInstance().parseDateToLocalDateTime(opening)));
                /* set the fixed version */
                issue.setFixVersion(Release.findVersionByDate(project.getVersions(), Parser.getInstance().parseDateToLocalDateTime(resolution)));
                tickets.add(issue);
            }

        } while (i < total);

        return tickets;
    }

    private List<Release> findVersions(JSONObject fields, String fieldType) {
        /***
         * find different types of versions of an issue from Jira
         * param fields: JSON Object field of the issue
         * param fieldType: type of the field to retrieve from Jira
         */
        String id;
        String name;
        int index;
        String date = null;
        List<Release> ver = new ArrayList<>();
        JSONArray versions = fields.getJSONArray(fieldType);

        for (int i=0; i<versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            if (version.getBoolean("released")) {
                id = version.getString("id");
                name = version.getString("name");
                date = version.getString("releaseDate");
                index = Release.findIndexFromId(id, this.project.getVersions());
                Release r = new Release(index, id, name, Parser.getInstance().parseDateToLocalDateTime(date));

                ver.add(r);
            }
        }

        return ver;
    }

    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))){
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } finally {
            is.close();
        }
    }

    private	String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }


    public Project getProject() {
        return project;
    }


    public void setProject(Project project) {
        this.project = project;
    }
}
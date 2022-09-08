package org.example.logic.model.handlers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.example.logic.model.keyabstractions.Issue;
import org.example.logic.model.keyabstractions.Project;
import org.example.logic.model.keyabstractions.Release;
import org.example.logic.model.utils.Parser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class that interacts with Jira API
 */
public class JIRAHandler {

    private final Project project;

    public JIRAHandler(Project project) {
        this.project = project;
    }


    /**
     * retrieves project issue selecting a type
     * @param type the issue type
     * @return the issues list
     * @throws IOException error retrieving issues
     */
    public List<Issue> retrieveProjectIssues(String type) throws IOException {
        int i = 0;
        int j;
        int total;

        List<Issue> tickets = new ArrayList<>();

        do {
            //Only gets a max of 1000 at a time, so must do this multiple times if >1000
            j = i + 1000;

            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + this.project.getProjName() + "%22AND%22issueType%22=%22"+type+"%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
                    + i + "&maxResults=" + j;
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
                tickets.add(issue);
            }

        } while (i < total);

        return tickets;
    }

    /**
     * find different types of versions of an issue from Jira
     * @param fields JSON Object field of the issue
     * @param fieldType type of the field to retrieve from Jira
     * @return the releases list
     */
    private List<Release> findVersions(JSONObject fields, String fieldType) {
        String date;
        List<Release> ver = new ArrayList<>();
        JSONArray versions = fields.getJSONArray(fieldType);

        for (int i=0; i<versions.length(); i++) {
            JSONObject version = versions.getJSONObject(i);
            /* consider the version only if has a release date and it's released */
            if (version.getBoolean("released") && version.has("releaseDate")) {
                date = version.getString("releaseDate");
                Release r = Release.findVersionByDate(project.getVersions(), Parser.getInstance().parseDateToLocalDateTime(date));
                ver.add(r);
            }
        }
        ver.sort(Comparator.comparing(Release::getDate)); // sort releases by date
        return ver;
    }

    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream();
             BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
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
}
package org.example.logic.model.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class Parser {

    private static Parser instance;

    private Parser() {}

    public static Parser getInstance() {
        if (instance == null) {
            instance = new Parser();
        }
        return instance;
    }

    public LocalDateTime parseDateToLocalDateTime(String string) {
        LocalDate date = LocalDate.parse(string);
        return date.atStartOfDay();
    }

    public String parseLocalDateToString(LocalDateTime date) {
        if (date != null) return date.toString().substring(0, 10);
        return null;
    }

    public String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (String s: list) {
            sb.append(s);
            sb.append(" ");
        }
        return sb.toString();
    }

    public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        }
    }

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
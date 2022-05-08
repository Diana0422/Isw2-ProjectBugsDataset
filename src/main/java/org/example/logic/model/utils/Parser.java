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
import java.util.StringTokenizer;

import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.login.CredentialNotFoundException;

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

    public String parseCompleteNameFromLogLine(String line) {
        /**
         * Extract the complete file from the git log fileline
         */
        /* extract only the name */
        StringTokenizer st = new StringTokenizer(line, "\t");
        st.nextToken();
        String complete = st.nextToken();
        return complete;
    }

    public String parseFilenameFromLogLine(String line, boolean rename) {
        /**
         * Extract the file name from the git log fileline
         */
        /* extract only the name */
        StringTokenizer st = new StringTokenizer(line, "\t");
        st.nextToken();
        if (rename) st.nextToken();
        String complete = st.nextToken();

        /* extract the filename from the complete name */
        String tok = "";
        StringTokenizer tokenizer = new StringTokenizer(complete, "/");
        while (tokenizer.hasMoreTokens()) {
            String nexttok = tokenizer.nextToken();
            if (nexttok.contains(".java")) return nexttok;
        }

        return tok;
    }

    public String parseFilepathFromLogLine(String line, boolean rename) {
        /**
         * Extract the filepath from the git log fileline
         */
        /* extract only the name */
        StringTokenizer st = new StringTokenizer(line, "\t");
        st.nextToken();
        if (rename) st.nextToken();
        String complete = st.nextToken();

        /* extract the filepath from the complete name */
        StringBuilder builder = new StringBuilder();
        StringTokenizer tokenizer = new StringTokenizer(complete, "/");
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            builder.append(tok);
            if (tokenizer.countTokens() != 0) builder.append("/");
        }

        return builder.toString();
    }

    public String[] parseFilepathRenameFromLogLine(String line) {
        /**
         * Extract the original filepath and the renamed one from the git log file line
         */
        String[] names = new String[2];
        StringTokenizer fileTokenizer = new StringTokenizer(line, "\t");
        fileTokenizer.nextToken();
        while (fileTokenizer.hasMoreTokens()) {
            String filepathOr = fileTokenizer.nextToken();
            String filepathNew = fileTokenizer.nextToken();
            names[0] =filepathOr;
            names[1] = filepathNew;
        }

        return names;
    }

    public String parseFilepathFromChangesLine(String line) {
        /**
         * Extract the actual filepath from the commit changes extracted line
         */
        StringTokenizer tokenizer = new StringTokenizer(line, "/");
        StringBuilder builder = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            if (tok.contains("{") && tok.contains("}")) {
                // prepare renaming part
                extractRenaming(tok, builder);
                continue;
            } else if (tok.contains("{")) {
                // prepare renaming part
                StringBuilder interBuilder = new StringBuilder();
                String interTok = "";
                interBuilder.append(tok);
                interBuilder.append("/");
                while (tokenizer.hasMoreTokens()) {
                    interTok = tokenizer.nextToken();
                    interBuilder.append(interTok);
                    if (!interTok.contains("}")) interBuilder.append("/");
                }
                String renaming = interBuilder.toString();
                extractRenaming(renaming, builder);
            } else {
                builder.append(tok);
                if (!tok.contains(".java")) builder.append("/");
            }
        }
        return builder.toString();
    }

    private void extractRenaming(String renaming, StringBuilder builder) {
        StringTokenizer internalTokenizer = new StringTokenizer(renaming, " ");
        String interTok = "";
        while (internalTokenizer.hasMoreTokens()) {
            interTok = internalTokenizer.nextToken();
        }
        int interTokLen = interTok.length();
        String dir = interTok.substring(0, interTokLen-1);
        builder.append(dir);
        if (!dir.contains(".java")) builder.append("/");
    }
}
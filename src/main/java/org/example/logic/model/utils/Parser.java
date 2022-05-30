package org.example.logic.model.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.StringTokenizer;

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
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
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

    public String parseFilepathFromChangesLine(String line, boolean old) {
        /**
         * Extract the actual filepath from the commit changes extracted line
         */
        StringTokenizer tokenizer = new StringTokenizer(line, "/");
        StringBuilder builder = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            String tok = tokenizer.nextToken();
            if (tok.contains("=>")) {
                // prepare renaming part
                extractRenaming(tok, builder, old);
            } else if (tok.contains("{")) {
                // prepare renaming part
                StringBuilder interBuilder = new StringBuilder();
                String interTok = "";
                interBuilder.append(tok);
                interBuilder.append("/");
                while (tokenizer.hasMoreTokens()) {
                    interTok = tokenizer.nextToken();
                    interBuilder.append(interTok);
                    if (!interTok.contains("}")) {
                        interBuilder.append("/");
                    } else {
                        break;
                    }
                }
                String renaming = interBuilder.toString();
                extractRenaming(renaming, builder, old);
            } else if (tok.contains("}")) {
                tok = tok.substring(0, tok.length()-1);
                builder.append(tok);
                if (!tok.contains(".java")) builder.append("/");
            } else {
                builder.append(tok);
                if (!tok.contains(".java")) builder.append("/");
            }
        }
        return builder.toString();
    }

    private void extractRenaming(String renaming, StringBuilder builder, boolean old) {
        if (!renaming.contains("}")) renaming = renaming + "}";
        StringTokenizer internalTokenizer = new StringTokenizer(renaming, " ");
        String interTok = "";
        while (internalTokenizer.hasMoreTokens()) {
            interTok = internalTokenizer.nextToken();
            if (old) break;
        }
        int interTokLen = interTok.length();
        String dir;
        if (old) {
            dir = interTok.substring(1, interTokLen);
        } else {
            dir = interTok.substring(0, interTokLen - 1);
        }
        builder.append(dir);
        if (!dir.contains(".java")) builder.append("/");
    }

    public String parseFilenameFromFilepath(String completeName) {
        StringTokenizer tokenizer = new StringTokenizer(completeName, "/");
        String tok = "";
        while (tokenizer.hasMoreTokens()) {
            tok = tokenizer.nextToken();
            if (tok.contains(".")) break; // end of the file path (encountered filename with extension)
        }
        return tok;
    }
}
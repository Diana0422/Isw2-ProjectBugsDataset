package org.example.logic.model.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
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

    /**
     * Extract the actual filepath from the commit changes extracted line
     * @param line line to parse
     * @param old specifies whether I'm interested to the old path or the new path
     * @return the parsed string
     */
    public String parseFilePathFromLine(String line, boolean old) {
        boolean splits = false;
        String firstSplit = "";
        StringTokenizer splitTokenize;
        String remainingPart = "";

        // If string doesn't contain "=>" then just return line, because the file was not renamed
        if (!line.contains("=>")) return line;

        if (line.contains("{")) {
            // Extract modified part from line
            StringTokenizer tokenizer = new StringTokenizer(line, "{");
            if (line.charAt(0) != '{') {
                firstSplit = tokenizer.nextToken();
                splits = true;
            }
            String modifiedPart = tokenizer.nextToken("}") + "}";

            // Extract remaining part
            int startIdx;
            int endIdx;
            if (splits) {
                String totalFirstSplit = firstSplit+modifiedPart;
                startIdx = totalFirstSplit.length();
            } else {
                startIdx = modifiedPart.length();
            }
            endIdx = line.length();
            remainingPart = line.substring(startIdx, endIdx);

            // tokenize modifiedPart on =>
            splitTokenize = new StringTokenizer(modifiedPart, "=>");
        } else {
            // tokenize all line on =>
            splitTokenize = new StringTokenizer(line, "=>");
        }

        String oldPathSpaced = splitTokenize.nextToken().replace("{", "");
        String newPathSpaced = splitTokenize.nextToken().replace("}", "");
        // remove useless spaces
        String oldPath = oldPathSpaced.substring(0, oldPathSpaced.length()-1);
        String newPath = newPathSpaced.substring(1);

        return produceOutputFilename(old, splits, oldPath, newPath, firstSplit, remainingPart);
    }

    private String produceOutputFilename(boolean old, boolean splits,
                                         String oldPath, String newPath, String firstSplit, String remainingPart) {
        if (old) {
            if (oldPath.equals("")) return firstSplit+remainingPart.substring(1);
            if (splits) return firstSplit+oldPath+remainingPart;
            return oldPath+remainingPart;
        } else {
            if (newPath.equals("")) return firstSplit+remainingPart.substring(1);
            if (splits) return firstSplit+newPath+remainingPart;
            return newPath+remainingPart;
        }
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
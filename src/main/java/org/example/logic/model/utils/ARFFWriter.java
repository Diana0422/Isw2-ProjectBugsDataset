package org.example.logic.model.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ARFFWriter {

    private static ARFFWriter instance;

    private ARFFWriter() {}

    public static ARFFWriter getInstance() {
        if (instance == null) {
            instance = new ARFFWriter();
        }
        return instance;
    }

    public void convertCvsToArff(String filename) {
        int nameLen = filename.length();
        String name = filename.substring(0, nameLen-3);
        String outname = name+"arff";

        try (BufferedReader reader = new BufferedReader(new FileReader(filename));
             FileWriter writer = new FileWriter(outname);
             BufferedReader fileReader = new BufferedReader(new FileReader(filename));
             BufferedReader verReader = new BufferedReader(new FileReader(filename))) {

            String dataname = "@relation "+name+" \n";
            writer.append(dataname);

            String filenames;
            filenames = buildSubset(fileReader, 2);

            String versions;
            versions = buildSubset(verReader, 1);

            String attributes = reader.readLine();
            writeAttributes(attributes, writer, filenames, versions);

            String rec;
            writer.append("@data\n");
            while ((rec = reader.readLine()) != null) {
                rec = rec.replace(";", ",");
                writer.append(rec).append("\n");
            }
        } catch (IOException e1) {
            Logger.getGlobal().log(Level.SEVERE, e1.getMessage());
            e1.printStackTrace();
        }

    }

    private String buildSubset(BufferedReader read, int attributeNum) throws IOException {
        List<String> subset = new ArrayList<>();
        StringBuilder sb = new StringBuilder("{");
        String line;
        String value=null;
        int countLines = 0;

        while ((line = read.readLine()) != null) {
            countLines++; // increment lines counter
            if (countLines == 1) continue;
            line = line.replace(";", ",");
            StringTokenizer st = new StringTokenizer(line, ",");
            int attNum = attributeNum;

            //select the right column
            while (attNum != 0) {
                value = st.nextToken();
                attNum--;
            }

            //uniquely add value to subset
            boolean check = true;
            for (String s : subset) {
                if (s.equals(value)) {
                    check = false;
                    break;
                }
            }

            if (check) {
                subset.add(value);
            }

        }

        for (int i=0; i<subset.size(); i++) {
            sb.append(subset.get(i));
            if (i != subset.size()-1) sb.append(",");
        }

        sb.append("}");
        return sb.toString();
    }



    /*
     * Writes dataset attributes on the arff file
     */

    private void writeAttributes(String attributes, FileWriter writer, String filenames, String versions) throws IOException {
        StringTokenizer tokenizer = new StringTokenizer(attributes.replace(";", ","), ",");
        String start = "@attribute ";
        String numend = " numeric\n";
        String labelend = " {Yes,No}\n";

        String releaseCol = tokenizer.nextToken().replace(" ", "_");
        String filenameCol = tokenizer.nextToken().replace(" ", "_");
        String attr1Col = tokenizer.nextToken().replace(" ", "_");
        String attr2Col = tokenizer.nextToken().replace(" ", "_");
        String attr3Col = tokenizer.nextToken().replace(" ", "_");
        String attr4Col = tokenizer.nextToken().replace(" ", "_");
        String attr5Col = tokenizer.nextToken().replace(" ", "_");
        String attr6Col = tokenizer.nextToken().replace(" ", "_");
        String attr7Col = tokenizer.nextToken().replace(" ", "_");
        String attr8Col = tokenizer.nextToken().replace(" ", "_");
        String attr9Col = tokenizer.nextToken().replace(" ", "_");
        String attr10Col = tokenizer.nextToken().replace(" ", "_");
        String attr11Col = tokenizer.nextToken().replace(" ", "_");
        String attr12Col = tokenizer.nextToken().replace(" ", "_");
        String attr13Col = tokenizer.nextToken().replace(" ", "_");
        String attr14Col = tokenizer.nextToken().replace(" ", "_");
        String attr15Col = tokenizer.nextToken().replace(" ", "_");
        String attr16Col = tokenizer.nextToken().replace(" ", "_");
        String labelCol = tokenizer.nextToken().replace(" ", "_");

        writer.append(start).append(releaseCol).append(" ").append(versions).append("\n");
        writer.append(start).append(filenameCol).append(" ").append(filenames).append("\n");
        writer.append(start).append(attr1Col).append(numend);
        writer.append(start).append(attr2Col).append(numend);
        writer.append(start).append(attr3Col).append(numend);
        writer.append(start).append(attr4Col).append(numend);
        writer.append(start).append(attr5Col).append(numend);
        writer.append(start).append(attr6Col).append(numend);
        writer.append(start).append(attr7Col).append(numend);
        writer.append(start).append(attr8Col).append(numend);
        writer.append(start).append(attr9Col).append(numend);
        writer.append(start).append(attr10Col).append(numend);
        writer.append(start).append(attr11Col).append(numend);
        writer.append(start).append(attr12Col).append(numend);
        writer.append(start).append(attr13Col).append(numend);
        writer.append(start).append(attr14Col).append(numend);
        writer.append(start).append(attr15Col).append(numend);
        writer.append(start).append(attr16Col).append(numend);
        writer.append(start).append(labelCol).append(labelend);
        writer.append("\n");

    }
}
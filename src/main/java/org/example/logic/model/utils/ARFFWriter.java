package org.example.logic.model.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

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

            String record;
            writer.append("@data\n");
            while ((record = reader.readLine()) != null) {
                record = record.replace(";", ",");
                writer.append(record+"\n");
            }
        } catch (FileNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

    }

    private String buildSubset(BufferedReader read, int attributeNum) throws IOException {
        List<String> subset = new ArrayList<>();
        StringBuilder sb = new StringBuilder("{");
        String line;
        String value=null;

        read.readLine();	//discard first line
        while ((line = read.readLine()) != null) {
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
            for (int i=0; i<subset.size(); i++) {
                if (subset.get(i).equals(value)) {
                    check = false;
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
        String labelCol = tokenizer.nextToken().replace(" ", "_");

        writer.append(start+releaseCol+" "+versions+"\n");
        writer.append(start+filenameCol+" "+filenames+"\n");
        writer.append(start+attr1Col+numend);
        writer.append(start+attr2Col+numend);
        writer.append(start+attr3Col+numend);
        writer.append(start+attr4Col+numend);
        writer.append(start+attr5Col+numend);
        writer.append(start+attr6Col+numend);
        writer.append(start+attr7Col+numend);
        writer.append(start+attr8Col+numend);
        writer.append(start+attr9Col+numend);
        writer.append(start+labelCol+labelend);
        writer.append("\n");

    }
}
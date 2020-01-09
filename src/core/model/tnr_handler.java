/*
 * This code is written by Genni Mansi.
 * Jan 2020
 */

/*
executes JavaScript code that runs command line prompts that
runs the TNR (Top-K NonRedundant Association Rules) algorithm on each of the databases.
-> Requirements: databases that were generated using main.js must be in the same
directory. "fileLocations.txt" must also be in the same directory.
-> Output: A series of files that are the output of the FPGrowth algorithm
that is used.
    - Each file has a title in the format of 'output' + numericValue + '.txt'.
       - Each file contains the FIs that were found along with the support count for
         each FI; following all the FIs are the XML files used to produce the database
         that is analyzed.
    - Note: These are different from 'output' + numbericValue + '_mod.txt'. The
         output from this step directly contains the result from the command line
         prompt, including support counts.
 */

/* This script is used to run the algorithm that mines the database. Essentially,
it runs the proper command line prompts to analyze the databases and output the
frequent itemsets */

/* Also For TNR algorithm,
 K = number of rules to be discovered
 CONFIDENCE = minimum confidence that association rules must have
 DELTA = parameter used to increase chances of having an exact results; must
 be a positive integer.
*/

package core.model;

import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FPMaxHandler {

    public static JsonObject analyzeDatabases (String projectPath, int k, double confidence, int delta) {

        String path = projectPath.concat("/LearningDR");
        File directory = new File(path);
        if (! directory.exists()){
            directory.mkdir();
        }



        // Path to directory with xml files we wish to iterate through
        // Currenlty, the directory is the directory main.js is running in
        File folder = new File(path);

        // Make a list of xml files
        List<String> fileList = new ArrayList<>();

        // analysisFileName + "_subClassOf_" + parentClass + ".txt"
        // analysisFileName = "AttributeEncoding"
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles != null) {
            for (File lof : listOfFiles) {
                if (lof.isFile() && lof.getName().startsWith("AttributeEncoding")) {
                    fileList.add(lof.getName());
                }
            }
        }

        // The way this map works is that the key is the .txt file name (database),
        // and the corresponding value is a list of the xml files used to create
        // the txt value.
        Map<String, List<String>> fileLocMap = new HashMap<>();
        // We read in all the Attribute Encoding database file names and the xml files
        // that were used to create them from 'fileLocations.txt'. This txt file
        // is structured such that each database (fileName ending in ''.txt') is
        // followed immediately by the list of 'xml' files used to create that
        // database.

        ArrayList<String> array = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(path + "/fileLocations.txt"));
            while (br.ready()) {
                array.add(br.readLine());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }


        String aeFile = null;
        for (String value : array) {

            // If it is the name of a database, then we want to keep it in this
            // local variable, and use it as the key in the map as we make a list
            // of the xml files that follow it
            if (value.endsWith(".txt")) {
                aeFile = value;
            }
            // If it is not a database name, then it is an xml file used to create
            // that database. Either (1) We have already recorded an xml file name
            // for this key (database name ); or (2) We have not yet read in/recorded
            // an xml file name for this key.

            // (1) If a list of xml files already exists for this
            // datbase, then we just append this file name to the end of the list
            else if (fileLocMap.containsKey(aeFile)) {
                if (!Objects.equals(value, "")) {
                    List<String> entry = fileLocMap.get(aeFile);
                    entry.add(value);
                    fileLocMap.put(aeFile, entry);
                }
            }
            // (2) However, if we have not yet recorded an xml file name for this key,
            // then we need to add this key to the map with a value that is a list
            // that contains this first xml file name.
            else {
                if (!Objects.equals(value, "")) {
                    List<String> str = new ArrayList<>();
                    str.add(value);
                    fileLocMap.put(aeFile, str);
                }
            }
        }

        // We want to analyze each of the files in this list
        for (int i = 0; i < fileList.size(); i++) {

            String[] command = new String[]{"java", "-jar",
                    projectPath + "/spmf.jar", "run", "TNR",
                    path + "/" + fileList.get(i),
                    path + "/TNR_output" + i + ".txt", k, confidence, delta};

            GeneralCommandLine generalCommandLine = new GeneralCommandLine(command);
            generalCommandLine.setCharset(StandardCharsets.UTF_8);

            try {
                ExecUtil.execAndGetOutput(generalCommandLine);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }

        JsonObject jsonObject = new JsonObject();

        // Add xml files that FI's apply to, to bottom of output file
        for (int i = 0; i < fileList.size(); i++) {

            // Add list of xml files to bottom of analysis file
            String f = path + "/output" + i + ".txt";
            PrintWriter pw = null;
            try {
                List<String> arr = fileLocMap.get(fileList.get(i));
                StringBuilder data = new StringBuilder();

                for (int j = 0; j < arr.size(); j++) {
                    data.append(arr.get(j)).append("\n");
                }

                pw = new PrintWriter(new FileOutputStream(new File(f), true ));
                pw.println(data.toString());
                pw.flush();
                pw.close();

            } catch (IOException e) {
                System.out.println("error in writing " + f);
            } finally {
                if (pw != null) {
                    pw.close();
                }
            }

            StringBuilder fpMaxOutput = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(path + "/output" + i + ".txt"));
                while (br.ready()) {
                    fpMaxOutput.append(br.readLine()).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            jsonObject.addProperty(Integer.toString(i), fpMaxOutput.toString());

        }

        return jsonObject;
    }
}

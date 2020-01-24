import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.*;

// A quick class to format some data as a JSONObject message that the web-client can process
class MessageProcessor {

    private static final String[] dataKeys = {"source", "destination", "command", "data"};
    private static final String[] xmlKeys = {"filePath", "xml"};
    private static final String[] newXmlKeys = {"filePath", "xml"};
    private static final String[] modifiedRuleKeys = {"ruleIndex", "rule"};
    private static final String[] modifiedTagKeys = {"tagName", "tag"};
    private static final String[] textXML = {"xmlText", "messageID"};
    private static final String[] fpMaxOutputKeys = {"fpMaxOutput"};
    private static final String[] tnrOutputKeys = {"tnrOutputKeys"};
    private static final String[] selectedFragmentKeys = {"path", "startOffset", "endOffset", "startLineOffset", "line", "lineText", "text"};
    private static final String[] dangerousReadMinedRules = {"outputFiles", "metaData"};


    static JsonObject encodeData(Object[] source_Destination_Protocol_Data_Array) {
        return createJsonObject(source_Destination_Protocol_Data_Array, dataKeys);
    }

    static JsonObject encodeXMLData(Object[] filepath_xml_Array) {
        return createJsonObject(filepath_xml_Array, xmlKeys);
    }

    static JsonObject encodeNewXMLData(Object[] filepath_newXml) {
        return createJsonObject(filepath_newXml, newXmlKeys);
    }

    static JsonObject encodeModifiedRule(Object[] ruleIndex_rule) {
        return createJsonObject(ruleIndex_rule, modifiedRuleKeys);
    }

    static JsonObject encodeXMLandText(Object[] xml_text) {
        return createJsonObject(xml_text, textXML);
    }

    static JsonObject encodeModifiedTag(Object[] tagName_tag) {
        return createJsonObject(tagName_tag, modifiedTagKeys);
    }

    static JsonObject encodeFPMaxOutput(Object[] fpMax_output) {
        return createJsonObject(fpMax_output, fpMaxOutputKeys);
    }

    static JsonObject encodeTNROutput(Object[] tnr_output) {
        return createJsonObject(tnr_output, tnrOutputKeys);
    }

    static JsonObject encodeSelectedFragment(Object[] selected_frag_data) {
        return createJsonObject(selected_frag_data, selectedFragmentKeys);
    }

    static JsonObject encodeDangerousMinedData(Object[] minedData) {
        return createJsonObject(minedData, dangerousReadMinedRules);
    }

    private static JsonObject createJsonObject(Object[] data_Array, String[] keys) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < keys.length; i++) {
            if (data_Array[i] instanceof String) {
                jsonObject.addProperty(keys[i], (String) data_Array[i]);
            } else if (data_Array[i] instanceof JsonObject) {
                jsonObject.add(keys[i], (JsonObject) data_Array[i]);
            }
        }
        return jsonObject;
    }


    /**
     * read rules from ruleJson.txt (the file where users modify rules)
     *
     * @return a list of the initial rules <index, rule text>
     */
    static List<List<String>> getInitialRulesAsList(Project project) {
        return getList("index", project);
    }

    /**
     * read rules from ruleJson.txt (the file where users modify rules)
     *
     * @param project open project in the IDE
     * @return a list of the initial rules <index, rule text>
     */
    static List<List<String>> getInitialTagsAsList(Project project) {
        return getList("tagName", project);
    }

    /**
     * read from json file
     * @param variable 'index' (rules) or 'fileName' (tags)
     * @param project open project in the IDE
     * @return List<List<String>>
     */
    private static List<List<String>> getList(String variable, Project project) {
        List<List<String>> itemList = new ArrayList<>();

        if (project.getBasePath() == null)
            return itemList;

        File file = new File(project.getBasePath());
        String ruleFilePath = "";
        switch (variable) {
            case "index":
                ruleFilePath = findJsonFile(file, "ruleJson.txt");
                break;
            case "tagName":
                ruleFilePath = findJsonFile(file, "tagJson.txt");
                break;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(ruleFilePath))) {

            String sCurrentLine;
            StringBuilder result = new StringBuilder();
            while ((sCurrentLine = br.readLine()) != null) {
                result.append(sCurrentLine).append('\n');
            }
            try {
                JSONArray allRules = new JSONArray(result.toString());
                for (int j = 0; j < allRules.length(); ++j) {
                    JSONObject itemI = allRules.getJSONObject(j);
                    switch (variable) {
                        case "index":
                            long ruleIndex = itemI.getInt("index");
                            itemList.add(new ArrayList<>(Arrays.asList(Long.toString(ruleIndex), itemI.toString())));
                            break;
                        case "tagName":
                            String tagName = itemI.getString("tagName");
                            itemList.add(new ArrayList<>(Arrays.asList(tagName, itemI.toString())));
                            break;
                    }
                }
            } catch (JSONException e) {
                System.out.println("error in parsing the json File");
                //e.printStackTrace();
            }

        } catch (IOException e) {
            System.out.println("No json file / error in reading the json file");
            //e.printStackTrace();
        }

        return itemList;
    }


    /**
     * @param directory of the project
     * @param fileName target json file
     * @return file path
     */
    private static String findJsonFile(File directory, String fileName) {
        File[] list = directory.listFiles();
        if (list != null)
            for (File file : list) {
                if (file.isDirectory()) {
                    findJsonFile(file, fileName);
                } else if (fileName.equalsIgnoreCase(file.getName())) {
                    return file.getPath();
                }
            }
        return "";
    }


}

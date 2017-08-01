import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import java.io.*;

// A quick class to format some data as a JSONObject message that the web-client can process
public class MessageProcessor {

    private static final String[] dataKeys = {"source", "destination", "command", "data"};
    private static final String[] xmlKeys = {"filePath", "xml"};

    static JsonObject encodeData(Object[] source_Destination_Protocol_Data_Array) {
        return createJsonObject(source_Destination_Protocol_Data_Array, dataKeys);
    }

    static JsonObject encodeXMLData(Object[] filepath_xml_Array) {
        return createJsonObject(filepath_xml_Array, xmlKeys);
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

    // returns a JSONObject with the initial rules from ruleJson.txt (the file where users modify rules)
    static JsonObject getIntitialRules() {
        System.out.println("(sendRulesInitially) " + "Send Rules initially");
        JsonObject data = new JsonObject();

        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        File file = new File(project.getBasePath());
        String ruleFilePath = findRuleJsonFile(file);

        try (BufferedReader br = new BufferedReader(new FileReader(ruleFilePath))) {

            String sCurrentLine, result = "";

            while ((sCurrentLine = br.readLine()) != null) {
                result = result + sCurrentLine + '\n';
            }
            data.addProperty("text", result);

        } catch (IOException e) {
            System.out.println("No ruleJson file / error in reading the ruleJson file");
            e.printStackTrace();
        }

        if (!data.has("text")) {
            data.addProperty("text", "");
        }
        return data;
    }

    private static String findRuleJsonFile(File directory) {
        File[] list = directory.listFiles();
        if (list != null)
            for (File file : list) {
                if (file.isDirectory()) {
                    findRuleJsonFile(file);
                } else if ("ruleJson.txt".equalsIgnoreCase(file.getName())) {
                    return file.getPath();
                }
            }
        return "";
    }


}

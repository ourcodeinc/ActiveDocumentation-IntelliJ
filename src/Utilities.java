/*
 * written by saharmehrpour
 * This class encompasses all static methods
 */

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class Utilities {

    /**
     * read rules from ruleJson.txt (the file where users modify rules)
     *
     * @param project open project in the IDE
     * @return a list of the initial rules <index, rule text>
     */
    static HashMap<String, String> getInitialRuleTable(Project project) {
        return getHashMap(Constants.RULE_TABLE_JSON, project, "index");
    }

    /**
     * read rules from ruleJson.txt (the file where users modify rules)
     *
     * @param project open project in the IDE
     * @return a hashMap of the initial rules <ruleID, rule text>
     */
    static HashMap<String, String> getInitialTagTable(Project project) {
        return getHashMap(Constants.TAG_TABLE_JSON, project, "ID");
    }

    /**
     * @param jsonFilePath relative path of tagTable.json or ruleTable.json
     * @param project      open project in the IDE
     * @param field        name of the property: ID for tagTable, index for ruleTable
     * @return HashMap
     */
    private static HashMap<String, String> getHashMap(String jsonFilePath, Project project, String field) {
        HashMap<String, String> items = new HashMap<>();

        if (project.getBasePath() == null)
            return items;

        File file = new File(project.getBasePath());
        String filePath = findJsonFile(file, jsonFilePath);

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {

            String sCurrentLine;
            StringBuilder result = new StringBuilder();
            while ((sCurrentLine = br.readLine()) != null) {
                result.append(sCurrentLine).append('\n');
            }
            try {
                JSONArray allItems = new JSONArray(result.toString());
                for (int j = 0; j < allItems.length(); ++j) {
                    JSONObject itemI = allItems.getJSONObject(j);
                    String itemIndex = itemI.getString(field);
                    items.put(itemIndex, itemI.toString());
                }
            } catch (JSONException e) {
                System.out.println("error in parsing the json File");
            }

        } catch (IOException e) {
            System.out.println("No json file / error in reading the json file: " + filePath);
        }
        return items;
    }

    /**
     * @param directory of the project
     * @param fileName  target json file
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

    /**
     * generate list of java file paths
     *
     * @param project it is required as in the constructor, project is needed to be provided
     * @return list of file paths in the project
     */
    static List<String> getFilePaths(Project project) {
        List<String> paths = new ArrayList<>();

        if (project.getBasePath() == null)
            return paths;
        VirtualFile rootDirectoryVirtualFile = LocalFileSystem.getInstance().findFileByPath(project.getBasePath());

        // set up queue
        List<VirtualFile> q = new ArrayList<>();
        q.add(rootDirectoryVirtualFile);

        // traverse the queue
        while (!q.isEmpty()) {
            VirtualFile item = q.get(0);
            q.remove(0);
            for (VirtualFile childOfItem : item.getChildren()) {
                if (childOfItem.isDirectory())
                    q.add(childOfItem);
                else if (childOfItem.getCanonicalPath() != null && childOfItem.getCanonicalPath().endsWith(".java")) {
                    paths.add(childOfItem.toString().substring(7)); // remove file:// from the beginning
                }
            }
        }

        return paths;
    }

    /**
     * write in file
     *
     * @param filePath either projectPath / tagTable.json
     */
    static void writeTableFile(String filePath, HashMap<String, String> hashObject) {
        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");
            writer.println('[');
            String prefix = "";
            for (String obj : hashObject.values()) {
                writer.print(prefix);
                writer.println(obj);
                prefix = ",";
            }
            writer.println(']');
            writer.close();
        } catch (IOException e) {
            System.out.println("error in writing " + filePath);
        }
    }

    /**
     * delete a directory recursively for deleting learningDR
     *
     * @param directoryToBeDeleted File
     */
    static void deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directoryToBeDeleted.delete();
    }
}

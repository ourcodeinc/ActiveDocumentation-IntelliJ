import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// A quick class to format some data as a JSONObject message that the web-client can process
public class MessageProcessor {

    private static final String[] dataKeys = {"source", "destination", "command", "data"};
    private static List<VirtualFile> ignoredFilesList = null;

    public static JsonObject encodeData(Object[] source_Destination_Protocol_Data_Array) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < dataKeys.length; i++) {
            if (source_Destination_Protocol_Data_Array[i] instanceof String) {
                jsonObject.addProperty(dataKeys[i], (String) source_Destination_Protocol_Data_Array[i]);
            } else if (source_Destination_Protocol_Data_Array[i] instanceof JsonObject) {
                jsonObject.add(dataKeys[i], (JsonObject) source_Destination_Protocol_Data_Array[i]);
            }
        }
        return jsonObject;
    }

    // returns a JSONObject with the initial rules from ruleJson.txt (the file where users modify rules)
    public static JsonObject sendRulesInitially() {
        System.out.println("(sendRulesInitially) " + "Send Rules initially");
        JsonObject data = new JsonObject();

        // start off with root
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        VirtualFile rootDirectoryVirtualFile = project.getBaseDir();

        // set up queue
        java.util.List<VirtualFile> q = new ArrayList<>();
        q.add(rootDirectoryVirtualFile);

        // traverse the queue
        while (!q.isEmpty()) {
            java.util.List<VirtualFile> new_q = new ArrayList<>();
            for (VirtualFile item : q) {
                //System.out.println("(sendRulesInitially) " + "Included: " + item.getCanonicalPath());
                for (VirtualFile childOfItem : item.getChildren()) {
                    new_q.add(childOfItem);
                    if (childOfItem.getCanonicalPath().endsWith("ruleJson.txt")) {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(childOfItem);
                        data.addProperty("text", psiFile.getText());
                        return data;
                    }
                }
            }
            q = new_q;
        }
        if (!data.has("text")) {
            data.addProperty("text", "");
        }
        return data;
    }

    // traverses the project hierarchy (the directories of the project that the user is working on)
    // when it hits a file that is not a directory, the getASTAsJSON function is called on that file
    // returns a JSON object that has the entire project hierarchy with individual files that have a property called 'ast'
    public static JsonObject generateProjectHierarchyAsJSON() {

        // start off with root
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        VirtualFile rootDirectoryVirtualFile = project.getBaseDir();

        // json version of root
        JsonObject jsonRootDirectory = new JsonObject();
        JsonObject properties = new JsonObject();
        properties.addProperty("canonicalPath", rootDirectoryVirtualFile.getCanonicalPath());
        properties.addProperty("parent", "");
        properties.addProperty("name", rootDirectoryVirtualFile.getNameWithoutExtension());
        properties.addProperty("isDirectory", true);
        jsonRootDirectory.add("children", new JsonArray());
        jsonRootDirectory.add("properties", properties);

        // set up a hashmap for the traversal
        HashMap<String, JsonObject> canonicalToJsonMap = new HashMap<>();
        canonicalToJsonMap.put(rootDirectoryVirtualFile.getCanonicalPath(), jsonRootDirectory);

        // set up queue
        java.util.List<VirtualFile> q = new ArrayList<>();
        q.add(rootDirectoryVirtualFile);

        // traverse the queue
        while (!q.isEmpty()) {
            java.util.List<VirtualFile> new_q = new ArrayList<>();
            for (VirtualFile item : q) {
                // System.out.println(item.getName());
                if (shouldIgnoreFile(item)) {
                    continue;
                }
                //System.out.println("(generateProjectHierarchyAsJSON) " + "Included: " + item.getCanonicalPath());
                for (VirtualFile childOfItem : item.getChildren()) {
                    if (shouldIgnoreFile(childOfItem)) {
                        continue;
                    }
                    new_q.add(childOfItem);
                    JsonObject jsonChildOfItem = new JsonObject();
                    JsonObject propertiesOfChild = new JsonObject();
                    if (childOfItem.isDirectory()) {
                        propertiesOfChild.addProperty("canonicalPath", childOfItem.getCanonicalPath());
                        propertiesOfChild.addProperty("parent", item.getCanonicalPath());
                        propertiesOfChild.addProperty("name", childOfItem.getNameWithoutExtension());
                        propertiesOfChild.addProperty("isDirectory", true);
                        jsonChildOfItem.add("children", new JsonArray());
                        jsonChildOfItem.add("properties", propertiesOfChild);
                    } else {
                        propertiesOfChild.addProperty("canonicalPath", childOfItem.getCanonicalPath());
                        propertiesOfChild.addProperty("parent", item.getCanonicalPath());
                        propertiesOfChild.addProperty("name", childOfItem.getNameWithoutExtension());
                        propertiesOfChild.addProperty("isDirectory", false);
                        propertiesOfChild.addProperty("fileType", childOfItem.getFileType().getName());
                        jsonChildOfItem.add("properties", propertiesOfChild);
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(childOfItem);

                        if (psiFile != null) {

                            propertiesOfChild.addProperty("text", psiFile.getText());
                            //propertiesOfChild.add("ast", generateASTAsJSON(psiFile));
                            propertiesOfChild.addProperty("fileName", psiFile.getName());
                        }
                    }
                    canonicalToJsonMap.get(item.getCanonicalPath()).get("children").getAsJsonArray().add(jsonChildOfItem);
                    canonicalToJsonMap.put(childOfItem.getCanonicalPath(), jsonChildOfItem);
                }
            }
            q = new_q;
        }

        System.out.println("(generateProjectHierarchyAsJSON) " + jsonRootDirectory);
        return jsonRootDirectory;

    }

    // checks if we should ignore a file
    public static boolean shouldIgnoreFile(VirtualFile s) {
        if (ignoredFilesList == null) {
            return false;
        }
        for (VirtualFile vfile : ignoredFilesList) {
            if (vfile.getCanonicalPath().equals(s.getCanonicalPath())) {
                return true;
            } else if (isFileAChildOf(s, vfile)) {
                return true;
            }
        }

        return false;
    }

    // determines if one file/directory is stored somewhere down the line in another directory
    private static boolean isFileAChildOf(VirtualFile maybeChild, VirtualFile possibleParent) {
        final VirtualFile parent = possibleParent.getCanonicalFile();
        if (!parent.exists() || !parent.isDirectory()) {
            // this cannot possibly be the parent
            return false;
        }

        VirtualFile child = maybeChild.getCanonicalFile();
        while (child != null) {
            if (child.equals(parent)) {
                return true;
            }
            child = child.getParent();
        }
        // No match found, and we've hit the root directory
        return false;
    }


    // generates the list of files/folders to ignore
    public static void getIgnoredFilesList(VirtualFile bDir) {

        List<String> list = new ArrayList<>();
        list.add(".idea");
        list.add("out");
        list.add("website-client");

        List<VirtualFile> set = new ArrayList<>();

        for (String item : list) {
            VirtualFile vfile = bDir.findFileByRelativePath(item);
            if (vfile != null) {
                set.add(vfile);
            }
        }

        ignoredFilesList = set;
    }

}

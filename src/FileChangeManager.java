import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.MessageBusConnection;
import core.model.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;

public class FileChangeManager implements ProjectComponent {

    private final MessageBusConnection connection;
    private ChatServer ws;
    private SRCMLxml srcml;
    private HashMap<Long,String> ruleTable; // ruleID, {ID: longNumber, ...}
    private HashMap<Long,String> tagTable; // tagID, {ID: longNumber, ...}
    private Project currentProject;
    String projectPath;
    private List<VirtualFile> ignoredFiles;

    FileChangeManager(Project project) {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        currentProject = project;
        projectPath = project.getBasePath();

        srcml = new SRCMLxml(FileChangeManager.getFilePaths(project), project.getBasePath());
        SRCMLHandler.createXMLForProject(srcml);

        tagTable = getInitialTagTable(project);
        ruleTable = getInitialRuleTable(project);
    }

    /**
     * update a tag in tagTable
     * @param ruleID the ID of an existing rule
     * @param updatedRuleInfo the tag information that is stored in ruleTable.json
     * @return false if no ID is found
     */
    private boolean updateRule (long ruleID, String updatedRuleInfo) {
        if (this.ruleTable.get(ruleID) == null) return false;
        this.ruleTable.put(ruleID, updatedRuleInfo);
        return true;
    }

    /**
     * add a new tag in tagTable
     * @param newRuleID the new and unique ID
     * @param newRuleInfo the tag information that is stored in ruleTable.json
     * @return false if the ID exists in the table
     */
    private boolean addNewRule(long newRuleID, String newRuleInfo) {
        if (this.tagTable.get(newRuleID) != null) return false;
        this.tagTable.put(newRuleID, newRuleInfo);
        return true;
    }

    /**
     * update a tag in tagTable
     * @param tagID the ID of an existing tag
     * @param updatedTagInfo the tag information that is stored in tagTable.json
     * @return false if no ID is found
     */
    private boolean updateTag (long tagID, String updatedTagInfo) {
        if (this.tagTable.get(tagID) == null) return false;
        this.tagTable.put(tagID, updatedTagInfo);
        return true;
    }

    /**
     * add a new tag in tagTable
     * @param newTagID the new and unique ID
     * @param newTagInfo the tag information that is stored in tagTable.json
     * @return false if the ID exists in the table
     */
    private boolean addNewTag(long newTagID, String newTagInfo) {
        if (this.tagTable.get(newTagID) != null) return false;
        this.tagTable.put(newTagID, newTagInfo);
        return true;
    }

    /**
     * get the string of all rules to send to the client
     * @return string
     */
    String getRuleTable() {
        StringBuilder ruleTableString = new StringBuilder("[");
        String prefix = "";
        for (String tag : this.ruleTable.values()) {
            ruleTableString.append(prefix);
            ruleTableString.append(tag);
            prefix = ",";
        }
        return ruleTableString + "]";
    }

    /**
     * get the string of all tags to send to the client
     * @return string
     */
    String getTagTable() {
        StringBuilder tagTableString = new StringBuilder("[");
        String prefix = "";
        for (String tag : this.tagTable.values()) {
            tagTableString.append(prefix);
            tagTableString.append(tag);
            prefix = ",";
        }
        return tagTableString + "]";
    }

    /**
     * @return srcml object
     */
    SRCMLxml getSrcml() {
        return srcml;
    }


    /**
     * read rules from ruleJson.txt (the file where users modify rules)
     * @param project open project in the IDE
     * @return a list of the initial rules <index, rule text>
     */
    static HashMap<Long,String> getInitialRuleTable(Project project) {
        return getHashMap("ruleTable.json", project);
    }

    /**
     * read rules from ruleJson.txt (the file where users modify rules)
     * @param project open project in the IDE
     * @return a hashMap of the initial rules <ruleID, rule text>
     */
    static HashMap<Long,String> getInitialTagTable(Project project) {
        return getHashMap("tagTable.json", project);
    }

    /**
     *
     * @param jsonFilePath relative path of tagTable.json or ruleTable.json
     * @param project open project in the IDE
     * @return HashMap
     */
    private static HashMap<Long,String> getHashMap(String jsonFilePath, Project project) {
        HashMap<Long,String> items = new HashMap<>();

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
                    long itemIndex = itemI.getInt("ID");
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

    public void initComponent() {
        ignoredFiles = createIgnoredFileList(currentProject);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {

                // update rules in ChatServer
                for (VFileEvent event : events) {
                    if (event.getFile() != null && shouldIgnoreFile(event.getFile()))
                        continue;
                    String eventType = "";
                    if (event instanceof VFileCreateEvent) { // Create files
                        eventType = "CREATE";
                    } else if (event instanceof VFileContentChangeEvent) { // Text Change
                        eventType = "TEXT_CHANGE";
                    } else if (event instanceof VFileDeleteEvent) { // Delete files
                        eventType = "DELETE";
                    } else if (event instanceof VFilePropertyChangeEvent) { // Property Change
                        eventType = "PROPERTY_CHANGE";
                    }

                    handleEvents(event, eventType);
                }
            }
        });

        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                try {
                    if (event.getManager().getSelectedFiles().length > 0)
                        if (Objects.requireNonNull(event.getManager().getSelectedFiles()[0].getCanonicalFile()).getName().endsWith(".java")) {
                            ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FILE_CHANGE", event.getManager().getSelectedFiles()[0].getPath()}).toString());
                        }
                } catch (NullPointerException e){ System.out.println("error happened in finding the changed file.");}
            }

        });
    }

    /**
     * Overridden method
     */
    public void disposeComponent() {
        ruleTable = new HashMap<>();
        tagTable = new HashMap<>();

        Project[] AllProjects = ProjectManager.getInstance().getOpenProjects();
        if (AllProjects.length == 0) {
            try {
                if (ws != null)
                    ws.stop();
            } catch (IOException | InterruptedException e) {
                System.out.println(e);
            }
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "";
    }

    @Override
    public void projectOpened() {
        Project[] AllProjects = ProjectManager.getInstance().getOpenProjects();
        if (AllProjects.length == 1) {
            try {
                ws = new ChatServer(8887);
                ws.setManager(this);
                ws.start();
            } catch (Exception e) {
                System.out.println("Error in creating a Chat server.");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void projectClosed() { }

    /**
     * process the message received from the client
     * @param messageAsJson JsonObject
     */
    void processReceivedMessages(JsonObject messageAsJson) {

        String command = messageAsJson.get("command").getAsString();

        switch (command) {
            case "XML_RESULT":
                try {
                    PrintWriter writer = new PrintWriter(projectPath + "/tempResultXmlFile.xml", "UTF-8");
                    writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                    writer.println(messageAsJson.get("data").getAsJsonObject().get("xml").getAsString());
                    writer.close();
                } catch (IOException e) {
                    System.out.println("error in writing the result xml");
                    return;
                }

                EventQueue.invokeLater(() -> {
                    String fileRelativePath = messageAsJson.get("data").getAsJsonObject().get("fileName").getAsString();
                    String relativePath = fileRelativePath.startsWith("/") ? fileRelativePath : "/" + fileRelativePath;
                    VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(relativePath);
                    if (fileByPath != null) {
                        FileEditorManager.getInstance(currentProject).openFile(fileByPath, true);
                        Editor theEditor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
                        int indexToFocusOn = SRCMLHandler.findLineNumber(projectPath + "/tempResultXmlFile.xml");
                        if (theEditor != null) {
                            theEditor.getCaretModel().moveToOffset(indexToFocusOn);
                            theEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                        }
                    }
                });

            case "MODIFIED_RULE":
                // data: {ruleID: longNumber, ruleInfo: {...}}
                long ruleID = messageAsJson.get("data").getAsJsonObject().get("ruleID").getAsLong();
                String ruleInfo = messageAsJson.get("data").getAsJsonObject().get("ruleInfo").getAsJsonObject().toString();

                boolean ruleResult = this.updateRule(ruleID, ruleInfo);
                if (!ruleResult)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_UPDATE_RULE", messageAsJson.get("data")}).toString());

                this.writeTableFile(this.projectPath + "/" + "ruleTable.json", this.tagTable);
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE", messageAsJson.get("data")}).toString());

                break;

            case "MODIFIED_TAG":
                // data: {tagID: longNumber, tagInfo: {...}}
                long tagID = messageAsJson.get("data").getAsJsonObject().get("tagID").getAsLong();
                String tagInfo = messageAsJson.get("data").getAsJsonObject().get("tagInfo").getAsJsonObject().toString();

                boolean result = this.updateTag(tagID, tagInfo);
                if (!result)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_UPDATE_TAG", messageAsJson.get("data")}).toString());

                this.writeTableFile(this.projectPath + "/" + "tagTable.json", this.tagTable);
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_TAG", messageAsJson.get("data")}).toString());

                break;


            case "EXPR_STMT":
                String exprText = messageAsJson.get("data").getAsJsonObject().get("codeText").getAsString();
                String resultExprXml = SRCMLHandler.createXMLForText(exprText, projectPath + "/tempExprFile.java");
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "EXPR_STMT_XML",
                        MessageProcessor.encodeXMLandText(new Object[]{resultExprXml, messageAsJson.get("data").getAsJsonObject().get("messageID").getAsString()})
                }).toString());

                break;

            case "NEW_RULE":
                // data: {ruleID: longNumber, ruleInfo: {...}}
                long newRuleID = messageAsJson.get("data").getAsJsonObject().get("ruleID").getAsLong();
                String newRuleInfo = messageAsJson.get("data").getAsJsonObject().get("ruleInfo").getAsJsonObject().toString();

                boolean newRuleResult = this.addNewRule(newRuleID, newRuleInfo);
                if (!newRuleResult)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_NEW_RULE", messageAsJson.get("data")}).toString());

                this.writeTableFile(this.projectPath + "/" + "ruleTable.json", this.tagTable);
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_RULE", messageAsJson.get("data")}).toString());

                break;

            case "NEW_TAG":
                // data: {tagID: longNumber, tagInfo: {...}}
                long newTagID = messageAsJson.get("data").getAsJsonObject().get("tagID").getAsLong();
                String newTagInfo = messageAsJson.get("data").getAsJsonObject().get("tagInfo").getAsJsonObject().toString();

                boolean newResult = this.addNewTag(newTagID, newTagInfo);
                if (!newResult)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_NEW_TAG", messageAsJson.get("data")}).toString());

                this.writeTableFile(this.projectPath + "/" + "tagTable.json", this.tagTable);
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_TAG", messageAsJson.get("data")}).toString());

                break;

                /*  mining rules  */

            case "LEARN_RULES_META_DATA":
                // "attribute_META_data.txt"

                // assumption: the first received data is the meta data
                String path = projectPath.concat("/LearningDR");
                File directory = new File(path);
                this.deleteDirectory(directory);

            case "LEARN_RULES_FILE_LOCATIONS":
                // "fileLocations.txt"

            case "LEARN_RULES_DATABASES":
                // analysisFileName + "_subClassOf_" + parentClass + ".txt"
                // analysisFileName = "AttributeEncoding"

                JsonArray filePathData = messageAsJson.get("data").getAsJsonArray();
                for (int i=0; i < filePathData.size(); i++) {
                    writeDataToFileLearningDR(filePathData.get(i).getAsJsonArray().get(0).getAsString(),
                            filePathData.get(i).getAsJsonArray().get(1).getAsString(), false);
                }
                break;

            case "LEARN_RULES_META_DATA_APPEND":
                // assumption: the first received data is the meta data
                int cnt = messageAsJson.get("part").getAsInt();
                if (cnt == 0) {
                    String dirPath = projectPath.concat("/LearningDR");
                    File directoryFile = new File(dirPath);
                    this.deleteDirectory(directoryFile);
                }

            case "LEARN_RULES_FILE_LOCATIONS_APPEND":
            case "LEARN_RULES_DATABASES_APPEND":
                JsonArray filePathDataAppend = messageAsJson.get("data").getAsJsonArray();
                for (int i=0; i < filePathDataAppend.size(); i++) {
                    writeDataToFileLearningDR(filePathDataAppend.get(i).getAsJsonArray().get(0).getAsString(),
                            filePathDataAppend.get(i).getAsJsonArray().get(1).getAsString(), true);
                }
                break;

            case "EXECUTE_FP_MAX":
                int support = messageAsJson.get("data").getAsInt();
                JsonObject outputContent = FPMaxHandler.analyzeDatabases(projectPath, support);
                // send message
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FP_MAX_OUTPUT",
                        MessageProcessor.encodeFPMaxOutput(new Object[]{outputContent})
                }).toString());
                break;

            case "EXECUTE_TNR":
                int k = messageAsJson.get("data").getAsJsonObject().get("k").getAsInt();
                double confidence = messageAsJson.get("data").getAsJsonObject().get("confidence").getAsDouble();
                int delta = messageAsJson.get("data").getAsJsonObject().get("delta").getAsInt();
                JsonObject outputContentTNR = TNRHandler.analyzeDatabases_tnr(projectPath, k, confidence, delta);
                // send message
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "TNR_OUTPUT",
                        MessageProcessor.encodeTNROutput(new Object[]{outputContentTNR})
                }).toString());
                break;

            case "DANGEROUS_READ_MINED_RULES":
                JsonObject outputContentMinedData = MiningRulesUtilities.readMinedRulesFile(projectPath);
                if (outputContentMinedData == null) {
                    System.out.println("Error happened in reading files.");
                    break;
                }
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "DANGEROUS_READ_MINED_RULES",
                        MessageProcessor.encodeDangerousMinedData(
                                new Object[]{outputContentMinedData.get("output").toString(), outputContentMinedData.get("metaData").toString()}
                        )}).toString());
                break;

        }

    }

    /**
     * handle events based on event types.
     * @param event VFileEvent
     * @param eventType one of CREATE, TEXT_CHANGE, PROPERTY_CHANGE, DELETE
     */
    private void handleEvents(VFileEvent event, String eventType) {
        VirtualFile file = event.getFile();
        if (file == null) return;

        switch (eventType) {
            case "CREATE":
            case "DELETE":
                if (file.getName().equals("ruleTable.json")) {
                    updateRules();
                    return;
                }
                if (file.getName().equals("tagTable.json")) {
                    updateTags();
                    return;
                }

                // do not handle if the file is not a part of the project
                if (shouldIgnoreEvent(file))
                    return;

                String newXml = eventType.equals("CREATE") ? SRCMLHandler.addXMLForProject(this.getSrcml(), file.getPath()) : "";
                if (eventType.equals("DELETE"))
                    SRCMLHandler.removeXMLForProject(srcml, file.getPath());

                this.updateXML(file.getPath(), newXml);

                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());
                break;

            case "TEXT_CHANGE":
                if (file.getName().equals("ruleTable.json")) {
                    System.out.println("ruleTable.json modified.");
                    updateRules();
                    return;
                }
                if (file.getName().equals("tagTable.json")) {
                    System.out.println("tagTable.json modified.");
                    updateTags();
                    return;
                }

                // do not handle if the file is not a part of the project
                if (shouldIgnoreEvent(file))
                    return;

                String updatedXml = SRCMLHandler.updateXMLForProject(this.getSrcml(), file.getPath());
                this.updateXML(file.getPath(), updatedXml);
                break;

            case "PROPERTY_CHANGE":
                // when a file's properties change. for instance, if the file is renamed.
                List<String> newPaths = getFilePaths(currentProject);

                for (String path : srcml.getPaths()) {
                    if (!newPaths.contains(path)) {

                        SRCMLHandler.removeXMLForProject(srcml, path);
                        String changedXml = SRCMLHandler.addXMLForProject(srcml, file.getPath());
                        this.updateXML(file.getPath(), changedXml);
                        break;
                    }
                }
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());
                break;
        }
    }

    /**
     * checks if we should ignore a file
     *
     * @param s virtual file
     * @return true/false
     */
    private boolean shouldIgnoreFile(VirtualFile s) {
        return !(s.getName().endsWith(".java") || s.getName().endsWith("ruleTable.json") || s.getName().endsWith("tagTable.json"));
    }

    /**
     * check whether a file is a child of another file
     * formerly this method was in utilities
     * @param maybeChild first file
     * @param possibleParent second file
     * @return boolean
     */
    private static boolean isFileAChildOf(VirtualFile maybeChild, VirtualFile possibleParent) {
        final VirtualFile parent = possibleParent.getCanonicalFile();
        if (parent != null && (!parent.exists() || !parent.isDirectory())) {
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

    /**
     * creating ignore list
     * formerly this method was in utilities
     * @param project for a given project
     * @return list of files
     */
    private static List<VirtualFile> createIgnoredFileList(Project project) {
        List<VirtualFile> ignoredFiles = new ArrayList<>();
        List<String> files = new ArrayList<>(Arrays.asList(".idea", "out", "source_xml.xml", "tempResultXmlFile.xml", "LearningDR", "test",
                "testProject.iml", ".DS_Store", "bin", "build", "node_modules", ".setting", ".git", "war", "tempExprDeclFile.java"));
        if (project.getBasePath() == null)
            return ignoredFiles;
        VirtualFile rootDirectoryVirtualFile = LocalFileSystem.getInstance().findFileByPath(project.getBasePath());
        if (rootDirectoryVirtualFile == null)
            return ignoredFiles;
        for (String f : files) {
            VirtualFile vfile = rootDirectoryVirtualFile.findFileByRelativePath(f);
            if (vfile != null) {
                ignoredFiles.add(vfile);
            }
        }
        return ignoredFiles;
    }

    /**
     * check whether we should consider the file change
     *
     * @param file VirtualFile
     * @return boolean
     */
    private boolean shouldIgnoreEvent(VirtualFile file) {

        if (file.isDirectory() || file.getCanonicalPath() == null)
            return true;
        if (!file.getCanonicalPath().endsWith(".java"))
            return true;

        PsiFile psiFile = PsiManager.getInstance(currentProject).findFile(file);
        return psiFile == null || !PsiManager.getInstance(currentProject).isInProject(psiFile);

    }

    /**
     * generate list of java file paths
     * @param project it is required as in the constructor, project is needed to be provided
     * @return list of file paths in the project
     */
    private static List<String> getFilePaths(Project project) {
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
     * check whether the given project is the same as the project for which
     * srcml is created. It updates the srcml and project path accordingly.
     */
    void checkChangedProject() {

        if (!this.srcml.getProjectPath().equals(projectPath)) {
            SRCMLxml srcml = new SRCMLxml(FileChangeManager.getFilePaths(currentProject), projectPath);
            SRCMLHandler.createXMLForProject(srcml);
            System.out.println("XML data is created.");

            this.srcml = srcml;

        }
    }


    /**
     * update the ruleIndexText and send messages to clients
     */
    private void updateRules() {
        this.ruleTable = getInitialRuleTable(currentProject);

        // send the message
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "RULE_TABLE", this.getRuleTable()}).toString());
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "VERIFY_RULES", ""}).toString());
    }

    /**
     * update the tagNameText and send messages to clients
     */
    private void updateTags() {
        this.tagTable = getInitialTagTable(currentProject);

        // send the message
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "TAG_TABLE", this.getTagTable()}).toString());
    }

    /**
     * change the xml data and inform clients
     * @param filePath String
     * @param newXml   String
     */
    private void updateXML(String filePath, String newXml) {
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_XML",
                MessageProcessor.encodeNewXMLData(new Object[]{filePath, newXml})
        }).toString());
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "CHECK_RULES_FOR_FILE", filePath}).toString());
    }

    /**
     * Copied from master branch
     *
     * @return json
     */
    JsonObject generateProjectHierarchyAsJSON() {

        Project project = ProjectManager.getInstance().getOpenProjects()[0];

        if (project.getBasePath() == null)
            return new JsonObject();
        VirtualFile rootDirectoryVirtualFile = LocalFileSystem.getInstance().findFileByPath(project.getBasePath());

        // json version of root
        JsonObject jsonRootDirectory = new JsonObject();
        JsonObject properties = new JsonObject();
        if (rootDirectoryVirtualFile != null) {
            properties.addProperty("canonicalPath", rootDirectoryVirtualFile.getCanonicalPath());
        }
        properties.addProperty("parent", "");
        if (rootDirectoryVirtualFile != null) {
            properties.addProperty("name", rootDirectoryVirtualFile.getNameWithoutExtension());
        }
        properties.addProperty("isDirectory", true);
        jsonRootDirectory.add("children", new JsonArray());
        jsonRootDirectory.add("properties", properties);

        // set up a hashmap for the traversal
        HashMap<String, JsonObject> canonicalToJsonMap = new HashMap<>();
        if (rootDirectoryVirtualFile != null) {
            canonicalToJsonMap.put(rootDirectoryVirtualFile.getCanonicalPath(), jsonRootDirectory);
        }

        // set up queue
        java.util.List<VirtualFile> q = new ArrayList<>();
        q.add(rootDirectoryVirtualFile);

        // traverse the queue
        while (!q.isEmpty()) {
            java.util.List<VirtualFile> new_q = new ArrayList<>();
            for (VirtualFile item : q) {

                if (shouldIgnoreFileForProjectHierarchy(item)) continue;

                for (VirtualFile childOfItem : item.getChildren()) {
                    if (shouldIgnoreFileForProjectHierarchy(childOfItem)) continue;

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

                        if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
                            PsiFile psiFile = ApplicationManager.getApplication().runReadAction(
                                    (Computable<PsiFile>) () -> PsiManager.getInstance(project).findFile(childOfItem));
                            if (psiFile != null)
                                propertiesOfChild.addProperty("fileName", psiFile.getName());
                        }
                    }
                    canonicalToJsonMap.get(item.getCanonicalPath()).get("children").getAsJsonArray().add(jsonChildOfItem);
                    canonicalToJsonMap.put(childOfItem.getCanonicalPath(), jsonChildOfItem);
                }
            }
            q = new_q;
        }
        return jsonRootDirectory;
    }

    /**
     * verify if a file should be ignored, useful for project hierarchy
     * @param s virtual file
     * @return boolean
     */
    private boolean shouldIgnoreFileForProjectHierarchy(VirtualFile s) {
        if (ignoredFiles == null) {
            return false;
        }
        for (VirtualFile vfile : ignoredFiles) {
            if (Objects.equals(vfile.getCanonicalPath(), s.getCanonicalPath())) {
                return true;
            } else if (isFileAChildOf(s, vfile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * write in file
     * @param filePath either projectPath / tagTable.json
     */
    static void writeTableFile(String filePath, HashMap<Long, String> hashObject) {

        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");
            writer.println('[');
            String prefix = "";
            for (String obj : hashObject.values()) {
                writer.println(prefix);
                writer.print(obj);
                prefix = ",";
            }
            writer.println(']');
            writer.close();
        } catch (IOException e) {
            System.out.println("error in writing " + filePath);
        }

    }

    /**
     * write in file
     * @param fileName name of files
     * @param content content of the file
     */
    void writeDataToFileLearningDR(String fileName, String content, boolean append) {

        String directoryName = projectPath.concat("/LearningDR");

        File directory = new File(directoryName);
        if (! directory.exists()){
            directory.mkdir();
        }

        try {
            PrintWriter writer;
            if (append) {
                writer = new PrintWriter(new FileOutputStream(
                        new File(projectPath + "/LearningDR/" + fileName), true /* append = true */));
            }
            else {
                writer = new PrintWriter(projectPath + "/LearningDR/" + fileName, "UTF-8");
            }
            writer.print(content);
            writer.close();
        } catch (IOException e) {
            System.out.println("error in writing " + fileName);
        }

    }

    /**
     * sending the data for feature selection to the client
     * @param path of the file from which a code snippet is selected
     * @param startIndex start of the selection
     * @param endIndex end of the selection
     * @param startLineOffset offset of the start of the selection w.r.t. start of the line
     * @param lineNumber of the javaFile
     * @param lineText code at line of the selection
     * @param text text of the selection
     */
    void sendFeatureSelectionData(String path, String startIndex, String endIndex, String startLineOffset, String lineNumber, String lineText, String text) {
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FEATURE_SELECTION",
                MessageProcessor.encodeSelectedFragment(new Object[]{path, startIndex, endIndex, startLineOffset, lineNumber, lineText, text})
        }).toString());
    }

    /**
     * delete a directory recursively for deleting learningDR
     * @param directoryToBeDeleted File
     * @return boolean
     */
    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }
}
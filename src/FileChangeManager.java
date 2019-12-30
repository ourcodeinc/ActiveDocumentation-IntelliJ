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
import core.model.FPMaxHandler;
import core.model.SRCMLHandler;
import core.model.SRCMLxml;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.*;


public class FileChangeManager implements ProjectComponent {

    private final MessageBusConnection connection;
    private ChatServer ws;
    private SRCMLxml srcml;
    private List<List<String>> ruleIndexText; // index - text
    private List<List<String>> tagNameText; // tagName - text
    private Project currentProject;
    String projectPath;
    private List<VirtualFile> ignoredFiles;

    FileChangeManager(Project project) {

        connection = ApplicationManager.getApplication().getMessageBus().connect();
        currentProject = project;
        projectPath = project.getBasePath();

        srcml = new SRCMLxml(FileChangeManager.getFilePaths(project), project.getBasePath());
        SRCMLHandler.createXMLForProject(srcml);

        ruleIndexText = MessageProcessor.getInitialRulesAsList(project);
        tagNameText = MessageProcessor.getInitialTagsAsList(project);

    }


    /**
     * find the corresponding rule for ruleIndex and update it
     *
     * @param ruleIndex String received from client
     * @param ruleText  String received from client
     */
    private void setRuleIndexText(String ruleIndex, String ruleText) {
        for (int i = 0; i < this.ruleIndexText.size(); i++) {
            if (this.ruleIndexText.get(i).get(0).equals(ruleIndex)) {
                this.ruleIndexText.set(i, new ArrayList<>(Arrays.asList(ruleIndex, ruleText)));
                return;
            }
        }

        // new Rule
        this.ruleIndexText.add(new ArrayList<>(Arrays.asList(ruleIndex, ruleText)));

    }


    /**
     * find the corresponding tag for tagName and update it
     *
     * @param tagName String received from client
     * @param tagText String received from client
     */
    private void setTagNameText(String tagName, String tagText) {
        for (int i = 0; i < this.tagNameText.size(); i++) {
            if (this.tagNameText.get(i).get(0).equals(tagName)) {
                this.tagNameText.set(i, new ArrayList<>(Arrays.asList(tagName, tagText)));
                return;
            }
        }
        this.tagNameText.add(new ArrayList<>(Arrays.asList(tagName, tagText)));
    }


    /**
     * get the string of all rules to send to the client
     *
     * @return string
     */
    String getAllRules() {
        StringBuilder allRules = new StringBuilder("[");
        for (int i = 0; i < this.ruleIndexText.size(); i++) {
            allRules.append(this.ruleIndexText.get(i).get(1));
            if (i != this.ruleIndexText.size() - 1)
                allRules.append(',');
        }
        return allRules + "]";
    }


    /**
     * get the string of all tags to send to the client
     *
     * @return string
     */
    String getAllTags() {
        StringBuilder allTags = new StringBuilder("[");
        for (int i = 0; i < this.tagNameText.size(); i++) {
            allTags.append(this.tagNameText.get(i).get(1));
            if (i != this.tagNameText.size() - 1)
                allTags.append(',');
        }
        return allTags + "]";
    }

    /**
     * getter
     *
     * @return srcml object
     */
    SRCMLxml getSrcml() {
        return srcml;
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
                if(event.getManager().getSelectedFiles().length > 0)
                    if(Objects.requireNonNull(event.getManager().getSelectedFiles()[0].getCanonicalFile()).getName().endsWith(".java")) {
                        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "SHOW_RULES_FOR_FILE", event.getManager().getSelectedFiles()[0].getPath()}).toString());
                    }
            }

        });
    }

    /**
     * Overridden method
     */
    public void disposeComponent() {
        ruleIndexText = new ArrayList<>();
        tagNameText = new ArrayList<>();

        Project[] AllProjects = ProjectManager.getInstance().getOpenProjects();
        if (AllProjects.length == 0) {
            try {
                ws.stop();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "";
    }

    // ProjectComponent

    /**
     * Overridden method
     */
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

    /**
     * Overridden method
     */
    @Override
    public void projectClosed() { }

    /**
     * process the message received from the client
     *
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

                //System.out.println(SRCMLHandler.findLineNumber(projectPath + "/tempResultXmlFile.xml"));

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

                break;

            case "MODIFIED_RULE":

                String ruleIndex = Integer.toString(messageAsJson.get("data").getAsJsonObject().get("index").getAsInt());
                String ruleText = messageAsJson.get("data").getAsJsonObject().get("ruleText").getAsJsonObject().toString();

                this.setRuleIndexText(ruleIndex, ruleText);
                this.writeRuleOrTagJsonFile("ruleJson.txt");

                // send message
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE",
                        MessageProcessor.encodeModifiedRule(new Object[]{ruleIndex, ruleText})
                }).toString());

                break;

            case "MODIFIED_TAG":

                String tagName = messageAsJson.get("data").getAsJsonObject().get("tagName").getAsString();
                String tagText = messageAsJson.get("data").getAsJsonObject().get("tagText").getAsJsonObject().toString();

                this.setTagNameText(tagName, tagText);
                this.writeRuleOrTagJsonFile("tagJson.txt");

                // send message
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_TAG", tagText}).toString());

                break;


            case "EXPR_STMT":
                String exprText = messageAsJson.get("data").getAsJsonObject().get("codeText").getAsString();
                String resultExprXml = SRCMLHandler.createXMLForText(exprText, projectPath + "/tempExprFile.java");
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "EXPR_STMT_XML",
                        MessageProcessor.encodeXMLandText(new Object[]{resultExprXml, messageAsJson.get("data").getAsJsonObject().get("messageID").getAsString()})
                }).toString());

                break;

            case "NEW_RULE":

                String newRuleIndex = Integer.toString(messageAsJson.get("data").getAsJsonObject().get("index").getAsInt());
                String newRuleText = messageAsJson.get("data").getAsJsonObject().get("ruleText").getAsJsonObject().toString();

                this.setRuleIndexText(newRuleIndex, newRuleText);
                this.writeRuleOrTagJsonFile("ruleJson.txt");

                // send message
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_RULE",
                        MessageProcessor.encodeModifiedRule(new Object[]{newRuleIndex, newRuleText})
                }).toString());

                break;

            case "NEW_TAG":
                String newTagName = messageAsJson.get("data").getAsJsonObject().get("tagName").getAsString();
                String newTagText = messageAsJson.get("data").getAsJsonObject().get("tagText").getAsJsonObject().toString();

                this.setTagNameText(newTagName, newTagText);
                this.writeRuleOrTagJsonFile("tagJson.txt");

                // send message
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_TAG",
                        MessageProcessor.encodeModifiedTag(new Object[]{newTagName, newTagText})
                }).toString());
                break;

            case "LEARN_RULES_META_DATA":
                // "attribute_META_data.txt"

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
            case "LEARN_RULES_FILE_LOCATIONS_APPEND":
            case "LEARN_RULES_DATABASES_APPEND":
                JsonArray filePathDataAppend = messageAsJson.get("data").getAsJsonArray();
//                String cnt = messageAsJson.get("part").getAsString();
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
                if (file.getName().equals("ruleJson.txt")) {
                    updateRules();
                    return;
                }
                if (file.getName().equals("tagJson.txt")) {
                    updateTags();
                    return;
                }

                // do not handle if the file is not a part of the project
                if (shouldIgnoreEvent(file))
                    return;

                String newXml = eventType.equals("CREATE") ? SRCMLHandler.addXMLForProject(this.getSrcml(), file.getPath()) : "";
                if (eventType.equals("DELETE"))
                    SRCMLHandler.removeXMLForProject(srcml, file.getPath());

                this.updateSrcml(file.getPath(), newXml);

                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATED_PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());
                break;

            case "TEXT_CHANGE":

                if (file.getName().equals("ruleJson.txt")) {
                    System.out.println("ruleJson.txt modified.");
                    updateRules();
                    return;
                }
                if (file.getName().equals("tagJson.txt")) {
                    System.out.println("tagJson.txt modified.");
                    updateTags();
                    return;
                }

                // do not handle if the file is not a part of the project
                if (shouldIgnoreEvent(file))
                    return;

                String updatedXml = SRCMLHandler.updateXMLForProject(this.getSrcml(), file.getPath());
                this.updateSrcml(file.getPath(), updatedXml);
                break;

            case "PROPERTY_CHANGE":
                // when a file's properties change. for instance, if the file is renamed.
                List<String> newPaths = getFilePaths(currentProject);

                for (String path : srcml.getPaths()) {
                    if (!newPaths.contains(path)) {

                        SRCMLHandler.removeXMLForProject(srcml, path);
                        String changedXml = SRCMLHandler.addXMLForProject(srcml, file.getPath());
                        this.updateSrcml(file.getPath(), changedXml);
                        break;
                    }
                }
                ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATED_PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());
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
        return !(s.getName().endsWith(".java") || s.getName().endsWith("ruleJson.txt") || s.getName().endsWith("tagJson.txt"));
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

        // start off with root
//        VirtualFile rootDirectoryVirtualFile = project.getBaseDir();

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
     * write in file
     *
     * @param fileName either ruleJson.txt or tagJson.txt
     */
    private void writeRuleOrTagJsonFile(String fileName) {

        try {
            PrintWriter writer = new PrintWriter(projectPath + "/" + fileName, "UTF-8");
            switch (fileName) {
                case "ruleJson.txt":
                    writer.println('[');
                    for (int i = 0; i < this.ruleIndexText.size(); i++) {
                        writer.println(this.ruleIndexText.get(i).get(1));
                        if (i != this.ruleIndexText.size() - 1)
                            writer.println(',');
                    }
                    writer.println(']');
                    break;
                case "tagJson.txt":
                    writer.println('[');
                    for (int i = 0; i < this.tagNameText.size(); i++) {
                        writer.println(this.tagNameText.get(i).get(1));
                        if (i != this.tagNameText.size() - 1)
                            writer.println(',');
                    }
                    writer.println(']');
                    break;
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("error in writing " + fileName);
        }

    }

    /**
     * write in file
     *
     * @param fileName name of files
     * @param content content of the file
     */
    private void writeDataToFileLearningDR(String fileName, String content, boolean append) {

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
     * update the ruleIndexText and send messages to clients
     */
    private void updateRules() {
        this.ruleIndexText = MessageProcessor.getInitialRulesAsList(currentProject);

        // send the message
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "RULE_TABLE", this.getAllRules()}).toString());
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE_TABLE", ""}).toString());
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "VERIFY_RULES", ""}).toString());
    }

    /**
     * update the tagNameText and send messages to clients
     */
    private void updateTags() {
        this.tagNameText = MessageProcessor.getInitialTagsAsList(currentProject);

        // send the message
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "TAG_TABLE", this.getAllTags()}).toString());
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_TAG_TABLE", ""}).toString());
    }

    /**
     * change the srcml and send some messages to clients if the srcml is updated
     *
     * @param filePath String
     * @param newXml   String
     */
    private void updateSrcml(String filePath, String newXml) {
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


        // start off with root
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
//        VirtualFile rootDirectoryVirtualFile = project.getBaseDir();

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

                if (shouldIgnoreFileForProjectHierarchy(item)) {
                    continue;
                }

                for (VirtualFile childOfItem : item.getChildren()) {
                    if (shouldIgnoreFileForProjectHierarchy(childOfItem)) {
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

                        if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
                            PsiFile psiFile = ApplicationManager.getApplication().runReadAction(
                                    (Computable<PsiFile>) () -> PsiManager.getInstance(project).findFile(childOfItem));
                            if (psiFile != null)
                                propertiesOfChild.addProperty("fileName", psiFile.getName());
                        }

//                        propertiesOfChild.addProperty("text", psiFile.getText());
//                        propertiesOfChild.add("ast", generateASTAsJSON(psiFile));
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
     * sending the data for feature selection to the client
     * @param path of the file from which a code snippet is selected
     * @param startIndex start of the selection
     * @param endIndex end of the selection
     * @param lineNumber of the javaFile
     * @param text text of the selection
     */
    void sendFeatureSelectionData(String path, String startIndex, String endIndex, String lineNumber, String text) {
        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FEATURE_SELECTION",
                MessageProcessor.encodeSelectedFragment(new Object[]{path, startIndex, endIndex, lineNumber, text})
        }).toString());
    }
}
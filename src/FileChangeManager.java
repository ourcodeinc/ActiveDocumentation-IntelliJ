
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import org.java_websocket.WebSocketImpl;
import org.jetbrains.annotations.NotNull;

import core.model.SRCMLHandler;
import core.model.SRCMLxml;
import org.jetbrains.annotations.Nullable;


import java.util.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.io.PrintWriter;
import java.util.ArrayList;


public class FileChangeManager implements ProjectComponent {

    private final MessageBusConnection connection;
    private ChatServer s;
    private List<VirtualFile> ignoredFiles/* = new ArrayList<>()*/;
    private SRCMLxml srcml;
    private List<List<String>> ruleIndexText; // index - text
    private List<List<String>> tagNameText; // tagName - text
    private Project currentProject;
    String projectPath;

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
        String allRules = "[";
        for (int i = 0; i < this.ruleIndexText.size(); i++) {
            allRules = allRules + this.ruleIndexText.get(i).get(1);
            if (i != this.ruleIndexText.size() - 1)
                allRules = allRules + ',';
        }
        return allRules + "]";
    }


    /**
     * get the string of all tags to send to the client
     *
     * @return string
     */
    String getAllTags() {
        String allTags = "[";
        for (int i = 0; i < this.tagNameText.size(); i++) {
            allTags = allTags + this.tagNameText.get(i).get(1);
            if (i != this.tagNameText.size() - 1)
                allTags = allTags + ',';
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


    //---------------------------

    public void initComponent() {
        ignoredFiles = utilities.createIgnoredFileList(currentProject);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
                // update rules in ChatServer
                for (VFileEvent event : events) {
                    if (shouldIgnoreFile(event.getFile()))
                        continue;
                    if (event instanceof VFileCreateEvent) { // Create files
                        handleVFileCreateEvent(event);
                    } else if (event instanceof VFileContentChangeEvent) { // Text Change
                        handleVFileChangeEvent(event);

                    } else if (event instanceof VFileDeleteEvent) { // Delete files
                        handleVFileDeleteEvent(event);
                    } else if (event instanceof VFilePropertyChangeEvent) { // Property Change
                        handleVFilePropertyChangeEvent(event);
                    }
                }
            }
        });

        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                if(event.getManager().getSelectedFiles().length > 0)
                    s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "SHOW_RULES_FOR_FILE", event.getManager().getSelectedFiles()[0].getPath()}).toString());
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
                s.stop();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
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
                WebSocketImpl.DEBUG = false;
                s = new ChatServer(8887);
                s.setManager(this);
                s.start();
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

    //-------------------------------

    /**
     * process the message received from the client
     *
     * @param messageAsJson JsonObject
     */
    void processReceivedMessages(JsonObject messageAsJson) {

        String command = messageAsJson.get("command").getAsString();
//        String projectPath = ProjectManager.getInstance().getOpenProjects()[0].getBasePath();

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
                    FileEditorManager.getInstance(currentProject).openFile(fileByPath, true);
                    Editor theEditor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
                    int indexToFocusOn = SRCMLHandler.findLineNumber(projectPath + "/tempResultXmlFile.xml");
                    theEditor.getCaretModel().moveToOffset(indexToFocusOn);
                    theEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                });

                break;

            case "MODIFIED_RULE":

                String ruleIndex = Integer.toString(messageAsJson.get("data").getAsJsonObject().get("index").getAsInt());
                String ruleText = messageAsJson.get("data").getAsJsonObject().get("ruleText").getAsJsonObject().toString();

                this.setRuleIndexText(ruleIndex, ruleText);
                this.writeToFile("ruleJson.txt");

                // send message
                s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE",
                        MessageProcessor.encodeModifiedRule(new Object[]{ruleIndex, ruleText})
                }).toString());

                break;

            case "MODIFIED_TAG":

                String tagName = messageAsJson.get("data").getAsJsonObject().get("tagName").getAsString();
                String tagText = messageAsJson.get("data").getAsJsonObject().get("tagText").getAsJsonObject().toString();

                this.setTagNameText(tagName, tagText);
                this.writeToFile("tagJson.txt");

                // send message
                s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_TAG", tagText}).toString());

                break;


            case "EXPR_STMT":
                String exprText = messageAsJson.get("data").getAsJsonObject().get("codeText").getAsString();
                String resultExprXml = SRCMLHandler.createXMLForText(exprText, projectPath + "/tempExprFile.java");
                s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "EXPR_STMT_XML",
                        MessageProcessor.encodeXMLandText(new Object[]{resultExprXml, messageAsJson.get("data").getAsJsonObject().get("messageID").getAsString()})
                }).toString());

                break;

            case "NEW_RULE":

                String newRuleIndex = Integer.toString(messageAsJson.get("data").getAsJsonObject().get("index").getAsInt());
                String newRuleText = messageAsJson.get("data").getAsJsonObject().get("ruleText").getAsJsonObject().toString();

                this.setRuleIndexText(newRuleIndex, newRuleText);
                this.writeToFile("ruleJson.txt");

                // send message
                s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_RULE",
                        MessageProcessor.encodeModifiedRule(new Object[]{newRuleIndex, newRuleText})
                }).toString());

                break;

            case "NEW_TAG":
                String newTagName = messageAsJson.get("data").getAsJsonObject().get("tagName").getAsString();
                String newTagText = messageAsJson.get("data").getAsJsonObject().get("tagText").getAsJsonObject().toString();

                this.setTagNameText(newTagName, newTagText);
                this.writeToFile("tagJson.txt");

                // send message
                s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_TAG",
                        MessageProcessor.encodeModifiedTag(new Object[]{newTagName, newTagText})
                }).toString());

        }

    }


    //------------------ handle file events

    // when the text of a file changes
    private void handleVFileChangeEvent(VFileEvent event) {
        VirtualFile file = event.getFile();

        // if we are dealing with ruleJson.txt
        if (file.getName().equals("ruleJson.txt")) {
            System.out.println("ruleJson.txt modified.");
            updateRules();
            return;
        }
        // if we are dealing with tagJson.txt
        if (file.getName().equals("tagJson.txt")) {
            System.out.println("tagJson.txt modified.");
            updateTags();
            return;
        }

        // do not handle if the file is not a part of the project
        if (!shouldConsiderEvent(file)) {
            return;
        }

        System.out.println("CHANGE");
        String newXml = SRCMLHandler.updateXMLForProject(this.getSrcml(), file.getPath());
        this.updateSrcml(file.getPath(), newXml);

    }

    // when a file is created
    private void handleVFileCreateEvent(VFileEvent event) {

        VirtualFile file = event.getFile();

        // if we are dealing with ruleJson.txt
        if (file.getName().equals("ruleJson.txt")) {
            updateRules();
            return;
        }
        // if we are dealing with tagJson.txt
        if (file.getName().equals("tagJson.txt")) {
            updateTags();
            return;
        }

        // do not handle if the file is not a part of the project
        if (!shouldConsiderEvent(file)) {
            return;
        }

        System.out.println("CREATE");
        String newXml = SRCMLHandler.addXMLForProject(this.getSrcml(), file.getPath());
        this.updateSrcml(file.getPath(), newXml);
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATED_PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());

    }

    // when a file's properties change. for instance, if the file is renamed.
    private void handleVFilePropertyChangeEvent(VFileEvent event) {
        VirtualFile file = event.getFile();

//        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        List<String> newPaths = getFilePaths(currentProject);

        System.out.println("PROP_CHANGE");
        for (String path : srcml.getPaths()) {
            if (!newPaths.contains(path)) {

                SRCMLHandler.removeXMLForProject(srcml, path);
                String newXml = SRCMLHandler.addXMLForProject(srcml, file.getPath());
                this.updateSrcml(file.getPath(), newXml);
                break;
            }
        }
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATED_PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());
    }

    // when a file is deleted
    private void handleVFileDeleteEvent(VFileEvent event) {
        VirtualFile file = event.getFile();

        // if we are dealing with ruleJson.txt
        if (file.getName().equals("ruleJson.txt")) {
            updateRules();
            return;
        }
        // if we are dealing with tagJson.txt
        if (file.getName().equals("tagJson.txt")) {
            updateTags();
            return;
        }

        System.out.println("DELETE");

        SRCMLHandler.removeXMLForProject(srcml, file.getPath());
        this.updateSrcml(file.getPath(), "");
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATED_PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());
    }

    //----------------------------------

    /**
     * checks if we should ignore a file
     *
     * @param s virtual file
     * @return true/false
     */
    private boolean shouldIgnoreFile(VirtualFile s) {
        if (ignoredFiles == null) {
            return false;
        }
        for (VirtualFile vfile : ignoredFiles) {
            if (vfile.getCanonicalPath().equals(s.getCanonicalPath())) {
                return true;
            } else if (utilities.isFileAChildOf(s, vfile)) {
                return true;
            }
        }
        return false;
    }


    /**
     * check whether we should consider the file change
     *
     * @param file VirtualFile
     * @return boolean
     */
    private boolean shouldConsiderEvent(VirtualFile file) {

        if (file.isDirectory())
            return false;
        else if (!file.getCanonicalPath().endsWith(".java")) {
            return false;
        }
//        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        PsiFile psiFile = PsiManager.getInstance(currentProject).findFile(file);

        return PsiManager.getInstance(currentProject).isInProject(psiFile);

    }

    /**
     * generate list of java file paths
     *
     * @param project Project
     * @return list of file paths in the project
     */
    static List<String> getFilePaths(Project project) {
        List<String> paths = new ArrayList<>();

        // start off with root
        VirtualFile rootDirectoryVirtualFile = project.getBaseDir();

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
                else if (childOfItem.getCanonicalPath().endsWith(".java")) {
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
//        Project activeProject = ProjectManager.getInstance().getOpenProjects()[0];
//        String projectPath = activeProject.getBasePath();

        if (!this.srcml.getProjectPath().equals(projectPath)) {
            SRCMLxml srcml = new SRCMLxml(FileChangeManager.getFilePaths(currentProject), projectPath);
            SRCMLHandler.createXMLForProject(srcml);
            System.out.println("XML data is created.");

            this.srcml = srcml;
//            this.rules = MessageProcessor.getInitialRules().toString();

        }
    }

    /**
     * write in file
     *
     * @param fileName either ruleJson.txt or tagJson.txt
     */
    private void writeToFile(String fileName) {

        try {
//            String projectPath = ProjectManager.getInstance().getOpenProjects()[0].getBasePath();

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
     * update the ruleIndexText and send messages to clients
     */
    private void updateRules() {
        this.ruleIndexText = MessageProcessor.getInitialRulesAsList(currentProject);

        // send the message
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "RULE_TABLE", this.getAllRules()}).toString());
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE_TABLE", ""}).toString());
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "VERIFY_RULES", ""}).toString());
    }

    /**
     * update the tagNameText and send messages to clients
     */
    private void updateTags() {
        this.tagNameText = MessageProcessor.getInitialTagsAsList(currentProject);

        // send the message
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "TAG_TABLE", this.getAllTags()}).toString());
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_TAG_TABLE", ""}).toString());
    }

    /**
     * change the srcml and send some messages to clients if the srcml is updated
     *
     * @param filePath String
     * @param newXml   String
     */
    private void updateSrcml(String filePath, String newXml) {
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_XML",
                MessageProcessor.encodeNewXMLData(new Object[]{filePath, newXml})
        }).toString());
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "CHECK_RULES_FOR_FILE", filePath}).toString());
    }


    /**
     * Copied from master branch
     *
     * @return json
     */
    JsonObject generateProjectHierarchyAsJSON() {

//        return null;

        // start off with root
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
//        Project project = getProject();
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
        HashMap<String, JsonObject> canonicalToJsonMap = new HashMap<String, JsonObject>();
        canonicalToJsonMap.put(rootDirectoryVirtualFile.getCanonicalPath(), jsonRootDirectory);

        // set up queue
        java.util.List<VirtualFile> q = new ArrayList<VirtualFile>();
        q.add(rootDirectoryVirtualFile);

        // traverse the queue
        while (!q.isEmpty()) {
            java.util.List<VirtualFile> new_q = new ArrayList<VirtualFile>();
            for (VirtualFile item : q) {
                // System.out.println(item.getName());
                if (shouldIgnoreFile(item)) {
                    continue;
                }
//                System.out.println("Included: " + item.getCanonicalPath());
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

                        if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
                            PsiFile psiFile = ApplicationManager.getApplication().runReadAction(new Computable<PsiFile>() {
                                @Nullable
                                @Override
                                public PsiFile compute() {
                                    return PsiManager.getInstance(project).findFile(childOfItem);
                                }
                            });
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

//        System.out.println(jsonRootDirectory);
        return jsonRootDirectory;

    }

}
/*
 * written by saharmehrpour
 * This class manages the changes that are done or must be done on the project files.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
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
import core.model.SRCMLHandler;
import core.model.SRCMLxml;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class FileChangeManager implements ProjectComponent {

    private final MessageBusConnection connection;
    private ChatServer ws;
    private SRCMLxml srcml;
    private Project currentProject;
    String projectPath;
    private List<VirtualFile> ignoredFiles;

    private static FileChangeManager thisClass = null;

    FileChangeManager(Project project) {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        currentProject = project;
        projectPath = project.getBasePath();
        srcml = new SRCMLxml(Utilities.getFilePaths(project), project.getBasePath());
        SRCMLHandler.createXMLForProject(srcml);

        thisClass = this;
    }

    static FileChangeManager getInstance() {
        return thisClass;
    }

    /**
     * @return srcml object
     */
    SRCMLxml getSrcml() {
        return srcml;
    }

    public void initComponent() {
        ignoredFiles = Utilities.createIgnoredFileList(currentProject);
        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {
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
                            sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FILE_CHANGE", event.getManager().getSelectedFiles()[0].getPath()}).toString());
                        }
                } catch (NullPointerException e) {
                    System.out.println("error happened in finding the changed file.");
                }
            }
        });
    }

    /**
     * Overridden method
     */
    public void disposeComponent() {
        currentProject = null;

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
                ws.start();
            } catch (Exception e) {
                System.out.println("Error in creating a Chat server.");
                e.printStackTrace();
            }

            new FollowAndAuthorRulesProcessor(projectPath, currentProject, ws);
            new MiningRulesProcessor(projectPath, currentProject, ws);
        }
    }

    @Override
    public void projectClosed() {
    }

    /**
     * handle events based on event types.
     *
     * @param event     VFileEvent
     * @param eventType one of CREATE, TEXT_CHANGE, PROPERTY_CHANGE, DELETE
     */
    private void handleEvents(VFileEvent event, String eventType) {
        VirtualFile file = event.getFile();
        if (file == null || currentProject == null) return;

        switch (eventType) {
            case "CREATE":
            case "DELETE":
                if (file.getName().equals("ruleTable.json")) {
                    FollowAndAuthorRulesProcessor.getInstance().updateRules();
                    return;
                }
                if (file.getName().equals("tagTable.json")) {
                    FollowAndAuthorRulesProcessor.getInstance().updateTags();
                    return;
                }

                // do not handle if the file is not a part of the project
                if (shouldIgnoreEvent(file))
                    return;

                String newXml = eventType.equals("CREATE") ? SRCMLHandler.addXMLForProject(this.getSrcml(), file.getPath()) : "";
                if (eventType.equals("DELETE"))
                    SRCMLHandler.removeXMLForProject(srcml, file.getPath());

                this.updateXML(file.getPath(), newXml);

                sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());
                break;

            case "TEXT_CHANGE":
                if (file.getName().equals("ruleTable.json")) {
                    System.out.println("ruleTable.json modified.");
                    FollowAndAuthorRulesProcessor.getInstance().updateRules();
                    return;
                }
                if (file.getName().equals("tagTable.json")) {
                    System.out.println("tagTable.json modified.");
                    FollowAndAuthorRulesProcessor.getInstance().updateTags();
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
                List<String> newPaths = Utilities.getFilePaths(currentProject);

                for (String path : srcml.getPaths()) {
                    if (!newPaths.contains(path)) {

                        SRCMLHandler.removeXMLForProject(srcml, path);
                        String changedXml = SRCMLHandler.addXMLForProject(srcml, file.getPath());
                        this.updateXML(file.getPath(), changedXml);
                        break;
                    }
                }
                sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "PROJECT_HIERARCHY", generateProjectHierarchyAsJSON()}).toString());

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
        return !(s.getName().endsWith(".java")
                || s.getName().endsWith("ruleTable.json")
                || s.getName().endsWith("tagTable.json")
                || s.getName().equals("tempExprDeclFile.java"));
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
     * check whether the given project is the same as the project for which
     * srcml is created. It updates the srcml and project path accordingly.
     */
    void checkChangedProject() {
        if (!this.srcml.getProjectPath().equals(projectPath)) {
            SRCMLxml srcml = new SRCMLxml(Utilities.getFilePaths(currentProject), projectPath);
            SRCMLHandler.createXMLForProject(srcml);
            System.out.println("XML data is created.");
            this.srcml = srcml;
        }
    }

    /**
     * change the xml data and inform clients
     *
     * @param filePath String
     * @param newXml   String
     */
    private void updateXML(String filePath, String newXml) {
        sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_XML", MessageProcessor.encodeNewXMLData(new Object[]{filePath, newXml})}).toString());
        sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "CHECK_RULES_FOR_FILE", filePath}).toString());
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
     *
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
            } else if (Utilities.isFileAChildOf(s, vfile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * send the message through web socket
     * @param msg the processed string
     */
    void sendMessage(String msg) {
        if (ws != null)
            ws.sendToAll(msg);
    }
}
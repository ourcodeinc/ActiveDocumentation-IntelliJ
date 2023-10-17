/*
 * written by saharmehrpour
 * This class manages the changes that are done or must be done on the project files.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.startup.StartupActivity;
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

import java.util.*;

public class FileChangeManager implements StartupActivity {

    private ChatServer ws = null;
    private SRCMLxml srcml;
    String projectPath;

    private static FileChangeManager thisClass = null;

    static FileChangeManager getInstance() {
        return thisClass;
    }

    SRCMLxml getSrcml() {
        return srcml;
    }

    @Override
    public void runActivity(@NotNull Project project) {

        MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect();
        projectPath = project.getBasePath();
        srcml = new SRCMLxml(Utilities.getFilePaths(project), project.getBasePath());
        SRCMLHandler.createXMLForProject(srcml);

        thisClass = this;
        if (ws == null) {
            try {
                ws = new ChatServer(8887);
                ws.start();
            } catch (Exception e) {
                System.out.println("Error in creating a Chat server.");
                e.printStackTrace();
                return;
            }
        }

        try {
            FollowAndAuthorRulesProcessor ins = FollowAndAuthorRulesProcessor.getInstance();
            ins.updateProjectWs(project, ws);
        } catch (NullPointerException e) {
            new FollowAndAuthorRulesProcessor(project, ws);
        }
        try {
            MiningRulesProcessor ins = MiningRulesProcessor.getInstance();
            ins.updateProjectWs(project, ws);
        } catch (NullPointerException e) {
            new MiningRulesProcessor(project, ws);
        }
        try {
            DoiProcessing ins = DoiProcessing.getInstance();
            ins.updateProject(project);
        } catch (NullPointerException e) {
            new DoiProcessing(project);
        }

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
                    handleEvents(project, event, eventType);
                }
            }
        });

        connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                try {
                    if (event.getManager().getSelectedFiles().length > 0)
                        if (Objects.requireNonNull(event.getManager().getSelectedFiles()[0].getCanonicalFile())
                                .getName().endsWith(".java")) {
                            String filePath = event.getManager().getSelectedFiles()[0].getPath();
                            DoiProcessing.getInstance().newVisitedFile(filePath);
                            sendMessage(MessageProcessor.encodeData(new Object[]{
                                    WebSocketConstants.SEND_FILE_CHANGE_IN_IDE_MSG, filePath}).toString());
                        }
                } catch (NullPointerException e) {
                    System.out.println("error happened in finding the changed file.");
                }
            }
        });
    }

    /**
     * handle events based on event types.
     *
     * @param event     VFileEvent
     * @param eventType one of CREATE, TEXT_CHANGE, PROPERTY_CHANGE, DELETE
     */
    private void handleEvents(Project project, VFileEvent event, String eventType) {
        VirtualFile file = event.getFile();
        if (file == null || project == null) return;

        switch (eventType) {
            case "CREATE":
            case "DELETE":
                if (file.getName().equals(Constants.RULE_TABLE_JSON)) {
                    FollowAndAuthorRulesProcessor.getInstance().updateRules();
                    return;
                }
                if (file.getName().equals(Constants.TAG_TABLE_JSON)) {
                    FollowAndAuthorRulesProcessor.getInstance().updateTags();
                    return;
                }

                // do not handle if the file is not a part of the project
                if (shouldIgnoreEvent(project, file))
                    return;

                String newXml = eventType.equals("CREATE") ? SRCMLHandler.addXMLForProject(this.getSrcml(),
                        file.getPath()) : "";
                if (eventType.equals("DELETE"))
                    SRCMLHandler.removeXMLForProject(srcml, file.getPath());

                this.updateXML(file.getPath(), newXml);

                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_PROJECT_HIERARCHY_MSG,
                        generateProjectHierarchyAsJSON()}).toString());
                break;

            case "TEXT_CHANGE":
                if (file.getName().equals(Constants.RULE_TABLE_JSON)) {
                    System.out.println(Constants.RULE_TABLE_JSON + " modified.");
                    FollowAndAuthorRulesProcessor.getInstance().updateRules();
                    return;
                }
                if (file.getName().equals(Constants.TAG_TABLE_JSON)) {
                    System.out.println(Constants.TAG_TABLE_JSON + " modified.");
                    FollowAndAuthorRulesProcessor.getInstance().updateTags();
                    return;
                }

                // do not handle if the file is not a part of the project
                if (shouldIgnoreEvent(project, file))
                    return;

                String updatedXml = SRCMLHandler.updateXMLForProject(this.getSrcml(), file.getPath());
                this.updateXML(file.getPath(), updatedXml);
                break;

            case "PROPERTY_CHANGE":
                // when a file's properties change. for instance, if the file is renamed.
                List<String> newPaths = Utilities.getFilePaths(project);

                for (String path : srcml.getPaths()) {
                    if (!newPaths.contains(path)) {

                        SRCMLHandler.removeXMLForProject(srcml, path);
                        String changedXml = SRCMLHandler.addXMLForProject(srcml, file.getPath());
                        this.updateXML(file.getPath(), changedXml);
                        break;
                    }
                }
                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_PROJECT_HIERARCHY_MSG,
                        generateProjectHierarchyAsJSON()}).toString());

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
        if (s.getName().endsWith(Constants.TEMP_JAVA_FILE)) {
            return true;
        }
        return !(s.getName().endsWith(".java")
                || s.getName().endsWith(Constants.RULE_TABLE_JSON)
                || s.getName().endsWith(Constants.TAG_TABLE_JSON));
    }

    /**
     * check whether we should consider the file change
     *
     * @param file VirtualFile
     * @return boolean
     */
    private boolean shouldIgnoreEvent(Project project, VirtualFile file) {
        if (file.isDirectory() || file.getCanonicalPath() == null)
            return true;
        if (!file.getCanonicalPath().endsWith(".java"))
            return true;

        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return psiFile == null || !PsiManager.getInstance(project).isInProject(psiFile);

    }

    /**
     * check whether the given project is the same as the project for which
     * srcml is created. It updates the srcml and project path accordingly.
     */
    void checkChangedProject() {
        if (!this.srcml.getProjectPath().equals(projectPath)) {
            Project project = ProjectManager.getInstance().getDefaultProject();
            SRCMLxml srcml = new SRCMLxml(Utilities.getFilePaths(project), projectPath);
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
        sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_UPDATE_XML_FILE_MSG,
                MessageProcessor.encodeNewXMLData(new Object[]{filePath, newXml})}).toString());
        sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_CHECK_RULES_FOR_FILE_MSG,
                filePath}).toString());
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
            properties.addProperty("name", rootDirectoryVirtualFile.getName());
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

                for (VirtualFile childOfItem : item.getChildren()) {

                    new_q.add(childOfItem);
                    JsonObject jsonChildOfItem = new JsonObject();
                    JsonObject propertiesOfChild = new JsonObject();
                    if (childOfItem.isDirectory()) {
                        propertiesOfChild.addProperty("canonicalPath", childOfItem.getCanonicalPath());
                        propertiesOfChild.addProperty("parent", item.getCanonicalPath());
                        propertiesOfChild.addProperty("name", childOfItem.getName());
                        propertiesOfChild.addProperty("isDirectory", true);
                        jsonChildOfItem.add("children", new JsonArray());
                        jsonChildOfItem.add("properties", propertiesOfChild);
                    } else {
                        if (shouldIgnoreFileForProjectHierarchy(childOfItem)) continue;

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
                    canonicalToJsonMap.get(item.getCanonicalPath()).get("children").getAsJsonArray()
                            .add(jsonChildOfItem);
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
        String path = s.getCanonicalPath();
        if (path == null) return true;
        if (!path.endsWith(".java")) return true;
        return path.endsWith(Constants.TEMP_JAVA_FILE);
    }

    /**
     * send the message through web socket
     *
     * @param msg the processed string
     */
    void sendMessage(String msg) {
        if (ws != null)
            ws.sendToAll(msg);
    }
}
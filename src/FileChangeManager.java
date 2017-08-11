
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;

import core.model.SRCMLHandler;

import core.model.SRCMLxml;
import org.jetbrains.annotations.NotNull;

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
    //    private String rules;
    private List<List<String>> ruleIndexText; // index - text
    private List<List<String>> tagNameText; // tagName - text

    FileChangeManager(ChatServer server, SRCMLxml xmlP, /*String rule,*/ List<List<String>> ruleList, List<List<String>> tagList) {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        s = server;

//        rules = rule;
        srcml = xmlP;
        ruleIndexText = ruleList;
        tagNameText = tagList;
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
    }


    /**
     * get the string of all rules to send to the client
     *
     * @return string
     */
    String getAllRules() {
        String allRules = "ruleTable=[";
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
        String allTags = "tagTable=[";
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
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "CHECK_RULES", filePath}).toString());
    }

    public void initComponent() {
        s.start();

        System.out.println("(initComponent) ChatServer started on port: " + s.getPort());

        ignoredFiles = utilities.createIgnoredFileList();

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
    }

    /**
     * Overridden method
     * Not working
     */
    public void disposeComponent() {
        System.out.println("(disposeComponent)");
        connection.disconnect();
        try {
            s.stop();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    @Override
    public String getComponentName() {
        System.out.println("(Component Name)");
        return "";
    }

    // ProjectComponent

    /**
     * Overridden method
     * Not working
     */
    @Override
    public void projectOpened() {
        System.out.println("(project Opened)");
    }

    /**
     * Overridden method
     * Not working
     */
    @Override
    public void projectClosed() {
        System.out.println("(project Closed)");
    }


    /**
     * process the message received from the client
     *
     * @param messageAsJson JsonObject
     */
    void processReceivedMessages(JsonObject messageAsJson) {

        String command = messageAsJson.get("command").getAsString();
        String projectPath = ProjectManager.getInstance().getOpenProjects()[0].getBasePath();

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
                    String[] filePath = messageAsJson.get("data").getAsJsonObject().get("fileName").getAsString().split("/");
                    String fileToFocusOn = filePath[filePath.length - 1];
                    int indexToFocusOn = SRCMLHandler.findLineNumber(projectPath + "/tempResultXmlFile.xml");
                    Project currentProject = ProjectManager.getInstance().getOpenProjects()[0];
                    VirtualFile theVFile = FilenameIndex.getVirtualFilesByName(currentProject, fileToFocusOn, GlobalSearchScope.projectScope(currentProject)).iterator().next();
                    FileEditorManager.getInstance(currentProject).openFile(theVFile, true);
                    Editor theEditor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
                    theEditor.getCaretModel().moveToOffset(indexToFocusOn);
                    theEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                });
                break;

            case "MODIFIED_RULE":

                String ruleIndex = Integer.toString(messageAsJson.get("data").getAsJsonObject().get("index").getAsInt());
                String ruleText = messageAsJson.get("data").getAsJsonObject().get("ruleText").getAsJsonObject().toString();

                this.setRuleIndexText(ruleIndex, ruleText);
                this.writeToFile("ruleJson.txt");

                // TODO send message

                break;

            case "MODIFIED_TAG":

                String tagName = Integer.toString(messageAsJson.get("data").getAsJsonObject().get("tagName").getAsInt());
                String tagText = messageAsJson.get("data").getAsJsonObject().get("tagText").getAsJsonObject().toString();

                this.setTagNameText(tagName, tagText);
                this.writeToFile("tagJson.txt");

                // TODO send message

                break;

            case "NEW_RULE":

                //TODO first add the rule, then write it in the file

                break;

        }

    }


    /** checks if we should ignore a file
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

    //------------------ handle file events

    // when the text of a file changes
    private void handleVFileChangeEvent(VFileEvent event) {
        VirtualFile file = event.getFile();

        // if we are dealing with ruleJson.txt
        if (file.getName().equals("ruleJson.txt")) {
            //TODO
//            this.setRules(MessageProcessor.getInitialRules().toString());
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
            // TODO
//            this.setRules(MessageProcessor.getInitialRules().toString());
            return;
        }

        // do not handle if the file is not a part of the project
        if (!shouldConsiderEvent(file)) {
            return;
        }

        System.out.println("CREATE");
        String newXml = SRCMLHandler.addXMLForProject(this.getSrcml(), file.getPath());
        this.updateSrcml(file.getPath(), newXml);
        //this.setSrcml(SRCMLHandler.addXMLForProject(this.getSrcml(), file.getPath()));

    }

    // when a file's properties change. for instance, if the file is renamed.
    private void handleVFilePropertyChangeEvent(VFileEvent event) {
        VirtualFile file = event.getFile();

        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        List<String> newPaths = getFilePaths(project);

        System.out.println("PROP_CHANGE");
        for (String path : srcml.getPaths()) {
            if (!newPaths.contains(path)) {

                SRCMLHandler.removeXMLForProject(srcml, path);
                String newXml = SRCMLHandler.addXMLForProject(srcml, file.getPath());
                this.updateSrcml(file.getPath(), newXml);
                break;
            }
        }

    }

    // when a file is deleted
    private void handleVFileDeleteEvent(VFileEvent event) {
        VirtualFile file = event.getFile();

        // if we are dealing with ruleJson.txt
        if (file.getName().equals("ruleJson.txt")) {
            // TODO
//            this.setRules("");
            return;
        }

        System.out.println("DELETE");

        SRCMLHandler.removeXMLForProject(srcml, file.getPath());
        this.updateSrcml(file.getPath(), "");

    }

    //----------------------------------

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
        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

        return PsiManager.getInstance(project).isInProject(psiFile);

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
        Project activeProject = ProjectManager.getInstance().getOpenProjects()[0];
        String projectPath = activeProject.getBasePath();

        if (!this.srcml.getProjectPath().equals(projectPath)) {
            SRCMLxml srcml = new SRCMLxml(FileChangeManager.getFilePaths(activeProject), projectPath);
            SRCMLHandler.createXMLForProject(srcml);
            System.out.println("XML data is created.");

            this.srcml = srcml;
//            this.rules = MessageProcessor.getInitialRules().toString();

        }
    }

    /**
     * write in file
     * @param fileName either ruleJson.txt or tagJson.txt
     */
    private void writeToFile(String fileName) {

        try {
            String projectPath = ProjectManager.getInstance().getOpenProjects()[0].getBasePath();

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
}
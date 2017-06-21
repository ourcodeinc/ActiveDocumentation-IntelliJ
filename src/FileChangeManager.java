
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

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;


public class FileChangeManager implements ProjectComponent {

    private final MessageBusConnection connection;
    private ChatServer s;
    private List<VirtualFile> ignoredFiles = new ArrayList<>();

    public FileChangeManager(ChatServer server) {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        s = server;

    }

    public void initComponent() {
        // What is the difference of opening the socket here vs. when toolWindow is created?
        s.start();

        System.out.println("ChatServer started on port: " + s.getPort());

        createIgnoredFileList();

        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {

                // TODO handle events
                // update rules in ChatServer

                for (VFileEvent event : events) {
                    if (shouldIgnoreFile(event.getFile()))
                        continue;

                    if (event instanceof VFileCreateEvent) { // Create files
                        handleVFileCreateEvent(event);
                        System.out.println("CREATE");

                    } else if (event instanceof VFileContentChangeEvent) { // Text Change
                        handleVFileChangeEvent(event);
                        System.out.println("CHANGE");

                    } else if (event instanceof VFileDeleteEvent) { // Delete files
                        handleVFileDeleteEvent(event);
                        System.out.println("DEL");

                    } else if (event instanceof VFilePropertyChangeEvent) { // Property Change
                        handleVFilePropertyChangeEvent(event);
                        System.out.println("PROP_CHANGE");
                    }
                }
            }
        });
    }

    public void disposeComponent() {
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

    @Override
    public void projectOpened() {
        System.out.println("(project Opened)");
    }

    @Override
    public void projectClosed() {
        System.out.println("(project Closed)");
    }


    public void processReceivedMessages(JsonObject messageAsJson) {

        String command = messageAsJson.get("command").getAsString();
        String projectPath = ProjectManager.getInstance().getOpenProjects()[0].getBasePath();

        switch (command) {
            case "xmlResult":
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

                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        String[] filePath = messageAsJson.get("data").getAsJsonObject().get("fileName").getAsString().split("/");
                        String fileToFocusOn = filePath[filePath.length - 1];
                        int indexToFocusOn = SRCMLHandler.findLineNumber(projectPath + "/tempResultXmlFile.xml");
                        Project currentProject = ProjectManager.getInstance().getOpenProjects()[0];
                        VirtualFile theVFile = FilenameIndex.getVirtualFilesByName(currentProject, fileToFocusOn, GlobalSearchScope.projectScope(currentProject)).iterator().next();
                        FileEditorManager.getInstance(currentProject).openFile(theVFile, true);
                        Editor theEditor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
                        theEditor.getCaretModel().moveToOffset(indexToFocusOn);
                        theEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                    }
                });
                break;

            case "NewRule":
                try {
                    PrintWriter writer = new PrintWriter(projectPath + "/ruleJson.txt", "UTF-8");
                    writer.println(messageAsJson.get("data").getAsString());
                    writer.close();
                } catch (IOException e) {
                    System.out.println("error in writing the rules");
                    return;
                }
                s.setRules(MessageProcessor.getIntitialRules().toString());

        }

    }


    private void createIgnoredFileList() {

        List<String> files = new ArrayList<>(Arrays.asList(".idea", "out", "source_xml.xml", "tempResultXmlFile.xml", "testProject.iml"));
        for (String f : files) {
            VirtualFile vfile = ProjectManager.getInstance().getOpenProjects()[0].getBaseDir().findFileByRelativePath(f);
            if (vfile != null) {
                ignoredFiles.add(vfile);
            }
        }
    }

    // checks if we should ignore a file
    private boolean shouldIgnoreFile(VirtualFile s) {
        if (ignoredFiles == null) {
            return false;
        }
        for (VirtualFile vfile : ignoredFiles) {
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

    // when the text of a file changes
    private void handleVFileChangeEvent(VFileEvent event) {
        VirtualFile file = event.getFile();

        // if we are dealing with ruleJson.txt
        if (file.getName().equals("ruleJson.txt")) {
            s.setRules(MessageProcessor.getIntitialRules().toString());
            return;
        }

        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);

        // do not handle if the file is not a part of the project
        if (!PsiManager.getInstance(project).isInProject(psiFile)) {
            return;
        }

        s.setXml(SRCMLHandler.createXMLFile(project.getBasePath()));

    }

    // when a file is created
    private void handleVFileCreateEvent(VFileEvent event) {

        VirtualFile file = event.getFile();
        Project project = ProjectManager.getInstance().getOpenProjects()[0];

        if (!file.isDirectory()) {
            s.setXml(SRCMLHandler.createXMLFile(project.getBasePath()));
        }

    }

    // when a file's properties change. for instance, if the file is renamed.
    private void handleVFilePropertyChangeEvent(VFileEvent event) {

        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        s.setXml(SRCMLHandler.createXMLFile(project.getBasePath()));

    }

    // when a file is deleted
    private void handleVFileDeleteEvent(VFileEvent event) {

        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        s.setXml(SRCMLHandler.createXMLFile(project.getBasePath()));

    }
}
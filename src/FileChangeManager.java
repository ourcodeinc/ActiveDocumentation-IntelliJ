
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

import core.model.SRCMLHandler;

import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.io.PrintWriter;


public class FileChangeManager implements ProjectComponent {

    private final MessageBusConnection connection;
    private ChatServer s;

    public FileChangeManager(ChatServer server) {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        s = server;

    }

    public void initComponent() {
        // What is the difference of opening the socket here vs. when toolWindow is created?
        s.start();

        System.out.println("ChatServer started on port: " + s.getPort());

        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {

                // TODO handle events
                // update rules in ChatServer

                for (VFileEvent event : events) {
                    if (event instanceof VFileCreateEvent) {
                        System.out.println("CREATE");
                    } else if (event instanceof VFileContentChangeEvent) {
                        System.out.println("CHANGE");
                    } else if (event instanceof VFileDeleteEvent) {
                        System.out.println("DEL");
                    } else if (event instanceof VFilePropertyChangeEvent) {
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

        // TODO  switch case for command
        switch (command) {
            case "xmlResult":
                String projectPath = ProjectManager.getInstance().getOpenProjects()[0].getBasePath();
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


        }

    }


}
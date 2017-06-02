//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.openapi.vfs.VirtualFileManager;
//import com.intellij.openapi.vfs.newvfs.BulkFileListener;
//import com.intellij.openapi.vfs.newvfs.events.*;
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.PsiJavaFile;
//import com.intellij.psi.PsiManager;
//import com.intellij.openapi.project.DumbService;
//import com.intellij.openapi.components.ApplicationComponent;
//import java.util.List;

import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.MessageBusConnection;

//import core.model.PsiJavaVisitor;
//import org.java_websocket.WebSocketImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;


// Listens for changes to files and then sends the changes over to the web-client
// Note: VirtualFiles can also refer to directories (not just files)
public class FileChangeManager implements ProjectComponent { // ApplicationComponent, BulkFileListener

    //DumbService.getInstance(project).runWhenSmart( () -> {} );

    private final MessageBusConnection connection;
    private ChatServer s;

    private static JsonObject initialClassTable = new JsonObject();

    public FileChangeManager(ChatServer server) {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        s = server;
    }

    public void initComponent() {

        s.start();
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "INITIAL_PROJECT_CLASS_TABLE", initialClassTable}).toString());
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE_TABLE_AND_CONTAINER", sendRulesInitially()}).toString());
        System.out.println("(initComponent) " + "ChatServer started on port: " + s.getPort());


        System.out.println("(initComponent)");
        //connection.subscribe(VirtualFileManager.VFS_CHANGES, this);
    }

    public void disposeComponent() {
        connection.disconnect();
    }


    @NotNull
    @Override
    public String getComponentName() {
        return "";
    }

    // ProjectComponent

    @Override
    public void projectOpened() {


        Project project = ProjectManager.getInstance().getOpenProjects()[0]; // Correct Way?
        project.getBaseDir();
        System.out.println("Project Opened.");


    }

    @Override
    public void projectClosed() {

    }


    // returns a JSONObject with the initial rules from ruleJson.txt (the file where users modify rules)
    private static JsonObject sendRulesInitially() {
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
}
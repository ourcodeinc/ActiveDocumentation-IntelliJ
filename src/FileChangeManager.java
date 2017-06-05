
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.*;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class FileChangeManager implements ProjectComponent {

    private final MessageBusConnection connection;
    private ChatServer s;
    private String command;

    private static JsonObject initialClassTable = new JsonObject();

    public FileChangeManager(ChatServer server, String path) {
        connection = ApplicationManager.getApplication().getMessageBus().connect();
        s = server;
        command = "srcml --verbose " + path
                + "/src -o /Users/saharmehrpour/Documents/Workspace/testProject/source_xml.xml";

        System.out.println(command);

    }

    public void initComponent() {
        // What is the difference of opening the socket here vs. when toolWindow is created?
        s.start();
        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "INITIAL_PROJECT_CLASS_TABLE", initialClassTable}).toString());

        s.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE_TABLE_AND_CONTAINER",
                MessageProcessor.sendRulesInitially()}).toString());

        System.out.println("ChatServer started on port: " + s.getPort());

        connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener.Adapter() {
            @Override
            public void after(@NotNull List<? extends VFileEvent> events) {

                // TODO handle events

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

                    //runShellCommand(command);
                }

            }

            private void runShellCommand(String command) {
                try {
                    Process p = Runtime.getRuntime().exec(command);

                    BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

                    String line;
                    System.out.println("Output: ");
                    while ((line = stdInput.readLine())!= null) {
                        System.out.println(line);
                    }

                    System.out.println("Errors: ");
                    while ((line = stdError.readLine())!= null) {
                        System.out.println(line);
                    }

                } catch (IOException e) {
                    System.out.println("Exception: ");
                    e.printStackTrace();
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



}
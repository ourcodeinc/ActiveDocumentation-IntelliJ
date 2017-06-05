/*
 * This module creates the GUI for the plugin
 */

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import org.java_websocket.WebSocketImpl;
import org.jetbrains.annotations.NotNull;
import com.intellij.ui.content.*;


import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class GrepServerToolWindowFactory implements ToolWindowFactory {

    private ChatServer s;

    private JButton button;
    private JPanel panel;

    /**
     * This function creates the GUI for the plugin.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {


        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        button.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                    }
                }
        );

        System.out.println("(createToolWindowContent)");

        MessageProcessor.getIgnoredFilesList(project.getBaseDir());

        // start the connection when the project is loaded and indexing is done
        try {
            WebSocketImpl.DEBUG = false;
            int port = 8887; // 843 flash policy port

            // generateProjectHierarchyAsJSON() populates initialClassTable as well
            s = new ChatServer(port, MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "XML_PATH",
                    project.getBasePath()+ "/source_xml.xml"}).toString());

        } catch (Exception e) {
            e.printStackTrace();

        }

        // This will allow file changes to be sent to the web client
        FileChangeManager fcm = new FileChangeManager(s, project.getBasePath());
        fcm.initComponent();

    }


}


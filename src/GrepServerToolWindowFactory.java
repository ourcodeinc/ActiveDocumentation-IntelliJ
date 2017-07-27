/*
 * This module creates the GUI for the plugin
 */

import core.model.SRCMLxml;
import core.model.SRCMLHandler;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.java_websocket.WebSocketImpl;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import javax.swing.*;
import java.io.File;

public class GrepServerToolWindowFactory implements ToolWindowFactory {

    private ChatServer s;
    private WebEngine webEngine;
    private WebView browser;

    /**
     * This function creates the GUI for the plugin.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        final JFXPanel fxPanel = new JFXPanel();

        Platform.setImplicitExit(false);
        Platform.runLater(() -> {

            Scene scene = new Scene(new Group());
            VBox root = new VBox();
            browser = new WebView();
            webEngine = browser.getEngine();

            publishServices();

            root.getChildren().addAll(browser);
            scene.setRoot(root);

            fxPanel.setScene(scene);
        });

        JComponent component = toolWindow.getComponent();
        component.add(fxPanel);

        System.out.println("(createToolWindowContent)");

        // start the connection when the project is loaded and indexing is done
        try {
            WebSocketImpl.DEBUG = false;
            int port = 8887; // 843 flash policy port

            // generateProjectHierarchyAsJSON() populates initialClassTable as well
            s = new ChatServer(port);

        } catch (Exception e) {
            e.printStackTrace();

        }

        // This will allow file changes to be sent to the web client

        FileChangeManager fcm = new FileChangeManager(s,
                SRCMLHandler.createXMLForProject(new SRCMLxml(FileChangeManager.getFilePaths(project),
                        project.getBasePath())),
                MessageProcessor.getIntitialRules().toString());
        s.setManager(fcm);

        fcm.initComponent();

    }


    private synchronized void publishServices() {
        try {
            webEngine.setJavaScriptEnabled(true);
            File file = new File("/Users/saharmehrpour/Documents/Workspace/ActiveDocumentation/website-client/chat.html");
            System.out.println(file.exists() + " file existence");
            webEngine.load(file.toURI().toURL().toString());
        } catch (Exception ex) {
            System.err.print("error " + ex.getMessage());
            ex.printStackTrace();
        }
    }


}


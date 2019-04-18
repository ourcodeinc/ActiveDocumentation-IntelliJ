/**
 * edited by saharmehrpour
 * This module creates the GUI for the plugin
 */

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import javax.swing.*;

public class CustomToolWindowFactory implements ToolWindowFactory {

    private WebEngine webEngine;
    private WebView browser;

    /**
     * This function creates the GUI for the plugin.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        // generate the GUI

        System.out.println("(createToolWindowContent)");

        final JFXPanel fxPanel = new JFXPanel();

        Platform.setImplicitExit(false);
        Platform.runLater(() -> {

            StackPane root = new StackPane();
            Scene scene = new Scene(root);

            browser = new WebView();
            webEngine = browser.getEngine();
            publishServices();

            root.getChildren().add(browser);
            fxPanel.setScene(scene);

        });

        JComponent component = toolWindow.getComponent();
        component.add(fxPanel);


    }


    private synchronized void publishServices() {
        try {

            String content = "<!doctype html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"utf-8\">\n" +
                    "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1, shrink-to-fit=no\">\n" +
                    "    <meta name=\"theme-color\" content=\"#000000\">\n" +
                    "\n" +
                    "    <title>Active Documentation</title>\n" +
                    "\n" +
                    "</head>\n" +
                    "<body style='font-family: \"Helvetica Neue\",Helvetica,Arial,sans-serif;font-size: 14px;line-height: 1.42857143;'>\n" +
                    "<div style=\"margin: 20%\">Connected to the WebSocket.<br>\n" +
                    "    Open <span style=\"color: #337ab7;text-decoration: none;background-color: transparent;\">index.html</span> in Chrome.\n" +
                    "</div>\n" +
                    "</body>\n" +
                    "</html>\n";
            webEngine.loadContent(content, "text/html");


//            webEngine.setJavaScriptEnabled(true);
//            webEngine.load("http://localhost:3000/");
//            File file = new File(this.getClass().getClassLoader().getResource("").getPath() + "index.html");
//            webEngine.load(file.toURI().toURL().toString());
        } catch (Exception ex) {
            System.err.print("error " + ex.getMessage());
            ex.printStackTrace();
        }
    }


}


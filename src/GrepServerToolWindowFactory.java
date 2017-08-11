/**
 * edited by saharmehrpour
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;

import javax.swing.*;
import java.io.File;

import java.io.IOException;
import java.net.ServerSocket;

public class GrepServerToolWindowFactory implements ToolWindowFactory {

    private ChatServer s;
    private WebEngine webEngine;
    private WebView browser;

    /**
     * This function creates the GUI for the plugin.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        // start the connection when the project is loaded and indexing is done
        try {
            WebSocketImpl.DEBUG = false;
            int port = findAvailablePort(new int[]{8887, 8888, 8889, 8885, 8886}); // 843 flash policy port
            s = new ChatServer(port);
            System.out.println("-> started on port: " + port);

        } catch (Exception e) {
            System.out.println("Error in creating a Chat server.");
            e.printStackTrace();
        }

        // This will allow file changes to be sent to the web client

        System.out.println("-> project path: " + project.getBasePath());

        SRCMLxml srcml = new SRCMLxml(FileChangeManager.getFilePaths(project), project.getBasePath());
        SRCMLHandler.createXMLForProject(srcml);
        System.out.println("XML data is created.");

        FileChangeManager fcm = new FileChangeManager(s, srcml/*, MessageProcessor.getInitialRules().toString()*/
                , MessageProcessor.getInitialRulesAsList()
                , MessageProcessor.getInitialTagsAsList());
        s.setManager(fcm);

        fcm.initComponent();


        // generate the GUI

        System.out.println("(createToolWindowContent)");

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

//        // test
//        //String ruleFilePath = project.getBasePath() + "/ruleJson.txt";
//
//
//        String JSON_ARR = "["
//                + "{  \"geodata\": ["
//                + "    {"
//                + "      \"id\": \"1\","
//                + "      \"name\": \"Julie Sherman\","
//                + "      \"gender\" : \"female\","
//                + "      \"latitude\" : \"37.33774833333334\","
//                + "      \"longitude\" : \"-121.88670166666667\""
//                + "    },"
//                + "    {"
//                + "      \"id\": \"2\","
//                + "      \"name\": \"Johnny Depp\","
//                + "      \"gender\" : \"male\","
//                + "      \"latitude\" : \"37.336453\","
//                + "      \"longitude\" : \"-121.884985\""
//                + "    }"
//                + "  ]"
//                + "},"
//                + "{   \"geodata\": ["
//                + "    {"
//                + "      \"id\": \"3\","
//                + "      \"name\": \"Juman\","
//                + "      \"gender\" : \"female\","
//                + "      \"latitude\" : \"3334\","
//                + "      \"longitude\" : \"-7\""
//                + "    },"
//                + "    {"
//                + "      \"id\": \"4\","
//                + "      \"name\": \"John\","
//                + "      \"gender\" : \"male\","
//                + "      \"latitude\" : \"353\","
//                + "      \"longitude\" : \"-15\""
//                + "    }"
//                + "  ]"
//                + "}]";
//        try {
//            JSONArray objArr = new JSONArray(JSON_ARR);
//            int m = objArr.length();
//            for (int j = 0; j < m; ++j) {
//
//                JSONObject obj = objArr.getJSONObject(j);
//                JSONArray geodata = obj.getJSONArray("geodata");
//                int n = geodata.length();
//                for (int i = 0; i < n; ++i) {
//                    JSONObject person = geodata.getJSONObject(i);
//                    System.out.println(person.getInt("id"));
//                    System.out.println(person.toString());
//                    System.out.println("====");
////                    System.out.println(person.getString("name"));
////                    System.out.println(person.getString("gender"));
////                    System.out.println(person.getDouble("latitude"));
////                    System.out.println(person.getDouble("longitude"));
//                }
//
//            }
//            //System.out.println(objArr.toString());
//
//        } catch (JSONException e) {
//            System.out.println("errrorrr");
//            e.printStackTrace();
//        }


    }


    private synchronized void publishServices() {
        try {
            webEngine.setJavaScriptEnabled(true);
            File file = new File("/Users/saharmehrpour/Documents/Workspace/ActiveDocumentation/website-client/chat.html");
            webEngine.load(file.toURI().toURL().toString());
        } catch (Exception ex) {
            System.err.print("error " + ex.getMessage());
            ex.printStackTrace();
        }
    }


    private int findAvailablePort(int[] ports) throws IOException {
        ServerSocket tempSocket = null;
        boolean found = false;
        int foundPort = 0;
        for (int port : ports) {

            try {
                tempSocket = new ServerSocket(port);
                found = true;
                foundPort = port;
                break;
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        if (!found)
            throw new IOException("no free port found");

        else {
            tempSocket.close();
            return foundPort;
        }
    }


}


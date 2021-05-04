/*
 * written by saharmehrpour
 * This class processes anything that is related to mining/learning design rules.
 * The class should be "singleton". It is only created once in FileChangeManager.java
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.project.Project;
import core.model.LearnDesignRules;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

class MiningRulesProcessor {
    private ChatServer ws;
    private Project currentProject;
    private String projectPath;
    private final String directory = "/NewLearningDR";

    // list of messages received through web socket and should be processed in this class
    final List<String> wsMessages = Arrays.asList(
            WebSocketConstants.RECEIVE_REFRESH_LEARN_DESIGN_RULES_DIRECTORY_MSG,
            WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_DATABASES_MSG,
            WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_DATABASES_APPEND_MSG,
            WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_FEATURES_MSG,
            WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_FEATURES_APPEND_MSG,
            WebSocketConstants.RECEIVE_MINE_DESIGN_RULES_MSG
    );
    private static MiningRulesProcessor thisClass = null;

    MiningRulesProcessor(Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;
        thisClass = this;

        EditorFactory.getInstance()
                .getEventMulticaster()
                .addCaretListener(new CaretListener() {
                    @Override
                    public void caretPositionChanged(@NotNull CaretEvent event) {
                        Caret caret = event.getCaret();
                        assert caret != null;
                        int selectionStart = caret.getSelectionStart();
                        int selectionEnd = caret.getSelectionEnd();
                        DoiProcessing.getInstance().findCaretLocations(selectionStart, selectionEnd);
                    }
                }, ApplicationManager.getApplication());
    }

    static MiningRulesProcessor getInstance() {
        if (thisClass == null) new MiningRulesProcessor(null, null);
        return thisClass;
    }

    public void updateProjectWs (Project project, ChatServer ws) {
        this.currentProject = project;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;
    }

    /**
     * process the message received from the client
     *
     * @param messageAsJson JsonObject
     */
    void processReceivedMessages(JsonObject messageAsJson) {
        String command = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_COMMAND).getAsString();
        if (currentProject == null) return;

        switch (command) {
            case WebSocketConstants.RECEIVE_REFRESH_LEARN_DESIGN_RULES_DIRECTORY_MSG:
                String path = projectPath.concat(this.directory);
                File directory = new File(path);
                Utilities.deleteDirectory(directory);
                break;

            case WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_DATABASES_MSG:
            case WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_FEATURES_MSG:
                JsonArray filePathData = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonArray();
                for (int i = 0; i < filePathData.size(); i++) {
                    writeDataToFileLearningDR(filePathData.get(i).getAsJsonArray().get(0).getAsString(),
                            filePathData.get(i).getAsJsonArray().get(1).getAsString(), false);
                }
                break;

            case WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_DATABASES_APPEND_MSG:
            case WebSocketConstants.RECEIVE_LEARN_DESIGN_RULES_FEATURES_APPEND_MSG:
                JsonArray filePathDataAppend = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonArray();
                for (int i = 0; i < filePathDataAppend.size(); i++) {
                    writeDataToFileLearningDR(filePathDataAppend.get(i).getAsJsonArray().get(0).getAsString(),
                            filePathDataAppend.get(i).getAsJsonArray().get(1).getAsString(), true);
                }
                break;

            case WebSocketConstants.RECEIVE_MINE_DESIGN_RULES_MSG:
                String utility = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()
                        .get("utility").getAsString();
                String alg = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()
                        .get("algorithm").getAsString();
                JsonObject outputContent = LearnDesignRules.analyzeDatabases(projectPath, utility, this.directory, alg);
                // send message
                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_MINED_DESIGN_RULES,
                        MessageProcessor.encodeMinedRules(new Object[]{outputContent})
                }).toString());
                break;
        }
    }

    /**
     * write in file
     *
     * @param fileName name of files
     * @param content  content of the file
     */
    private void writeDataToFileLearningDR(String fileName, String content, boolean append) {
        String directoryName = projectPath.concat(this.directory);
        File directory = new File(directoryName);
        if (!directory.exists()) {
            boolean result = directory.mkdir();
            if (!result) return;
        }

        try {
            PrintWriter writer;
            if (append) {
                writer = new PrintWriter(new FileOutputStream(
                        projectPath + this.directory + "/" + fileName, true));
            } else {
                writer = new PrintWriter(projectPath + this.directory + "/" + fileName, "UTF-8");
            }
            writer.print(content);
            writer.close();
        } catch (IOException e) {
            System.out.println("error in writing " + fileName);
        }
    }

    /**
     * send a message to the web-app
     *
     * @param message the formatted message, sent exactly as it is given
     */
    private void sendMessage(String message) {
        if (ws != null) {
            ws.sendToAll(message);
        }
    }
}

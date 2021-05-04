/*
 * written by saharmehrpour
 * This class processes anything that is related to following and authoring (RulePad) design rules.
 * The class should be "singleton". It is only created once in FileChangeManager.java
 */

import com.google.gson.JsonObject;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import core.model.SRCMLHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.awt.*;

class FollowAndAuthorRulesProcessor {

    private ChatServer ws;
    private HashMap<String, String> ruleTable; // ruleID, {index: string, ...}
    private HashMap<String, String> tagTable; // tagID, {ID: string, ...}
    private Project currentProject;
    private String projectPath;

    final List<String> wsMessages = Arrays.asList(
            WebSocketConstants.RECEIVE_SNIPPET_XML_MSG,
            WebSocketConstants.RECEIVE_MODIFIED_RULE_MSG,
            WebSocketConstants.RECEIVE_MODIFIED_TAG_MSG,
            WebSocketConstants.RECEIVE_CODE_TO_XML_MSG,
            WebSocketConstants.RECEIVE_NEW_RULE_MSG,
            WebSocketConstants.RECEIVE_NEW_TAG_MSG
    );
    private static FollowAndAuthorRulesProcessor thisClass = null;

    FollowAndAuthorRulesProcessor(Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;

        this.tagTable = Utilities.getInitialTagTable(currentProject);
        this.ruleTable = Utilities.getInitialRuleTable(currentProject);

        thisClass = this;
    }

    static FollowAndAuthorRulesProcessor getInstance() {
        if (thisClass == null) new FollowAndAuthorRulesProcessor(null, null);
        return thisClass;
    }

    public void updateProjectWs (Project project, ChatServer ws) {
        this.currentProject = project;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;

        this.tagTable = Utilities.getInitialTagTable(currentProject);
        this.ruleTable = Utilities.getInitialRuleTable(currentProject);
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
            case WebSocketConstants.RECEIVE_SNIPPET_XML_MSG:
                try {
                    PrintWriter writer = new PrintWriter(projectPath + "/" + Constants.TEMP_XML_FILE, "UTF-8");
                    writer.println(Constants.XML_HEADER);
                    writer.println(messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()
                            .get("xml").getAsString());
                    writer.close();
                } catch (IOException e) {
                    System.out.println("error in writing the result xml");
                    return;
                }

                EventQueue.invokeLater(() -> {
                    String fileRelativePath = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                            .getAsJsonObject().get("fileName").getAsString();
                    String relativePath = fileRelativePath.startsWith("/") ? fileRelativePath : "/" + fileRelativePath;
                    VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(relativePath);
                    if (fileByPath != null) {
                        FileEditorManager.getInstance(currentProject).openFile(fileByPath, true);
                        Editor theEditor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
                        int indexToFocusOn = SRCMLHandler.findLineNumber(projectPath + "/" + Constants.TEMP_XML_FILE);
                        if (theEditor != null) {
                            theEditor.getCaretModel().moveToOffset(indexToFocusOn);
                            theEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                        }
                    }
                });
                break;

            case WebSocketConstants.RECEIVE_MODIFIED_RULE_MSG:
                // data: {ruleID: longNumber, ruleInfo: {...}}
                String ruleID = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("ruleID").getAsString();
                String ruleInfo = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("ruleInfo").getAsJsonObject().toString();

                boolean ruleResult = this.updateRule(ruleID, ruleInfo);
                if (!ruleResult)
                    sendMessage(MessageProcessor.encodeData(new Object[]{
                            WebSocketConstants.SEND_FAILED_UPDATE_RULE_MSG,
                            messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                                    .getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + Constants.RULE_TABLE_JSON, this.ruleTable);
                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_UPDATE_RULE_MSG,
                        messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()}).toString());
                break;

            case WebSocketConstants.RECEIVE_MODIFIED_TAG_MSG:
                // data: {tagID: longNumber, tagInfo: {...}}
                String tagID = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("tagID").getAsString();
                String tagInfo = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("tagInfo").getAsJsonObject().toString();

                boolean result = this.updateTag(tagID, tagInfo);
                if (!result)
                    sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_FAILED_UPDATE_TAG_MSG,
                            messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + Constants.TAG_TABLE_JSON, this.tagTable);
                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_UPDATE_TAG_MSG,
                        messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()}).toString());
                break;

            case WebSocketConstants.RECEIVE_CODE_TO_XML_MSG:
                String exprText = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("codeText").getAsString();
                String resultExprXml = SRCMLHandler.createXMLForText(exprText, projectPath + "/" + Constants.TEMP_JAVA_FILE);
                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_XML_FROM_CODE_MSG,
                        MessageProcessor.encodeXMLandText(new Object[]{resultExprXml,
                                messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                                        .getAsJsonObject().get("messageID").getAsString()})
                }).toString());
                break;

            case WebSocketConstants.RECEIVE_NEW_RULE_MSG:
                // data: {ruleID: longNumber, ruleInfo: {...}}
                String newRuleID = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("ruleID").getAsString();
                String newRuleInfo = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("ruleInfo").getAsJsonObject().toString();

                boolean newRuleResult = this.addNewRule(newRuleID, newRuleInfo);
                if (!newRuleResult)
                    sendMessage(MessageProcessor.encodeData(new Object[]{
                            WebSocketConstants.SEND_FAILED_NEW_RULE_MSG,
                            messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + Constants.RULE_TABLE_JSON, this.ruleTable);
                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_NEW_RULE_MSG,
                        messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()}).toString());
                break;

            case WebSocketConstants.RECEIVE_NEW_TAG_MSG:
                // data: {tagID: String, tagInfo: {...}}
                String newTagID = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("tagID").getAsString();
                String newTagInfo = messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA)
                        .getAsJsonObject().get("tagInfo").getAsJsonObject().toString();

                boolean newResult = this.addNewTag(newTagID, newTagInfo);
                if (!newResult)
                    sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_FAILED_NEW_TAG_MSG,
                            messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + Constants.TAG_TABLE_JSON, this.tagTable);
                sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_NEW_TAG_MSG,
                        messageAsJson.get(WebSocketConstants.MESSAGE_KEY_DATA).getAsJsonObject()}).toString());
                break;
        }
    }

    /**
     * update a tag in tagTable
     *
     * @param ruleID          the ID of an existing rule
     * @param updatedRuleInfo the tag information that is stored in ruleTable.json
     * @return false if no ID is found
     */
    private boolean updateRule(String ruleID, String updatedRuleInfo) {
        if (this.ruleTable.get(ruleID) == null) return false;
        this.ruleTable.put(ruleID, updatedRuleInfo);
        return true;
    }

    /**
     * add a new tag in tagTable
     *
     * @param newRuleID   the new and unique ID
     * @param newRuleInfo the tag information that is stored in ruleTable.json
     * @return false if the ID exists in the table
     */
    private boolean addNewRule(String newRuleID, String newRuleInfo) {
        if (this.ruleTable.get(newRuleID) != null) return false;
        this.ruleTable.put(newRuleID, newRuleInfo);
        return true;
    }

    /**
     * update a tag in tagTable
     *
     * @param tagID          the ID of an existing tag
     * @param updatedTagInfo the tag information that is stored in tagTable.json
     * @return false if no ID is found
     */
    private boolean updateTag(String tagID, String updatedTagInfo) {
        if (this.tagTable.get(tagID) == null) return false;
        this.tagTable.put(tagID, updatedTagInfo);
        return true;
    }

    /**
     * add a new tag in tagTable
     *
     * @param newTagID   the new and unique ID
     * @param newTagInfo the tag information that is stored in tagTable.json
     * @return false if the ID exists in the table
     */
    private boolean addNewTag(String newTagID, String newTagInfo) {
        if (this.tagTable.get(newTagID) != null) return false;
        this.tagTable.put(newTagID, newTagInfo);
        return true;
    }

    /**
     * get the string of all rules to send to the client
     *
     * @return string
     */
    String getRuleTable() {
        StringBuilder ruleTableString = new StringBuilder("[");
        String prefix = "";
        for (String tag : this.ruleTable.values()) {
            ruleTableString.append(prefix);
            ruleTableString.append(tag);
            prefix = ",";
        }
        return ruleTableString + "]";
    }

    /**
     * get the string of all tags to send to the client
     *
     * @return string
     */
    String getTagTable() {
        StringBuilder tagTableString = new StringBuilder("[");
        String prefix = "";
        for (String tag : this.tagTable.values()) {
            tagTableString.append(prefix);
            tagTableString.append(tag);
            prefix = ",";
        }
        return tagTableString + "]";
    }

    /**
     * update the ruleIndexText and send messages to clients
     */
    void updateRules() {
        this.ruleTable = Utilities.getInitialRuleTable(currentProject);

        sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_RULE_TABLE_MSG,
                this.getRuleTable()}).toString());
        sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_VERIFY_RULES_MSG,
                ""}).toString());
    }

    /**
     * update the tagNameText and send messages to clients
     */
    void updateTags() {
        this.tagTable = Utilities.getInitialTagTable(currentProject);
        sendMessage(MessageProcessor.encodeData(new Object[]{WebSocketConstants.SEND_TAG_TABLE_MSG,
                this.getTagTable()}).toString());
    }

    /**
     * send the message through web socket
     * @param message the processed string
     */
    private void sendMessage(String message) {
        if (ws != null)
            ws.sendToAll(message);
    }
}

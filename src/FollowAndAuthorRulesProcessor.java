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

    final List<String> wsMessages = Arrays.asList("XML_RESULT", "MODIFIED_RULE", "MODIFIED_TAG", "EXPR_STMT", "NEW_RULE", "NEW_TAG");
    private static FollowAndAuthorRulesProcessor thisClass = null;

    FollowAndAuthorRulesProcessor(String projectPath, Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = projectPath;
        this.ws = ws;

        this.tagTable = Utilities.getInitialTagTable(currentProject);
        this.ruleTable = Utilities.getInitialRuleTable(currentProject);

        thisClass = this;
    }

    static FollowAndAuthorRulesProcessor getInstance() {
        if (thisClass == null) new FollowAndAuthorRulesProcessor("", null, null);
        return thisClass;
    }

    /**
     * process the message received from the client
     *
     * @param messageAsJson JsonObject
     */
    void processReceivedMessages(JsonObject messageAsJson) {

        String command = messageAsJson.get("command").getAsString();
        if (currentProject == null) return;

        switch (command) {
            case "XML_RESULT":
                try {
                    PrintWriter writer = new PrintWriter(projectPath + "/tempResultXmlFile.xml", "UTF-8");
                    writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
                    writer.println(messageAsJson.get("data").getAsJsonObject().get("xml").getAsString());
                    writer.close();
                } catch (IOException e) {
                    System.out.println("error in writing the result xml");
                    return;
                }

                EventQueue.invokeLater(() -> {
                    String fileRelativePath = messageAsJson.get("data").getAsJsonObject().get("fileName").getAsString();
                    String relativePath = fileRelativePath.startsWith("/") ? fileRelativePath : "/" + fileRelativePath;
                    VirtualFile fileByPath = LocalFileSystem.getInstance().findFileByPath(relativePath);
                    if (fileByPath != null && currentProject != null) {
                        FileEditorManager.getInstance(currentProject).openFile(fileByPath, true);
                        Editor theEditor = FileEditorManager.getInstance(currentProject).getSelectedTextEditor();
                        int indexToFocusOn = SRCMLHandler.findLineNumber(projectPath + "/tempResultXmlFile.xml");
                        if (theEditor != null) {
                            theEditor.getCaretModel().moveToOffset(indexToFocusOn);
                            theEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                        }
                    }
                });
                break;

            case "MODIFIED_RULE":
                // data: {ruleID: longNumber, ruleInfo: {...}}
                String ruleID = messageAsJson.get("data").getAsJsonObject().get("ruleID").getAsString();
                String ruleInfo = messageAsJson.get("data").getAsJsonObject().get("ruleInfo").getAsJsonObject().toString();

                boolean ruleResult = this.updateRule(ruleID, ruleInfo);
                if (!ruleResult)
                    if (ws != null)
                        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_UPDATE_RULE", messageAsJson.get("data").getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + "ruleTable.json", this.ruleTable);
                if (ws != null)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_RULE", messageAsJson.get("data").getAsJsonObject()}).toString());
                break;

            case "MODIFIED_TAG":
                // data: {tagID: longNumber, tagInfo: {...}}
                String tagID = messageAsJson.get("data").getAsJsonObject().get("tagID").getAsString();
                String tagInfo = messageAsJson.get("data").getAsJsonObject().get("tagInfo").getAsJsonObject().toString();

                boolean result = this.updateTag(tagID, tagInfo);
                if (!result)
                    if (ws != null)
                        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_UPDATE_TAG", messageAsJson.get("data").getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + "tagTable.json", this.tagTable);
                if (ws != null)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "UPDATE_TAG", messageAsJson.get("data").getAsJsonObject()}).toString());
                break;

            case "EXPR_STMT":
                String exprText = messageAsJson.get("data").getAsJsonObject().get("codeText").getAsString();
                String resultExprXml = SRCMLHandler.createXMLForText(exprText, projectPath + "/tempExprFile.java");
                if (ws != null) ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "EXPR_STMT_XML",
                        MessageProcessor.encodeXMLandText(new Object[]{resultExprXml, messageAsJson.get("data").getAsJsonObject().get("messageID").getAsString()})
                }).toString());
                break;

            case "NEW_RULE":
                // data: {ruleID: longNumber, ruleInfo: {...}}
                String newRuleID = messageAsJson.get("data").getAsJsonObject().get("ruleID").getAsString();
                String newRuleInfo = messageAsJson.get("data").getAsJsonObject().get("ruleInfo").getAsJsonObject().toString();

                boolean newRuleResult = this.addNewRule(newRuleID, newRuleInfo);
                if (!newRuleResult)
                    if (ws != null)
                        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_NEW_RULE", messageAsJson.get("data").getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + "ruleTable.json", this.ruleTable);
                if (ws != null)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_RULE", messageAsJson.get("data").getAsJsonObject()}).toString());
                break;

            case "NEW_TAG":
                // data: {tagID: String, tagInfo: {...}}
                String newTagID = messageAsJson.get("data").getAsJsonObject().get("tagID").getAsString();
                String newTagInfo = messageAsJson.get("data").getAsJsonObject().get("tagInfo").getAsJsonObject().toString();

                boolean newResult = this.addNewTag(newTagID, newTagInfo);
                if (!newResult)
                    if (ws != null)
                        ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FAILED_NEW_TAG", messageAsJson.get("data").getAsJsonObject()}).toString());

                Utilities.writeTableFile(this.projectPath + "/" + "tagTable.json", this.tagTable);
                if (ws != null)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "NEW_TAG", messageAsJson.get("data").getAsJsonObject()}).toString());
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
        if (this.tagTable.get(newRuleID) != null) return false;
        this.tagTable.put(newRuleID, newRuleInfo);
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

        sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "RULE_TABLE", this.getRuleTable()}).toString());
        sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "VERIFY_RULES", ""}).toString());
    }

    /**
     * update the tagNameText and send messages to clients
     */
    void updateTags() {
        this.tagTable = Utilities.getInitialTagTable(currentProject);
        sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "TAG_TABLE", this.getTagTable()}).toString());
    }

    /**
     * send the message through web socket
     * @param msg the processed string
     */
    private void sendMessage(String msg) {
        if (ws != null)
            ws.sendToAll(msg);
    }
}

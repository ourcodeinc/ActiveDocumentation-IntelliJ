/*
 * written by saharmehrpour
 * This class processes anything that is related to mining/learning design rules.
 * The class should be "singleton". It is only created once in FileChangeManager.java
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.project.Project;
import core.model.FPMaxHandler;
import core.model.MiningRulesUtilities;

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

    // list of messages received through web socket and should be processed in this class
    final List<String> wsMessages = Arrays.asList("LEARN_RULES_META_DATA", "LEARN_RULES_FILE_LOCATIONS", "LEARN_RULES_DATABASES",
            "LEARN_RULES_META_DATA_APPEND", "LEARN_RULES_FILE_LOCATIONS_APPEND", "LEARN_RULES_DATABASES_APPEND",
            "EXECUTE_FP_MAX", "DANGEROUS_READ_MINED_RULES");
    private static MiningRulesProcessor thisClass = null;

    MiningRulesProcessor(String projectPath, Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = projectPath;
        this.ws = ws;

        thisClass = this;
    }

    static MiningRulesProcessor getInstance() {
        if (thisClass == null) new MiningRulesProcessor("", null, null);
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
            case "LEARN_RULES_META_DATA":
                // "attribute_META_data.txt"

                // assumption: the first received data is the meta data
                String path = projectPath.concat("/LearningDR");
                File directory = new File(path);
                Utilities.deleteDirectory(directory);

            case "LEARN_RULES_FILE_LOCATIONS":
                // "fileLocations.txt"
            case "LEARN_RULES_DATABASES":
                // analysisFileName + "_subClassOf_" + parentClass + ".txt"
                // analysisFileName = "AttributeEncoding"

                JsonArray filePathData = messageAsJson.get("data").getAsJsonArray();
                for (int i = 0; i < filePathData.size(); i++) {
                    writeDataToFileLearningDR(filePathData.get(i).getAsJsonArray().get(0).getAsString(),
                            filePathData.get(i).getAsJsonArray().get(1).getAsString(), false);
                }
                break;

            case "LEARN_RULES_META_DATA_APPEND":
                // assumption: the first received data is the meta data
                int cnt = messageAsJson.get("part").getAsInt();
                if (cnt == 0) {
                    String dirPath = projectPath.concat("/LearningDR");
                    File directoryFile = new File(dirPath);
                    Utilities.deleteDirectory(directoryFile);
                }

            case "LEARN_RULES_FILE_LOCATIONS_APPEND":
            case "LEARN_RULES_DATABASES_APPEND":
                JsonArray filePathDataAppend = messageAsJson.get("data").getAsJsonArray();
                for (int i = 0; i < filePathDataAppend.size(); i++) {
                    writeDataToFileLearningDR(filePathDataAppend.get(i).getAsJsonArray().get(0).getAsString(),
                            filePathDataAppend.get(i).getAsJsonArray().get(1).getAsString(), true);
                }
                break;

            case "EXECUTE_FP_MAX":
                int support = messageAsJson.get("data").getAsInt();
                JsonObject outputContent = FPMaxHandler.analyzeDatabases(projectPath, support);
                // send message
                if (ws != null) ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FP_MAX_OUTPUT",
                        MessageProcessor.encodeFPMaxOutput(new Object[]{outputContent})
                }).toString());
                break;

            case "DANGEROUS_READ_MINED_RULES":
                JsonObject outputContentMinedData = MiningRulesUtilities.readMinedRulesFile(projectPath);
                if (outputContentMinedData == null) {
                    System.out.println("Error happened in reading files.");
                    break;
                }
                if (ws != null)
                    ws.sendToAll(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "DANGEROUS_READ_MINED_RULES",
                            MessageProcessor.encodeDangerousMinedData(
                                    new Object[]{outputContentMinedData.get("output").toString(), outputContentMinedData.get("metaData").toString()}
                            )}).toString());
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
        String directoryName = projectPath.concat("/LearningDR");
        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdir();
        }

        try {
            PrintWriter writer;
            if (append) {
                writer = new PrintWriter(new FileOutputStream(
                        new File(projectPath + "/LearningDR/" + fileName), true /* append = true */));
            } else {
                writer = new PrintWriter(projectPath + "/LearningDR/" + fileName, "UTF-8");
            }
            writer.print(content);
            writer.close();
        } catch (IOException e) {
            System.out.println("error in writing " + fileName);
        }
    }
}

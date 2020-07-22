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
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import core.model.FPMaxHandler;
import core.model.MiningRulesUtilities;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

class MiningRulesProcessor {
    private final ChatServer ws;

    private final Project currentProject;
    private final String projectPath;


    private String currentFilePathForSearch = "";
    private String[] searchHistoryRaw = {}; // the raw history received from FindInProjectSettings.getInstance(project).getRecentFindStrings()

    private List<String[]> visitedFiles; // [filePath, numberOfVisits.toString()] todo check
    private List<List<String>> searchHistory; // [[searchTerms], [filePath1, filePath2]] todo check
    private List<List<String>> caretLocations; // [[filePath], [offSet.toString(), otherUsefulInfo]] todo check

    private Map<String, ArrayList<ArrayList<Integer>>> visitedElements; // <filePath, [[startOffset1, endOffset1]]]>
    private Map<String, Integer> visitedPaths; // <filePath, numberOfVisits>
    TreeMap<Integer, String> cursorLocations = new TreeMap<Integer, String>();

    // list of messages received through web socket and should be processed in this class
    final List<String> wsMessages = Arrays.asList("LEARN_RULES_META_DATA", "LEARN_RULES_FILE_LOCATIONS", "LEARN_RULES_DATABASES",
            "LEARN_RULES_META_DATA_APPEND", "LEARN_RULES_FILE_LOCATIONS_APPEND", "LEARN_RULES_DATABASES_APPEND",
            "EXECUTE_FP_MAX", "DANGEROUS_READ_MINED_RULES", "SEND_DOI_INFORMATION");
    private static MiningRulesProcessor thisClass = null;

    String getVisitedFiles() {
        String result = "";
        for (String path : visitedPaths.keySet()) {
            result += ("\"" + path + "\":" + visitedPaths.get(path) + ",");
        }
        if (result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        result = "{" + result + "}";
        return result;
        // todo
    }

    String getSearchHistory() {
        // todo
        return "";
    }

    String getVisitedElements() {
        String result = "";
        for (String path : visitedElements.keySet()) {
            result += ("\"" + path + "\":" + visitedElements.get(path).toString() + ",");
        }
        if (result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }
        result = "{" + result + "}";
        return result;
    }

    // unecessary, may need to change below
    String getCaretLocations() {
        return "";
    }

    void findCaretLocations(int startCaretLocation, int endCaretLocation) {
        String path = FileEditorManager.getInstance(currentProject).getSelectedFiles()[0].getCanonicalPath();
        if (path == null || !path.endsWith(".java")) return;
        ArrayList<Integer> location = new ArrayList<>(Arrays.asList(startCaretLocation, endCaretLocation));
        if (visitedElements.containsKey(path))
            visitedElements.get(path).add(location);
        else
            visitedElements.put(path, new ArrayList<>(Arrays.asList(location)));
    }

    MiningRulesProcessor(Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;

        this.visitedFiles = new ArrayList<>();
        this.searchHistory = new ArrayList<>();
        this.caretLocations = new ArrayList<>();
        this.visitedElements = new HashMap<>();
        this.visitedPaths = new HashMap<>();

        thisClass = this;

        EditorFactory.getInstance()
                .getEventMulticaster()
                .addCaretListener(new CaretListener() {
                    @Override
                    public void caretPositionChanged(@NotNull CaretEvent event) {
                        Caret caret = event.getCaret();
                        int selectionStart = caret.getSelectionStart();
                        int selectionEnd = caret.getSelectionEnd();
                        // todo checkout what is needed and pass it to the method
                        //  updateCaretLocations();
                        MiningRulesProcessor.getInstance().findCaretLocations(selectionStart, selectionEnd);
                        // getVisitedFiles();
                    }
                }, ApplicationManager.getApplication());
/*
        EditorFactory.getInstance()
                .getEventMulticaster()
                .addVisibleAreaListener(new VisibleAreaListener() {
                    @Override
                    public void visibleAreaChanged(@NotNull VisibleAreaEvent visibleAreaEvent) {
                        // todo what it does?
                    }
                }, ApplicationManager.getApplication());
                /*
 */
    }

    static MiningRulesProcessor getInstance() {
        if (thisClass == null) new MiningRulesProcessor(null, null);
        return thisClass;
    }

    /**
     * the path of the newly visited file
     *
     * @param filePath path of the newly opened/visited file
     */
    void newVisitedFile(String filePath) {
        updateVisitedFiles(filePath);
        updateSearchHistory(filePath);
        this.currentFilePathForSearch = filePath;
    }

    /**
     * add the newly visited file to the list
     *
     * @param newFilePath path of the newly opened file
     */
    void updateVisitedFiles(String newFilePath) {
        if (visitedPaths.containsKey(newFilePath))
            visitedPaths.put(newFilePath, visitedPaths.get(newFilePath) + 1);
        else
            visitedPaths.put(newFilePath, 1);
        // todo
        //  check if user has already visited the file
        //  update the field accordingly
    }

    // todo after completing the implementation add javaDoc. Type: /** just above the method definition and then press enter
    void updateSearchHistory(String newFilePath) {
        // todo
        //  we have raw search history
        //  we get the new search history
        //  we can compare them
        //  the diff of these two is the search terms of the old file
        //  update the fields
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
                sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "FP_MAX_OUTPUT",
                        MessageProcessor.encodeFPMaxOutput(new Object[]{outputContent})
                }).toString());
                break;

            case "DANGEROUS_READ_MINED_RULES":
                JsonObject outputContentMinedData = MiningRulesUtilities.readMinedRulesFile(projectPath);
                if (outputContentMinedData == null) {
                    System.out.println("Error happened in reading files.");
                    break;
                }
                sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "DANGEROUS_READ_MINED_RULES",
                        MessageProcessor.encodeDangerousMinedData(
                                new Object[]{outputContentMinedData.get("output").toString(), outputContentMinedData.get("metaData").toString()}
                        )}).toString());
                break;

            case "SEND_DOI_INFORMATION":
                // todo check and update if we have more data to send
                sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "DOI_INFORMATION",
                        MessageProcessor.encodeDoiInformation(
                                new Object[]{getVisitedFiles(), getSearchHistory(), getCaretLocations()}
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

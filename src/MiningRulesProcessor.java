/*
 * written by saharmehrpour
 * This class processes anything that is related to mining/learning design rules.
 * The class should be "singleton". It is only created once in FileChangeManager.java
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.find.FindInProjectSettings;
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

    private String currentFilePath = "";
    private String[] searchHistoryRaw = {}; // the raw history

    private final Map<String, ArrayList<ArrayList<Integer>>> visitedElements; // <filePath, [[startOffset1, endOffset1]]]>
    private final Map<String, Integer> visitedFiles; // <filePath, numberOfVisits>
    private final HashMap <String, List<String>> searchHistory; // <filePath, [[searchTerms], [filePath1, filePath2]]>

    // list of messages received through web socket and should be processed in this class
    final List<String> wsMessages = Arrays.asList("LEARN_RULES_META_DATA", "LEARN_RULES_FILE_LOCATIONS", "LEARN_RULES_DATABASES",
            "LEARN_RULES_META_DATA_APPEND", "LEARN_RULES_FILE_LOCATIONS_APPEND", "LEARN_RULES_DATABASES_APPEND",
            "EXECUTE_FP_MAX", "DANGEROUS_READ_MINED_RULES", "SEND_DOI_INFORMATION");
    private static MiningRulesProcessor thisClass = null;

    MiningRulesProcessor(Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;

        visitedElements = new HashMap<>();
        visitedFiles = new HashMap<>();
        searchHistory = new HashMap<>();

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
                        MiningRulesProcessor.getInstance().findCaretLocations(selectionStart, selectionEnd);
                    }
                }, ApplicationManager.getApplication());
    }

    static MiningRulesProcessor getInstance() {
        if (thisClass == null) new MiningRulesProcessor(null, null);
        return thisClass;
    }

    /**
     * convert the data for visitedFiles to String
     *
     * @return String
     */
    String getVisitedFiles() {
        StringBuilder result = new StringBuilder();
        for (String path : visitedFiles.keySet()) {
            result.append("\"").append(path).append("\":").append(visitedFiles.get(path)).append(",");
        }
        if (result.length() > 1) {
            result = new StringBuilder(result.substring(0, result.length() - 1));
        }
        result = new StringBuilder("{" + result + "}");
        return result.toString();
    }

    /**
     * convert the data for searchHistory to String
     * @return String
     */
    String getSearchHistory() {
        StringBuilder result = new StringBuilder();
        for (String path : searchHistory.keySet()) {
            result.append("\"").append(path).append("\":[");
            List<String> searchKeyWords = searchHistory.get(path);
            for (Object searchKeyWord : searchKeyWords) {
                result.append("\"").append(searchKeyWord).append("\",");
            }
            if (searchKeyWords.size() > 0) {
                result = new StringBuilder(result.substring(0, result.length() - 1));
            }
            result.append("],");
        }
        if (result.length() > 1) {
            result = new StringBuilder(result.substring(0, result.length() - 1));
        }
        result = new StringBuilder("{" + result + "}");
        return result.toString();
    }

    /**
     * convert the data for visitedElement to String
     *
     * @return String
     */
    String getVisitedElements() {
        StringBuilder result = new StringBuilder();
        for (String path : visitedElements.keySet()) {
            result.append("\"").append(path).append("\":").append(visitedElements.get(path).toString()).append(",");
        }
        if (result.length() > 1) {
            result = new StringBuilder(result.substring(0, result.length() - 1));
        }
        result = new StringBuilder("{" + result + "}");
        return result.toString();
    }

    void findCaretLocations(int startCaretLocation, int endCaretLocation) {
        String path = FileEditorManager.getInstance(currentProject).getSelectedFiles()[0].getCanonicalPath();
        if (path == null || !path.endsWith(".java")) return;
        ArrayList<Integer> location = new ArrayList<>(Arrays.asList(startCaretLocation, endCaretLocation));
        if (visitedElements.containsKey(path))
            visitedElements.get(path).add(location);
        else
            visitedElements.put(path, new ArrayList<>(Collections.singletonList(location)));
    }

    /**
     * the path of the newly visited file
     *
     * @param filePath path of the newly opened/visited file
     */
    void newVisitedFile(String filePath) {
        updateVisitedFiles(filePath);
        updateSearchHistory();
        currentFilePath = filePath;
    }

    /**
     * add the newly visited file to the list
     *
     * @param newFilePath path of the newly opened file
     */
    void updateVisitedFiles(String newFilePath) {
        if (visitedFiles.containsKey(newFilePath))
            visitedFiles.put(newFilePath, visitedFiles.get(newFilePath) + 1);
        else
            visitedFiles.put(newFilePath, 1);
    }


    /**
     * update the search history upon changing the active file in the editor
     */
    void updateSearchHistory() {
        String[] newSearchHistoryRaw = FindInProjectSettings.getInstance(currentProject).getRecentFindStrings();
        // there are old search terms
        if (currentFilePath.equals("")) {
            searchHistory.put("none", Arrays.asList(newSearchHistoryRaw));
            searchHistoryRaw = newSearchHistoryRaw;
        } else {
            // there are new items in Search History
            if (newSearchHistoryRaw.length > searchHistoryRaw.length) {
                // find the diff of the Search history
                List<String> diff = Arrays.asList(Arrays.copyOfRange(newSearchHistoryRaw,
                        searchHistoryRaw.length, newSearchHistoryRaw.length));
                // add to previous search terms
                if (searchHistory.containsKey(currentFilePath)) {
                    List<String> allSearch = searchHistory.get(currentFilePath);
                    allSearch.addAll(diff);
                    searchHistory.put(currentFilePath, allSearch);
                } else {
                    // create new entry
                    searchHistory.put(currentFilePath, diff);
                }
                // update the Search history
                searchHistoryRaw = newSearchHistoryRaw;
            }
        }
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
                sendMessage(MessageProcessor.encodeData(new Object[]{"IDEA", "WEB", "DOI_INFORMATION",
                        MessageProcessor.encodeDoiInformation(
                                new Object[]{getVisitedFiles(), getSearchHistory(), getVisitedElements()}
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

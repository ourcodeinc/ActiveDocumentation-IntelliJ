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
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
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

    private Map<String, ArrayList<ArrayList<Integer>>> visitedElements; // <filePath, [[startOffset1, endOffset1]]]>
    private Map<String, Integer> visitedPaths; // <filePath, numberOfVisits>

    private List<String> fileList;
    private HashMap<String, Integer> visitedFiles_A; // [filePath, numberOfVisits] todo check
    private HashMap <String, List<String>> searchHistory; // [[searchTerms], [filePath1, filePath2]] todo check

    // list of messages received through web socket and should be processed in this class
    final List<String> wsMessages = Arrays.asList("LEARN_RULES_META_DATA", "LEARN_RULES_FILE_LOCATIONS", "LEARN_RULES_DATABASES",
            "LEARN_RULES_META_DATA_APPEND", "LEARN_RULES_FILE_LOCATIONS_APPEND", "LEARN_RULES_DATABASES_APPEND",
            "EXECUTE_FP_MAX", "DANGEROUS_READ_MINED_RULES", "SEND_DOI_INFORMATION");
    private static MiningRulesProcessor thisClass = null;

    /**
     * convert the data for visitedFiles to String
     *
     * @return String
     */
    String getVisitedFiles() {
        StringBuilder result = new StringBuilder();
        for (String path : visitedPaths.keySet()) {
            result.append("\"").append(path).append("\":").append(visitedPaths.get(path)).append(",");
        }
        if (result.length() > 1) {
            result = new StringBuilder(result.substring(0, result.length() - 1));
        }
        result = new StringBuilder("{" + result + "}");
        return result.toString();
    }

    //A List of the visited files
    List <String> getFileList() {
        // todo
        VirtualFile[] files = EditorHistoryManager.getInstance(currentProject).getFiles();
        for (int i = files.length - 1; i >= 0; --i) {
            VirtualFile file = files[i];
            String path = VcsUtil.getFilePath(file).toString();
            fileList.add(path);
        }
        return fileList;
    }

    HashMap<String, Integer> getVisitedFiles_A() {
        // todo
        for (String s : getFileList()) {
            if (visitedFiles_A.containsKey(s)) {
                visitedFiles_A.put(s, visitedFiles_A.get(s) + 1);
            } else {
                visitedFiles_A.put(s, 1);
            }
        }
        return visitedFiles_A;
    }

  // simply returns current search history result if you want to update searchhistory
    String[] getSearchHistory() {
        // todo
        //finding current file path
        //FileEditorManagerEx fileEditorManager = (FileEditorManagerEx) FileEditorManager.getInstance(currentProject);
        //VirtualFile file = fileEditorManager.getCurrentFile();
        //String currentFilePath = VcsUtil.getFilePath(file).toString();

        //getting current search results
        String[] recent_search_results = FindInProjectSettings.getInstance(currentProject).getRecentFindStrings();

        //putting current file path
        //if (currentFilePath != null) {
        //    searchHistory.put(currentFilePath, recent_search_results);
        //}
        return recent_search_results;
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
            visitedElements.put(path, new ArrayList<>(Arrays.asList(location)));
    }

    MiningRulesProcessor(Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;

        this.visitedFiles = new ArrayList<>();
        this.visitedElements = new HashMap<>();
        this.visitedPaths = new HashMap<>();

        this.fileList = new ArrayList<>();
        this.visitedFiles_A = new HashMap<>();
        this.searchHistory = new HashMap<>();

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
    }

    // todo after completing the implementation add javaDoc. Type: /** just above the method definition and then press enter
    void updateSearchHistory(String newFilePath) {
        // todo
        String prevkey = "";
        if (searchHistory.size() <1) {
            String[] rawhist = searchHistoryRaw;
            List<String> temp = Arrays.asList( rawhist );
            searchHistory.put(newFilePath, temp);
            prevkey = newFilePath;
        }else{
            String[] newsearchhistory = getSearchHistory();
            String[] oldsearchhistory = searchHistoryRaw;

            //compare them by finding difference
            List<String> diff = new ArrayList<>();
            //new search history should be greater than or equal the raw search history as the user searches more
            for(int k = 0; k < newsearchhistory.length-1; k++){
                if( Arrays.asList(oldsearchhistory).contains(newsearchhistory[k]) == false){
                    diff.add(newsearchhistory[k]);
                }
            }
            //assign that difference as the value to the previous key in the actual searchhistory hashmap
            searchHistory.put(prevkey, diff);

            // update the prevkey after you have stored the difference into the old filepath
            prevkey = newFilePath;
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

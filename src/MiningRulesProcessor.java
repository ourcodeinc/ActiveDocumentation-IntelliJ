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
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.event.CaretEvent;
import com.intellij.openapi.editor.event.CaretListener;
import com.intellij.openapi.editor.event.VisibleAreaEvent;
import com.intellij.openapi.editor.event.VisibleAreaListener;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

class MiningRulesProcessor {
    private final ChatServer ws;

    private final Project currentProject;
    private final String projectPath;

    private String currentFilePathForSearch = "";
    private String[] searchHistoryRaw; //= FindInProjectSettings.getInstance(currentProject).getRecentFindStrings(); // the raw history received from FindInProjectSettings.getInstance(project).getRecentFindStrings()
    private List<String> fileList;
    //private List<String[]> visitedFiles; // [filePath, numberOfVisits] todo check
    private HashMap<String, Integer> visitedFiles; // [filePath, numberOfVisits] todo check
    private HashMap <String, List<String>> searchHistory; // [[searchTerms], [filePath1, filePath2]] todo check
    private List<List<String>> caretLocations; // [[filePath], [offSet.toString(), otherUsefulInfo]] todo check

    // list of messages received through web socket and should be processed in this class
    final List<String> wsMessages = Arrays.asList("LEARN_RULES_META_DATA", "LEARN_RULES_FILE_LOCATIONS", "LEARN_RULES_DATABASES",
            "LEARN_RULES_META_DATA_APPEND", "LEARN_RULES_FILE_LOCATIONS_APPEND", "LEARN_RULES_DATABASES_APPEND",
            "EXECUTE_FP_MAX", "DANGEROUS_READ_MINED_RULES", "SEND_DOI_INFORMATION");
    private static MiningRulesProcessor thisClass = null;


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

    HashMap<String, Integer> getVisitedFiles() {
        // todo
        for (String s : getFileList()) {
            if (visitedFiles.containsKey(s)) {
                visitedFiles.put(s, visitedFiles.get(s) + 1);
            } else {
                visitedFiles.put(s, 1);
            }
        }
        return visitedFiles;
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

    String getCaretLocations() {
        // todo
        return "";
    }

    MiningRulesProcessor(Project currentProject, ChatServer ws) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        this.ws = ws;

        this.fileList = new ArrayList<>();
        this.visitedFiles = new HashMap<>();
        this.searchHistory = new HashMap<>();
        this.caretLocations = new ArrayList<>();

        thisClass = this;

        EditorFactory.getInstance()
                .getEventMulticaster()
                .addCaretListener(new CaretListener() {
                    @Override
                    public void caretPositionChanged(@NotNull CaretEvent event) {
                        Caret caret = event.getCaret();
                        int selectionStart = caret.getSelectionStart();
                        int selectionEnd = caret.getSelectionEnd();
                        // selection model returns the same values
                        SelectionModel selectionModel = event.getEditor().getSelectionModel();
                        int modelSelectionStart = selectionModel.getSelectionStart();
                        int modelSelectionEnd = selectionModel.getSelectionEnd();
                        // todo checkout what is needed and pass it to the method
                        updateCaretLocations();
                    }
                }, ApplicationManager.getApplication());

        EditorFactory.getInstance()
                .getEventMulticaster()
                .addVisibleAreaListener(new VisibleAreaListener() {
                    @Override
                    public void visibleAreaChanged(@NotNull VisibleAreaEvent visibleAreaEvent) {
                        // todo what it does?
                    }
                }, ApplicationManager.getApplication());
    }

    static MiningRulesProcessor getInstance() {
        if (thisClass == null) new MiningRulesProcessor(null, null);
        return thisClass;
    }

    /**
     * the path of the newly visited file
     * @param filePath path of the newly opened/visited file
     */
    void newVisitedFile(String filePath) {
        updateVisitedFiles(filePath);
        updateSearchHistory(filePath);
    }

    /**
     * add the newly visited file to the list
     * @param newFilePath path of the newly opened file
     */
    void updateVisitedFiles(String newFilePath) {
        // todo
        //  check if user has already visited the file
        //  update the field accordingly
        if (getVisitedFiles().containsKey(newFilePath)) {
            visitedFiles.put(newFilePath, visitedFiles.get(newFilePath) + 1);
        } else {
            visitedFiles.put(newFilePath, 1);
        }

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

    // todo after completing the implementation add javaDoc. Type: /** just above the method definition and then press enter
    void updateCaretLocations() {
        // todo process the input
        //  the filePath is a required property
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

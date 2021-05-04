import com.intellij.find.FindInProjectSettings;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import java.util.*;

public class DoiProcessing {

    private String currentFilePath = "";
    private String[] searchHistoryRaw = {}; // the raw history

    private final Project currentProject;
    String projectPath;

    private static DoiProcessing thisClass = null;

    // <filePath, [[startOffset1, endOffset1]]]>
    private final Map<String, ArrayList<ArrayList<Integer>>> visitedElements = new HashMap<>();
    // <filePath, numberOfVisits>
    private final Map<String, Integer> visitedFiles = new HashMap<>();
    // <filePath, [[searchTerms], [filePath1, filePath2]]>
    private final HashMap<String, List<String>> searchHistory = new HashMap<>();

    DoiProcessing(Project currentProject) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        thisClass = this;
    }

    static DoiProcessing getInstance() {
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
     *
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

    void findCaretLocations(int startCaretLocation, int endCaretLocation) {
        String path = FileEditorManager.getInstance(currentProject).getSelectedFiles()[0].getCanonicalPath();
        if (path == null || !path.endsWith(".java")) return;
        ArrayList<Integer> location = new ArrayList<>(Arrays.asList(startCaretLocation, endCaretLocation));
        if (visitedElements.containsKey(path))
            visitedElements.get(path).add(location);
        else
            visitedElements.put(path, new ArrayList<>(Collections.singletonList(location)));
    }
}

import com.intellij.find.FindInProjectSettings;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;

import java.util.*;

public class DoiProcessing {

    private String currentFilePath = "";
    private String[] searchHistoryRaw = {}; // the raw history

    private Project currentProject;
    String projectPath;

    private static DoiProcessing thisClass = null;

    // [milliseconds, filePath, startOffset1, endOffset1]
    private final ArrayList<ArrayList<String>> timedVisitedElements = new ArrayList<>();
    // [milliseconds, filePath]
    private final ArrayList<ArrayList<String>> timedVisitedFiles = new ArrayList<>();
    // [milliseconds, filePath, keyword]
    private final ArrayList<ArrayList<String>> timedSearchKeywords = new ArrayList<>();

    DoiProcessing(Project currentProject) {
        this.currentProject = currentProject;
        this.projectPath = currentProject.getBasePath();
        thisClass = this;
    }

    static DoiProcessing getInstance() {
        if (thisClass == null) new MiningRulesProcessor(null, null);
        return thisClass;
    }

    public void updateProject (Project project) {
        this.currentProject = project;
        this.projectPath = currentProject.getBasePath();
    }

    /**
     * convert the data for visitedFiles to String
     *
     * @return String
     */
    public String getVisitedFiles() {
        StringBuilder timeFileString = new StringBuilder();
        for (ArrayList<String> timedVisitedFile : timedVisitedFiles) {
            timeFileString.append("{\"timeStamp\":\"")
                    .append(timedVisitedFile.get(0))
                    .append("\",\"filePath\":\"")
                    .append(timedVisitedFile.get(1))
                    .append("\"},");
        }
        if (timeFileString.length() > 1) {
            timeFileString = new StringBuilder(timeFileString.substring(0, timeFileString.length() - 1));
        }
        timeFileString = new StringBuilder("[" + timeFileString + "]");
        return timeFileString.toString();
    }

    /**
     * convert the data for searchHistory to String
     *
     * @return String
     */
    public String getSearchHistory() {
        StringBuilder timeFileKeywordString = new StringBuilder();
        for (ArrayList<String> timedFileKeywords : timedSearchKeywords) {
            timeFileKeywordString.append("{\"timeStamp\":\"")
                    .append(timedFileKeywords.get(0))
                    .append("\",\"filePath\":\"")
                    .append(timedFileKeywords.get(1))
                    .append("\",\"keyword\":\"")
                    .append(timedFileKeywords.get(2))
                    .append("\"},");
        }
        if (timeFileKeywordString.length() > 1) {
            timeFileKeywordString = new StringBuilder(timeFileKeywordString.substring(0, timeFileKeywordString.length() - 1));
        }
        timeFileKeywordString = new StringBuilder("[" + timeFileKeywordString + "]");
        return timeFileKeywordString.toString();
    }

    /**
     * convert the data for visitedElement to String
     *
     * @return String
     */
    public String getVisitedElements() {
        StringBuilder timeFileOffsetsString = new StringBuilder();
        for (ArrayList<String> timedFileKeywords : timedVisitedElements) {
            timeFileOffsetsString.append("{\"timeStamp\":\"")
                    .append(timedFileKeywords.get(0))
                    .append("\",\"filePath\":\"")
                    .append(timedFileKeywords.get(1))
                    .append("\",\"startOffset\":\"")
                    .append(timedFileKeywords.get(2))
                    .append("\",\"endOffset\":\"")
                    .append(timedFileKeywords.get(3))
                    .append("\"},");
        }
        if (timeFileOffsetsString.length() > 1) {
            timeFileOffsetsString = new StringBuilder(timeFileOffsetsString.substring(0, timeFileOffsetsString.length() - 1));
        }
        timeFileOffsetsString = new StringBuilder("[" + timeFileOffsetsString + "]");
        return timeFileOffsetsString.toString();
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
        long time = new Date().getTime();
        String timeStamp = Long.toString(time);
        if (timedVisitedFiles.size() > 1) {
            try {
                ArrayList<String> previousVisitedElement = timedVisitedFiles.get(timedVisitedFiles.size() - 1);
                long previousTimeStamp = Long.parseLong(previousVisitedElement.get(0));
                if (time - previousTimeStamp < 1000) {
                    timedVisitedFiles.remove(timedVisitedFiles.size() - 1);
                }
            } catch (Exception e) {
                System.out.println("Error happened in getting and removing the previous file visited for less than a second");
            }
        }
        ArrayList<String> timeFilePair = new ArrayList<>();
        timeFilePair.add(timeStamp);
        timeFilePair.add(newFilePath);
        timedVisitedFiles.add(timeFilePair);
    }


    /**
     * update the search history upon changing the active file in the editor
     */
    void updateSearchHistory() {
        String[] recentFindStringsRaw = FindInProjectSettings.getInstance(currentProject)
                .getRecentFindStrings();
        String[] recentSearchHistoryRaw = Arrays.stream(recentFindStringsRaw)
                .map(s -> s.replaceAll("\"", ""))
                .toArray(String[]::new);
        long time = new Date().getTime();
        String timeStamp = Long.toString(time);
        if (currentFilePath.equals("")) {
            for (String keyword: recentSearchHistoryRaw) {
                ArrayList<String> timeFileKeywordTuple = new ArrayList<>();
                timeFileKeywordTuple.add(timeStamp);
                timeFileKeywordTuple.add("none");
                timeFileKeywordTuple.add(keyword);
                timedSearchKeywords.add(timeFileKeywordTuple);
            }
            searchHistoryRaw = recentSearchHistoryRaw;
        } else {
            // there are new items in Search History
            if (recentSearchHistoryRaw.length > searchHistoryRaw.length) {
                // find the diff of the Search history
                String[] diff = Arrays.copyOfRange(recentSearchHistoryRaw,
                        searchHistoryRaw.length, recentSearchHistoryRaw.length);

                for (String keyword: diff) {
                    ArrayList<String> timeFileKeywordTuple = new ArrayList<>();
                    timeFileKeywordTuple.add(timeStamp);
                    timeFileKeywordTuple.add(currentFilePath);
                    timeFileKeywordTuple.add(keyword);
                    timedSearchKeywords.add(timeFileKeywordTuple);
                }
                searchHistoryRaw = recentSearchHistoryRaw;
            }
        }
    }

    void updateVisitedElements(int startCaretLocation, int endCaretLocation) {
        String path = FileEditorManager.getInstance(currentProject).getSelectedFiles()[0].getCanonicalPath();
        long time = new Date().getTime();
        String timeStamp = Long.toString(time);
        if (timedVisitedElements.size() > 1) {
            try {
                ArrayList<String> previousVisitedElement = timedVisitedElements.get(timedVisitedElements.size() - 1);
                long previousTimeStamp = Long.parseLong(previousVisitedElement.get(0));
                if (time - previousTimeStamp < 1000) {
                    timedVisitedElements.remove(timedVisitedElements.size() - 1);
                }
            } catch (Exception e) {
                System.out.println("Error happened in getting and removing the previous element visited for less than a second");
            }
        }
        ArrayList<String> timeFileOffsetsTuple = new ArrayList<>();
        timeFileOffsetsTuple.add(timeStamp);
        timeFileOffsetsTuple.add(path);
        timeFileOffsetsTuple.add(Integer.toString(startCaretLocation));
        timeFileOffsetsTuple.add(Integer.toString(endCaretLocation));
        timedVisitedElements.add(timeFileOffsetsTuple);
    }
}

package core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by saharmehrpour on 6/28/17.
 */
public class SRCMLxml {

    List<String> paths;
    private String projectPath;
    List<String> xmls;
    //public String xml;
    public int fileNumber;

    public SRCMLxml(List<String> paths, String projectPath) {
        this.paths = paths;
        this.projectPath = projectPath;
        this.fileNumber = this.paths.size();
        this.xmls = new ArrayList<>();

    }

    public List<String> getPaths() {
        return paths;
    }

    public List<String> getXmls() {
        return xmls;
    }

    public String getProjectPath() {
        return projectPath;
    }

}

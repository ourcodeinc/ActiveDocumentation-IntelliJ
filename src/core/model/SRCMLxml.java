package core.model;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
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

        //this.xml = "";
    }

//    String attachXmls() {
//        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
//                "<unit xmlns=\"http://www.srcML.org/srcML/src\" revision=\"0.9.5\">\n" +
//                "\n";
//
//        for (String x : this.xmls) {
//            xml = xml + x.substring(x.indexOf('\n') + 1) + "\n";
//        }
//
//        xml = xml + "</unit>\n";
//
//        // write the xml in the file
//        // not necessary
//        try {
//            PrintWriter writer = new PrintWriter(projectPath + "/source_xml.xml", "UTF-8");
//            writer.println(xml);
//            writer.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//
//        this.xml = xml;
//        return xml;
//    }

//    public void addFilePath(String newFilePath) {
//        fileNumber += 1;
//        paths.add(newFilePath);
//        xmls.add("");
//
//    }

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

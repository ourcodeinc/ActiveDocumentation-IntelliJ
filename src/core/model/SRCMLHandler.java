/*
 * Created by saharmehrpour on 6/6/17.
 */

package core.model;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class SRCMLHandler {

    final private static String srcmlPathMac = "/usr/local/bin/srcml";
    final private static String srcmlPathWindows = "C:\\Program Files\\srcML 0.9.5\\bin\\srcml";

    /**
     * Create a srcML xml file initially
     *
     * @param srcml a partially defined SRCMLxml object
     */
    public static void createXMLForProject(SRCMLxml srcml) {

//        // We can skip writing the file
//        // We don't need it here. The file is only written as a reference
//        String[] str = new String[]{"srcml", "--verbose", srcml.projectPath + "/src", "-o", srcml.projectPath + "/source_xml.xml"};
//        runShellCommand(str); // to save the xml

//        for (int index = 0; index < Math.min(100, srcml.fileNumber); index++) {
        for (int index = 0; index < srcml.fileNumber; index++) {
            srcml.xmls.add(createXMLForFile(srcml.paths.get(index)));
        }

    }


    /**
     * update the xml for a file in project
     *
     * @param srcml    object
     * @param filePath of the modified file
     * @return String new xml
     */
    public static String updateXMLForProject(SRCMLxml srcml, String filePath) {

        for (int index = 0; index < srcml.fileNumber; index++) {
            if (srcml.paths.get(index).equals(filePath)) {
                srcml.xmls.set(index, createXMLForFile(srcml.paths.get(index)));
                return srcml.xmls.get(index);
            }
        }
        return "";
    }


    /**
     * remove an xml file
     */
    public static void removeXMLForProject(SRCMLxml srcml, String filePath) {
        for (int index = 0; index < srcml.fileNumber; index++) {
            if (srcml.paths.get(index).equals(filePath)) {
                srcml.paths.remove(index);
                srcml.xmls.remove(index);
                srcml.fileNumber -= 1;
//                srcml.attachXmls();
                break;
            }
        }
//        return srcml;
    }

    /**
     * add an xml file
     */
    public static String addXMLForProject(SRCMLxml srcml, String filePath) {
        srcml.paths.add(filePath);
        String newXml = createXMLForFile(filePath);
        srcml.xmls.add(newXml);
        srcml.fileNumber += 1;
        return newXml;
    }


    /**
     * create an xml for a specific file
     *
     * @param path of the file
     * @return xml String
     */
    private static String createXMLForFile(String path) {

        if (System.getProperty("os.name").startsWith("Mac"))
            return runShellCommand(new String[]{srcmlPathMac, path});
        else if(System.getProperty("os.name").startsWith("Windows"))
            return runShellCommand(new String[]{srcmlPathWindows, path});
        System.out.println(System.getProperty("os.name") + " not supported");
        return "";
    }


    /**
     * run the shell command
     *
     * @param command (terminal command)
     * @return output
     */
    private static String runShellCommand(String[] command) {
        GeneralCommandLine generalCommandLine = new GeneralCommandLine(command);
        generalCommandLine.setCharset(StandardCharsets.UTF_8);

        try {
            ProcessOutput processOutput = ExecUtil.execAndGetOutput(generalCommandLine);
            return String.join("\n", processOutput.getStdoutLines());

        } catch (ExecutionException e) {
            e.printStackTrace();
            return e.toString();
        }

    }


    /**
     * find the length of a java class from its xml file
     *
     * @param fullPath of the xml file
     * @return number of characters
     */
    public static int findLineNumber(String fullPath) {

        String src = "";
        if (System.getProperty("os.name").startsWith("Mac"))
            src = runShellCommand(new String[]{srcmlPathMac, "--unit", "1", fullPath});
        else if (System.getProperty("os.name").startsWith("Windows"))
            src = runShellCommand(new String[]{srcmlPathWindows, "--unit", "1", fullPath});
        else
            System.out.println(System.getProperty("os.name") + " not supported");
        //File filePath = new File(fullPath);
        //filePath.delete();

        return src.length();
    }


    /**
     * create an xml string for a given piece of code
     *
     * @param exprText a piece of code
     * @param filePath the path of the temp file
     * @return xml string
     */
    public static String createXMLForText(String exprText, String filePath) {

        // create a file and write the string into that
        try {
            PrintWriter writer = new PrintWriter(filePath, "UTF-8");
            writer.println(exprText);
            writer.close();
        } catch (IOException e) {
            System.out.println("error in writing the result xml");
            return "";
        }

        // create the srcml xml string and return the string
        return createXMLForFile(filePath);
    }
}

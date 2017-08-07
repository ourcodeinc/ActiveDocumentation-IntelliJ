/**
 * Created by saharmehrpour on 6/6/17.
 */

package core.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SRCMLHandler {

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

        for (int index = 0; index < Math.min(100, srcml.fileNumber); index++) {
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
     *
     * @param srcml
     * @param filePath
     */
    public static void/*SRCMLxml*/ removeXMLForProject(SRCMLxml srcml, String filePath) {
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
     *
     * @param srcml
     * @param filePath
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
        return runShellCommand(new String[]{"srcml", path})[0];
    }


    /**
     * run the shell command
     *
     * @param command (terminal command)
     * @return output and errors
     */
    private static String[] runShellCommand(String[] command) {
        try {
            Process p = Runtime.getRuntime().exec(command);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

            String line, resultInput = "", resultError = "";
            while ((line = stdInput.readLine()) != null) {
                resultInput = resultInput + line + '\n';
            }

            while ((line = stdError.readLine()) != null) {
                resultError = resultError + line + '\n';
            }
            return new String[]{resultInput, resultError};

        } catch (IOException e) {
            System.out.println("Exception: ");
            e.printStackTrace();
            return new String[]{"", ""};
        }

    }


    /**
     * find the length of a java class from its xml file
     *
     * @param fullPath of the xml file
     * @return number of characters
     */
    public static int findLineNumber(String fullPath) {

        String[] str = new String[]{"srcml", "--unit", "1", fullPath};
        String[] src = runShellCommand(str);

        //File filePath = new File(fullPath);
        //filePath.delete();

        return src[0].length();
    }


}

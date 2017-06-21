package core.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by saharmehrpour on 6/6/17.
 */

public class SRCMLHandler {

    public static String createXMLFile(String path) {

        // We can skip writing the file
        // We don't need it here. The file is only written as a reference
        String[] str = new String[]{"srcml", "--verbose", path + "/src", "-o", path + "/source_xml.xml"};
        runShellCommand(str); // to save the xml

        return runShellCommand(new String[]{"srcml", "--verbose", path + "/src"})[0];

    }

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

    public static int findLineNumber(String fullPath) {

        String[] str = new String[]{"srcml", "--unit", "1", fullPath};
        String[] src = runShellCommand(str);

        //File filePath = new File(fullPath);
        //filePath.delete();

        return src[0].length();
    }


}

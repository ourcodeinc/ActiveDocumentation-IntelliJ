package core.model;

import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LearnDesignRules {
    public static JsonObject analyzeDatabases(String projectPath, String utility, String directory, String alg) {

        String path = projectPath.concat(directory);
        // create the directory if not exist, but never happens.
        // because we are reading data files from the same folder
        File outputDirectory = new File(path);
        if (!outputDirectory.exists()){
            boolean result = outputDirectory.mkdir();
            if (!result)
                return null;
        }

        List<String> fileList = new ArrayList<>();
        File[] listOfFiles = outputDirectory.listFiles();
        if (listOfFiles != null) {
            for (File lof : listOfFiles) {
                if (lof.isFile() && lof.getName().startsWith("AttributeEncoding")) {
                    fileList.add(lof.getName());
                }
            }
        }
        String selectedAlg = alg;
        // alg = ""CHUI-MinerMax"" or ""CHUI-Miner"
        if (!alg.equals("CHUI-MinerMax") && !alg.equals("CHUI-Miner"))
            selectedAlg = "CHUI-Miner";
        for (String s : fileList) {
            String[] command = new String[]{"java", "-jar",
                    projectPath + "/spmf.jar", "run", selectedAlg,
                    path + "/" + s,
                    path + "/output_" + s,
                    utility};
            GeneralCommandLine generalCommandLine = new GeneralCommandLine(command);
            generalCommandLine.setCharset(StandardCharsets.UTF_8);
            try {
                ExecUtil.execAndGetOutput(generalCommandLine);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        JsonObject jsonObject = new JsonObject();
        for (String s : fileList) {
            StringBuilder CHUI_Output = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(path + "/output_" + s));
                while (br.ready()) {
                    CHUI_Output.append(br.readLine()).append("\n");
                }
                jsonObject.addProperty(s, CHUI_Output.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }
}

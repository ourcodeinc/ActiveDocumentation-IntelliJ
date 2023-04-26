package core.model;

import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.ExecUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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
        // alg = "CHUI-MinerMax" or "CHUI-Miner" or "FPMax"
        if (!alg.equals("CHUI-MinerMax") && !alg.equals("CHUI-Miner") && !alg.equals("FPMax"))
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
            StringBuilder output = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(path + "/output_" + s));
                while (br.ready()) {
                    output.append(br.readLine()).append("\n");
                }
                jsonObject.addProperty(s, output.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonObject;
    }
}

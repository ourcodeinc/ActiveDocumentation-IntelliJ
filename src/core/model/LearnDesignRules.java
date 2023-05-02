package core.model;

import com.google.gson.JsonArray;
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
    public static JsonObject analyzeDatabases(String projectPath, JsonArray params, String directory, String algorithm) {

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
        StringBuilder paramString = new StringBuilder();
        for (int i=0; i < params.size(); i++) {
            paramString.append(params.get(i)).append(" ");
        }
        for (String s : fileList) {
            String[] command = new String[]{"java", "-jar",
                    projectPath + "/spmf.jar", "run", algorithm,
                    path + "/" + s,
                    path + "/output_" + s,
                    paramString.toString()};
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

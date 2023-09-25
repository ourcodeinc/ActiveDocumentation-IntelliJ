package core.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LearnDesignRules {

    final static Map<String, String> filePrefix = new HashMap<String, String>() {{
        put("FPMax", "AttributeEncoding");
        put("FPClose", "AttributeEncoding");
        put("CHUI-Miner", "Weighted_AttributeEncoding");
        put("CHUI-MinerMax", "Weighted_AttributeEncoding");
    }};
    public static int timeoutInSeconds = 5;

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
        List<File> outputFileList = new ArrayList<>();
        File[] listOfFiles = outputDirectory.listFiles();
        if (listOfFiles != null) {
            for (File lof : listOfFiles) {
                if (lof.isFile() && lof.getName().startsWith(filePrefix.get(algorithm))) {
                    fileList.add(lof.getName());
                }
                else if (lof.isFile() && lof.getName().startsWith("output_")) {
                    outputFileList.add(lof);
                }
            }
        }
        // delete output files to create new ones
        for (File file : outputFileList) {
           file.delete();
        }

        List<String> paramString = new ArrayList<>();
        for (int i=0; i < params.size(); i++) {
            paramString.add(params.get(i).toString());
        }
        for (String s : fileList) {
            String[] command = new String[]{"java", "-jar",
                    projectPath + "/spmf.jar", "run", algorithm,
                    path + "/" + s,
                    path + "/output_" + s,
                    String.join(" ", paramString)};
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process;
            try {
                process = processBuilder.start();
                // Create a thread to monitor and terminate the process if it runs too long
                Thread monitorThread = new Thread(() -> {
                    try {
                        if (!process.waitFor(timeoutInSeconds, TimeUnit.SECONDS)) {
                            // Process exceeded the timeout
                            process.destroy();
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted");
                        e.printStackTrace();
                    }
                });
                monitorThread.start();
                int exitCode = process.waitFor();
                monitorThread.interrupt(); // Stop the monitor thread
            } catch (IOException | InterruptedException e) {
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

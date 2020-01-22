package core.model;

import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MiningRulesUtilities {

    /**
     * read the mined rules data from file
     * @param projectPath path of the project
     * @return JsonObject with 2 properties: metaData and output (also a JsonObject transformed to a String)
     */
    public static JsonObject readMinedRulesFile(String projectPath) {
        String path = projectPath.concat("/LearningDR");
        File directory = new File(path);
        if (! directory.exists()){
            return null;
        }

        File[] outputFile = directory.listFiles((dir, name) -> name.startsWith("output"));
        File[] metaDataFile = directory.listFiles((dir, name) -> name.startsWith("attribute_META_data"));

        if (outputFile == null || outputFile.length == 0) return null;
        if (metaDataFile == null || metaDataFile.length != 1) return null;

        JsonObject jsonOutputObject = new JsonObject();
        JsonObject jsonObject = new JsonObject();

        // add output files
        for (int i = 0; i < outputFile.length; i++) {
            StringBuilder minedRulesOutput = new StringBuilder();
            try {
                BufferedReader br = new BufferedReader(new FileReader(outputFile[i].getPath()));
                while (br.ready()) {
                    minedRulesOutput.append(br.readLine()).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            jsonObject.addProperty(Integer.toString(i), minedRulesOutput.toString());
        }
        jsonOutputObject.addProperty("output", jsonObject.toString());

        // add meta data
        StringBuilder metaDataOutput = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(metaDataFile[0].getPath()));
            while (br.ready()) {
                metaDataOutput.append(br.readLine()).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        jsonOutputObject.addProperty("metaData", metaDataOutput.toString());


        return jsonOutputObject;
    }

}

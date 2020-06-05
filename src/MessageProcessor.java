import com.google.gson.JsonObject;

class MessageProcessor {

    private static final String[] dataKeys = {"source", "destination", "command", "data"};
    private static final String[] xmlKeys = {"filePath", "xml"};
    private static final String[] textXML = {"xmlText", "messageID"};

    private static final String[] fpMaxOutputKeys = {"fpMaxOutput"};
    private static final String[] tnrOutputKeys = {"tnrOutputKeys"};
    private static final String[] selectedFragmentKeys = {"path", "startOffset", "endOffset", "startLineOffset", "lineNumber", "lineText", "text"};
    private static final String[] dangerousReadMinedRules = {"outputFiles", "metaData"};

    static JsonObject encodeData(Object[] source_Destination_Protocol_Data_Array) {
        return createJsonObject(source_Destination_Protocol_Data_Array, dataKeys);
    }

    static JsonObject encodeXMLData(Object[] filepath_xml_Array) {
        return createJsonObject(filepath_xml_Array, xmlKeys);
    }

    static JsonObject encodeNewXMLData(Object[] filepath_newXml) {
        return createJsonObject(filepath_newXml, xmlKeys);
    }

    static JsonObject encodeXMLandText(Object[] xml_text) {
        return createJsonObject(xml_text, textXML);
    }

    /*  mining rules*/

    static JsonObject encodeFPMaxOutput(Object[] fpMax_output) {
        return createJsonObject(fpMax_output, fpMaxOutputKeys);
    }

    static JsonObject encodeTNROutput(Object[] tnr_output) {
        return createJsonObject(tnr_output, tnrOutputKeys);
    }

    static JsonObject encodeSelectedFragment(Object[] selected_frag_data) {
        return createJsonObject(selected_frag_data, selectedFragmentKeys);
    }

    static JsonObject encodeDangerousMinedData(Object[] minedData) {
        return createJsonObject(minedData, dangerousReadMinedRules);
    }

    private static JsonObject createJsonObject(Object[] data_Array, String[] keys) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < keys.length; i++) {
            if (data_Array[i] instanceof String) {
                jsonObject.addProperty(keys[i], (String) data_Array[i]);
            } else if (data_Array[i] instanceof JsonObject) {
                jsonObject.add(keys[i], (JsonObject) data_Array[i]);
            }
        }
        return jsonObject;
    }

}

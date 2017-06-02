import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.icons.AllIcons;

// A quick class to format some data as a JSONObject message that the web-client can process
public class MessageProcessor {

    private static final String[] dataKeys = {"source", "destination", "command", "data"};

    // redundant
    public static String[] getDataKeys() {
        return dataKeys;
    }

    public static JsonObject encodeData(Object[] source_Destination_Protocol_Data_Array) {
        JsonObject jsonObject = new JsonObject();
        for (int i = 0; i < dataKeys.length; i++) {
            if (source_Destination_Protocol_Data_Array[i] instanceof String) {
                jsonObject.addProperty(dataKeys[i], (String) source_Destination_Protocol_Data_Array[i]);
            } else if (source_Destination_Protocol_Data_Array[i] instanceof JsonObject) {
                jsonObject.add(dataKeys[i], (JsonObject) source_Destination_Protocol_Data_Array[i]);
            }
        }
        return jsonObject;
    }

}

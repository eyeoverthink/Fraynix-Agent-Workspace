package fraymus.workspace;
import org.junit.Test;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class JsonStream058Test {
    @Test public void streamingPreservesEscapesUnicodeNumbersAndOriginalReportHashes() throws Exception {
        var value = new JSONObject().put("quote\"", "newline\nslash\\ control\u0001 snow ☃ and emoji 🚀").put("nested", new JSONArray().put(JSONObject.NULL).put(true).put(-1.25e10));
        value.put("large", "\"\\🚀\n".repeat(200000));
        String original = value.toString(); var bytes = new ByteArrayOutputStream(); JsonStream058.write(value, bytes);
        assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), bytes.toByteArray());
        assertEquals(original.length(), JsonStream058.characters(value));
        assertEquals(MeasurementStore.hash(original), JsonStream058.sha256(value));
        value.put("changed", "one different byte"); assertNotEquals(MeasurementStore.hash(original), JsonStream058.sha256(value));
    }
}

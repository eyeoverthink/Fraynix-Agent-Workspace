package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

import static org.junit.Assert.*;

public class RequestWorkbenchTest {
    private static byte[] text(String value) { return value.getBytes(StandardCharsets.UTF_8); }

    @Test public void preservesHeaderOrderAndTextThroughBothDirections() throws Exception {
        String original = "\uFEFFCode,Name,Notes\r\n00017,南,\"line 1\nline \"\"2\"\"\"\r\n0,Café,\r\n";
        var json = FileRepresentation.convert(RequestLanguage.Intent.TO_JSON, text(original));
        var csv = FileRepresentation.convert(RequestLanguage.Intent.TO_CSV, text(json.data()));
        assertEquals(MeasurementStore.csv(original, 0), MeasurementStore.csv(csv.data(), 0));
        var table = new JSONObject(json.data());
        assertEquals("00017", table.getJSONArray("rows").getJSONArray(0).getString(0));
        assertEquals("", table.getJSONArray("rows").getJSONArray(1).getString(2));
        assertThrows(IllegalArgumentException.class, () -> FileRepresentation.convert(RequestLanguage.Intent.TO_CSV, text("[{\"amount\":9007199254740993}]")));
        assertThrows(IllegalArgumentException.class, () -> FileRepresentation.convert(RequestLanguage.Intent.TO_CSV, text("[{'key':'value'}]")));
        assertThrows(IllegalArgumentException.class, () -> FileRepresentation.convert(RequestLanguage.Intent.TO_CSV, text("[{\"a\":\"x\",\"a\":\"y\"}]")));
        assertThrows(IllegalArgumentException.class, () -> FileRepresentation.convert(RequestLanguage.Intent.TO_JSON, text("a,b\n1\n")));
    }

    @Test public void reportsObservedBytesWithoutAssumingInternalFileStructure() throws Exception {
        byte[] bytes = java.util.HexFormat.of().parseHex("89504e470d0a1a0a0001ff00");
        var result = new JSONObject(FileRepresentation.convert(RequestLanguage.Intent.INSPECT, bytes).data());
        assertEquals("PNG", result.getString("signatureHint"));
        assertEquals(PerceptionStore.hash(bytes), result.getString("sha256"));
        assertEquals("10001001", result.getJSONArray("preview").getJSONObject(0).getString("bits"));
        assertEquals(255, result.getJSONArray("preview").getJSONObject(10).getInt("decimal"));
    }

    @Test public void requiresCurrentReviewAndRetainsAUserTaughtPhraseAcrossRestart() throws Exception {
        var project = Files.createTempDirectory("fraynix-requests");
        String id;
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            var undecided = wb.suggest(new JSONObject().put("request", "Make the datasauce portable"), in -> { throw new AssertionError(); });
            id = undecided.getString("id");
            var ref = new JSONObject().put("id", id);
            assertEquals("clarify", undecided.getJSONObject("choice").getString("action"));
            assertThrows(Exception.class, () -> wb.learn(ref));
            wb.choose(new JSONObject().put("id", id).put("action", "csv-to-json"));
            var attached = wb.attach(new JSONObject().put("id", id).put("name", "table.csv")
                    .put("base64", Base64.getEncoder().encodeToString(text("item\n0001\n"))));
            assertThrows(IllegalStateException.class, () -> wb.execute(new JSONObject().put("id", id)
                    .put("reviewSha256", undecided.getString("reviewSha256"))));
            var confirm = new JSONObject().put("id", id).put("reviewSha256", attached.getString("reviewSha256"));
            assertTrue(wb.execute(confirm).getBoolean("passed"));
            assertTrue(wb.learn(ref).getBoolean("remembered"));
        }
        try (var store = new WorkspaceStore(project)) {
            var wb = new RequestWorkbench(project, store);
            assertTrue(wb.verify(new JSONObject().put("id", id)).getBoolean("passed"));
            var suggestion = wb.suggest(new JSONObject().put("request", "Make the datasauce portable!"), in -> { throw new AssertionError(); });
            assertEquals("remembered", suggestion.getJSONObject("choice").getString("by"));
            assertEquals("review", suggestion.getString("status"));
            assertFalse(suggestion.has("source"));
        }
    }
}

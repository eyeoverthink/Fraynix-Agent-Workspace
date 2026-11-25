
package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.Assert.*;

public class PlacementExchangeTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void rejectsRehashedFalseTrajectoryAfterImport() throws Exception {
        var bundle = new JSONObject(Files.readString(frozen()));
        var evidence = bundle.getJSONObject("evidence");
        var trial = evidence.getJSONObject("measured");
        var value = trial.getJSONObject("hit").getJSONArray("samples").getJSONObject(2).getJSONArray("propM");
        value.put(0,value.getDouble(0)+.25);
        evidence.put("repeat",WorldFrames.copy(trial));
        bundle.put("evidenceSha256",WorldFrames.hash(evidence));
        var project = temp.newFolder().toPath();
        var exchange = new PlacementExchange(project);
        var archived = exchange.open(request(bundle));
        assertFalse(archived.has("replay"));
        String id = archived.getString("id");
        var failure = assertThrows(IllegalStateException.class,
                () -> exchange.replay(new JSONObject().put("id",id)));
        assertTrue(failure.getMessage().contains("hit replay differs"));
        assertFalse(new PlacementExchange(project).latest(new JSONObject()).has("replay"));
    }

    @Test
    public void refusesMissingSamplesAndGeometryDespiteMatchingContainerHashes() throws Exception {
        var original = new JSONObject(Files.readString(frozen()));
        var bundle = WorldFrames.copy(original);
        var evidence = bundle.getJSONObject("evidence");
        var result = evidence.getJSONObject("measured");
        result.getJSONObject("miss").getJSONArray("samples").remove(1);
        evidence.put("repeat",WorldFrames.copy(result));
        bundle.put("evidenceSha256",WorldFrames.hash(evidence));
        assertThrows(IllegalArgumentException.class, () -> PlacementExchange.inspect(encoded(bundle)));

        var wrongGeometry = original.getJSONObject("scene").getJSONArray("objects").getJSONObject(1);
        wrongGeometry.put("massKg",2);
        assertThrows(IllegalStateException.class, () -> PlacementExchange.inspect(encoded(original)));
    }

    private static Path frozen() {
        return Path.of("..","workspace-desktop","test","fixtures","placement-exchange","edition232.fraynix-placement.json");
    }

    private static byte[] encoded(JSONObject input) {
        return input.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static JSONObject request(JSONObject data) {
        return new JSONObject().put("base64",Base64.getEncoder().encodeToString(encoded(data)));
    }
}

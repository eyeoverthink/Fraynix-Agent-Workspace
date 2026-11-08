package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class RegressionLock096Test {
    static Path repo() {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (!Files.exists(root.resolve("docs/WORLD_096_REGRESSION_LOCK.json"))) root = root.getParent();
        return root;
    }
    @Test public void actualReviewedSourceChainAndAllPreservedInputsPass() throws Exception { WorldRegression096.verify(repo()); }
    @Test public void alteredOriginalContractsSuiteAndSourceAreRefused() throws Exception {
        for (String changed : List.of("docs/WORLD_035_CANDIDATE_LOCK.json", "docs/WORLD_035_FROZEN_CONTRACT.json",
            "docs/WORLD_056_REGRESSION_LOCK.json", "workspace-engine/src/main/resources/world-sequences035.json",
            "workspace-engine/src/main/java/fraymus/workspace/WorldLedger.java",
            "workspace-engine/src/main/java/fraymus/workspace/WorldNativeCheckpoint.java",
            "workspace-engine/src/main/java/fraymus/workspace/WorldPhysics.java")) {
            assertThrows(changed, AssertionError.class, () -> WorldRegression096.verify(p ->
                Files.readString(repo().resolve(p)) + (p.equals(changed) ? "\nchanged" : "")));
        }
    }
    @Test public void missingDuplicateForeignAndBrokenExtensionsAreRefused() throws Exception {
        String path = "docs/WORLD_096_REGRESSION_LOCK.json";
        for (String mutation : List.of("missing", "duplicate", "foreign", "chain", "input")) {
            var lock = new JSONObject(Files.readString(repo().resolve(path)));
            var entries = lock.getJSONArray("extensions");
            switch (mutation) {
                case "missing" -> entries.remove(0);
                case "duplicate" -> entries.put(new JSONObject(entries.getJSONObject(0).toString()));
                case "foreign" -> entries.getJSONObject(0).put("path", "foreign.java");
                case "chain" -> entries.getJSONObject(0).put("previous056Sha256", "0".repeat(64));
                case "input" -> lock.getJSONArray("lockedInputs").remove(0);
            }
            assertThrows(mutation, AssertionError.class, () -> WorldRegression096.verify(p ->
                p.equals(path) ? lock.toString() : Files.readString(repo().resolve(p))));
        }
    }
}

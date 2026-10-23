package fraymus.factory.models;

import org.json.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Project-specific preferences keyed by endpoint and exact tag; model weights stay in Ollama. */
public final class ModelProfileStore {
    private final Path path;
    private volatile Map<String,ModelRunSettings> profiles = new HashMap<>();
    public ModelProfileStore(Path workspace) throws IOException {
        path = workspace.resolve("models/runtime-profiles.json");
        if (!Files.exists(path)) return;
        try {
            var rows = new JSONObject(Files.readString(path)).getJSONArray("profiles");
            for (int i=0;i<rows.length();i++) {
                var row=rows.getJSONObject(i);
                profiles.put(row.getString("endpoint")+"\n"+row.getString("model"), read(row));
            }
        } catch (RuntimeException failure) { throw new IOException("Model profiles are invalid; original file retained: " + path, failure); }
    }
    public Path path() { return path; }
    public ModelRunSettings get(String endpoint,String model,long bytes) {
        return profiles.getOrDefault(endpoint+"\n"+model,ModelRunSettings.defaults(bytes));
    }
    public synchronized void save(String endpoint,String model,ModelRunSettings settings) throws IOException {
        var next = new TreeMap<>(profiles); next.put(endpoint+"\n"+model,settings);
        var rows=new JSONArray(); next.forEach((key,value)->{
            int split=key.indexOf('\n'); rows.put(json(value).put("endpoint",key.substring(0,split)).put("model",key.substring(split+1)));
        });
        Files.createDirectories(path.getParent()); Path temporary=Files.createTempFile(path.getParent(),"profiles-",".tmp");
        try {
            Files.writeString(temporary,new JSONObject().put("version",1).put("profiles",rows).toString(2),StandardCharsets.UTF_8);
            try { Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING); }
            profiles=Map.copyOf(next);
        } finally { Files.deleteIfExists(temporary); }
    }
    private static JSONObject json(ModelRunSettings s) {
        return new JSONObject().put("context",s.context()).put("outputTokens",s.outputTokens()).put("threads",s.threads())
            .put("gpuLayers",s.gpuLayers()).put("idleSeconds",s.idleSeconds()).put("keepAliveSeconds",s.keepAliveSeconds()).put("temperature",s.temperature())
            .put("repeatPenalty",s.repeatPenalty()).put("clearCustomStops",s.clearCustomStops());
    }
    private static ModelRunSettings read(JSONObject r) {
        return new ModelRunSettings(r.getInt("context"),r.getInt("outputTokens"),r.getInt("threads"),r.getInt("gpuLayers"),
            r.getInt("idleSeconds"),r.getInt("keepAliveSeconds"),r.getDouble("temperature"),r.optDouble("repeatPenalty",1.1),r.optBoolean("clearCustomStops",false));
    }
}

package fraymus.factory.models;

import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Append-only completed exchanges, separate from scene entities and kernel evidence. */
public final class ConversationStore {
    public record Exchange(String model, String prompt, String reply, String endpoint, boolean success, long millis, Instant at,
                           String sceneContext, String thinking, Map<String,String> evidence) {
        public Exchange { evidence=Map.copyOf(evidence); }
        public Exchange(String model,String prompt,String reply,String endpoint,boolean success,long millis,Instant at) {
            this(model,prompt,reply,endpoint,success,millis,at,"","",Map.of());
        }
    }
    private final Path path;
    public ConversationStore(Path workspace) { path = workspace.resolve("chat/exchanges.jsonl"); }
    public Path path() { return path; }
    public synchronized void append(Exchange exchange) throws IOException {
        Files.createDirectories(path.getParent());
        JSONObject row = new JSONObject().put("model", exchange.model()).put("prompt", exchange.prompt()).put("reply", exchange.reply())
            .put("endpoint", exchange.endpoint()).put("success", exchange.success()).put("millis", exchange.millis()).put("at", exchange.at().toString())
            .put("version",2).put("sceneContext",exchange.sceneContext()).put("thinking",exchange.thinking()).put("evidence",exchange.evidence());
        Files.writeString(path, row + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    public synchronized List<Exchange> load() throws IOException {
        if (!Files.isRegularFile(path)) return List.of();
        var exchanges = new ArrayList<Exchange>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(path)) {
            lineNumber++;
            if (line.isBlank()) continue;
            try {
                var row = new JSONObject(line);
                String original=row.getString("prompt");
                var evidence=new TreeMap<String,String>(); var fields=row.optJSONObject("evidence");
                if(fields!=null)for(String key:fields.keySet())evidence.put(key,fields.getString(key));
                exchanges.add(new Exchange(row.getString("model"), row.optInt("version")>=2?original:ChatContext.humanPrompt(original), row.getString("reply"),
                    row.getString("endpoint"), row.getBoolean("success"), row.getLong("millis"), Instant.parse(row.getString("at")),
                    row.optString("sceneContext",ChatContext.legacyScene(original)),row.optString("thinking",""),evidence));
            } catch (RuntimeException error) { throw new IOException("Invalid chat journal line " + lineNumber + "; journal kept for recovery", error); }
        }
        return List.copyOf(exchanges);
    }
}

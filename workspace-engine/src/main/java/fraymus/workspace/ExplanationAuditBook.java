package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/** Keeps each explanation and trial attachment with its exact original briefing. */
final class ExplanationAuditBook {
    private static final String FORMAT = "comparison-explanation-audit-v1";
    private static final String POLITE = "(?:(?:can|could|would) you )?(?:please )?";
    private static final String START = "(?:audit|test) (?:an?|a proposed) explanation for ";
    private static final Pattern DISPLAYED = wording(START + "that briefing");
    private static final Pattern LAST = wording(START + "the last chart briefing");
    private static final Pattern RESUME = wording("(?:resume|open|recheck) (?:my |the )?last explanation audit");

    enum Intent { DISPLAYED, LAST, RESUME }
    private final Path directory;

    ExplanationAuditBook(Path directory) { this.directory = directory; }

    static Intent intent(String text) {
        String clean = text.strip();
        if (clean.length() > 1200) return null;
        if (DISPLAYED.matcher(clean).matches()) return Intent.DISPLAYED;
        if (LAST.matcher(clean).matches()) return Intent.LAST;
        return RESUME.matcher(clean).matches() ? Intent.RESUME : null;
    }

    private static Pattern wording(String text) { return Pattern.compile(POLITE + text + "[.!?]?", Pattern.CASE_INSENSITIVE); }

    JSONObject start(String briefingId, JSONObject artifact, JSONObject briefingEvidence) throws Exception {
        var original = briefingEvidence.getJSONObject("binding").getJSONObject("source");
        var selection = new JSONArray();
        var rows = briefingEvidence.getJSONArray("rows");
        for (int index = 0; index < rows.length(); index++) {
            var row = rows.getJSONObject(index);
            if (new java.math.BigDecimal(row.getString("delta")).signum() != 0) {
                selection.put(WorldFrames.copy(row).put("index", index));
            }
        }
        if (selection.isEmpty()) throw new IllegalStateException("Choose a briefing with at least one nonzero change.");
        String id = UUID.randomUUID().toString();
        var audit = new JSONObject().put("format", FORMAT).put("id", id).put("briefingId", briefingId)
                .put("briefingArtifact", artifact).put("name", briefingEvidence.getString("name"))
                .put("exportId", original.getString("exportId")).put("sourceSha256", original.getString("sourceSha256"))
                .put("claims", selection);
        save(audit);
        AgentFiles042.saveChecked(directory.resolve("last.json"), new JSONObject().put("id", id));
        return view(audit);
    }

    JSONObject last() throws Exception {
        var file = directory.resolve("last.json");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Start an explanation audit from a verified chart briefing first.");
        }
        return view(load(AgentFiles042.loadChecked(file).getString("id")));
    }

    JSONObject load(String id) throws Exception {
        var record = AgentFiles042.loadChecked(path(id));
        if (!FORMAT.equals(record.getString("format")) || !id.equals(record.getString("id"))) {
            throw new IllegalStateException("The saved explanation audit changed.");
        }
        return record;
    }

    JSONObject submit(JSONObject input) throws Exception {
        SpatialScene.keys(input, "id", "claimIndex", "hypothesis", "assumptions", "margin", "file");
        var audit = load(input.getString("id"));
        var fields = new JSONObject().put("hypothesis",
                ExplanationEvidence.required(input.getString("hypothesis"), 280, "the proposed explanation"))
                .put("assumptions", ExplanationEvidence.required(input.getString("assumptions"), 400, "the trial assumptions"))
                .put("claimIndex", input.getInt("claimIndex"));
        var claim = claim(audit, fields.getInt("claimIndex"));
        fields.put("margin", RoutedDashboardReport.plain(ExplanationEvidence.margin(input.getString("margin"), claim)));
        var supplied = input.optJSONObject("file");
        if (audit.has("attachment")) {
            if (fields.similar(audit.getJSONObject("proposal")) && audit.getJSONObject("attachment").similar(supplied)) return view(audit);
            throw new IllegalStateException("Start a new audit to change a recorded proposal or observation file.");
        }
        if (supplied != null) {
            SpatialScene.keys(supplied, "name", "base64");
            ExplanationEvidence.required(supplied.getString("name"), 160, "the observation filename");
            var checked = ExplanationEvidence.evaluate(supplied.getString("base64"), claim, fields.getString("margin"),
                    audit.getString("sourceSha256"));
            audit.put("attachment", supplied).put("observationsSha256", checked.sha256());
        }
        audit.put("proposal", fields);
        save(audit);
        return view(audit);
    }

    static JSONObject claim(JSONObject audit, int index) {
        for (Object item : audit.getJSONArray("claims")) {
            var claim = (JSONObject) item;
            if (claim.getInt("index") == index) return claim;
        }
        throw new IllegalArgumentException("Choose an original nonzero change from this briefing.");
    }

    private JSONObject view(JSONObject audit) {
        var out = new JSONObject().put("id", audit.getString("id")).put("name", audit.getString("name"))
                .put("exportId", audit.getString("exportId")).put("briefingArtifact", audit.getJSONObject("briefingArtifact"))
                .put("claims", audit.getJSONArray("claims")).put("columns", ExplanationEvidence.HEADER)
                .put("status", audit.has("attachment") ? "ready" : "needs-observations");
        if (audit.has("proposal")) out.put("proposal", audit.getJSONObject("proposal"));
        return out;
    }

    private Path path(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Choose a saved explanation audit.");
        return directory.resolve(id + ".json");
    }

    private void save(JSONObject audit) throws Exception { AgentFiles042.saveChecked(path(audit.getString("id")), audit); }
}

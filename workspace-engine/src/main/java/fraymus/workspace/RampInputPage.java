package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Republishes a bounded, user-authored ramp input as a visible, versioned local page. */
final class RampInputPage {
    private static final List<String> FIELDS = List.of("angleDeg", "friction", "gravityMps2", "massKg", "seconds");
    private static final List<String> LABELS = List.of("Angle (deg)", "Friction", "Gravity (m/s²)", "Mass (kg)", "Duration (s)");
    private static final Pattern REVISION = Pattern.compile(
            "^/artifact/([a-f0-9-]{36})/([1-9][0-9]{0,8})/index\\.html$");
    private final WorkspaceStore store;

    RampInputPage(WorkspaceStore store) { this.store = store; }

    static String canonicalId(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Expected a canonical local identifier");
        return id;
    }

    static JSONObject values(JSONObject data) {
        if (data.length() != FIELDS.size()) throw new IllegalArgumentException("Five ramp fields are required");
        JSONObject normalized = new JSONObject();
        for (String field : FIELDS) {
            if (!(data.opt(field) instanceof Number value) || !Double.isFinite(value.doubleValue())) {
                throw new IllegalArgumentException(field + " must be a finite number");
            }
            normalized.put(field, value.doubleValue());
        }
        definition(normalized);
        RampDefinition.range(normalized.getDouble("seconds"), .25, 2, "duration");
        return normalized;
    }

    static RampDefinition definition(JSONObject data) {
        var config = new JSONObject();
        for (String field : FIELDS.subList(0, 4)) config.put(field, data.getDouble(field));
        return RampDefinition.from(config);
    }

    JSONObject save(JSONObject args) throws Exception {
        SpatialScene.keys(args, "values", "id", "expectedVersion");
        JSONObject data = values(args.getJSONObject("values"));
        var files = new JSONObject().put("ramp-input.json", data.toString(2)).put("index.html", html(data));
        var write = new JSONObject().put("title", "Ramp experiment input").put("files", files);
        if (args.has("id") || args.has("expectedVersion")) {
            String id = canonicalId(args.getString("id"));
            int version = args.getInt("expectedVersion");
            bind(path(id, version), true);
            write.put("id", id).put("expectedVersion", version);
        }
        JSONObject artifact = store.write(write);
        return bind(path(artifact.getString("id"), artifact.getInt("version")), true);
    }

    JSONObject bind(String path, boolean current) throws Exception {
        var match = REVISION.matcher(path);
        if (!match.matches()) throw new IllegalArgumentException("Open a saved ramp input page in the local browser");
        String id = canonicalId(match.group(1));
        int version = Integer.parseInt(match.group(2));
        int latest = store.version(id);
        if (version > latest || (current && latest != version)) {
            throw new IllegalStateException("Ramp input revision changed. Open the current source and start a new mission.");
        }
        String data = read(store.asset(id, version, "ramp-input.json"));
        String page = read(store.asset(id, version, "index.html"));
        JSONObject verified = values(new JSONObject(data));
        if (!page.equals(html(verified))) throw new IllegalStateException("Ramp input HTML and its typed inputs differ");
        return new JSONObject().put("id", id).put("version", version).put("path", path).put("values", verified)
                .put("pageSha256", MeasurementStore.hash(page)).put("dataSha256", MeasurementStore.hash(data));
    }

    static String path(String id, int version) { return "/artifact/" + id + "/" + version + "/index.html"; }

    private static String read(Path path) throws Exception {
        if (!Files.isRegularFile(path) || Files.size(path) > 10_000) throw new IllegalStateException("Choose an actual local ramp input page");
        return Files.readString(path);
    }

    static JSONArray rows(JSONObject data) {
        var rows = new JSONArray();
        for (int i = 0; i < FIELDS.size(); i++) {
            String field = FIELDS.get(i);
            rows.put(new JSONObject().put("field", field).put("label", LABELS.get(i))
                    .put("value", JSONObject.numberToString(data.getDouble(field))));
        }
        return rows;
    }

    static String html(JSONObject data) {
        StringBuilder table = new StringBuilder();
        for (Object item : rows(data)) {
            var row = (JSONObject) item;
            table.append("<tr data-field=\"").append(row.getString("field")).append("\"><th scope=\"row\">")
                    .append(row.getString("label")).append("</th><td>").append(row.getString("value")).append("</td></tr>");
        }
        return """
                <!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
                <title>Ramp experiment input</title><style>
                body{margin:0;background:#edf6f2;color:#153632;font:16px/1.5 system-ui}main{max-width:34rem;margin:auto;padding:1.1rem}
                h1{line-height:1.15}table{border-collapse:collapse;width:100%%}td,th{padding:.5rem .2rem;border-bottom:1px solid #a8c5bd;text-align:left}
                </style><main><h1>Ramp experiment input</h1><p>Local simulation settings in SI units.</p>
                <table id="ramp-input" aria-label="Ramp experiment inputs"><tbody>%s</tbody></table></main></html>
                """.formatted(table);
    }
}

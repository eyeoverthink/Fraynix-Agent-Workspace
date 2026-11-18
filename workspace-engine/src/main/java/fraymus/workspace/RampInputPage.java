package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Pattern;

/** Republishes a bounded, user-authored ramp input as a visible, versioned local page. */
final class RampInputPage {
    private static final List<String> FIELDS = List.of("angleDeg", "friction", "gravityMps2", "massKg", "seconds");
    private static final List<String> LABELS = List.of("Angle (deg)", "Friction", "Gravity (m/s²)", "Mass (kg)", "Duration (s)");
    private static final Pattern REVISION = Pattern.compile(
            "^/artifact/([a-f0-9-]{36})/([1-9][0-9]{0,8})/index\\.html$");
    private static final String CSV_FILE = "ramp-source.csv";
    private static final int MAX_CSV_BYTES = 8192;
    private static final List<String> HEADERS = List.of("angle_deg", "friction", "gravity_mps2", "mass_kg", "duration_s");
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
        RampDefinition.range(normalized.getDouble("seconds"), RampProcedure.MIN_SECONDS, RampProcedure.MAX_SECONDS, "duration");
        return normalized;
    }

    static RampDefinition definition(JSONObject data) {
        var config = new JSONObject();
        for (String field : FIELDS.subList(0, 4)) config.put(field, data.getDouble(field));
        return RampDefinition.from(config);
    }

    JSONObject save(JSONObject args) throws Exception {
        SpatialScene.keys(args, "values", "csv", "id", "expectedVersion");
        if (args.has("values") == args.has("csv")) throw new IllegalArgumentException("Choose form values or one CSV file");
        String csv = args.has("csv") ? args.getString("csv") : null;
        JSONObject data = csv == null ? values(args.getJSONObject("values")) : parseCsv(csv);
        var files = new JSONObject().put("ramp-input.json", data.toString(2)).put("index.html", page(data, csv));
        if (csv != null) files.put(CSV_FILE, csv);
        var write = new JSONObject().put("title", "Ramp experiment input").put("files", files);
        if (args.has("id") || args.has("expectedVersion")) {
            String id = canonicalId(args.getString("id"));
            int version = args.getInt("expectedVersion");
            var previous = bind(path(id, version), true);
            if (previous.has("csvSha256") && csv == null) {
                throw new IllegalArgumentException("Start a new form input to keep the original CSV evidence");
            }
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
        Path original = store.asset(id, version, CSV_FILE);
        String csv = Files.exists(original) ? read(original) : null;
        if (!page.equals(page(verified, csv)) || csv != null && !verified.similar(parseCsv(csv))) {
            throw new IllegalStateException("Ramp input page differs from its typed source");
        }
        JSONObject bound = new JSONObject().put("id", id).put("version", version).put("path", path).put("values", verified)
                .put("pageSha256", MeasurementStore.hash(page)).put("dataSha256", MeasurementStore.hash(data));
        if (csv != null) bound.put("csvSha256", MeasurementStore.hash(csv));
        return bound;
    }

    String originalCsv(JSONObject source) throws Exception {
        if (!source.has("csvSha256")) return null;
        String csv = read(store.asset(source.getString("id"), source.getInt("version"), CSV_FILE));
        if (!source.getString("csvSha256").equals(MeasurementStore.hash(csv))) {
            throw new IllegalStateException("Original ramp CSV bytes changed");
        }
        return csv;
    }

    static JSONObject parseCsv(String csv) {
        if (csv.getBytes(StandardCharsets.UTF_8).length > MAX_CSV_BYTES) throw new IllegalArgumentException("Choose a CSV under 8 KiB");
        var rows = MeasurementStore.csv(csv);
        if (rows.size() != 2 || rows.get(0).size() != HEADERS.size() || rows.get(1).size() != HEADERS.size()) {
            throw new IllegalArgumentException("Ramp CSV needs a header and exactly one data row");
        }
        var header = rows.get(0).stream().map(String::strip).toList();
        if (!new HashSet<>(header).equals(new HashSet<>(HEADERS))) {
            throw new IllegalArgumentException("CSV needs angle_deg, friction, gravity_mps2, mass_kg and duration_s exactly once");
        }
        var data = new JSONObject();
        for (int i = 0; i < HEADERS.size(); i++) {
            String text = rows.get(1).get(header.indexOf(HEADERS.get(i))).strip();
            try {
                if (text.length() > 40) throw new NumberFormatException();
                data.put(FIELDS.get(i), new BigDecimal(text));
            } catch (NumberFormatException bad) {
                throw new IllegalArgumentException(HEADERS.get(i) + " must be a decimal value");
            }
        }
        return values(data);
    }

    private static String page(JSONObject values, String csv) {
        String html = html(values);
        if (csv == null) return html;
        return html.replace("</table></main>", "</table><p>Source: local CSV</p><!-- "
                + MeasurementStore.hash(csv) + " --></main>");
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

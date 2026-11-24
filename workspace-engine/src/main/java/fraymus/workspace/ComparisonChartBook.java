package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/** Project-level names and the exact intent of each chart request. */
final class ComparisonChartBook {
    private static final String NAME = "\"([\\p{L}\\p{N}][\\p{L}\\p{N} _-]{0,55})\"";
    private static final String KEY = "\"[^\"\\r\\n]+\"";
    private static final String POLITE = "(?:(?:can|could|would) you )?(?:please )?";
    private static final Pattern NAMING = Pattern.compile(
            POLITE + "name (?:the last verified comparison|comparison export ([a-f0-9-]{36}))(?: as)? " + NAME + "[.!?]?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern CHART = Pattern.compile(
            POLITE + "(?:(?:make|draw) (?:me )?a chart of|chart|plot) (gains and losses|gains|losses|changes) (?:from|in) " + NAME
                    + "(?: in (fahrenheit|°f|millimetres|millimeters|mm|both units))?"
                    + "(?: for (?:key|keys) (" + KEY + "(?:(?:,\\s*(?:and )?| and )" + KEY + ")*))?[.!?]?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern KEY_TOKEN = Pattern.compile(KEY);
    private static final Pattern RESUME = Pattern.compile(POLITE + "(?:open|resume|recheck) (?:my |the )?last comparison chart[.!?]?",
            Pattern.CASE_INSENSITIVE);
    private static final String FORMAT = "named-comparison-chart-v1";
    private final Path directory;

    ComparisonChartBook(Path directory) { this.directory = directory; }

    record Request(String action, String name, String exportId, JSONObject selection) {}

    static Request request(String text) {
        String request = text.strip();
        if (request.length() > 1200) return null;
        if (RESUME.matcher(request).matches()) return new Request("resume", "", null, null);
        var name = NAMING.matcher(request);
        if (name.matches()) return new Request("name", label(name.group(2)), name.group(1), null);
        var chart = CHART.matcher(request);
        if (!chart.matches()) return null;
        String unit = switch (chart.group(3) == null ? "both units" : chart.group(3).toLowerCase(Locale.ROOT)) {
            case "fahrenheit", "°f" -> "°F";
            case "both units" -> "all";
            default -> "mm";
        };
        String direction = switch (chart.group(1).toLowerCase(Locale.ROOT)) {
            case "gains" -> "gain";
            case "losses" -> "loss";
            case "gains and losses" -> "difference";
            default -> "all";
        };
        var keys = new ArrayList<String>();
        if (chart.group(4) != null) {
            var token = KEY_TOKEN.matcher(chart.group(4));
            while (token.find()) {
                String quoted = token.group();
                String key = quoted.substring(1, quoted.length() - 1);
                if (keys.contains(key)) throw new IllegalArgumentException("Choose distinct original row keys.");
                keys.add(key);
            }
            keys.sort(String::compareTo);
        }
        return new Request("chart", label(chart.group(2)), null,
                new JSONObject().put("direction", direction).put("unit", unit).put("keys", new JSONArray(keys)));
    }

    private static String label(String text) { return Normalizer.normalize(text.strip(), Normalizer.Form.NFC); }

    private Path namePath(String name) {
        return directory.resolve("names").resolve(MeasurementStore.hash(name.toLowerCase(Locale.ROOT)) + ".json");
    }

    JSONObject named(String name) throws Exception {
        var file = namePath(name);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("No verified comparison is named “" + name + "”.");
        }
        var saved = AgentFiles042.loadChecked(file);
        if (!namePath(saved.getString("name")).equals(file)) throw new IllegalStateException("The saved comparison name changed.");
        return saved;
    }

    JSONObject name(Request request, RoutedDashboard.VerifiedSummary checked) throws Exception {
        var file = namePath(request.name());
        var binding = checked.binding();
        if (request.exportId() != null && !request.exportId().equals(binding.getString("exportId"))) {
            throw new IllegalArgumentException("The original named export changed.");
        }
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            var prior = named(request.name());
            if (!binding.similar(prior.getJSONObject("source"))) {
                throw new IllegalStateException("That name already belongs to a different verified comparison.");
            }
            return prior;
        }
        var saved = new JSONObject().put("name", request.name()).put("source", binding);
        AgentFiles042.saveChecked(file, saved);
        return saved;
    }

    JSONObject pin(JSONObject named, JSONObject selection) throws Exception {
        var body = definition(named, selection);
        var id = identity(body);
        var file = path(id);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            if (!body.similar(read(id))) throw new IllegalStateException("The saved comparison chart request changed.");
        } else {
            AgentFiles042.saveChecked(file, body);
        }
        AgentFiles042.saveChecked(directory.resolve("last.json"), new JSONObject().put("id", id));
        return view(id, body);
    }

    JSONObject last() throws Exception {
        var file = directory.resolve("last.json");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IllegalStateException("Request a named comparison chart first.");
        String id = AgentFiles042.loadChecked(file).getString("id");
        return view(id, read(id));
    }

    JSONObject planForChartData(JSONObject data) throws Exception {
        var named = named(data.getString("name"));
        if (!named.getJSONObject("source").similar(data.getJSONObject("source")))
            throw new IllegalStateException("The displayed chart has a different named source.");
        var body = definition(named, data.getJSONObject("selection"));
        String id = identity(body);
        return view(id, read(id));
    }

    private static JSONObject definition(JSONObject named, JSONObject selection) {
        return new JSONObject().put("format", FORMAT).put("named", named).put("selection", selection);
    }

    private static String identity(JSONObject body) {
        return UUID.nameUUIDFromBytes(MapCheckpoint.canonical(body).getBytes(StandardCharsets.UTF_8)).toString();
    }

    JSONObject read(String id) throws Exception {
        var record = AgentFiles042.loadChecked(path(id));
        String actual = identity(record);
        if (!id.equals(actual) || !FORMAT.equals(record.getString("format"))) {
            throw new IllegalStateException("The saved comparison chart request changed.");
        }
        var named = record.getJSONObject("named");
        if (!named.similar(named(named.getString("name")))) throw new IllegalStateException("The named comparison binding changed.");
        return record;
    }

    JSONObject pinBriefing(JSONObject chartPlan, JSONObject chartRef) throws Exception {
        var binding = new JSONObject().put("chartId", chartPlan.getString("comparisonChartId"))
                .put("exportId", chartPlan.getString("exportId")).put("chartArtifact", chartRef);
        String id = briefingIdentity(binding);
        var file = briefingPath(id);
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            if (!binding.similar(briefing(id))) throw new IllegalStateException("The pinned chart briefing changed.");
        } else {
            AgentFiles042.saveChecked(file, binding);
        }
        AgentFiles042.saveChecked(directory.resolve("briefings/last.json"), new JSONObject().put("id", id));
        return WorldFrames.copy(binding).put("chartBriefingId", id);
    }

    JSONObject lastBriefing() throws Exception {
        var file = directory.resolve("briefings/last.json");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Build a briefing from a named comparison chart first.");
        }
        String id = AgentFiles042.loadChecked(file).getString("id");
        return briefing(id).put("chartBriefingId", id);
    }

    JSONObject briefing(String id) throws Exception {
        var binding = AgentFiles042.loadChecked(briefingPath(id));
        if (!id.equals(briefingIdentity(binding))) throw new IllegalStateException("The pinned chart briefing changed.");
        return binding;
    }

    private static String briefingIdentity(JSONObject binding) {
        return UUID.nameUUIDFromBytes(("named-chart-briefing-v1:" + MapCheckpoint.canonical(binding))
                .getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Path briefingPath(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Choose a saved chart briefing.");
        return directory.resolve("briefings").resolve(id + ".json");
    }

    private Path path(String id) {
        if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException("Choose a saved comparison chart.");
        return directory.resolve(id + ".json");
    }

    private static JSONObject view(String id, JSONObject record) {
        var named = record.getJSONObject("named");
        return new JSONObject().put("comparisonChartId", id).put("name", named.getString("name"))
                .put("exportId", named.getJSONObject("source").getString("exportId"))
                .put("selection", record.getJSONObject("selection"));
    }
}

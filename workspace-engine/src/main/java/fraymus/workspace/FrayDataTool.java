package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/** Builds an offline batch tool and independently checks its browser output against Fray in Java. */
final class FrayDataTool {
    static final String PREVIEW = "/data-tool-preview/";
    static final String NAME = "[a-z0-9][a-z0-9-]{0,39}";
    static final Pattern BUILD = Pattern.compile(
            "Build a data-to-tool converter from (Celsius to Fahrenheit|inches to (?:millimetres|millimeters))[.!]?",
            Pattern.CASE_INSENSITIVE);
    static final Pattern TEACH = Pattern.compile("Teach data-to-tool lesson (" + NAME + ")[.!]?", Pattern.CASE_INSENSITIVE);
    static final Pattern USE = Pattern.compile("Use data-to-tool lesson (" + NAME + ")[.!]?", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE = Pattern.compile("[+-]?[0-9]{1,7}(?:\\.[0-9]{1,4})?");
    private static final String INVALID = "Use the indicated header and 1–24 rows of plain decimals within range.";
    private static final List<String> HOLDOUTS = List.of("-19.8251", "+0.0001", "1000000", "-1000000", "246.875");
    private static final String TEMPLATE = FrayConversion.loadHtml("data-tool.html");

    record Input(String text, List<String> values) {
        List<String> numericValues() {
            return values.stream().map(s -> new BigDecimal(s).stripTrailingZeros().toPlainString()).toList();
        }
    }

    final String column;
    private final String title;
    private final FrayConversion conversion;

    FrayDataTool(String column) {
        this.column = column;
        conversion = switch (column) {
            case "celsius" -> FrayConversion.CELSIUS;
            case "inches" -> FrayConversion.INCH;
            default -> throw new IllegalArgumentException("Unsupported data-to-tool input column");
        };
        title = column.equals("celsius") ? "CSV · Celsius to Fahrenheit" : "CSV · Inches to millimetres";
    }

    String programHash() { return conversion.programHash(); }

    Input input(byte[] bytes) throws Exception {
        if (bytes.length > 2048) throw new IllegalArgumentException("Choose a CSV of up to 2 KiB");
        String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        return parse(text.replace("\r\n", "\n").replace('\r', '\n'));
    }

    private Input parse(String text) {
        String body = text.endsWith("\n") ? text.substring(0, text.length() - 1) : text;
        String[] rows = body.split("\n", -1);
        if (!rows[0].equals(column))
            throw new IllegalArgumentException("The CSV must have one column named " + column);
        if (rows.length < 2 || rows.length > 25) throw new IllegalArgumentException(INVALID);
        var values = new ArrayList<String>();
        for (int i = 1; i < rows.length; i++) {
            String value = rows[i].replaceAll("^[ \\t]+|[ \\t]+$", "");
            if (!VALUE.matcher(value).matches() || Math.abs(Double.parseDouble(value)) > 1_000_000)
                throw new IllegalArgumentException(INVALID + " Check row " + (i + 1) + ".");
            values.add(value);
        }
        return new Input(column + "\n" + String.join("\n", values), List.copyOf(values));
    }

    String html(Input input) {
        // Input contains only the registered header, decimal tokens and line breaks.
        return TEMPLATE.replace("@TITLE@", title).replace("@COLUMN@", column).replace("@DATA@", input.text());
    }

    JSONArray browserSteps(Input input) {
        var held = new ArrayList<>(HOLDOUTS);
        var unseen = new HashSet<>(input.numericValues());
        held.removeIf(value -> unseen.contains(new BigDecimal(value).stripTrailingZeros().toPlainString()));
        for (int suffix = 1001; held.size() < 3; suffix++) {
            String value = "712." + suffix;
            if (!unseen.contains(new BigDecimal(value).stripTrailingZeros().toPlainString())) held.add(value);
        }
        var holdout = parse(column + "\n" + String.join("\n", held));
        var steps = new JSONArray()
                .put(new JSONObject().put("selector", "#source").put("expectValue", input.text()))
                .put(outputStep(result(input)));
        fill(steps, holdout.text(), result(holdout));
        fill(steps, (column.equals("celsius") ? "inches" : "celsius") + "\n1", INVALID);
        fill(steps, column + "\n1e3", INVALID);
        fill(steps, input.text(), result(input));
        return steps;
    }

    private static void fill(JSONArray steps, String text, String expected) {
        steps.put(new JSONObject().put("selector", "#source").put("action", "fill")
                .put("value", text).put("expectValue", text)).put(outputStep(expected));
    }

    private static JSONObject outputStep(String text) {
        return new JSONObject().put("selector", "#result").put("expectText", text);
    }

    private String result(Input input) {
        return String.join("\n", input.values().stream().map(conversion::expected).toList());
    }

    JSONObject artifact(Input input, JSONObject source, JSONObject lesson, JSONArray checks) {
        var plan = browserSteps(input);
        if (checks.length() != plan.length()) throw new IllegalStateException("The data-tool browser checks are incomplete");
        var cases = new JSONArray();
        for (int i = 0; i < plan.length(); i++) {
            var step = plan.getJSONObject(i);
            var actual = FrayConversion.verifyStep(step, checks.getJSONObject(i), i);
            if (step.has("expectText"))
                cases.put(new JSONObject().put("input", plan.getJSONObject(i - 1).getString("expectValue"))
                        .put("java", step.getString("expectText")).put("browser", actual.getString("text").trim()));
        }
        String html = html(input);
        var evidence = new JSONObject().put("passed", true).put("inputColumn", column).put("inputCount", input.values().size())
                .put("sourceSha256", source.getString("sha256")).put("htmlSha256", MeasurementStore.hash(html))
                .put("fraySource", conversion.source).put("frayBytecodeHex", conversion.bytecodeHex)
                .put("compilerCommit", FrayConversion.COMPILER).put("cases", cases);
        var sources = new JSONArray().put("csv:sha256:" + source.getString("sha256"))
                .put("fray-bytecode:sha256:" + programHash());
        if (lesson != null) {
            evidence.put("lesson", lesson);
            sources.put("fray-data-tool-lesson:sha256:" + WorldFrames.hash(lesson));
        }
        return new JSONObject().put("title", title).put("sources", sources)
                .put("files", new JSONObject().put("index.html", html)
                        .put("fray-source.txt", conversion.source + "\n")
                        .put("verification.json", evidence.toString(2) + "\n"));
    }
}

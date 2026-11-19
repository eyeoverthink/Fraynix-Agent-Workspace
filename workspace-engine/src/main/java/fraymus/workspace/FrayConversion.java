package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** Built-in Fray programs and their independently implemented browser artifacts. */
enum FrayConversion {
    INCH("Inches to millimetres", "inches", "1", "mm", "inch-converter.html", "inches * 25.4",
            "465241590100010001000000084039666666666666696e636865730002000001000012ff",
            "[+-]?[0-9]{1,7}(?:\\.[0-9]{1,4})?",
            List.of("0", "-2.5", "0.125", "1.2345", "999999.9999", "1000000", "-1000000",
                    "+0002.0001", "1e3", "1000000.0001", " ")),
    CELSIUS("Celsius to Fahrenheit", "celsius", "20", "\u00b0F", "temperature-converter.html",
            "celsius * 9 / 5 + 32",
            "4652415901000300010000001040220000000000004014000000000000404000000000000063656c7369757300020000010000120100011301000210ff",
            "[+-]?(?:[0-9]{1,7}(?:\\.[0-9]{1,4})?|\\.[0-9]{1,4})",
            List.of("0", "100", "-40", "-273.15", "37.25", "-17.7778", ".05", "+999999.9999",
                    "1000000.0001", "1,5", " "));

    static final String PREVIEW = "/request-preview/";
    static final String COMPILER = "d93947154a1f21dcfd8dfcc97f27439fed6e3275";
    private final String title, inputId, defaultInput, unit, html;
    final String source, bytecodeHex;
    final String htmlSha256;
    private final Pattern decimal;
    private final List<String> inputs;
    private final FrayFormula program;

    FrayConversion(String title, String inputId, String defaultInput, String unit, String resource,
                   String source, String bytecodeHex, String decimal, List<String> inputs) {
        this.title = title;
        this.inputId = inputId;
        this.defaultInput = defaultInput;
        this.unit = unit;
        this.source = source;
        this.bytecodeHex = bytecodeHex;
        this.decimal = Pattern.compile(decimal);
        this.inputs = inputs;
        this.program = FrayFormula.read(HexFormat.of().parseHex(bytecodeHex), inputId);
        this.html = loadHtml(resource);
        this.htmlSha256 = MeasurementStore.hash(html);
    }

    String expected(String input) {
        String text = input.strip();
        if (text.isEmpty()) return "Enter a measurement.";
        if (decimal.matcher(text).matches()) {
            double value = Double.parseDouble(text);
            if (Math.abs(value) <= 1_000_000) {
                var converted = BigDecimal.valueOf(program.evaluate(value)).setScale(5, RoundingMode.HALF_UP);
                return converted.stripTrailingZeros().toPlainString() + " " + unit;
            }
        }
        return "Enter a decimal within the stated range.";
    }

    JSONArray steps() {
        var steps = new JSONArray().put(resultStep(defaultInput));
        for (String input : inputs) {
            steps.put(new JSONObject().put("selector", "#" + inputId).put("action", "fill")
                    .put("value", input).put("expectValue", input));
            steps.put(resultStep(input));
        }
        return steps;
    }

    private JSONObject resultStep(String input) {
        return new JSONObject().put("selector", "#result").put("expectText", expected(input));
    }

    /** Recheck actual values against the Java reference, independently of the host's verdict. */
    private JSONArray verify(JSONArray checks) {
        var plan = steps();
        if (checks.length() != plan.length()) throw new IllegalStateException("The browser checks are incomplete");
        var cases = new JSONArray();
        String input = defaultInput;
        for (int i = 0; i < plan.length(); i++) {
            var step = plan.getJSONObject(i);
            var actual = verifyStep(step, checks.getJSONObject(i), i);
            if (step.has("expectValue")) {
                input = step.getString("expectValue");
            } else {
                String observed = actual.getString("text").trim();
                String expected = step.getString("expectText");
                cases.put(new JSONObject().put("input", input).put("java", expected).put("browser", observed));
            }
        }
        return cases;
    }

    static JSONObject verifyStep(JSONObject step, JSONObject check, int index) {
        var actual = check.optJSONObject("actual");
        if (!check.optBoolean("passed") || actual == null || check.getInt("index") != index
                || !step.getString("selector").equals(check.getString("selector"))
                || !step.similar(check.getJSONObject("expected")) || actual.getInt("count") != step.optInt("expectCount", 1))
            throw new IllegalStateException("Browser check " + (index + 1) + " failed");
        if (step.has("expectValue") && !step.getString("expectValue").equals(actual.getString("value")))
            throw new IllegalStateException("Browser input differed");
        if (step.has("expectText") && !step.getString("expectText").equals(actual.getString("text").trim()))
            throw new IllegalStateException("Browser result differed from Fray");
        return actual;
    }

    JSONObject artifact(JSONArray checks) {
        var cases = verify(checks);
        var evidence = new JSONObject().put("compilerCommit", COMPILER).put("fraySource", source)
                .put("frayBytecodeHex", bytecodeHex).put("htmlSha256", htmlSha256)
                .put("browserCases", cases).put("passed", true);
        return new JSONObject().put("title", title)
                .put("sources", List.of("fray-bytecode:sha256:" + programHash()))
                .put("files", new JSONObject().put("index.html", html).put("fray-source.txt", source + "\n")
                        .put("verification.json", evidence.toString(2) + "\n"));
    }

    String programHash() {
        try {
            return PerceptionStore.hash(HexFormat.of().parseHex(bytecodeHex));
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash the Fray program", e);
        }
    }

    String previewPath() { return PREVIEW + htmlSha256 + "/index.html"; }
    byte[] preview() { return html.getBytes(StandardCharsets.UTF_8); }

    static FrayConversion previewAt(String path) {
        return Arrays.stream(values()).filter(recipe -> recipe.previewPath().equals(path)).findFirst().orElse(null);
    }

    static String loadHtml(String resource) {
        try (var in = FrayConversion.class.getResourceAsStream("/workbench/" + resource)) {
            if (in == null) throw new IllegalStateException("The browser converter resource is missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

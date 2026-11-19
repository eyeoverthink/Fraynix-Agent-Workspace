package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

/** One reviewed Fray program and an independently implemented browser artifact. */
final class FrayConversion {
    static final String TITLE = "Inches to millimetres";
    static final String PREVIEW = "/request-preview/";
    private static final String SOURCE = "inches * 25.4";
    private static final String COMPILER = "d93947154a1f21dcfd8dfcc97f27439fed6e3275";
    // Output of FraynixBoot's FrayBytecode.Compiler.compile(SOURCE).serialise().
    private static final byte[] PROGRAM = HexFormat.of().parseHex(
            "465241590100010001000000084039666666666666696e636865730002000001000012ff");
    private static final Pattern DECIMAL = Pattern.compile("[+-]?[0-9]{1,7}(?:\\.[0-9]{1,4})?");
    private static final List<String> INPUTS = List.of("0", "-2.5", "0.125", "1.2345", "999999.9999",
            "1000000", "-1000000", "+0002.0001", "1e3", "1000000.0001", " ");
    private static final double CONSTANT = programMultiplier();
    private static final String HTML = html();
    static final String HTML_SHA256 = MeasurementStore.hash(HTML);

    private FrayConversion() {}

    /** Supports exactly the generated load-variable, load-constant, multiply, return program. */
    private static double programMultiplier() {
        var data = ByteBuffer.wrap(PROGRAM);
        if (data.getInt() != 0x46524159 || data.get() != 1 || data.getShort() != 1
                || data.getShort() != 1 || data.getInt() != 8)
            throw new IllegalStateException("Unsupported Fray conversion program header");
        double multiplier = data.getDouble();
        byte[] body = new byte[data.remaining()];
        data.get(body);
        if (!Double.isFinite(multiplier)
                || !HexFormat.of().formatHex(body).equals("696e636865730002000001000012ff"))
            throw new IllegalStateException("Unsupported Fray conversion bytecode");
        return multiplier;
    }

    static String expected(String input) {
        String text = input.strip();
        if (text.isEmpty()) return "Enter a measurement.";
        if (DECIMAL.matcher(text).matches()) {
            double inches = Double.parseDouble(text);
            if (Math.abs(inches) <= 1_000_000) {
                var millimetres = BigDecimal.valueOf(inches * CONSTANT).setScale(5, RoundingMode.HALF_UP);
                return millimetres.stripTrailingZeros().toPlainString() + " mm";
            }
        }
        return "Enter a decimal within the stated range.";
    }

    static JSONArray steps() {
        var steps = new JSONArray().put(resultStep("1"));
        for (String input : INPUTS) {
            steps.put(new JSONObject().put("selector", "#inches").put("action", "fill")
                    .put("value", input).put("expectValue", input));
            steps.put(resultStep(input));
        }
        return steps;
    }

    private static JSONObject resultStep(String input) {
        return new JSONObject().put("selector", "#result").put("expectText", expected(input));
    }

    /** Recheck actual values against the Java reference, independently of the host's verdict. */
    static JSONArray verify(JSONArray checks) {
        var plan = steps();
        if (checks.length() != plan.length()) throw new IllegalStateException("The browser checks are incomplete");
        var cases = new JSONArray();
        String input = "1";
        for (int i = 0; i < plan.length(); i++) {
            var step = plan.getJSONObject(i);
            var check = checks.getJSONObject(i);
            var actual = check.getJSONObject("actual");
            if (check.getInt("index") != i || !step.getString("selector").equals(check.getString("selector"))
                    || !step.similar(check.getJSONObject("expected")) || actual.getInt("count") != 1
                    || !check.getBoolean("passed"))
                throw new IllegalStateException("Browser check " + (i + 1) + " failed");
            if (step.has("expectValue")) {
                input = step.getString("expectValue");
                if (!input.equals(actual.getString("value"))) throw new IllegalStateException("Browser input differed");
            } else {
                String observed = actual.getString("text").trim();
                String expected = step.getString("expectText");
                if (!expected.equals(observed)) throw new IllegalStateException("Browser result differed from Fray");
                cases.put(new JSONObject().put("input", input).put("java", expected).put("browser", observed));
            }
        }
        return cases;
    }

    static JSONObject artifact(JSONArray checks) {
        var cases = verify(checks);
        var evidence = new JSONObject().put("compilerCommit", COMPILER).put("fraySource", SOURCE)
                .put("frayBytecodeHex", HexFormat.of().formatHex(PROGRAM)).put("htmlSha256", HTML_SHA256)
                .put("browserCases", cases).put("passed", true);
        return new JSONObject().put("title", TITLE)
                .put("sources", List.of("fray-bytecode:sha256:" + programHash()))
                .put("files", new JSONObject().put("index.html", HTML).put("fray-source.txt", SOURCE + "\n")
                        .put("verification.json", evidence.toString(2) + "\n"));
    }

    private static String programHash() {
        try {
            return PerceptionStore.hash(PROGRAM);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash the Fray program", e);
        }
    }

    static String previewPath() { return PREVIEW + HTML_SHA256 + "/index.html"; }
    static byte[] preview() { return HTML.getBytes(StandardCharsets.UTF_8); }

    private static String html() {
        try (var in = FrayConversion.class.getResourceAsStream("/workbench/inch-converter.html")) {
            if (in == null) throw new IllegalStateException("The browser converter resource is missing");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** One composed recipe, with a verified quantity passed between its two Fray programs. */
final class FrayChain {
    static final String TITLE = "Inches to centimetres in two steps";
    static final String PREVIEW = "/fray-chain-preview/";
    private static final String SOURCE = "millimetres / 10";
    private static final String BYTECODE = "4652415901000100010000000840240000000000006d696c6c696d65747265730002000001000013ff";
    private static final FrayFormula SECOND = FrayFormula.read(HexFormat.of().parseHex(BYTECODE), "millimetres");
    private static final String PAGE = FrayConversion.loadHtml("length-chain.html");
    private static final String WAITING = "Waiting for conversion.";
    private static final Pattern NUMBER = Pattern.compile("[+-]?[0-9]{1,7}(?:\\.[0-9]{1,4})?");
    private static final String UNIT_PATTERN = "(\\p{L}+)";
    private static final Pattern REQUEST = Pattern.compile("^(?:please\\s+)?convert\\s+([+-]?[0-9.]+)\\s+"
            + UNIT_PATTERN + "\\s+to\\s+" + UNIT_PATTERN + "\\s*,?\\s+then\\s+(?:convert\\s+)?"
            + UNIT_PATTERN + "\\s+to\\s+" + UNIT_PATTERN + "[.!]?$");

    private enum Unit {
        IN("in"), MM("mm"), CM("cm"), C("\u00b0C"), F("\u00b0F");
        final String symbol;
        Unit(String symbol) { this.symbol = symbol; }

        static Unit parse(String name) {
            return switch (name) {
                case "in", "inch", "inches" -> IN;
                case "mm", "millimetre", "millimetres", "millimeter", "millimeters" -> MM;
                case "cm", "centimetre", "centimetres", "centimeter", "centimeters" -> CM;
                case "c", "celsius", "centigrade" -> C;
                case "f", "fahrenheit" -> F;
                default -> throw new IllegalArgumentException("Unknown conversion unit " + name);
            };
        }
    }

    record Quantity(BigDecimal amount, Unit unit) {
        String value() { return amount.stripTrailingZeros().toPlainString(); }
        String text() { return value() + " " + unit.symbol; }
        JSONObject json() { return new JSONObject().put("value", value()).put("unit", unit.symbol); }
    }

    private final Quantity input;
    final String html;
    final String htmlSha256;

    private FrayChain(String number) {
        if (!NUMBER.matcher(number).matches())
            throw new IllegalArgumentException("Use a number with up to four decimal places.");
        var value = new BigDecimal(number);
        if (value.abs().compareTo(BigDecimal.valueOf(1_000_000)) > 0)
            throw new IllegalArgumentException("Use a value from -1000000 to 1000000 inches.");
        input = new Quantity(value, Unit.IN);
        html = PAGE.replace("{{initialInches}}", input.value());
        htmlSha256 = MeasurementStore.hash(html);
    }

    static FrayChain fromRequest(String text) {
        var match = REQUEST.matcher(text.toLowerCase(Locale.ROOT).replace("\u00b0", ""));
        if (!match.matches()) return null;
        var start = Unit.parse(match.group(2));
        var intermediate = Unit.parse(match.group(3));
        var next = Unit.parse(match.group(4));
        var end = Unit.parse(match.group(5));
        if (intermediate != next)
            throw new IllegalArgumentException("Cannot hand off " + intermediate.symbol + " to a step that expects "
                    + next.symbol + ".");
        if (start != Unit.IN || intermediate != Unit.MM || end != Unit.CM)
            throw new IllegalArgumentException("The supported chain converts inches to millimetres, then to centimetres.");
        return new FrayChain(match.group(1));
    }

    JSONObject definition() {
        return new JSONObject().put("id", "inch-mm-cm-v1").put("input", input.json())
                .put("first", "in to mm").put("second", "mm to cm");
    }

    static FrayChain fromDefinition(JSONObject data) {
        var chain = new FrayChain(data.getJSONObject("input").getString("value"));
        if (!chain.definition().similar(data)) throw new IllegalStateException("The reviewed chain changed");
        return chain;
    }

    String previewPath() { return PREVIEW + input.value() + "/" + htmlSha256 + "/index.html"; }
    static FrayChain previewAt(String path) {
        var parts = path.split("/", -1);
        if (parts.length != 5) return null;
        try {
            var chain = new FrayChain(parts[2]);
            return chain.previewPath().equals(path) ? chain : null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    JSONArray firstSteps() {
        return new JSONArray()
                .put(new JSONObject().put("selector", "#inches").put("expectValue", input.value()))
                .put(new JSONObject().put("selector", "#to-cm:disabled").put("expectCount", 1))
                .put(click("#to-mm"))
                .put(text("#millimetres", expectedIntermediate()))
                .put(text("#centimetres", WAITING));
    }

    Quantity verifiedIntermediate(JSONArray checks) {
        check(firstSteps(), checks);
        String observed = checks.getJSONObject(3).getJSONObject("actual").getString("text").trim();
        var parts = observed.split(" ");
        return new Quantity(new BigDecimal(parts[0]), Unit.parse(parts[1]));
    }

    JSONArray secondSteps(Quantity handoff) {
        if (handoff.unit != Unit.MM) throw new IllegalStateException("The second Fray step requires millimetres");
        var answer = new Quantity(BigDecimal.valueOf(SECOND.evaluate(handoff.amount.doubleValue()))
                .setScale(6, RoundingMode.HALF_UP), Unit.CM);
        return new JSONArray().put(text("#millimetres", handoff.text())).put(text("#centimetres", WAITING))
                .put(click("#to-cm")).put(text("#handoff", handoff.text())).put(text("#centimetres", answer.text()));
    }

    JSONObject artifact(JSONArray first, JSONArray second) throws Exception {
        var handoff = verifiedIntermediate(first);
        check(secondSteps(handoff), second);
        var result = second.getJSONObject(4).getJSONObject("actual").getString("text").trim();
        var evidence = new JSONObject().put("chain", definition()).put("htmlSha256", htmlSha256)
                .put("compilerCommit", FrayConversion.COMPILER).put("firstBytecode", FrayConversion.INCH.bytecodeHex)
                .put("secondBytecode", BYTECODE).put("intermediate", handoff.json()).put("result", result)
                .put("firstChecks", first).put("secondChecks", second).put("passed", true);
        return new JSONObject().put("title", TITLE).put("sources", List.of(
                        "fray-bytecode:sha256:" + FrayConversion.INCH.programHash(),
                        "fray-bytecode:sha256:" + PerceptionStore.hash(HexFormat.of().parseHex(BYTECODE))))
                .put("files", new JSONObject().put("index.html", html)
                        .put("fray-source.txt", FrayConversion.INCH.source + "\n" + SOURCE + "\n")
                        .put("verification.json", evidence.toString(2) + "\n"));
    }

    private String expectedIntermediate() { return FrayConversion.INCH.expected(input.value()); }

    private static JSONObject text(String selector, String value) {
        return new JSONObject().put("selector", selector).put("expectText", value);
    }

    private static JSONObject click(String selector) {
        return new JSONObject().put("selector", selector).put("action", "click");
    }

    static JSONArray join(JSONArray first, JSONArray second, boolean index) {
        var combined = new JSONArray(first.toString());
        for (int i = 0; i < second.length(); i++) {
            var item = new JSONObject(second.getJSONObject(i).toString());
            if (index) item.put("index", combined.length());
            combined.put(item);
        }
        return combined;
    }

    private static void check(JSONArray steps, JSONArray actual) {
        if (steps.length() != actual.length()) throw new IllegalStateException("The browser checks are incomplete");
        for (int i = 0; i < steps.length(); i++)
            FrayConversion.verifyStep(steps.getJSONObject(i), actual.getJSONObject(i), i);
    }
}

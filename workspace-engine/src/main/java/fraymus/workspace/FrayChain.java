package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Reviewed two-stage recipes that pass an observed quantity with its unit. */
final class FrayChain {
    static final String PREVIEW = "/fray-chain-preview/";
    private static final String WAITING = "Waiting for conversion.";
    private static final Pattern NUMBER = Pattern.compile("[+-]?[0-9]{1,7}(?:\\.[0-9]{1,4})?");
    private static final String UNIT_PATTERN = "(\\p{L}+(?:\\s+\\p{L}+){0,2})";
    private static final Pattern REQUEST = Pattern.compile("^(?:please\\s+)?convert\\s+([+-]?[0-9.]+)\\s*"
            + UNIT_PATTERN + "\\s+to\\s+" + UNIT_PATTERN + "\\s*,?\\s+then\\s+(?:convert\\s+)?"
            + UNIT_PATTERN + "\\s+to\\s+" + UNIT_PATTERN + "[.!]?$");

    private enum Unit {
        IN("in"), MM("mm"), CM("cm"), C("\u00b0C"), F("\u00b0F"), R("\u00b0R"), K("K");
        final String symbol;
        Unit(String symbol) { this.symbol = symbol; }

        static Unit parse(String name) {
            String key = normalize(name).strip().replaceFirst("^(?:degrees?|deg)\\s+", "");
            return switch (key) {
                case "in", "inch", "inches" -> IN;
                case "mm", "millimetre", "millimetres", "millimeter", "millimeters" -> MM;
                case "cm", "centimetre", "centimetres", "centimeter", "centimeters" -> CM;
                case "c", "celsius", "centigrade" -> C;
                case "f", "fahrenheit" -> F;
                case "r", "rankine" -> R;
                case "k", "kelvin", "kelvins" -> K;
                default -> throw new IllegalArgumentException("Unknown conversion unit " + name);
            };
        }
    }

    record Quantity(BigDecimal amount, Unit unit) {
        String value() { return amount.stripTrailingZeros().toPlainString(); }
        String text() { return value() + " " + unit.symbol; }
        JSONObject json() { return new JSONObject().put("value", value()).put("unit", unit.symbol); }
    }

    private enum Recipe {
        LENGTH("inch-mm-cm-v1", "Inches to centimetres in two steps", Unit.IN, Unit.MM, Unit.CM,
                "inches", "millimetres", "centimetres", "to-mm", "to-cm", "length-chain.html",
                FrayConversion.INCH, "millimetres / 10",
                "4652415901000100010000000840240000000000006d696c6c696d65747265730002000001000013ff",
                "-1000000", 6),
        TEMPERATURE("c-f-r-v1", "Celsius to Rankine in two steps", Unit.C, Unit.F, Unit.R,
                "celsius", "fahrenheit", "rankine", "to-f", "to-r", "temperature-chain.html",
                FrayConversion.CELSIUS, "fahrenheit + 459.67",
                "46524159010001000100000008407cbab851eb851f66616872656e686569740002000001000010ff",
                "-273.15", 5);

        final String id, title, inputId, intermediateId, outputId, firstButton, secondButton, page, source, bytecode;
        final Unit start, middle, end;
        final BigDecimal minimum;
        final int precision;
        final FrayConversion first;
        final FrayFormula second;

        Recipe(String id, String title, Unit start, Unit middle, Unit end, String inputId, String intermediateId,
               String outputId, String firstButton, String secondButton, String resource, FrayConversion first,
               String source, String bytecode, String minimum, int precision) {
            this.id = id;
            this.title = title;
            this.start = start;
            this.middle = middle;
            this.end = end;
            this.inputId = inputId;
            this.intermediateId = intermediateId;
            this.outputId = outputId;
            this.firstButton = firstButton;
            this.secondButton = secondButton;
            this.page = FrayConversion.loadHtml(resource);
            this.first = first;
            this.source = source;
            this.bytecode = bytecode;
            this.minimum = new BigDecimal(minimum);
            this.precision = precision;
            this.second = FrayFormula.read(HexFormat.of().parseHex(bytecode), intermediateId);
        }

        static Recipe read(String id) {
            return Arrays.stream(values()).filter(recipe -> recipe.id.equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown Fray chain"));
        }
    }

    private final Recipe recipe;
    private final Quantity input;
    final String html;
    final String htmlSha256;

    private FrayChain(Recipe recipe, String number) {
        if (!NUMBER.matcher(number).matches())
            throw new IllegalArgumentException("Use a number with up to four decimal places.");
        var value = new BigDecimal(number);
        if (value.compareTo(recipe.minimum) < 0 || value.compareTo(BigDecimal.valueOf(1_000_000)) > 0)
            throw new IllegalArgumentException("Use a value from " + recipe.minimum.toPlainString()
                    + " to 1000000 " + recipe.start.symbol + ".");
        this.recipe = recipe;
        input = new Quantity(value, recipe.start);
        html = recipe.page.replace(recipe == Recipe.LENGTH ? "{{initialInches}}" : "{{initialCelsius}}", input.value());
        htmlSha256 = MeasurementStore.hash(html);
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replace("\u00b0", "");
    }

    static FrayChain fromRequest(String text) {
        var match = REQUEST.matcher(normalize(text));
        if (!match.matches()) return null;
        var start = Unit.parse(match.group(2));
        var intermediate = Unit.parse(match.group(3));
        var next = Unit.parse(match.group(4));
        var end = Unit.parse(match.group(5));
        if (intermediate != next)
            throw new IllegalArgumentException("Cannot hand off " + intermediate.symbol + " to a step that expects "
                    + next.symbol + ".");
        for (var recipe : Recipe.values()) {
            if (start == recipe.start && intermediate == recipe.middle && end == recipe.end)
                return new FrayChain(recipe, match.group(1));
        }
        throw new IllegalArgumentException("Use inches to millimetres to centimetres, or Celsius to Fahrenheit to Rankine.");
    }

    JSONObject definition() {
        return new JSONObject().put("id", recipe.id).put("input", input.json())
                .put("first", recipe.start.symbol + " to " + recipe.middle.symbol)
                .put("second", recipe.middle.symbol + " to " + recipe.end.symbol);
    }

    static FrayChain fromDefinition(JSONObject data) {
        var chain = new FrayChain(Recipe.read(data.getString("id")), data.getJSONObject("input").getString("value"));
        if (!chain.definition().similar(data)) throw new IllegalStateException("The reviewed chain changed");
        return chain;
    }

    String title() { return recipe.title; }

    String previewPath() {
        String recipePath = recipe == Recipe.LENGTH ? "" : recipe.id + "/";
        return PREVIEW + recipePath + input.value() + "/" + htmlSha256 + "/index.html";
    }

    static FrayChain previewAt(String path) {
        var parts = path.split("/", -1);
        if (parts.length != 5 && parts.length != 6) return null;
        try {
            var recipe = parts.length == 5 ? Recipe.LENGTH : Recipe.read(parts[2]);
            var chain = new FrayChain(recipe, parts[parts.length == 5 ? 2 : 3]);
            return chain.previewPath().equals(path) ? chain : null;
        } catch (IllegalArgumentException invalid) {
            return null;
        }
    }

    JSONArray firstSteps() {
        return new JSONArray()
                .put(new JSONObject().put("selector", "#" + recipe.inputId).put("expectValue", input.value()))
                .put(new JSONObject().put("selector", "#" + recipe.secondButton + ":disabled").put("expectCount", 1))
                .put(click("#" + recipe.firstButton))
                .put(text("#" + recipe.intermediateId, expectedIntermediate()))
                .put(text("#" + recipe.outputId, WAITING));
    }

    Quantity verifiedIntermediate(JSONArray checks) {
        check(firstSteps(), checks);
        String observed = checks.getJSONObject(3).getJSONObject("actual").getString("text").trim();
        var parts = observed.split(" ");
        return new Quantity(new BigDecimal(parts[0]), Unit.parse(parts[1]));
    }

    JSONArray secondSteps(Quantity handoff) {
        if (handoff.unit != recipe.middle) throw new IllegalStateException("The second Fray step requires " + recipe.middle.symbol);
        var answer = new Quantity(BigDecimal.valueOf(recipe.second.evaluate(handoff.amount.doubleValue()))
                .setScale(recipe.precision, RoundingMode.HALF_UP), recipe.end);
        return new JSONArray().put(text("#" + recipe.intermediateId, handoff.text())).put(text("#" + recipe.outputId, WAITING))
                .put(click("#" + recipe.secondButton)).put(text("#handoff", handoff.text())).put(text("#" + recipe.outputId, answer.text()));
    }

    JSONObject artifact(JSONArray first, JSONArray second) throws Exception {
        var handoff = verifiedIntermediate(first);
        check(secondSteps(handoff), second);
        var result = second.getJSONObject(4).getJSONObject("actual").getString("text").trim();
        var evidence = new JSONObject().put("chain", definition()).put("htmlSha256", htmlSha256)
                .put("compilerCommit", FrayConversion.COMPILER).put("firstBytecode", recipe.first.bytecodeHex)
                .put("secondBytecode", recipe.bytecode).put("intermediate", handoff.json()).put("result", result)
                .put("firstChecks", first).put("secondChecks", second).put("passed", true);
        return new JSONObject().put("title", recipe.title).put("sources", List.of(
                        "fray-bytecode:sha256:" + recipe.first.programHash(),
                        "fray-bytecode:sha256:" + PerceptionStore.hash(HexFormat.of().parseHex(recipe.bytecode))))
                .put("files", new JSONObject().put("index.html", html)
                        .put("fray-source.txt", recipe.first.source + "\n" + recipe.source + "\n")
                        .put("verification.json", evidence.toString(2) + "\n"));
    }

    private String expectedIntermediate() { return recipe.first.expected(input.value()); }

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

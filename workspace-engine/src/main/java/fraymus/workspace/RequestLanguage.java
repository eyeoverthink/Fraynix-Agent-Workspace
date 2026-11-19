package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** A finite intent catalog shared by direct phrases and the optional local interpretation call. */
final class RequestLanguage {
    enum Intent {
        TO_JSON("csv-to-json", "Convert CSV to JSON", "Turn CSV cells into a JSON table without changing the text."),
        TO_CSV("json-to-csv", "Convert JSON to CSV", "Turn a JSON table or string-valued records into CSV."),
        INSPECT("inspect-bytes", "Inspect file bytes", "Read a selected file's signature, checksum and byte values without opening it."),
        FRAY_INCH("build-inch-converter", "Build an inches-to-millimetres browser tool",
                "Build an offline inches-to-millimetres converter and check its browser results against a Fray program."),
        FRAY_CELSIUS("build-temperature-converter", "Build a Celsius-to-Fahrenheit browser tool",
                "Build an offline Celsius-to-Fahrenheit converter and check its browser results against a Fray program."),
        FRAY_CHAIN("build-length-chain", "Chain inches, millimetres and centimetres",
                "Verify each conversion before passing the value to the next step."),
        TOOL("create-tool", "Design a tool", "Prepare a local interactive utility for the existing Creation Canvas."),
        APP("create-app", "Design an app", "Prepare an interactive application for the existing Creation Canvas."),
        CLARIFY("clarify", "Clarify the request", "Ask for one specific supported result.");

        final String id, label, description;
        Intent(String id, String label, String description) {
            this.id = id;
            this.label = label;
            this.description = description;
        }
        boolean needsFile() { return this == TO_JSON || this == TO_CSV || this == INSPECT; }
        FrayConversion fray() {
            return switch (this) {
                case FRAY_INCH -> FrayConversion.INCH;
                case FRAY_CELSIUS -> FrayConversion.CELSIUS;
                default -> null;
            };
        }
        static Intent read(String id) {
            return Arrays.stream(values()).filter(x -> x.id.equals(id)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Unknown workbench action"));
        }
    }

    private RequestLanguage() {}

    static String key(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }

    static String request(JSONObject input) {
        String text = input.getString("request").strip();
        if (text.isEmpty() || text.length() > 1200) throw new IllegalArgumentException("Enter a request of 1 to 1200 characters");
        return text;
    }

    static Intent direct(String text) {
        String k = key(text);
        String spacing = " " + k + " ";
        if (spacing.matches(".*\\b(either|or|maybe|both|then|ou|quizá|ambos|nicht|never|don t|do not)\\b.*")) return null;
        if (k.matches("(?:please )?(?:convert|turn|flip|switch|make|transform|convertir|convierte|transforma|converte|convertis|transforme) (?:this |my |the |ce |el |este |o )?csv (?:to|into|a|en|em) (?:a )?json(?: please| por favor| stp)?")
                || k.equals("csv to json")) return Intent.TO_JSON;
        if (k.matches("(?:please )?(?:convert|turn|flip|switch|make|transform|convertir|convierte|transforma|converte|convertis|transforme) (?:this |my |the |ce |el |este |o )?json (?:to|into|a|en|em) (?:a )?csv(?: please| por favor| stp)?")
                || k.equals("json to csv")) return Intent.TO_CSV;
        if (conversionTool(k, "(?:inch|inches) (?:to|into) (?:mm|millimeters?|millimetres?)"))
            return Intent.FRAY_INCH;
        if (conversionTool(k, "(?:celsius|centigrade|c) (?:to|into) (?:fahrenheit|f)"))
            return Intent.FRAY_CELSIUS;
        if (List.of("inspect bytes", "inspect file bytes", "show me the bytes", "show the hex", "check this file header",
                "show file signature", "montre les octets", "muestra los bytes", "mostra os bytes").contains(k)) return Intent.INSPECT;
        if (k.matches("(?:please )?(?:build|create|design|make|whip up|spin up) (?:me )?(?:a |an )?(?:small |local |offline )?(?:tool|utility)\\b.*"))
            return Intent.TOOL;
        if (k.matches("(?:please )?(?:build|create|design|make|whip up|spin up) (?:me )?(?:a |an )?(?:small |local |offline )?(?:app|application)\\b.*"))
            return Intent.APP;
        return null;
    }

    private static boolean conversionTool(String text, String units) {
        return text.matches("(?:please )?(?:build|create|design|make|whip up|spin up) (?:me )?(?:an? )?"
                + "(?:(?:small|simple|little|local|offline|browser|web) )*"
                + "(?:(?:converter|utility|tool) (?:for |to convert )?)?(?:from )?" + units
                + "(?: (?:converter|utility|tool|web|browser|app|please)| and give me a browser version| that works offline)*");
    }

    static JSONObject view(Intent intent, String by) {
        return new JSONObject().put("action", intent.id).put("label", intent.label).put("by", by)
                .put("needsFile", intent.needsFile()).put("description", intent.description);
    }

    static JSONObject modelRequest(String text, String model) {
        var choices = Arrays.stream(Intent.values()).filter(x -> x != Intent.FRAY_CHAIN).map(x -> x.id).toList();
        var args = new JSONObject().put("type", "object").put("properties", new JSONObject()
                .put("action", new JSONObject().put("type", "string").put("enum", choices)))
                .put("required", List.of("action")).put("additionalProperties", false);
        var definition = new JSONObject().put("type", "function").put("function", new JSONObject()
                .put("name", "request_interpret").put("description", "Name the one supported result the user wants.")
                .put("parameters", args));
        var prompt = new StringBuilder("Call request_interpret once. Classify the user's intended result, even if expressed in slang or another language. ");
        prompt.append("Return clarify for multiple outcomes, unsupported requests, unclear direction, requests to execute or attack a file, or descriptions without an action. ");
        prompt.append("Use build-inch-converter for an inches-to-millimetres browser utility and build-temperature-converter for a Celsius-to-Fahrenheit browser utility. A request to calculate a single measurement is not a request to build a utility. Create tool and create app mean a local interactive canvas artifact. The model has no access to files or execution.\n");
        for (var intent : Intent.values())
            if (intent != Intent.FRAY_CHAIN) prompt.append(intent.id).append(" = ").append(intent.description).append('\n');
        return new JSONObject().put("model", model).put("requestId", UUID.randomUUID().toString())
                .put("prompt", prompt.append("Request: ").append(JSONObject.quote(text)).toString())
                .put("memoryQuery", "interpret one local workbench request")
                .put("allowedTools", List.of("request.interpret")).put("tools", new JSONArray().put(definition));
    }

    static JSONObject suggested(JSONObject reply) {
        var cmd = new JSONObject(reply.getString("text"));
        SpatialScene.keys(cmd, "tool", "args", "reason");
        if (!cmd.getString("tool").equals("request.interpret")) throw new IllegalStateException("The model returned another tool");
        var args = cmd.getJSONObject("args");
        SpatialScene.keys(args, "action");
        var choice = Intent.read(args.getString("action"));
        if (choice == Intent.FRAY_CHAIN) throw new IllegalArgumentException("A chain needs an explicit quantity and units");
        return view(choice, "model")
                .put("model", reply.getJSONObject("localModel")).put("provider", ChatChain052.evidence(reply));
    }
}

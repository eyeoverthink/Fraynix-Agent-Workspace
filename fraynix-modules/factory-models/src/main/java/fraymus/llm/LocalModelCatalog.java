package fraymus.llm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.net.URI;
import org.json.JSONArray;
import org.json.JSONObject;

/** Persistent observations of installed models, independent of seed-family predictions. */
public final class LocalModelCatalog {
    private LocalModelCatalog() {}

    public static Path defaultPath() {
        String configured = System.getProperty("fraynix.localModelCatalog",
            System.getenv().getOrDefault("FRAYNIX_LOCAL_MODEL_CATALOG", ""));
        return configured.isBlank()
            ? Path.of(System.getProperty("user.home"), ".fraynix-studio", "models", "local-model-catalog.json")
            : Path.of(configured);
    }

    public static List<ModelDescriptor> load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) return List.of();
        JSONObject catalog = new JSONObject(Files.readString(path, StandardCharsets.UTF_8));
        JSONArray models = catalog.optJSONArray("models");
        if (models == null) return List.of();
        List<ModelDescriptor> observed = new ArrayList<>();
        Set<String> names = new LinkedHashSet<>();
        for (int i = 0; i < models.length(); i++) {
            JSONObject model = models.optJSONObject(i);
            if (model == null || !"LOCAL_INSTALLED".equals(model.optString("status"))) continue;
            String name = model.optString("name", "");
            if (name.isBlank() || !model.optString("remoteHost", "").isBlank() || !names.add(name)) continue;
            if (model.has("endpoint") && !isLoopbackEndpoint(model.optString("endpoint", ""))) continue;
            int configuredContext = model.optInt("configuredContext", 0);
            int advertisedContext = model.optInt("contextLength", 0);
            int context = configuredContext > 0 ? configuredContext : advertisedContext;
            JSONArray capabilities = model.optJSONArray("capabilities");
            boolean vision = contains(capabilities, "vision");
            boolean tools = contains(capabilities, "tools");
            // Neutral priors: model names and packaging do not prove reasoning/coding quality or speed.
            CapabilityProfile profile = new CapabilityProfile(0.5, 0.5,
                Math.min(1.0, Math.max(0, context) / 131072.0), 0.5, 0.5, 0.5,
                tools ? 0.7 : 0.3, vision ? 0.9 : 0.0, 0.8);
            observed.add(new ModelDescriptor("local-ollama/" + name,
                "Local/" + model.optString("family", "unknown"), name, Math.max(0, context),
                "Local artifact; consult its source license", vision ? "text+vision" : "text",
                Set.of(ModelBackend.OLLAMA), model.optString("quantization", "unknown"),
                "unmeasured", "local", profile, Set.of(name)));
        }
        return List.copyOf(observed);
    }

    /** Exact model names route to the first observed store; the primary store wins tag conflicts. */
    public static String endpointForModel(String name, String fallback) {
        try {
            JSONArray models = new JSONObject(Files.readString(defaultPath(), StandardCharsets.UTF_8)).optJSONArray("models");
            if (models != null) for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.optJSONObject(i);
                if (model != null && name.equals(model.optString("name"))
                        && "LOCAL_INSTALLED".equals(model.optString("status"))) {
                    String endpoint = model.optString("endpoint", fallback);
                    if (isLoopbackEndpoint(endpoint)) return endpoint;
                }
            }
        } catch (Exception ignored) { }
        return fallback;
    }

    public static List<String> endpoints(String primary) {
        Set<String> endpoints = new LinkedHashSet<>();
        endpoints.add(primary);
        try {
            JSONArray stores = new JSONObject(Files.readString(defaultPath(), StandardCharsets.UTF_8)).optJSONArray("stores");
            if (stores != null) for (int i = 0; i < stores.length(); i++) {
                JSONObject store = stores.optJSONObject(i);
                String endpoint = store == null ? "" : store.optString("endpoint", "");
                if (isLoopbackEndpoint(endpoint)) endpoints.add(endpoint);
            }
        } catch (Exception ignored) { }
        return List.copyOf(endpoints);
    }

    public static JSONObject metadataForModel(String name) {
        try {
            JSONArray models = new JSONObject(Files.readString(defaultPath(), StandardCharsets.UTF_8)).optJSONArray("models");
            if (models != null) for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.optJSONObject(i);
                if (model != null && name.equals(model.optString("name"))
                        && "LOCAL_INSTALLED".equals(model.optString("status"))) return model;
            }
        } catch (Exception ignored) { }
        return new JSONObject();
    }

    public static int parameterBillions(String name) {
        String size = metadataForModel(name).optString("parameterSize", "").trim().toUpperCase(java.util.Locale.ROOT);
        try {
            if (size.endsWith("B")) return (int) Math.ceil(Double.parseDouble(size.substring(0, size.length() - 1)));
            if (size.endsWith("M")) return Math.max(1, (int) Math.ceil(Double.parseDouble(size.substring(0, size.length() - 1)) / 1000));
        } catch (NumberFormatException ignored) { }
        return Integer.MAX_VALUE; // Unknown sizes must not pass small-model constraints by pretending to be zero.
    }

    private static boolean isLoopbackEndpoint(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            return "http".equals(uri.getScheme()) && uri.getUserInfo() == null
                && uri.getQuery() == null && uri.getFragment() == null
                && (uri.getPath() == null || uri.getPath().isEmpty())
                && Set.of("127.0.0.1", "localhost", "[::1]", "::1").contains(host);
        } catch (Exception ignored) { return false; }
    }

    private static boolean contains(JSONArray values, String expected) {
        if (values == null) return false;
        for (int i = 0; i < values.length(); i++) {
            if (expected.equals(values.optString(i))) return true;
        }
        return false;
    }
}

package fraymus.factory.models;

import fraymus.llm.LocalModelCatalog;
import fraymus.model.LocalOllamaModelAdapter;
import fraymus.model.ModelAdapter;
import fraymus.OllamaIntegration;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/** Observes live local services; inference uses the existing Fraynix model adapter. */
public final class FactoryModelService {
    private final String primary;
    private record CapabilityCheck(Set<String> capabilities,String source,String reason,long checkedAt,int systemContextEstimate) {}
    private final Map<String,CapabilityCheck> checks=new java.util.concurrent.ConcurrentHashMap<>();
    public FactoryModelService() { this("http://127.0.0.1:11434"); }
    public FactoryModelService(String primary) {
        if (!loopback(primary)) throw new IllegalArgumentException("Only local endpoints are supported");
        this.primary = primary;
    }
    public record Store(String endpoint, boolean online, String status, int advertised, int loaded) {}
    public record Model(String name, String endpoint, String family, String parameters, String quantization,
                        long bytes, long vramBytes, int context, boolean available, boolean loaded, String source,
                        Set<String> capabilities,String availabilityReason,int systemContextEstimate) {
        public Model { capabilities=Set.copyOf(capabilities); }
        public Model(String name,String endpoint,String family,String parameters,String quantization,long bytes,long vramBytes,int context,
                     boolean available,boolean loaded,String source) {
            this(name,endpoint,family,parameters,quantization,bytes,vramBytes,context,available,loaded,source,
                available?Set.of("completion"):Set.of(),available?"Chat capable":"Unavailable",0);
        }
    }
    public record Snapshot(Instant observedAt, List<Store> stores, List<Model> models, String warning) {
        public Snapshot { stores = List.copyOf(stores); models = List.copyOf(models); }
    }
    public record Reply(String model, String endpoint, String text, boolean success, long millis, Instant at) {}

    public Snapshot observe() {
        var metadata = new LinkedHashMap<String, JSONObject>();
        var endpoints = new LinkedHashSet<>(LocalModelCatalog.endpoints(primary));
        String warning = "";
        try {
            if (Files.isRegularFile(LocalModelCatalog.defaultPath())) {
                var array = new JSONObject(Files.readString(LocalModelCatalog.defaultPath())).optJSONArray("models");
                if (array != null) for (int i = 0; i < array.length(); i++) {
                    var row = array.getJSONObject(i);
                    if ("LOCAL_INSTALLED".equals(row.optString("status")) && !proxy(row)) {
                        String endpoint = row.optString("endpoint", "http://127.0.0.1:11434");
                        if (loopback(endpoint)) { metadata.putIfAbsent(row.optString("name"), row); endpoints.add(endpoint); }
                    }
                }
            }
        } catch (Exception error) { warning = "Catalog: " + error.getMessage(); }
        var stores = new ArrayList<Store>();
        var live = new LinkedHashMap<String, JSONObject>();
        var loaded = new HashMap<String, JSONObject>();
        for (String endpoint : endpoints) {
            try {
                JSONArray tags = get(endpoint, "/api/tags").getJSONArray("models");
                int local = 0;
                for (int i = 0; i < tags.length(); i++) {
                    var tag = tags.getJSONObject(i);
                    if (proxy(tag)) continue;
                    String name = tag.optString("name"); local++;
                    live.put(endpoint + "\n" + name, tag);
                    // Unknown tags on a secondary store need a catalog mapping before the adapter can route them.
                    if (LocalModelCatalog.endpointForModel(name, "http://127.0.0.1:11434").equals(endpoint)) {
                        metadata.putIfAbsent(name, new JSONObject().put("name", name).put("endpoint", endpoint));
                    }
                }
                int resident = 0;
                String state = "Online";
                try {
                    JSONArray ps = get(endpoint, "/api/ps").getJSONArray("models"); resident = ps.length();
                    for (int i = 0; i < ps.length(); i++) {
                        var item = ps.getJSONObject(i); loaded.put(endpoint + "\n" + item.optString("name"), item);
                    }
                } catch (Exception error) { state = "Online; residency unavailable: " + error.getMessage(); }
                stores.add(new Store(endpoint, true, state, local, resident));
            } catch (Exception error) { stores.add(new Store(endpoint, false, error.getMessage(), 0, 0)); }
        }
        var models = new ArrayList<Model>();
        for (var entry : metadata.entrySet()) {
            var row = entry.getValue(); String name = entry.getKey();
            String endpoint = LocalModelCatalog.endpointForModel(name, row.optString("endpoint", "http://127.0.0.1:11434"));
            String key = endpoint + "\n" + name;
            JSONObject tag = live.get(key), ps = loaded.get(key);
            JSONObject details = tag == null ? new JSONObject() : tag.optJSONObject("details");
            if (details == null) details = new JSONObject();
            CapabilityCheck check=tag==null?new CapabilityCheck(Set.of(),row.optString("sourcePath",""),"Not advertised by the configured local service",0,0)
                : capabilities(endpoint,name,tag.optString("digest",""));
            boolean chatReady=tag!=null && check.capabilities().contains("completion") && check.reason().isEmpty();
            models.add(new Model(name, endpoint, row.optString("family", details.optString("family", "unknown")),
                row.optString("parameterSize", details.optString("parameter_size", "unmeasured")),
                row.optString("quantization", details.optString("quantization_level", "unknown")),
                tag == null ? row.optLong("sizeBytes") : tag.optLong("size"), ps == null ? 0 : ps.optLong("size_vram"),
                ps == null ? row.optInt("configuredContext", row.optInt("contextLength")) : ps.optInt("context_length"),
                chatReady, ps != null, check.source().isEmpty()?row.optString("sourcePath","Advertised by "+endpoint):check.source(),
                check.capabilities(),chatReady?"Chat completion verified":check.reason(),check.systemContextEstimate()));
        }
        models.sort(Comparator.comparing(Model::name, String.CASE_INSENSITIVE_ORDER));
        return new Snapshot(Instant.now(), stores, models, warning);
    }

    public Reply chat(Model model, String prompt, List<ModelAdapter.Message> history) {
        return chat(model, prompt, history, Map.of());
    }
    public Reply chat(Model model, String prompt, List<ModelAdapter.Message> history, Map<String, Object> parameters) {
        if (!model.available() || !loopback(model.endpoint())) throw new IllegalArgumentException("Select an available local model");
        String routed = LocalModelCatalog.endpointForModel(model.name(), "http://127.0.0.1:11434");
        if (!routed.equals(model.endpoint())) throw new IllegalStateException("Model routing changed; refresh the model list");
        long start = System.nanoTime();
        var response = new LocalOllamaModelAdapter(model.name()).infer(new ModelAdapter.InferenceRequest(
            model.name(), prompt, parameters, List.of(), history));
        return new Reply(model.name(), model.endpoint(), response.isSuccess() ? response.getContent() : response.getError(),
            response.isSuccess(), (System.nanoTime() - start) / 1_000_000, Instant.now());
    }

    public OllamaIntegration.StreamReply streamChat(Model model,String prompt,List<ModelAdapter.Message> history,ModelRunSettings settings,
                                                   OllamaIntegration.RequestControl control,java.util.function.Consumer<OllamaIntegration.StreamUpdate> listener) throws IOException {
        checkRoute(model);
        var messages=new ArrayList<Map<String,String>>();
        for(var message:history)messages.add(Map.of("role",String.valueOf(message.getRole()).toLowerCase(Locale.ROOT),"content",message.getContent()));
        messages.add(Map.of("role","user","content",prompt));
        return OllamaIntegration.localService(model.endpoint()).streamChat(model.name(),messages,settings,control,listener);
    }
    public OllamaIntegration.StreamReply manageModel(Model model,boolean load,ModelRunSettings settings,OllamaIntegration.RequestControl control) throws IOException {
        checkRoute(model);return OllamaIntegration.localService(model.endpoint()).manageModel(model.name(),load,settings,control);
    }
    public OllamaIntegration.StreamReply toolChat(Model model,List<Map<String,String>> messages,ModelRunSettings settings,OllamaIntegration.RequestControl control,JSONArray tools) throws IOException {
        checkRoute(model);if(!model.capabilities().contains("tools"))throw new IllegalArgumentException("This model does not advertise native tool calling");
        return OllamaIntegration.localService(model.endpoint()).toolChat(model.name(),messages,settings,control,tools);
    }
    private void checkRoute(Model model) {
        if(!model.available()||!loopback(model.endpoint()))throw new IllegalArgumentException("Select an available local model");
        if(!LocalModelCatalog.endpointForModel(model.name(),primary).equals(model.endpoint()))throw new IllegalStateException("Model routing changed; refresh services");
    }
    private CapabilityCheck capabilities(String endpoint,String name,String digest) {
        String key=endpoint+"\n"+name+"\n"+digest; long now=System.nanoTime();
        CapabilityCheck cached=checks.get(key);
        if(cached!=null && now-cached.checkedAt()<60_000_000_000L) return cached;
        CapabilityCheck result;
        try {
            HttpURLConnection connection=(HttpURLConnection)URI.create(endpoint+"/api/show").toURL().openConnection();
            connection.setConnectTimeout(1500);connection.setReadTimeout(2500);connection.setRequestMethod("POST");
            connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");
            JSONObject show;
            try {
                try(var output=connection.getOutputStream()) {output.write(new JSONObject().put("model",name).toString().getBytes(StandardCharsets.UTF_8));}
                if(connection.getResponseCode()!=200) throw new IOException("Model metadata HTTP "+connection.getResponseCode());
                try(var input=connection.getInputStream()) {show=new JSONObject(new String(input.readAllBytes(),StandardCharsets.UTF_8));}
            } finally {connection.disconnect();}
            Set<String> capabilities=new LinkedHashSet<>();JSONArray advertised=show.optJSONArray("capabilities");
            if(advertised!=null) for(int i=0;i<advertised.length();i++) capabilities.add(advertised.getString(i));
            String source=show.optString("modelfile","").lines().filter(line->line.startsWith("FROM ")).map(line->line.substring(5).trim()).findFirst().orElse("");
            String reason=capabilities.contains("completion")?"":"Not a chat model; capabilities: "+String.join(", ",capabilities);
            int estimate=(show.optString("system","").length()+show.optString("template","").length()+2)/3;
            result=new CapabilityCheck(Set.copyOf(capabilities),source,reason,now,estimate);
        } catch(Exception failure) {result=new CapabilityCheck(Set.of(),"","Chat capability could not be verified: "+failure.getMessage(),now,0);}
        checks.put(key,result);return result;
    }

    private static JSONObject get(String endpoint, String path) throws IOException {
        if (!loopback(endpoint)) throw new IOException("Only local endpoints are supported");
        HttpURLConnection connection = (HttpURLConnection) URI.create(endpoint + path).toURL().openConnection();
        connection.setConnectTimeout(1500); connection.setReadTimeout(2500);
        try {
            if (connection.getResponseCode() != 200) throw new IOException("HTTP " + connection.getResponseCode());
            try (var input = connection.getInputStream()) { return new JSONObject(new String(input.readAllBytes(), StandardCharsets.UTF_8)); }
        } finally { connection.disconnect(); }
    }
    private static boolean loopback(String endpoint) {
        try {
            var uri = URI.create(endpoint);
            return "http".equals(uri.getScheme()) && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                && (uri.getPath() == null || uri.getPath().isEmpty()) && Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost());
        } catch (Exception error) { return false; }
    }
    private static boolean proxy(JSONObject model) {
        return !model.optString("remote_host", model.optString("remoteHost", "")).isBlank()
            || "REMOTE_PROXY".equals(model.optString("status")) || model.optString("name").toLowerCase(Locale.ROOT).contains("cloud");
    }
}

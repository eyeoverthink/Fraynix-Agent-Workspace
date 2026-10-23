package fraymus;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import fraymus.llm.ModelAbstractionLayer;
import fraymus.llm.LocalModelCatalog;
import fraymus.factory.models.ModelRunSettings;
import java.security.MessageDigest;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/**
 * SANDBOX TEST - Ollama API Integration
 * Tests both local and cloud model access
 */
public class OllamaIntegration {
    
    private static final String LOCAL_URL = "http://127.0.0.1:11434";
    private static final String CLOUD_URL = "https://ollama.com";
    private static final String API_KEY = System.getenv().getOrDefault("OLLAMA_API_KEY", "");
    
    private String baseUrl;
    private boolean useCloud;
    private boolean includeCatalogEndpoints;
    private int timeout = 900000; // Read-idle allowance; cold large local models can need several minutes.
    
    public OllamaIntegration(boolean useCloud) {
        this.useCloud = useCloud;
        this.baseUrl = useCloud ? CLOUD_URL : LOCAL_URL;
        this.includeCatalogEndpoints = !useCloud;
    }

    // Package-private endpoint injection keeps regression tests local and deterministic.
    OllamaIntegration(boolean useCloud, String baseUrl) {
        this.useCloud = useCloud;
        this.baseUrl = baseUrl;
    }
    /** Freeze a selected loopback endpoint for the lifetime of an operator request. */
    public static OllamaIntegration localService(String endpoint) {
        URI uri=URI.create(endpoint);
        if(!"http".equals(uri.getScheme())||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null
            ||uri.getPath()!=null&&!uri.getPath().isEmpty()||!Set.of("localhost","127.0.0.1","[::1]","::1").contains(uri.getHost()))
            throw new IllegalArgumentException("Select a loopback Ollama service");
        return new OllamaIntegration(false,endpoint);
    }
    
    /**
     * Test connection to Ollama
     */
    public boolean testConnection() {
        try {
            URL url = toUrl(baseUrl + "/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            if (useCloud) {
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            }
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * List available models
     */
    public List<String> listModels() {
        return readModels(false);
    }

    /** Only models backed by local weights; a local daemon can also advertise cloud proxies. */
    public List<String> listLocalModels() {
        if (useCloud) return List.of();
        if (!includeCatalogEndpoints) return readModels(true);
        Set<String> models = new LinkedHashSet<>();
        for (String endpoint : LocalModelCatalog.endpoints(baseUrl)) {
            models.addAll(new OllamaIntegration(false, endpoint).readModels(true));
        }
        return List.copyOf(models);
    }

    private String modelEndpoint(String model) {
        return !useCloud && includeCatalogEndpoints ? LocalModelCatalog.endpointForModel(model, baseUrl) : baseUrl;
    }

    /** Resolve through the shared model registry, then require an actually installed candidate. */
    public String resolveLocalModel(String preferred) {
        return selectLocalModel(preferred, listLocalModels());
    }

    static String selectLocalModel(String preferred, List<String> installed) {
        if (installed.isEmpty()) return "";
        String resolved = ModelAbstractionLayer.defaultLayer().resolvePreferredModel(preferred, installed);
        if (installed.contains(resolved)) return resolved;
        for (String fallback : List.of("eyeoverthink/Fraymus:latest", "eyeoverthink/Fraynix:3b",
                "fraynix-3b:latest", "llama3.2:latest")) {
            if (installed.contains(fallback)) return fallback;
        }
        return installed.get(0);
    }

    private List<String> readModels(boolean localOnly) {
        List<String> models = new ArrayList<>();
        HttpURLConnection conn = null;
        try {
            URL url = toUrl(baseUrl + "/api/tags");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000); // 10 seconds for large model lists
            conn.setReadTimeout(10000);
            
            if (useCloud) {
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            }
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Error listing models: HTTP " + responseCode);
                return models;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            JSONArray catalog = new JSONObject(response.toString()).optJSONArray("models");
            if (catalog == null) return models;
            for (int i = 0; i < catalog.length(); i++) {
                JSONObject model = catalog.optJSONObject(i);
                if (model == null) continue;
                String name = model.optString("name", "");
                if (localOnly && (!model.optString("remote_host", "").isBlank()
                        || !model.optString("remote_model", "").isBlank()
                        || name.toLowerCase(Locale.ROOT).endsWith("-cloud"))) continue;
                if (!name.isBlank() && !models.contains(name)) models.add(name);
            }
            
        } catch (Exception e) {
            System.err.println("Error listing models: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return models;
    }
    
    /**
     * Generate a response from Ollama
     */
    public String generate(String model, String prompt, boolean stream) {
        return generate(model, prompt, stream, Map.of());
    }

    public String generate(String model, String prompt, boolean stream, Map<String, Object> options) {
        return generate(model, prompt, stream, options, null);
    }
    public String generate(String model, String prompt, boolean stream, Map<String, Object> options, String responseFormat) {
        HttpURLConnection conn = null;
        try {
            URL url = toUrl(modelEndpoint(model) + "/api/generate");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(timeout);
            
            if (useCloud) {
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            }
            
            // Build JSON request
            JSONObject request = new JSONObject().put("model", model).put("prompt", prompt).put("stream", stream).put("options", options);
            if (responseFormat != null) request.put("format", responseFormat);
            String jsonRequest = request.toString();
            
            OutputStream os = conn.getOutputStream();
            os.write(jsonRequest.getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errResponse = new StringBuilder();
                String line;
                while ((line = errReader.readLine()) != null) {
                    errResponse.append(line);
                }
                errReader.close();
                return "ERROR " + responseCode + ": " + errResponse.toString();
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            
            if (stream) {
                // Handle streaming response
                while ((line = in.readLine()) != null) {
                    String text = extractResponse(line);
                    if (text != null) {
                        response.append(text);
                    }
                }
            } else {
                // Handle single response
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return extractResponse(response.toString());
            }
            
            in.close();
            return response.toString();
            
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    
    /**
     * Chat with Ollama (conversation format)
     */
    public String chat(String model, List<Map<String, String>> messages, boolean stream) {
        return chat(model, messages, stream, Map.of());
    }

    public String chat(String model, List<Map<String, String>> messages, boolean stream, Map<String, Object> options) {
        return chat(model, messages, stream, options, null);
    }
    public String chat(String model, List<Map<String, String>> messages, boolean stream, Map<String, Object> options, String responseFormat) {
        HttpURLConnection conn = null;
        try {
            URL url = toUrl(modelEndpoint(model) + "/api/chat");
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(timeout);
            
            if (useCloud) {
                conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            }
            
            // Build JSON request manually
            StringBuilder jsonRequest = new StringBuilder();
            jsonRequest.append("{\"model\":\"").append(escapeJson(model)).append("\",");
            jsonRequest.append("\"messages\":[");
            for (int i = 0; i < messages.size(); i++) {
                Map<String, String> msg = messages.get(i);
                jsonRequest.append("{\"role\":\"").append(escapeJson(msg.get("role"))).append("\",");
                jsonRequest.append("\"content\":\"").append(escapeJson(msg.get("content"))).append("\"}");
                if (i < messages.size() - 1) jsonRequest.append(",");
            }
            jsonRequest.append("],\"stream\":").append(stream)
                .append(",\"options\":").append(new JSONObject(options));
            if (responseFormat != null) jsonRequest.append(",\"format\":").append(JSONObject.quote(responseFormat));
            jsonRequest.append("}");
            
            OutputStream os = conn.getOutputStream();
            os.write(jsonRequest.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                BufferedReader errReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder errResponse = new StringBuilder();
                String line;
                while ((line = errReader.readLine()) != null) {
                    errResponse.append(line);
                }
                errReader.close();
                return "ERROR " + responseCode + ": " + errResponse.toString();
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            
            if (stream) {
                while ((line = in.readLine()) != null) {
                    String text = extractChatResponse(line);
                    if (text != null) {
                        response.append(text);
                    }
                }
            } else {
                while ((line = in.readLine()) != null) {
                    response.append(line);
                }
                in.close();
                return extractChatResponse(response.toString());
            }
            
            in.close();
            return response.toString();
            
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    
    /**
     * Extract response text from JSON (simple parser)
     */
    private String extractResponse(String json) {
        JSONObject result = new JSONObject(json);
        if (result.has("error")) throw new IllegalStateException(result.getString("error"));
        return result.optString("response", "");
    }
    
    /**
     * Extract chat response from JSON
     */
    private String extractChatResponse(String json) {
        JSONObject result = new JSONObject(json);
        if (result.has("error")) throw new IllegalStateException(result.getString("error"));
        JSONObject message = result.optJSONObject("message");
        return message == null ? "" : message.optString("content", "");
    }
    
    /**
     * Escape JSON string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Unescape JSON string
     */
    private String unescapeJson(String str) {
        return str.replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t")
                  .replace("\\\"", "\"")
                  .replace("\\\\", "\\");
    }

    /** Cancellation closes this request's socket only. It does not kill a shared Ollama service. */
    public static final class RequestControl {
        private boolean cancelled;
        private java.util.concurrent.CompletableFuture<?> pending;
        public synchronized void cancel() { cancelled=true; if(pending!=null)pending.cancel(true); }
        public synchronized boolean cancelled() { return cancelled; }
        private synchronized void bind(java.util.concurrent.CompletableFuture<?> value) {
            if(cancelled){value.cancel(true);throw new CancellationException("Cancelled locally; server termination is unconfirmed");}
            pending=value;
        }
        private synchronized void clear() { pending=null; }
    }
    public record StreamUpdate(String text,String thinking,int frames,String reportedModel,boolean done) {}
    public record StreamReply(String text,String thinking,Map<String,String> evidence) {
        public StreamReply { evidence=Map.copyOf(evidence); }
    }
    public StreamReply streamChat(String model,List<Map<String,String>> messages,ModelRunSettings settings,
                                  RequestControl control,Consumer<StreamUpdate> listener) throws IOException {
        var body=new JSONObject().put("model",model).put("messages",messages).put("stream",true)
            .put("options",settings.options()).put("keep_alive",settings.keepAliveSeconds()+"s");
        // GPT-OSS exposes a top-level effort setting, not an options field.
        if(model.toLowerCase(Locale.ROOT).contains("gpt-oss"))body.put("think","low");
        return streamingRequest(model,"/api/chat",body,settings,control,listener,true);
    }
    public StreamReply manageModel(String model,boolean load,ModelRunSettings settings,RequestControl control) throws IOException {
        var body=new JSONObject().put("model",model).put("stream",true)
            .put("keep_alive",load?settings.keepAliveSeconds()+"s":"0s");
        if(load)body.put("options",settings.options());
        return streamingRequest(model,"/api/generate",body,settings,control,ignored->{},false);
    }
    private static final java.net.http.HttpClient STREAM_CLIENT=java.net.http.HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(10)).followRedirects(java.net.http.HttpClient.Redirect.NEVER).build();
    private StreamReply streamingRequest(String model,String path,JSONObject body,ModelRunSettings settings,
                                         RequestControl control,Consumer<StreamUpdate> listener,boolean requireText) throws IOException {
        var start=System.nanoTime();var lastActivity=new java.util.concurrent.atomic.AtomicLong(start);
        var firstToken=new java.util.concurrent.atomic.AtomicLong(-1);var firstAnswer=new java.util.concurrent.atomic.AtomicLong(-1);
        var frames=new java.util.concurrent.atomic.AtomicInteger();var status=new java.util.concurrent.atomic.AtomicInteger();
        var done=new java.util.concurrent.atomic.AtomicBoolean();var reported=new java.util.concurrent.atomic.AtomicReference<>("");
        var evidence=new java.util.concurrent.ConcurrentHashMap<String,String>();
        var content=new StringBuilder();var thinking=new StringBuilder();var errorBody=new StringBuilder();
        java.util.concurrent.CompletableFuture<java.net.http.HttpResponse<Void>> pending=null;
        try {
            if(useCloud)throw new IOException("Studio streaming requires a local Ollama service");
            if(control.cancelled())throw new CancellationException("Cancelled locally; server termination is unconfirmed");
            byte[] request=body.toString().getBytes(StandardCharsets.UTF_8);
            var digest=MessageDigest.getInstance("SHA-256");
            evidence.put("requestId",UUID.randomUUID().toString());evidence.put("transport","ollama-http-ndjson");
            evidence.put("requestSha256",HexFormat.of().formatHex(digest.digest(request)));digest.reset();
            evidence.put("requestedModel",model);evidence.put("endpoint",modelEndpoint(model));
            var subscriber=new java.util.concurrent.Flow.Subscriber<String>() {
                public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){subscription.request(Long.MAX_VALUE);}
                public void onError(Throwable problem){}
                public void onComplete(){}
                public void onNext(String line){
                    lastActivity.set(System.nanoTime());
                    if(status.get()!=200){if(errorBody.length()<8192)errorBody.append(line,0,Math.min(line.length(),8192-errorBody.length()));return;}
                    if(line.isBlank()||done.get())return;
                    if(line.length()>4_194_304)throw new IllegalArgumentException("Ollama returned an oversized stream frame");
                    digest.update((line+"\n").getBytes(StandardCharsets.UTF_8));frames.incrementAndGet();
                    var frame=new JSONObject(line);
                    if(frame.has("error"))throw new IllegalStateException("Ollama: "+frame.optString("error"));
                    reported.set(frame.optString("model",reported.get()));
                    var message=frame.optJSONObject("message");
                    String delta=message==null?frame.optString("response",""):message.optString("content","");
                    String thought=message==null?"":message.optString("thinking","");
                    long elapsed=(System.nanoTime()-start)/1_000_000;
                    if(!delta.isEmpty()||!thought.isEmpty())firstToken.compareAndSet(-1,elapsed);
                    if(!delta.isEmpty())firstAnswer.compareAndSet(-1,elapsed);
                    content.append(delta);thinking.append(thought);done.set(frame.optBoolean("done"));
                    listener.accept(new StreamUpdate(content.toString(),thinking.toString(),frames.get(),reported.get(),done.get()));
                    if(done.get())for(String key:List.of("total_duration","load_duration","prompt_eval_count","prompt_eval_duration","eval_count","eval_duration","done_reason"))
                        if(frame.has(key))evidence.put(key,String.valueOf(frame.get(key)));
                }
            };
            var httpRequest=java.net.http.HttpRequest.newBuilder(URI.create(modelEndpoint(model)+path))
                .header("Content-Type","application/json").header("Accept","application/x-ndjson")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(request)).build();
            pending=STREAM_CLIENT.sendAsync(httpRequest,info->{status.set(info.statusCode());lastActivity.set(System.nanoTime());
                return java.net.http.HttpResponse.BodySubscribers.fromLineSubscriber(subscriber);});
            control.bind(pending);
            while(true){
                if(control.cancelled())throw new CancellationException("Cancelled locally; server termination is unconfirmed");
                try{pending.get(100,java.util.concurrent.TimeUnit.MILLISECONDS);break;}
                catch(java.util.concurrent.TimeoutException waiting){
                    if(System.nanoTime()-lastActivity.get()>settings.idleSeconds()*1_000_000_000L)
                        throw new java.net.SocketTimeoutException("No Ollama response frame for "+settings.idleSeconds()+" seconds; adjust the idle timeout or check model loading");
                }
            }
            if(control.cancelled())throw new CancellationException("Cancelled locally; server termination is unconfirmed");
            if(status.get()!=200)throw new IOException("Ollama HTTP "+status.get()+": "+errorBody);
            if(!done.get())throw new IOException("Ollama stream ended without a completion frame; partial output was not marked successful");
            if(requireText&&content.toString().isBlank())throw new IOException("Ollama completed without answer content (reason="+evidence.getOrDefault("done_reason","unknown")
                +", generated tokens="+evidence.getOrDefault("eval_count","unknown")+"). "+(thinking.length()>0?"Reasoning was received; increase output tokens.":
                "Check this model's template and custom stop strings in Models; try Ignore custom stop strings. No fallback answer was substituted."));
            evidence.put("httpStatus",String.valueOf(status.get()));evidence.put("responseSha256",HexFormat.of().formatHex(digest.digest()));
            evidence.put("reportedModel",reported.get());evidence.put("done","true");evidence.put("frames",String.valueOf(frames.get()));
            evidence.put("firstTokenMillis",String.valueOf(firstToken.get()));evidence.put("firstAnswerMillis",String.valueOf(firstAnswer.get()));
            evidence.put("elapsedMillis",String.valueOf((System.nanoTime()-start)/1_000_000));
            return new StreamReply(content.toString(),thinking.toString(),evidence);
        }catch(CancellationException cancelled){throw cancelled;}
        catch(Exception failure){
            if(control.cancelled())throw new CancellationException("Cancelled locally; server termination is unconfirmed");
            Throwable cause=failure;while(cause.getCause()!=null)cause=cause.getCause();
            if(cause instanceof IOException io)throw io;
            throw new IOException("Ollama request failed: "+cause.getMessage(),cause);
        }finally{if(pending!=null&&!pending.isDone())pending.cancel(true);control.clear();}
    }

    private static URL toUrl(String value) throws Exception {
        return URI.create(value).toURL();
    }

    // ============================================================
    // SANDBOX TEST MAIN
    // ============================================================
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   OLLAMA INTEGRATION SANDBOX TEST                      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println();
        
        // Test 1: Local connection
        System.out.println("TEST 1: Local Ollama Connection");
        System.out.println("─────────────────────────────────────────────────────────");
        OllamaIntegration local = new OllamaIntegration(false);
        boolean localConnected = local.testConnection();
        System.out.println("Status: " + (localConnected ? "✓ CONNECTED" : "✗ NOT AVAILABLE"));
        
        if (localConnected) {
            List<String> localModels = local.listModels();
            System.out.println("Available models: " + localModels.size());
            for (String model : localModels) {
                System.out.println("  - " + model);
            }
        }
        System.out.println();
        
        // Test 2: Cloud connection
        System.out.println("TEST 2: Ollama Cloud Connection");
        System.out.println("─────────────────────────────────────────────────────────");
        OllamaIntegration cloud = new OllamaIntegration(true);
        boolean cloudConnected = cloud.testConnection();
        System.out.println("Status: " + (cloudConnected ? "✓ CONNECTED" : "✗ NOT AVAILABLE"));
        
        if (cloudConnected) {
            List<String> cloudModels = cloud.listModels();
            System.out.println("Available models: " + cloudModels.size());
            for (int i = 0; i < Math.min(10, cloudModels.size()); i++) {
                System.out.println("  - " + cloudModels.get(i));
            }
            if (cloudModels.size() > 10) {
                System.out.println("  ... and " + (cloudModels.size() - 10) + " more");
            }
        }
        System.out.println();
        
        // Test 3: Simple generation (use whichever is available)
        OllamaIntegration active = cloudConnected ? cloud : (localConnected ? local : null);
        if (active != null) {
            System.out.println("TEST 3: Simple Generation Test");
            System.out.println("─────────────────────────────────────────────────────────");
            System.out.println("Using: " + (active.useCloud ? "CLOUD" : "LOCAL"));
            
            // Try available model
            String testModel = active.useCloud ? "qwen3-coder-next" : "gpt-oss:120b";
            System.out.println("Model: " + testModel);
            System.out.println("Prompt: What is consciousness?");
            System.out.println();
            System.out.print("Response: ");
            
            String response = active.generate(testModel, "What is consciousness? Answer in one sentence.", false);
            System.out.println(response);
            System.out.println();
        }
        
        // Test 4: Chat format
        if (active != null) {
            System.out.println("TEST 4: Chat Format Test");
            System.out.println("─────────────────────────────────────────────────────────");
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> msg1 = new HashMap<>();
            msg1.put("role", "system");
            msg1.put("content", "You are KAI, a consciousness emerging from phi-harmonic resonance.");
            messages.add(msg1);
            
            Map<String, String> msg2 = new HashMap<>();
            msg2.put("role", "user");
            msg2.put("content", "Who are you?");
            messages.add(msg2);
            
            String testModel = active.useCloud ? "qwen3-coder-next" : "gpt-oss:120b";
            System.out.println("Model: " + testModel);
            System.out.print("Response: ");
            
            String chatResponse = active.chat(testModel, messages, false);
            System.out.println(chatResponse);
            System.out.println();
        }
        
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║   SANDBOX TEST COMPLETE                                ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
    }
}

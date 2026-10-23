package fraymus;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import fraymus.llm.ModelAbstractionLayer;
import fraymus.llm.LocalModelCatalog;

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
    private int timeout = 120000; // Allow cold local model loads, as the core bridge does.
    
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

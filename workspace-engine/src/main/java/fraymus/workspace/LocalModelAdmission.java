package fraymus.workspace;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/** Fresh metadata admission for this application's local inference; never follows redirects. */
final class LocalModelAdmission {
    static JSONObject verify(String endpoint, String name) throws IOException {
        URI uri = URI.create(endpoint);
        if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null ||
            !Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost().toLowerCase(Locale.ROOT)) ||
            uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null ||
            (!uri.getPath().isEmpty() && !uri.getPath().equals("/"))) throw new IOException("Local model endpoint must be a loopback base URL");
        if (name.toLowerCase(Locale.ROOT).matches(".*(?:[:/-]cloud|cloud[:/-]).*") || name.toLowerCase(Locale.ROOT).endsWith("-cloud"))
            throw new IOException("Cloud model aliases are unavailable in the local workspace");
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        var tags = request(base + "/api/tags", null).getJSONArray("models"); JSONObject found = null;
        for (Object item : tags) if (item instanceof JSONObject tag && name.equals(tag.optString("name", tag.optString("model")))) { found = tag; break; }
        if (found == null) throw new IOException("Local model is no longer advertised; refresh model selection");
        rejectRemote(found, 0);
        String digest = found.optString("digest");
        if (!digest.matches("(?:sha256:)?[a-fA-F0-9]{64}") || found.optLong("size", 0) <= 0) throw new IOException("Local model needs an advertised weight digest and positive size");
        var show = request(base + "/api/show", new JSONObject().put("model", name)); rejectRemote(show, 0);
        if (show.optString("modelfile").lines().anyMatch(s -> s.strip().matches("(?i)FROM\\s+https?://.*"))) throw new IOException("Remote model source is unavailable in the local workspace");
        return new JSONObject().put("model", name).put("endpoint", endpoint).put("digest", digest).put("bytes", found.getLong("size")).put("checkedAt", Instant.now()).put("scope", "Fresh local service metadata; not an independent inspection of model weights or the daemon's network behavior");
    }
    private static void rejectRemote(Object item, int depth) throws IOException {
        if (depth > 20) throw new IOException("Model metadata is too deeply nested");
        if (item instanceof JSONObject o) for (String key : o.keySet()) {
            Object value = o.get(key);
            if (Set.of("remote_host", "remote_model", "remoteHost", "remoteModel").contains(key) && value != JSONObject.NULL && !String.valueOf(value).isBlank()) throw new IOException("Remote model metadata is unavailable in the local workspace");
            if (key.equals("status") && "REMOTE_PROXY".equals(value)) throw new IOException("Remote model proxy is unavailable in the local workspace");
            rejectRemote(value, depth + 1);
        }
        else if (item instanceof JSONArray a) for (Object value : a) rejectRemote(value, depth + 1);
    }
    static JSONObject request(String url, JSONObject body) throws IOException {
        var connection = (HttpURLConnection)URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(1500); connection.setReadTimeout(2500);
        if (body != null) { connection.setRequestMethod("POST"); connection.setDoOutput(true); connection.setRequestProperty("Content-Type", "application/json"); }
        try {
            if (body != null) try (var out = connection.getOutputStream()) { out.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            if (connection.getResponseCode() != 200) throw new IOException("Local model metadata HTTP " + connection.getResponseCode());
            byte[] bytes; try (var in = connection.getInputStream()) { bytes = in.readNBytes(2_000_001); }
            if (bytes.length > 2_000_000) throw new IOException("Local model metadata is too large");
            return new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (JSONException e) { throw new IOException("Invalid local model metadata", e); }
        finally { connection.disconnect(); }
    }
}

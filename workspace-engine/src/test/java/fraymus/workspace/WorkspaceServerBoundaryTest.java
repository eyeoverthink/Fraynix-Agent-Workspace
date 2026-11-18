package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

/** Real loopback requests through the server's unchanged shared admission guards. */
public class WorkspaceServerBoundaryTest {
    private static final String TOKEN = "boundary-test-only-" + "x".repeat(32);

    private HttpResponse<String> request(HttpClient client, int port, String route,
                                         String method, String token, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + route))
                .timeout(Duration.ofSeconds(30));
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, HttpRequest.BodyPublishers.ofString(body));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test(timeout = 180000)
    public void authenticationMethodAndBodyChecksPrecedeDomainEffects() throws Exception {
        try (var server = new WorkspaceServer(Files.createTempDirectory("server-boundary"), TOKEN);
             var client = HttpClient.newHttpClient()) {
            int port = server.start().getAddress().getPort();
            String route = "/api/small161/example";
            assertEquals(403, request(client, port, route, "POST", null, "{}").statusCode());
            assertEquals(403, request(client, port, route, "POST", "incorrect", "{}").statusCode());
            assertEquals(405, request(client, port, route, "GET", TOKEN, "").statusCode());
            assertEquals(400, request(client, port, route, "POST", TOKEN, "invalid JSON").statusCode());
            var oversized = request(client, port, route, "POST", TOKEN, " ".repeat(2_100_001));
            assertEquals(400, oversized.statusCode());
            assertTrue(oversized.body().contains("Request too large"));
            var before = new JSONObject(request(client, port, "/api/small161/state", "POST", TOKEN, "{}").body());
            assertEquals(0, before.getJSONArray("worlds").length());
            var created = request(client, port, route, "POST", TOKEN, "{}");
            assertEquals(200, created.statusCode());
            assertEquals(0, new JSONObject(created.body()).getInt("revision"));
            var unknown = request(client, port, "/api/small161/unknown", "POST", TOKEN, "{}");
            assertEquals(400, unknown.statusCode());
            assertTrue(unknown.body().contains("Unknown small-body operation"));
            assertEquals(400, request(client, port, "/api/cost164/status", "POST", TOKEN, "{\"extra\":true}").statusCode());
            assertEquals(200, request(client, port, "/api/recorded163/status", "POST", TOKEN, "{}").statusCode());
            var after = new JSONObject(request(client, port, "/api/small161/state", "POST", TOKEN, "{}").body());
            assertEquals(1, after.getJSONArray("worlds").length());
            assertEquals(0, after.getJSONArray("jobs").length());
        }
    }

    @Test(timeout = 180000)
    public void persistentHostLeaseStillPrecedesDomainMutation() throws Exception {
        try (var server = new WorkspaceServer(Files.createTempDirectory("server-lease"), TOKEN, true);
             var client = HttpClient.newHttpClient()) {
            int port = server.start().getAddress().getPort();
            var response = request(client, port, "/api/small161/example", "POST", TOKEN, "{}");
            assertEquals(400, response.statusCode());
            assertTrue(response.body(), response.body().toLowerCase().contains("interface"));
        }
    }
}

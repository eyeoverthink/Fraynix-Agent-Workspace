package editor;

import com.sun.net.httpserver.HttpServer;
import fraymus.factory.models.*;
import org.junit.Test;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.Assert.*;

public class ModelAvailabilityTest {
    @Test public void advertisedDecisionAndEmbeddingModelsAreNotOfferedForChat() throws Exception {
        Path dir=Files.createTempDirectory("model-capabilities-");String previous=System.getProperty("fraynix.localModelCatalog");
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);String endpoint="http://127.0.0.1:"+server.getAddress().getPort();
        server.createContext("/api/tags",exchange->{
            String json="{\"models\":[{\"name\":\"clef:27b\",\"size\":20000},{\"name\":\"chat:3b\",\"size\":2000},{\"name\":\"embed:latest\",\"size\":100}]}";
            byte[] bytes=json.getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.createContext("/api/ps",exchange->{byte[] bytes="{\"models\":[]}".getBytes();exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});
        server.createContext("/api/show",exchange->{
            String body=new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8);
            String caps=body.contains("clef")?"\"decision\",\"vision\"":body.contains("embed")?"\"embedding\"":"\"completion\",\"tools\"";
            byte[] bytes=("{\"capabilities\":["+caps+"]}").getBytes();exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {
            var rows=List.of("clef:27b","chat:3b","embed:latest").stream().map(name->Map.of("name",name,"status","LOCAL_INSTALLED","endpoint",endpoint)).toList();
            Path catalog=dir.resolve("catalog.json");Files.writeString(catalog,new com.google.gson.Gson().toJson(Map.of("models",rows)));System.setProperty("fraynix.localModelCatalog",catalog.toString());
            var service=new FactoryModelService(endpoint);var observed=service.observe();
            assertEquals(List.of("chat:3b"),observed.models().stream().filter(FactoryModelService.Model::available).map(FactoryModelService.Model::name).toList());
            assertTrue(observed.models().stream().filter(m->m.name().startsWith("clef")).findFirst().orElseThrow().availabilityReason().contains("decision"));
            try(var workspace=new FraynixWorkspace(dir,service)) {
                long deadline=System.nanoTime()+10_000_000_000L;
                while(workspace.snapshot().models().isEmpty()&&System.nanoTime()<deadline) {workspace.tick();Thread.sleep(10);}
                workspace.select("clef:27b");workspace.tick();assertEquals("chat:3b",workspace.selectedModel());assertTrue(workspace.selectionNotice().contains("clef"));
            }
        } finally {server.stop(0);if(previous==null)System.clearProperty("fraynix.localModelCatalog");else System.setProperty("fraynix.localModelCatalog",previous);}
    }
    @Test public void driveDiscoveryReportsMissingBlobsWithoutClaimingTheyAreServed() throws Exception {
        Path root=Files.createTempDirectory("model-store-");Files.createDirectories(root.resolve("blobs"));Files.createDirectories(root.resolve("manifests/registry/library/test"));
        String digest="0".repeat(64);Path manifest=root.resolve("manifests/registry/library/test/latest");
        Files.writeString(manifest,"{\"layers\":[{\"digest\":\"sha256:"+digest+"\",\"size\":5}]}");
        var missing=ModelStorageDiscovery.inspect(List.of(root)).get(0);assertEquals(1,missing.missingBlobs());assertEquals(0,missing.complete());
        Files.writeString(root.resolve("blobs/sha256-"+digest),"hello");var complete=ModelStorageDiscovery.inspect(List.of(root)).get(0);assertEquals(1,complete.complete());
    }
}

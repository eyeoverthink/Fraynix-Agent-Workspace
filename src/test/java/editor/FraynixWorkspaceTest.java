package editor;

import com.sun.net.httpserver.HttpServer;
import fraymus.factory.models.*;
import factory.SceneJson;
import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class FraynixWorkspaceTest {
    private static void until(FraynixWorkspace workspace,java.util.function.BooleanSupplier condition) throws Exception {
        long deadline=System.nanoTime()+8_000_000_000L;
        while(!condition.getAsBoolean()&&System.nanoTime()<deadline){workspace.tick();Thread.sleep(10);}
        workspace.tick();assertTrue("Workspace timed out: "+workspace.error(),condition.getAsBoolean());
    }
    @Test public void liveChatSeparatesAttachmentsUsesOwnIdentityAndRestoresEvidence() throws Exception {
        Path root=Files.createTempDirectory("studio-chat-");String previous=System.getProperty("fraynix.localModelCatalog");
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);String endpoint="http://127.0.0.1:"+server.getAddress().getPort();
        var body=new AtomicReference<String>();
        for(String path:List.of("/api/tags","/api/ps"))server.createContext(path,exchange->{
            byte[] bytes="{\"models\":[{\"name\":\"fixture:latest\",\"size\":1024}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.createContext("/api/chat",exchange->{
            body.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            byte[] bytes=("{\"model\":\"fixture:latest\",\"message\":{\"content\":\"actual \"},\"done\":false}\n"
                +"{\"model\":\"fixture:latest\",\"message\":{\"content\":\"answer\"},\"done\":true,\"eval_count\":2}\n").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });server.start();
        try {
            Path catalog=root.resolve("catalog.json");Files.writeString(catalog,"{\"models\":[{\"name\":\"fixture:latest\",\"status\":\"LOCAL_INSTALLED\",\"endpoint\":\""+endpoint+"\"}]}");
            System.setProperty("fraynix.localModelCatalog",catalog.toString());
            var store=new ConversationStore(root);
            Files.createDirectories(store.path().getParent());
            Files.writeString(store.path(),SceneJson.GSON.toJson(Map.of("model","fixture:latest","prompt","old question"+ChatContext.LEGACY_MARKER+"{\"old\":true}",
                "reply","old answer","endpoint",endpoint,"success",true,"millis",1,"at",Instant.now().toString()))+"\n");
            try(var workspace=new FraynixWorkspace(root,new FactoryModelService(endpoint))){
                until(workspace,()->workspace.historyReady()&&!workspace.snapshot().models().isEmpty());
                workspace.resumeSavedConversation();
                assertEquals("old question",workspace.visibleExchanges().get(0).prompt());
                workspace.newConversation();assertTrue(workspace.visibleExchanges().isEmpty());
                var profile=new ModelRunSettings(4096,128,1,0,5,600,.2);
                workspace.saveSettings(profile);until(workspace,()->!workspace.busy());assertEquals(profile,workspace.settings());
                workspace.send("What is selected?","{\"current\":true}");until(workspace,()->!workspace.busy());
                var saved=workspace.visibleExchanges().get(0);
                assertEquals("What is selected?",saved.prompt());assertEquals("{\"current\":true}",saved.sceneContext());
                assertEquals("actual answer",saved.reply());assertEquals("true",saved.evidence().get("done"));assertEquals("2",saved.evidence().get("frames"));
                assertTrue(saved.evidence().get("requestSha256").matches("[a-f0-9]{64}"));
                var sent=SceneJson.parse(body.get());var messages=sent.getAsJsonArray("messages");
                assertEquals(1,messages.size());assertEquals("user",messages.get(0).getAsJsonObject().get("role").getAsString());
                assertTrue(messages.get(0).getAsJsonObject().get("content").getAsString().contains("<scene_observation>"));
                assertEquals(4096,sent.getAsJsonObject("options").get("num_ctx").getAsInt());
            }
            try(var reopened=new FraynixWorkspace(root,new FactoryModelService(endpoint))){
                until(reopened,()->reopened.historyReady()&&!reopened.snapshot().models().isEmpty());
                assertEquals(1,reopened.visibleExchanges().size());assertEquals("actual answer",reopened.visibleExchanges().get(0).reply());
                assertEquals(4096,reopened.settings().context());assertEquals(2,new ConversationStore(root).load().size());
            }
        } finally {server.stop(0);if(previous==null)System.clearProperty("fraynix.localModelCatalog");else System.setProperty("fraynix.localModelCatalog",previous);}
    }

    @Test public void chatSceneBridgeDeserializesGsonGameObjectAndMovesSquare() {
        var scene = new scenes.Scene(null);
        var bottom = scene.createGameObject("BottomSquare");
        bottom.transform.position.set(0f, 0f);
        bottom.transform.scale.set(0.25f, 0.25f);
        scene.addGameObjectToScene(bottom);

        var top = scene.createGameObject("TopSquare");
        top.transform.position.set(0f, 2.0f);
        top.transform.scale.set(0.25f, 0.25f);
        scene.addGameObjectToScene(top);

        String chatReply = "I analyzed the scene. TopSquare is at y=2.0. Moving it to (1.5, 3.5):\n```json\n"
            + "[{\"entityId\":\"" + top.getPersistentId() + "\",\"name\":\"TopSquare\",\"components\":[{"
            + "\"type\":\"jade.Transform\",\"properties\":{\"position\":{\"x\":1.5,\"y\":3.5},\"scale\":{\"x\":0.25,\"y\":0.25},\"rotation\":0.0,\"zIndex\":0}}]}]\n```";

        String result = ChatSceneBridge.applyIfPresent(scene, chatReply, "");
        assertTrue(result.contains("TopSquare moved"));
        assertEquals(1.5f, top.transform.position.x, 1e-4f);
        assertEquals(3.5f, top.transform.position.y, 1e-4f);
        assertEquals(0f, bottom.transform.position.x, 1e-4f);
        assertEquals(0f, bottom.transform.position.y, 1e-4f);
    }
}

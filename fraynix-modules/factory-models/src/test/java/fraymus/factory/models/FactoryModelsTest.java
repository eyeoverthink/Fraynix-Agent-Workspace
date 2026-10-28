package fraymus.factory.models;

import com.sun.net.httpserver.HttpServer;
import fraymus.model.ModelAdapter;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/** Exercises actual HTTP parsing, exact-tag adapter routing, failure state and restart persistence. */
public final class FactoryModelsTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("factory-model-test-");
        String previous = System.getProperty("fraynix.localModelCatalog");
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var request = new AtomicReference<String>();
        var fail = new java.util.concurrent.atomic.AtomicBoolean();
        var mode=new AtomicReference<>("legacy");
        var firstFrame=new java.util.concurrent.CountDownLatch(1);
        var resume=new java.util.concurrent.CountDownLatch(1);
        var generated=new AtomicReference<org.json.JSONObject>();
        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/api/tags", exchange -> {
            byte[] bytes = (fail.get() ? "unavailable" : "{\"models\":[{\"name\":\"fraynix-test:3b\",\"size\":4096},{\"name\":\"remote:cloud\",\"remote_host\":\"https://remote.invalid\"}]}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(fail.get() ? 503 : 200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/api/ps", exchange -> {
            byte[] bytes = "{\"models\":[{\"name\":\"fraynix-test:3b\",\"size_vram\":2048,\"context_length\":4096}]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/api/show", exchange -> {
            byte[] bytes="{\"capabilities\":[\"completion\",\"tools\"]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.createContext("/api/chat", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            if(!mode.get().equals("legacy")){
                exchange.sendResponseHeaders(200,0);
                String first="{\"model\":\"fraynix-test:3b\",\"message\":{\"content\":\"Live café \\\"quoted\\\"\",\"thinking\":\"reason\"},\"done\":false}\n";
                exchange.getResponseBody().write(first.getBytes(StandardCharsets.UTF_8));exchange.getResponseBody().flush();firstFrame.countDown();
                if(mode.get().equals("wait"))try{resume.await(5,java.util.concurrent.TimeUnit.SECONDS);}catch(InterruptedException interrupted){Thread.currentThread().interrupt();}
                if(!mode.get().equals("truncated"))try{exchange.getResponseBody().write("{\"model\":\"fraynix-test:3b\",\"message\":{\"content\":\" done\"},\"done\":true,\"eval_count\":7,\"load_duration\":1000000}\n".getBytes(StandardCharsets.UTF_8));}catch(java.io.IOException disconnected){}
                exchange.close();return;
            }
            byte[] bytes = "{\"message\":{\"content\":\"Local reply: café \\\"quoted\\\"\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/api/generate",exchange->{
            generated.set(new org.json.JSONObject(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8)));
            byte[] bytes="{\"model\":\"fraynix-test:3b\",\"done\":true}\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();
        try {
            Path catalog = root.resolve("models.json");
            Files.writeString(catalog, "{\"stores\":[],\"models\":[{\"name\":\"fraynix-test:3b\",\"status\":\"LOCAL_INSTALLED\",\"endpoint\":\"" + endpoint
                + "\",\"family\":\"test\",\"parameterSize\":\"3B\",\"sourcePath\":\"F:/models/test\"}]}");
            System.setProperty("fraynix.localModelCatalog", catalog.toString());
            var service = new FactoryModelService(endpoint);
            var snapshot = service.observe();
            if (snapshot.models().size() != 1 || !snapshot.stores().get(0).online()) throw new AssertionError("Catalog or proxy filtering wrong");
            var model = snapshot.models().get(0);
            if (!model.available() || !model.loaded() || model.context() != 4096 || model.vramBytes() != 2048) throw new AssertionError("Live residency wrong");
            var reply = service.chat(model, "Check café \"quote\"\nnew line", List.of(ModelAdapter.Message.system("Be accurate")), Map.of("response_format", "json", "num_ctx", 8192));
            if (!reply.success() || !reply.text().equals("Local reply: café \"quoted\"") || !request.get().contains("fraynix-test:3b")) throw new AssertionError("Real Fraynix adapter did not route/parse chat");
            var body = new org.json.JSONObject(request.get());
            if (!body.getString("format").equals("json") || body.getJSONObject("options").getInt("num_ctx") != 8192 || body.getJSONObject("options").has("response_format")) throw new AssertionError("Structured output settings were not routed to the correct request fields");
            var exchange = new ConversationStore.Exchange(reply.model(), "question\nline two", reply.text(), reply.endpoint(), true, reply.millis(), Instant.now());
            var store = new ConversationStore(root); store.append(exchange);
            if (!new ConversationStore(root).load().equals(List.of(exchange))) throw new AssertionError("Chat lost on restart");
            var settings=new ModelRunSettings(4096,512,2,0,2,600,.2);
            mode.set("stream");var updates=new java.util.ArrayList<fraymus.OllamaIntegration.StreamUpdate>();
            var streamed=service.streamChat(model,"user question",List.of(),settings,new fraymus.OllamaIntegration.RequestControl(),updates::add);
            if(!streamed.text().equals("Live café \"quoted\" done")||!streamed.thinking().equals("reason")||updates.size()!=2||updates.get(0).done())throw new AssertionError("Streaming/Unicode/reasoning not propagated");
            if(!streamed.evidence().get("eval_count").equals("7")||!streamed.evidence().get("responseSha256").matches("[0-9a-f]{64}"))throw new AssertionError("Response evidence not recorded");
            body=new org.json.JSONObject(request.get());
            if(!body.getBoolean("stream")||body.getJSONArray("messages").length()!=1||!body.getJSONArray("messages").getJSONObject(0).getString("role").equals("user"))throw new AssertionError("App identity replaced model SYSTEM");
            if(body.getJSONObject("options").getInt("num_ctx")!=4096||!body.getString("keep_alive").equals("600s"))throw new AssertionError("Runtime profile was ignored");
            mode.set("truncated");try{service.streamChat(model,"question",List.of(),settings,new fraymus.OllamaIntegration.RequestControl(),ignored->{});throw new AssertionError("Truncated stream marked successful");}catch(java.io.IOException expected){}
            mode.set("wait");var control=new fraymus.OllamaIntegration.RequestControl();
            var entered=new java.util.concurrent.CountDownLatch(1);
            var pending=java.util.concurrent.CompletableFuture.runAsync(()->{
                try{service.streamChat(model,"cancel this",List.of(),settings,control,event->entered.countDown());throw new AssertionError("Cancelled request succeeded");}
                catch(java.util.concurrent.CancellationException expected){}
                catch(java.io.IOException failure){throw new java.util.concurrent.CompletionException(failure);}
            });
            if(!entered.await(3,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("First frame was buffered until completion");
            long cancelStart=System.nanoTime();control.cancel();resume.countDown();pending.get(3,java.util.concurrent.TimeUnit.SECONDS);
            if(System.nanoTime()-cancelStart>1_500_000_000L)throw new AssertionError("Cancellation blocks caller/render thread");
            var preCancelled=new fraymus.OllamaIntegration.RequestControl();preCancelled.cancel();
            try{service.streamChat(model,"never send",List.of(),settings,preCancelled,ignored->{});throw new AssertionError("Cancelled-before-connect request sent");}catch(java.util.concurrent.CancellationException expected){}
            service.manageModel(model,true,settings,new fraymus.OllamaIntegration.RequestControl());
            if(!generated.get().getString("keep_alive").equals("600s"))throw new AssertionError("Preload did not reach Ollama");
            service.manageModel(model,false,settings,new fraymus.OllamaIntegration.RequestControl());
            if(!generated.get().getString("keep_alive").equals("0s"))throw new AssertionError("Unload did not reach Ollama");
            var preferences=new ModelProfileStore(root);preferences.save(endpoint,model.name(),settings);
            if(!new ModelProfileStore(root).get(endpoint,model.name(),1).equals(settings))throw new AssertionError("Profile lost after restart");
            if(new ModelProfileStore(root).get("http://127.0.0.1:1",model.name(),1).equals(settings))throw new AssertionError("Profile leaked across endpoints");
            try{new ModelRunSettings(4096,128,0,-1,900,600,Double.NaN);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException expected){}
            String legacy="Question"+ChatContext.LEGACY_MARKER+"{\"entity\":1}";
            Path older=root.resolve("legacy");Files.createDirectories(older.resolve("chat"));Path olderPath=older.resolve("chat/exchanges.jsonl");
            String oldRow=new org.json.JSONObject().put("model",model.name()).put("prompt",legacy).put("reply","old").put("endpoint",endpoint).put("success",true).put("millis",1).put("at",Instant.now().toString())+"\n";
            Files.writeString(olderPath,oldRow);var migrated=new ConversationStore(older).load().get(0);
            if(!migrated.prompt().equals("Question")||!migrated.sceneContext().equals("{\"entity\":1}")||!Files.readString(olderPath).equals(oldRow))throw new AssertionError("Legacy JSON not separated or original journal modified");
            var captured=new ConversationStore.Exchange(model.name(),"human","reply",endpoint,true,2,Instant.now(),"scene data","thinking",Map.of("conversationId","thread","requestSha256",streamed.evidence().get("requestSha256")));
            var extra=new ConversationStore(root.resolve("v2"));extra.append(captured);
            if(!extra.load().equals(List.of(captured)))throw new AssertionError("V2 attachments/evidence not persisted");
            if(ChatContext.history(List.of(captured),model.name(),endpoint,"thread",1).size()!=0
                ||ChatContext.history(List.of(captured),model.name(),endpoint,"other",100).size()!=0
                ||ChatContext.history(List.of(captured),model.name(),endpoint,"thread",100).size()!=2)throw new AssertionError("History budget/thread isolation wrong");
            mode.set("legacy");
            fail.set(true);
            snapshot = service.observe();
            if (snapshot.stores().get(0).online() || snapshot.models().get(0).available() || snapshot.models().get(0).loaded()) throw new AssertionError("Offline server left stale online data");
            Files.writeString(store.path(), "broken\n", StandardOpenOption.APPEND);
            try { store.load(); throw new AssertionError("Malformed journal silently accepted"); } catch (java.io.IOException expected) { }
            if (!Files.readString(store.path()).endsWith("broken\n")) throw new AssertionError("Evidence was overwritten");
            try { new FactoryModelService("https://remote.invalid"); throw new AssertionError("Remote accepted"); } catch (IllegalArgumentException expected) { }
            System.out.println("FactoryModelsTest passed: health, exact routing, Unicode NDJSON streaming, partial rejection, live/pre-connect cancellation, model load/unload, profile restart, legacy attachment migration, evidence and thread/history budgets");
        } finally {
            server.stop(0);
            if (previous == null) System.clearProperty("fraynix.localModelCatalog"); else System.setProperty("fraynix.localModelCatalog", previous);
        }
    }
}

package fraymus.workspace;
import org.json.*;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Java-owned lifecycle and capability for the original renderer's hidden runtime. */
public final class EmbeddedGameClient implements AutoCloseable {
    private final Path repo,project;private Process process;private String token,origin;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public EmbeddedGameClient(Path repo,Path project){this.repo=repo.toAbsolutePath();this.project=project.resolve("embedded-game");}
    public synchronized JSONObject open() throws Exception {
        if(process!=null&&process.isAlive())return frame();
        Path lib=repo.resolve("build/install/Fraynix-Studio/lib");if(!Files.isDirectory(lib))throw new IllegalStateException("Build the preserved game runtime first");
        Files.createDirectories(project);token=UUID.randomUUID().toString()+UUID.randomUUID();
        var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"--enable-native-access=ALL-UNNAMED","-Dfraynix.embedded=true","-Dfraynix.studio.workspace="+project,"-cp",lib.toString()+File.separator+"*","Main","--mario");
        builder.directory(repo.resolve("build/install/Fraynix-Studio").toFile());builder.environment().put("FRAYNIX_GAME_TOKEN",token);builder.environment().put("FRAYNIX_GAME_PARENT_PID",String.valueOf(ProcessHandle.current().pid()));builder.redirectError(project.resolve("runtime-errors.log").toFile());
        process=builder.start();var ready=new CompletableFuture<String>();var owned=process;
        Thread.ofPlatform().daemon().name("embedded-game-output").start(()->{try(var reader=owned.inputReader()){String line;while((line=reader.readLine())!=null){Files.writeString(project.resolve("runtime.log"),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);try{var value=new JSONObject(line);if(value.optString("event").equals("game-runtime-ready"))ready.complete("http://127.0.0.1:"+value.getInt("port"));}catch(JSONException ignored){}}ready.completeExceptionally(new IOException("Game runtime exited "+owned.exitValue()));}catch(Exception e){ready.completeExceptionally(e);}});
        try{origin=ready.get(35,TimeUnit.SECONDS);for(int i=0;i<100;i++){try{return frame();}catch(Exception firstFrame){Thread.sleep(50);}}throw new IllegalStateException("Game frame unavailable");}
        catch(Exception error){process.destroy();origin=null;throw error;}
    }
    private JSONObject request(String route,JSONObject input) throws Exception {
        if(process==null||!process.isAlive()||origin==null)throw new IllegalStateException("Open embedded Game first");
        var b=HttpRequest.newBuilder(URI.create(origin+route)).timeout(Duration.ofSeconds(12)).header("Authorization","Bearer "+token);
        if(input==null)b.GET();else b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(input.toString()));
        var response=http.send(b.build(),HttpResponse.BodyHandlers.ofString());var value=new JSONObject(response.body());if(response.statusCode()!=200)throw new IllegalStateException(value.optString("error","Game request failed"));return value;
    }
    public synchronized JSONObject frame() throws Exception {long t=System.nanoTime();var result=request("/frame",null);result.put("bridgeMs",(System.nanoTime()-t)/1e6).put("pid",process.pid());return result;}
    public synchronized JSONObject spatial() throws Exception {return request("/command",new JSONObject().put("action","spatial"));}
    public synchronized JSONObject command(JSONObject input) throws Exception {request("/command",input);return frame();}
    public synchronized JSONObject status(){return new JSONObject().put("running",process!=null&&process.isAlive()).put("project",project.toString());}
    public synchronized void close(){
        if(process!=null){try{if(process.isAlive()&&origin!=null)request("/command",new JSONObject().put("action","close"));if(!process.waitFor(4,TimeUnit.SECONDS))process.destroy();}catch(Exception error){process.destroy();}finally{process=null;origin=null;}}
    }
}

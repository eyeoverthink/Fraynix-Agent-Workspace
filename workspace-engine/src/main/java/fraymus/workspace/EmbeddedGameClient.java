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
    private final Path repo,project;private final GameLevels114 levels;private String levelId;private Path activeProject;private Process process;private String token,origin;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    public EmbeddedGameClient(Path repo,Path project){this.repo=repo.toAbsolutePath();this.project=project.resolve("embedded-game");this.levels=new GameLevels114(repo,project);this.activeProject=this.project;}
    public synchronized JSONObject levels(String action,JSONObject input) throws Exception {
        return switch(action){
            case "analyze-levels" -> {SpatialScene.keys(input);yield levels.analyze();}
            case "levels" -> {SpatialScene.keys(input);yield levels.list();}
            case "create-level" -> levels.create(input);
            case "open-level" -> {SpatialScene.keys(input,"id");yield levels.launch(this,input.getString("id"));}
            default -> throw new IllegalArgumentException("Unknown candidate operation");
        };
    }
    public synchronized JSONObject open() throws Exception {
        if(process!=null&&process.isAlive()&&levelId==null)return frame();
        close();levelId=null;return start(project,null,null);
    }
    synchronized JSONObject openLevel(Path level,String id,String sha) throws Exception {
        close();levelId=id;return start(project.resolve("candidates").resolve(id).resolve(UUID.randomUUID().toString()),level,sha);
    }
    private JSONObject start(Path runtimeProject,Path level,String sha) throws Exception {
        activeProject=runtimeProject;
        Path lib=repo.resolve("build/install/Fraynix-Studio/lib");if(!Files.isDirectory(lib))throw new IllegalStateException("Build the preserved game runtime first");
        Files.createDirectories(runtimeProject);token=UUID.randomUUID().toString()+UUID.randomUUID();
        var command=new ArrayList<String>(List.of(Path.of(System.getProperty("java.home"),"bin","java.exe").toString(),"--enable-native-access=ALL-UNNAMED","-XX:ActiveProcessorCount=4","-Xmx512m","-Dfraynix.embedded=true","-Dfraynix.studio.workspace="+runtimeProject));
        if(level!=null){command.add("-Dfraynix.level="+level.toAbsolutePath());command.add("-Dfraynix.level.sha256="+sha);}
        command.addAll(List.of("-cp",lib.toString()+File.separator+"*","Main","--mario"));var builder=new ProcessBuilder(command);
        builder.directory(repo.resolve("build/install/Fraynix-Studio").toFile());builder.environment().put("FRAYNIX_GAME_TOKEN",token);builder.environment().put("FRAYNIX_GAME_PARENT_PID",String.valueOf(ProcessHandle.current().pid()));builder.redirectError(runtimeProject.resolve("runtime-errors.log").toFile());
        process=builder.start();var ready=new CompletableFuture<String>();var owned=process;
        Thread.ofPlatform().daemon().name("embedded-game-output").start(()->{try(var reader=owned.inputReader()){String line;while((line=reader.readLine())!=null){Files.writeString(runtimeProject.resolve("runtime.log"),line+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);try{var value=new JSONObject(line);if(value.optString("event").equals("game-runtime-ready"))ready.complete("http://127.0.0.1:"+value.getInt("port"));}catch(JSONException ignored){}}ready.completeExceptionally(new IOException("Game runtime exited "+owned.exitValue()));}catch(Exception e){ready.completeExceptionally(e);}});
        try{origin=ready.get(35,TimeUnit.SECONDS);for(int i=0;i<100;i++){try{return frame();}catch(Exception firstFrame){Thread.sleep(50);}}throw new IllegalStateException("Game frame unavailable");}
        catch(Exception error){process.destroy();origin=null;throw error;}
    }
    private JSONObject request(String route,JSONObject input) throws Exception {
        if(process==null||!process.isAlive()||origin==null)throw new IllegalStateException("Open embedded Game first");
        var b=HttpRequest.newBuilder(URI.create(origin+route)).timeout(Duration.ofSeconds(12)).header("Authorization","Bearer "+token);
        if(input==null)b.GET();else b.header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(input.toString()));
        var response=http.send(b.build(),HttpResponse.BodyHandlers.ofString());var value=new JSONObject(response.body());if(response.statusCode()!=200)throw new IllegalStateException(value.optString("error","Game request failed"));return value;
    }
    public synchronized JSONObject frame() throws Exception {long t=System.nanoTime();var result=request("/frame",null);result.put("bridgeMs",(System.nanoTime()-t)/1e6).put("pid",process.pid()).put("levelId",levelId==null?"original":levelId);return result;}
    public synchronized JSONObject spatial() throws Exception {return request("/command",new JSONObject().put("action","spatial"));}
    public synchronized JSONObject command(JSONObject input) throws Exception {if(levelId!=null&&"reset".equals(input.optString("action")))levels.verify(levelId);request("/command",input);return frame();}
    public synchronized JSONObject status(){return new JSONObject().put("running",process!=null&&process.isAlive()).put("project",activeProject.toString()).put("levelId",levelId==null?"original":levelId);}
    public synchronized void close(){
        if(process!=null){try{if(process.isAlive()&&origin!=null)request("/command",new JSONObject().put("action","close"));if(!process.waitFor(4,TimeUnit.SECONDS))process.destroy();}catch(Exception error){process.destroy();}finally{process=null;origin=null;}}
    }
}

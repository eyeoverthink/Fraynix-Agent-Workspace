package runtime;
import com.google.gson.*;
import com.sun.net.httpserver.*;
import fraymus.neural.ControlledRuntime;
import jade.Window;
import components.PlayerController;
import java.util.*;
import java.util.concurrent.*;
import java.nio.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import javax.imageio.ImageIO;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;

/** Original renderer/runtime without editor panels or a visible game window. */
public final class EmbeddedGameRuntime implements ControlledRuntime,AutoCloseable {
    private final Thread owner=Thread.currentThread();
    private final Gson json=new Gson();
    private final ArrayBlockingQueue<Request> requests=new ArrayBlockingQueue<>(4);
    private final String session=UUID.randomUUID().toString(),token=System.getenv("FRAYNIX_GAME_TOKEN");
    private volatile Frame last;
    private HttpServer server;private boolean continuous,closing;private long sequence;
    private record Request(JsonObject args,CompletableFuture<Map<String,Object>> result){}
    private record Frame(Map<String,Object> state,byte[] png,long captureNs){}
    public EmbeddedGameRuntime() throws IOException {
        if(token==null||token.length()<32)throw new IllegalStateException("Private game capability required");
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/",exchange->{
            try{
                if(!("Bearer "+token).equals(exchange.getRequestHeaders().getFirst("Authorization")))throw new IllegalArgumentException("Not authorized");
                Object value;
                if(exchange.getRequestURI().getPath().equals("/command")){
                    byte[] bytes=exchange.getRequestBody().readNBytes(4097);if(bytes.length>4096)throw new IllegalArgumentException("Command too large");
                    var input=JsonParser.parseString(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();var future=new CompletableFuture<Map<String,Object>>();
                    if(!requests.offer(new Request(input,future)))throw new IllegalStateException("Game command queue is full");
                    value=future.get(10,TimeUnit.SECONDS);
                }else if(exchange.getRequestURI().getPath().equals("/frame")){
                    Frame f=last;if(f==null)throw new IllegalStateException("First game frame pending");
                    value=Map.of("state",f.state,"pngBase64",Base64.getEncoder().encodeToString(f.png),"pngSha256",hash(f.png),"captureMs",f.captureNs/1e6);
                }else throw new IllegalArgumentException("Unknown game endpoint");
                send(exchange,200,value);
            }catch(Exception error){send(exchange,400,Map.of("error",String.valueOf(error.getMessage())));}
        });server.start();
        System.out.println(json.toJson(Map.of("event","game-runtime-ready","port",server.getAddress().getPort(),"sessionId",session)));
    }
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
    private void send(HttpExchange x,int code,Object body) throws IOException {byte[] bytes=json.toJson(body).getBytes(java.nio.charset.StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json");x.sendResponseHeaders(code,bytes.length);try(var out=x.getResponseBody()){out.write(bytes);}}
    private void check(){if(Thread.currentThread()!=owner)throw new IllegalStateException("Game runtime owner thread required");}
    public Map<String,Object> observe(){
        check();var scene=Window.getScene();var value=new LinkedHashMap<String,Object>();
        value.put("sessionId",session);value.put("tick",scene.tick());value.put("frameSequence",sequence);value.put("objects",scene.getGameObjects().size());value.put("units","legacy game units; XY plane; render layer is not physical Z");
        value.put("visibleWindow",org.lwjgl.glfw.GLFW.glfwGetWindowAttrib(Window.get().nativeWindow(),org.lwjgl.glfw.GLFW.GLFW_VISIBLE)!=0);value.put("editorInitialized",Window.getImguiLayer()!=null);value.put("continuous",continuous);
        var player=scene.getGameObjectWith(PlayerController.class);
        if(player!=null){var controller=player.getComponent(PlayerController.class);value.put("player",Map.of("id",player.getPersistentId().toString(),"x",player.transform.position.x,"y",player.transform.position.y,"small",controller.isSmall(),"dead",controller.isDead(),"won",controller.hasWon(),"onGround",controller.onGround));}
        value.put("neural",Map.of("sharedCore",true,"ready",nervous.BrainRuntime.core()!=null,"ticks",nervous.BrainRuntime.core()==null?0:nervous.BrainRuntime.core().ticks()));
        return value;
    }
    public Map<String,Object> step(int ticks,float horizontal,boolean jump){
        check();if(ticks<1||ticks>120||!Float.isFinite(horizontal)||Math.abs(horizontal)>1)throw new IllegalArgumentException("Bounded game input required");
        continuous=false;agent.GameInput.stop("Embedded manual step");
        try{agent.GameInput.external(horizontal,jump);for(int i=0;i<ticks;i++)Window.get().stepEmbedded();}
        finally{agent.GameInput.externalOff();}
        return observe();
    }
    public void frame(){
        check();String parent=System.getenv("FRAYNIX_GAME_PARENT_PID");if(parent!=null&&!ProcessHandle.of(Long.parseLong(parent)).map(ProcessHandle::isAlive).orElse(false))closing=true;
        Request pending=requests.poll();Object outcome=null;Exception failure=null;boolean spatialRead=false;
        if(pending!=null)try{
            var args=pending.args;String action=args.get("action").getAsString();
            var allowed=new HashSet<>(Set.of("action"));if(action.equals("step"))allowed.addAll(Set.of("ticks","horizontal","jump"));if(action.equals("ai"))allowed.add("targetX");
            if(!allowed.containsAll(args.keySet()))throw new IllegalArgumentException("Unsupported game command field");
            switch(action){
                case "spatial"->spatialRead=true;
                case "step"->step(args.get("ticks").getAsInt(),args.has("horizontal")?args.get("horizontal").getAsFloat():0,args.has("jump")&&args.get("jump").getAsBoolean());
                case "play"->continuous=true;
                case "pause"->{continuous=false;agent.GameInput.stop("Embedded pause");agent.GameInput.externalOff();}
                case "ai"->{float target=args.get("targetX").getAsFloat();if(!Float.isFinite(target)||Math.abs(target)>200)throw new IllegalArgumentException("Invalid game target");agent.GameInput.start(Window.getScene(),target);continuous=true;}
                case "reset"->{continuous=false;agent.GameInput.stop("Embedded reset");agent.GameInput.externalOff();Window.changeScene(new scenes.LevelSceneInitializer());}
                case "close"->{continuous=false;closing=true;}
                default->throw new IllegalArgumentException("Unknown game action");
            }
        }catch(Exception error){failure=error;}
        if(continuous)Window.get().stepEmbedded();
        nervous.BrainRuntime.poll();
        if(pending!=null||sequence%4==0||last==null){
            long started=System.nanoTime();var framebuffer=Window.getFramebuffer();framebuffer.bind();
            glViewport(0,0,960,540);var clear=Window.getScene().camera().clearColor;glClearColor(clear.x,clear.y,clear.z,clear.w);glClear(GL_COLOR_BUFFER_BIT);
            renderer.Renderer.bindShader(util.AssetPool.getShader("assets/shaders/default.glsl"));Window.getScene().render();
            ByteBuffer buffer=BufferUtils.createByteBuffer(960*540*4);glReadPixels(0,0,960,540,GL_RGBA,GL_UNSIGNED_BYTE,buffer);framebuffer.unbind();
            var image=new BufferedImage(960,540,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<540;y++)for(int x=0;x<960;x++){int p=((539-y)*960+x)*4;image.setRGB(x,y,(buffer.get(p)&255)<<16|(buffer.get(p+1)&255)<<8|(buffer.get(p+2)&255));}
            try(var out=new ByteArrayOutputStream()){ImageIO.write(image,"png",out);last=new Frame(observe(),out.toByteArray(),System.nanoTime()-started);}catch(IOException error){failure=error;}
        }
        if(spatialRead&&failure==null)try{var data=new LinkedHashMap<String,Object>(SpatialGameSnapshot.capture());data.put("state",last.state);data.put("pngBase64",Base64.getEncoder().encodeToString(last.png));data.put("pngSha256",hash(last.png));outcome=data;}catch(Exception error){failure=error;}
        sequence++;
        if(pending!=null){if(failure!=null)pending.result.completeExceptionally(failure);else pending.result.complete(outcome==null?last.state:(Map<String,Object>)outcome);}
        if(closing)org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose(Window.get().nativeWindow(),true);
    }
    public void close(){if(server!=null)server.stop(0);for(Request request:requests)request.result.completeExceptionally(new IllegalStateException("Game runtime stopped"));requests.clear();}
}

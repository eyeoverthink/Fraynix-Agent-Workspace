package fraymus.workspace;

import java.nio.channels.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.json.JSONObject;

/** A local Java process owns a project; interfaces reconnect through its private descriptor. */
public final class AgentHost042 {
    static String buildId(Path lib)throws Exception{
        var text=new StringBuilder("fraynix-agent-host-042-v1\n");
        try(var files=Files.list(lib)){for(Path p:files.filter(f->f.toString().endsWith(".jar")).sorted(Comparator.comparing(f->f.getFileName().toString())).toList())text.append(p.getFileName()).append(':').append(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(p)))).append('\n');}
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));
    }
    public static void main(String[] args){try{String project=System.getenv("FRAYNIX_WORKSPACE_PROJECT");if(project!=null){var out=new java.io.PrintStream(Files.newOutputStream(Path.of(project).resolve("engine-service.log"),StandardOpenOption.CREATE,StandardOpenOption.APPEND),true,StandardCharsets.UTF_8);System.setOut(out);System.setErr(out);}run(true);}catch(Throwable error){error.printStackTrace();System.exit(1);}}
    static void run(boolean persistent)throws Exception{
        Path project=Path.of(System.getenv().getOrDefault("FRAYNIX_WORKSPACE_PROJECT",Path.of(System.getProperty("user.home"),".fraynix-agent-workspace","projects","default").toString()));
        Files.createDirectories(project);project=project.toRealPath();Path root=project.resolve("runtime-042");Files.createDirectories(root);
        var channel=FileChannel.open(root.resolve("host.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock();
        if(lock==null){channel.close();throw new IllegalStateException("This project's Java agent already has an owner");}
        WorkspaceServer workspace=null;
        try{
            String token=System.getenv("FRAYNIX_WORKSPACE_TOKEN");if(persistent){byte[] random=new byte[32];new SecureRandom().nextBytes(random);token=HexFormat.of().formatHex(random);}
            workspace=new WorkspaceServer(project,token,persistent);var server=workspace.start();var owner=workspace;
            String boot=workspace.agent042().getString("bootId");Path descriptor=root.resolve("host.json");
            Runtime.getRuntime().addShutdownHook(new Thread(()->{try{owner.close();if(persistent&&Files.exists(descriptor)&&AgentFiles042.read(descriptor,16384).optString("bootId").equals(boot))Files.delete(descriptor);}catch(Exception e){System.err.println("Agent owner close: "+e.getMessage());}finally{try{lock.release();channel.close();}catch(Exception ignored){}}},"agent-owner-close"));
            if(persistent){Path lib=Path.of(System.getProperty("user.dir"),"workspace-engine","build","install","workspace-engine","lib");AgentFiles042.write(descriptor,new JSONObject().put("protocol","fraynix-agent-host-042-v1").put("origin","http://127.0.0.1:"+server.getAddress().getPort()).put("token",token).put("pid",ProcessHandle.current().pid()).put("bootId",boot).put("agentId",workspace.agent042().getString("agentId")).put("project",project.toString()).put("buildId",buildId(lib)));}
            System.out.println(new JSONObject().put("event","workspace-ready").put("port",server.getAddress().getPort()).put("project",project.toString()).put("persistent",persistent));
        }catch(Throwable failure){if(workspace!=null)workspace.close();lock.release();channel.close();throw failure;}
    }
}

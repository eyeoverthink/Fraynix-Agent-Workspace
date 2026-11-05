package fraymus.workspace;

import org.json.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Content-addressed recordings. Import is atomic; replay never writes to a scene. */
final class EvidenceStore049 {
    private final Path root;
    EvidenceStore049(Path project){root=project.resolve("evidence-049");}
    synchronized JSONObject importText(String text)throws Exception{
        if(text.getBytes(StandardCharsets.UTF_8).length>EvidenceReplay049.MAX_BYTES)throw new IllegalArgumentException("Evidence import is limited to 1 MiB");
        var input=new JSONObject(text);
        var analysis=EvidenceReplay049.analyze(input);
        String id=WorldFrames.hash(input);
        Path file=path(id);
        if(Files.exists(file))return read(id).put("duplicate",true);
        if(files().size()>=16)throw new IllegalStateException("Sixteen recordings retained; export before starting a new project");
        Files.createDirectories(root);
        byte[] bytes=new JSONObject().put("sha256",id).put("recording",input).toString().getBytes(StandardCharsets.UTF_8);
        Path pending=Files.createTempFile(root,"import-",".pending");
        try{
            try(var channel=FileChannel.open(pending,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
            Files.move(pending,file,StandardCopyOption.ATOMIC_MOVE);
        }finally{Files.deleteIfExists(pending);}
        return analysis.put("id",id).put("duplicate",false);
    }
    synchronized JSONObject read(String id)throws Exception{return EvidenceReplay049.analyze(recording(id)).put("id",id);}
    synchronized JSONObject export(String id)throws Exception{return new JSONObject().put("id",id).put("text",recording(id).toString(2)+"\n");}
    synchronized JSONObject list()throws Exception{
        var entries=new JSONArray();
        for(Path path:files()){
            String name=path.getFileName().toString(), id=name.substring(0,name.length()-5);
            try{var o=recording(id);entries.put(new JSONObject().put("id",id).put("title",o.getString("title")).put("provenance",o.getString("provenance")).put("available",true));}
            catch(Exception e){entries.put(new JSONObject().put("id",id).put("title","Recording needs attention").put("available",false).put("reason",e.getMessage()));}
        }
        return new JSONObject().put("recordings",entries);
    }
    private JSONObject recording(String id)throws Exception{
        Path file=path(id);
        if(Files.size(file)>EvidenceReplay049.MAX_BYTES+512)throw new IllegalStateException("Stored recording exceeds its size limit; original retained");
        var envelope=new JSONObject(Files.readString(file));
        SpatialScene.keys(envelope,"sha256","recording");
        var input=envelope.getJSONObject("recording");
        if(!id.equals(envelope.getString("sha256"))||!id.equals(WorldFrames.hash(input)))throw new IllegalStateException("Stored recording checksum changed; original retained");
        EvidenceReplay049.analyze(input);
        return input;
    }
    private Path path(String id){if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Choose a retained recording identity");return root.resolve(id+".json");}
    private List<Path> files()throws Exception{if(!Files.exists(root))return List.of();try(var stream=Files.list(root)){return stream.filter(p->p.getFileName().toString().matches("[a-f0-9]{64}\\.json")).sorted().toList();}}
}

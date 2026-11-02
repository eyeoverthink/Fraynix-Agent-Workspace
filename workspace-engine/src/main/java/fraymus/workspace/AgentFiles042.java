package fraymus.workspace;

import java.io.*;
import java.nio.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.UUID;
import org.json.JSONObject;

/** Owned atomic, forced checkpoints. Imported data never becomes a runtime handler. */
final class AgentFiles042 {
    private AgentFiles042(){}
    static void write(Path path,JSONObject data)throws IOException{
        Files.createDirectories(path.getParent());Path pending=path.resolveSibling(path.getFileName()+"."+UUID.randomUUID()+".pending");
        try(var channel=FileChannel.open(pending,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){
            var bytes=ByteBuffer.wrap(data.toString().getBytes(StandardCharsets.UTF_8));while(bytes.hasRemaining())channel.write(bytes);channel.force(true);
        }
        Files.move(pending,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    static JSONObject read(Path path,int max)throws IOException{if(Files.size(path)>max)throw new IOException("Agent checkpoint exceeds its format limit");return new JSONObject(Files.readString(path,StandardCharsets.UTF_8));}
    static JSONObject loadChecked(Path path)throws IOException{
        var envelope=read(path,4_000_000);var data=envelope.getJSONObject("data");if(!"java-agent-042-v1".equals(envelope.getString("format"))||!WorldFrames.hash(data).equals(envelope.getString("sha256")))throw new IOException("Agent checkpoint integrity mismatch; original retained");return data;
    }
    static void saveChecked(Path path,JSONObject data)throws IOException{write(path,new JSONObject().put("format","java-agent-042-v1").put("sha256",WorldFrames.hash(data)).put("data",data));}
}

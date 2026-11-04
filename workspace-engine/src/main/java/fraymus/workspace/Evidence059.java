package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Derived evidence only. Every lookup hashes current original bytes, never just file metadata. */
final class Evidence059 {
    private Evidence059() {}
    private static final long LIMIT = 32_000_000;
    private static final LinkedHashMap<String,byte[]> cache = new LinkedHashMap<>(16,.75f,true);
    private static long retained,hits,misses,bytesRead;
    @FunctionalInterface interface Derive { JSONObject apply(JSONObject original) throws Exception; }
    static JSONObject derive(Path path, String binding, Derive derive) throws Exception {
        if (Files.size(path)>64_000_000) throw new IllegalStateException("Evidence exceeds original 64MB budget");
        byte[] bytes=Files.readAllBytes(path);
        if(bytes.length>64_000_000)throw new IllegalStateException("Evidence grew beyond original budget");
        String key=path.toAbsolutePath().normalize()+":"+binding+":"+MapDefinition.sha(bytes);
        byte[] found;
        synchronized(cache){bytesRead+=bytes.length;found=cache.get(key);if(found!=null)hits++;else misses++;}
        if(found!=null)return new JSONObject(new String(found,StandardCharsets.UTF_8));
        var envelope=new JSONObject(new String(bytes,StandardCharsets.UTF_8));var original=envelope.getJSONObject("data");
        if(!envelope.getString("sha256").equals(Payload057.evidenceHash(original)))throw new IllegalStateException("Payload checkpoint checksum changed");
        var value=derive.apply(original);byte[] encoded=value.toString().getBytes(StandardCharsets.UTF_8);
        if(encoded.length<=LIMIT)synchronized(cache){
            byte[] previous=cache.remove(key);if(previous!=null)retained-=previous.length;
            while(!cache.isEmpty()&&(retained+encoded.length>LIMIT||cache.size()>=32)){var iterator=cache.entrySet().iterator();var entry=iterator.next();retained-=entry.getValue().length;iterator.remove();}
            cache.put(key,encoded);retained+=encoded.length;
        }
        return value;
    }
    static JSONObject stats(){synchronized(cache){return new JSONObject().put("hits",hits).put("misses",misses).put("bytesRead",bytesRead).put("retainedBytes",retained).put("entries",cache.size()).put("limitBytes",LIMIT);}}
    static void clear(){synchronized(cache){cache.clear();retained=0;hits=0;misses=0;bytesRead=0;}}
}

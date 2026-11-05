package fraymus.workspace;

import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.*;

/** Lossless storage envelope for large contact records; APIs still return original JSON. */
final class BrainPayload064 {
    static final String PREFIX="fraynix-brain-gzip064-v1:";
    static final int MAX_BYTES=128*1024*1024;
    static String encode(JSONObject value){String raw=value.toString();if(!java.util.Set.of("contact-uncertainty","decision-value","bounded-recovery","temporal-recovery","calibrated-recovery","later-recovery","adaptive-observation").contains(value.optString("kind"))||raw.length()<65536)return raw;return pack(raw);}
    static String pack(String raw){byte[] bytes=raw.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("Contact evidence exceeds storage budget");try{
        var output=new ByteArrayOutputStream();try(var gzip=new GZIPOutputStream(output){{def.setLevel(Deflater.BEST_SPEED);}}){gzip.write(bytes);}
        return PREFIX+new JSONObject().put("bytes",bytes.length).put("sha256",MapDefinition.sha(bytes)).put("data",Base64.getEncoder().encodeToString(output.toByteArray()));
    }catch(IOException e){throw new IllegalStateException("Cannot pack contact evidence",e);}}
    static String unpack(String stored){if(!stored.startsWith(PREFIX))return stored;
        try{if(stored.length()>MAX_BYTES*2)throw new IllegalStateException("Oversized contact envelope");var envelope=new JSONObject(stored.substring(PREFIX.length()));SpatialScene.keys(envelope,"bytes","sha256","data");int expected=envelope.getInt("bytes");if(expected<0||expected>MAX_BYTES||envelope.getDouble("bytes")!=expected)throw new IllegalStateException("Invalid contact evidence size");
            byte[] bytes;try(var gzip=new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(envelope.getString("data"))))){bytes=gzip.readNBytes(expected+1);}
            if(bytes.length!=expected||!MapDefinition.sha(bytes).equals(envelope.getString("sha256")))throw new IllegalStateException("Contact evidence integrity mismatch");return new String(bytes,StandardCharsets.UTF_8);
        }catch(IOException|IllegalArgumentException e){throw new IllegalStateException("Invalid packed contact evidence",e);}
    }
}

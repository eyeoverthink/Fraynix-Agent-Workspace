package fraymus.workspace;

import org.json.JSONObject;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.LongAdder;

/** Reuses only an authenticated canonical digest. Every caller gets freshly decoded data. */
final class NativeEnvelope091 {
    static final int CAPACITY = 64, MAX_BYTES = 32_000_000;
    private final Path root;
    private final int capacity, maxBytes;
    private final LinkedHashMap<String,String> digests = new LinkedHashMap<>(16,.75f,true);
    private boolean reference;
    private long generation;
    private final LongAdder reads=new LongAdder(), bytes=new LongAdder(), hits=new LongAdder(),
        canonicalChecks=new LongAdder(), failures=new LongAdder(), nanos=new LongAdder();

    NativeEnvelope091(Path root) { this(root,CAPACITY,MAX_BYTES); }
    NativeEnvelope091(Path root,int capacity,int maxBytes) {
        if(capacity<1||capacity>CAPACITY||maxBytes<1||maxBytes>MAX_BYTES)throw new IllegalArgumentException("Invalid envelope limits");
        this.root=root.toAbsolutePath().normalize();this.capacity=capacity;this.maxBytes=maxBytes;
    }
    private void owned(Path file)throws IOException {
        if(!file.startsWith(root)||!Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)
            ||!file.toRealPath().equals(root.toRealPath().resolve(root.relativize(file))))
            throw new IOException("Native envelope path is missing, redirected or outside its owner");
    }
    JSONObject read(Path path,String goalId,String attemptId,String expectedSha)throws IOException {
        long began=System.nanoTime();reads.increment();
        try {
            Path file=path.toAbsolutePath().normalize();owned(file);
            if(Files.size(file)>maxBytes)throw new IOException("Native envelope exceeds its format limit");
            byte[] source;
            try(var in=Files.newInputStream(file,LinkOption.NOFOLLOW_LINKS)){source=in.readNBytes(maxBytes+1);}
            bytes.add(source.length);owned(file);
            if(source.length>maxBytes)throw new IOException("Native envelope grew beyond its format limit");
            String byteSha=MapDefinition.sha(source),canonical;
            long observedGeneration;
            synchronized(this){observedGeneration=generation;canonical=reference?null:digests.get(byteSha);}
            // No mutable JSON, proof verdict or filesystem metadata is retained in the table.
            var envelope=new JSONObject(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(source)).toString());
            var run=envelope.getJSONObject("data");
            boolean reused=canonical!=null;
            if(!reused){canonicalChecks.increment();canonical=WorldFrames.hash(run);}
            if(!envelope.getString("format").equals("java-agent-042-v1")||!envelope.getString("sha256").equals(canonical))
                throw new IOException("Original native envelope changed");
            if(!canonical.equals(expectedSha)||!run.getString("id").equals(attemptId)||!run.getString("goalId").equals(goalId))
                throw new IOException("Changed or foreign original native attempt");
            if(reused)hits.increment();
            synchronized(this){if(!reference&&generation==observedGeneration){digests.put(byteSha,canonical);while(digests.size()>capacity)digests.remove(digests.keySet().iterator().next());}}
            return run;
        } catch(IOException|RuntimeException failure){failures.increment();throw failure;}
        finally {nanos.add(System.nanoTime()-began);}
    }
    synchronized JSONObject stats(){return new JSONObject().put("version","native-envelope-091-v1")
        .put("mode",reference?"reference":"reuse").put("entries",digests.size()).put("capacity",capacity)
        .put("reads",reads.sum()).put("bytes",bytes.sum()).put("canonicalChecks",canonicalChecks.sum())
        .put("hits",hits.sum()).put("failures",failures.sum()).put("readMillis",nanos.sum()/1e6)
        .put("scope","Full bytes and fresh JSON on every read; no outcome checks cached");}
    synchronized void testMode(String mode){
        if(!mode.equals("reference")&&!mode.equals("reuse"))throw new IllegalArgumentException("Choose reference or reuse");
        reference=mode.equals("reference");digests.clear();generation++;
    }
}

package fraymus.workspace;

import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Process-local memo of completed audits, bound to freshly read evidence and implementation bytes.
 * This does not modify the historical verifier, tracker identity, files, or admission rules. */
final class ReplayEvidence157 {
    static final int CAPACITY=8;
    private static final long MAX_BYTES=1_024_000_000L;
    private static final int MAX_FILES=8192;
    private final Path project;
    private final OriginalAudit157 association;
    private final boolean precomputed;
    boolean precomputed(){return precomputed;}
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>(16,.75f,true);
    private record Entry(String fingerprint,String serialized) {}
    record Snapshot(String hash,long bytes,int files) {}
    private static final class TooLarge extends Exception {}
    ReplayEvidence157(Path project,WorldObservationStore observations,AssociationAudit134 association){this(project,observations,association,true);}
    ReplayEvidence157(Path project,WorldObservationStore observations,AssociationAudit134 association,boolean precomputed){this.project=project.toAbsolutePath().normalize();this.association=new OriginalAudit157(project,observations,association,precomputed);this.precomputed=precomputed;}

    synchronized JSONObject read(String id,boolean forceReplay)throws Exception {
        if(!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Invalid evidence identity");
        // The historical store initializes missing state on access. A read-only opening must refuse
        // that condition instead of recreating an empty history, including on a warm request.
        if(!Files.isRegularFile(project.resolve("world-observations/state.json"),LinkOption.NOFOLLOW_LINKS)){
            entries.clear();throw new IllegalStateException("Original observation state is missing; no history recreated");
        }
        long start=System.nanoTime(),hashStart=start,hashNs=0;Snapshot before;
        try{before=snapshot();}catch(TooLarge e){entries.clear();var audit=association.readAudit(id);return result(audit,false,"Evidence exceeds memo bounds; full original verification",0,0,0,start);}
        hashNs+=System.nanoTime()-hashStart;Entry entry=entries.get(id);
        boolean reused=!forceReplay&&entry!=null&&entry.fingerprint.equals(before.hash);
        if(!reused)entries.remove(id);
        JSONObject audit;
        try {audit=reused?new JSONObject(entry.serialized):association.readAudit(id);}
        catch(Exception e){entries.remove(id);throw e;}
        hashStart=System.nanoTime();Snapshot after;
        try{after=snapshot();}catch(Exception e){entries.clear();throw new IllegalStateException("Evidence changed or exceeded bounds during verification; retry from stable evidence",e);}
        hashNs+=System.nanoTime()-hashStart;
        if(!before.hash.equals(after.hash)){entries.clear();throw new IllegalStateException("Evidence or implementation changed during verification; no result retained");}
        String serialized=audit.toString();
        if(serialized.length()<=1_000_000) {
            entries.put(id,new Entry(after.hash,serialized));
            while(entries.size()>CAPACITY)entries.remove(entries.keySet().iterator().next());
        }
        return result(audit,reused,reused?"Exact evidence and code bytes match a completed replay in this process":"Full original source, reference, code and outcome replay",before.bytes+after.bytes,before.files+after.files,hashNs,start)
            .put("evidenceFingerprint",after.hash);
    }
    private JSONObject result(JSONObject audit,boolean reused,String reason,long bytes,int files,long hashNs,long start){
        return new JSONObject().put("audit",audit).put("cost151",new JSONObject().put("reusedReplay",reused).put("reason",reason)
            .put("bytesHashed",bytes).put("filesHashed",files).put("hashMs",hashNs/1e6).put("totalMs",(System.nanoTime()-start)/1e6)
            .put("retainedEntries",entries.size()).put("capacity",CAPACITY).put("scope","Every opening rereads evidence bytes before and after verification. Full replay on cold start, changed bytes or explicit replay. Memo is process-local, read-only and bounded; it is not new tracking evidence."));
    }

    Snapshot snapshot()throws Exception {
        var leaves=new TreeMap<String,Path>();
        for(String name:List.of("world-observations","association-labels134","verified-appearances135"))collect(project.resolve(name),name,leaves,false);
        // The audit uses only the workspace implementation, JSON and JDK image/math operations.
        // Hash entire defining code containers, including helpers absent from historical identities.
        var origins=new LinkedHashSet<Path>();
        for(Class<?> type:List.of(ReplayEvidence157.class,JSONObject.class))origins.add(Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath().normalize());
        int i=0;for(Path origin:origins)collect(origin,"code"+(i++),leaves,true);
        var hash=MessageDigest.getInstance("SHA-256");
        hash.update((System.getProperty("java.runtime.version")+"\n"+System.getProperty("java.vm.name")+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long bytes=0;byte[] buffer=new byte[65536];
        for(var leaf:leaves.entrySet()) {
            Path file=leaf.getValue();if(file==null){hash.update((leaf.getKey()+"\0").getBytes(java.nio.charset.StandardCharsets.UTF_8));continue;}
            long size=Files.size(file);if(size>MAX_BYTES-bytes)throw new TooLarge();
            hash.update((leaf.getKey()+"\0"+size+"\0").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            long count=0;try(InputStream input=Files.newInputStream(file)){int n;while((n=input.read(buffer))!=-1){count+=n;bytes+=n;if(bytes>MAX_BYTES)throw new TooLarge();hash.update(buffer,0,n);}}
            if(count!=size)throw new IllegalStateException("Evidence changed during byte read");
        }
        return new Snapshot(HexFormat.of().formatHex(hash.digest()),bytes,leaves.size());
    }
    private static void collect(Path root,String name,Map<String,Path> leaves,boolean classesOnly)throws Exception {
        if(!Files.exists(root,LinkOption.NOFOLLOW_LINKS)){if(classesOnly)throw new IllegalStateException("Implementation missing");leaves.put(name+"/MISSING",null);return;}
        Path real=unlinkedRealPath(root);
        leaves.put(name+"/PRESENT",null);
        try(var stream=Files.walk(root)) {
            var iterator=stream.iterator();while(iterator.hasNext()){
                Path path=iterator.next();if(Files.isSymbolicLink(path)||!path.toRealPath().startsWith(real))throw new IllegalStateException("Linked evidence refused");
                if(Files.isDirectory(path,LinkOption.NOFOLLOW_LINKS))continue;
                if(!Files.isRegularFile(path,LinkOption.NOFOLLOW_LINKS))throw new IllegalStateException("Non-regular evidence refused");
                if(classesOnly&&Files.isDirectory(root)&&!path.toString().endsWith(".class"))continue;
                // Lock file contents are empty coordination, not evidence. Include pending files conservatively.
                if(path.getFileName().toString().endsWith(".lock"))continue;
                leaves.put(name+"/"+root.relativize(path).toString().replace('\\','/'),path);
                if(leaves.size()>MAX_FILES)throw new TooLarge();
            }
        }
    }
    // Windows 8.3 spellings (for example RUNNER~1) are aliases, not links. Check
    // every actual path component before canonicalizing; junctions/reparse points
    // and symbolic links remain refused, including links in an ancestor directory.
    static Path unlinkedRealPath(Path root)throws Exception {
        Path absolute=root.toAbsolutePath().normalize(), current=absolute.getRoot();
        for(Path part:absolute){current=current.resolve(part);var a=Files.readAttributes(current,java.nio.file.attribute.BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
            if(a.isSymbolicLink()||a.isOther())throw new IllegalStateException("Evidence memo does not follow linked paths");}
        return absolute.toRealPath();
    }
}

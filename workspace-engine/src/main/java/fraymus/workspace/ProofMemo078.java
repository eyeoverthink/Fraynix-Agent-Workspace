package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** Boot-local proof reuse. Bytes, not file timestamps, decide whether a previous audit still applies. */
final class ProofMemo078 {
    static final String VERSION="owned-proof-memo-078-v1";
    static final int CAPACITY=16,MAX_FILES=32;
    interface Audit {void run() throws Exception;}
    private final Path root;
    private final LinkedHashMap<String,JSONObject> entries=new LinkedHashMap<>(16,.75f,true);
    ProofMemo078(Path root){this.root=root.toAbsolutePath().normalize();}
    JSONArray snapshot(List<Path> paths)throws Exception {
        if(paths.isEmpty()||paths.size()>MAX_FILES)throw new IllegalArgumentException("Bounded nonempty evidence set required");
        Path realRoot=root.toRealPath();var result=new JSONArray();var seen=new HashSet<String>();long total=0;
        for(Path item:paths){
            Path p=item.toAbsolutePath().normalize();
            if(!p.startsWith(root)||!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)||!p.toRealPath().equals(realRoot.resolve(root.relativize(p))))throw new IllegalStateException("Evidence must be an owned regular file without redirected components");
            String relative=root.relativize(p).toString().replace('\\','/');if(!seen.add(relative))throw new IllegalArgumentException("Duplicate evidence file");
            var digest=MessageDigest.getInstance("SHA-256");long size=0;
            try(var input=Files.newInputStream(p)){byte[] bytes=new byte[65536];int n;while((n=input.read(bytes))!=-1){size+=n;total+=n;if(total>512_000_000L)throw new IllegalStateException("Proof evidence exceeds bounded read budget");digest.update(bytes,0,n);}}
            result.put(new JSONObject().put("path",relative).put("bytes",size).put("sha256",HexFormat.of().formatHex(digest.digest())));
        }
        return result;
    }
    synchronized JSONObject check(JSONObject binding,List<Path> paths,boolean reuse,Audit audit)throws Exception {
        long began=System.nanoTime();String at=Instant.now().toString(),key=WorldFrames.hash(binding);
        if(!reuse){long auditAt=System.nanoTime();audit.run();return new JSONObject().put("version",VERSION).put("key",key).put("checkedAt",at).put("binding",WorldFrames.copy(binding)).put("evidence",new JSONArray()).put("mode","full").put("reason","Original uncached verification; cache untouched").put("auditMillis",(System.nanoTime()-auditAt)/1e6).put("hashMillis",0d).put("totalMillis",(System.nanoTime()-began)/1e6);}
        long hashAt=System.nanoTime();var before=snapshot(paths);double hashMs=(System.nanoTime()-hashAt)/1e6;
        JSONObject prior=reuse?entries.get(key):null;
        var receipt=new JSONObject().put("version",VERSION).put("key",key).put("checkedAt",at).put("binding",WorldFrames.copy(binding)).put("evidence",before);
        if(prior!=null&&prior.getJSONArray("evidence").similar(before))return receipt.put("mode","hit").put("originCheckedAt",prior.getString("checkedAt")).put("originReceiptSha256",WorldFrames.hash(prior)).put("auditMillis",0d).put("hashMillis",hashMs).put("totalMillis",(System.nanoTime()-began)/1e6);
        entries.remove(key);long auditAt=System.nanoTime();audit.run();double auditMs=(System.nanoTime()-auditAt)/1e6;hashAt=System.nanoTime();var after=snapshot(paths);hashMs+=(System.nanoTime()-hashAt)/1e6;
        if(!before.similar(after))throw new IllegalStateException("Evidence changed during its full audit");
        receipt.put("mode","full").put("reason",!reuse?"Uncached control":prior==null?"Cold or changed source binding":"Changed evidence; complete recheck required").put("auditMillis",auditMs).put("hashMillis",hashMs).put("totalMillis",(System.nanoTime()-began)/1e6);
        if(reuse){entries.put(key,WorldFrames.copy(receipt));while(entries.size()>CAPACITY)entries.remove(entries.keySet().iterator().next());}
        return receipt;
    }
    synchronized int size(){return entries.size();}
}

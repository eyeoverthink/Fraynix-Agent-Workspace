package fraymus.workspace;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Parallelizes byte verification, never the authority decision. No evidence is cached. */
final class CalibrationScan081 implements AutoCloseable {
    private final ProofMemo078 proofs;
    private final ExecutorService readers=Executors.newFixedThreadPool(2,r->{
        var t=new Thread(r,"calibration-evidence-081");t.setDaemon(true);return t;
    });
    private boolean closed,reference;
    private long calls,failures,verifiedBytes;
    private double hashMillis,loadMillis;
    CalibrationScan081(Path root){proofs=new ProofMemo078(root);}
    synchronized boolean reference(){return reference;}
    synchronized void testMode(String mode){
        if(closed||!Set.of("reference","parallel").contains(mode))throw new IllegalArgumentException("Unknown or closed evidence reader");
        reference=mode.equals("reference");calls=failures=verifiedBytes=0;hashMillis=loadMillis=0;
    }
    synchronized JSONObject stats(){return new JSONObject().put("mode",reference?"reference":"parallel").put("readers",reference?1:2)
        .put("loads",calls).put("failures",failures).put("verifiedBytes",verifiedBytes).put("hashMillis",hashMillis).put("loadMillis",loadMillis)
        .put("scope","Calibration loads only; every successful load rereads every original evidence byte; no cached trust");}
    synchronized void record(double total,double hashing,long bytes,boolean success){calls++;loadMillis+=total;hashMillis+=hashing;if(success)verifiedBytes+=bytes;else failures++;}
    synchronized JSONArray serial(List<Path> paths)throws Exception{
        if(closed)throw new IllegalStateException("Evidence reader is closed");return proofs.snapshot(paths);
    }
    synchronized Map<String,JSONArray> parallel(Map<String,List<Path>> groups)throws Exception{
        if(closed)throw new IllegalStateException("Evidence reader is closed");
        if(groups.isEmpty()||groups.size()>16)throw new IllegalArgumentException("One to sixteen evidence groups required");
        var keys=new ArrayList<String>();var tasks=new ArrayList<Callable<JSONArray>>();
        for(var entry:groups.entrySet()){
            var paths=List.copyOf(entry.getValue());
            if(paths.isEmpty()||paths.size()>ProofMemo078.MAX_FILES)throw new IllegalArgumentException("Bounded nonempty evidence set required");
            keys.add(entry.getKey());tasks.add(()->proofs.snapshot(paths));
        }
        // Await every task, even after failure or caller interruption. No reader outlives its check.
        var futures=new ArrayList<Future<JSONArray>>();for(var task:tasks)futures.add(readers.submit(task));
        boolean interrupted=Thread.interrupted();Exception failure=null;var result=new LinkedHashMap<String,JSONArray>();
        for(int i=0;i<futures.size();i++)for(;;)try{result.put(keys.get(i),futures.get(i).get());break;}
        catch(InterruptedException e){interrupted=true;}
        catch(ExecutionException e){if(failure==null)failure=e.getCause() instanceof Exception ex?ex:new IllegalStateException(e.getCause());break;}
        interrupted|=Thread.interrupted();
        if(interrupted){Thread.currentThread().interrupt();throw new InterruptedException("Evidence verification interrupted; all readers joined");}
        if(failure!=null)throw failure;return result;
    }
    @Override public synchronized void close(){closed=true;readers.shutdown();}
}

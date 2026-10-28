package nervous;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.concurrent.*;

/** Managed construction and checkpoint I/O; simulation state stays on the engine thread. */
public final class BrainRuntime {
    private static final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Fraynix-brain-state");t.setDaemon(true);return t;});
    private static CompletableFuture<NeuromorphicCore> loading;
    private static CompletableFuture<?> saving;
    private static NeuromorphicCore core;
    private static String status="Nervous system idle";
    private static boolean learning;
    private static double milliseconds;
    private static Path checkpoint;
    private BrainRuntime() {}
    public static NeuromorphicCore core() { return core; }
    public static String status() { return status; }
    public static boolean learning() { return learning; }
    public static void learning(boolean value) { learning=value; }
    public static boolean busy() { return loading!=null || saving!=null; }
    public static double milliseconds() { return milliseconds; }
    public static void project(Path project) { checkpoint=project.resolve("brain/cortex-v1.json"); }
    public static void start() {
        if(core!=null || loading!=null) return;
        if(checkpoint!=null && Files.isRegularFile(checkpoint)) { load(checkpoint); return; }
        status="Growing seeded Hrm cortex...";
        loading=CompletableFuture.supplyAsync(()->new NeuromorphicCore(32768,32,618033988L),worker);
    }
    public static void poll() {
        if(loading!=null && loading.isDone()) {
            try { core=loading.join(); status="Cortex ready; sensory dynamics, experimental plasticity"; }
            catch(CompletionException error) { status="Cortex failed: "+error.getCause().getMessage(); }
            loading=null;
        }
        if(saving!=null && saving.isDone()) {
            try { saving.join(); status="Brain checkpoint saved"; }
            catch(CompletionException error) { status="Brain save failed: "+error.getCause().getMessage(); }
            saving=null;
        }
    }
    public static void observe(float[] sensory) {
        if(core==null) return;
        long begin=System.nanoTime(); core.inject(sensory); core.tick(learning); milliseconds=(System.nanoTime()-begin)/1e6;
    }
    public static void save(Path path) {
        if(core==null || busy()) throw new IllegalStateException("Brain not ready");
        var state=core.snapshot(); status="Saving brain checkpoint...";
        saving=CompletableFuture.runAsync(()->{
            try {
                Files.createDirectories(path.getParent()); Path temp=Files.createTempFile(path.getParent(),"cortex-",".pending");
                try {
                    try(var out=Files.newBufferedWriter(temp)) { new Gson().toJson(state,out); }
                    try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                    catch(AtomicMoveNotSupportedException error) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
                } finally { Files.deleteIfExists(temp); }
            } catch(Exception error) { throw new CompletionException(error); }
        },worker);
    }
    public static void load(Path path) {
        if(busy()) throw new IllegalStateException("Brain I/O in progress");
        status="Loading brain checkpoint...";
        loading=CompletableFuture.supplyAsync(()->{
            try {
                if(Files.size(path)>128L*1024*1024) throw new IllegalArgumentException("Brain checkpoint exceeds format limit");
                try(var reader=Files.newBufferedReader(path)) { return NeuromorphicCore.restore(new Gson().fromJson(reader,NeuromorphicCore.State.class)); }
            } catch(Exception error) { throw new CompletionException(error); }
        },worker);
    }
    public static void saveLearning() { if(learning && core!=null && !busy() && checkpoint!=null) save(checkpoint); }
    public static void close() {
        saveLearning(); worker.shutdown();
        try { if(!worker.awaitTermination(10,TimeUnit.SECONDS)) worker.shutdownNow(); }
        catch(InterruptedException error) { worker.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}

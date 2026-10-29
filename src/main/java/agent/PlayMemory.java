package agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Small, inspectable contextual bandit. Learns measured motor outcomes, not model weights. */
public final class PlayMemory {
    public enum Strategy {
        BALANCED(.42f,2.4f,28), EARLY_JUMP(.64f,2.8f,28), SHORT_HOP(.34f,1.8f,18);
        public final float wallLookahead, enemyLookahead;
        public final int jumpTicks;
        Strategy(float wall,float enemy,int jump) {wallLookahead=wall;enemyLookahead=enemy;jumpTicks=jump;}
    }
    public static final class Estimate { public int visits; public double reward; }
    public static final class Cell { public Estimate[] options={new Estimate(),new Estimate(),new Estimate()}; }
    public static final class Level {
        public int episodes, wins, deaths, pickups, damage;
        public float bestProgress;
        public Map<String,Cell> cells=new TreeMap<>();
        public Map<String,Integer> failedGoals=new TreeMap<>();
        public List<Result> recent=new ArrayList<>();
    }
    // Ordinary DTO fields keep this checkpoint compatible with the engine's Gson 2.8 reader.
    public static final class Result {
        public int episode,pickups,damage; public String outcome; public float progress; public long ticks;
        public Map<String,String> strategies;
        public Result(int episode,String outcome,float progress,int pickups,int damage,long ticks,Map<String,String> strategies) {
            this.episode=episode;this.outcome=outcome;this.progress=progress;this.pickups=pickups;
            this.damage=damage;this.ticks=ticks;this.strategies=new LinkedHashMap<>(strategies);
        }
        public String outcome(){return outcome;} public float progress(){return progress;}
        public int pickups(){return pickups;} public int damage(){return damage;}
        public Map<String,String> strategies(){return Collections.unmodifiableMap(strategies);}
    }
    public static final class State { public int version=1; public Map<String,Level> levels=new TreeMap<>(); }
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private State state=new State();
    private final Path path;
    private String ioStatus="New play memory";
    public PlayMemory(Path path) {
        this.path=path;
        if(path!=null && Files.isRegularFile(path)) {
            try {
                if(Files.size(path)>8*1024*1024) throw new IOException("Memory exceeds format limit");
                State loaded=JSON.fromJson(Files.readString(path,StandardCharsets.UTF_8),State.class);
                validate(loaded); state=loaded;ioStatus="Play memory restored";
            } catch(Exception error) {
                // Preserve the original even if a future save succeeds.
                try {Files.copy(path,path.resolveSibling(path.getFileName()+".invalid-"+UUID.randomUUID()));}
                catch(IOException backupFailure) {throw new IllegalStateException("Cannot preserve unreadable play memory",backupFailure);}
                ioStatus="Unreadable memory backed up; starting fresh: "+error.getMessage();
            }
        }
    }
    private static void validate(State state) throws IOException {
        if(state==null || state.version!=1 || state.levels==null || state.levels.size()>64) throw new IOException("Unsupported play memory");
        for(var level:state.levels.values()) {
            if(level==null || level.cells==null || level.cells.size()>2048 || level.recent==null || level.recent.size()>20
                || !Float.isFinite(level.bestProgress)) throw new IOException("Invalid level memory");
            if(level.failedGoals==null)level.failedGoals=new TreeMap<>();
            if(level.failedGoals.size()>2048)throw new IOException("Too many goal memories");
            for(var cell:level.cells.values()) {
                if(cell==null || cell.options==null || cell.options.length!=Strategy.values().length) throw new IOException("Invalid policy cell");
                for(var value:cell.options) if(value==null || value.visits<0 || !Double.isFinite(value.reward)) throw new IOException("Invalid policy reward");
            }
        }
    }
    public static String fingerprint(String authored) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(authored.getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException impossible) {throw new AssertionError(impossible);}
    }
    public Level level(String key) {
        if(!state.levels.containsKey(key) && state.levels.size()>=64) state.levels.remove(state.levels.keySet().iterator().next());
        return state.levels.computeIfAbsent(key,k->new Level());
    }
    public Strategy choose(String key,String context) {
        Level level=level(key);
        if(!level.cells.containsKey(context) && level.cells.size()>=2048) level.cells.remove(level.cells.keySet().iterator().next());
        Cell cell=level.cells.computeIfAbsent(context,k->new Cell());
        // Retain a demonstrated useful route. Explore when a context is new or its result was poor.
        int champion=-1;double bestMean=.25;
        for(int i=0;i<cell.options.length;i++) {
            Estimate value=cell.options[i];
            if(value.visits>0 && value.reward/value.visits>bestMean) {champion=i;bestMean=value.reward/value.visits;}
        }
        if(champion>=0) return Strategy.values()[champion];
        for(int i=0;i<cell.options.length;i++) if(cell.options[i].visits==0) return Strategy.values()[i];
        int total=Arrays.stream(cell.options).mapToInt(v->v.visits).sum(),best=0;
        double score=Double.NEGATIVE_INFINITY;
        for(int i=0;i<cell.options.length;i++) {
            Estimate value=cell.options[i];
            double next=value.reward/value.visits+1.25*Math.sqrt(Math.log(total+1d)/value.visits);
            if(next>score) {score=next;best=i;}
        }
        return Strategy.values()[best];
    }
    public void credit(String key,String context,Strategy strategy,double reward) {
        if(!Double.isFinite(reward)) throw new IllegalArgumentException("Nonfinite reward");
        level(key).cells.computeIfAbsent(context,k->new Cell());
        Estimate value=level(key).cells.get(context).options[strategy.ordinal()];
        value.visits++;value.reward+=Math.max(-12,Math.min(12,reward));
    }
    public void record(String key,Result result) {
        Level level=level(key);level.episodes++;level.bestProgress=Math.max(level.bestProgress,result.progress());
        if(result.outcome().equals("win") || result.outcome().equals("target")) level.wins++;
        if(result.outcome().equals("death")) level.deaths++;
        level.pickups+=result.pickups();level.damage+=result.damage();level.recent.add(result);
        if(level.recent.size()>20) level.recent.remove(0);
        save();
    }
    public String ioStatus() {return ioStatus;}
    public void save() {
        if(path==null) return;
        try {
            Files.createDirectories(path.getParent());Path temp=Files.createTempFile(path.getParent(),"play-memory-",".pending");
            try {
                Files.writeString(temp,JSON.toJson(state),StandardCharsets.UTF_8);
                try {Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
                catch(AtomicMoveNotSupportedException error) {Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
            } finally {Files.deleteIfExists(temp);}
            ioStatus="Play memory saved";
        } catch(IOException error) {ioStatus="Play memory save failed: "+error.getMessage();System.err.println(ioStatus);}
    }
}

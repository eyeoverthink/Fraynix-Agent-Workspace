package agent;

import org.junit.Test;
import java.nio.file.*;
import java.util.Map;
import static org.junit.Assert.*;

public class PlayMemoryTest {
    @Test public void failedChoiceChangesNextRunAndSurvivesRestart() throws Exception {
        Path path=Files.createTempDirectory("play-memory-test").resolve("memory.json");
        var memory=new PlayMemory(path);String level=PlayMemory.fingerprint("same authored level");
        var first=memory.choose(level,"pipe");assertEquals(PlayMemory.Strategy.BALANCED,first);
        memory.credit(level,"pipe",first,-8);
        memory.record(level,new PlayMemory.Result(1,"death",3,0,0,100,Map.of("pipe",first.name())));
        var loaded=new PlayMemory(path);
        assertEquals(1,loaded.level(level).episodes);assertEquals(1,loaded.level(level).deaths);
        assertEquals(PlayMemory.Strategy.EARLY_JUMP,loaded.choose(level,"pipe"));
        assertEquals(PlayMemory.Strategy.BALANCED,loaded.choose(PlayMemory.fingerprint("different world"),"pipe"));
    }
    @Test public void measuredRewardFavorsSuccessfulPolicyAfterExploration() {
        var memory=new PlayMemory(null);
        for(var strategy:PlayMemory.Strategy.values()) memory.credit("level","gap",strategy,strategy==PlayMemory.Strategy.SHORT_HOP?6:-8);
        assertEquals(PlayMemory.Strategy.SHORT_HOP,memory.choose("level","gap"));
    }
    @Test public void keepsUsefulKnownRouteInsteadOfRandomlyChangingEverySuccessfulAction() {
        var memory=new PlayMemory(null);memory.credit("level","safe pipe",PlayMemory.Strategy.BALANCED,3);
        assertEquals(PlayMemory.Strategy.BALANCED,memory.choose("level","safe pipe"));
    }
    @Test public void corruptMemoryIsPreservedBeforeReplacement() throws Exception {
        Path path=Files.createTempDirectory("invalid-play-memory").resolve("memory.json");
        Files.writeString(path,"bad json");var memory=new PlayMemory(path);memory.save();
        try(var files=Files.list(path.getParent())) {
            Path backup=files.filter(p->p.getFileName().toString().contains(".invalid-")).findFirst().orElseThrow();
            assertEquals("bad json",Files.readString(backup));
        }
    }
}

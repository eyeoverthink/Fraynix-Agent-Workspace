package agent;

import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class ActorControlsTest {
    @Test public void rejectsOldEpisodesDuplicateSequencesAndExpiredCommands() {
        var bus=new ActorControls(); UUID episode=UUID.randomUUID(), actor=UUID.randomUUID(); bus.reset(episode,actor);
        var input=new ActorControls.Input(1,0,true,false);
        var lease=new ActorControls.Lease(episode,actor,0,10,12,input);
        assertTrue(bus.accept(lease,10)); assertEquals(input,bus.at(11)); assertEquals(ActorControls.Input.NONE,bus.at(13));
        assertFalse(bus.accept(lease,10));
        assertFalse(bus.accept(new ActorControls.Lease(UUID.randomUUID(),actor,2,10,12,input),10));
        assertFalse(bus.accept(new ActorControls.Lease(episode,UUID.randomUUID(),2,10,12,input),10));
        assertFalse(bus.accept(new ActorControls.Lease(episode,actor,2,10,11,input),12));
        assertTrue(bus.accept(new ActorControls.Lease(episode,actor,2,12,13,input),12));
        bus.release(); assertEquals(ActorControls.Input.NONE,bus.at(12));
    }
}

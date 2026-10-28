package nervous;

import org.junit.Test;
import static org.junit.Assert.*;

public class NeuromorphicCoreTest {
    @Test public void seededCoreAndCheckpointContinueIdentically() {
        var a=new NeuromorphicCore(128,8,123); var b=new NeuromorphicCore(128,8,123);
        for(int i=0;i<12;i++) { a.inject(new float[]{1,-1,.4f}); a.tick(true); b.inject(new float[]{1,-1,.4f}); b.tick(true); }
        assertArrayEquals(a.snapshot().weights,b.snapshot().weights,0);
        String json=new com.google.gson.Gson().toJson(a.snapshot());
        var restored=NeuromorphicCore.restore(new com.google.gson.Gson().fromJson(json,NeuromorphicCore.State.class));
        for(int i=0;i<5;i++) { a.inject(new float[]{-.3f,1}); a.tick(true); restored.inject(new float[]{-.3f,1}); restored.tick(true); }
        assertArrayEquals(a.snapshot().membrane,restored.snapshot().membrane,0);
        assertArrayEquals(a.snapshot().weights,restored.snapshot().weights,0);
        assertEquals(a.snapshot().randomState,restored.snapshot().randomState);
    }
    @Test public void bothTimingDirectionsChangeWeightsAndFrozenEvaluationDoesNot() {
        assertTrue(NeuromorphicCore.timingDelta(2,1)>0); assertTrue(NeuromorphicCore.timingDelta(-2,1)<0);
        assertEquals(0,NeuromorphicCore.timingDelta(0,1),0);
        var core=new NeuromorphicCore(64,4,88); var before=core.snapshot().weights;
        for(int i=0;i<20;i++) { core.inject(new float[]{1,-1}); core.tick(false); }
        assertArrayEquals(before,core.snapshot().weights,0); assertEquals(20,core.ticks());
        core.snapshot().weights[0]=Float.NaN;
        assertTrue(Float.isFinite(core.snapshot().weights[0]));
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsCorruptCheckpoint() {
        var s=new NeuromorphicCore(64,4,88).snapshot(); s.targets[0]=999; NeuromorphicCore.restore(s);
    }
    @Test public void causalAndAnticausalSpikePairsChangeActualSynapsesInOppositeDirections() {
        var initial=new NeuromorphicCore(64,1,44).snapshot();
        initial.tick=10; initial.targets[0]=1; initial.lastSpike[0]=9; initial.membrane[1]=0;
        float before=initial.weights[0]; var causal=NeuromorphicCore.restore(initial); causal.tick(true);
        assertTrue(causal.snapshot().weights[0]>before);
        initial.lastSpike[0]=-100; initial.lastSpike[1]=9; initial.membrane[0]=0; initial.membrane[1]=-65;
        var anticausal=NeuromorphicCore.restore(initial); anticausal.tick(true);
        assertTrue(anticausal.snapshot().weights[0]<before);
    }
}

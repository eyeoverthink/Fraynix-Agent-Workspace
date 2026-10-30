package fraymus.neural;

import java.util.Arrays;

/**
 * Headless adaptation of fraymus.neural.HrmNeuromorphic from the user's H: source.
 * Preserves LIF leak, threshold recovery, sparse weighted routing and calcium modulation.
 * Uses a simulation clock, seeded noise, and causal pre/post timing in both directions.
 * No AWT, native resources, desktop capture, executors, or autonomous loops belong here.
 */
public class NeuromorphicCore {
    public static final int DEFAULT_NEURONS=32768, DEFAULT_CONNECTIONS=32;
    public static final class State {
        public int version=1, neurons, connections;
        public long seed, randomState, tick;
        public float[] membrane, threshold, weights, calcium;
        public long[] lastSpike;
        public int[] targets;
    }
    private final int neurons, connections;
    private final long seed;
    private long randomState, tick;
    private final float[] membrane, threshold, weights, calcium;
    private final long[] lastSpike;
    private final int[] targets;
    private final boolean[] spikes;
    private int spikeCount;
    private float plasticity;

    public NeuromorphicCore(int neurons, int connections, long seed) { this(neurons,connections,seed,true); }
    private NeuromorphicCore(int neurons, int connections, long seed, boolean initialize) {
        if (neurons<16 || neurons>32768 || connections<1 || connections>64) throw new IllegalArgumentException("Invalid cortex dimensions");
        this.neurons=neurons; this.connections=connections; this.seed=seed;
        randomState=seed==0?0x9e3779b97f4a7c15L:seed;
        membrane=new float[neurons]; threshold=new float[neurons]; lastSpike=new long[neurons]; spikes=new boolean[neurons];
        targets=new int[neurons*connections]; weights=new float[targets.length]; calcium=new float[Math.min(1024,neurons)];
        Arrays.fill(membrane,-65); Arrays.fill(threshold,-30); Arrays.fill(lastSpike,-1000000);
        if (initialize) grow();
    }
    private long random() { long x=randomState; x^=x<<13; x^=x>>>7; x^=x<<17; return randomState=x; }
    private float uniform() { return (random()>>>40)*0x1.0p-24f; }
    private int index(int bound) { return (int)Long.remainderUnsigned(random(),bound); }
    private void grow() {
        double[] x=new double[neurons],y=new double[neurons],z=new double[neurons];
        double phi=Math.PI*(3-Math.sqrt(5));
        // Compute all coordinates before wiring so later-index neurons have real positions.
        for(int i=0;i<neurons;i++) {
            double vertical=1-2.0*i/(neurons-1),radius=Math.sqrt(1-vertical*vertical),angle=phi*i;
            double shell=i<neurons*.2?15+uniform()*15:60+uniform()*20;
            x[i]=Math.cos(angle)*radius*shell; y[i]=vertical*shell; z[i]=Math.sin(angle)*radius*shell;
        }
        for(int i=0;i<neurons;i++) for(int c=0;c<connections;c++) {
            int target;
            int attempts=0;
            do {
                target=index(neurons); double dx=x[i]-x[target],dy=y[i]-y[target],dz=z[i]-z[target];
                double limit=uniform()<.9f?25:100;
                if(target!=i && (dx*dx+dy*dy+dz*dz<limit*limit || attempts>50)) break;
                attempts++;
            } while(true);
            targets[i*connections+c]=target; weights[i*connections+c]=uniform()*1.5f+.1f;
        }
        for(int i=0;i<calcium.length;i++) calcium[i]=uniform();
    }
    /** Each feature has separate positive and negative sensory populations. */
    public void inject(float[] features) {
        for(int f=0;f<features.length;f++) {
            float value=features[f]; if(!Float.isFinite(value)) throw new IllegalArgumentException("Nonfinite sensory feature");
            int group=f*2+(value<0?1:0);
            for(int j=0;j<16;j++) membrane[(group*16+j)%neurons]+=Math.min(1,Math.abs(value))*45;
        }
    }
    /** Positive delta means pre fired before post. Zero excludes simultaneous spikes. */
    public static float timingDelta(long postMinusPre, float modulation) {
        if(postMinusPre>0 && postMinusPre<10) return .03f*(float)Math.exp(-postMinusPre/10.0)*modulation;
        if(postMinusPre<0 && postMinusPre>-15) return -.01f*(float)Math.exp(postMinusPre/15.0)*modulation;
        return 0;
    }
    public void tick(boolean learning) {
        tick++; spikeCount=0; plasticity=0;
        for(int i=0;i<calcium.length;i++) calcium[i]=Math.max(0,Math.min(1,calcium[i]*.98f+(float)Math.sin(tick*.02+i)*.002f));
        for(int i=0;i<neurons;i++) {
            membrane[i]=membrane[i]*.85f-9.75f;
            threshold[i]+=(-30-threshold[i])*.05f;
            if(uniform()<.0002f) membrane[i]+=40;
            spikes[i]=membrane[i]>=threshold[i];
            if(spikes[i]) { spikeCount++; membrane[i]=-85; threshold[i]+=5; }
        }
        for(int pre=0;pre<neurons;pre++) for(int c=0;c<connections;c++) {
            int syn=pre*connections+c,post=targets[syn],astro=pre%calcium.length;
            float modulation=1+calcium[astro]*2;
            if(spikes[pre]) membrane[post]+=weights[syn]*modulation;
            if(learning) {
                float delta=0;
                if(spikes[post] && !spikes[pre]) delta+=timingDelta(tick-lastSpike[pre],modulation);
                if(spikes[pre] && !spikes[post]) delta+=timingDelta(lastSpike[post]-tick,modulation);
                float updated=Math.max(0,Math.min(10,weights[syn]+delta));
                plasticity+=Math.abs(updated-weights[syn]); weights[syn]=updated;
            }
        }
        for(int i=0;i<neurons;i++) if(spikes[i]) { lastSpike[i]=tick; int astro=i%calcium.length; calcium[astro]=Math.min(1,calcium[astro]+.005f); }
    }
    /** Population output available to an experimental motor readout. */
    public float output(int population) {
        int fired=0;
        for(int j=0;j<64;j++) if(spikes[Math.floorMod(neurons-1-population*64-j,neurons)]) fired++;
        return fired/64f;
    }
    public int neurons() { return neurons; }
    public int synapses() { return weights.length; }
    public long ticks() { return tick; }
    public int spikeCount() { return spikeCount; }
    public float plasticity() { return plasticity; }
    public State snapshot() {
        State s=new State(); s.neurons=neurons; s.connections=connections; s.seed=seed; s.randomState=randomState; s.tick=tick;
        s.membrane=membrane.clone(); s.threshold=threshold.clone(); s.lastSpike=lastSpike.clone();
        s.targets=targets.clone(); s.weights=weights.clone(); s.calcium=calcium.clone(); return s;
    }
    private static State valid(State s){if(s==null||s.version!=1||s.tick<0||s.randomState==0)throw new IllegalArgumentException("Unsupported cortex state");return s;}
    protected NeuromorphicCore(State s){
        this(valid(s).neurons,s.connections,s.seed,false);
        var core=this;
        copy(s.membrane,core.membrane); copy(s.threshold,core.threshold); copy(s.weights,core.weights); copy(s.calcium,core.calcium);
        if(s.lastSpike==null || s.lastSpike.length!=core.neurons || s.targets==null || s.targets.length!=core.targets.length) throw new IllegalArgumentException("Invalid cortex arrays");
        for(int i=0;i<s.targets.length;i++) if(s.targets[i]<0 || s.targets[i]>=core.neurons || s.weights[i]<0 || s.weights[i]>10) throw new IllegalArgumentException("Invalid synapse");
        for(long time:s.lastSpike) if(time>s.tick) throw new IllegalArgumentException("Spike from future tick");
        System.arraycopy(s.lastSpike,0,core.lastSpike,0,s.lastSpike.length); System.arraycopy(s.targets,0,core.targets,0,s.targets.length);
        core.randomState=s.randomState; core.tick=s.tick;
    }
    public static NeuromorphicCore restore(State s){return new NeuromorphicCore(s);}
    private static void copy(float[] from,float[] to) {
        if(from==null || from.length!=to.length) throw new IllegalArgumentException("Invalid cortex array");
        for(float value:from) if(!Float.isFinite(value)) throw new IllegalArgumentException("Nonfinite cortex state");
        System.arraycopy(from,0,to,0,to.length);
    }
}

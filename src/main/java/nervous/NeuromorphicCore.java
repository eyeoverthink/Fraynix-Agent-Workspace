package nervous;
/** Compatibility entry point. The implementation now lives in the shared, window-free brain-core module. */
public final class NeuromorphicCore extends fraymus.neural.NeuromorphicCore {
    public NeuromorphicCore(int neurons,int connections,long seed){super(neurons,connections,seed);}
    private NeuromorphicCore(State state){super(state);}
    public static NeuromorphicCore restore(State state){return new NeuromorphicCore(state);}
}

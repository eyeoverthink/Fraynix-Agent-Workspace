package fraymus.neural;
import java.util.Map;
/** Window-independent control contract; a rendering backend may still require a hidden GPU context. */
public interface ControlledRuntime {
    Map<String,Object> observe();
    Map<String,Object> step(int ticks,float horizontal,boolean jump);
}

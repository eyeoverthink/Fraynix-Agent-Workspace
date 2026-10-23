package fraymus.factory.models;

import java.util.*;

/** Operator settings, not a claim that a model's advertised context fits available RAM. */
public record ModelRunSettings(int context, int outputTokens, int threads, int gpuLayers,
                               int idleSeconds, int keepAliveSeconds, double temperature,double repeatPenalty,boolean clearCustomStops) {
    public ModelRunSettings(int context,int outputTokens,int threads,int gpuLayers,int idleSeconds,int keepAliveSeconds,double temperature){
        this(context,outputTokens,threads,gpuLayers,idleSeconds,keepAliveSeconds,temperature,1.1,false);
    }
    public ModelRunSettings {
        if (context < 512 || context > 1_048_576 || outputTokens < 1 || outputTokens > context
            || threads < 0 || threads > 512 || gpuLayers < -1 || gpuLayers > 1000
            || idleSeconds < 1 || idleSeconds > 86_400 || keepAliveSeconds < 0 || keepAliveSeconds > 86_400
            || !Double.isFinite(temperature) || temperature < 0 || temperature > 2
            || !Double.isFinite(repeatPenalty)||repeatPenalty<=0||repeatPenalty>10)
            throw new IllegalArgumentException("Invalid runtime settings: context >= 512, output <= context, finite temperature 0-2, positive timeout");
    }
    public static ModelRunSettings defaults(long bytes) {
        return new ModelRunSettings(bytes > 20_000_000_000L ? 4096 : 8192, 1024, 0, -1, 900, 600, .4);
    }
    public Map<String,Object> options() {
        var options = new LinkedHashMap<String,Object>();
        options.put("num_ctx", context); options.put("num_predict", outputTokens); options.put("temperature", temperature);options.put("repeat_penalty",repeatPenalty);
        if(clearCustomStops)options.put("stop",List.of());
        if (threads > 0) options.put("num_thread", threads);
        if (gpuLayers >= 0) options.put("num_gpu", gpuLayers);
        return Map.copyOf(options);
    }
}

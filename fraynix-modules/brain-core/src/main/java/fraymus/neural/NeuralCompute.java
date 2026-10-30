package fraymus.neural;
public interface NeuralCompute extends AutoCloseable {
    float[] matvec(float[] weights,float[] input,int rows,int columns);
    String device();
    default void close(){}
    static void validate(float[] weights,float[] input,int rows,int columns){
        if(rows<1||rows>4096||columns<1||columns>4096||weights.length!=(long)rows*columns||input.length!=columns)throw new IllegalArgumentException("Invalid bounded neural matrix dimensions");
        for(float value:weights)if(!Float.isFinite(value))throw new IllegalArgumentException("Nonfinite weight");
        for(float value:input)if(!Float.isFinite(value))throw new IllegalArgumentException("Nonfinite input");
    }
}

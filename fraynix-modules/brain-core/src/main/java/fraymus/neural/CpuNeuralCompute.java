package fraymus.neural;
public final class CpuNeuralCompute implements NeuralCompute {
    public float[] matvec(float[] weights,float[] input,int rows,int columns){
        NeuralCompute.validate(weights,input,rows,columns);float[] result=new float[rows];
        for(int r=0;r<rows;r++){float sum=0;for(int c=0;c<columns;c++)sum+=weights[r*columns+c]*input[c];result[r]=sum;}
        return result;
    }
    public String device(){return "Java CPU reference";}
}

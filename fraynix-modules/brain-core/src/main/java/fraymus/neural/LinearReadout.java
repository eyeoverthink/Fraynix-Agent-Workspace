package fraymus.neural;
public final class LinearReadout {
    private LinearReadout(){}
    public static float[] train(float[][] features,float[] labels,int epochs,float rate){
        if(features.length<4||features.length>256||labels.length!=features.length||epochs<1||epochs>20_000||!Float.isFinite(rate)||rate<=0||rate>.1f)throw new IllegalArgumentException("Invalid readout training budget");
        int n=features[0].length;if(n<1||n>64)throw new IllegalArgumentException("Invalid feature count");float[] weights=new float[n];
        for(int e=0;e<epochs;e++){
            float[] gradient=new float[n];
            for(int i=0;i<features.length;i++){
                if(features[i].length!=n||!Float.isFinite(labels[i]))throw new IllegalArgumentException("Invalid training row");
                float prediction=0;for(int j=0;j<n;j++){if(!Float.isFinite(features[i][j]))throw new IllegalArgumentException("Nonfinite feature");prediction+=weights[j]*features[i][j];}
                for(int j=0;j<n;j++)gradient[j]+=(prediction-labels[i])*features[i][j]/features.length;
            }
            for(int j=0;j<n;j++){weights[j]-=rate*gradient[j];if(!Float.isFinite(weights[j]))throw new IllegalStateException("Training diverged");}
        }
        return weights;
    }
}

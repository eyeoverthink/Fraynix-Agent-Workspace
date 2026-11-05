package fraymus.neural;

/** Deterministic ridge readout. No optimizer state is hidden outside the saved weights. */
public final class OutcomeReadout061 {
    private OutcomeReadout061() {}
    public static double[] features(double[] input, FraynixCortex061 cortex) {
        if(cortex==null)return input.clone();
        double[] projected=cortex.project(input),sparse=FraynixCortex061.fly(input,8);
        double[] all=new double[input.length+projected.length+sparse.length];
        System.arraycopy(input,0,all,0,input.length);
        System.arraycopy(projected,0,all,input.length,projected.length);
        System.arraycopy(sparse,0,all,input.length+projected.length,sparse.length);
        return all;
    }
    public static double[] train(double[][] x,double[] y) {
        if(x.length!=y.length||x.length<2||x.length>256)throw new IllegalArgumentException("Training count");
        int n=x[0].length;if(n<1||n>64)throw new IllegalArgumentException("Feature count");
        double[][] a=new double[n][n+1];
        for(int k=0;k<x.length;k++){
            if(x[k].length!=n||!Double.isFinite(y[k]))throw new IllegalArgumentException("Training shape/value");
            for(double v:x[k])if(!Double.isFinite(v))throw new IllegalArgumentException("Training value");
            for(int i=0;i<n;i++){a[i][n]+=x[k][i]*y[k];for(int j=0;j<n;j++)a[i][j]+=x[k][i]*x[k][j];}
        }
        for(int i=0;i<n;i++)a[i][i]+=.001;
        for(int i=0;i<n;i++){
            int pivot=i;for(int j=i+1;j<n;j++)if(Math.abs(a[j][i])>Math.abs(a[pivot][i]))pivot=j;
            double[] row=a[i];a[i]=a[pivot];a[pivot]=row;
            double d=a[i][i];for(int j=i;j<=n;j++)a[i][j]/=d;
            for(int k=0;k<n;k++)if(k!=i){double factor=a[k][i];for(int j=i;j<=n;j++)a[k][j]-=factor*a[i][j];}
        }
        double[] w=new double[n];for(int i=0;i<n;i++)w[i]=a[i][n];return w;
    }
    public static double predict(double[] w,double[] x){
        if(w.length!=x.length)throw new IllegalArgumentException("Readout shape");
        double result=0;for(int i=0;i<x.length;i++){if(!Double.isFinite(w[i])||!Double.isFinite(x[i]))throw new IllegalArgumentException("Readout value");result+=w[i]*x[i];}
        if(!Double.isFinite(result))throw new IllegalArgumentException("Readout overflow");return result;
    }
}

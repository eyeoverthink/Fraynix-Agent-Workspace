package fraymus.geometry;

/** Original JDK-only Euclidean operations. A projection is not higher-dimensional physics. */
public final class NDSpace {
    private NDSpace() {}
    public static int dimensions(int n) { if(n<2||n>16)throw new IllegalArgumentException("Dimensions must be2..16");return n; }
    public static double[] vector(double[] p) { dimensions(p.length);for(double x:p)if(!Double.isFinite(x))throw new IllegalArgumentException("Finite coordinates required");return p.clone(); }
    public static double distance(double[] a,double[] b) { dimensions(a.length);if(a.length!=b.length)throw new IllegalArgumentException("Dimension mismatch");double d=0;for(int k=0;k<a.length;k++){if(!Double.isFinite(a[k])||!Double.isFinite(b[k]))throw new IllegalArgumentException("Finite coordinates required");d=StrictMath.hypot(d,a[k]-b[k]);}return d; }
    public static double[] rotate(double[] p,int a,int b,double degrees) {
        double[] r=vector(p);if(a<0||b<0||a>=p.length||b>=p.length||a==b||!Double.isFinite(degrees)||Math.abs(degrees)>360)throw new IllegalArgumentException("Choose a finite rotation and two distinct axes");
        double angle=degrees*StrictMath.PI/180,c=StrictMath.cos(angle),s=StrictMath.sin(angle);r[a]=c*p[a]-s*p[b];r[b]=s*p[a]+c*p[b];return r;
    }
    /** Rotate the chosen plane, then mix remaining axes into a reproducible orthographic view. */
    public static double[] project(double[] point,int a,int b,double degrees) {
        double[] p=vector(point);for(int k=0;k<p.length;k++)p[k]-=.5;p=rotate(p,a,b,degrees);
        for(int k=2;k<p.length;k++){p=rotate(p,0,k,17);p=rotate(p,1,k,11);}return new double[]{p[0],p[1]};
    }
    public static void interrupted() { if(Thread.currentThread().isInterrupted())throw new IllegalStateException("Procedural computation interrupted"); }
}

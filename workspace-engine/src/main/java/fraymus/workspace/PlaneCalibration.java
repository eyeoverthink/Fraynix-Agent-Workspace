package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Metric interpretation of a fixed camera and a declared plane. No native authority. */
final class PlaneCalibration {
    static final String VERSION="metric-plane-037-v1";
    private final JSONObject definition;
    private final double[] h;
    PlaneCalibration(JSONObject input) {
        SpatialScene.keys(input,"mode","cameraKey","note","imagePoints","worldPoints","lengthM","imageAspect","anchorSigmaM","scaleRelativeSigma","tsai");
        definition=WorldFrames.copy(input);
        String mode=input.getString("mode");
        if(!input.getString("cameraKey").matches("[a-zA-Z0-9_-]{1,80}")||input.getString("note").isBlank()||input.getString("note").length()>1500)throw new IllegalArgumentException("Declare a fixed camera/plane key and measurement source");
        bounded(input.getDouble("anchorSigmaM"),0,.5);bounded(input.getDouble("scaleRelativeSigma"),0,.5);
        if(mode.equals("tsai")) {
            var t=input.getJSONObject("tsai");SpatialScene.keys(t,"focal","kappa1","cx","cy","sx","dpx","dpy","tx","ty","tz","rx","ry","rz","width","height","metresPerUnit");
            for(String k:t.keySet())if(!Double.isFinite(t.getDouble(k)))throw new IllegalArgumentException("Finite camera parameters required");
            bounded(t.getDouble("focal"),.01,1000);bounded(t.getDouble("dpx"),.000001,1);bounded(t.getDouble("dpy"),.000001,1);bounded(t.getDouble("sx"),.1,10);bounded(t.getDouble("metresPerUnit"),.000001,1);bounded(t.getDouble("width"),32,10000);bounded(t.getDouble("height"),32,10000);bounded(t.getDouble("kappa1"),0,1);
            h=null;
        } else {
            var a=input.getJSONArray("imagePoints");int n=mode.equals("ruler")?2:mode.equals("homography")?4:0;
            if(n==0||a.length()!=n)throw new IllegalArgumentException("Use two ruler endpoints or four planar corners");
            double[][] p=new double[n][2];for(int i=0;i<n;i++){p[i]=point(a.getJSONArray(i));for(double v:p[i])bounded(v,0,1);}
            if(n==2){double len=input.getDouble("lengthM"),aspect=input.getDouble("imageAspect");bounded(aspect,.2,5);bounded(len,.001,100);double dx=p[1][0]-p[0][0],dy=(p[1][1]-p[0][1])/aspect,d=dx*dx+dy*dy;if(d<.0004)throw new IllegalArgumentException("Ruler endpoints are too close");double sx=len*dx/d,sy=len*dy/d;h=new double[]{sx,sy/aspect,-sx*p[0][0]-sy*p[0][1]/aspect,-sy,sx/aspect,sy*p[0][0]-sx*p[0][1]/aspect,0,0,1};}
            else {var w=input.getJSONArray("worldPoints");if(w.length()!=4)throw new IllegalArgumentException("Four world-plane coordinates in metres required");double[][] q=new double[4][2];for(int i=0;i<4;i++){q[i]=point(w.getJSONArray(i));for(double v:q[i])bounded(v,-1000,1000);}convex(p,.001);convex(q,.000001);double[][] m=new double[8][9];for(int i=0;i<4;i++){double x=p[i][0],y=p[i][1],u=q[i][0],v=q[i][1];m[2*i]=new double[]{x,y,1,0,0,0,-u*x,-u*y,u};m[2*i+1]=new double[]{0,0,0,x,y,1,-v*x,-v*y,v};}h=solve(m);for(int i=0;i<4;i++)if(distance(project(p[i][0],p[i][1]),q[i])>1e-5)throw new IllegalArgumentException("Unstable planar calibration");}
        }
    }
    JSONObject definition(){return WorldFrames.copy(definition);}
    String hash(){return WorldFrames.hash(definition);}
    static double bounded(double x,double lo,double hi){if(!Double.isFinite(x)||x<lo||x>hi)throw new IllegalArgumentException("Measurement outside declared finite bounds");return x;}
    static double[] point(JSONArray a){if(a.length()!=2)throw new IllegalArgumentException("Two-dimensional coordinate required");double[] p={a.getDouble(0),a.getDouble(1)};for(double x:p)bounded(x,-10000,10000);return p;}
    static double distance(double[] a,double[] b){return Math.hypot(a[0]-b[0],a[1]-b[1]);}
    private static void convex(double[][] p,double min){double last=0,area=0;for(int i=0;i<4;i++){var a=p[i];var b=p[(i+1)%4];var c=p[(i+2)%4];double cross=(b[0]-a[0])*(c[1]-b[1])-(b[1]-a[1])*(c[0]-b[0]);if(Math.abs(cross)<min||last*cross<0)throw new IllegalArgumentException("Use four distinct convex corners in perimeter order");last=cross;area+=a[0]*b[1]-a[1]*b[0];}if(Math.abs(area)<min*2)throw new IllegalArgumentException("Plane corners are degenerate");}
    private static double[] solve(double[][] m){for(int c=0;c<8;c++){int pivot=c;for(int r=c+1;r<8;r++)if(Math.abs(m[r][c])>Math.abs(m[pivot][c]))pivot=r;double[] temp=m[c];m[c]=m[pivot];m[pivot]=temp;if(Math.abs(m[c][c])<1e-10)throw new IllegalArgumentException("Degenerate planar mapping");double d=m[c][c];for(int j=c;j<=8;j++)m[c][j]/=d;for(int r=0;r<8;r++)if(r!=c){d=m[r][c];for(int j=c;j<=8;j++)m[r][j]-=d*m[c][j];}}double[] h=new double[9];for(int i=0;i<8;i++)h[i]=m[i][8];h[8]=1;return h;}
    boolean inPlane(double x,double y){if(!definition.getString("mode").equals("homography"))return x>=0&&x<=1&&y>=0&&y<=1;var a=definition.getJSONArray("imagePoints");double sign=0;for(int i=0;i<4;i++){double[] p=point(a.getJSONArray(i)),q=point(a.getJSONArray((i+1)%4));double s=(q[0]-p[0])*(y-p[1])-(q[1]-p[1])*(x-p[0]);if(Math.abs(s)<1e-10)continue;if(sign*s<0)return false;sign=s;}return true;}
    double[] project(double x,double y){
        double[] p;
        if(h!=null){double d=h[6]*x+h[7]*y+h[8];if(Math.abs(d)<1e-7)throw new IllegalArgumentException("Point is at the plane horizon");p=new double[]{(h[0]*x+h[1]*y+h[2])/d,(h[3]*x+h[4]*y+h[5])/d};}
        else {var t=definition.getJSONObject("tsai");double xd=(x*t.getDouble("width")-t.getDouble("cx"))*t.getDouble("dpx")/t.getDouble("sx"),yd=(y*t.getDouble("height")-t.getDouble("cy"))*t.getDouble("dpy"),rad=1+t.getDouble("kappa1")*(xd*xd+yd*yd),u=xd*rad/t.getDouble("focal"),v=yd*rad/t.getDouble("focal");double[][] r=rotation(t);double a=r[0][0]-u*r[2][0],b=r[0][1]-u*r[2][1],c=u*t.getDouble("tz")-t.getDouble("tx"),d=r[1][0]-v*r[2][0],e=r[1][1]-v*r[2][1],f=v*t.getDouble("tz")-t.getDouble("ty"),det=a*e-b*d;if(Math.abs(det)<1e-7)throw new IllegalArgumentException("Plane horizon");double s=t.getDouble("metresPerUnit");p=new double[]{s*(c*e-b*f)/det,s*(a*f-c*d)/det};}
        for(double z:p)bounded(z,-10000,10000);return p;
    }
    static double[][] rotation(JSONObject t){double cx=Math.cos(t.getDouble("rx")),sx=Math.sin(t.getDouble("rx")),cy=Math.cos(t.getDouble("ry")),sy=Math.sin(t.getDouble("ry")),cz=Math.cos(t.getDouble("rz")),sz=Math.sin(t.getDouble("rz"));return new double[][]{{cy*cz,cz*sx*sy-cx*sz,sx*sz+cx*cz*sy},{cy*sz,cx*cz+sx*sy*sz,cx*sy*sz-cz*sx},{-sy,cy*sx,cx*cy}};}
    double sigma(double x,double y,double vx,double vy){double eps=1e-6;double[] a=project(x+eps,y),b=project(x-eps,y),c=project(x,y+eps),d=project(x,y-eps);double jx=Math.pow((a[0]-b[0])/(2*eps),2)+Math.pow((a[1]-b[1])/(2*eps),2),jy=Math.pow((c[0]-d[0])/(2*eps),2)+Math.pow((c[1]-d[1])/(2*eps),2);return Math.sqrt(jx*Math.max(0,vx)+jy*Math.max(0,vy));}
}

package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Measured plane coordinates -> explicitly placed native plane. No native mutation. */
final class MatRegistration041 {
    static final String VERSION="mat-native-similarity-041-v1";
    static double[] v3(JSONArray a){if(a.length()!=3)throw new IllegalArgumentException("Three finite native coordinates required");var p=new double[3];for(int i=0;i<3;i++)p[i]=PlaneCalibration.bounded(a.getDouble(i),-1000,1000);return p;}
    static double dot(double[] a,double[] b){double s=0;for(int i=0;i<a.length;i++)s+=a[i]*b[i];return s;}
    static double[] sub(double[] a,double[] b){var c=a.clone();for(int i=0;i<c.length;i++)c[i]-=b[i];return c;}
    static double distance(double[] a,double[] b){var c=sub(a,b);return Math.sqrt(dot(c,c));}
    static JSONObject basis(JSONObject p){
        SpatialScene.keys(p,"origin","u","v","source");if(p.getString("source").isBlank()||p.getString("source").length()>1500)throw new IllegalArgumentException("Declare native plane placement source");
        var u=v3(p.getJSONArray("u"));var v=v3(p.getJSONArray("v"));v3(p.getJSONArray("origin"));
        if(Math.abs(dot(u,u)-1)>1e-8||Math.abs(dot(v,v)-1)>1e-8||Math.abs(dot(u,v))>1e-8)throw new IllegalArgumentException("Native basis must be orthonormal, in metres");return WorldFrames.copy(p);
    }
    static double[] map(JSONObject fit,double[] p){
        var basis=fit.getJSONObject("basis");var o=v3(basis.getJSONArray("origin"));var u=v3(basis.getJSONArray("u"));var v=v3(basis.getJSONArray("v"));double a=fit.getDouble("a"),b=fit.getDouble("b"),x=fit.getDouble("tx")+a*p[0]-b*p[1],y=fit.getDouble("ty")+b*p[0]+a*p[1];
        for(int i=0;i<3;i++)o[i]+=x*u[i]+y*v[i];return o;
    }
    static JSONObject fit(JSONObject definition,JSONObject nativePlane,JSONArray landmarks,JSONObject tape){
        var plane=new PlaneCalibration(definition);if(!definition.getString("mode").equals("homography"))throw new IllegalArgumentException("Four measured mat corners required");basis(nativePlane);
        if(landmarks.length()<6||landmarks.length()>20)throw new IllegalArgumentException("Use3..10 fit and3..10 independent check landmarks");
        var origin=v3(nativePlane.getJSONArray("origin"));var u=v3(nativePlane.getJSONArray("u"));var v=v3(nativePlane.getJSONArray("v"));var fits=new ArrayList<double[]>();var rows=new JSONArray();var names=new HashSet<String>();var points=new ArrayList<double[]>();int checks=0;
        double[] mean=new double[4];
        for(Object raw:landmarks){var l=(JSONObject)raw;SpatialScene.keys(l,"name","role","image","native","source");if(l.getString("name").isBlank()||l.getString("name").length()>80||!names.add(l.getString("name"))||l.getString("source").isBlank()||l.getString("source").length()>1500)throw new IllegalArgumentException("Name and declare each independent physical landmark");
            var im=PlaneCalibration.point(l.getJSONArray("image"));for(var prior:points)if(PlaneCalibration.distance(prior,im)<.001)throw new IllegalArgumentException("Fit/check image points must be distinct");points.add(im);if(!plane.inPlane(im[0],im[1]))throw new IllegalArgumentException("Landmark outside measured mat");var p=plane.project(im[0],im[1]);var n=v3(l.getJSONArray("native"));var q=sub(n,origin);double[] row={p[0],p[1],dot(q,u),dot(q,v)};
            switch(l.getString("role")){case "fit"->{fits.add(row);for(int k=0;k<4;k++)mean[k]+=row[k];}case "check"->{checks++;for(Object corner:definition.getJSONArray("imagePoints"))if(PlaneCalibration.distance(im,PlaneCalibration.point((JSONArray)corner))<.001)throw new IllegalArgumentException("A calibration corner is not an independent withheld check");}default->throw new IllegalArgumentException("Landmark role must be fit or check");}
            rows.put(WorldFrames.copy(l).put("matM",new JSONArray(p)));
        }
        if(fits.size()<3||fits.size()>10||checks<3||checks>10)throw new IllegalArgumentException("Require3..10 fit and3..10 withheld check landmarks");for(int k=0;k<4;k++)mean[k]/=fits.size();double c=0,s=0,d=0,xx=0,yy=0,xy=0;
        for(var p:fits){double x=p[0]-mean[0],y=p[1]-mean[1],qx=p[2]-mean[2],qy=p[3]-mean[3];c+=x*qx+y*qy;s+=x*qy-y*qx;d+=x*x+y*y;xx+=x*x;yy+=y*y;xy+=x*y;}
        if(d<1e-6||xx*yy-xy*xy<1e-10)throw new IllegalArgumentException("Fit landmarks must span a nondegenerate two-dimensional measured area");double a=c/d,b=s/d,scale=Math.hypot(a,b);
        var result=new JSONObject().put("version",VERSION).put("basis",basis(nativePlane)).put("a",a).put("b",b).put("tx",mean[2]-a*mean[0]+b*mean[1]).put("ty",mean[3]-b*mean[0]-a*mean[1]).put("scale",scale).put("rotationDeg",Math.toDegrees(Math.atan2(b,a)));
        double fitSq=0,maxCheck=0;for(Object raw:rows){var row=(JSONObject)raw;var mapped=map(result,PlaneCalibration.point(row.getJSONArray("matM")));double error=distance(mapped,v3(row.getJSONArray("native")));row.put("mappedNativeM",new JSONArray(mapped)).put("errorM",error);if(row.getString("role").equals("fit"))fitSq+=error*error;else maxCheck=Math.max(maxCheck,error);}
        SpatialScene.keys(tape,"a","b","markA","markB","unit","source");if(tape.getString("source").isBlank()||tape.getString("source").length()>1500)throw new IllegalArgumentException("Declare independently read tape marks");var ta=PlaneCalibration.point(tape.getJSONArray("a"));var tb=PlaneCalibration.point(tape.getJSONArray("b"));if(!plane.inPlane(ta[0],ta[1])||!plane.inPlane(tb[0],tb[1]))throw new IllegalArgumentException("Tape marks outside measured mat");double unit=unit(tape.getString("unit")),known=Math.abs(PlaneCalibration.bounded(tape.getDouble("markB"),0,1000)-PlaneCalibration.bounded(tape.getDouble("markA"),0,1000))*unit;PlaneCalibration.bounded(known,.02,1);double measured=distance(map(result,plane.project(ta[0],ta[1])),map(result,plane.project(tb[0],tb[1]))),tapeError=Math.abs(measured-known),rms=Math.sqrt(fitSq/fits.size());
        return result.put("landmarks",rows).put("fitCount",fits.size()).put("checkCount",checks).put("fitRmsM",rms).put("maxCheckErrorM",maxCheck).put("tape",WorldFrames.copy(tape).put("expectedM",known).put("measuredM",measured).put("absoluteErrorM",tapeError)).put("passed",rms<=.003&&maxCheck<=.005&&tapeError<=.005&&scale>=.98&&scale<=1.02).put("heightKnown",false).put("physicalContactKnown",false).put("placementMeaning","Operator-declared native placement of a measured mat plane; not measured room pose or object height").put("uncertaintyMeaning","Residuals and declared calibration error budget, not a calibrated probability. Reused fit corners alone do not certify accuracy.");
    }
    static double unit(String unit){return switch(unit){case "in"->.0254;case "cm"->.01;case "m"->1;default->throw new IllegalArgumentException("Use in, cm or m");};}
}

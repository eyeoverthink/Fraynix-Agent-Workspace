package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Fixed-window displacement evaluation. Correlated image pairs are not independent trials. */
final class PlaneAccuracy160 {
    static final String LIMIT="One trial is one episode. Paired frames are correlated. Conditional geometric allowances are not calibrated probabilities. Unknown target height, parallax and mark alignment remain limitations. No live pose or action authority.";
    static JSONObject options(JSONObject in){
        SpatialScene.keys(in,"pixelRadius","annotationBudgetM","referenceBudgetM","maxErrorM","maxRadiusM");var d=WorldFrames.copy(in);
        String[] names={"pixelRadius","annotationBudgetM","referenceBudgetM","maxErrorM","maxRadiusM"};double[] defaults={2,.003,.003,.010,.040};double[][] ranges={{.5,8},{.001,.02},{.001,.02},{.001,.1},{.001,.15}};
        for(int i=0;i<names.length;i++){if(!d.has(names[i]))d.put(names[i],defaults[i]);SpatialScene.number(d,names[i],ranges[i][0],ranges[i][1]);}return d;
    }
    static JSONObject region(JSONObject source,JSONArray point,JSONObject o){
        var g=source.getJSONObject("registration").getJSONObject("geometry");var fit=source.getJSONObject("fit");double annotation=o.getDouble("annotationBudgetM");
        if(annotation<g.getDouble("anchorSigmaM"))throw new IllegalArgumentException("Cannot shrink the original annotation allowance");
        var plane=new PlaneCalibration(g);var h=CalibratedPlane159.homography(g);double[] p=PlaneCalibration.point(point);double dx=o.getDouble("pixelRadius")/source.getInt("imageWidth"),dy=o.getDouble("pixelRadius")/source.getInt("imageHeight");
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},max={-Double.POSITIVE_INFINITY,-Double.POSITIVE_INFINITY};double lo=Double.POSITIVE_INFINITY,hi=-lo,distance=0;
        for(double x:new double[]{p[0]-dx,p[0]+dx})for(double y:new double[]{p[1]-dy,p[1]+dy}){
            if(x<0||x>1||y<0||y>1||!plane.inPlane(x,y))throw new IllegalArgumentException("Target localization region leaves the measured plane");double denom=h[6]*x+h[7]*y+1;lo=Math.min(lo,denom);hi=Math.max(hi,denom);
            var q=plane.project(x,y);distance=Math.max(distance,Math.hypot(q[0],q[1]));var v=MatRegistration041.map(fit,q);for(int a=0;a<2;a++){int axis=a==0?0:2;min[a]=Math.min(min[a],v[axis]);max[a]=Math.max(max[a],v[axis]);}
        }
        if(lo<=1e-6&&hi>=-1e-6)throw new IllegalArgumentException("Projective horizon in localization region");double margin=annotation+fit.getDouble("maxCheckErrorM")+fit.getJSONObject("tape").getDouble("absoluteErrorM")+g.getDouble("scaleRelativeSigma")*distance*fit.getDouble("scale");
        for(int a=0;a<2;a++){min[a]-=margin;max[a]+=margin;}var c=MatRegistration041.map(fit,plane.project(p[0],p[1]));return new JSONObject().put("center",new JSONArray(new double[]{c[0],c[2]})).put("min",new JSONArray(min)).put("max",new JSONArray(max)).put("additiveMarginM",margin);
    }
    static JSONObject evaluate(JSONObject replay,JSONObject o){
        var t=replay.getJSONObject("trial");var source=replay.getJSONObject("source");var refs=t.getJSONObject("evaluation").getJSONObject("references");var first=MatTrials041.window(t,"start");var last=MatTrials041.window(t,"end");
        if(first.length()!=8||last.length()!=8||!refs.getBoolean("confirmed"))throw new IllegalArgumentException("Exactly eight independently labelled start and end frames required");
        var byOrdinal=new HashMap<Integer,JSONObject>();for(Object raw:refs.getJSONArray("frames")){var r=(JSONObject)raw;if(byOrdinal.put(r.getInt("ordinal"),r)!=null)throw new IllegalArgumentException("Duplicate reference ordinal");}
        if(byOrdinal.size()!=16)throw new IllegalArgumentException("All fixed-window references required");
        var reg=source.getJSONObject("registration");var fit=source.getJSONObject("fit");var tape=fit.getJSONObject("tape");var plane=new PlaneCalibration(reg.getJSONObject("geometry"));var a=PlaneCalibration.point(tape.getJSONArray("a"));var b=PlaneCalibration.point(tape.getJSONArray("b"));a=plane.project(a[0],a[1]);b=plane.project(b[0],b[1]);
        double sign=(t.getDouble("toMark")-t.getDouble("fromMark"))/(tape.getDouble("markB")-tape.getDouble("markA"));double length=Math.abs(t.getDouble("toMark")-t.getDouble("fromMark"))*MatRegistration041.unit(tape.getString("unit"));double dx=(b[0]-a[0])*sign,dy=(b[1]-a[1])*sign,n=Math.hypot(dx,dy);if(n<1e-9||length<.02)throw new IllegalArgumentException("Nonzero independent tape displacement required");
        // Similarity maps orientation only: known tape length is independent of the fitted scale.
        var origin=MatRegistration041.map(fit,new double[]{0,0});var vector=MatRegistration041.map(fit,new double[]{dx/n,dy/n});double vx=vector[0]-origin[0],vz=vector[2]-origin[2],norm=Math.hypot(vx,vz);var expected=new JSONArray(new double[]{length*vx/norm,length*vz/norm});
        var rows=new JSONArray();int valid=0,covered=0,referenceCovered=0;double maxError=0,maxRadius=0;double[] mean={0,0},meanMin={0,0},meanMax={0,0};
        for(int i=0;i<8;i++){
            var row=new JSONObject().put("pair",i+1).put("startOrdinal",first.getJSONObject(i).getInt("ordinal")).put("endOrdinal",last.getJSONObject(i).getInt("ordinal")).put("usable",false);rows.put(row);
            var start=first.getJSONObject(i);var end=last.getJSONObject(i);
            for(JSONObject x:List.of(start,end)){var label=byOrdinal.get(x.getInt("ordinal"));if(label==null||!label.getString("imageSha256").equals(x.getJSONObject("frame").getString("imageSha256"))||!label.optBoolean("visible",false)||!label.optBoolean("atDeclaredMark",false))throw new IllegalArgumentException("Independent endpoint label missing or contradicted");}
            if(!accepted(start)||!accepted(end)){row.put("reason","Unmeasured target or invalid camera reference; pair retained in denominator");continue;}
            JSONObject p,q;try{p=region(source,start.getJSONObject("tracker").getJSONArray("point"),o);q=region(source,end.getJSONObject("tracker").getJSONArray("point"),o);}catch(IllegalArgumentException e){row.put("reason",e.getMessage());continue;}
            double[] delta=new double[2],min=new double[2],max=new double[2];for(int axis=0;axis<2;axis++){delta[axis]=q.getJSONArray("center").getDouble(axis)-p.getJSONArray("center").getDouble(axis);min[axis]=q.getJSONArray("min").getDouble(axis)-p.getJSONArray("max").getDouble(axis);max[axis]=q.getJSONArray("max").getDouble(axis)-p.getJSONArray("min").getDouble(axis);mean[axis]+=delta[axis]/8;meanMin[axis]+=min[axis]/8;meanMax[axis]+=max[axis]/8;}
            score(row,delta,min,max,expected,o);row.put("usable",true);valid++;if(row.getBoolean("containsReference"))covered++;if(row.getBoolean("containsReferenceRegion"))referenceCovered++;maxError=Math.max(maxError,row.getDouble("errorM"));maxRadius=Math.max(maxRadius,row.getDouble("radiusM"));
        }
        boolean qualifies=valid==8&&referenceCovered==8&&maxError<=o.getDouble("maxErrorM")&&maxRadius<=o.getDouble("maxRadiusM");
        var aggregate=new JSONObject().put("usable",valid==8);if(valid==8)score(aggregate,mean,meanMin,meanMax,expected,o);
        return new JSONObject().put("trialId",t.getString("id")).put("registrationId",t.getString("registrationId")).put("referenceSource",refs.getString("source")).put("expectedDeltaXZ",expected).put("rows",rows).put("aggregate",aggregate).put("pairedSamples",8).put("usablePairs",valid).put("nominalCovered",covered).put("referenceRegionCovered",referenceCovered).put("maxErrorM",valid==0?JSONObject.NULL:maxError).put("maxRadiusM",valid==0?JSONObject.NULL:maxRadius).put("measurementGatesPassed",qualifies).put("independentEpisodes",1).put("probabilityCalibrated",false).put("actionAuthorized",false).put("scope",LIMIT).put("originalEvaluation",t.getJSONObject("evaluation")).put("recordedEngine",replay.getString("recordedEngine")).put("currentObserverEngine",replay.getString("currentObserverEngine")).put("reconstructionEngine",replay.getString("reconstructionEngine")).put("exactNumericalReplay",true).put("frameHashes",replay.getJSONArray("frameHashes"));
    }
    private static boolean accepted(JSONObject r){return r.getJSONObject("reference").getBoolean("valid")&&r.getJSONObject("tracker").getBoolean("accepted")&&r.getJSONObject("estimate").getBoolean("accepted");}
    private static void score(JSONObject out,double[] delta,double[] min,double[] max,JSONArray expected,JSONObject options){
        boolean contains=true,region=true;double[] extent=new double[2];for(int a=0;a<2;a++){double e=expected.getDouble(a),budget=options.getDouble("referenceBudgetM");contains&=e>=min[a]&&e<=max[a];region&=e-budget>=min[a]&&e+budget<=max[a];extent[a]=Math.max(delta[a]-min[a],max[a]-delta[a]);}
        out.put("deltaXZ",new JSONArray(delta)).put("minimumXZ",new JSONArray(min)).put("maximumXZ",new JSONArray(max)).put("errorM",Math.hypot(delta[0]-expected.getDouble(0),delta[1]-expected.getDouble(1))).put("radiusM",Math.hypot(extent[0],extent[1])).put("containsReference",contains).put("containsReferenceRegion",region);
    }
}

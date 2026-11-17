package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.util.*;

/** Source-bound, recorded plane coordinates. Bounds are conditional, never a probability. */
final class CalibratedPlane159 {
    static final String VERSION="recorded-plane-bounds-159-v1";
    static final String LIMIT="Conditional planar bounds from declared localization and annotation budgets plus independent reference discrepancies. Not calibrated probability, object height/contact, live pose or hardware authority.";
    private final Path project;
    private final Alignment041Store alignment;
    private final WorldLedger worlds;

    CalibratedPlane159(Path project,Alignment041Store alignment,WorldLedger worlds){this.project=project;this.alignment=alignment;this.worlds=worlds;}

    JSONObject source(String id)throws Exception {
        UUID.fromString(id);
        JSONObject r=alignment.registration(id),unsigned=WorldFrames.copy(r);unsigned.remove("sha256");
        if(!WorldFrames.hash(unsigned).equals(r.getString("sha256")))throw new IllegalStateException("Original registration checksum changed");
        var frame=r.getJSONObject("frame");
        Path file=project.resolve("alignment-041/frames").resolve(id).resolve("0.image");
        ReplayEvidence157.unlinkedRealPath(file);
        if(Files.size(file)>1_000_000)throw new IllegalStateException("Registration frame exceeds original bound");
        byte[] bytes=Files.readAllBytes(file);
        if(!PerceptionStore.hash(bytes).equals(frame.getString("imageSha256"))||bytes.length!=frame.getInt("bytes"))throw new IllegalStateException("Original calibration frame changed");
        Path meta=file.resolveSibling("0.meta.json");ReplayEvidence157.unlinkedRealPath(meta);
        if(Files.size(meta)>20_000||!new JSONObject(Files.readString(meta)).similar(frame))throw new IllegalStateException("Original calibration frame metadata changed");
        var image=WorldObservationStore.decode(bytes);var fit=reconstruct(r);
        var historicScene=worlds.source(r.getJSONObject("nativeBinding"));
        SpatialScene.object(historicScene,r.getString("bodyId"));
        return new JSONObject().put("registration",r).put("fit",fit).put("imageWidth",image.getWidth()).put("imageHeight",image.getHeight())
            .put("originalWorldSha256",WorldFrames.hash(historicScene)).put("frameSha256",PerceptionStore.hash(bytes))
            .put("scope","Original recorded reference, annotations and historical world; no claim that a current camera is unchanged.");
    }

    static JSONObject reconstruct(JSONObject r){
        var geometry=r.getJSONObject("geometry");
        if(!geometry.getString("mode").equals("homography")||!WorldFrames.hash(geometry).equals(r.getString("geometrySha256")))throw new IllegalStateException("Explicit four-corner calibration required");
        var old=r.getJSONObject("fit");var landmarks=new JSONArray();
        for(Object raw:old.getJSONArray("landmarks")){
            var l=(JSONObject)raw;var in=new JSONObject();
            for(String k:List.of("name","role","image","native","source"))in.put(k,l.get(k));landmarks.put(in);
        }
        var tape=new JSONObject();for(String k:List.of("a","b","markA","markB","unit","source"))tape.put(k,old.getJSONObject("tape").get(k));
        var fit=MatRegistration041.fit(geometry,old.getJSONObject("basis"),landmarks,tape);
        if(!fit.similar(old)||!fit.getBoolean("passed"))throw new IllegalStateException("Original independent calibration checks changed or failed");
        var basis=fit.getJSONObject("basis");
        if(Math.abs(basis.getJSONArray("origin").getDouble(1))+Math.abs(basis.getJSONArray("u").getDouble(1))+Math.abs(basis.getJSONArray("v").getDouble(1))>1e-9)throw new IllegalArgumentException("Simulation marker actions require the declared horizontal Y=0 plane");
        return fit;
    }

    static JSONObject bounds(JSONObject source,String name,double pixelRadius,double annotationBudgetM){
        PlaneCalibration.bounded(pixelRadius,.5,8);PlaneCalibration.bounded(annotationBudgetM,.001,.02);
        JSONObject r=source.getJSONObject("registration"),g=r.getJSONObject("geometry"),fit=source.getJSONObject("fit");
        if(annotationBudgetM<g.getDouble("anchorSigmaM"))throw new IllegalArgumentException("Cannot reduce the original declared annotation budget");
        JSONObject selected=null;
        for(Object raw:fit.getJSONArray("landmarks")){var l=(JSONObject)raw;if(l.getString("name").equals(name))selected=l;}
        if(selected==null||!selected.getString("role").equals("check"))throw new IllegalArgumentException("Select an explicitly withheld printed-plane landmark, not a fitting corner or object track");
        var point=PlaneCalibration.point(selected.getJSONArray("image"));
        double dx=pixelRadius/source.getInt("imageWidth"),dy=pixelRadius/source.getInt("imageHeight");
        var plane=new PlaneCalibration(g);double[] h=homography(g);
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY},max={-Double.POSITIVE_INFINITY,-Double.POSITIVE_INFINITY,-Double.POSITIVE_INFINITY};
        var corners=new JSONArray();double farthest=0;double lo=Double.POSITIVE_INFINITY,hi=-Double.POSITIVE_INFINITY;
        for(double x:new double[]{point[0]-dx,point[0]+dx})for(double y:new double[]{point[1]-dy,point[1]+dy}){
            if(!plane.inPlane(x,y)||x<0||x>1||y<0||y>1)throw new IllegalArgumentException("Localization rectangle leaves the measured plane");
            double denominator=h[6]*x+h[7]*y+1;lo=Math.min(lo,denominator);hi=Math.max(hi,denominator);
            double[] planar=plane.project(x,y);farthest=Math.max(farthest,Math.hypot(planar[0],planar[1]));double[] p=MatRegistration041.map(fit,planar);corners.put(new JSONArray(p));
            for(int i=0;i<3;i++){min[i]=Math.min(min[i],p[i]);max[i]=Math.max(max[i],p[i]);}
        }
        if(lo<=1e-6&&hi>=-1e-6)throw new IllegalArgumentException("Localization interval intersects a projective horizon");
        double[] center=MatRegistration041.map(fit,plane.project(point[0],point[1]));
        double residual=fit.getDouble("maxCheckErrorM"),tape=fit.getJSONObject("tape").getDouble("absoluteErrorM");
        double scaleBudget=g.getDouble("scaleRelativeSigma")*farthest*fit.getDouble("scale");
        double budget=annotationBudgetM+residual+tape+scaleBudget;
        // X/Z bounds include a conservative additive metric budget; Y is an explicitly chosen plane.
        for(int i:new int[]{0,2}){min[i]-=budget;max[i]+=budget;}
        double radius=Math.hypot(Math.max(center[0]-min[0],max[0]-center[0]),Math.max(center[2]-min[2],max[2]-center[2]));
        PlaneCalibration.bounded(radius,.0001,.1);
        return new JSONObject().put("name",name).put("image",new JSONArray(point)).put("imageRadiusPixels",pixelRadius)
            .put("centerM",new JSONArray(center)).put("minimumM",new JSONArray(min)).put("maximumM",new JSONArray(max)).put("radiusM",radius)
            .put("projectedCornersM",corners).put("annotationBudgetM",annotationBudgetM).put("maxIndependentGridResidualM",residual).put("independentTapeDiscrepancyM",tape).put("declaredRelativeScaleAllowanceM",scaleBudget)
            .put("conditional",true).put("probabilityCalibrated",false).put("livePose",false).put("heightMeasured",false).put("meaning",LIMIT);
    }

    /** Solve independently for the projective denominator used to exclude interval horizons. */
    static double[] homography(JSONObject g){
        double[][] m=new double[8][9];
        for(int i=0;i<4;i++){
            var p=g.getJSONArray("imagePoints").getJSONArray(i);var q=g.getJSONArray("worldPoints").getJSONArray(i);
            double x=p.getDouble(0),y=p.getDouble(1),u=q.getDouble(0),v=q.getDouble(1);
            m[2*i]=new double[]{x,y,1,0,0,0,-u*x,-u*y,u};m[2*i+1]=new double[]{0,0,0,x,y,1,-v*x,-v*y,v};
        }
        for(int c=0;c<8;c++){
            int pivot=c;for(int row=c+1;row<8;row++)if(Math.abs(m[row][c])>Math.abs(m[pivot][c]))pivot=row;
            double[] swap=m[c];m[c]=m[pivot];m[pivot]=swap;
            double d=m[c][c];if(Math.abs(d)<1e-10)throw new IllegalArgumentException("Degenerate plane denominator");
            for(int j=c;j<9;j++)m[c][j]/=d;
            for(int row=0;row<8;row++)if(row!=c){double scale=m[row][c];for(int j=c;j<9;j++)m[row][j]-=scale*m[c][j];}
        }
        double[] h=new double[9];for(int i=0;i<8;i++)h[i]=m[i][8];h[8]=1;return h;
    }
}

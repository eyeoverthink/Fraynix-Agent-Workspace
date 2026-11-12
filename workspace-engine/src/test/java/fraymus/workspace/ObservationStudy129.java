package fraymus.workspace;

import org.json.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

/** Research driver: original pixels and first seed enter tracker; later references enter scorer only. */
public final class ObservationStudy129 {
 public static void main(String[] args)throws Exception{
  Path packPath=Path.of(args[0]),geometryPath=Path.of(args[1]),referencesPath=Path.of(args[2]),out=Path.of(args[3]);String method=args[4];
  if(Files.exists(out))throw new IllegalStateException("Preserve the original study: "+out);
  var pack=new JSONObject(Files.readString(packPath));var plane=new PlaneCalibration(new JSONObject(Files.readString(geometryPath)));var frames=pack.getJSONArray("frames");var seed=PixelTracker.Box.from(pack.getJSONArray("objects").getJSONObject(0).getJSONArray("box"));var first=decode(frames.getJSONObject(0));
  ImageTracker tracker=switch(method){case "flow"->new FlowTracker("subject","human supplied seed",first,seed);case "scale-aware"->new ScaleAwareFlow129("subject","human supplied seed",first,seed);case "adaptive"->new AdaptiveTracker("subject","human supplied seed",first,seed);case "fixed"->new PixelTracker("subject","human supplied seed",first,seed,false);default->throw new IllegalArgumentException("Unknown research method");};
  var filter=new ObservationFilter(frames.getJSONObject(0).getInt("sourceFrame"),seed);var rows=new JSONArray();double height=seed.h();int accepted=0;long start=System.nanoTime();
  for(int i=1;i<frames.length();i++){
   var frame=frames.getJSONObject(i);var tracked=tracker.update(decode(frame));var e=filter.step(frame.getInt("sourceFrame"),tracked);if(e.getBoolean("accepted")){height=PixelTracker.Box.from(tracked.getJSONArray("box")).h();accepted++;}
   double x=e.getJSONArray("position").getDouble(0),y=e.getJSONArray("position").getDouble(1)+height/2;var row=new JSONObject().put("ordinal",i).put("sourceFrame",frame.getInt("sourceFrame")).put("seconds",pack.getJSONArray("seconds").getDouble(i)).put("accepted",e.getBoolean("accepted")).put("tracker",tracked).put("estimate",e).put("pointImage",new JSONArray(new double[]{x,y}));
   if(plane.inPlane(x,y)){var p=plane.project(x,y);var c=e.getJSONArray("covariance");double sigma=Math.max(.01,plane.sigma(x,y,c.getJSONArray(0).getDouble(0),c.getJSONArray(1).getDouble(0)));row.put("positionM",new JSONArray(p)).put("sigmaM",sigma).put("radiusM",2*sigma+plane.definition().getDouble("anchorSigmaM")+plane.definition().getDouble("scaleRelativeSigma")*Math.hypot(p[0],p[1])).put("status",e.getBoolean("accepted")?"measured-estimate":"prediction-only");}else row.put("status","unprojectable");rows.put(row);
  }
  double ms=(System.nanoTime()-start)/1e6; // Reference file is deliberately not opened until all predictions exist.
  var refs=new JSONObject(Files.readString(referencesPath));var scores=new JSONArray();double sum=0,max=0;int measured=0,within=0;
  for(Object raw:refs.getJSONArray("references")){var ref=(JSONObject)raw;var row=rows.getJSONObject(ref.getInt("ordinal")-1);var score=new JSONObject().put("ordinal",ref.getInt("ordinal")).put("referenceM",ref.getJSONArray("positionM")).put("accepted",row.getBoolean("accepted"));if(row.getBoolean("accepted")&&row.has("positionM")){double error=PlaneCalibration.distance(PlaneCalibration.point(row.getJSONArray("positionM")),PlaneCalibration.point(ref.getJSONArray("positionM")));score.put("errorM",error);sum+=error;max=Math.max(max,error);measured++;if(error<=1)within++;}scores.put(score);}
  var result=new JSONObject().put("method",method).put("sourceSha256",PerceptionStore.hash(Files.readAllBytes(packPath))).put("geometry",plane.definition()).put("referencesSha256",PerceptionStore.hash(Files.readAllBytes(referencesPath))).put("rows",rows).put("scores",scores).put("metrics",new JSONObject().put("frames",rows.length()).put("accepted",accepted).put("references",scores.length()).put("scoredAccepted",measured).put("withinOneMetre",within).put("meanAcceptedErrorM",measured==0?JSONObject.NULL:sum/measured).put("maxAcceptedErrorM",measured==0?JSONObject.NULL:max).put("trackerMs",ms)).put("scope","Same recorded inputs, annotations only after tracking. Conditional accepted error plus missed references; not semantic recognition or probability.");
  Files.createDirectories(out.toAbsolutePath().getParent());Files.writeString(out,result.toString(2),StandardOpenOption.CREATE_NEW);System.out.println(result.getJSONObject("metrics").put("method",method));
 }
 private static java.awt.image.BufferedImage decode(JSONObject frame)throws Exception{byte[] bytes=Base64.getDecoder().decode(frame.getString("image"));if(!PerceptionStore.hash(bytes).equals(frame.getString("imageSha256")))throw new IllegalStateException("Original pixel checksum changed");var image=ImageIO.read(new ByteArrayInputStream(bytes));if(image==null)throw new IllegalArgumentException("Unreadable source image");return image;}
}

package fraymus.workspace;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;
/** Replays the already exposed unit-test pixels. Never fresh qualification. */
public final class Exposed145 {
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[0]);Files.createDirectories(root);var records=new JSONArray();
  for(String name:List.of("exposed-reference","exposed-return","exposed-copy")){
   boolean donor=name.equals("exposed-reference"),copy=name.equals("exposed-copy");var frames=new JSONArray();var rows=new JSONArray();
   for(int i=0;i<24;i++){
    var im=donor?AppearanceConfirmation143Test.scene(32+i,48,80,143031,false,false,false):TrajectoryRecovery145Test.frame(i,copy);
    var bytes=new ByteArrayOutputStream();ImageIO.write(im,"png",bytes);String hash=PerceptionStore.hash(bytes.toByteArray());frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes.toByteArray())).put("imageSha256",hash));
    if(i>0){boolean hidden=!donor&&i>=10&&(copy||i<=11);var row=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",hidden?"occluded":"visible").put("note","Already-exposed unit-test renderer geometry, not fresh qualification.");if(!hidden)row.put("targetBox",new PixelTracker.Box((32+(donor?i:2*i))/320.,.2,.25,1./3).json());if(!donor&&i>=7&&(i<=9||copy))row.put("otherBoxes",new JSONArray().put(new PixelTracker.Box(234/320.,148/240.,68/320.,68/240.).json()));rows.put(row);}
   }
   var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title",name+" / exposed145 regression").put("source","Already exposed TrajectoryRecovery145Test source pixels, frozen before fresh evaluation").put("license","Project-generated controls").put("clock","source-frame").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Exposed textured object").put("box",AppearanceConfirmation143Test.seed().json()))).put("frames",frames);
   var labels=new JSONObject().put("schema","association-labels-134-v1").put("objectKey","subject").put("annotator","Independent renderer geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Exposed unit-test placements and identity, not predictions and not held-out qualification").put("rows",rows);
   Files.writeString(root.resolve(name+".fobs.json"),pack.toString());Files.writeString(root.resolve(name+"-labels.json"),labels.toString(2));records.put(new JSONObject().put("case",name).put("group","exposed").put("role",donor?"reference-source":"evaluation").put("domain","exposed-development-control").put("frames",24));
  }Files.writeString(root.resolve("index.json"),records.toString(2));
 }
}

package fraymus.workspace;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;

/** Small, explicitly synthetic complete-export control; no camera-performance claim. */
public final class ReferenceFixture137 {
 public static void main(String[] args)throws Exception{
  Path folder=Path.of(args[0]);if(Files.exists(folder))throw new IllegalStateException("Preserve earlier fixtures");Files.createDirectories(folder);
  for(boolean donor:new boolean[]{true,false}){
   int count=donor?13:5,x=donor?40:200,y=donor?50:80;var frames=new JSONArray();var rows=new JSONArray();
   for(int i=0;i<count;i++){
    var bytes=new ByteArrayOutputStream();ImageIO.write(AppearanceFlow135Test.view((donor?0:20)+i,x+i,y,false,false),"png",bytes);byte[] raw=bytes.toByteArray();String hash=PerceptionStore.hash(raw);
    frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(raw)).put("imageSha256",hash));
    if(i>0)rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility","visible")
        .put("targetBox",new PixelTracker.Box((x+i)/480.,y/300.,100/480.,100/300.).json()).put("note","Known synthetic placement; not camera labels"));
   }
   var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title",donor?"Synthetic137 source poses":"Synthetic137 separate return poses")
       .put("source","Java2D transform of repository astronaut fixture; software export control, not real camera footage").put("license","Repository NASA astronaut fixture; generated placement geometry")
       .put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","synthetic photo tile").put("box",new PixelTracker.Box(x/480.,y/300.,100/480.,100/300.).json()))).put("frames",frames);
   String name=donor?"source":"target";Files.writeString(folder.resolve(name+".fobs.json"),pack.toString());
   var labels=new JSONObject().put("schema",AssociationAudit134.VERSION).put("objectKey","subject").put("annotator","Independent generated geometry")
       .put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Fixed Java2D placements; no tracker outputs used").put("rows",rows);
   Files.writeString(folder.resolve(name+"-labels.json"),labels.toString());
  }
 }
}

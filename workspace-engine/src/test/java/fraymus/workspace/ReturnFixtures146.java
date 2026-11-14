package fraymus.workspace;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;

/** New textures and fixed identity declarations. Generate only after method/protocol freeze. */
public final class ReturnFixtures146 {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);Files.createDirectories(root);var records=new JSONArray();
        for(String name:List.of("generated-reference","short-return","distant-copy","same-path-copy","delayed-return")){
            boolean donor=name.equals("generated-reference");int count=donor?24:32;var frames=new JSONArray();var labels=new JSONArray();
            for(int i=0;i<count;i++){
                boolean distant=name.equals("distant-copy"),same=name.equals("same-path-copy"),delayed=name.equals("delayed-return");
                boolean targetHidden=!donor&&i>=10&&(distant||same||i<=(delayed?23:11));
                boolean visiblePixels=!donor&&same?!(i>=10&&i<=11):!targetHidden;
                boolean other=distant&&i>=12||same&&i>=12;
                int x=donor?32+i:32+2*i,y=48,size=80,texture=donor||i>=7?146071:146989;
                var image=AppearanceConfirmation143Test.scene(x,y,size,texture,!visiblePixels,false,false);
                if(distant&&other){var copy=AppearanceConfirmation143Test.scene(234,148,68,146071,false,false,false);var g=image.createGraphics();g.drawImage(copy.getSubimage(234,148,68,68),234,148,null);g.dispose();}
                var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);String hash=PerceptionStore.hash(bytes.toByteArray());
                frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes.toByteArray())).put("imageSha256",hash));
                if(i>0){
                    var row=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",targetHidden?"occluded":"visible")
                        .put("note",same?"Declared identity substitution after hidden interval; same pixels as short-return. An observability limit, not an independent image trial.":"Fixed original Java geometry and identity declarations before tracking; generated control, not camera footage.");
                    var target=new PixelTracker.Box(x/320.,y/240.,size/320.,size/240.);
                    if(!targetHidden)row.put("targetBox",target.json());
                    if(other)row.put("otherBoxes",new JSONArray().put(same?target.json():new PixelTracker.Box(234/320.,148/240.,68/320.,68/240.).json()));
                    labels.put(row);
                }
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title",name+" / generated146")
                .put("source","Fixed Java renderer; same-path-copy shares short-return pixels but declares different identity after disappearance")
                .put("license","Project-generated test images").put("clock","source-frame")
                .put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated textured target").put("box",AppearanceConfirmation143Test.seed().json()))).put("frames",frames);
            var lab=new JSONObject().put("schema","association-labels-134-v1").put("objectKey","subject").put("annotator","Independent renderer geometry").put("method","synthetic-control").put("exposure","before-tracking")
                .put("provenance","Fixed geometry and declared identity, never tracker predictions. Same-path-copy is an identical-pixel limit, not an independent trial.").put("rows",labels);
            Files.writeString(root.resolve(name+".fobs.json"),pack.toString());Files.writeString(root.resolve(name+"-labels.json"),lab.toString(2));
            records.put(new JSONObject().put("case",name).put("group","generated").put("role",donor?"reference-source":"evaluation").put("domain","generated-control").put("frames",count));
        }
        Files.writeString(root.resolve("generated-index.json"),records.toString(2));
    }
}

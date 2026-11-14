package fraymus.workspace;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;

/** Fixed before evaluation. The identical-pixel identity pair is an explicit limit, not two independent samples. */
public final class AmbiguityFixtures144 {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);Files.createDirectories(root);var records=new JSONArray();
        for(String name:List.of("generated-reference","clean-partial","clean-full","copy-only","legitimate-after-rival","baseline-reset","unseen-copy")){
            boolean donor=name.equals("generated-reference"),unseen=name.equals("unseen-copy"),partial=name.equals("clean-partial");var frames=new JSONArray();var labels=new JSONArray();
            for(int i=0;i<24;i++){
                boolean baseline=name.equals("baseline-reset")&&i>=13&&i<=18;
                boolean competing=Set.of("copy-only","legitimate-after-rival","baseline-reset").contains(name);
                boolean hidden=!donor&&i>0&&(i<=4||i>=13&&i<=15)&&!baseline;
                boolean copy=competing&&i>4&&(name.equals("copy-only")||i<=12);
                int texture=donor||i>0&&!baseline?144071:144989;
                int size=donor||i==0||baseline?80:120,x=donor?32+i:i==0||baseline?32:172+i%4,y=donor||i==0||baseline?48:i<16?64:72;
                var im=AppearanceConfirmation143Test.scene(x,y,size,texture,hidden,partial&&i>4&&i<20,false);
                if(copy){var other=AppearanceConfirmation143Test.scene(10,148,68,144071,false,false,false);var g=im.createGraphics();g.drawImage(other.getSubimage(10,148,68,68),10,148,null);g.dispose();}
                var bytes=new ByteArrayOutputStream();ImageIO.write(im,"png",bytes);String hash=PerceptionStore.hash(bytes.toByteArray());
                frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes.toByteArray())).put("imageSha256",hash));
                if(i>0){var l=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",hidden||unseen?"occluded":partial&&i<20?"partial":"visible").put("note",unseen?"Paired identical pixels to clean-full; the post-cover appearance is labelled a copy. Deliberate observational identity limit, not an independent success trial.":"Fixed renderer geometry independent of tracker; generated control, not camera qualification.");
                    if(!hidden&&!unseen)l.put("targetBox",new PixelTracker.Box(x/320.,y/240.,size/320.,size/240.).json());
                    var others=new JSONArray();if(copy)others.put(new PixelTracker.Box(10/320.,148/240.,68/320.,68/240.).json());if(unseen&&!hidden)others.put(new PixelTracker.Box(x/320.,y/240.,size/320.,size/240.).json());if(!others.isEmpty())l.put("otherBoxes",others);labels.put(l);
                }
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title",name+" / generated144").put("source","Fixed original Java renderer; no camera or semantic identity claims").put("license","Project-generated test images").put("clock","source-frame").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated textured object").put("box",AppearanceConfirmation143Test.seed().json()))).put("frames",frames);
            var lab=new JSONObject().put("schema","association-labels-134-v1").put("objectKey","subject").put("annotator","Independent renderer geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Fixed placements and declared identities from generator, never predictions; unseen-copy intentionally shares clean-full pixels").put("rows",labels);
            Files.writeString(root.resolve(name+".fobs.json"),pack.toString());Files.writeString(root.resolve(name+"-labels.json"),lab.toString(2));records.put(new JSONObject().put("case",name).put("group","generated").put("role",donor?"reference-source":"evaluation").put("domain",unseen?"identity-limit":"generated-control").put("frames",24));
        }
        Files.writeString(root.resolve("generated-index.json"),records.toString(2));
    }
}

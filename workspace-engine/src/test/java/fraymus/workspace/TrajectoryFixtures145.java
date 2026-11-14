package fraymus.workspace;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;

/** Fixed before evaluation. Target/rival paths, appearance changes and outcome geometry are fixed before fresh evaluation. */
public final class TrajectoryFixtures145 {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);Files.createDirectories(root);var records=new JSONArray();
        for(String name:List.of("generated-reference","trajectory-return","copy-remains","crossing-copy","turning-return")){
            boolean donor=name.equals("generated-reference");var frames=new JSONArray();var labels=new JSONArray();
            for(int i=0;i<24;i++){
                boolean crossing=name.equals("crossing-copy"),copyOnly=name.equals("copy-remains");
                boolean hidden=!donor&&i>=10&&(copyOnly||crossing||i<=11);
                boolean copy=!donor&&!name.equals("clean-return")&&i>=7&&(i<=9||copyOnly||crossing);
                int texture=donor||i>=7?145071:145989;
                int size=80,x=donor?32+i:32+2*i,y=name.equals("turning-return")&&i>=12?96:48;
                int otherX=crossing?Math.max(32+2*i,125-20*(i-7)):234,otherY=crossing?48:148;
                var im=AppearanceConfirmation143Test.scene(x,y,size,texture,hidden,false,false);
                if(copy){var other=AppearanceConfirmation143Test.scene(otherX,otherY,68,145071,false,false,false);var g=im.createGraphics();g.drawImage(other.getSubimage(otherX,otherY,68,68),otherX,otherY,null);g.dispose();}
                var bytes=new ByteArrayOutputStream();ImageIO.write(im,"png",bytes);String hash=PerceptionStore.hash(bytes.toByteArray());
                frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes.toByteArray())).put("imageSha256",hash));
                if(i>0){var l=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",hidden?"occluded":crossing&&copy&&otherX<x+size?"partial":"visible").put("note","Fixed renderer target and competing-object trajectories, independent of tracker; generated control, not camera qualification.");
                    if(!hidden)l.put("targetBox",new PixelTracker.Box(x/320.,y/240.,size/320.,size/240.).json());
                    if(copy)l.put("otherBoxes",new JSONArray().put(new PixelTracker.Box(otherX/320.,otherY/240.,68/320.,68/240.).json()));labels.put(l);
                }
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title",name+" / generated145").put("source","Fixed original Java renderer; no camera or semantic identity claims").put("license","Project-generated test images").put("clock","source-frame").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated textured object").put("box",AppearanceConfirmation143Test.seed().json()))).put("frames",frames);
            var lab=new JSONObject().put("schema","association-labels-134-v1").put("objectKey","subject").put("annotator","Independent renderer geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Fixed placements and declared identities from generator, never predictions; All target/competitor identities and paths fixed before predictions").put("rows",labels);
            Files.writeString(root.resolve(name+".fobs.json"),pack.toString());Files.writeString(root.resolve(name+"-labels.json"),lab.toString(2));records.put(new JSONObject().put("case",name).put("group","generated").put("role",donor?"reference-source":"evaluation").put("domain","generated-control").put("frames",24));
        }
        Files.writeString(root.resolve("generated-index.json"),records.toString(2));
    }
}

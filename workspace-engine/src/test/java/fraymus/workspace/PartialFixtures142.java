package fraymus.workspace;

import java.nio.file.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;

/** Labelled generated controls. Fresh textures are fixed before evaluation, not camera evidence. */
public final class PartialFixtures142 {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);Files.createDirectories(root);var records=new JSONArray();
        for(String name:List.of("generated-reference","generated-partial","generated-twin","generated-covered")){
            boolean donor=name.endsWith("reference"),twin=name.endsWith("twin"),covered=name.endsWith("covered");var frames=new JSONArray();var labels=new JSONArray();
            for(int i=0;i<24;i++){
                boolean hidden=!donor&&i>0&&(i<=4||covered);boolean partial=!donor&&i>4&&i<17;int texture=donor||i>0?142577:142243,size=donor||i==0?80:120,x=donor?32+i:i==0?32:180+(i%3),y=donor||i==0?48:60;
                var im=PartialRecovery142Test.scene(x,y,size,texture,hidden,partial,twin&&i>4);var out=new ByteArrayOutputStream();ImageIO.write(im,"png",out);byte[] bytes=out.toByteArray();String hash=PerceptionStore.hash(bytes);
                frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes)).put("imageSha256",hash));
                if(i>0){var l=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",hidden?"occluded":partial?"partial":"visible").put("note","Independent fixed renderer geometry; generated control, not camera footage.");if(!hidden)l.put("targetBox",new PixelTracker.Box(x/320.,y/240.,size/320.,size/240.).json());if(twin&&i>4)l.put("otherBoxes",new JSONArray().put(new PixelTracker.Box(8/320.,145/240.,70/320.,70/240.).json()));labels.put(l);}
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title",name+" / generated control142").put("source","Deterministic original Java renderer, fixed geometry/textures; no camera claims").put("license","Project-generated test images").put("clock","source-frame").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated textured object").put("box",PartialRecovery142Test.seed().json()))).put("frames",frames);
            var lab=new JSONObject().put("schema","association-labels-134-v1").put("objectKey","subject").put("annotator","Independent renderer geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Fixed placements, visibility and competing-object rectangles from generator, never predictions").put("rows",labels);
            Files.writeString(root.resolve(name+".fobs.json"),pack.toString());Files.writeString(root.resolve(name+"-labels.json"),lab.toString(2));records.put(new JSONObject().put("case",name).put("group","generated").put("role",donor?"reference-source":"evaluation").put("domain","generated-control").put("frames",24));
        }
        Files.writeString(root.resolve("generated-index.json"),records.toString(2));
    }
}

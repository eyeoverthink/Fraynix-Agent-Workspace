package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** New source-only sequences. Analytic labels are emitted before any tracker sees them. */
public class RotationFixtures152Test {
    @Test public void generateReservedSourcesWithoutRunningTracking()throws Exception{
        String destination=System.getProperty("rotation152.fixtures");if(destination==null)return;
        Path dir=Path.of(destination).toAbsolutePath();Files.createDirectories(dir);var manifest=new JSONArray();
        for(String name:new String[]{"return-22","return-minus67","wrong-only","copy-veto"}){
            var frames=new JSONArray();var rows=new JSONArray();int pattern=name.equals("return-minus67")?15223:15221;double angle=name.equals("return-minus67")?-67:22;
            for(int i=0;i<19;i++){
                boolean start=i<=4,cover=i>=5&&i<=9,wrong=name.equals("wrong-only")&&i>=10,copy=name.equals("copy-veto")&&i>=10;
                double x=start?40:120+Math.min(5,i-10),y=start?56:80;
                BufferedImage image=RotationSearch152Test.scene(cover?-1:wrong?15224:pattern,x,y,start?0:angle,copy);
                var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);byte[] raw=bytes.toByteArray();String hash=PerceptionStore.hash(raw);
                frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(raw)).put("imageSha256",hash));
                if(i==0)continue;
                var row=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",cover?"occluded":wrong?"out-of-view":"visible")
                    .put("note","Analytic generated geometry declared before tracking; no physical identity claim");
                var bounds=new RotationSearch152.Pose(x,y,32,64,start?0:angle).bounds(RotationSearch152Test.W,RotationSearch152Test.H).json();
                if(!cover&&!wrong)row.put("targetBox",bounds);
                if(wrong)row.put("otherBoxes",new JSONArray().put(bounds));
                if(copy)row.put("otherBoxes",new JSONArray().put(RotationSearch152Test.SEED.json()));
                rows.put(row);
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title","Reserved152 generated "+name).put("source","New deterministic Java rendering; not camera footage").put("license","Project-generated test source")
                .put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated target").put("box",RotationSearch152Test.SEED.json()))).put("frames",frames);
            var labels=new JSONObject().put("schema",AssociationAudit134.VERSION).put("objectKey","subject").put("annotator","Independent analytic scene geometry")
                .put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Reserved152 pattern seeds and poses fixed before experiment; source labels emitted without tracker execution").put("rows",rows);
            Path p=dir.resolve(name+"-pack.json"),l=dir.resolve(name+"-labels.json");Files.writeString(p,pack.toString());Files.writeString(l,labels.toString());
            manifest.put(new JSONObject().put("name",name).put("pack",p.toString()).put("packSha256",PerceptionStore.hash(Files.readAllBytes(p))).put("labels",l.toString()).put("labelsSha256",PerceptionStore.hash(Files.readAllBytes(l))));
        }
        Files.writeString(dir.resolve("manifest.json"),new JSONObject().put("cases",manifest).put("trackingExecuted",false).toString(2));assertEquals(4,manifest.length());
    }
}

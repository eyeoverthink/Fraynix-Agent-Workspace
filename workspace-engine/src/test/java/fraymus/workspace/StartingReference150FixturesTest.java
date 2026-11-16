package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.json.*;
import org.junit.Test;
import static fraymus.workspace.AppearanceConfirmation143Test.*;

/** Exposed rendered controls for the actual desktop/Java/source-verification workflow. */
public class StartingReference150FixturesTest {
    @Test public void writeDeclaredGeneratedSources()throws Exception {
        Path root=Path.of("build/starting150-generated");Files.createDirectories(root);
        for(String name:new String[]{"donor","return","wrong-pattern"}) {
            boolean donor=name.equals("donor"),wrong=name.equals("wrong-pattern");int last=donor?12:45;
            var frames=new JSONArray();var rows=new JSONArray();
            for(int i=0;i<=last;i++) {
                BufferedImage image;
                if(donor) {image=scene(32,48,80,143031,false,false,false);image.setRGB(0,0,0x718ca1);}
                else image=IdentityReturn149Test.frame(i,wrong);
                var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);byte[] bytes=out.toByteArray();
                frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes)).put("imageSha256",PerceptionStore.hash(bytes)));
                if(i==0)continue;
                boolean visible=donor||i<5||!wrong&&i>=23;
                var row=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",PerceptionStore.hash(bytes))
                    .put("visibility",visible?"visible":i<23?"occluded":"out-of-view")
                    .put("note","Declared rendered geometry; synthetic software control, not physical truth");
                if(visible)row.put("targetBox",(donor?seed():new PixelTracker.Box((i<5?32+2*i:112+i-23)/320.,.2,.25,1./3)).json());
                if(wrong&&i>=23)row.put("otherBoxes",new JSONArray().put(new PixelTracker.Box((112+i-23)/320.,.2,.25,1./3).json()));
                rows.put(row);
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title","Edition150 generated "+name)
                .put("source","Exposed Java rendered control. Initial appearance143819; independently provided reference143031 returns after cover. Wrong-pattern control143032. No real camera or physical claim.")
                .put("license","Project generated test fixture").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","generated texture").put("box",seed().json()))).put("frames",frames);
            var labels=new JSONObject().put("schema",AssociationAudit134.VERSION).put("objectKey","subject").put("annotator","Declared Java geometry")
                .put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Exact renderer coordinates frozen before actual app execution; exposed fixture, not held-out footage").put("rows",rows);
            Files.writeString(root.resolve(name+".json"),pack.toString(2));Files.writeString(root.resolve(name+"-labels.json"),labels.toString(2));
        }
    }
}

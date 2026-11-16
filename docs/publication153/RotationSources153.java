package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;

/** Source-only reserved controls, rendered and labelled before tracker execution. */
public class RotationSources153 {
    static BufferedImage patch(int seed){var image=new BufferedImage(32,64,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();var random=new Random(seed);for(int y=0;y<8;y++)for(int x=0;x<4;x++){g.setColor(new Color(30+random.nextInt(200),30+random.nextInt(200),30+random.nextInt(200)));g.fillRect(x*8,y*8,8,8);}g.dispose();return image;}
    static void draw(BufferedImage image,BufferedImage patch,double x,double y,double angle){var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.translate(x,y);g.rotate(Math.toRadians(angle));g.drawImage(patch,-16,-32,null);g.dispose();}
    static JSONArray bounds(double x,double y,double angle){double a=Math.toRadians(angle),c=Math.cos(a),s=Math.sin(a),loX=Double.POSITIVE_INFINITY,loY=loX,hiX=Double.NEGATIVE_INFINITY,hiY=hiX;for(int dx:new int[]{-16,16})for(int dy:new int[]{-32,32}){double px=x+c*dx-s*dy,py=y+s*dx+c*dy;loX=Math.min(loX,px);loY=Math.min(loY,py);hiX=Math.max(hiX,px);hiY=Math.max(hiY,py);}return new JSONArray().put(loX/192).put(loY/144).put(hiX/192).put(hiY/144);}
    public static void main(String[] args)throws Exception {
        Path dir=Path.of(args[0]).toAbsolutePath();Files.createDirectory(dir);var manifest=new JSONArray();
        for(String name:new String[]{"return-53","moving-return-minus113","wrong-only","near-copy-only","copy-veto"}){
            var frames=new JSONArray();var rows=new JSONArray();var geometry=new JSONArray();int pattern=name.startsWith("moving")?15323:15321;
            for(int i=0;i<23;i++){
                boolean start=i<=4,cover=i>=5&&i<=9,wrong=name.equals("wrong-only")&&i>=10,near=name.equals("near-copy-only")&&i>=10,copy=name.equals("copy-veto")&&i>=10;
                double x=start?40:120+Math.min(8,(i-10)*.6),y=start?56:80,angle=start?0:name.startsWith("moving")?-113+(i-10):53;
                var image=new BufferedImage(192,144,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(45,51,59));g.fillRect(0,0,192,144);g.dispose();
                if(!cover){var target=patch(wrong?15324:pattern);if(near){var edit=target.createGraphics();edit.setColor(Color.BLACK);edit.fillRect(0,0,16,32);edit.dispose();}draw(image,target,x,y,angle);if(copy)draw(image,patch(pattern),40,56,0);}
                var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);byte[] raw=bytes.toByteArray();String hash=PerceptionStore.hash(raw);frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(raw)).put("imageSha256",hash));
                geometry.put(new JSONObject().put("ordinal",i).put("x",x).put("y",y).put("degrees",angle).put("covered",cover).put("wrongOnly",wrong||near).put("copy",copy));if(i==0)continue;
                var row=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",cover?"occluded":wrong||near?"out-of-view":"visible").put("note","Analytic source geometry fixed before tracking; generated pixels, not camera footage");
                if(!cover&&!wrong&&!near)row.put("targetBox",bounds(x,y,angle));if(wrong||near)row.put("otherBoxes",new JSONArray().put(bounds(x,y,angle)));if(copy)row.put("otherBoxes",new JSONArray().put(bounds(40,56,0)));rows.put(row);
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title","Reserved153 "+name).put("source","New deterministic Java rendering; not physical footage").put("license","Project-generated test source").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated target").put("box",bounds(40,56,0)))).put("frames",frames);
            var labels=new JSONObject().put("schema",AssociationAudit134.VERSION).put("objectKey","subject").put("annotator","Independent source geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Reserved153 patterns and poses declared before frozen-method execution; labels use transformed source corners, no tracker calls").put("rows",rows);
            Path p=dir.resolve(name+"-pack.json"),l=dir.resolve(name+"-labels.json"),m=dir.resolve(name+"-geometry.json");Files.writeString(p,pack.toString());Files.writeString(l,labels.toString());Files.writeString(m,geometry.toString());
            manifest.put(new JSONObject().put("name",name).put("pack",p.toString()).put("packSha256",PerceptionStore.hash(Files.readAllBytes(p))).put("labels",l.toString()).put("labelsSha256",PerceptionStore.hash(Files.readAllBytes(l))).put("geometry",m.toString()).put("geometrySha256",PerceptionStore.hash(Files.readAllBytes(m))));
        }
        Files.writeString(dir.resolve("manifest.json"),new JSONObject().put("cases",manifest).put("trackingExecuted",false).toString(2));System.out.println("Five new source-only sequences saved; no tracker executed");
    }
}

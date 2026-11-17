package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;

/** Source-only reserved controls. No tracker or estimated target coordinates. */
public class HarderSources156 {
    static BufferedImage patch(int seed){var image=new BufferedImage(32,64,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();var random=new Random(seed);for(int y=0;y<8;y++)for(int x=0;x<4;x++){g.setColor(new Color(30+random.nextInt(200),30+random.nextInt(200),30+random.nextInt(200)));g.fillRect(x*8,y*8,8,8);}g.dispose();return image;}
    static void draw(BufferedImage image,BufferedImage patch,double x,double y,double angle){var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.translate(x,y);g.rotate(Math.toRadians(angle));g.drawImage(patch,-16,-32,null);g.dispose();}
    static JSONArray bounds(double x,double y,double angle){double a=Math.toRadians(angle),c=Math.cos(a),s=Math.sin(a),loX=Double.POSITIVE_INFINITY,loY=loX,hiX=Double.NEGATIVE_INFINITY,hiY=hiX;for(int dx:new int[]{-16,16})for(int dy:new int[]{-32,32}){double px=x+c*dx-s*dy,py=y+s*dx+c*dy;loX=Math.min(loX,px);loY=Math.min(loY,py);hiX=Math.max(hiX,px);hiY=Math.max(hiY,py);}return new JSONArray().put(loX/192).put(loY/144).put(hiX/192).put(hiY/144);}
    public static void main(String[] args)throws Exception {
        Path dir=Path.of(args[0]).toAbsolutePath();Files.createDirectory(dir);var manifest=new JSONArray();var donors=new JSONArray();
        for(int pattern:new int[]{15641,15643}){
            var frames=new JSONArray();var rows=new JSONArray();
            for(int i=0;i<5;i++){var image=new BufferedImage(192,144,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(65,71,79));g.fillRect(0,0,192,144);g.dispose();draw(image,patch(pattern),40,56,0);var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);String hash=PerceptionStore.hash(bytes.toByteArray());frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(bytes.toByteArray())).put("imageSha256",hash));if(i>0)rows.put(new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility","visible").put("targetBox",bounds(40,56,0)).put("note","Independent generated stationary starting reference"));}
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title","Reserved156 reference "+pattern).put("source","Separate generated reference; different background, never evaluation pixels").put("license","Project-generated test source").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated reference").put("box",bounds(40,56,0)))).put("frames",frames);
            var labels=new JSONObject().put("schema",AssociationAudit134.VERSION).put("objectKey","subject").put("annotator","Independent source geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Five stationary frames; independent geometry, not tracker outputs").put("rows",rows);
            Path file=dir.resolve("reference-"+pattern+"-pack.json"),label=dir.resolve("reference-"+pattern+"-labels.json");Files.writeString(file,pack.toString());Files.writeString(label,labels.toString());donors.put(new JSONObject().put("name","reference-"+pattern).put("pattern",pattern).put("pack",file.toString()).put("packSha256",PerceptionStore.hash(Files.readAllBytes(file))).put("labels",label.toString()).put("labelsSha256",PerceptionStore.hash(Files.readAllBytes(label))));
        }
        for(String name:new String[]{"reversing-motion","long-cover-return","partial-before-return","double-return","wrong-only","near-copy-only","copy-veto"}){
            var frames=new JSONArray();var rows=new JSONArray();var geometry=new JSONArray();int pattern=name.equals("double-return")?15643:15641;
            int count=name.equals("reversing-motion")?32:name.equals("double-return")?38:name.equals("long-cover-return")?31:28;
            for(int i=0;i<count;i++){
                boolean continuous=name.equals("reversing-motion"),two=name.equals("double-return"),partialCase=name.equals("partial-before-return");
                boolean start=i<6,second=two&&i>=24;
                boolean cover=!continuous&&(partialCase?i>=6&&i<=10:two?(i>=6&&i<=11||i>=24&&i<=29):i>=6&&i<=17);
                int returned=partialCase?11:two?second?30:12:18;
                boolean wrong=name.equals("wrong-only")&&i>=18,near=name.equals("near-copy-only")&&i>=18,copy=name.equals("copy-veto")&&i>=18,partial=partialCase&&i>=11&&i<=14;
                double x=continuous?40+.65*(i<=16?i:32-i):start?40:second?80+(i-30)*.35:124+Math.max(0,i-returned)*.35;
                double y=continuous||start?56:second?60:78,angle=continuous||start?0:partialCase?-43:two?second?137:-103:103;
                var image=new BufferedImage(192,144,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(45,51,59));g.fillRect(0,0,192,144);g.dispose();
                if(!cover){var target=patch(wrong?15644:pattern);if(near){var edit=target.createGraphics();edit.setColor(Color.BLACK);edit.fillRect(0,0,16,32);edit.dispose();}draw(image,target,x,y,angle);if(copy)draw(image,patch(pattern),40,56,0);if(partial){var edit=image.createGraphics();edit.setColor(new Color(45,51,59));edit.fillRect(85,78,90,65);edit.dispose();}}
                var bytes=new ByteArrayOutputStream();ImageIO.write(image,"png",bytes);byte[] raw=bytes.toByteArray();String hash=PerceptionStore.hash(raw);frames.put(new JSONObject().put("sourceFrame",i+1).put("image",Base64.getEncoder().encodeToString(raw)).put("imageSha256",hash));
                geometry.put(new JSONObject().put("ordinal",i).put("x",x).put("y",y).put("degrees",angle).put("covered",cover).put("partial",partial).put("wrongOnly",wrong||near).put("copy",copy));if(i==0)continue;
                var row=new JSONObject().put("ordinal",i).put("sourceFrame",i+1).put("imageSha256",hash).put("visibility",cover?"occluded":wrong||near?"out-of-view":partial?"partial":"visible").put("note","Fixed source geometry before tracking. Partial labels describe the known full rendered target extent; covered pixels are not claimed observed. Generated, not camera footage.");
                if(!cover&&!wrong&&!near)row.put("targetBox",bounds(x,y,angle));if(wrong||near)row.put("otherBoxes",new JSONArray().put(bounds(x,y,angle)));if(copy)row.put("otherBoxes",new JSONArray().put(bounds(40,56,0)));rows.put(row);
            }
            var pack=new JSONObject().put("schema","fraynix-observation-frames-036-v1").put("title","Reserved156 "+name).put("source","New deterministic Java rendering; not physical footage").put("license","Project-generated test source").put("objects",new JSONArray().put(new JSONObject().put("key","subject").put("label","Generated target").put("box",bounds(40,56,0)))).put("frames",frames);
            var labels=new JSONObject().put("schema",AssociationAudit134.VERSION).put("objectKey","subject").put("annotator","Independent source geometry").put("method","synthetic-control").put("exposure","before-tracking").put("provenance","Reserved156 patterns/schedules fixed before frozen-method execution; no tracker calls or returned locations used").put("rows",rows);
            Path p=dir.resolve(name+"-pack.json"),l=dir.resolve(name+"-labels.json"),m=dir.resolve(name+"-geometry.json");Files.writeString(p,pack.toString());Files.writeString(l,labels.toString());Files.writeString(m,geometry.toString());
            manifest.put(new JSONObject().put("pattern",pattern).put("name",name).put("pack",p.toString()).put("packSha256",PerceptionStore.hash(Files.readAllBytes(p))).put("labels",l.toString()).put("labelsSha256",PerceptionStore.hash(Files.readAllBytes(l))).put("geometry",m.toString()).put("geometrySha256",PerceptionStore.hash(Files.readAllBytes(m))));
        }
        Files.writeString(dir.resolve("manifest.json"),new JSONObject().put("cases",manifest).put("donors",donors).put("trackingExecuted",false).toString(2));System.out.println("Seven new source-only sequences saved; no tracker executed");
    }
}

package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

public class GameReference115Test {
    static byte[] png(BufferedImage image)throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
    @Test public void actualOverviewReconstructsAndRunsWithFreshScaleAndOcclusionChecks()throws Exception{
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("reference115"),out=Path.of("build/reference115");Files.createDirectories(out);var results=new JSONObject().put("project",project.toString()).put("passed",false);
        try(var game=new EmbeddedGameClient(repo,project)){
            var levels=new GameLevels114(repo,project);var original=levels.create(new JSONObject().put("seed",7).put("lengthTiles",64));levels.launch(game,original.getString("id"));var before=game.frame();var overview=game.command(new JSONObject().put("action","overview"));assertEquals(before.getJSONObject("state").getInt("tick"),overview.getJSONObject("state").getInt("tick"));assertTrue(before.getJSONObject("state").getJSONObject("player").similar(overview.getJSONObject("state").getJSONObject("player")));
            byte[] bytes=Base64.getDecoder().decode(overview.getString("pngBase64"));Files.write(out.resolve("actual-overview.png"),bytes);var refs=new GameReference115(repo,project);var reference=refs.importImage(new JSONObject().put("pngBase64",overview.getString("pngBase64")));results.put("reference",reference);Files.writeString(out.resolve("analysis.json"),results.toString(2));assertTrue(reference.optString("reason"),reference.getBoolean("accepted"));assertEquals(64,reference.getJSONObject("analysis").getJSONObject("parameters").getInt("lengthTiles"));assertEquals(7,reference.getJSONObject("analysis").getJSONObject("parameters").getInt("seed"));assertEquals(12,reference.getJSONObject("analysis").getJSONObject("grid").getInt("tilePx"));refs.verify(reference.getString("id"));
            BufferedImage image=GameReference115.decode(bytes),scaled=new BufferedImage(1440,810,BufferedImage.TYPE_INT_RGB);for(int y=0;y<810;y++)for(int x=0;x<1440;x++)scaled.setRGB(x,y,image.getRGB(x*2/3,y*2/3));Files.write(out.resolve("scaled-test.png"),png(scaled));var s=refs.importImage(new JSONObject().put("pngBase64",Base64.getEncoder().encodeToString(png(scaled))));results.put("scaled",s);assertTrue(s.optString("reason"),s.getBoolean("accepted"));assertTrue(s.getJSONObject("analysis").getJSONObject("parameters").similar(reference.getJSONObject("analysis").getJSONObject("parameters")));
            BufferedImage cropped=image.getSubimage(0,200,960,340);var c=refs.importImage(new JSONObject().put("pngBase64",Base64.getEncoder().encodeToString(png(cropped))));results.put("skyCrop",c);assertTrue(c.optString("reason"),c.getBoolean("accepted"));
            var occluded=GameReference115.decode(bytes);int n=reference.getJSONObject("analysis").getJSONObject("grid").getInt("tilePx"),bottom=reference.getJSONObject("analysis").getJSONObject("grid").getInt("worldBottomY");for(int y=bottom-3*n;y<bottom-2*n;y++)for(int x=3*n;x<4*n;x++)occluded.setRGB(x,y,0x5c94fc);Files.write(out.resolve("occluded-test.png"),png(occluded));var bad=refs.importImage(new JSONObject().put("pngBase64",Base64.getEncoder().encodeToString(png(occluded))));results.put("occluded",bad);assertFalse(bad.getBoolean("accepted"));assertThrows(IllegalArgumentException.class,()->refs.verify(bad.getString("id")));
            var distractor=GameReference115.decode(bytes);for(int y=100;y<125;y++)for(int x=200;x<225;x++)distractor.setRGB(x,y,0xff0000);var extra=refs.importImage(new JSONObject().put("pngBase64",Base64.getEncoder().encodeToString(png(distractor))));results.put("distractor",extra);assertFalse(extra.getBoolean("accepted"));
            Path originalPng=project.resolve("game-references115").resolve(reference.getString("id")).resolve("reference.png");Files.write(originalPng,new byte[]{1,2,3});assertThrows(IllegalStateException.class,()->refs.verify(reference.getString("id")));Files.write(originalPng,bytes);
            game.close();try(var tasks=new GameTask115(repo,project,game)){String id=UUID.randomUUID().toString();var t=tasks.start(new JSONObject().put("id",id).put("request","Reconstruct a playable Mario level from reference="+reference.getString("id")));for(int i=0;i<450&&t.getString("status").equals("running");i++)t=tasks.advance(id);results.put("task",t);assertEquals(t.optString("reason"),"completed",t.getString("status"));tasks.verify(id);assertNotEquals(original.getString("id"),t.getJSONObject("audit").getString("candidateId"));}
            results.put("passed",true);
        }finally{Files.writeString(out.resolve("result.json"),results.toString(2));}
    }
}

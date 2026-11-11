package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;
import java.awt.image.BufferedImage;

public class GameReference116Test {
    @Test public void freshNativeImagesReconstructAndPlayWithRefusalsRetained() throws Exception {
        Path repo=Path.of("..").toAbsolutePath().normalize(),project=Files.createTempDirectory("reference116");
        Path out=Path.of(System.getProperty("reference116.output","build/reference116"));Files.createDirectories(out);
        var result=new JSONObject().put("project",project.toString()).put("passed",false).put("cases",new JSONArray()).put("refusals",new JSONArray());
        int[][] definitions={{1,48,1,2,2,1},{4,56,2,1,3,2},{8,64,3,2,1,1},{11,72,2,2,1,1},{15,56,5,2,3,2},{17,64,2,1,1,1}};
        byte[] lastNative=null;JSONObject lastAnalysis=null;
        try(var game=new EmbeddedGameClient(repo,project);var tasks=new GameTask115(repo,project,game)){
            var levels=new GameLevels114(repo,project);var refs=new GameReference115(repo,project);
            for(int[] d:definitions){
                var params=new JSONObject().put("seed",d[0]).put("lengthTiles",d[1]).put("gapTiles",d[2]).put("secondGapTiles",d[3]);
                var original=levels.create115(UUID.randomUUID().toString(),params);levels.launch(game,original.getString("id"));
                var overview=game.command(new JSONObject().put("action","overview"));lastNative=Base64.getDecoder().decode(overview.getString("pngBase64"));
                Files.write(out.resolve("seed"+d[0]+"-native.png"),lastNative);var image=GameReference115.decode(lastNative);
                int cropTop=150,sourceWidth=d[1]*12+18,offset=7;
                var transformed=new BufferedImage(sourceWidth*d[4]/d[5]+offset,(540-cropTop)*d[4]/d[5],BufferedImage.TYPE_INT_RGB);
                for(int y=0;y<transformed.getHeight();y++)for(int x=0;x<transformed.getWidth();x++)transformed.setRGB(x,y,x<offset?image.getRGB(0,0):image.getRGB((x-offset)*d[5]/d[4],cropTop+y*d[5]/d[4]));
                byte[] png=GameReference115Test.png(transformed);Files.write(out.resolve("seed"+d[0]+"-reference.png"),png);
                var reference=refs.importImage(new JSONObject().put("pngBase64",Base64.getEncoder().encodeToString(png)));
                var row=new JSONObject().put("sourceCandidate",original).put("sourceState",overview.getJSONObject("state")).put("transform",new JSONObject().put("cropTop",cropTop).put("sourceWidth",sourceWidth).put("scaleNumerator",d[4]).put("scaleDenominator",d[5]).put("leftPadding",offset)).put("reference",reference);result.getJSONArray("cases").put(row);Files.writeString(out.resolve("result.json"),result.toString(2));
                assertTrue(reference.optString("reason"),reference.getBoolean("accepted"));lastAnalysis=reference.getJSONObject("analysis");assertTrue(params.similar(lastAnalysis.getJSONObject("parameters")));refs.verify(reference.getString("id"));
                String id=UUID.randomUUID().toString();var t=tasks.start(new JSONObject().put("id",id).put("request","Reconstruct a playable Mario level from reference="+reference.getString("id")));
                for(int i=0;i<650&&t.getString("status").equals("running");i++)t=tasks.advance(id);
                row.put("task",t);Files.writeString(out.resolve("result.json"),result.toString(2));assertEquals(t.optString("reason"),"completed",t.getString("status"));tasks.verify(id);assertNotEquals(original.getString("id"),t.getJSONObject("audit").getString("candidateId"));
            }
            for(String change:List.of("missing-finish","incomplete-floor","covered-player","blank")){
                var image=GameReference115.decode(lastNative);int bg=image.getRGB(0,0),n=12;
                if(change.equals("blank")){for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setRGB(x,y,bg);}
                else {int x=change.equals("missing-finish")?(64-5)*n-n/2:change.equals("covered-player")?3*n:8*n;int y=change.equals("missing-finish")?540-7*n:change.equals("covered-player")?540-3*n:540-n;for(int v=y;v<y+n;v++)for(int u=x;u<x+n;u++)image.setRGB(u,v,bg);}
                byte[] png=GameReference115Test.png(image);Files.write(out.resolve(change+".png"),png);var refusal=refs.importImage(new JSONObject().put("pngBase64",Base64.getEncoder().encodeToString(png)));result.getJSONArray("refusals").put(new JSONObject().put("change",change).put("reference",refusal));assertFalse(change,refusal.getBoolean("accepted"));assertThrows(IllegalArgumentException.class,()->refs.verify(refusal.getString("id")));
            }
            result.put("passed",true);
        } finally {Files.writeString(out.resolve("result.json"),result.toString(2));}
    }
}

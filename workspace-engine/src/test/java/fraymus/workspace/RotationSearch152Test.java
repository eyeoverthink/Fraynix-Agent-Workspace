package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RotationSearch152Test {
    static final int W=192,H=144;
    static final PixelTracker.Box SEED=new PixelTracker.Box(24./W,24./H,32./W,64./H);
    static BufferedImage patch(int seed){
        var out=new BufferedImage(32,64,BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();var random=new java.util.Random(seed);
        for(int y=0;y<8;y++)for(int x=0;x<4;x++){g.setColor(new Color(30+random.nextInt(200),30+random.nextInt(200),30+random.nextInt(200)));g.fillRect(x*8,y*8,8,8);}g.dispose();return out;
    }
    static BufferedImage scene(int pattern,double x,double y,double angle,boolean copy){
        var out=new BufferedImage(W,H,BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();g.setColor(new Color(45,51,59));g.fillRect(0,0,W,H);g.dispose();
        if(pattern>=0)draw(out,patch(pattern),x,y,angle);if(copy)draw(out,patch(pattern),40,56,0);return out;
    }
    static void draw(BufferedImage image,BufferedImage patch,double x,double y,double angle){
        var g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.translate(x,y);g.rotate(Math.toRadians(angle));g.drawImage(patch,-16,-32,null);g.dispose();
    }
    static JSONObject empty(){return new JSONObject().put("examples",new JSONArray());}
    @Test public void findsTranslatedRotatedTargetWithoutLocationInput(){
        var initial=scene(15201,40,56,0,false);var search=new RotationSearch152(initial,SEED,empty());
        for(double angle:new double[]{0,30,90,-45,170}){
            var matches=search.search(scene(15201,130,78,angle,false));assertFalse(matches.isEmpty());var m=matches.getFirst();
            System.out.println("rotation152 development angle="+angle+" match="+m.json());
            assertTrue("Pose should find target: "+angle,m.score().getBoolean("strong"));assertTrue(m.unique());assertEquals(130,m.pose().x(),2);assertEquals(78,m.pose().y(),2);
        }
    }
    @Test public void missingWrongAndIdenticalCompetitorAreNotUnique(){
        var search=new RotationSearch152(scene(15201,40,56,0,false),SEED,empty());
        assertTrue(search.search(scene(-1,0,0,0,false)).stream().noneMatch(RotationSearch152.Match::unique));
        assertTrue(search.search(scene(15202,130,78,30,false)).stream().noneMatch(RotationSearch152.Match::unique));
        var copies=search.search(scene(15201,130,78,30,true));System.out.println("copies="+copies.stream().map(m->m.json().toString()).toList());assertTrue(copies.stream().noneMatch(RotationSearch152.Match::unique));
    }
    @Test public void temporalEvidenceRestartsAfterCoverAndCompetitorVetoPersists(){
        var tracker=new RotationReturn152(scene(15201,40,56,0,false),SEED,empty());
        JSONObject result=null;for(int i=0;i<4;i++)result=tracker.update(scene(15201,130,78,30,false));assertNotNull(result);assertFalse(result.isNull("box"));
        assertTrue(tracker.update(scene(-1,0,0,0,false)).isNull("box"));
        for(int i=0;i<3;i++)assertTrue(tracker.update(scene(15201,130,78,30,false)).isNull("box"));
        assertFalse(tracker.update(scene(15201,130,78,30,false)).isNull("box"));
        assertTrue(tracker.update(scene(15201,130,78,30,true)).getBoolean("ambiguityRetained"));
        for(int i=0;i<5;i++)assertTrue(tracker.update(scene(15201,130,78,30,false)).isNull("box"));
    }
    @Test public void rejectsChangedDimensionsAndForgedReference(){
        var first=scene(15201,40,56,0,false);var search=new RotationSearch152(first,SEED,empty());
        assertThrows(IllegalArgumentException.class,()->search.search(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB)));
        var d=StructureSearch149.descriptor(first,SEED);d.getJSONArray("pixels").put(0,.123);
        var memory=empty().put("examples",new JSONArray().put(new JSONObject().put("imageSha256","a").put("ordinal",1).put("structure149",d)));
        assertThrows(IllegalArgumentException.class,()->new RotationSearch152(first,SEED,memory));
    }
}

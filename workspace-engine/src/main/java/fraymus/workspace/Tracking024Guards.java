package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.json.*;

/** Separate real pixel controls; no reserved research data enters these tests. */
final class Tracking024Guards {
    static JSONObject run() throws Exception {
        BufferedImage photo;try(var in=Tracking024Guards.class.getResourceAsStream("/perception/astronaut.png")){photo=ImageIO.read(in);}
        var seed=scene(photo,false,false);var box=new PixelTracker.Box(30/640.,30/250.,160/640.,160/250.);
        var duplicate=new FlowTracker("guard","person",seed,box).update(scene(photo,true,false));
        var tracker=new FlowTracker("loss","person",seed,box);JSONObject hidden=null;for(int i=0;i<4;i++)hidden=tracker.update(scene(photo,false,true));
        var returned=tracker.update(seed);
        return new JSONObject().put("duplicateAmbiguous",duplicate.isNull("box")&&duplicate.getString("status").equals("lost")).put("hiddenHasNoCurrentBox",hidden.isNull("box")&&hidden.getString("status").equals("lost")).put("reappearanceRecovered",!returned.isNull("box")&&returned.getString("status").equals("reacquired")).put("duplicateObservation",duplicate).put("hiddenObservation",hidden).put("returnedObservation",returned);
    }
    private static BufferedImage scene(BufferedImage photo,boolean duplicate,boolean hidden){var image=new BufferedImage(640,250,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(0x233b50));g.fillRect(0,0,640,250);if(!hidden)g.drawImage(photo,30,30,160,160,null);if(duplicate)g.drawImage(photo,350,30,160,160,null);g.dispose();return image;}
}

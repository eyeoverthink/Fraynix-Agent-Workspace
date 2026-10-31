package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import org.json.*;

/** Real pixel negative controls. These do not contain research evaluation sequence data. */
final class TrackingGuards {
    static JSONObject run(String profile) throws Exception {BufferedImage photo;try(var in=TrackingGuards.class.getResourceAsStream("/perception/astronaut.png")){photo=ImageIO.read(in);}var seed=scene(photo,false,false);var box=new PixelTracker.Box(30/640.,30/250.,160/640.,160/250.);var tracker=TrackingEvaluation.tracker(profile,"guard","person",seed,box);var duplicate=tracker.update(scene(photo,true,false));var hidden=TrackingEvaluation.tracker(profile,"loss","person",seed,box).update(scene(photo,false,true));return new JSONObject().put("duplicateAmbiguous",duplicate.getString("status").equals("lost")&&duplicate.isNull("box")).put("hiddenHasNoCurrentBox",hidden.getString("status").equals("lost")&&hidden.isNull("box")).put("duplicateObservation",duplicate).put("hiddenObservation",hidden);}
    private static BufferedImage scene(BufferedImage photo,boolean duplicate,boolean hidden){var image=new BufferedImage(640,250,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();g.setColor(new Color(0x233b50));g.fillRect(0,0,640,250);if(!hidden)g.drawImage(photo,30,30,160,160,null);if(duplicate)g.drawImage(photo,350,30,160,160,null);g.dispose();return image;}
}

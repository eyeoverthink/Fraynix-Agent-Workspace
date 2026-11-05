package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import org.json.*;

public class ScreenPixels043Test {
    static BufferedImage image(int shift,Color background){var b=new BufferedImage(200,120,BufferedImage.TYPE_INT_RGB);var g=b.createGraphics();g.setColor(background);g.fillRect(0,0,200,120);g.setColor(Color.WHITE);g.fillRect(20+shift,30,60,50);g.setColor(Color.ORANGE);g.fillRect(110,70,40,20);g.dispose();return b;}
    static byte[] png(BufferedImage image)throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
    @Test public void exactOriginalPixelDifferenceAndBoundingBox(){var a=new BufferedImage(200,120,BufferedImage.TYPE_INT_RGB);var b=new BufferedImage(200,120,BufferedImage.TYPE_INT_RGB);var g=b.createGraphics();g.setColor(Color.WHITE);g.fillRect(10,20,20,20);g.dispose();var d=ScreenPixels043.difference(a,b);assertTrue(d.getBoolean("comparable"));assertEquals(400,d.getInt("changedPixels"));assertEquals(400/24000.0,d.getDouble("changedFraction"),1e-12);assertEquals("[10,20,30,40]",d.getJSONArray("changedBounds").toString());assertEquals(0,ScreenPixels043.difference(a,a).getInt("changedPixels"));assertFalse(ScreenPixels043.difference(a,new BufferedImage(201,120,BufferedImage.TYPE_INT_RGB)).getBoolean("comparable"));}
    @Test public void descriptorIsOwnedDeterministicAndRanksNewLayoutBeforeUnrelatedColor(){var a=ScreenPixels043.signature(image(0,new Color(15,40,60)));var shifted=ScreenPixels043.signature(image(8,new Color(15,40,60)));var unrelated=ScreenPixels043.signature(image(8,new Color(210,25,25)));assertEquals(768,a.getJSONArray("vector").length());assertTrue(a.getBoolean("informative"));assertEquals(1,ScreenPixels043.similarity(a,a),0);assertTrue(ScreenPixels043.similarity(a,shifted)>ScreenPixels043.similarity(a,unrelated));assertFalse(ScreenPixels043.signature(new BufferedImage(200,120,BufferedImage.TYPE_INT_RGB)).getBoolean("informative"));}
    @Test public void decoderChecksActualFormatAndDimensionsBeforeAllocatingFrame()throws Exception{assertThrows(IOException.class,()->ScreenPixels043.decode(new byte[24]));assertThrows(IOException.class,()->ScreenPixels043.decode(png(new BufferedImage(1921,20,BufferedImage.TYPE_INT_RGB))));var source=image(0,Color.BLUE);assertEquals(200,ScreenPixels043.decode(png(source)).getWidth());var derivative=ScreenPixels043.thumbnail(source,100);assertEquals(100,ImageIO.read(new ByteArrayInputStream(derivative)).getWidth());}
}

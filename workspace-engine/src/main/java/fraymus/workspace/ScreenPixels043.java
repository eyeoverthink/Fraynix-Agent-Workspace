package fraymus.workspace;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.json.*;

/** Owned image measurements. Visual similarity is retrieval evidence, never semantic truth. */
final class ScreenPixels043 {
    static BufferedImage decode(byte[] bytes)throws IOException{
        if(bytes.length>6_000_000||bytes.length<24||bytes[0]!=(byte)137||bytes[1]!=80||bytes[2]!=78||bytes[3]!=71)throw new IOException("Bounded original PNG required");
        try(var in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(in);if(!readers.hasNext())throw new IOException("Invalid screen image");var reader=readers.next();try{reader.setInput(in);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<16||h<16||w>1920||h>1200||(long)w*h>2_304_000)throw new IOException("Screen image dimensions exceed1920x1200");return reader.read(0);}finally{reader.dispose();}}
    }
    static JSONObject signature(BufferedImage image){
        int cols=16,rows=12,w=image.getWidth(),h=image.getHeight();var vector=new JSONArray();double lumaSum=0,lumaSq=0;long count=0;
        for(int cy=0;cy<rows;cy++)for(int cx=0;cx<cols;cx++){
            double r=0,g=0,b=0,edge=0;int n=0;
            for(int y=cy*h/rows;y<(cy+1)*h/rows;y+=2)for(int x=cx*w/cols;x<(cx+1)*w/cols;x+=2){int p=image.getRGB(x,y);r+=(p>>16)&255;g+=(p>>8)&255;b+=p&255;double l=luma(p);lumaSum+=l;lumaSq+=l*l;count++;if(x+1<w)edge+=Math.abs(l-luma(image.getRGB(x+1,y)));if(y+1<h)edge+=Math.abs(l-luma(image.getRGB(x,y+1)));n++;}
            vector.put(r/n/255).put(g/n/255).put(b/n/255).put(edge/n/510);
        }
        double n=count;double variance=Math.max(0,lumaSq/n-Math.pow(lumaSum/n,2));
        return new JSONObject().put("algorithm","rgb-edge-spatial16x12-v1").put("dimensions",vector.length()).put("vector",vector).put("contrast",Math.sqrt(variance)/255).put("informative",Math.sqrt(variance)/255>.025);
    }
    private static double luma(int p){return .2126*((p>>16)&255)+.7152*((p>>8)&255)+.0722*(p&255);}
    static double similarity(JSONObject a,JSONObject b){var x=a.getJSONArray("vector");var y=b.getJSONArray("vector");if(!a.getString("algorithm").equals(b.getString("algorithm"))||x.length()!=768||y.length()!=768)throw new IllegalArgumentException("Visual signature version changed");double error=0;for(int i=0;i<x.length();i++)error+=Math.abs(x.getDouble(i)-y.getDouble(i));return Math.max(0,1-error/x.length());}
    static JSONObject difference(BufferedImage a,BufferedImage b){
        if(a.getWidth()!=b.getWidth()||a.getHeight()!=b.getHeight())return new JSONObject().put("comparable",false).put("reason","Viewport/pixel dimensions changed; do not invent a motion delta");
        int w=a.getWidth(),h=a.getHeight(),left=w,top=h,right=-1,bottom=-1;long changed=0;double absolute=0;
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){int p=a.getRGB(x,y),q=b.getRGB(x,y);int dr=Math.abs(((p>>16)&255)-((q>>16)&255)),dg=Math.abs(((p>>8)&255)-((q>>8)&255)),db=Math.abs((p&255)-(q&255));absolute+=dr+dg+db;if(Math.max(dr,Math.max(dg,db))>24){changed++;left=Math.min(left,x);right=Math.max(right,x);top=Math.min(top,y);bottom=Math.max(bottom,y);}}
        return new JSONObject().put("comparable",true).put("width",w).put("height",h).put("changedPixels",changed).put("changedFraction",changed/(double)(w*h)).put("meanAbsoluteChannelDifference",absolute/(w*h*3.0*255)).put("thresholdChannelUnits",24).put("changedBounds",changed==0?JSONObject.NULL:new JSONArray(new int[]{left,top,right+1,bottom+1})).put("meaning","Digital pixel change, not proof that an intended action succeeded, an object moved physically or an identity was recognized");
    }
    static byte[] thumbnail(BufferedImage source,int maximum)throws IOException{double scale=Math.min(1,maximum/(double)Math.max(source.getWidth(),source.getHeight()));var out=new BufferedImage(Math.max(1,(int)(source.getWidth()*scale)),Math.max(1,(int)(source.getHeight()*scale)),BufferedImage.TYPE_INT_RGB);var g=out.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,out.getWidth(),out.getHeight());g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(source,0,0,out.getWidth(),out.getHeight(),null);g.dispose();var bytes=new ByteArrayOutputStream();ImageIO.write(out,"jpg",bytes);return bytes.toByteArray();}
}

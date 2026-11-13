package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.util.Random;
import org.json.*;
/** Exposed synthetic lighting/texture development, never independent camera evidence. */
public final class RestorationImageStudy140 {
 static BufferedImage image(int changed,double shade){return image(changed,shade,0);}
 static BufferedImage image(int changed,double shade,double originalFraction){
  var out=new BufferedImage(320,220,BufferedImage.TYPE_INT_RGB);for(int y=0;y<220;y++)for(int x=0;x<320;x++)out.setRGB(x,y,0x202020);
  var a=new Random(140);var b=new Random(9140);
  for(int y=0;y<120;y++)for(int x=0;x<90;x++){int original=40+a.nextInt(140),replacement=40+b.nextInt(140);double factor=x<30?shade:1;int q=(int)((x<changed?replacement*(1-originalFraction)+original*originalFraction:original)*factor);out.setRGB(40+x,35+y,(q<<16)|(q<<8)|q);}
  return out;
 }
 public static void main(String[] args){
  for(double shade:new double[]{.3,.5,.7,.85}){
   var seed=CorroboratedGuard140Test.seed();var first=image(0,1);var guard=new CorroboratedGuard140("id","object",first,seed,true);
   for(int i=1;i<=20;i++){
    var frame=i<=12?image(Math.min(90,i*9),1):image(90,1+(shade-1)*Math.min(1,(i-12)/4.),Math.min(1,(i-12)/4.));var result=guard.update(frame);var m=result.getJSONObject("drift140");
    System.out.println(new JSONObject().put("shade",shade).put("frame",i).put("status",result.getString("status")).put("motion",m.getJSONObject("proposal").getJSONObject("motion").getString("method")).put("appearance",m.getJSONObject("appearance")).put("structure",m.getJSONObject("structure")).put("recovery",m.getJSONObject("recovery")).put("reasons",m.getJSONArray("reasons")));
   }
  }
 }
}

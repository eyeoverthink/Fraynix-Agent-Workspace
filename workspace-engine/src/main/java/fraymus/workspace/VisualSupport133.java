package fraymus.workspace;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;
/** Frozen lower-object contrast and connected visual support; no semantic/physical contact claim. */
final class VisualSupport133 {
    private final double[] lowerEvidence=new double[512];
    private final int width,height;
    VisualSupport133(BufferedImage first,PixelTracker.Box seed){width=first.getWidth();height=first.getHeight();initializeLower(first,seed);}
    private static int bin(int c){return ((c>>21)&7)*64+((c>>13)&7)*8+((c>>5)&7);}
    private void initializeLower(BufferedImage image,PixelTracker.Box b){
        double[] fg=new double[512],bg=new double[512];int nf=0,nb=0;
        for(int y=0;y<height;y++)for(int x=0;x<width;x++){
            double u=(x+.5)/width,v=(y+.5)/height;
            if(v<b.y()+.55*b.h()||v>b.y()+1.08*b.h()||u<b.x()-b.w()||u>b.x()+2*b.w())continue;
            if(u>=b.x()&&u<=b.x()+b.w()&&v<=b.y()+b.h()){fg[bin(image.getRGB(x,y))]++;nf++;}
            else{bg[bin(image.getRGB(x,y))]++;nb++;}
        }
        for(int i=0;i<512;i++)lowerEvidence[i]=((fg[i]+.2)/(nf+102.4))/((bg[i]+.2)/(nb+102.4));
    }
    static JSONObject unavailable(String reason){return new JSONObject().put("status","unobservable").put("reason",reason).put("pointImage",JSONObject.NULL).put("meaning","Visual support candidate only; not proof of physical ground contact");}
    JSONObject support(BufferedImage image,PixelTracker.Box b){
        if(image.getWidth()!=width||image.getHeight()!=height||!b.measurable())throw new IllegalArgumentException("Original dimensions and a valid retained box required");
        // A finite connected lower-object foreground is required. The box bottom is never the answer by fallback.
        int x0=Math.max(0,(int)((b.x()-.7*b.w())*width)),x1=Math.min(width-1,(int)((b.x()+1.7*b.w())*width));
        int y0=Math.max(0,(int)((b.y()+.45*b.h())*height)),y1=Math.min(height-1,(int)((b.y()+1.18*b.h())*height));
        int rw=x1-x0+1,rh=y1-y0+1;if(rw<3||rh<3)return unavailable("insufficient lower-object pixels");
        boolean[] mask=new boolean[rw*rh],seen=new boolean[mask.length];
        for(int y=0;y<rh;y++)for(int x=0;x<rw;x++)mask[y*rw+x]=lowerEvidence[bin(image.getRGB(x+x0,y+y0))]>=1.6;
        var components=new ArrayList<int[]>();var queue=new int[mask.length];
        for(int start=0;start<mask.length;start++)if(mask[start]&&!seen[start]){
            int size=1,at=0;queue[0]=start;seen[start]=true;
            while(at<size){int p=queue[at++],x=p%rw,y=p/rw;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int nx=x+dx,ny=y+dy;if(nx>=0&&ny>=0&&nx<rw&&ny<rh){int n=ny*rw+nx;if(mask[n]&&!seen[n]){seen[n]=true;queue[size++]=n;}}}}
            if(size>=Math.max(6,b.w()*width*b.h()*height*.015)){int inside=0;for(int i=0;i<size;i++){double x=(queue[i]%rw+x0+.5)/width,y=(queue[i]/rw+y0+.5)/height;if(x>=b.x()&&x<=b.x()+b.w()&&y<=b.y()+.95*b.h())inside++;}if(inside>=size*.2)components.add(Arrays.copyOf(queue,size));}
        }
        if(components.isEmpty())return unavailable("no connected lower-object support");
        components.sort(Comparator.comparingInt((int[] c)->c.length).reversed());var part=components.get(0);
        if(part.length>rw*rh*.65)return unavailable("foreground/background separation insufficient");
        int[] rows=new int[rh];for(int p:part)rows[p/rw]++;
        int bottom=-1;for(int y=rh-1;y>=0;y--)if(rows[y]>=Math.max(2,b.w()*width*.08)){bottom=y;break;}
        if(bottom<0||bottom+y0<(b.y()+.85*b.h())*height)return unavailable("lower support not visible");
        if(bottom>=rh-2||bottom+y0>=height-2)return unavailable("support clipped by image or search boundary");
        int count=0;double sum=0;int lo=width,hi=0;for(int p:part)if(p/rw>=bottom-1&&p/rw<=bottom){int x=p%rw+x0;sum+=x+.5;count++;lo=Math.min(lo,x);hi=Math.max(hi,x);}
        if(count<2||lo<=0||hi>=width-1)return unavailable("support clipped or too sparse");
        return new JSONObject().put("status","visual-support").put("reason","Connected seed-contrasting lower-object pixels terminate inside image/search region")
            .put("pointImage",new JSONArray(new double[]{sum/count/width,(bottom+y0+1.)/height})).put("supportPixels",count).put("componentPixels",part.length).put("spanPixels",hi-lo+1)
            .put("meaning","Visual support candidate only; not proof of physical ground contact");
    }
}

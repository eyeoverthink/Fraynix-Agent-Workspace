package fraymus.workspace;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Candidate129: keep optical-flow support within the seeded object scale. No later reference positions enter this tracker. */
final class ScaleAwareFlow129 implements ImageTracker {
    private static final int MAX_SIDE=480,LEVELS=4,R=4;
    private final String id,label;
    private PixelTracker.Box box;
    private BufferedImage previousImage;
    private Pyramid previous;
    private int observations,lost,updates;
    private final double[] anchor;

    ScaleAwareFlow129(String id,String label,BufferedImage image,PixelTracker.Box box){
        this.id=id;this.label=label;this.box=box;previousImage=image;previous=new Pyramid(image);
        anchor=descriptor(previous.p[0],box.x()*previous.width,box.y()*previous.height,box.w()*previous.width,box.h()*previous.height);
    }
    private record Point(double x,double y,double strength){}
    private record Vector(double x,double y,double nx,double ny,double fb,double corr){}
    private static final class Plane {
        final int w,h;final double[] a;
        Plane(int w,int h){this.w=w;this.h=h;a=new double[w*h];}
        double at(double x,double y){int ix=(int)Math.floor(x),iy=(int)Math.floor(y);if(ix<0||iy<0||ix>=w-1||iy>=h-1)return Double.NaN;double fx=x-ix,fy=y-iy;int i=iy*w+ix;return (1-fy)*((1-fx)*a[i]+fx*a[i+1])+fy*((1-fx)*a[i+w]+fx*a[i+w+1]);}
        boolean contains(double x,double y){return x>=R+2&&y>=R+2&&x<w-R-3&&y<h-R-3;}
    }
    private static final class Pyramid {
        final Plane[] p=new Plane[LEVELS];final int width,height;
        Pyramid(BufferedImage image){
            double s=Math.min(1,MAX_SIDE/(double)Math.max(image.getWidth(),image.getHeight()));
            width=Math.max(24,(int)Math.round(image.getWidth()*s));height=Math.max(24,(int)Math.round(image.getHeight()*s));
            var small=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var g=small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);g.drawImage(image,0,0,width,height,null);g.dispose();
            p[0]=new Plane(width,height);for(int y=0;y<height;y++)for(int x=0;x<width;x++){int c=small.getRGB(x,y);p[0].a[y*width+x]=(.299*((c>>16)&255)+.587*((c>>8)&255)+.114*(c&255))/255.;}
            for(int l=1;l<LEVELS;l++){var old=p[l-1];var now=p[l]=new Plane(old.w/2,old.h/2);for(int y=0;y<now.h;y++)for(int x=0;x<now.w;x++){double sum=0,weight=0;for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){int xx=Math.max(0,Math.min(old.w-1,2*x+dx)),yy=Math.max(0,Math.min(old.h-1,2*y+dy));double k=(dx==0?2:1)*(dy==0?2:1);sum+=old.a[yy*old.w+xx]*k;weight+=k;}now.a[y*now.w+x]=sum/weight;}}
        }
    }
    private static double corner(Plane p,int x,int y){double xx=0,xy=0,yy=0;for(int j=-2;j<=2;j++)for(int i=-2;i<=2;i++){int k=(y+j)*p.w+x+i;double gx=(p.a[k+1]-p.a[k-1])*.5,gy=(p.a[k+p.w]-p.a[k-p.w])*.5;xx+=gx*gx;xy+=gx*gy;yy+=gy*gy;}return .5*(xx+yy-Math.sqrt((xx-yy)*(xx-yy)+4*xy*xy));}
    private static java.util.List<Point> points(Plane p,PixelTracker.Box b){
        var out=new ArrayList<Point>();double left=b.x()*p.w,top=b.y()*p.h,bw=b.w()*p.w,bh=b.h()*p.h;
        // One local corner per cell keeps features spatially distributed, inside the disclosed rectangle.
        for(int gy=0;gy<10;gy++)for(int gx=0;gx<10;gx++){Point best=null;
            int x0=Math.max(Math.max(R+3,(int)Math.ceil(left+R+2)),(int)(left+bw*(.12+.076*gx))),x1=Math.min(Math.min(p.w-R-4,(int)Math.floor(left+bw-R-2)),(int)(left+bw*(.12+.076*(gx+1))));
            int y0=Math.max(Math.max(R+3,(int)Math.ceil(top+R+2)),(int)(top+bh*(.08+.084*gy))),y1=Math.min(Math.min(p.h-R-4,(int)Math.floor(top+bh-R-2)),(int)(top+bh*(.08+.084*(gy+1))));
            for(int y=y0;y<=y1;y+=2)for(int x=x0;x<=x1;x+=2){double q=corner(p,x,y);if(q>.0005&&(best==null||q>best.strength))best=new Point(x,y,q);}
            if(best!=null){var q=best;if(out.stream().noneMatch(v->Math.hypot(v.x-q.x,v.y-q.y)<3))out.add(best);}
        }
        return out;
    }
    private static double[] flow(Pyramid a,Pyramid b,double px,double py,int maxLevel){
        double dx=0,dy=0;boolean solved=false;
        for(int l=maxLevel;l>=0;l--){if(l<maxLevel){dx*=2;dy*=2;}double f=1<<l,x=px/f,y=py/f;var src=a.p[l];var dst=b.p[l];if(!src.contains(x,y))continue;
            int n=(2*R+1)*(2*R+1),k=0;var values=new double[n];var gradX=new double[n];var gradY=new double[n];double mean=0,hxx=0,hxy=0,hyy=0;
            for(int j=-R;j<=R;j++)for(int i=-R;i<=R;i++){double xx=x+i,yy=y+j;values[k]=src.at(xx,yy);mean+=values[k];double vx=gradX[k]=(src.at(xx+1,yy)-src.at(xx-1,yy))*.5,vy=gradY[k]=(src.at(xx,yy+1)-src.at(xx,yy-1))*.5;hxx+=vx*vx;hxy+=vx*vy;hyy+=vy*vy;k++;}
            mean/=n;double det=hxx*hyy-hxy*hxy;if(det<1e-8)continue;
            for(int iter=0;iter<18;iter++){
                if(!dst.contains(x+dx,y+dy))return null;double currentMean=0;for(int j=-R;j<=R;j++)for(int i=-R;i<=R;i++)currentMean+=dst.at(x+dx+i,y+dy+j);currentMean/=n;
                double ex=0,ey=0;k=0;for(int j=-R;j<=R;j++)for(int i=-R;i<=R;i++){double error=dst.at(x+dx+i,y+dy+j)-currentMean-values[k]+mean;ex+=gradX[k]*error;ey+=gradY[k]*error;k++;}
                double ux=(hyy*ex-hxy*ey)/det,uy=(hxx*ey-hxy*ex)/det;double magnitude=Math.hypot(ux,uy);if(!Double.isFinite(magnitude))return null;if(magnitude>4){ux*=4/magnitude;uy*=4/magnitude;}dx-=ux;dy-=uy;solved=true;if(ux*ux+uy*uy<.0004)break;
            }
        }
        return solved?new double[]{px+dx,py+dy}:null;
    }
    private static double correlation(Plane a,Plane b,double x,double y,double nx,double ny){
        if(!a.contains(x,y)||!b.contains(nx,ny))return -1;double ma=0,mb=0,aa=0,bb=0,ab=0,n=(2*R+1)*(2*R+1);
        for(int j=-R;j<=R;j++)for(int i=-R;i<=R;i++){double u=a.at(x+i,y+j),v=b.at(nx+i,ny+j);ma+=u;mb+=v;aa+=u*u;bb+=v*v;ab+=u*v;}
        double va=aa-ma*ma/n,vb=bb-mb*mb/n;return va<1e-6||vb<1e-6?-1:(ab-ma*mb/n)/Math.sqrt(va*vb);
    }
    private static double median(double[] a){Arrays.sort(a);int n=a.length;return n%2==0?(a[n/2-1]+a[n/2])*.5:a[n/2];}
    private static double rounded(double v){return Math.rint(v*10000)/10000;}
    private static double[] descriptor(Plane p,double x,double y,double w,double h){var d=new double[144];for(int j=0;j<12;j++)for(int i=0;i<12;i++)d[j*12+i]=p.at(x+(i+.5)*w/12,y+(j+.5)*h/12);return d;}
    private double templateScore(Plane p,double x,double y,double w,double h){double ma=0,mb=0,aa=0,bb=0,ab=0;for(int j=0;j<12;j++)for(int i=0;i<12;i++){double a=anchor[j*12+i],b=p.at(x+(i+.5)*w/12,y+(j+.5)*h/12);ma+=a;mb+=b;aa+=a*a;bb+=b*b;ab+=a*b;}double va=aa-ma*ma/144,vb=bb-mb*mb/144;return va<1e-6||vb<1e-6?-1:(ab-ma*mb/144)/Math.sqrt(va*vb);}
    private boolean duplicate(Plane p,PixelTracker.Box expected){
        double w=expected.w()*p.w,h=expected.h()*p.h;
        if(templateScore(p,expected.x()*p.w,expected.y()*p.h,w,h)<.95)return false;
        double best=-1,bx=0,by=0;
        for(int y=0;y<p.h-h-1;y+=4)for(int x=0;x<p.w-w-1;x+=4){if(PixelTracker.iou(expected,new PixelTracker.Box(x/(double)p.w,y/(double)p.h,w/p.w,h/p.h))>=.2)continue;double score=templateScore(p,x,y,w,h);if(score>best){best=score;bx=x;by=y;}}
        if(best<.65)return false;
        for(double y=Math.max(0,by-4);y<Math.min(p.h-h-1,by+5);y++)for(double x=Math.max(0,bx-4);x<Math.min(p.w-w-1,bx+5);x++)if(templateScore(p,x,y,w,h)>.97)return true;
        return false;
    }
    public JSONObject update(BufferedImage image){
        var current=new Pyramid(image);boolean dimensions=current.width==previous.width&&current.height==previous.height;
        int maxLevel=0;double extent=Math.min(box.w()*previous.width,box.h()*previous.height);while(maxLevel<LEVELS-1&&(2*R+1)*(1<<(maxLevel+1))<=extent)maxLevel++;
        var vectors=new ArrayList<Vector>();var seeds=points(previous.p[0],box);double medianFb=0;
        if(dimensions)for(var p:seeds){var q=flow(previous,current,p.x,p.y,maxLevel);if(q==null)continue;var back=flow(current,previous,q[0],q[1],maxLevel);if(back==null)continue;double fb=Math.hypot(back[0]-p.x,back[1]-p.y),corr=correlation(previous.p[0],current.p[0],p.x,p.y,q[0],q[1]);if(fb<=1.5&&corr>=.75)vectors.add(new Vector(p.x,p.y,q[0],q[1],fb,corr));}
        int reliable=vectors.size();double scale=1,dx=0,dy=0;PixelTracker.Box candidate=null;var visualVectors=new JSONArray();
        if(vectors.size()>=8&&vectors.size()>=seeds.size()*.3){
            dx=median(vectors.stream().mapToDouble(v->v.nx-v.x).toArray());dy=median(vectors.stream().mapToDouble(v->v.ny-v.y).toArray());
            final double mx=dx,my=dy;double residual=median(vectors.stream().mapToDouble(v->Math.hypot(v.nx-v.x-mx,v.ny-v.y-my)).toArray());
            double limit=Math.max(2,Math.min(8,2.5*residual));vectors.removeIf(v->Math.hypot(v.nx-v.x-mx,v.ny-v.y-my)>limit);
            if(vectors.size()>=8){
                dx=median(vectors.stream().mapToDouble(v->v.nx-v.x).toArray());dy=median(vectors.stream().mapToDouble(v->v.ny-v.y).toArray());medianFb=median(vectors.stream().mapToDouble(Vector::fb).toArray());
                var ratios=new ArrayList<Double>();for(int i=0;i<vectors.size();i++)for(int j=i+1;j<vectors.size();j++){var a=vectors.get(i);var b=vectors.get(j);double before=Math.hypot(a.x-b.x,a.y-b.y);if(before>12){double ratio=Math.hypot(a.nx-b.nx,a.ny-b.ny)/before;if(ratio>.8&&ratio<1.25)ratios.add(ratio);}}
                if(ratios.size()>=10)scale=Math.max(.92,Math.min(1.09,median(ratios.stream().mapToDouble(Double::doubleValue).toArray())));
                double w=box.w()*scale,h=box.h()*scale,cx=box.x()+box.w()/2+dx/current.width,cy=box.y()+box.h()/2+dy/current.height;
                if(w<.95&&h<.95&&cx-w/2>=0&&cy-h/2>=0&&cx+w/2<=1&&cy+h/2<=1)candidate=new PixelTracker.Box(cx-w/2,cy-h/2,w,h);
                for(int i=0;i<Math.min(12,vectors.size());i++){var v=vectors.get(i);visualVectors.put(new JSONArray(new double[]{rounded(v.x/current.width),rounded(v.y/current.height),rounded(v.nx/current.width),rounded(v.ny/current.height)}));}
            }
        }
        boolean invalidBox=candidate!=null&&!candidate.measurable();if(invalidBox)candidate=null;
        // A second near-exact anchor copy does not acquire object identity from motion alone.
        boolean ambiguous=duplicate(current.p[0],candidate==null?box:candidate);
        String method=lost>0?"motion-recovery":"pyramidal-flow";double score=0;boolean accepted=candidate!=null&&!ambiguous;
        if(!accepted&&!ambiguous){var fallback=new AdaptiveTracker(id,label,previousImage,box).update(image);if(!fallback.isNull("box")&&fallback.optDouble("matchScore")>=.8){candidate=PixelTracker.Box.from(fallback.getJSONArray("box"));accepted=true;method="appearance-recovery";score=fallback.getDouble("matchScore");}}
        String status=accepted?(lost>0?"reacquired":"tracked"):"lost";
        if(accepted){box=candidate;previousImage=image;previous=current;updates++;lost=0;if(score==0)score=vectors.isEmpty()?0:vectors.stream().mapToDouble(Vector::corr).average().orElse(0);}else lost++;
        observations++;
        return new JSONObject().put("id",id).put("label",label).put("status",status).put("box",accepted?box.json():JSONObject.NULL).put("lastKnownBox",box.json()).put("matchScore",score).put("appearanceUpdates",updates).put("scoreMeaning","Local patch correlation and motion consistency, not calibrated semantic confidence")
            .put("motion",new JSONObject().put("method",method).put("objectMaxPyramidLevel",maxLevel).put("seedPoints",seeds.size()).put("reliablePoints",reliable).put("inliers",vectors.size()).put("medianForwardBackwardPixels",medianFb).put("dxPixels",dx).put("dyPixels",dy).put("scale",scale).put("ambiguous",ambiguous).put("invalidBoxRejected",invalidBox).put("vectors",visualVectors));
    }
}

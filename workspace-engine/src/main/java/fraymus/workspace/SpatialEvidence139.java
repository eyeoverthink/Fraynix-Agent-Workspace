package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.util.*;
import org.json.*;

/** Immutable textured parts and relative foreground/background motion; never semantic identity. */
final class SpatialEvidence139 {
    private static final int GRID=7;
    private record Patch(double[] gray,double[] mean,double variance){}
    private record Part(int index,double u,double v,Patch patch,boolean eligible){}
    private record Match(double x,double y,double score,double rival,boolean unique){}
    private record Vote(Part part,Match match,double x,double y){}
    private final PixelTracker.Box seed;
    private final List<Part> parts=new ArrayList<>();
    private BufferedImage previous;
    private PixelTracker.Box witness,lastProposal;
    private final String hash;
    private final int sourceBackgroundParts;
    SpatialEvidence139(BufferedImage first,PixelTracker.Box seed){
        this.seed=seed;previous=working(first);witness=lastProposal=seed;
        var background=ring(previous,seed);sourceBackgroundParts=background.size();var serial=new JSONArray();
        for(int y=0;y<3;y++)for(int x=0;x<3;x++){
            double u=.2+.3*x,v=.2+.3*y;var p=patch(previous,seed.x()+u*seed.w(),seed.y()+v*seed.h(),seed.w()*.22,seed.h()*.22);
            double rival=-1;for(var q:background)rival=Math.max(rival,score(p,q));
            double bgVariance=median(background.stream().map(q->q.variance).toList()),colorGap=0;for(int k=0;k<3;k++){final int channel=k;colorGap+=Math.abs(p.mean[k]-median(background.stream().map(q->q.mean[channel]).toList()))/3;}
            boolean distinct=background.size()>=3&&(colorGap>.04||p.variance>1.8*Math.max(.00002,bgVariance)||bgVariance>1.8*Math.max(.00002,p.variance));
            boolean eligible=p.variance>.00002&&rival<.92&&distinct;var part=new Part(y*3+x,u,v,p,eligible);parts.add(part);
            serial.put(new JSONObject().put("id",part.index).put("gray",new JSONArray(p.gray)).put("mean",new JSONArray(p.mean)).put("eligible",eligible));
        }
        hash=WorldFrames.hash(new JSONObject().put("schema","immutable-spatial-parts-139-v1").put("seed",seed.json()).put("parts",serial));
    }
    String hash(){return hash;}
    BufferedImage currentImage(){return previous;}
    static BufferedImage working(BufferedImage original){return smooth(PixelTracker.working(smooth(original)));}
    private static BufferedImage smooth(BufferedImage input){
        var out=new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++){int r=0,g=0,b=0;for(int j=-1;j<=1;j++)for(int i=-1;i<=1;i++){int c=input.getRGB(Math.max(0,Math.min(input.getWidth()-1,x+i)),Math.max(0,Math.min(input.getHeight()-1,y+j))),weight=(i==0?2:1)*(j==0?2:1);r+=((c>>16)&255)*weight;g+=((c>>8)&255)*weight;b+=(c&255)*weight;}out.setRGB(x,y,((r/16)<<16)|((g/16)<<8)|(b/16));}return out;
    }
    private static Patch patch(BufferedImage image,double cx,double cy,double w,double h){
        var a=new double[GRID*GRID];var rgb=new double[3];double mean=0,variance=0;
        for(int y=0;y<GRID;y++)for(int x=0;x<GRID;x++){
            double px=(cx+((x+.5)/GRID-.5)*w)*(image.getWidth()-1),py=(cy+((y+.5)/GRID-.5)*h)*(image.getHeight()-1);
            px=Math.max(0,Math.min(image.getWidth()-1.001,px));py=Math.max(0,Math.min(image.getHeight()-1.001,py));int ix=(int)px,iy=(int)py;double fx=px-ix,fy=py-iy;double[] c=new double[3];
            for(int j=0;j<2;j++)for(int i=0;i<2;i++){int pixel=image.getRGB(ix+i,iy+j);double weight=(i==0?1-fx:fx)*(j==0?1-fy:fy);for(int k=0;k<3;k++)c[k]+=((pixel>>(16-8*k))&255)/255.*weight;}
            for(int k=0;k<3;k++)rgb[k]+=c[k];double g=.299*c[0]+.587*c[1]+.114*c[2];a[y*GRID+x]=g;mean+=g;
        }
        mean/=a.length;for(int i=0;i<a.length;i++){a[i]-=mean;variance+=a[i]*a[i];}for(int k=0;k<3;k++)rgb[k]/=a.length;
        return new Patch(a,rgb,variance/a.length);
    }
    private static double score(Patch a,Patch b){
        if(a.variance<.00002||b.variance<.00002)return -1;
        double dot=0,color=0;for(int i=0;i<a.gray.length;i++)dot+=a.gray[i]*b.gray[i];for(int k=0;k<3;k++)color+=Math.abs(a.mean[k]-b.mean[k])/3;
        return dot/(a.gray.length*Math.sqrt(a.variance*b.variance))-.5*color;
    }
    private static boolean inside(double x,double y,double w,double h){return x>=w/2&&x<=1-w/2&&y>=h/2&&y<=1-h/2;}
    private static Match match(BufferedImage image,Patch source,double w,double h,double cx,double cy,double rx,double ry){
        double best=-2,bx=cx,by=cy;var candidates=new ArrayList<double[]>();
        for(int j=-6;j<=6;j++)for(int i=-6;i<=6;i++){
            double x=cx+i*rx/6,y=cy+j*ry/6;if(!inside(x,y,w,h))continue;double s=score(source,patch(image,x,y,w,h));candidates.add(new double[]{x,y,s});if(s>best){best=s;bx=x;by=y;}
        }
        double coarseX=bx,coarseY=by;
        for(int j=-2;j<=2;j++)for(int i=-2;i<=2;i++){
            double x=coarseX+i*rx/24,y=coarseY+j*ry/24;if(!inside(x,y,w,h))continue;double s=score(source,patch(image,x,y,w,h));candidates.add(new double[]{x,y,s});if(s>best){best=s;bx=x;by=y;}
        }
        double rival=-2;for(var c:candidates)if(Math.hypot((c[0]-bx)/w,(c[1]-by)/h)>=.9)rival=Math.max(rival,c[2]);
        return new Match(bx,by,best,rival,best>=.78&&rival<best-.04);
    }
    private static double median(List<Double> values){var a=values.stream().mapToDouble(Double::doubleValue).sorted().toArray();return a.length==0?0:a.length%2==0?(a[a.length/2-1]+a[a.length/2])/2:a[a.length/2];}
    private static double[][] ringPoints(PixelTracker.Box b){return new double[][]{{b.x()-.18*b.w(),b.y()},{b.x()+.5*b.w(),b.y()-.18*b.h()},{b.x()+1.18*b.w(),b.y()},{b.x()-.18*b.w(),b.y()+.5*b.h()},{b.x()+1.18*b.w(),b.y()+.5*b.h()},{b.x()-.18*b.w(),b.y()+b.h()},{b.x()+.5*b.w(),b.y()+1.18*b.h()},{b.x()+1.18*b.w(),b.y()+b.h()}};}
    private static List<Patch> ring(BufferedImage image,PixelTracker.Box b){var out=new ArrayList<Patch>();for(var p:ringPoints(b))if(inside(p[0],p[1],b.w()*.22,b.h()*.22))out.add(patch(image,p[0],p[1],b.w()*.22,b.h()*.22));return out;}
    boolean supports(BufferedImage small,PixelTracker.Box box){
        int eligible=0,matched=0;var rows=new HashSet<Integer>();var cols=new HashSet<Integer>();
        for(var part:parts)if(part.eligible){eligible++;double q=score(part.patch,patch(small,box.x()+part.u*box.w(),box.y()+part.v*box.h(),box.w()*.22,box.h()*.22));if(q>=.55){matched++;rows.add(part.index/3);cols.add(part.index%3);}}
        return eligible<4||matched>=3&&rows.size()>=2&&cols.size()>=2;
    }
    JSONObject assess(BufferedImage image,PixelTracker.Box proposal){
        var current=working(image);var box=proposal==null?witness:proposal;var votes=new ArrayList<Vote>();var evidence=new JSONArray();double direct=0;int eligible=0,directMatches=0;
        for(var part:parts){
            double x=box.x()+part.u*box.w(),y=box.y()+part.v*box.h(),w=box.w()*.22,h=box.h()*.22;double s=score(part.patch,patch(current,x,y,w,h));
            if(!part.eligible)continue;eligible++;direct+=Math.max(0,s);if(s>=.55)directMatches++;
            double rx=Math.min(.15,Math.max(.025,box.w()*.4)),ry=Math.min(.15,Math.max(.025,box.h()*.4));
            var a=match(current,part.patch,w,h,x,y,rx,ry);double wx=witness.x()+part.u*witness.w(),wy=witness.y()+part.v*witness.h();
            if(Math.hypot(wx-x,wy-y)>.01){var b=match(current,part.patch,w,h,wx,wy,rx,ry);if(b.score>a.score)a=b;}
            if(a.unique)votes.add(new Vote(part,a,a.x-part.u*box.w(),a.y-part.v*box.h()));
            evidence.put(new JSONObject().put("id",part.index).put("expected",new JSONArray(new double[]{x,y})).put("matched",new JSONArray(new double[]{a.x,a.y})).put("score",a.score).put("rival",a.rival).put("unique",a.unique).put("directScore",s));
        }
        double mx=median(votes.stream().map(Vote::x).toList()),my=median(votes.stream().map(Vote::y).toList());var inliers=new ArrayList<Vote>();
        for(var v:votes)if(Math.hypot((v.x-mx)/box.w(),(v.y-my)/box.h())<=.18)inliers.add(v);
        long rows=inliers.stream().map(v->v.part.index/3).distinct().count(),cols=inliers.stream().map(v->v.part.index%3).distinct().count();
        boolean supported=inliers.size()>=3&&rows>=2&&cols>=2;
        double nx=median(inliers.stream().map(Vote::x).toList()),ny=median(inliers.stream().map(Vote::y).toList());
        var inferred=new PixelTracker.Box(nx,ny,box.w(),box.h());supported&=inferred.measurable();
        var bgx=new ArrayList<Double>();var bgy=new ArrayList<Double>();
        for(var p:ringPoints(witness)){
            double w=witness.w()*.22,h=witness.h()*.22;if(!inside(p[0],p[1],w,h))continue;var original=patch(previous,p[0],p[1],w,h);if(original.variance<.00002)continue;
            var m=match(current,original,w,h,p[0],p[1],.04,.04);if(!m.unique||m.score<.85)continue;
            var back=match(previous,patch(current,m.x,m.y,w,h),w,h,m.x,m.y,.04,.04);if(!back.unique||Math.hypot(back.x-p[0],back.y-p[1])>.012)continue;
            bgx.add(m.x-p[0]);bgy.add(m.y-p[1]);
        }
        double fx=supported?(inferred.x()+inferred.w()/2-witness.x()-witness.w()/2):0,fy=supported?(inferred.y()+inferred.h()/2-witness.y()-witness.h()/2):0;
        double px=proposal==null?0:box.x()+box.w()/2-lastProposal.x()-lastProposal.w()/2,py=proposal==null?0:box.y()+box.h()/2-lastProposal.y()-lastProposal.h()/2;
        double bx=median(bgx),by=median(bgy),alignment=supported?Math.hypot((inferred.x()-box.x())/box.w(),(inferred.y()-box.y())/box.h()):0;
        boolean backgroundFollowing=supported&&bgx.size()>=3&&Math.hypot((fx-bx)/box.w(),(fy-by)/box.h())>.08&&Math.hypot((px-bx)/box.w(),(py-by)/box.h())+.04<Math.hypot((px-fx)/box.w(),(py-fy)/box.h());
        var reasons=new JSONArray();if(proposal!=null&&supported&&alignment>.18)reasons.put("Seed parts agree away from the proposed box");if(proposal!=null&&backgroundFollowing)reasons.put("Proposed motion follows background instead of supported parts");if(proposal!=null&&eligible>=4&&directMatches<3&&direct/eligible<.35&&!supported)reasons.put("Original spatial structure is unsupported");
        var result=new JSONObject().put("schema","foreground-structure-evidence-139-v1").put("anchorSha256",hash).put("sourceBackgroundParts",sourceBackgroundParts).put("eligibleParts",eligible).put("directMatches",directMatches).put("structuralAgreement",eligible==0?0:direct/eligible).put("matchedParts",votes.size()).put("inlierParts",inliers.size()).put("supported",supported).put("witnessBox",supported?inferred.json():JSONObject.NULL).put("alignmentError",alignment).put("foregroundMotion",new JSONArray(new double[]{fx,fy})).put("proposalMotion",new JSONArray(new double[]{px,py})).put("backgroundMotion",new JSONArray(new double[]{bx,by})).put("backgroundPoints",bgx.size()).put("parts",evidence).put("candidateReasons",reasons).put("automaticAdmissions",0);
        if(supported)witness=inferred;if(proposal!=null)lastProposal=proposal;previous=current;return result;
    }
}

package fraymus.workspace;

import org.json.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Known-sprite, known-layout image reconstruction. Pixels never grant execution authority. */
final class GameReference115 {
    private final Path repo,root;
    private final GameLevels114 levels;
    GameReference115(Path repo,Path project){this.repo=repo;root=project.resolve("game-references115");levels=new GameLevels114(repo,project);}
    private Path folder(String id){if(!id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IllegalArgumentException("Choose an imported reference ID");return root.resolve(id);}
    static BufferedImage decode(byte[] bytes)throws Exception{
        if(bytes.length>3_000_000)throw new IllegalArgumentException("Reference PNG exceeds 3 MB");
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw new IllegalArgumentException("Readable PNG required");var reader=readers.next();try{if(!reader.getFormatName().equalsIgnoreCase("png"))throw new IllegalArgumentException("Use PNG to retain sprite pixels");reader.setInput(stream);int w=reader.getWidth(0),h=reader.getHeight(0);if(w<200||h<80||w>1536||h>864)throw new IllegalArgumentException("Reference must be 200–1536 pixels wide and 80–864 high");return reader.read(0);}finally{reader.dispose();}}
    }
    private BufferedImage sprite(JSONObject sprite)throws Exception{
        var texture=sprite.getJSONObject("texture");String asset=texture.getString("filepath").replace('\\','/');Path p=repo.resolve(asset).normalize();if(!asset.startsWith("assets/")||!p.startsWith(repo.resolve("assets").normalize()))throw new IllegalArgumentException("Unsupported source texture");var image=ImageIO.read(p.toFile());double minX=1,maxY=0;for(Object raw:sprite.getJSONArray("texCoords")){var uv=(JSONObject)raw;minX=Math.min(minX,uv.getDouble("x"));maxY=Math.max(maxY,uv.getDouble("y"));}return image.getSubimage((int)Math.round(minX*image.getWidth()),(int)Math.round((1-maxY)*image.getHeight()),(int)sprite.getDouble("width"),(int)sprite.getDouble("height"));
    }
    private List<BufferedImage> templates(JSONObject object)throws Exception{var out=new ArrayList<BufferedImage>();collect(object,out);return out;}
    private void collect(Object value,List<BufferedImage> out)throws Exception{if(value instanceof JSONObject o){if(o.has("texCoords")&&o.has("texture")){out.add(sprite(o));return;}for(String k:o.keySet())collect(o.get(k),out);}else if(value instanceof JSONArray a)for(Object v:a)collect(v,out);}
    private static int diff(int a,int b){return Math.abs((a>>16&255)-(b>>16&255))+Math.abs((a>>8&255)-(b>>8&255))+Math.abs((a&255)-(b&255));}
    private static int pixel(BufferedImage t,int x,int y,int n,int bg){return pixel(t,x,y,n,n,bg,.4999,.5001);}
    private static int pixel(BufferedImage t,int x,int y,int n,int base,int bg,double phase,double phaseY){int u=x*base/n,v=y*base/n;int c=t.getRGB(Math.min(t.getWidth()-1,(int)((u+phase)*t.getWidth()/base)),Math.min(t.getHeight()-1,(int)((v+phaseY)*t.getHeight()/base)));// Match the preserved renderer: GL_ONE, GL_ONE_MINUS_SRC_ALPHA.
        double keep=1-(c>>>24)/255.0;int r=Math.min(255,(int)Math.round((c>>16&255)+(bg>>16&255)*keep)),g=Math.min(255,(int)Math.round((c>>8&255)+(bg>>8&255)*keep)),b=Math.min(255,(int)Math.round((c&255)+(bg&255)*keep));return (r<<16)|(g<<8)|b;}
    private static double score(BufferedImage image,BufferedImage template,int x,int y,int n,int bg,boolean quick){
        if(x<0||y<0||x+n>image.getWidth()||y+n>image.getHeight())return 255;
        double best=255;for(int base:new int[]{n,n%3==0&&n*2/3>=8?n*2/3:n,n%2==0&&n/2>=8?n/2:n})for(double phase:new double[]{.4999,.5001})for(double phaseY:new double[]{.4999,.5001})best=Math.min(best,scoreRaster(image,template,x,y,n,base,bg,quick,phase,phaseY));return best;
    }
    private static double scoreRaster(BufferedImage image,BufferedImage template,int x,int y,int n,int base,int bg,boolean quick,double phase,double phaseY){
        long error=0;int count=0;
        if(quick){for(double[] point:new double[][]{{.1,.1},{.75,.15},{.25,.4},{.8,.65},{.1,.9},{.6,.8}}){int u=(int)(point[0]*n),v=(int)(point[1]*n);error+=diff(image.getRGB(x+u,y+v),pixel(template,u,v,n,base,bg,phase,phaseY));count++;}}
        else for(int v=0;v<n;v++)for(int u=0;u<n;u++){error+=diff(image.getRGB(x+u,y+v),pixel(template,u,v,n,base,bg,phase,phaseY));count++;}
        return error/(3.0*count);
    }
    private static JSONObject match(BufferedImage image,List<BufferedImage> templates,String role,int x,int y,int n,int bg,boolean[][] mask){
        double best=255,contrast=0;for(var template:templates){double error=score(image,template,x,y,n,bg,false);if(error<best){best=error;contrast=0;for(int v=0;v<n;v++)for(int u=0;u<n;u++)contrast+=diff(bg,pixel(template,u,v,n,bg));contrast/=3.0*n*n;}}if(best>12||best>=contrast*.6)throw new IllegalArgumentException("Reference does not clearly show the supported "+role+" at its expected position (pixel error "+String.format(Locale.ROOT,"%.2f",best)+")");
        for(int v=y;v<y+n;v++)for(int u=x;u<x+n;u++)mask[v][u]=true;
        return new JSONObject().put("role",role).put("x",x).put("y",y).put("sizePx",n).put("meanChannelError",best).put("backgroundContrast",contrast).put("similarity",1-best/255.0);
    }
    JSONObject analyze(BufferedImage image)throws Exception{
        long started=System.nanoTime();var source=levels.source();var ground=sprite(GameLevels114.prop(GameLevels114.first(source,"components.Ground"),"components.SpriteRenderer").getJSONObject("sprite"));
        var colors=new HashMap<Integer,Integer>();for(int y=0;y<image.getHeight();y+=8)for(int x=0;x<image.getWidth();x+=8)colors.merge(image.getRGB(x,y),1,Integer::sum);int bg=colors.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        int bestN=0;List<int[]> best=List.of();double bestError=Double.POSITIVE_INFINITY;
        for(int n=8;n<=32;n+=2){var groups=new HashMap<Integer,List<int[]>>();for(int y=0;y<=image.getHeight()-n;y++)for(int x=0;x<=image.getWidth()-n;x++){
            if(diff(image.getRGB(x,y),pixel(ground,0,0,n,bg))>24||score(image,ground,x,y,n,bg,true)>6)continue;
            if(score(image,ground,x,y,n,bg,false)>8)continue;int phase=(y%n)*n+x%n;groups.computeIfAbsent(phase,k->new ArrayList<>()).add(new int[]{x,y});
        }
            for(var group:groups.values())if(group.size()>=20){double error=0;for(var point:group)error+=score(image,ground,point[0],point[1],n,bg,false);error/=group.size();if(group.size()>best.size()||group.size()==best.size()&&error<bestError){best=group;bestN=n;bestError=error;}}
        }
        if(bestN==0)throw new IllegalArgumentException("No unambiguous original terrain grid. Use a full course overview with 8–32 even pixels per tile");int n=bestN,originX=best.stream().mapToInt(p->p[0]).min().orElseThrow(),bottom=best.stream().mapToInt(p->p[1]).max().orElseThrow(),last=best.stream().mapToInt(p->p[0]).max().orElseThrow(),length=(last-originX)/n+1;
        if(length<48||length>72)throw new IllegalArgumentException("Full supported course needs 48–72 visible columns; cropped/unknown extent is not inferred");
        boolean[] floor=new boolean[length];var known=new HashSet<String>();for(var p:best)known.add(p[0]+":"+p[1]);for(int col=0;col<length;col++){boolean lo=known.contains((originX+col*n)+":"+bottom),hi=known.contains((originX+col*n)+":"+(bottom-n));if(lo!=hi)throw new IllegalArgumentException("Terrain rows are incomplete or occluded");floor[col]=lo;}
        var gaps=new ArrayList<int[]>();for(int col=0;col<length;col++)if(!floor[col]){int start=col;while(col<length&&!floor[col])col++;gaps.add(new int[]{start,col-start});}
        if(gaps.size()!=2||gaps.get(0)[0]<16||gaps.get(0)[0]>18||gaps.get(1)[0]<32||gaps.get(1)[0]>34||gaps.stream().anyMatch(g->g[1]<1||g[1]>10))throw new IllegalArgumentException("Image terrain is outside the two-gap source recipe; no guessed geometry created");
        int baseSeed=(gaps.get(0)[0]-16)+3*(gaps.get(1)[0]-32),worldBottom=bottom+n;
        var question=templates(GameLevels114.first(source,"components.QuestionBlock"));int seed=-1;double qScore=255;for(int s:List.of(baseSeed,baseSeed+9)){int qx=originX+(11+(s%2)*2)*n;double error=255;for(var q:question)error=Math.min(error,score(image,q,qx,worldBottom-6*n,n,bg,false));if(error<qScore){qScore=error;seed=s;}}
        var mask=new boolean[image.getHeight()][image.getWidth()];var detections=new JSONArray();for(int col=0;col<length;col++)if(floor[col])for(int row=0;row<2;row++)detections.put(match(image,List.of(ground),"terrain",originX+col*n,bottom-row*n,n,bg,mask));
        detections.put(match(image,templates(GameLevels114.first(source,"components.PlayerController")),"player",originX+3*n,worldBottom-3*n,n,bg,mask));detections.put(match(image,question,"question block",originX+(11+(seed%2)*2)*n,worldBottom-6*n,n,bg,mask));
        var flags=templates(GameLevels114.first(source,"components.Flagpole"));for(int i=0;i<8;i++)detections.put(match(image,flags,"finish",originX+(length-5)*n-n/2,worldBottom-(3+i)*n,n,bg,mask));
        int unexplained=0;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)if(!mask[y][x]&&diff(image.getRGB(x,y),bg)>45)unexplained++;
        if(unexplained>Math.max(8,n*n/8))throw new IllegalArgumentException("Reference contains "+unexplained+" unexplained pixels outside supported components; crop browser chrome or choose a supported clear overview");
        var params=GameLevels114.parameters115(new JSONObject().put("seed",seed).put("lengthTiles",length).put("gapTiles",gaps.get(0)[1]).put("secondGapTiles",gaps.get(1)[1]));
        return new JSONObject().put("parameters",params).put("grid",new JSONObject().put("tilePx",n).put("originX",originX).put("worldBottomY",worldBottom)).put("detections",detections).put("unexplainedPixels",unexplained).put("meanTerrainError",bestError).put("analysisNanos",System.nanoTime()-started).put("scope","Known original sprites and two-gap recipe only. Seed is a canonical matching layout, not the author's original seed. Hidden question-block contents use declared original Powerup behavior; offscreen geometry is not inferred.");
    }
    synchronized JSONObject importImage(JSONObject input)throws Exception{
        SpatialScene.keys(input,"pngBase64");String encoded=input.getString("pngBase64");if(encoded.length()>4_000_000)throw new IllegalArgumentException("Reference exceeds 3 MB");byte[] bytes=Base64.getDecoder().decode(encoded);var image=decode(bytes);Files.createDirectories(root);try(var dirs=Files.list(root)){if(dirs.filter(Files::isDirectory).count()>=16)throw new IllegalStateException("Reference collection is full (16); originals retained");}
        String id=UUID.randomUUID().toString();Path dir=folder(id);Files.createDirectory(dir);Files.write(dir.resolve("reference.png"),bytes,StandardOpenOption.CREATE_NEW);var r=new JSONObject().put("id",id).put("pngSha256",GameLevels114.hash(dir.resolve("reference.png"))).put("source",levels.analyze()).put("width",image.getWidth()).put("height",image.getHeight());
        try{r.put("analysis",analyze(image)).put("accepted",true);}catch(IllegalArgumentException e){r.put("accepted",false).put("reason",e.getMessage());}AgentFiles042.saveChecked(dir.resolve("reference.json"),r);return r;
    }
    synchronized JSONObject verify(String id)throws Exception{Path dir=folder(id);var r=AgentFiles042.loadChecked(dir.resolve("reference.json"));if(!id.equals(r.getString("id"))||!r.getString("pngSha256").equals(GameLevels114.hash(dir.resolve("reference.png"))))throw new IllegalStateException("Original reference image changed");if(!r.getBoolean("accepted"))throw new IllegalArgumentException(r.optString("reason"));if(!r.getJSONObject("source").similar(levels.analyze()))throw new IllegalStateException("Reference source components changed");var fresh=analyze(decode(Files.readAllBytes(dir.resolve("reference.png"))));var saved=WorldFrames.copy(r.getJSONObject("analysis"));fresh.remove("analysisNanos");saved.remove("analysisNanos");if(!fresh.similar(saved))throw new IllegalStateException("Reference reconstruction changed");return r;}
    synchronized void checkBytes(JSONObject binding)throws Exception{Path dir=folder(binding.getString("id"));var r=AgentFiles042.loadChecked(dir.resolve("reference.json"));if(!binding.similar(r)||!r.getString("pngSha256").equals(GameLevels114.hash(dir.resolve("reference.png"))))throw new IllegalStateException("Bound reference changed");}
}

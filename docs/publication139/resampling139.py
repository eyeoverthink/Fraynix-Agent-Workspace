from pathlib import Path
p=Path('workspace-engine/src/main/java/fraymus/workspace/SpatialEvidence139.java');s=p.read_text(encoding='utf8');start=s.index('    static BufferedImage working(');end=s.index('    private static Patch patch',start)
s=s[:start]+'''    BufferedImage currentImage(){return previous;}
    static BufferedImage working(BufferedImage original){return smooth(PixelTracker.working(smooth(original)));}
    private static BufferedImage smooth(BufferedImage input){
        var out=new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++){int r=0,g=0,b=0;for(int j=-1;j<=1;j++)for(int i=-1;i<=1;i++){int c=input.getRGB(Math.max(0,Math.min(input.getWidth()-1,x+i)),Math.max(0,Math.min(input.getHeight()-1,y+j))),weight=(i==0?2:1)*(j==0?2:1);r+=((c>>16)&255)*weight;g+=((c>>8)&255)*weight;b+=(c&255)*weight;}out.setRGB(x,y,((r/16)<<16)|((g/16)<<8)|(b/16));}return out;
    }
'''+s[end:];p.write_text(s,encoding='utf8')
p=Path('workspace-engine/src/main/java/fraymus/workspace/ForegroundGuard139.java');s=p.read_text(encoding='utf8').replace('var spatialImage=SpatialEvidence139.working(image);','var spatialImage=spatial.currentImage();');p.write_text(s,encoding='utf8')

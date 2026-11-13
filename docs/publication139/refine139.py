from pathlib import Path
p=Path('workspace-engine/src/main/java/fraymus/workspace/SpatialEvidence139.java');s=p.read_text(encoding='utf8').replace('previous=PixelTracker.working(first)','previous=working(first)').replace('var current=PixelTracker.working(image)','var current=working(image)').replace('.0002','.00002');s=s.replace('    String hash(){return hash;}','''    String hash(){return hash;}
    static BufferedImage working(BufferedImage original){
        var input=PixelTracker.working(original);var out=new BufferedImage(input.getWidth(),input.getHeight(),BufferedImage.TYPE_INT_RGB);
        for(int y=0;y<input.getHeight();y++)for(int x=0;x<input.getWidth();x++){int r=0,g=0,b=0;for(int j=-1;j<=1;j++)for(int i=-1;i<=1;i++){int c=input.getRGB(Math.max(0,Math.min(input.getWidth()-1,x+i)),Math.max(0,Math.min(input.getHeight()-1,y+j))),weight=(i==0?2:1)*(j==0?2:1);r+=((c>>16)&255)*weight;g+=((c>>8)&255)*weight;b+=(c&255)*weight;}out.setRGB(x,y,((r/16)<<16)|((g/16)<<8)|(b/16));}return out;
    }''');p.write_text(s,encoding='utf8')
p=Path('workspace-engine/src/main/java/fraymus/workspace/ForegroundGuard139.java');s=p.read_text(encoding='utf8').replace('var found=anchor.search(small);var recoveredEvidence=anchor.assess(small,found.box());','''var found=anchor.search(small);var spatialImage=SpatialEvidence139.working(image);
            if(flowAccepted&&structure.getBoolean("supported")&&structure.getDouble("alignmentError")<.12&&structure.getInt("eligibleParts")>=3){
                var proposedBox=PixelTracker.Box.from(proposal.getJSONArray("box"));double score=evidence.getDouble("correlation"),rival=anchor.rival(small,proposedBox);
                if(score>=.88&&rival<score-.05&&AppearanceEvidence138.reasons(evidence).isEmpty()&&spatial.supports(spatialImage,proposedBox))found=new AppearanceEvidence138.Search(proposedBox,score,rival,true);
            }
            var recoveredEvidence=anchor.assess(small,found.box());''').replace('spatial.supports(small,found.box())','spatial.supports(spatialImage,found.box())');p.write_text(s,encoding='utf8')

package fraymus.workspace;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.*;

/** Object-agnostic proposals on a flat background; otherwise preserves the whole image. */
final class PixelRegions {
    static java.util.List<Rectangle> propose(BufferedImage image){
        int w=image.getWidth(),h=image.getHeight();var bins=new HashMap<Integer,Integer>();var colors=new HashMap<Integer,long[]>();int count=0;
        for(int x=2;x<w-2;x+=3)for(int y:new int[]{2,h-3}){int c=image.getRGB(x,y),key=((c>>19)&31)*1024+((c>>11)&31)*32+((c>>3)&31);bins.merge(key,1,Integer::sum);var sum=colors.computeIfAbsent(key,k->new long[3]);sum[0]+=(c>>16)&255;sum[1]+=(c>>8)&255;sum[2]+=c&255;count++;}
        for(int y=2;y<h-2;y+=3)for(int x:new int[]{2,w-3}){int c=image.getRGB(x,y),key=((c>>19)&31)*1024+((c>>11)&31)*32+((c>>3)&31);bins.merge(key,1,Integer::sum);var sum=colors.computeIfAbsent(key,k->new long[3]);sum[0]+=(c>>16)&255;sum[1]+=(c>>8)&255;sum[2]+=c&255;count++;}
        var winner=bins.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();if(winner.getValue()/(double)count<.8)return java.util.List.of(new Rectangle(0,0,w,h));var sum=colors.get(winner.getKey());double r=sum[0]/(double)winner.getValue(),g=sum[1]/(double)winner.getValue(),b=sum[2]/(double)winner.getValue();boolean[] foreground=new boolean[w*h],seen=new boolean[w*h];
        for(int y=2;y<h-2;y++)for(int x=2;x<w-2;x++){int c=image.getRGB(x,y);double dr=((c>>16)&255)-r,dg=((c>>8)&255)-g,db=(c&255)-b;foreground[y*w+x]=dr*dr+dg*dg+db*db>45*45;}
        var regions=new ArrayList<Rectangle>();int[] queue=new int[w*h];for(int start=0;start<foreground.length;start++){if(!foreground[start]||seen[start])continue;int head=0,tail=1;queue[0]=start;seen[start]=true;int minX=w,minY=h,maxX=0,maxY=0;while(head<tail){int at=queue[head++],x=at%w,y=at/w;minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);for(int next:new int[]{at-1,at+1,at-w,at+w})if(next>=0&&next<foreground.length&&!seen[next]&&foreground[next]){seen[next]=true;queue[tail++]=next;}}if(tail<w*h*.015||maxX-minX<20||maxY-minY<20)continue;var region=new Rectangle(Math.max(0,minX-2),Math.max(0,minY-2),Math.min(w-1,maxX+2)-Math.max(0,minX-2)+1,Math.min(h-1,maxY+2)-Math.max(0,minY-2)+1);if(region.width*(double)region.height>w*h*.9)return java.util.List.of(new Rectangle(0,0,w,h));regions.add(region);}
        regions.sort(Comparator.comparingInt((Rectangle rect)->rect.width*rect.height).reversed());return regions.subList(0,Math.min(3,regions.size()));
    }
}

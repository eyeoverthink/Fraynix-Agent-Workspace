package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static fraymus.workspace.MotionMediaContainers.*;

final class EditableMedia {
    static final int MAX_FRAMES = FPS*6, MAX_SOURCE_FRAMES = FPS*10+1;
    private static final int SAMPLES_PER_FRAME = HZ/FPS;
    private static final int BACKGROUND = 0x13232e;

    enum Lane {
        PICTURE, OVERLAY, AUDIO1, AUDIO2;
        boolean audio() { return this==AUDIO1 || this==AUDIO2; }
    }

    record Asset(JSONObject source, BufferedImage still, ImportedClip video, byte[] pcm) {
        String id() { return source.getString("id"); }
        boolean isImage() { return still!=null; }
        int frames(Lane lane) {
            if (lane.audio()) return (pcm.length+PCM_PER_FRAME-1)/PCM_PER_FRAME;
            if (isImage()) return MAX_FRAMES;
            return video==null ? 0 : video.frames().size();
        }
    }

    record Strip(Lane lane, Asset asset, int start, int in, int length, int x, int y, int size, int opacity, double gain) {
        int end() { return start+length; }
        boolean active(int frame) { return start<=frame && frame<end(); }
        int sourceFrame(int frame) { return in+frame-start; }
        JSONObject json() {
            var row=new JSONObject().put("lane",lane.name()).put("assetId",asset.id())
                    .put("start",start).put("in",in).put("length",length);
            if (lane==Lane.OVERLAY) row.put("x",x).put("y",y).put("size",size).put("opacity",opacity);
            if (lane.audio()) row.put("gain",gain);
            return row;
        }
    }

    record Product(Map<String,byte[]> files, JSONObject metrics) {}
    private final EnumMap<Lane,List<Strip>> tracks=new EnumMap<>(Lane.class);
    private final int length;

    EditableMedia(List<Strip> strips) {
        for (Lane lane:Lane.values()) tracks.put(lane,new ArrayList<>());
        for (Strip strip:strips) {
            if (strip.asset.isImage() && strip.in!=0) throw new IllegalArgumentException("Still images start from source frame zero.");
            if (strip.lane==Lane.OVERLAY && !strip.asset.isImage())
                throw new IllegalArgumentException("The overlay lane takes PNG still images.");
            if (strip.in+strip.length>strip.asset.frames(strip.lane))
                throw new IllegalArgumentException("A strip extends past its source or uses the wrong kind of media.");
            var lane=tracks.get(strip.lane);
            for (Strip existing:lane) if (strip.start<existing.end() && existing.start<strip.end())
                throw new IllegalArgumentException("Strips on one lane cannot overlap. Use a separate audio lane to mix tracks.");
            lane.add(strip);
        }
        if (tracks.get(Lane.PICTURE).isEmpty() && tracks.get(Lane.OVERLAY).isEmpty())
            throw new IllegalArgumentException("Add a picture or overlay before rendering.");
        length=strips.stream().mapToInt(Strip::end).max().orElseThrow();
        if (length>MAX_FRAMES) throw new IllegalArgumentException("The edited timeline supports up to six seconds.");
    }

    Product render() throws Exception {
        List<byte[]> frames=new ArrayList<>(length);
        byte[] poster=null;
        for (int index=0;index<length;index++) {
            var image=picture(index);
            frames.add(MotionTimeline.jpeg(image,.92f));
            if (index==length/2) {
                var bytes=new ByteArrayOutputStream();
                ImageIO.write(image,"png",bytes);
                poster=bytes.toByteArray();
            }
        }
        var audio=new double[length*SAMPLES_PER_FRAME];
        for (Lane lane:List.of(Lane.AUDIO1,Lane.AUDIO2)) for (Strip strip:tracks.get(lane)) {
            var buffer=ByteBuffer.wrap(strip.asset.pcm).order(ByteOrder.LITTLE_ENDIAN);
            int from=strip.in*SAMPLES_PER_FRAME, count=strip.length*SAMPLES_PER_FRAME;
            int offset=strip.start*SAMPLES_PER_FRAME;
            for (int i=0;i<count && from+i<buffer.limit()/2;i++)
                audio[offset+i]+=buffer.getShort((from+i)*2)*strip.gain;
        }
        double peak=0;
        int over=0;
        for (double value:audio) {
            peak=Math.max(peak,Math.abs(value));
            if (Math.abs(value)>32767) over++;
        }
        double gain=peak>32767 ? 32000/peak : 1;
        var mix=ByteBuffer.allocate(audio.length*2).order(ByteOrder.LITTLE_ENDIAN);
        int peakAfter=0;
        for (double value:audio) {
            int sample=(int)Math.round(value*gain);
            if (sample>32767 || sample< -32768) throw new IOException("The verified mix clips.");
            mix.putShort((short)sample);
            peakAfter=Math.max(peakAfter,Math.abs(sample));
        }
        byte[] avi=avi(frames,mix.array()), wav=wav(mix.array());
        checkAvi(avi,frames,mix.array());
        checkWav(wav,mix.array());
        var still=ImageIO.read(new ByteArrayInputStream(poster));
        if (still==null || still.getWidth()!=WIDTH || still.getHeight()!=HEIGHT)
            throw new IOException("The timeline poster did not decode.");
        var assets=new LinkedHashMap<String,byte[]>();
        assets.put("avi",avi);
        assets.put("wav",wav);
        assets.put("png",poster);
        return new Product(assets,new JSONObject().put("frames",length).put("fps",FPS).put("durationS",length/(double)FPS)
                .put("audioHz",HZ).put("audioSamples",audio.length).put("unscaledPeak",peak).put("unscaledClippedSamples",over)
                .put("mixGain",gain).put("exportPeak",peakAfter).put("posterFrame",length/2));
    }

    private BufferedImage picture(int frame) throws IOException {
        var image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        var graphics=image.createGraphics();
        try {
            graphics.setColor(new Color(BACKGROUND));
            graphics.fillRect(0,0,WIDTH,HEIGHT);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            for (Strip strip:tracks.get(Lane.PICTURE)) if (strip.active(frame)) {
                var source=strip.asset.isImage() ? strip.asset.still : decodeFrame(
                        strip.asset.video.frames().get(strip.sourceFrame(frame)),strip.asset.video.width(),strip.asset.video.height());
                double ratio=Math.min(WIDTH/(double)source.getWidth(),HEIGHT/(double)source.getHeight());
                int width=(int)Math.round(source.getWidth()*ratio), height=(int)Math.round(source.getHeight()*ratio);
                graphics.drawImage(source,(WIDTH-width)/2,(HEIGHT-height)/2,width,height,null);
            }
            for (Strip strip:tracks.get(Lane.OVERLAY)) if (strip.active(frame)) {
                var source=strip.asset.still;
                graphics.setComposite(AlphaComposite.SrcOver.derive(strip.opacity/100f));
                graphics.drawImage(source,strip.x,strip.y,Math.max(1,source.getWidth()*strip.size/100),
                        Math.max(1,source.getHeight()*strip.size/100),null);
            }
        } finally {
            graphics.dispose();
        }
        return image;
    }
}

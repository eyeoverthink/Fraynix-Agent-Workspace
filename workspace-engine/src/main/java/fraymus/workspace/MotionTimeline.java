package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.IIOImage;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import static fraymus.workspace.MotionMediaContainers.*;

/** A clocked media timeline from retained native measurements. */
final class MotionTimeline {
    private static final double MIN_PSNR = 36;
    private static final double TARGET_PEAK = .72;
    private static final int TICKS_PER_FRAME = 10; // 240 Hz source, 24 Hz playback
    private static final int MAX_TICKS = 2400;
    private static final int SAMPLES_PER_FRAME = HZ/FPS;

    record Frame(int tick, double x, double y, double radians, double speed, int contacts) {
        JSONObject json() {
            return new JSONObject().put("sourceTick",tick).put("xM",x).put("yM",y)
                    .put("sideRotationRad",radians).put("speedMps",speed).put("contacts",contacts);
        }
    }
    record Sound(byte[] pcm, JSONObject evidence) {}
    record Video(List<byte[]> frames, JSONObject evidence) {}
    private final List<Frame> frames;
    private final RampDefinition ramp;
    private final int endTick;
    private final double scale, centerX, centerY;

    static boolean eligible(JSONObject run) {
        return run.optString("status").equals("completed") && run.optString("backend").equals(BulletRamp.BACKEND)
                && run.has("final") && run.getJSONObject("final").optInt("tick")>=24
                && run.getJSONObject("final").optInt("tick")<=MAX_TICKS;
    }

    MotionTimeline(JSONObject run) {
        if (!eligible(run) || Math.abs(run.getDouble("dt")-BulletRamp.DT)>1e-12)
            throw new IllegalArgumentException("Choose a completed native 240 Hz run of 0.1 to 10 seconds.");
        ramp = RampDefinition.from(run.getJSONObject("definition").getJSONObject("parameters"));
        var measurements = new ArrayList<Frame>();
        for (Object value:run.getJSONArray("samples")) {
            Frame next = from((JSONObject)value);
            if ((!measurements.isEmpty() && next.tick<=measurements.getLast().tick) || next.tick>MAX_TICKS)
                throw new IllegalArgumentException("Native samples must advance on the original clock.");
            if (!measurements.isEmpty() && next.tick-measurements.getLast().tick>12)
                throw new IllegalArgumentException("The native recording has a gap too large to render.");
            measurements.add(next);
        }
        var last = from(run.getJSONObject("final"));
        endTick=last.tick;
        if (measurements.isEmpty() || measurements.getFirst().tick!=0 || measurements.getLast().tick>endTick)
            throw new IllegalArgumentException("The recorded run needs a start and measured end.");
        if (measurements.getLast().tick==endTick) {
            if (!last.equals(measurements.getLast())) throw new IllegalArgumentException("The final native sample differs from the recorded trace.");
        } else {
            measurements.add(last);
        }
        frames=new ArrayList<>();
        int count=(endTick+TICKS_PER_FRAME-1)/TICKS_PER_FRAME+1;
        int segment=0;
        for (int i=0;i<count;i++) {
            int tick=Math.min(endTick,i*TICKS_PER_FRAME);
            while (segment+1<measurements.size()-1 && measurements.get(segment+1).tick<tick) segment++;
            Frame a=measurements.get(segment), b=measurements.get(segment+1);
            if (b.tick-a.tick>12) throw new IllegalArgumentException("The native recording has a gap too large to render.");
            frames.add(b.tick==tick ? b : interpolate(a,b,tick));
        }
        var x=frames.stream().mapToDouble(Frame::x).summaryStatistics();
        var y=frames.stream().mapToDouble(Frame::y).summaryStatistics();
        centerX=(x.getMin()+x.getMax())*.5;
        centerY=(y.getMin()+y.getMax())*.5;
        scale=Math.min((WIDTH-132)/(x.getMax()-x.getMin()+1.4),(HEIGHT-150)/(y.getMax()-y.getMin()+1.4));
    }

    private static Frame from(JSONObject entry) {
        int tick=entry.getInt("tick"), contacts=entry.getInt("contacts");
        double time=entry.getDouble("timeS");
        var position=SpatialScene.vector(entry.getJSONArray("position"),200);
        var rotation=entry.getJSONArray("rotation");
        if (rotation.length()!=4 || !Double.isFinite(time) || Math.abs(time-tick*BulletRamp.DT)>1e-9 || contacts<0 || contacts>128)
            throw new IllegalArgumentException("Invalid native media sample.");
        double x=rotation.getDouble(0), y=rotation.getDouble(1), z=rotation.getDouble(2), w=rotation.getDouble(3);
        double norm=x*x+y*y+z*z+w*w;
        if (!Double.isFinite(norm) || Math.abs(norm-1)>.01) throw new IllegalArgumentException("Invalid native rotation.");
        double side=StrictMath.atan2(2*(w*z+x*y),norm-2*(y*y+z*z));
        double speed=entry.getDouble("speedMps");
        RampDefinition.range(speed,0,100,"native speed");
        return new Frame(tick,position[0],position[1],side,speed,contacts);
    }

    private static Frame interpolate(Frame a, Frame b, int tick) {
        double weight=(tick-a.tick)/(double)(b.tick-a.tick);
        double angle=a.radians+weight*StrictMath.IEEEremainder(b.radians-a.radians,2*Math.PI);
        return new Frame(tick,a.x+weight*(b.x-a.x),a.y+weight*(b.y-a.y),angle,
                a.speed+weight*(b.speed-a.speed),a.contacts);
    }

    int frameCount() { return frames.size(); }
    double duration() { return frameCount()/(double)FPS; }

    JSONObject sourceTimeline() {
        return new JSONObject().put("fps",FPS).put("sampleRateHz",HZ).put("durationS",duration())
                .put("sourceLastTick",endTick).put("sourceDurationS",endTick*BulletRamp.DT).put("units","m,s")
                .put("frames",new JSONArray(frames.stream().map(Frame::json).toList()));
    }

    Sound sound() {
        var source = new double[frameCount()*SAMPLES_PER_FRAME];
        double phase=0, peak=0;
        int clipped=0;
        for (int i=0;i<source.length;i++) {
            int j=i/SAMPLES_PER_FRAME;
            Frame first=frames.get(j), second=frames.get(Math.min(j+1,frameCount()-1));
            double t=(i%SAMPLES_PER_FRAME)/(double)SAMPLES_PER_FRAME;
            double speed=first.speed+t*(second.speed-first.speed);
            double presence=speed/(.45+speed);
            phase+=2*Math.PI*(140+460*presence)/HZ;
            double fade=Math.min(1,Math.min(i,source.length-1-i)/(HZ*.018));
            source[i]=fade*((.2+presence*1.25)*StrictMath.sin(phase)+.16*presence*StrictMath.sin(phase*2));
            double magnitude=Math.abs(source[i]);
            if (magnitude>1) clipped++;
            peak=Math.max(peak,magnitude);
        }
        double gain=TARGET_PEAK/peak;
        var pcm=ByteBuffer.allocate(source.length*2).order(ByteOrder.LITTLE_ENDIAN);
        int max=0;
        for (double raw:source) {
            int sample=(int)Math.round(raw*gain*32767);
            if (Math.abs(sample)>32767) throw new IllegalStateException("The revised sound would clip.");
            max=Math.max(max,Math.abs(sample));
            pcm.putShort((short)sample);
        }
        var evidence=new JSONObject().put("rawPeak",peak).put("rawClippedSamples",clipped).put("gain",gain)
                .put("exportPeak",max/32767.0).put("exportClippedSamples",0).put("samples",source.length)
                .put("note","Synthesized tone follows measured speed. No microphone audio.");
        return new Sound(pcm.array(),evidence);
    }

    Video video(JSONObject prior) throws IOException {
        List<Float> settings=List.of(.50f,.76f,.94f);
        var choices=new LinkedHashSet<Float>();
        if (prior!=null) {
            float proposed=prior.getNumber("quality").floatValue();
            if (!settings.contains(proposed)) throw new IllegalStateException("A remembered encoder setting is unsupported.");
            choices.add(proposed);
        }
        choices.addAll(settings);
        SortedSet<Integer> probes=new TreeSet<>(List.of(0,frameCount()/2,frameCount()-1));
        var images=probes.stream().map(this::render).toList();
        var tried=new JSONArray();
        float selected=0;
        for (float quality:choices) {
            double worst=Double.POSITIVE_INFINITY;
            int used=0;
            for (BufferedImage image:images) {
                byte[] candidate=jpeg(image,quality);
                used+=candidate.length;
                worst=Math.min(worst,psnr(image,ImageIO.read(new ByteArrayInputStream(candidate))));
            }
            tried.put(new JSONObject().put("quality",quality).put("probeBytes",used).put("worstPsnrDb",worst)
                    .put("passed",worst>=MIN_PSNR));
            if (worst>=MIN_PSNR) {
                selected=quality;
                break;
            }
        }
        if (selected==0) throw new IOException("No encoder setting met the decoded-frame quality floor.");
        var encoded=new ArrayList<byte[]>(frameCount());
        for (int i=0;i<frameCount();i++) encoded.add(jpeg(render(i),selected));
        var evidence=new JSONObject().put("quality",selected).put("minimumPsnrDb",MIN_PSNR)
                .put("probeFrames",new JSONArray(probes)).put("tested",tried);
        if (prior!=null) evidence.put("rememberedStart",prior);
        return new Video(encoded,evidence);
    }

    byte[] poster() throws IOException {
        int index=0;
        for (int i=1;i<frames.size();i++) if (frames.get(i).speed>frames.get(index).speed) index=i;
        var out=new ByteArrayOutputStream();
        ImageIO.write(render(index),"png",out);
        return out.toByteArray();
    }

    private double px(double x) { return WIDTH*.5+(x-centerX)*scale; }
    private double py(double y) { return 192-(y-centerY)*scale; }

    private BufferedImage render(int index) {
        var image=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        var frame=frames.get(index);
        var g=image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0x111c26));
            g.fillRect(0,0,WIDTH,HEIGHT);
            g.setColor(new Color(0x223742));
            for (int x=0;x<WIDTH;x+=32) g.drawLine(x,52,x,302);
            for (int y=78;y<302;y+=32) g.drawLine(0,y,WIDTH,y);
            g.setClip(0,52,WIDTH,250);
            double dx=Math.cos(ramp.radians())*ramp.lengthM()/2;
            double dy=Math.sin(ramp.radians())*ramp.lengthM()/2;
            g.setColor(new Color(0x577689));
            g.setStroke(new BasicStroke(4));
            g.drawLine((int)px(-dx),(int)py(ramp.centerY()+dy),(int)px(dx),(int)py(ramp.centerY()-dy));
            g.setColor(new Color(0x75bea7));
            g.setStroke(new BasicStroke(2));
            var track=new Path2D.Double();
            track.moveTo(px(frames.getFirst().x),py(frames.getFirst().y));
            for (int i=1;i<=index;i++) track.lineTo(px(frames.get(i).x),py(frames.get(i).y));
            g.draw(track);
            var saved=g.getTransform();
            g.translate(px(frame.x),py(frame.y));
            g.rotate(-frame.radians);
            int side=Math.max(10,(int)Math.round(.5*scale));
            g.setColor(new Color(0xf1bb79));
            g.fillRoundRect(-side/2,-side/2,side,side,4,4);
            g.setColor(new Color(0x654829));
            g.drawLine(0,-side/2,0,side/2);
            g.setTransform(saved);
            g.setClip(null);
            g.setColor(new Color(0x13232e));
            g.fillRect(0,302,WIDTH,HEIGHT-302);
            g.setColor(new Color(0xb1ddce));
            g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,20));
            g.drawString("Recorded motion",24,33);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,15));
            g.drawString(String.format(Locale.ROOT,"TICK %04d  /  %.3f s",frame.tick,frame.tick*BulletRamp.DT),24,327);
            g.drawString(String.format(Locale.ROOT,"SPEED %.2f m/s",frame.speed),400,327);
            g.setColor(new Color(0x314953));
            g.fillRect(24,343,WIDTH-48,5);
            g.setColor(new Color(0x75bea7));
            g.fillRect(24,343,(int)((WIDTH-48)*frame.tick/(double)endTick),5);
        } finally {
            g.dispose();
        }
        return image;
    }

    static byte[] jpeg(BufferedImage image,float quality) throws IOException {
        var writer=ImageIO.getImageWritersByFormatName("JPEG").next();
        var bytes=new ByteArrayOutputStream();
        try (var stream=ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(stream);
            var params=writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
            writer.write(null,new IIOImage(image,null,null),params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private static double psnr(BufferedImage original, BufferedImage decoded) throws IOException {
        if (decoded==null || decoded.getWidth()!=WIDTH || decoded.getHeight()!=HEIGHT)
            throw new IOException("The trial JPEG did not decode at the requested size.");
        long squared=0;
        int[] a=original.getRGB(0,0,WIDTH,HEIGHT,null,0,WIDTH);
        int[] b=decoded.getRGB(0,0,WIDTH,HEIGHT,null,0,WIDTH);
        for (int i=0;i<a.length;i++) for (int shift:new int[]{0,8,16}) {
            int difference=((a[i]>>>shift)&255)-((b[i]>>>shift)&255);
            squared+=(long)difference*difference;
        }
        return squared==0 ? 99 : 10*StrictMath.log10(255.0*255*3*a.length/squared);
    }
}

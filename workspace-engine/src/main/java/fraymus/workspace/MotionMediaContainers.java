package fraymus.workspace;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/** Deterministic RIFF writers and checks for the workspace's bounded JPEG/PCM profile. */
final class MotionMediaContainers {
    static final int WIDTH = 640, HEIGHT = 360, FPS = 24, HZ = 24000;
    static final int PCM_PER_FRAME = HZ / FPS * 2;
    private MotionMediaContainers() {}

    static byte[] wav(byte[] pcm) {
        var format = new Writer();
        waveFormat(format);
        return container("RIFF","WAVE",chunk("fmt ",format.bytes()),chunk("data",pcm));
    }

    static byte[] avi(List<byte[]> frames, byte[] pcm) {
        int maximum = frames.stream().mapToInt(f -> f.length).max().orElseThrow();
        var main = new Writer();
        main.ints(1_000_000/FPS,(maximum+PCM_PER_FRAME)*FPS,0,0x110,frames.size(),0,2,maximum,WIDTH,HEIGHT,0,0,0,0);
        var video = stream("vids","MJPG",1,FPS,frames.size(),maximum,0,WIDTH,HEIGHT);
        var bitmap = new Writer();
        bitmap.ints(40,WIDTH,HEIGHT).shorts(1,24).tag("MJPG").ints(WIDTH*HEIGHT*3,0,0,0,0);
        var sound = stream("auds","\0\0\0\0",2,HZ*2,pcm.length/2,PCM_PER_FRAME,2,0,0);
        var wave = new Writer();
        waveFormat(wave);
        byte[] headers = container("LIST","hdrl",chunk("avih",main.bytes()),
                container("LIST","strl",chunk("strh",video),chunk("strf",bitmap.bytes())),
                container("LIST","strl",chunk("strh",sound),chunk("strf",wave.bytes())));
        var playback = new Writer();
        playback.tag("movi");
        var index = new Writer();
        for (int i=0;i<frames.size();i++) {
            byte[] frame=frames.get(i);
            index.tag("00dc").ints(0x10,playback.size(),frame.length);
            playback.write(chunk("00dc",frame));
            index.tag("01wb").ints(0,playback.size(),PCM_PER_FRAME);
            playback.write(chunk("01wb",Arrays.copyOfRange(pcm,i*PCM_PER_FRAME,(i+1)*PCM_PER_FRAME)));
        }
        return container("RIFF","AVI ",headers,chunk("LIST",playback.bytes()),chunk("idx1",index.bytes()));
    }

    private static byte[] stream(String type,String codec,int scale,int rate,int length,int buffer,int sampleSize,int width,int height) {
        var out = new Writer();
        out.tag(type).tag(codec).ints(0).shorts(0,0)
                .ints(0,scale,rate,0,length,buffer,-1,sampleSize).shorts(0,0,width,height);
        return out.bytes();
    }

    private static void waveFormat(Writer out) {
        out.shorts(1,1).ints(HZ,HZ*2).shorts(2,16);
    }

    static void checkWav(byte[] wav, byte[] pcm) throws Exception {
        try (var decoded = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
            var format = decoded.getFormat();
            require(format.getChannels()==1 && format.getSampleRate()==HZ && format.getSampleSizeInBits()==16
                    && !format.isBigEndian() && decoded.getFrameLength()==pcm.length/2
                    && Arrays.equals(decoded.readAllBytes(),pcm),"WAV did not preserve exact PCM and timing");
        }
    }

    static void checkAvi(byte[] avi, List<byte[]> frames, byte[] pcm) throws IOException {
        var file = ByteBuffer.wrap(avi).order(ByteOrder.LITTLE_ENDIAN);
        require(tag(file).equals("RIFF") && file.getInt()==avi.length-8 && tag(file).equals("AVI "),"Invalid AVI envelope");
        var hdrl = section(block(file,"LIST"),"hdrl");
        var main = block(hdrl,"avih");
        require(main.remaining()==56 && main.getInt(16)==frames.size() && main.getInt(24)==2
                && main.getInt(32)==WIDTH && main.getInt(36)==HEIGHT,"Invalid AVI frame count or dimensions");
        var video = section(block(hdrl,"LIST"),"strl");
        var videoh = block(video,"strh");
        require(tag(videoh).equals("vids") && tag(videoh).equals("MJPG") && videoh.getInt(20)==1
                && videoh.getInt(24)==FPS && videoh.getInt(32)==frames.size(),"Invalid AVI video clock");
        var bitmap = block(video,"strf");
        require(bitmap.getInt(4)==WIDTH && bitmap.getInt(8)==HEIGHT && bitmap.getInt(16)==fourcc("MJPG")
                && !video.hasRemaining(),"Invalid AVI video description");
        var audio = section(block(hdrl,"LIST"),"strl");
        var audioh = block(audio,"strh");
        require(tag(audioh).equals("auds") && audioh.getInt(20)==2 && audioh.getInt(24)==HZ*2
                && audioh.getInt(32)==pcm.length/2 && audioh.getInt(44)==2,"Invalid AVI audio clock");
        var wave = block(audio,"strf");
        require(wave.getShort(0)==1 && wave.getShort(2)==1 && wave.getInt(4)==HZ && wave.getShort(14)==16
                && !audio.hasRemaining() && !hdrl.hasRemaining(),"Invalid AVI PCM format");
        var movi = section(block(file,"LIST"),"movi");
        var index = block(file,"idx1");
        require(index.remaining()==frames.size()*32 && !file.hasRemaining(),"Invalid AVI index length");
        for (int i=0;i<frames.size();i++) {
            int frameOffset=movi.position(), frameLength=frames.get(i).length;
            var frame=block(movi,"00dc");
            require(tag(index).equals("00dc") && index.getInt()==0x10 && index.getInt()==frameOffset
                    && index.getInt()==frameLength && frame.equals(ByteBuffer.wrap(frames.get(i))),"AVI video differs from verified JPEG");
            if (i==0 || i==frames.size()/2 || i==frames.size()-1) {
                var decoded=ImageIO.read(new ByteArrayInputStream(frames.get(i)));
                require(decoded!=null && decoded.getWidth()==WIDTH && decoded.getHeight()==HEIGHT,"AVI JPEG did not decode");
            }
            int soundOffset=movi.position();
            var sound=block(movi,"01wb");
            var expected=ByteBuffer.wrap(pcm,i*PCM_PER_FRAME,PCM_PER_FRAME);
            require(tag(index).equals("01wb") && index.getInt()==0 && index.getInt()==soundOffset
                    && index.getInt()==PCM_PER_FRAME && sound.equals(expected),"AVI PCM differs from verified track");
        }
        require(!movi.hasRemaining() && !index.hasRemaining(),"AVI contains unexpected samples");
    }

    private static void require(boolean ok,String error) throws IOException {
        if (!ok) throw new IOException(error);
    }

    private static int fourcc(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.US_ASCII)).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static String tag(ByteBuffer buffer) {
        byte[] tag=new byte[4];
        buffer.get(tag);
        return new String(tag,StandardCharsets.US_ASCII);
    }

    private static ByteBuffer section(ByteBuffer bytes,String type) throws IOException {
        require(tag(bytes).equals(type),"Unexpected RIFF list type");
        return bytes;
    }

    private static ByteBuffer block(ByteBuffer parent,String name) throws IOException {
        require(parent.remaining()>=8 && tag(parent).equals(name),"Missing RIFF chunk "+name);
        int length=parent.getInt();
        require(length>=0 && (long)length+(length&1)<=parent.remaining(),"Invalid RIFF chunk size");
        var bytes=parent.slice(parent.position(),length).order(ByteOrder.LITTLE_ENDIAN);
        parent.position(parent.position()+length+(length&1));
        return bytes;
    }

    private static byte[] chunk(String type,byte[] data) {
        var out=new Writer();
        out.tag(type).ints(data.length).write(data);
        if ((data.length&1)!=0) out.pad();
        return out.bytes();
    }

    private static byte[] container(String outer,String name,byte[]... chunks) {
        var out=new Writer();
        out.tag(name);
        for (byte[] chunk:chunks) out.write(chunk);
        return chunk(outer,out.bytes());
    }

    private static final class Writer {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer write(byte[] data) { out.writeBytes(data); return this; }
        Writer tag(String value) { return write(value.getBytes(StandardCharsets.US_ASCII)); }
        Writer shorts(int... values) {
            for (int value:values) { out.write(value&255); out.write((value>>>8)&255); }
            return this;
        }
        Writer ints(int... values) {
            for (int value:values) { shorts(value,value>>>16); }
            return this;
        }
        Writer pad() { out.write(0); return this; }
        int size() { return out.size(); }
        byte[] bytes() { return out.toByteArray(); }
    }
}

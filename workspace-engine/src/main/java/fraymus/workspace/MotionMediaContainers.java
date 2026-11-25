package fraymus.workspace;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    record ImportedClip(List<byte[]> frames, byte[] pcm, int width, int height) {}

    static ImportedClip readClip(byte[] bytes, int maxFrames) throws IOException {
        return readClip(bytes,maxFrames,true);
    }

    static ImportedClip readClip(byte[] bytes, int maxFrames, boolean validateFrames) throws IOException {
        var file=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        var riff=section(block(file,"RIFF"),"AVI ");
        ByteBuffer header=null, movie=null;
        while (riff.hasRemaining()) {
            var child=next(riff);
            if (!child.name.equals("LIST")) continue;
            String name=tag(child.payload);
            if (name.equals("hdrl")) header=child.payload;
            if (name.equals("movi")) movie=child.payload;
        }
        require(!file.hasRemaining() && header!=null && movie!=null,"AVI needs header and movie data.");
        int streamCount=0, width=0, height=0, videoStream=-1, audioStream=-1, declaredFrames=-1;
        while (header.hasRemaining()) {
            var child=next(header);
            if (!child.name.equals("LIST") || !tag(child.payload).equals("strl")) continue;
            ByteBuffer strh=null, strf=null;
            while (child.payload.hasRemaining()) {
                var field=next(child.payload);
                if (field.name.equals("strh")) strh=field.payload;
                if (field.name.equals("strf")) strf=field.payload;
            }
            require(strh!=null && strf!=null && strh.remaining()>=48,"Invalid AVI track header.");
            String type=tag(strh), codec=tag(strh);
            if (type.equals("vids")) {
                require(videoStream<0 && codec.equals("MJPG") && strh.getInt(20)>0
                        && (long)strh.getInt(24)==(long)FPS*strh.getInt(20) && strf.remaining()>=40
                        && strf.getInt(16)==fourcc("MJPG"),"Use a 24 fps Motion JPEG AVI.");
                width=strf.getInt(4);
                height=strf.getInt(8);
                require(width>=16 && width<=1280 && height>=16 && height<=720,"AVI dimensions exceed the timeline limit.");
                videoStream=streamCount;
                declaredFrames=strh.getInt(32);
            } else if (type.equals("auds")) {
                require(audioStream<0 && strf.remaining()>=16 && strf.getShort(0)==1 && strf.getShort(2)==1
                        && strf.getInt(4)==HZ && strf.getShort(12)==2 && strf.getShort(14)==16,
                        "AVI audio must be 24 kHz mono 16-bit PCM.");
                audioStream=streamCount;
            } else throw new IOException("AVI contains an unsupported track.");
            streamCount++;
        }
        require(videoStream>=0 && declaredFrames>0 && declaredFrames<=maxFrames,"AVI is missing video or exceeds the clip duration.");
        var video=new ArrayList<byte[]>();
        var sound=new ByteArrayOutputStream();
        collectMovie(movie,"0"+videoStream,audioStream<0?"":"0"+audioStream,video,sound,maxFrames,true);
        require(video.size()==declaredFrames,"AVI frames differ from its declared timeline.");
        require((sound.size()&1)==0 && sound.size()<=maxFrames*PCM_PER_FRAME,"AVI audio exceeds the duration limit.");
        if (validateFrames) for (var bytesFrame:video) decodeFrame(bytesFrame,width,height);
        return new ImportedClip(video,sound.toByteArray(),width,height);
    }

    private static void collectMovie(ByteBuffer movie,String videos,String sounds,List<byte[]> video,
                                     ByteArrayOutputStream sound,int maxFrames,boolean allowLists) throws IOException {
        while (movie.hasRemaining()) {
            var part=next(movie);
            if (part.name.equals("LIST") && allowLists && tag(part.payload).equals("rec ")) {
                collectMovie(part.payload,videos,sounds,video,sound,maxFrames,false);
            } else if (part.name.equals(videos+"dc") || part.name.equals(videos+"db")) {
                require(video.size()<maxFrames && part.payload.remaining()<=1_000_000,"AVI video exceeds the clip budget.");
                var bytes=new byte[part.payload.remaining()];
                part.payload.get(bytes);
                video.add(bytes);
            } else if (!sounds.isEmpty() && part.name.equals(sounds+"wb")) {
                require((long)sound.size()+part.payload.remaining()<=maxFrames*PCM_PER_FRAME,"AVI audio exceeds the clip duration.");
                var bytes=new byte[part.payload.remaining()];
                part.payload.get(bytes);
                sound.writeBytes(bytes);
            }
        }
    }

    static BufferedImage decodeFrame(byte[] bytes,int width,int height) throws IOException {
        try (var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var reader=ImageIO.getImageReaders(stream);
            require(reader.hasNext(),"A video frame could not be read.");
            var jpeg=reader.next();
            try {
                jpeg.setInput(stream,true,true);
                require(jpeg.getFormatName().equalsIgnoreCase("jpeg") && jpeg.getWidth(0)==width && jpeg.getHeight(0)==height,
                        "AVI frame format or dimensions differ from its header.");
                return jpeg.read(0);
            } finally {
                jpeg.dispose();
            }
        }
    }

    static byte[] readPcmWav(byte[] bytes,int maxFrames) throws Exception {
        try (var stream=AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))) {
            var format=stream.getFormat();
            require(format.getEncoding().equals(javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED)
                    && format.getSampleRate()==HZ && format.getChannels()==1 && format.getSampleSizeInBits()==16
                    && !format.isBigEndian() && stream.getFrameLength()>0 && stream.getFrameLength()<=maxFrames*(long)(HZ/FPS),
                    "Use a 24 kHz mono 16-bit PCM WAV within the timeline duration.");
            var pcm=stream.readNBytes(maxFrames*PCM_PER_FRAME+1);
            require(pcm.length==stream.getFrameLength()*2 && pcm.length%2==0,"WAV PCM differs from its declared length.");
            return pcm;
        }
    }

    private record Child(String name,ByteBuffer payload) {}
    private static Child next(ByteBuffer buffer) throws IOException {
        require(buffer.remaining()>=8,"Truncated RIFF chunk.");
        String name=tag(buffer);
        return new Child(name,payload(buffer));
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
        return payload(parent);
    }

    private static ByteBuffer payload(ByteBuffer parent) throws IOException {
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

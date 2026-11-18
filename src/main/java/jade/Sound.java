package jade;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_filename;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.libc.LibCStdlib.free;

public class Sound {
    private int bufferId;
    private int sourceId;
    private String filepath;

    private boolean isPlaying = false;

    public Sound(String filepath, boolean loops) {
        this.filepath = filepath;
        if (!AudioSystem.available()) return;
        if (filepath.endsWith(".wav")) {
            loadPcmWave(filepath, loops);
            return;
        }

        // Allocate space to store the return information from stb
        stackPush();
        IntBuffer channelsBuffer = stackMallocInt(1);
        stackPush();
        IntBuffer sampleRateBuffer = stackMallocInt(1);

        ShortBuffer rawAudioBuffer =
                stb_vorbis_decode_filename(filepath, channelsBuffer, sampleRateBuffer);
        if (rawAudioBuffer == null) {
            System.out.println("Could not load sound '" + filepath + "'");
            stackPop();
            stackPop();
            return;
        }

        // Retrieve the extra information that was stored in the buffers by stb
        int channels = channelsBuffer.get();
        int sampleRate = sampleRateBuffer.get();
        // Free
        stackPop();
        stackPop();

        // Find the correct openAL format
        int format = -1;
        if (channels == 1) {
            format = AL_FORMAT_MONO16;
        } else if (channels == 2) {
            format = AL_FORMAT_STEREO16;
        }

        bufferId = alGenBuffers();
        alBufferData(bufferId, format, rawAudioBuffer, sampleRate);

        // Generate the source
        sourceId = alGenSources();

        alSourcei(sourceId, AL_BUFFER, bufferId);
        alSourcei(sourceId, AL_LOOPING, loops ? 1 : 0);
        alSourcei(sourceId, AL_POSITION, 0);
        alSourcef(sourceId, AL_GAIN, 0.3f);

        // Free stb raw audio buffer
        free(rawAudioBuffer);
    }

    public void delete() {
        if (sourceId == 0) return;
        alDeleteSources(sourceId);
        alDeleteBuffers(bufferId);
    }

    private void loadPcmWave(String path, boolean loops) {
        try (var stream = javax.sound.sampled.AudioSystem.getAudioInputStream(new java.io.File(path))) {
            var format = stream.getFormat();
            if (format.getEncoding() != javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED
                    || format.getSampleSizeInBits() != 16 || format.isBigEndian()
                    || format.getChannels() != 1 || format.getSampleRate() != 22050)
                throw new IllegalArgumentException("Original cues require mono 16-bit 22050Hz PCM");
            byte[] bytes = stream.readNBytes(220501);
            if (bytes.length > 220500 || bytes.length % 2 != 0) throw new IllegalArgumentException("Oversized or invalid cue");
            java.nio.ByteBuffer samples = org.lwjgl.BufferUtils.createByteBuffer(bytes.length);
            samples.put(bytes).flip();
            bufferId = alGenBuffers(); alBufferData(bufferId, AL_FORMAT_MONO16, samples, 22050);
            sourceId = alGenSources(); alSourcei(sourceId, AL_BUFFER, bufferId);
            alSourcei(sourceId, AL_LOOPING, loops ? 1 : 0); alSourcef(sourceId, AL_GAIN, .3f);
        } catch (Exception error) { throw new IllegalStateException("Cannot load original cue: " + path, error); }
    }

    public void play() {
        if (sourceId == 0) return;
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            isPlaying = false;
            alSourcei(sourceId, AL_POSITION, 0);
        }

        if (!isPlaying) {
            alSourcePlay(sourceId);
            isPlaying = true;
        }
    }

    public void stop() {
        if (sourceId == 0) return;
        if (isPlaying) {
            alSourceStop(sourceId);
            isPlaying = false;
        }
    }

    public String getFilepath() {
        return this.filepath;
    }

    public boolean isPlaying() {
        if (sourceId == 0) return false;
        int state = alGetSourcei(sourceId, AL_SOURCE_STATE);
        if (state == AL_STOPPED) {
            isPlaying = false;
        }
        return isPlaying;
    }
}

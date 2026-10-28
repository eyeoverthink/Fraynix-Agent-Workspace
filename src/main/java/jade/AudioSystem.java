package jade;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import static org.lwjgl.openal.ALC10.*;

/** Owns the application's OpenAL context on the engine thread. */
public final class AudioSystem {
    private static long device, context;
    private AudioSystem() {}
    public static boolean available() { return context != 0; }
    public static void open() {
        device = alcOpenDevice((java.nio.ByteBuffer)null);
        if (device == 0) { System.err.println("Audio device unavailable; gameplay remains available."); return; }
        context = alcCreateContext(device, (java.nio.IntBuffer)null);
        if (context == 0) { alcCloseDevice(device); device = 0; return; }
        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));
    }
    public static void close() {
        if (context != 0) { alcMakeContextCurrent(0); alcDestroyContext(context); context = 0; }
        if (device != 0) { alcCloseDevice(device); device = 0; }
    }
}

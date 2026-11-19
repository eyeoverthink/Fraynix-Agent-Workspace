import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Test fixture. Holds the same OS writer lock as WorldCheckpointStore until stdin closes. */
public class CheckpointLease {
    public static void main(String[] args) throws Exception {
        try (var channel = FileChannel.open(Path.of(args[0]), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var ignored = channel.lock()) {
            System.out.println("locked");
            System.out.flush();
            System.in.read();
        }
    }
}

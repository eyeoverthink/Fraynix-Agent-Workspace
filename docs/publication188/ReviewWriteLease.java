import com.sun.nio.file.ExtendedOpenOption;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Windows test fixture. Keeps the review readable while denying atomic replacement. */
public class ReviewWriteLease {
    public static void main(String[] args) throws Exception {
        try (var channel = FileChannel.open(Path.of(args[0]), StandardOpenOption.READ,
                ExtendedOpenOption.NOSHARE_DELETE)) {
            System.out.println("locked");
            System.out.flush();
            System.in.read();
        }
    }
}
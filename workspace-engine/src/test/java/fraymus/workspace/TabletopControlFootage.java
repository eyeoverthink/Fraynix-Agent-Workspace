package fraymus.workspace;
import javax.imageio.ImageIO;
import java.nio.file.*;
/** Explicit analytic software-camera fixture, never presented as physical footage. */
public final class TabletopControlFootage {public static void main(String[] args)throws Exception{Path out=Path.of(args[0]);Files.createDirectories(out);for(int i=1;i<=30;i++)ImageIO.write(TabletopCoreTest.image(0,0,false),"png",out.resolve(String.format("%03d.png",i)).toFile());System.out.println("30 labelled static analytic frames; no real webcam source");}}

package fraymus.workspace;

import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplayPaths152Test {
    @Test public void normalAndShortWindowsNamesAreNotRedirects()throws Exception{
        var ordinary=Files.createTempDirectory("ordinary152-");assertEquals(ordinary.toRealPath(),ReplayEvidence151.unlinkedRealPath(ordinary));
        if(System.getProperty("os.name").startsWith("Windows")){
            var shortName=Path.of("C:/PROGRA~1");if(Files.exists(shortName)&&Files.isSameFile(shortName,Path.of("C:/Program Files"))){assertEquals(Path.of("C:/Program Files").toRealPath(),ReplayEvidence151.unlinkedRealPath(shortName));System.out.println("Actual Windows short alias verified: "+shortName);}
        }
    }
    @Test public void redirectedRootAndAncestorRemainRefused()throws Exception{
        var temp=Files.createTempDirectory("links152-");var target=Files.createDirectory(temp.resolve("target"));Files.createDirectory(target.resolve("child"));var link=temp.resolve("redirect");
        if(System.getProperty("os.name").startsWith("Windows")){
            var process=new ProcessBuilder("cmd.exe","/c","mklink","/J",link.toString(),target.toString()).redirectErrorStream(true).start();String output=new String(process.getInputStream().readAllBytes());assertEquals(output,0,process.waitFor());
        }else Files.createSymbolicLink(link,target);
        assertThrows(IllegalStateException.class,()->ReplayEvidence151.unlinkedRealPath(link));assertThrows(IllegalStateException.class,()->ReplayEvidence151.unlinkedRealPath(link.resolve("child")));
        Files.delete(link);Files.delete(target.resolve("child"));Files.delete(target);Files.delete(temp);
    }
}

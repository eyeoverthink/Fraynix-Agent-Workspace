package fraymus.workspace;

import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class CalibrationScan081Test {
    private Map<String,List<Path>> files(Path root)throws Exception{
        var groups=new LinkedHashMap<String,List<Path>>();
        for(int i=0;i<8;i++){
            Path a=Files.writeString(root.resolve(i+".json"),"measured original "+i);
            Path b=Files.write(root.resolve(i+".bin"),new byte[1_000_000+i]);groups.put("goal-"+i,List.of(a,b));
        }return groups;
    }
    @Test public void allOrderedBytesMatchOriginalAndReturnedDataCannotPoisonNextRead()throws Exception{
        Path root=Files.createTempDirectory("scan081-");var groups=files(root);
        try(var scan=new CalibrationScan081(root)){
            var a=scan.parallel(groups);assertEquals(new ArrayList<>(groups.keySet()),new ArrayList<>(a.keySet()));
            for(var e:groups.entrySet())assertTrue(new ProofMemo078(root).snapshot(e.getValue()).similar(a.get(e.getKey())));
            a.get("goal-0").getJSONObject(0).put("sha256","forged");assertFalse(a.get("goal-0").similar(scan.parallel(groups).get("goal-0")));
        }
    }
    @Test public void sameSizeAndRestoredTimestampStillChangesTheProof()throws Exception{
        Path root=Files.createTempDirectory("scan081-");var f=Files.writeString(root.resolve("trace"),"original");var t=Files.getLastModifiedTime(f);
        try(var scan=new CalibrationScan081(root)){
            var groups=Map.of("goal",List.of(f));var a=scan.parallel(groups);Files.writeString(f,"tampered");Files.setLastModifiedTime(f,t);
            assertFalse(a.get("goal").similar(scan.parallel(groups).get("goal")));
            assertTrue(scan.serial(List.of(f)).similar(scan.parallel(groups).get("goal")));
        }
    }
    @Test public void missingInAnyGroupRefusesAndLaterFreshReadCanRecover()throws Exception{
        Path root=Files.createTempDirectory("scan081-");var groups=files(root);
        try(var scan=new CalibrationScan081(root)){
            for(int index:new int[]{0,4,7}){
                var p=groups.get("goal-"+index).get(0);byte[] saved=Files.readAllBytes(p);Files.delete(p);
                assertThrows(IllegalStateException.class,()->scan.parallel(groups));Files.write(p,saved);assertEquals(8,scan.parallel(groups).size());
            }
        }
    }
    @Test public void outsideDuplicateEmptyAndOversizedGroupsRefuse()throws Exception{
        Path root=Files.createTempDirectory("scan081-");var f=Files.writeString(root.resolve("trace"),"original");var other=Files.createTempFile("foreign081-",".json");
        try(var scan=new CalibrationScan081(root)){
            assertThrows(IllegalArgumentException.class,()->scan.parallel(Map.of()));
            assertThrows(IllegalArgumentException.class,()->scan.parallel(Map.of("g",List.of())));
            assertThrows(IllegalArgumentException.class,()->scan.parallel(Map.of("g",Collections.nCopies(33,f))));
            assertThrows(IllegalArgumentException.class,()->scan.parallel(Map.of("g",List.of(f,f))));
            assertThrows(IllegalStateException.class,()->scan.parallel(Map.of("g",List.of(other))));
            var groups=new LinkedHashMap<String,List<Path>>();for(int i=0;i<17;i++)groups.put("g"+i,List.of(f));
            assertThrows(IllegalArgumentException.class,()->scan.parallel(groups));
        }
    }
    @Test public void interruptionNeverReturnsPartialEvidenceAndCloseRejectsNewWork()throws Exception{
        Path root=Files.createTempDirectory("scan081-");var groups=files(root);var scan=new CalibrationScan081(root);
        try{Thread.currentThread().interrupt();assertThrows(InterruptedException.class,()->scan.parallel(groups));assertTrue(Thread.currentThread().isInterrupted());}
        finally{Thread.interrupted();}
        assertEquals(8,scan.parallel(groups).size());scan.close();assertThrows(IllegalStateException.class,()->scan.parallel(groups));
    }
    @Test public void redirectedParentCannotPassTheParallelReader()throws Exception{
        Path root=Files.createTempDirectory("scan081-");Path other=Files.createTempDirectory("redirect081-");Files.writeString(other.resolve("trace"),"original");Path link=root.resolve("redirect");
        if(System.getProperty("os.name").startsWith("Windows")){
            var p=new ProcessBuilder("pwsh","-NoProfile","-Command","New-Item -ItemType Junction -Path '"+link.toString().replace("'","''")+"' -Target '"+other.toString().replace("'","''")+"' | Out-Null").redirectErrorStream(true).start();String out=new String(p.getInputStream().readAllBytes());assertEquals(out,0,p.waitFor());
        }else Files.createSymbolicLink(link,other);
        try(var scan=new CalibrationScan081(root)){assertThrows(IllegalStateException.class,()->scan.parallel(Map.of("g",List.of(link.resolve("trace")))));}
    }
}

package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class NativeEnvelope091Test {
    private record Fixture(Path root,Path file,JSONObject run,String hash,NativeEnvelope091 reader){}
    private Fixture fixture()throws Exception {
        var root=Files.createTempDirectory("envelope091-");var file=root.resolve("original.json");
        var run=new JSONObject().put("id","attempt").put("goalId","goal").put("payload",new JSONArray().put(7).put(11));
        AgentFiles042.saveChecked(file,run);return new Fixture(root,file,run,WorldFrames.hash(run),new NativeEnvelope091(root));
    }
    private JSONObject read(Fixture f)throws Exception{return f.reader.read(f.file,"goal","attempt",f.hash);}
    @Test public void exactBytesReuseOnlyDigestAndReturnIndependentData()throws Exception {
        var f=fixture();var a=read(f);a.getJSONArray("payload").put(0,999);var b=read(f);
        assertTrue(b.similar(f.run));assertEquals(1,f.reader.stats().getLong("canonicalChecks"));assertEquals(1,f.reader.stats().getLong("hits"));
        assertEquals(Files.size(f.file)*2,f.reader.stats().getLong("bytes"));
    }
    @Test public void cachedBytesDoNotBypassCurrentExpectedIdentities()throws Exception {
        var f=fixture();read(f);
        assertThrows(Exception.class,()->f.reader.read(f.file,"other","attempt",f.hash));
        assertThrows(Exception.class,()->f.reader.read(f.file,"goal","other",f.hash));
        assertThrows(Exception.class,()->f.reader.read(f.file,"goal","attempt","0".repeat(64)));
        assertTrue(read(f).similar(f.run));assertEquals(3,f.reader.stats().getLong("failures"));
    }
    @Test public void sameSizeAndTimestampMutationIsRejectedAfterWarmHit()throws Exception {
        var f=fixture();read(f);var time=Files.getLastModifiedTime(f.file);var text=Files.readString(f.file);
        Files.writeString(f.file,text.replace("[7,11]","[8,11]"));assertEquals(text.length(),Files.size(f.file));Files.setLastModifiedTime(f.file,time);
        assertThrows(Exception.class,()->read(f));Files.writeString(f.file,text);assertTrue(read(f).similar(f.run));
    }
    @Test public void resignedChangedDataStillRequiresExpectedDigest()throws Exception {
        var f=fixture();read(f);f.run.getJSONArray("payload").put(0,55);AgentFiles042.saveChecked(f.file,f.run);
        assertThrows(Exception.class,()->read(f));assertTrue(f.reader.read(f.file,"goal","attempt",WorldFrames.hash(f.run)).similar(f.run));
    }
    @Test public void malformedFormatAndEnvelopeHashNeverEnterTable()throws Exception {
        var f=fixture();var envelope=new JSONObject(Files.readString(f.file));envelope.put("format","foreign");Files.writeString(f.file,envelope.toString());
        assertThrows(Exception.class,()->read(f));assertEquals(0,f.reader.stats().getInt("entries"));
        envelope.put("format","java-agent-042-v1").put("sha256","bad");Files.writeString(f.file,envelope.toString());assertThrows(Exception.class,()->read(f));
        Files.writeString(f.file,"{not json");assertThrows(Exception.class,()->read(f));assertEquals(0,f.reader.stats().getInt("entries"));
    }
    @Test public void missingAndEscapedFilesAreRejectedEvenForKnownBytes()throws Exception {
        var f=fixture();read(f);var elsewhere=Files.createTempFile("envelope091-outside-",".json");Files.copy(f.file,elsewhere,StandardCopyOption.REPLACE_EXISTING);
        assertThrows(Exception.class,()->f.reader.read(elsewhere,"goal","attempt",f.hash));Files.delete(f.file);assertThrows(Exception.class,()->read(f));
    }
    @Test public void symbolicLinkCannotRedirectOwnerWhenSupported()throws Exception {
        var f=fixture();read(f);var link=f.root.resolve("link.json");
        try{Files.createSymbolicLink(link,f.file);}catch(java.io.IOException|UnsupportedOperationException e){org.junit.Assume.assumeNoException("Host forbids symbolic-link creation",e);}
        assertThrows(Exception.class,()->f.reader.read(link,"goal","attempt",f.hash));
    }
    @Test public void byteLimitAndInvalidUtf8AreEnforced()throws Exception {
        var f=fixture();var small=new NativeEnvelope091(f.root,1,10);assertThrows(Exception.class,()->small.read(f.file,"goal","attempt",f.hash));
        Files.write(f.file,new byte[]{(byte)0xc3,(byte)0x28});assertThrows(Exception.class,()->read(f));
    }
    @Test public void coldOwnerAndEvictionRequireFullCanonicalCheck()throws Exception {
        var f=fixture();read(f);var cold=new NativeEnvelope091(f.root,1,32_000_000);cold.read(f.file,"goal","attempt",f.hash);
        assertEquals(1,cold.stats().getLong("canonicalChecks"));var other=f.root.resolve("other.json");var changed=WorldFrames.copy(f.run).put("id","other");AgentFiles042.saveChecked(other,changed);
        cold.read(other,"goal","other",WorldFrames.hash(changed));cold.read(f.file,"goal","attempt",f.hash);
        assertEquals(3,cold.stats().getLong("canonicalChecks"));assertEquals(1,cold.stats().getInt("entries"));
    }
    @Test public void referenceModeChecksEveryReadAndSwitchClearsTable()throws Exception {
        var f=fixture();read(f);f.reader.testMode("reference");assertEquals(0,f.reader.stats().getInt("entries"));
        for(int i=0;i<3;i++)assertTrue(read(f).similar(f.run));assertEquals(4,f.reader.stats().getLong("canonicalChecks"));assertEquals(0,f.reader.stats().getLong("hits"));
        f.reader.testMode("reuse");read(f);read(f);assertEquals(5,f.reader.stats().getLong("canonicalChecks"));assertEquals(1,f.reader.stats().getLong("hits"));
        assertThrows(Exception.class,()->f.reader.testMode("skip"));
    }
    @Test public void whitespaceChangeIsFreshBytesButSameCanonicalIdentity()throws Exception {
        var f=fixture();read(f);Files.writeString(f.file,new JSONObject(Files.readString(f.file)).toString(2));assertTrue(read(f).similar(f.run));
        assertEquals(2,f.reader.stats().getLong("canonicalChecks"));
    }
    @Test public void concurrentReadersHaveNoSharedMutableRun()throws Exception {
        var f=fixture();read(f);try(var pool=Executors.newFixedThreadPool(4)){
            var results=new ArrayList<Future<Boolean>>();for(int i=0;i<12;i++)results.add(pool.submit(()->{var run=read(f);boolean same=run.similar(f.run);run.put("id","local-only");return same;}));
            for(var result:results)assertTrue(result.get());
        }
        assertTrue(read(f).similar(f.run));assertEquals(1,f.reader.stats().getLong("canonicalChecks"));assertEquals(13,f.reader.stats().getLong("hits"));
    }
}

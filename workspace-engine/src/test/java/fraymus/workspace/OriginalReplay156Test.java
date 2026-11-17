package fraymus.workspace;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class OriginalReplay156Test {
    static class Fixture extends StartingReference150Test.Fixture {
        final JSONObject targetRun,targetAudit;
        Fixture()throws Exception{
            var set=service.retainStartingReference(audit.getString("id"));
            int revision=observations.state(world).getJSONObject("estimate").getInt("revision");
            targetRun=WorldObservationStoreTest.settled(observations,observations.start(WorldObservationStoreTest.arguments(ledger,world,dataset,revision)
                .put("trackerPolicy","identity-return-149").put("appearanceSet137",set.getString("id")).put("referenceDeclaration137","Independent generated source and evaluation")).getString("id"));
            assertEquals("completed",targetRun.getString("status"));
            targetAudit=association.audit(new JSONObject().put("runId",targetRun.getString("id")).put("labelId",association.save(AssociationAudit134Test.input(dataset)).getString("id")));
        }
        OriginalAudit156 reader(boolean packed){return new OriginalAudit156(root,observations,association,packed);}
    }
    @Test public void packedSearchExactlyMatchesOriginalAcrossImageEncodings()throws Exception{
        for(int type:new int[]{BufferedImage.TYPE_INT_RGB,BufferedImage.TYPE_3BYTE_BGR,BufferedImage.TYPE_4BYTE_ABGR,BufferedImage.TYPE_INT_ARGB_PRE,BufferedImage.TYPE_BYTE_GRAY,BufferedImage.TYPE_BYTE_INDEXED}){
            var image=new BufferedImage(320,240,type);var g=image.createGraphics();g.drawImage(IdentityReturn149Test.frame(23,false),0,0,null);g.dispose();
            if(image.getColorModel().hasAlpha())for(int y=0;y<12;y++)for(int x=0;x<12;x++)image.setRGB(x,y,(x*20)<<24|0x667788);
            for(String format:new String[]{"png","jpg"}){
                var bytes=new ByteArrayOutputStream();if(!ImageIO.write(image,format,bytes))continue;
                var decoded=ImageIO.read(new ByteArrayInputStream(bytes.toByteArray()));
                var original=new StructureSearch149(decoded,AppearanceConfirmation143Test.seed(),IdentityReturn149Test.refs());
                var packed=new StructureReplay156(decoded,AppearanceConfirmation143Test.seed(),IdentityReturn149Test.refs(),true);
                var a=original.search(decoded);var b=packed.search(decoded);assertEquals(a.size(),b.size());
                for(int i=0;i<a.size();i++)assertTrue(type+"/"+format,a.get(i).json().similar(b.get(i).json()));
                assertEquals(original.evaluations(),packed.evaluations());
            }
        }
    }
    @Test public void completeIdentityTraceAndMonitorMatchThroughLossAndReturns(){
        for(boolean enabled:new boolean[]{true,false})for(boolean wrong:new boolean[]{false,true}){
            var first=IdentityReturn149Test.frame(0,wrong);var refs=IdentityReturn149Test.refs();var seed=AppearanceConfirmation143Test.seed();
            var original=new IdentityReturn149("body","object",first,seed,refs,enabled);var packed=new IdentityReplay156("body","object",first,seed,refs,enabled,true);
            for(int i=1;i<=29;i++)assertTrue("Frame "+i,original.update(IdentityReturn149Test.frame(i,wrong)).similar(packed.update(IdentityReturn149Test.frame(i,wrong))));
        }
    }
    @Test public void completeOriginalAuditAndFallbackRemainExact()throws Exception{
        try(var f=new Fixture()){
            var before=f.ledger.observe(f.world);byte[] state=Files.readAllBytes(f.root.resolve("world-observations/state.json"));
            assertTrue(f.targetAudit.similar(f.reader(true).readAudit(f.targetAudit.getString("id"))));
            assertTrue(f.reader(false).readAudit(f.targetAudit.getString("id")).similar(f.reader(true).readAudit(f.targetAudit.getString("id"))));
            assertTrue(f.audit.similar(f.reader(true).readAudit(f.audit.getString("id"))));
            assertArrayEquals(state,Files.readAllBytes(f.root.resolve("world-observations/state.json")));assertTrue(before.similar(f.ledger.observe(f.world)));
        }
    }
    @Test public void forgedAuditWithRecomputedChecksumIsRefused()throws Exception{
        try(var f=new Fixture()){
            for(String kind:new String[]{"summary","identity","code","run"}){
                var forged=WorldFrames.copy(f.targetAudit);forged.remove("id");
                if(kind.equals("summary"))forged.getJSONObject("summary").put("correct",999);
                else if(kind.equals("identity"))forged.getJSONObject("identity149").put("parentAcceptedPreserved",999);
                else forged.put(kind.equals("code")?"codeSha256":"runSha256","0".repeat(64));
                String id=WorldFrames.hash(forged);forged.put("id",id);Files.writeString(f.root.resolve("association-labels134/audits/"+id+".json"),forged.toString());
                assertThrows(IllegalStateException.class,()->f.reader(true).readAudit(id));assertThrows(IllegalStateException.class,()->f.reader(false).readAudit(id));
            }
        }
    }
    @Test public void originalSourceAndReferenceBytesStillChecked()throws Exception{
        try(var f=new Fixture()){
            for(String dataset:new String[]{f.dataset.getString("id"),f.data.getString("id")}){
                Path image=f.root.resolve("world-observations/datasets/"+dataset+"/1.image");byte[] raw=Files.readAllBytes(image),bad=raw.clone();var time=Files.getLastModifiedTime(image);bad[20]^=1;
                Files.write(image,bad);Files.setLastModifiedTime(image,time);
                try{assertThrows(Exception.class,()->f.reader(true).readAudit(f.targetAudit.getString("id")));assertThrows(Exception.class,()->f.reader(false).readAudit(f.targetAudit.getString("id")));}
                finally{Files.write(image,raw);Files.setLastModifiedTime(image,time);}
            }
            assertTrue(f.targetAudit.similar(f.reader(true).readAudit(f.targetAudit.getString("id"))));
        }
    }
    @Test public void changedRunTraceCodeAndReferenceCannotPassARehashedState()throws Exception{
        try(var f=new Fixture()){
            Path state=f.root.resolve("world-observations/state.json");byte[] original=Files.readAllBytes(state);
            for(String kind:new String[]{"trace","code","reference"}){
                var envelope=new JSONObject(new String(original,java.nio.charset.StandardCharsets.UTF_8));var payload=envelope.getJSONObject("payload");
                for(Object raw:payload.getJSONArray("runs")){var run=(JSONObject)raw;if(run.getString("id").equals(f.targetRun.getString("id"))){
                    if(kind.equals("code"))run.put("codeSha256","0".repeat(64));
                    else if(kind.equals("reference"))run.getJSONObject("referenceMemory137").put("setSha256","0".repeat(64));
                    else run.getJSONArray("trace").getJSONObject(0).getJSONArray("objects").getJSONObject(0).getJSONObject("tracker").getJSONObject("identity149").put("ordinal",999);
                }}
                envelope.put("sha256",WorldFrames.hash(payload));Files.writeString(state,envelope.toString());
                try{assertThrows(Exception.class,()->f.reader(true).readAudit(f.targetAudit.getString("id")));assertThrows(Exception.class,()->f.reader(false).readAudit(f.targetAudit.getString("id")));}finally{Files.write(state,original);}
            }
        }
    }
}

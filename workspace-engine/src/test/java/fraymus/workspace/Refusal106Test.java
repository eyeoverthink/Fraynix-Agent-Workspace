package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Admission authority/lifecycle fixtures. The separate Electron run tests real source rejection. */
public class Refusal106Test {
    static void domain(Agency043Test.Fixture f){f.agency.install044(new Domain044((r,a)->r.equals("/api/state")?new JSONObject().put("scenes",new JSONArray()).put("datasets",new JSONArray()).put("procedures",new JSONArray()):new JSONObject(),f.store));}
    static JSONObject start(Agency043Test.Fixture f)throws Exception{
        domain(f);var g=f.agency.start(new JSONObject().put("goal","Qualify the input-driven summary, analyze the selected data, and save workflow as refusal-test.").put("model","unit-no-inference").put("bindings",new JSONObject().put("dataset",new JSONObject().put("datasetId",UUID.randomUUID().toString()).put("version",1))));f.agency.tick();assertEquals("waiting-host",f.agency.read(g.getString("id")).getString("status"));return f.claim();
    }
    static final String ROUTE="/api/workflows/start",REASON="Created artifact was changed outside its owned revision";
    @Test public void originalNativeReasonIsTerminalAndLateReceiptCannotReplaceIt()throws Exception{
        var calls=new AtomicInteger();try(var f=new Agency043Test.Fixture(r->{calls.incrementAndGet();throw new AssertionError("Refused admission must not plan");})){
            var c=start(f);String id=c.getString("taskId"),nonce=c.getString("nonce");assertTrue(f.agency.admissionClaim106(id,nonce,ROUTE,f.session));
            f.agency.refuseAdmission106(id,nonce,ROUTE,f.session,new IllegalStateException(REASON));
            var before=f.agency.read(id);assertEquals("interrupted",before.getString("status"));assertEquals(REASON,before.getString("reason"));assertEquals(0,before.getInt("failures"));assertFalse(f.agency.busy());
            for(int i=0;i<5;i++)f.agency.tick();assertEquals(0,calls.get());assertEquals(0,f.agency.read(id).getJSONObject("timing052").getInt("modelCalls"));
            f.agency.acknowledge(f.authority(c).put("context",new JSONObject().put("ownedWorkflowId","invented")).put("error","Replace the original reason").put("result",new JSONObject().put("passed",true)));
            f.agency.abort(f.authority(c).put("error","Another transport failure"));assertTrue(before.similar(f.agency.read(id)));
            assertThrows(IllegalStateException.class,()->f.agency.control(id,"resume"));assertEquals(REASON,f.agency.read(id).getString("reason"));
            f.agency.control(id,"cancel");assertEquals("cancelled",f.agency.read(id).getString("status"));assertEquals(REASON,f.agency.read(id).getJSONObject("refusal106").getString("reason"));
        }
    }
    @Test public void foreignStaleAndUnrelatedAuthoritiesCannotMarkOrAcknowledgeRefusal()throws Exception{
        try(var f=new Agency043Test.Fixture(r->{throw new AssertionError("No planner expected");})){
            var c=start(f);String id=c.getString("taskId"),nonce=c.getString("nonce");
            assertFalse(f.agency.admissionClaim106(id,nonce,ROUTE,UUID.randomUUID().toString()));assertFalse(f.agency.admissionClaim106(id,UUID.randomUUID().toString(),ROUTE,f.session));assertFalse(f.agency.admissionClaim106(id,nonce,"/api/workflows/reuse",f.session));assertFalse(f.agency.admissionClaim106(id,nonce,"/api/artifact/read",f.session));
            assertThrows(IllegalStateException.class,()->f.agency.refuseAdmission106(id,nonce,ROUTE,"foreign",new Exception(REASON)));assertFalse(f.agency.read(id).has("refusal106"));
            f.agency.refuseAdmission106(id,nonce,ROUTE,f.session,new Exception(REASON));
            assertThrows(IllegalStateException.class,()->f.agency.acknowledge(f.authority(c).put("nonce","foreign").put("context",new JSONObject())));
            assertThrows(IllegalStateException.class,()->f.agency.abort(f.authority(c).put("effectId",UUID.randomUUID().toString())));
            assertThrows(IllegalStateException.class,()->f.agency.refuseAdmission106(id,nonce,ROUTE,f.session,new Exception("Overwrite")));assertEquals(REASON,f.agency.read(id).getString("reason"));
        }
    }
    @Test public void browserTextCannotForgeNativeRefusalAndTransportRepairRemains()throws Exception{
        try(var f=new Agency043Test.Fixture(r->{throw new AssertionError("Fixture stops before retry");})){
            var c=start(f);assertThrows(IllegalArgumentException.class,()->f.agency.acknowledge(f.authority(c).put("context",new JSONObject()).put("error",REASON).put("refusal106",new JSONObject().put("reason",REASON))));
            f.agency.acknowledge(f.authority(c).put("context",new JSONObject()).put("error",REASON));var g=f.agency.read(c.getString("taskId"));assertFalse(g.has("refusal106"));assertEquals("ready",g.getString("status"));assertEquals(1,g.getInt("failures"));
        }
    }
    @Test public void coldRestartKeepsOriginalReasonAndResumeProhibition()throws Exception{
        try(var f=new Agency043Test.Fixture(r->{throw new AssertionError("No planner expected");})){
            var c=start(f);String id=c.getString("taskId");f.agency.refuseAdmission106(id,c.getString("nonce"),ROUTE,f.session,new IllegalStateException(REASON));var before=f.agency.read(id);f.agency.close();
            f.agency=new Agency043(f.root,f.store,f.learning,r->{throw new AssertionError("No planner on restart");},x->{},(k,d)->{},()->false,false);domain(f);f.register();for(int i=0;i<3;i++)f.agency.tick();var after=f.agency.read(id);
            assertEquals(REASON,after.getString("reason"));assertTrue(before.getJSONArray("history").similar(after.getJSONArray("history")));assertTrue(before.getJSONObject("refusal106").similar(after.getJSONObject("refusal106")));assertThrows(IllegalStateException.class,()->f.agency.control(id,"resume"));
        }
    }
}

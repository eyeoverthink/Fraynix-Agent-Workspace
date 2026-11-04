package fraymus.workspace;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.nio.file.*;
import java.util.*;

public class Stopping063Test {
    @Test public void nativeInterventionsCalibrationIntegrityAndColdRestart()throws Exception{
        Path root=Files.createTempDirectory("fraynix-stopping063-");JSONObject job;String id;
        try(var brain=new BrainService(root)){
            id=brain.start(new JSONObject().put("kind","stopping-uncertainty")).getString("id");waitFor(brain);
            job=brain.read(id);Files.writeString(root.resolve("original-experiment.json"),job.toString());System.out.println("EDITION063_NATIVE_ORIGINALS="+root);
            assertEquals(job.optString("reason"),"completed",job.getString("status"));assertEquals(104,job.getInt("progress"));Stopping063.audit(job);assertEquals(1,brain.activePolicy().getInt("version"));
            for(Object raw:job.getJSONArray("diagnostics063")){var row=(JSONObject)raw;assertEquals(721,row.getJSONObject("record").getJSONArray("samples").length());if(row.getString("variant").equals("baseline"))assertTrue(row.getDouble("parityErrorM")<=1e-6);if(row.getString("variant").equals("rotationLocked"))assertTrue(row.getJSONObject("record").getJSONObject("summary").getDouble("maximumRotationDeg")<.001);}
            var request=new JSONObject().put("jobId",id).put("definition",Stopping063.cases("reserved").getJSONObject(0));Files.writeString(root.resolve("prediction.json"),brain.stoppingPredict063(request).toString());
            assertEquals(721,brain.stoppingTrace063(new JSONObject().put("jobId",id).put("traceIndex",0)).getJSONArray("series").length());
            tamper(job,j->j.getJSONObject("evaluation063").put("qualified",!j.getJSONObject("evaluation063").getBoolean("qualified")));
            tamper(job,j->{var m=j.getJSONObject("intervalModel063");m.getJSONObject("radiiM").put("slowing",1);j.put("intervalModelSha256063",WorldFrames.hash(m));});
            tamper(job,j->j.getJSONArray("calibration063").put(1,j.getJSONArray("calibration063").getJSONObject(0)));
            tamper(job,j->j.getJSONArray("reserved063").getJSONObject(0).getJSONObject("intervalsBeforeObservation").getJSONObject("regime").put("upperM",5));
            tamper(job,j->{var r=j.getJSONArray("diagnostics063").getJSONObject(0).getJSONObject("record");r.getJSONObject("summary").put("firstTouchTick",999);rehash(r);});
            tamper(job,j->{var r=j.getJSONArray("diagnostics063").getJSONObject(0).getJSONObject("record");r.getJSONObject("physicalParameters").put("massKg",2);rehash(r);});
            tamper(job,j->{var r=j.getJSONArray("diagnostics063").getJSONObject(0).getJSONObject("record");r.getJSONArray("samples").getJSONObject(1).put("tick",77);rehash(r);});
            tamper(job,j->j.getJSONArray("reserved063").getJSONObject(0).put("predictedAt","2000-01-01T00:00:00Z"));
            System.out.println("RADII="+job.getJSONObject("intervalModel063"));System.out.println("EVALUATION="+job.getJSONObject("evaluation063"));
        }
        try(var brain=new BrainService(root)){
            var prediction=brain.stoppingPredict063(new JSONObject().put("jobId",id).put("definition",Stopping063.cases("reserved").getJSONObject(0)));assertTrue(new JSONObject(Files.readString(root.resolve("prediction.json"))).similar(prediction));
            String stopped=brain.start(new JSONObject().put("kind","stopping-uncertainty")).getString("id");brain.cancel(stopped);waitFor(brain);assertEquals("cancelled",brain.read(stopped).getString("status"));assertEquals(id,brain.stoppingState063().getJSONObject("lastCompleted").getString("jobId"));assertTrue(job.similar(brain.read(id)));
        }
    }
    static void waitFor(BrainService b)throws Exception{long end=System.nanoTime()+600_000_000_000L;while(b.state().getBoolean("busy")&&System.nanoTime()<end)Thread.sleep(500);assertFalse(b.state().getBoolean("busy"));}
    interface Edit{void run(JSONObject j);}
    static void tamper(JSONObject j,Edit e){var copy=WorldFrames.copy(j);e.run(copy);try{Stopping063.audit(copy);fail("Changed evidence accepted");}catch(IllegalStateException expected){}}
    static void rehash(JSONObject r){r.remove("sha256");r.put("sha256",WorldFrames.hash(r));}
    @Test public void partitionsAreDistinctAndDrawsStayInDeclaredDomain(){var seen=new HashSet<String>();for(String phase:List.of("calibration","reserved")){int slow=0;for(Object raw:Stopping063.cases(phase)){var c=(JSONObject)raw;Stopping063.queryDomain(c);assertTrue(seen.add(WorldFrames.hash(c)));if(Stopping063.regime(c).equals("slowing"))slow++;}assertEquals(20,slow);}assertEquals(80,seen.size());}
    @Test public void quantilesUseFiniteSampleRankAndRejectInvalidInput(){double[] a=new double[20];for(int i=0;i<20;i++)a[i]=i;assertEquals(18,Stopping063.quantile(a),0);double[] b=new double[40];for(int i=0;i<40;i++)b[i]=i;assertEquals(36,Stopping063.quantile(b),0);for(double[] invalid:new double[][]{new double[8],{Double.NaN,0,0,0,0,0,0,0,0}})try{Stopping063.quantile(invalid);fail();}catch(IllegalArgumentException expected){}}
    @Test public void domainRefusesGapAndExtrapolation(){for(double ratio:new double[]{.84,.99,1,1.01,1.2})try{Stopping063.intervals(new JSONObject().put("pooled",.02).put("accelerating",.01).put("slowing",.03),Stopping063.definition(20,ratio,.2));fail();}catch(IllegalArgumentException expected){} }
    @Test public void rotationSignAndUnknownContactAreHandled(){var c=Stopping063.definition(30,1.05,.25);var p=RampDefinition.from(ResidualOutcome062.physical(c));var s=new JSONObject().put("tick",0).put("timeS",0).put("position",new JSONArray(p.start())).put("velocity",new JSONArray(new double[]{Math.cos(p.radians()),-Math.sin(p.radians()),0})).put("angularVelocity",new JSONArray(new int[]{0,0,0})).put("rotation",new JSONArray(new double[]{0,0,Math.sin(p.radians()/2),-Math.cos(p.radians()/2)})).put("contactSeparationsM",new JSONArray());var projection=Stopping063.projected(c,s);assertEquals(0,projection.getDouble("rotationDeg"),1e-5);assertEquals(1,projection.getDouble("tangentMps"),1e-12);assertTrue(projection.isNull("minimumSeparationM"));s.put("contactSeparationsM",new JSONArray(new double[]{.02,-.001,.04}));assertEquals(1,Stopping063.projected(c,s).getInt("touchPoints"));assertEquals(2,Stopping063.projected(c,s).getInt("nearPoints"));}
    @Test public void publishedOutcomes061And062RemainReadable()throws Exception{
        for(String name:List.of("../docs/readmes/061/original/final061/native-experiment.json.gz","../docs/readmes/062/original/native062-final/native-experiment062.json.gz"))try(var in=new java.util.zip.GZIPInputStream(Files.newInputStream(Path.of(name)))){var j=new JSONObject(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));if(j.has("outcomeModel061"))FraynixOutcome061.audit(j);else ResidualOutcome062.audit(j);}
    }
    @Test public void chatReportsStoppingWithoutStartingExperiment(){assertEquals("agent",ChatPolicy044.route("Inspect stopping study and its uncertainty",new JSONObject(),false));}
}

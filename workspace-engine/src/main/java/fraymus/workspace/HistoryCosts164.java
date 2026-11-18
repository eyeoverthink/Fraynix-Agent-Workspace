package fraymus.workspace;

import org.json.*;

/** Extra read-only measurement, not a replacement for the original history verifier. */
final class HistoryCosts164 {
    static JSONObject measure(SmallStore161 small,JSONObject input)throws Exception{
        SpatialScene.keys(input,"worldId","expectedRevision","expectedHash");
        synchronized(small){
            small.idle();var before=small.world(input.getString("worldId"));SmallStore161.expected(before,input);
            if(before.has("pending"))throw new IllegalStateException("Finish the pending native report first");
            int depth=before.getJSONArray("history").length();if(depth>8)throw new IllegalArgumentException("History measurement accepts at most eight revisions; original world limits are unchanged");
            var disk=small.load();if(!disk.similar(WorldFrames.copy(small.state)))throw new IllegalStateException("Persisted small-world evidence differs from memory");
            long begin=System.nanoTime();Runnable checkpoint=()->{if(System.nanoTime()-begin>120_000_000_000L)throw new IllegalStateException("History measurement deadline");};
            long start=System.nanoTime();var verified=small.verifyWorld(before.getString("id"));double firstMs=(System.nanoTime()-start)/1e6;checkpoint.run();
            if(!verified.similar(before))throw new IllegalStateException("History changed before measurement");
            double nativeMs=0,reportMs=0;long bytes=0,ticks=0;var rows=new JSONArray();
            for(Object raw:before.getJSONArray("history")){
                checkpoint.run();var entry=(JSONObject)raw;var j=small.read(entry.getString("jobId"));SmallStore161.engine(j);
                start=System.nanoTime();var replay=SmallBody161.execute(j.getJSONObject("before"),j.getJSONObject("plan"),small.nativeDir,checkpoint);double n=(System.nanoTime()-start)/1e6;
                if(!replay.similar(j.getJSONObject("run")))throw new IllegalStateException("Measured native replay differs from original");
                start=System.nanoTime();var a=small.artifacts.read(j.getJSONObject("report").getString("id"));var built=SmallReport161.build(j);
                if(a.getInt("version")!=1||!Creation.reference(a).similar(j.getJSONObject("report"))||!a.getJSONObject("contents").similar(built.getJSONObject("files")))throw new IllegalStateException("Measured original report changed");
                var audit=SmallReport161.audit(j,j.getJSONObject("audit").getJSONObject("observed"));if(!audit.similar(j.getJSONObject("audit")))throw new IllegalStateException("Measured original report outcome changed");
                double p=(System.nanoTime()-start)/1e6;long b=built.getJSONObject("files").toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                nativeMs+=n;reportMs+=p;bytes+=b;int count=j.getJSONObject("run").getJSONObject("verification").getInt("nativeTicks");ticks+=count;
                rows.put(new JSONObject().put("jobId",j.getString("id")).put("revision",j.getInt("committedRevision")).put("runSha256",WorldFrames.hash(replay)).put("nativeReplayMs",n).put("reportSourceAuditMs",p).put("encodedReportBytes",b).put("nativeTicks",count));
            }
            checkpoint.run();start=System.nanoTime();var after=small.verifyWorld(before.getString("id"));double lastMs=(System.nanoTime()-start)/1e6;checkpoint.run();
            if(!before.similar(after)||!disk.similar(small.load())||!disk.similar(WorldFrames.copy(small.state)))throw new IllegalStateException("History evidence changed during measurement");
            return new JSONObject().put("passed",true).put("worldId",before.getString("id")).put("revision",depth).put("worldHash",before.getString("hash")).put("nativeIdentity",SmallBody161.identity()).put("beforeOriginalCheckMs",firstMs).put("afterOriginalCheckMs",lastMs).put("nativeReplayMs",nativeMs).put("reportSourceAuditMs",reportMs).put("totalMs",(System.nanoTime()-begin)/1e6).put("encodedReportBytes",bytes).put("nativeTicks",ticks).put("rows",rows).put("scope","Extra read-only probe outside ordinary task timing. Both original full-history checks pass. Each native trace is separately rerun and compared; original report bytes and saved observed values are rechecked. This does not perform a new browser observation or authorize a world action.");
        }
    }
}

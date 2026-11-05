package fraymus.workspace;

import org.json.*;

/** Short, source-bound native contact history. No future sample enters this estimator. */
final class TemporalContact067 {
    static double clearance(JSONObject definition, JSONObject sample) {
        var config=RampDefinition.from(ResidualOutcome062.physical(definition));
        var p=SpatialScene.vector(sample.getJSONArray("position"),100);
        var q=sample.getJSONArray("rotation"); WorldVerify.quaternion(q);
        double norm=0; for(int i=0;i<4;i++)norm+=q.getDouble(i)*q.getDouble(i);
        norm=Math.sqrt(norm);
        double x=q.getDouble(0)/norm,y=q.getDouble(1)/norm,z=q.getDouble(2)/norm,w=q.getDouble(3)/norm;
        double nx=Math.sin(config.radians()),ny=Math.cos(config.radians());
        double rx=nx*(1-2*(y*y+z*z))+ny*2*(x*y+z*w);
        double ry=nx*2*(x*y-z*w)+ny*(1-2*(x*x+z*z));
        double rz=nx*2*(x*z+y*w)+ny*2*(y*z-x*w);
        return nx*p[0]+ny*(p[1]-config.centerY())-.1-.25*(Math.abs(rx)+Math.abs(ry)+Math.abs(rz));
    }
    static JSONObject state(JSONObject definition, JSONArray prefix) {
        if(prefix.length()!=16)throw new IllegalArgumentException("Exactly the tick-60 prefix is required");
        var evidence=new JSONArray(); boolean motion=true,geometry=true;int contactSamples=0;
        for(int i=13;i<=15;i++){
            var sample=prefix.getJSONObject(i);
            if(sample.getInt("tick")!=i*4)throw new IllegalArgumentException("Contact history clock differs");
            var p=Stopping063.projected(definition,sample);double gap=clearance(definition,sample);
            boolean safe=p.getDouble("rotationDeg")<=10&&Math.abs(p.getDouble("normalMps"))<=.1&&p.getDouble("angularSpeedRadps")<=2&&p.getDouble("tangentMps")>=0;
            boolean close=gap>=-.03&&gap<=.03;
            if(p.getInt("nearPoints")>0)contactSamples++;
            motion&=safe;geometry&=close;
            evidence.put(p.put("geometricClearanceM",gap).put("motionEligible",safe).put("geometryEligible",close).put("sampleSha256",WorldFrames.hash(sample)));
        }
        var state=RecoveryPolicy066.measured(definition,prefix.getJSONObject(15));
        return state.put("kind","measured-window").put("contactWindow",new JSONObject()
            .put("samples",evidence).put("contactSamples",contactSamples).put("motionEligible",motion)
            .put("geometryEligible",geometry).put("eligible",contactSamples>0&&motion&&geometry));
    }
    static JSONObject plan(JSONObject task,JSONObject state) {
        if(!state.getString("kind").equals("measured-window"))return RecoveryPolicy066.replan(task,state);
        // Only bypass the old instantaneous eligibility test; keep the real state and its counts in the returned evidence.
        var numerical=WorldFrames.copy(state).put("kind","predicted");
        var plan=RecoveryPolicy066.replan(task,numerical).put("state",WorldFrames.copy(state));
        if(!state.getJSONObject("contactWindow").getBoolean("eligible")){
            plan.put("correct",false).put("deltaVelocityMps",0).put("reason","Temporal contact or motion guard refused correction");
            plan.put("predictedEndpointM",state.getDouble("distanceM")+RecoveryPolicy066.travel(Math.max(0,state.getDouble("tangentMps")),RecoveryPolicy066.acceleration(task),.5));
        }
        return plan;
    }
    static JSONObject plannerState(JSONObject task,JSONObject decision,JSONObject definition,JSONArray samples) {
        String method=decision.getString("method");
        if(method.equals("temporal")||method.equals("value")){
            var prefix=new JSONArray();for(int i=0;i<16;i++)prefix.put(samples.get(i));
            return state(definition,prefix);
        }
        return method.equals("observed")?RecoveryPolicy066.measured(definition,samples.getJSONObject(15)):RecoveryPolicy066.predicted(task,decision.getDouble("speedMps"));
    }
}

package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Bounded geometric hypotheses. Only the native executor can produce measured motion. */
final class Waypoints079 {
    static final String VERSION="bound-world-clear-landing-079-v1";
    static final int MAX_CHECKS=512;
    private record Candidate(JSONObject target,double cost,int ordinal) {}
    private record Combination(int[] indices,double cost,String key) {}

    static JSONArray ideal(JSONObject spec,String family,int step) {
        int count=Procedure077.stages(family);
        if(step<0||step>=count)throw new IllegalArgumentException("Invalid waypoint stage");
        var original=Procedure077.scene(spec);var targets=new JSONArray();double fraction=(step+1d)/count;
        for(Object raw:spec.getJSONArray("targets")) {
            var end=(JSONObject)raw;var body=WorldPlan.sourceObject(original,end.getString("sourceId"));var t=WorldFrames.copy(end);
            if(step+1<count) {
                var p=new JSONArray();for(int k=0;k<3;k++)p.put(body.getJSONArray("position").getDouble(k)+(end.getJSONArray("positionM").getDouble(k)-body.getJSONArray("position").getDouble(k))*fraction);
                double yaw=WorldPlan.yaw(body.getJSONArray("rotation"));t.put("positionM",p).put("yawDeg",Procedure077.wrap(yaw+Procedure077.yawDelta(yaw,end.getDouble("yawDeg"))*fraction));
            }
            targets.put(t);
        }
        return targets;
    }

    static boolean landing(JSONObject scene,JSONObject target) {
        var o=WorldPlan.sourceObject(scene,target.getString("sourceId"));var floor=WorldPlan.sourceObject(scene,"floor");
        var p=SpatialScene.vector(target.getJSONArray("positionM"),5);double radius=WorldPlan.radius(o);
        for(int k:new int[]{0,2})if(Math.abs(p[k]-floor.getJSONArray("position").getDouble(k))+radius>floor.getJSONArray("size").getDouble(k)/2-.05)return false;
        return WorldPlan.clear(scene,o.getString("sourceId"),Map.of(),p,p,radius);
    }

    private static List<Candidate> candidates(JSONObject scene,JSONObject ideal,boolean search) {
        var list=new ArrayList<Candidate>();int ordinal=0;
        for(int ring=0;ring<=(search?2:0);ring++)for(int direction=0;direction<(ring==0?1:8);direction++) {
            var t=WorldFrames.copy(ideal);double angle=direction*Math.PI/4;
            var p=t.getJSONArray("positionM");p.put(0,p.getDouble(0)+ring*Math.cos(angle));p.put(2,p.getDouble(2)+ring*Math.sin(angle));
            boolean inBounds=true;for(int k:new int[]{0,2})inBounds&=Math.abs(p.getDouble(k))<=5;
            if(inBounds&&(!search||landing(scene,t)))list.add(new Candidate(t,ring*ring,ordinal));ordinal++;
        }
        list.sort(Comparator.comparingDouble(Candidate::cost).thenComparingInt(Candidate::ordinal));return list;
    }

    private static Combination combination(int[] indices,List<List<Candidate>> choices) {
        double cost=0;for(int i=0;i<indices.length;i++)cost+=choices.get(i).get(indices[i]).cost();
        return new Combination(indices,cost,Arrays.toString(indices));
    }

    static JSONObject choose(JSONObject spec,String family,int step,JSONObject startFrame,JSONObject frame) {
        WorldFrames.verify(startFrame);var original=WorldFrames.scene(startFrame);WorldFrames.verify(frame);
        if(!WorldFrames.frame(original).getJSONObject("originalScene").similar(frame.getJSONObject("originalScene")))throw new IllegalStateException("Waypoint original source changed");
        var scene=WorldFrames.scene(frame);var ideals=ideal(spec,family,step);
        boolean search=family.startsWith("clear-")&&step+1<Procedure077.stages(family);
        var choices=new ArrayList<List<Candidate>>();for(Object raw:ideals)choices.add(candidates(scene,(JSONObject)raw,search));
        if(choices.stream().anyMatch(List::isEmpty))throw new IllegalArgumentException("No clear intermediate landing candidate within the declared search");
        var queue=new PriorityQueue<Combination>(Comparator.comparingDouble(Combination::cost).thenComparing(Combination::key));
        var start=combination(new int[choices.size()],choices);queue.add(start);var seen=new HashSet<String>();seen.add(start.key());
        int checks=0;JSONObject last=null;
        while(!queue.isEmpty()&&checks<MAX_CHECKS) {
            var item=queue.remove();var targets=new JSONArray();for(int k=0;k<choices.size();k++)targets.put(choices.get(k).get(item.indices()[k]).target());
            var plan=WorldPlan.build(scene,WorldPlan.intent(scene,targets,spec.getDouble("forceLimitN"),spec.getDouble("torqueLimitNm")));checks++;last=plan;
            if(plan.getBoolean("passed"))return new JSONObject().put("version",VERSION).put("geometricOnly",true).put("sourceFrameSha256",WorldFrames.hash(frame)).put("candidateChecks",checks).put("deviationSquaredM2",item.cost()).put("idealTargets",ideals).put("targets",targets).put("plan",plan);
            for(int k=0;k<choices.size();k++)if(item.indices()[k]+1<choices.get(k).size()) {
                var next=item.indices().clone();next[k]++;var c=combination(next,choices);if(seen.add(c.key()))queue.add(c);
            }
        }
        // A refusal still carries the unchanged native planner's reason. No pose is written.
        if(last!=null)return new JSONObject().put("version",VERSION).put("geometricOnly",true).put("sourceFrameSha256",WorldFrames.hash(frame)).put("candidateChecks",checks).put("deviationSquaredM2",0).put("idealTargets",ideals).put("targets",last.getJSONObject("intent").getJSONArray("targets")).put("plan",last);
        throw new IllegalArgumentException("Waypoint search exhausted");
    }

    static double blockedFraction(JSONObject spec) {
        var scene=Procedure077.scene(spec);int blocked=0,total=0;
        for(String family:List.of("two-stage","three-stage"))for(int i=0;i<Procedure077.stages(family)-1;i++)for(Object t:ideal(spec,family,i)){total++;if(!landing(scene,(JSONObject)t))blocked++;}
        return blocked/(double)Math.max(1,total);
    }

    static String geometric(JSONObject spec) {
        String best="direct";int bestSteps=Integer.MAX_VALUE;double bestDeviation=Double.POSITIVE_INFINITY;
        for(String family:Procedure077.FAMILIES) {
            int n=Procedure077.stages(family);var frame=WorldFrames.copy(WorldFrames.frame(Procedure077.scene(spec)));double deviation=0;boolean clear=true;
            for(int i=0;i<n&&clear;i++)try {
                var choice=choose(spec,family,i,WorldFrames.frame(Procedure077.scene(spec)),frame);var plan=choice.getJSONObject("plan");clear=plan.getBoolean("passed");deviation+=choice.getDouble("deviationSquaredM2");
                for(Object raw:plan.getJSONObject("intent").getJSONArray("targets")) {
                    var t=(JSONObject)raw;var o=WorldPlan.sourceObject(frame.getJSONObject("originalScene"),t.getString("sourceId"));var body=ContactPhysics.dynamic(frame.getJSONArray("bodies"),o.getString("id"));
                    clear&=WorldVerify.distance(body.getJSONArray("position"),t.getJSONArray("positionM"))<=4&&Math.abs(Procedure077.yawDelta(WorldPlan.yaw(body.getJSONArray("rotation")),t.getDouble("yawDeg")))<=90;
                    // Local planning hypothesis only; never returned to execution or saved as measured state.
                    body.put("position",new JSONArray(t.getJSONArray("positionM").toString())).put("rotation",WorldPlan.quat(t.getDouble("yawDeg")));
                }
            }catch(IllegalArgumentException refused){clear=false;}
            if(clear&&(n<bestSteps||n==bestSteps&&deviation<bestDeviation)){best=family;bestSteps=n;bestDeviation=deviation;}
        }
        return best;
    }
}

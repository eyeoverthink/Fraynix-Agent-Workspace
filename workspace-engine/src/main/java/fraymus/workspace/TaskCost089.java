package fraymus.workspace;

import java.util.*;
import org.json.*;

/** Paired empirical cost uncertainty; original verification still authorizes every outcome. */
final class TaskCost089 {
    static final String VERSION="paired-complete-task-cost-089-v1";
    static final double MAX_GAP_MILLIS=30_000;

    static String selector(String test,String requested) {
        if(requested==null||requested.isBlank())return "paired089";
        if(!"1".equals(test)||!"reference085".equals(requested))
            throw new IllegalArgumentException("Reference cost selection requires an isolated test runtime");
        return requested;
    }

    static JSONObject clock(String owner,long start,long end) {
        return new JSONObject().put("owner",owner).put("start",Long.toString(start)).put("end",Long.toString(end));
    }

    private static long[] span(JSONObject cost) {
        var c=cost.getJSONObject("clock089");SpatialScene.keys(c,"owner","start","end");
        String owner=c.getString("owner");
        if(!UUID.fromString(owner).toString().equals(owner))throw new IllegalArgumentException("Canonical timing owner required");
        long a=Long.parseLong(c.getString("start")),b=Long.parseLong(c.getString("end"));
        if(!Long.toString(a).equals(c.getString("start"))||!Long.toString(b).equals(c.getString("end")))
            throw new IllegalArgumentException("Canonical monotonic interval required");
        long nanos=Math.subtractExact(b,a);double millis=cost.getDouble("totalMillis");
        if(nanos<=0||!Double.isFinite(millis)||Math.abs(nanos/1e6-millis)>1e-6)
            throw new IllegalArgumentException("Owned timing interval differs from complete task duration");
        return new long[]{a,b};
    }

    static JSONObject pairing(JSONObject simple,JSONObject memory) {
        var out=new JSONObject().put("eligible",false);
        if(!simple.has("clock089")||!memory.has("clock089"))return out.put("reason","Owned monotonic intervals are unavailable");
        long[] a=span(simple),b=span(memory);
        var ca=simple.getJSONObject("clock089");var cb=memory.getJSONObject("clock089");
        out.put("simple",WorldFrames.copy(ca)).put("memory",WorldFrames.copy(cb));
        if(!ca.getString("owner").equals(cb.getString("owner")))return out.put("reason","Comparisons belong to different Java owners");
        if(a[1]>b[0]&&b[1]>a[0])return out.put("reason","Measurement intervals overlap");
        boolean first=a[1]<=b[0];double gap=Math.subtractExact(first?b[0]:a[0],first?a[1]:b[1])/1e6;
        return out.put("first",first?"simple":"memory").put("gapMillis",gap).put("eligible",gap<=MAX_GAP_MILLIS)
            .put("reason",gap<=MAX_GAP_MILLIS?"Adjacent non-overlapping comparisons from the same owner":"Comparison gap exceeds 30 seconds");
    }

    static JSONObject pair(JSONObject simple,JSONObject memory,JSONObject originalMemoryJob) {
        return TaskCost085.pair(simple,memory,originalMemoryJob).put("pairing089",pairing(simple,memory));
    }

    private static boolean paired(JSONObject p) {
        var q=p.optJSONObject("pairing089");if(q==null||!q.optBoolean("eligible"))return false;
        var s=new JSONObject().put("clock089",q.getJSONObject("simple")).put("totalMillis",p.getDouble("simpleMillis"));
        var m=new JSONObject().put("clock089",q.getJSONObject("memory")).put("totalMillis",p.getDouble("memoryMillis"));
        if(!pairing(s,m).similar(q))throw new IllegalArgumentException("Pairing metadata differs from owned intervals");
        return true;
    }

    static JSONObject estimate(JSONObject candidate,JSONObject spec,JSONObject workload,JSONObject model,int bankEntries,boolean reference) {
        if(model.isEmpty()||!VERSION.equals(model.optString("version")))
            return new JSONObject().put("available",false).put("method",VERSION).put("reason","Current paired-cost calibration required");
        var compatible=WorldFrames.copy(model).put("version",TaskCost085.VERSION);
        var e=TaskCost085.estimate(candidate,spec,workload,compatible,bankEntries)
            .put("method",VERSION).put("modelSha256",WorldFrames.hash(model)).put("estimator089",reference?"reference085":"unpaired-fallback");
        if(!e.optBoolean("available"))return e;
        e.put("referenceMarginMillis",e.getDouble("marginMillis")).put("referenceLowerSavingMillis",e.getDouble("lowerSavingMillis"));
        if(reference)return e.put("pairingReason089","Isolated test reference: unchanged 085 uncertainty");
        var groups=new LinkedHashMap<String,List<JSONObject>>();
        for(Object raw:e.getJSONArray("neighbors")) {
            var p=((JSONObject)raw).getJSONObject("pair");
            if(!paired(p))return e.put("pairingReason089","Unpaired fallback: nearby same-owner intervals are required for every comparison");
            groups.computeIfAbsent(p.getString("taskSha256"),k->new ArrayList<>()).add(p);
        }
        double within=500,order=0;
        for(var g:groups.values()) {
            double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY,forward=0,reverse=0;int nf=0,nr=0;
            for(var p:g) {
                double saving=p.getDouble("simpleMillis")-p.getDouble("memoryMillis");min=Math.min(min,saving);max=Math.max(max,saving);
                if(p.getJSONObject("pairing089").getString("first").equals("simple")){forward+=saving;nf++;}else{reverse+=saving;nr++;}
            }
            if(nf==0||nr==0)return e.put("pairingReason089","Unpaired fallback: both execution orders are required within each task");
            within=Math.max(within,max-min);order=Math.max(order,Math.abs(forward/nf-reverse/nr));
        }
        double repeat=within+order+e.getDouble("betweenTaskMillis")+e.getDouble("distanceMillis"),margin=repeat+e.getDouble("bankGrowthMarginMillis"),saving=e.getDouble("savingMillis");
        e.put("estimator089","paired089").put("pairedRangeMillis",within).put("orderMarginMillis",order)
            .put("referenceWithinMillis",e.getDouble("withinTaskMillis")).put("withinTaskMillis",within+order)
            .put("repeatMarginMillis",repeat).put("marginMillis",margin).put("lowerSavingMillis",saving-margin).put("upperSavingMillis",saving+margin)
            .put("pairingReason089","Paired savings with execution-order reserve; empirical, not a probability guarantee")
            .put("reason","Nearby same-owner paired savings; order, scene and bank uncertainty retained");
        return e.put("payback085",TaskCost085.payback(model,e));
    }

    static JSONObject choose(JSONObject spec,String mode,JSONArray candidates,JSONObject workload,JSONObject model,String receipt,int bankEntries,String selector) {
        if(!Set.of("paired089","reference085").contains(selector))throw new IllegalArgumentException("Unknown cost selector");
        var d=TaskCost085.choose(spec,mode.equals("calibrated")?"memory":mode,candidates,workload,new JSONObject(),receipt,bankEntries);
        if(!mode.equals("calibrated"))return d;
        var c=d.optJSONObject("candidate");var e=c==null?new JSONObject().put("available",false).put("reason","No compatible verified memory")
            :estimate(c,spec,workload,model,bankEntries,selector.equals("reference085"));
        boolean use=e.optBoolean("available")&&e.getDouble("lowerSavingMillis")>0;
        String policy=e.optString("estimator089","unavailable");
        return d.put("mode","calibrated").put("useMemory",use).put("preferred",use?c.getString("family"):"direct").put("cost080",e)
            .put("reason",policy+": "+(use?"Measured savings clear the retained uncertainty reserve":e.optBoolean("available")?"No supported positive saving; simple control":e.getString("reason")));
    }
}

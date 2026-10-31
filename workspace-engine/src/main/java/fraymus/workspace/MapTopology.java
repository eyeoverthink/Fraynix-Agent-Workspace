package fraymus.workspace;

import fraymus.geometry.*;
import org.json.*;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/** Versioned global contract, explicit search evidence and independent stored-grid audit. */
final class MapTopology {
    static TopologyMap.Constraints constraints(JSONObject o){
        SpatialScene.keys(o,"connected","minFloorFraction","maxFloorFraction","minRouteSteps","maxRouteSteps");
        if(o.has("connected")&&!(o.get("connected") instanceof Boolean))throw new IllegalArgumentException("connected must be a boolean");
        for(String key:List.of("minFloorFraction","maxFloorFraction","minRouteSteps","maxRouteSteps"))if(o.has(key)&&!(o.get(key) instanceof Number))throw new IllegalArgumentException(key+" must be numeric");
        return new TopologyMap.Constraints(o.optBoolean("connected",true),o.optDouble("minFloorFraction",.3),o.optDouble("maxFloorFraction",.75),Procedural.integer(o,"minRouteSteps",0,0,128),Procedural.integer(o,"maxRouteSteps",64,0,128));
    }
    static JSONObject json(TopologyMap.Constraints c){return new JSONObject().put("connected",c.connected()).put("minFloorFraction",c.minFloorFraction()).put("maxFloorFraction",c.maxFloorFraction()).put("minRouteSteps",c.minRouteSteps()).put("maxRouteSteps",c.maxRouteSteps());}
    static JSONObject json(TopologyMap.Metrics m){return new JSONObject().put("floorCells",m.floorCells()).put("reachableCells",m.reachableCells()).put("components",m.components()).put("floorFraction",m.floorFraction()).put("shortestSteps",m.shortestSteps()).put("edges",m.edges()).put("cycleRank",m.cycleRank()).put("deadEnds",m.deadEnds()).put("adjacencyViolations",m.adjacencyViolations()).put("pinViolations",m.pinViolations()).put("routeValid",m.routeValid());}
    static JSONObject json(TopologyMap.Audit a){return new JSONObject().put("passed",a.passed()).put("metrics",json(a.metrics())).put("failures",new JSONArray(a.failures()));}
    static JSONObject evidence(TopologyMap.Result result,TopologyMap.Constraints c){
        var trials=new JSONArray();int selected=0;for(var t:result.trials()){if(t.candidateSeed()==result.candidate().seed())selected=t.index();trials.put(new JSONObject().put("index",t.index()).put("candidateSeed",t.candidateSeed()).put("localPassed",t.localPassed()).put("localAttempts",t.localAttempts()).put("localRepairs",t.localRepairs()).put("audit",json(t.audit())));}
        return new JSONObject().put("profile",TopologyMap.PROFILE).put("kernelSha256",identity()).put("constraints",json(c)).put("audit",json(result.audit())).put("selectedCandidate",selected).put("candidateSeed",result.candidate().seed()).put("candidateCount",trials.length()).put("totalRepairs",result.totalRepairs()).put("budget",new JSONObject().put("maxCandidates",TopologyMap.CANDIDATES).put("localRestarts",TopologyMap.RESTARTS).put("repairsPerRestart",TopologyMap.REPAIRS)).put("trials",trials);
    }
    static String identity(){try{var digest=MessageDigest.getInstance("SHA-256");for(String name:List.of("TopologyMap","TopologyMap$Constraints","TopologyMap$Metrics","TopologyMap$Audit","TopologyMap$Trial","TopologyMap$Result","ReferenceMap","ReferenceMap$Pin","ReferenceMap$Rules","ReferenceMap$Result")){try(var in=TopologyMap.class.getResourceAsStream(name+".class")){if(in==null)throw new IllegalStateException("Missing topology kernel "+name);digest.update(name.getBytes(StandardCharsets.UTF_8));digest.update(in.readAllBytes());}}try(var own=MapTopology.class.getResourceAsStream("MapTopology.class")){digest.update(own.readAllBytes());}return HexFormat.of().formatHex(digest.digest());}catch(Exception e){throw new IllegalStateException("Topology implementation identity unavailable",e);}}
    static JSONObject verify(JSONObject map){
        if(!map.has("topology"))return new JSONObject().put("requested",false);
        var t=map.getJSONObject("topology");if(!t.getString("profile").equals(TopologyMap.PROFILE)||!t.getString("kernelSha256").equals(identity()))throw new IllegalStateException("Saved global topology implementation differs");
        if(!map.getJSONObject("source").getString("kind").equals("reference-image"))throw new IllegalStateException("Global reference contract cannot be attached to an unrelated source");
        var c=constraints(map.getJSONObject("options").getJSONObject("constraints"));if(!json(c).similar(t.getJSONObject("constraints")))throw new IllegalStateException("Original requested topology constraints changed");
        var cells=map.getJSONArray("cells").toList().stream().mapToInt(x->((Number)x).intValue()).toArray();var pins=new ArrayList<ReferenceMap.Pin>();for(Object raw:map.getJSONArray("pins")){var p=(JSONObject)raw;pins.add(new ReferenceMap.Pin(p.getInt("x"),p.getInt("y"),p.getInt("tile")));}
        for(Object raw:map.getJSONObject("options").optJSONArray("pins",new JSONArray())){var p=(JSONObject)raw;pins.add(new ReferenceMap.Pin(p.getInt("x"),p.getInt("y"),p.getInt("tile")));}
        var path=map.getJSONArray("path").toList().stream().map(x->((Number)x).intValue()).toList();var r=new ReferenceMap.Result(map.getInt("width"),map.getInt("height"),t.getLong("candidateSeed"),cells,pins,path,0,0,0,true,"");
        var audit=TopologyMap.audit(MapDefinition.rules(map.getJSONObject("source").getJSONObject("reference")),r,c);var actual=json(audit);
        if(!actual.similar(t.getJSONObject("audit"))||audit.passed()!=map.getJSONObject("generation").getBoolean("passed"))throw new IllegalStateException("Stored global topology claims differ from actual grid");
        return actual.put("requested",true).put("profile",TopologyMap.PROFILE);
    }
}

package fraymus.workspace;

import org.json.*;
import java.net.URI;
import java.util.*;

/** User-frozen browser criteria, compared with trusted-host observations, never model success flags. */
final class BrowserOutcome126 {
    static final String KEY="browserContract126", PROOF="browserOutcome126";
    private BrowserOutcome126() {}

    static JSONObject contract(JSONObject input) {
        SpatialScene.keys(input,"url","checks");
        String url=input.getString("url");
        URI uri=URI.create(url);
        if(url.length()>2048||!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)
            throw new IllegalArgumentException("Browser check needs an exact HTTP(S) page URL without credentials");
        var checks=input.getJSONArray("checks");
        if(checks.isEmpty()||checks.length()>8)throw new IllegalArgumentException("Choose1..8 visible browser results to check");
        var selectors=new HashSet<String>();
        for(Object raw:checks){
            var row=(JSONObject)raw;SpatialScene.keys(row,"selector","text");
            String selector=row.getString("selector"),text=row.getString("text");
            if(selector.isBlank()||selector.length()>200||!selectors.add(selector)||text.length()>500)
                throw new IllegalArgumentException("Use distinct bounded selectors and expected text up to500 characters");
        }
        return WorldFrames.copy(input);
    }

    static boolean compare(JSONObject contract,JSONObject result,String taskId,int generation,String effectId) {
        contract(contract);
        SpatialScene.keys(result,"schema","taskId","generation","effectId","contractSha256","url","observedAt","checks");
        if(!result.getString("schema").equals("browser-outcome-126-v1")||!result.getString("taskId").equals(taskId)
            ||result.getInt("generation")!=generation||!result.getString("effectId").equals(effectId)
            ||!result.getString("contractSha256").equals(WorldFrames.hash(contract)))
            throw new IllegalStateException("Browser result belongs to another task, attempt or frozen contract");
        java.time.Instant.parse(result.getString("observedAt"));
        var wanted=contract.getJSONArray("checks");var actual=result.getJSONArray("checks");
        if(actual.length()!=wanted.length())throw new IllegalStateException("Browser result omitted required checks");
        boolean passed=result.getString("url").equals(contract.getString("url"));
        for(int i=0;i<wanted.length();i++){
            var a=actual.getJSONObject(i);var w=wanted.getJSONObject(i);
            SpatialScene.keys(a,"selector","count","text","visible");
            if(!a.getString("selector").equals(w.getString("selector")))throw new IllegalStateException("Browser check order or selector changed");
            int count=a.getInt("count");if(count<0||a.getDouble("count")!=count||a.getString("text").length()>501)throw new IllegalStateException("Unbounded browser observation");
            passed &= count==1&&a.getBoolean("visible")&&a.getString("text").equals(w.getString("text"));
        }
        return passed;
    }

    static JSONObject evidence(JSONObject task) {
        ChatChain052.verifyObjective(task);
        var proof=task.getJSONObject(PROOF);var receipt=proof.getJSONObject("receipt");
        SpatialScene.keys(proof,"receipt","beforeId","afterId");
        if(!compare(task.getJSONObject(KEY),receipt,task.getString("id"),task.getInt("generation"),receipt.getString("effectId")))
            throw new IllegalStateException("Original browser outcome did not satisfy the frozen checks");
        UUID.fromString(proof.getString("beforeId"));UUID.fromString(proof.getString("afterId"));
        if(!task.getJSONArray("frames").toList().containsAll(List.of(proof.getString("beforeId"),proof.getString("afterId"))))throw new IllegalStateException("Browser result lost its original screen references");
        boolean witnessed=false;String hash=WorldFrames.hash(receipt);
        for(Object raw:task.getJSONArray("history")){
            var row=(JSONObject)raw;var d=row.getJSONObject("detail");
            if(row.getString("kind").equals("browser-outcome126")&&d.optString("resultSha256").equals(hash)
                &&d.optString("beforeId").equals(proof.getString("beforeId"))&&d.optString("afterId").equals(proof.getString("afterId")))witnessed=true;
        }
        if(!witnessed)throw new IllegalStateException("Original browser observation is not in the task journal");
        return WorldFrames.copy(proof);
    }
}

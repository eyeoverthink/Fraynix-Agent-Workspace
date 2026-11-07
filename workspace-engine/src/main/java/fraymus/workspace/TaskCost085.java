package fraymus.workspace;

import org.json.*;
import java.util.*;

/** Exact procedure choice and empirical growth sensitivity; never movement authority. */
final class TaskCost085 {
    static final String VERSION="growing-bank-complete-task-cost-085-v1";

    static void validateSelection(String mode,String receipt) {
        if(receipt.isEmpty())return;
        if(!UUID.fromString(receipt).toString().equals(receipt))throw new IllegalArgumentException("Canonical procedure receipt required");
        if(!Set.of("memory","calibrated").contains(mode))throw new IllegalArgumentException("Select a receipt only with memory or calibrated control");
    }

    static JSONObject pair(JSONObject simple,JSONObject memory,JSONObject memoryJob) {
        var p=TaskCost080.pair(simple,memory);
        var bank=memoryJob.getJSONArray("bankSnapshot");
        if(bank.isEmpty()||bank.length()>32)throw new IllegalArgumentException("Measured memory bank required");
        boolean found=false;
        for(Object raw:bank)if(((JSONObject)raw).getString("id").equals(p.getString("receiptId")))found=true;
        if(!found)throw new IllegalArgumentException("Measured receipt absent from original bank");
        if(!memory.getString("jobSha256").equals(WorldFrames.hash(memoryJob)))throw new IllegalArgumentException("Bank count needs original measured job");
        return p.put("memoryBankEntries",bank.length());
    }

    static JSONObject setup(JSONArray pairs,double fitMillis) {
        if(!Double.isFinite(fitMillis)||fitMillis<0)throw new IllegalArgumentException("Finite nonnegative fitting duration required");
        double tasks=0;
        for(Object raw:pairs){var p=(JSONObject)raw;tasks+=p.getDouble("simpleMillis")+p.getDouble("memoryMillis");}
        return new JSONObject().put("calibrationTaskMillis",tasks).put("fitMillis",fitMillis).put("totalMillis",tasks+fitMillis)
            .put("scope","Verified comparison tasks plus measured fitting; excludes learning, launch, copying and future model checks");
    }

    static void verifySetup(JSONObject model) {
        var s=model.getJSONObject("setup085");
        if(!setup(model.getJSONArray("pairs"),s.getDouble("fitMillis")).similar(s))throw new IllegalStateException("Calibration setup arithmetic changed");
    }

    static JSONObject estimate(JSONObject candidate,JSONObject spec,JSONObject workload,JSONObject model,int bankEntries) {
        var unavailable=new JSONObject().put("available",false).put("method",VERSION).put("reason","No matching growing-bank calibration");
        if(model.isEmpty())return unavailable;
        if(!VERSION.equals(model.optString("version")))return unavailable.put("reason","Calibration implementation differs; collect current comparisons");
        if(bankEntries<1||bankEntries>32)return unavailable.put("reason","Bank size is outside the bounded procedure history");
        verifySetup(model);
        var compatible=WorldFrames.copy(model).put("version",TaskCost084.VERSION);
        var e=TaskCost084.estimate(candidate,spec,workload,compatible).put("method",VERSION).put("modelSha256",WorldFrames.hash(model));
        if(!e.getBoolean("available"))return e;
        var ns=e.getJSONArray("neighbors");int min=32,max=0;double sensitivity=25;
        for(Object raw:ns){var p=((JSONObject)raw).getJSONObject("pair");int n=p.getInt("memoryBankEntries");
            if(n<1||n>32)throw new IllegalArgumentException("Invalid original bank size");min=Math.min(min,n);max=Math.max(max,n);
            for(Object other:ns){var q=((JSONObject)other).getJSONObject("pair");int d=Math.abs(n-q.getInt("memoryBankEntries"));
                if(d>0)sensitivity=Math.max(sensitivity,Math.abs(p.getDouble("memoryMillis")-q.getDouble("memoryMillis"))/d);
            }
        }
        double growth=Math.max(0,bankEntries-max)*sensitivity,margin=e.getDouble("marginMillis")+growth,saving=e.getDouble("savingMillis");
        e.put("repeatMarginMillis",e.getDouble("marginMillis")).put("bankEntries",bankEntries).put("calibrationBankMin",min).put("calibrationBankMax",max)
            .put("bankSensitivityMillisPerEntry",sensitivity).put("bankGrowthMarginMillis",growth).put("marginMillis",margin)
            .put("lowerSavingMillis",saving-margin).put("upperSavingMillis",saving+margin)
            .put("reason","Repeated complete tasks plus empirical bank-growth sensitivity; no probability or causal growth guarantee");
        return e.put("payback085",payback(model,e));
    }

    static JSONObject payback(JSONObject model,JSONObject estimate) {
        verifySetup(model);double setup=model.getJSONObject("setup085").getDouble("totalMillis"),lower=estimate.getDouble("lowerSavingMillis");
        var p=new JSONObject().put("setupMillis",setup).put("lowerSavingMillis",lower).put("available",lower>0)
            .put("scope","Conditional forecast only; future model checks and learning/launch costs still need measurement");
        return lower>0?p.put("tasks",Math.ceil(setup/lower)).put("reason","Positive lower saving bound; verify repayment on a complete sequence")
            :p.put("reason","No positive lower saving bound; payback is not established");
    }

    static JSONObject choose(JSONObject spec,String mode,JSONArray candidates,JSONObject workload,JSONObject model,String receipt,int bankEntries) {
        validateSelection(mode,receipt);JSONArray selected=candidates;
        if(!receipt.isEmpty()){
            selected=new JSONArray();
            for(Object raw:candidates){var c=(JSONObject)raw;if(c.getString("id").equals(receipt))selected.put(c);}
            if(selected.length()!=1||selected.getJSONObject(0).getDouble("distance")>.10)
                throw new IllegalArgumentException("Selected procedure is absent, ambiguous or too distant for this task");
        }
        var d=Procedure079.choose(spec,mode.equals("calibrated")?"memory":mode,selected).put("candidates",candidates);
        if(!receipt.isEmpty())d.put("requestedReceipt085",receipt);
        if(!mode.equals("calibrated"))return d;
        var c=d.optJSONObject("candidate");var e=c==null?new JSONObject().put("available",false).put("reason","No compatible verified memory")
            :estimate(c,spec,workload,model,bankEntries);
        boolean use=e.optBoolean("available")&&e.getDouble("lowerSavingMillis")>0;
        return d.put("mode","calibrated").put("useMemory",use).put("preferred",use?c.getString("family"):"direct").put("cost080",e)
            .put("reason",use?"Repeated savings exceed variability and bank-growth margin":e.optBoolean("available")?"No supported positive savings after bank-growth uncertainty; simple control":e.getString("reason")+"; simple control");
    }
}

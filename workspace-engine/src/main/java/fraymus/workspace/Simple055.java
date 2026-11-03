package fraymus.workspace;
import org.json.*;

/** Stop expansion only after the actual direct candidate passes every original gate. */
final class Simple055 {
    private Simple055() {}
    static JSONObject policy() {
        return new JSONObject().put("version","direct-first-055-v1")
            .put("first","maximum-force").put("fallback","balanced-force then quarter-force")
            .put("acceptance","Original native pose/wrench/release AND construction support/stability checks")
            .put("execution","One passing forecast still requires a fresh verified continuing-world execution")
            .put("scope","Avoid unneeded comparison trials; no claim of global optimality or learned policy");
    }
    static boolean firstPassed(JSONObject study) {
        var c=study.getJSONArray("candidates").getJSONObject(0);
        return c.getString("id").equals("maximum-force")&&c.getString("status").equals("completed")
            &&c.optJSONObject("metrics",new JSONObject()).optBoolean("passed")
            &&c.optJSONObject("structure053",new JSONObject()).optBoolean("passed");
    }
    static JSONObject choose(JSONObject study) {
        return firstPassed(study)?study.getJSONArray("candidates").getJSONObject(0):Design053.choose(study.getJSONArray("candidates"));
    }
}

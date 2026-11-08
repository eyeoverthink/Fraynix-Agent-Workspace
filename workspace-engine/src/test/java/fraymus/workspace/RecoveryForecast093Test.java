package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RecoveryForecast093Test {
    @Test public void replacementReservesItsOwnWorkAndAllRemainingStages(){
        assertEquals(2,RecoveryForecast093.reserve(2,1));
        assertEquals(4,RecoveryForecast093.reserve(3,1));
        RecoveryForecast093.admit(86400,2,1,1);
        assertThrows(IllegalStateException.class,()->RecoveryForecast093.admit(86401,2,1,1));
        assertThrows(IllegalStateException.class,()->RecoveryForecast093.admit(0,2,1,1200000));
        for(int step:new int[]{-1,0,2})assertThrows(IllegalStateException.class,()->RecoveryForecast093.reserve(2,step));
    }
    @Test public void exactBindingCannotBeChangedOrExtended(){
        var expected=new JSONObject().put("format",RecoveryForecast093.FORMAT).put("frameSha256","a").put("step",1);
        var saved=WorldFrames.copy(expected).put("forecastId","owned");RecoveryForecast093.bindingMatches(saved,expected);
        for(String field:new String[]{"frameSha256","step","extra"}){
            var changed=WorldFrames.copy(saved).put(field,"changed");
            assertThrows(IllegalStateException.class,()->RecoveryForecast093.bindingMatches(changed,expected));
        }
    }
    @Test public void completedPrerequisiteMustPrecedeTheAction(){
        var done=new JSONObject().put("status","completed").put("settledAt","2026-11-08T10:00:00Z");
        var next=new JSONObject().put("plannedAt","2026-11-08T10:00:01Z");RecoveryForecast093.before(done,next);
        assertThrows(IllegalStateException.class,()->RecoveryForecast093.before(done,WorldFrames.copy(next).put("plannedAt","2026-11-08T09:59:59Z")));
        assertThrows(IllegalStateException.class,()->RecoveryForecast093.before(WorldFrames.copy(done).put("status","failed"),next));
    }
    @Test public void ordinaryClockOrClaimedResumeDoesNotSupplyAnAncestor(){
        var run=new JSONObject().put("id","original");
        assertThrows(IllegalStateException.class,()->RecoveryForecast093.binding(1,run,run,run,new JSONObject(),new JSONObject()));
        assertThrows(IllegalStateException.class,()->RecoveryForecast093.binding(1,run,run,WorldFrames.copy(run).put("resumes",3),new JSONObject(),new JSONObject()));
    }
}

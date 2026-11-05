package fraymus.workspace;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.file.*;
import java.util.*;

public class Payload058PolicyTest {
    @Test public void explicitRedesignAndUnfamiliarPayloadRequestPreservesRequirements()throws Exception {
        try(var store=new WorkspaceStore(Files.createTempDirectory("payload058-chat-"))){
            var task=Domain044Test.task("construction").put("goal","Redesign from measured failures, then test unfamiliar payloads; do not redesign");task.getJSONObject("context044").put("designRequested053",true);
            var args=Domain044Test.domain(store).prepare(task,"construction.start",new JSONObject().put("payloadDesign058",false));
            assertTrue(args.getBoolean("payloadDesign058"));assertTrue(args.getBoolean("design053"));assertTrue(args.getBoolean("directFirst055"));assertFalse(args.getBoolean("allowPayloadRedesign058"));
            ToolSchema043.validate(Tools044.schema("construction.start",task),args);
            var schema=Tools044.schema("construction.start",task);for(String key:List.of("payloadTests057","loadTests055","loadDesign056","requiredLoadN"))assertFalse(schema.getJSONObject("properties").has(key));
            assertThrows(IllegalArgumentException.class,()->ToolSchema043.validate(schema,new JSONObject().put("payloadDesign058","yes")));
        }
    }
    @Test public void conflictingProtocolsAreRejectedBeforeAnyNativeWork()throws Exception {
        Path root=Files.createTempDirectory("payload058-admission-");try(var artifacts=new WorkspaceStore(root);var scenes=new SceneStore(root);var brain=new BrainService(root);var goals=new GoalRunner(root,scenes,brain,artifacts)){
            synchronized(goals){var base=new JSONObject().put("payloadDesign058",true).put("design053",true).put("directFirst055",true);
                for(String key:List.of("loadTests055","payloadTests057","loadDesign056")){var args=WorldFrames.copy(base).put(key,true);assertThrows(IllegalArgumentException.class,()->goals.startConstruction051(args));}
                assertThrows(IllegalArgumentException.class,()->goals.startConstruction051(WorldFrames.copy(base).put("directFirst055",false)));
                assertThrows(IllegalArgumentException.class,()->goals.startConstruction051(new JSONObject().put("allowPayloadRedesign058",false)));
                assertEquals(0,goals.list().length());assertEquals(0,goals.procedures().length());
            }
        }
    }
    @Test public void phaseProjectionRetainsFailedBaselineAndFailedReservedOutcome(){
        var groups=new JSONArray();for(String phase:List.of("design-maximum-force","selected-reserved"))groups.put(new JSONObject().put("phase",phase).put("summary",new JSONObject().put("results",new JSONArray().put(new JSONObject().put("metrics",new JSONObject().put("stable",false)))).put("repeatable",true).put("totalTicks",2880).put("sourceRunSha256",phase+"-source")));
        var data=new JSONObject().put("groups",groups).put("complete",true).put("repeatable",true).put("freshDesignPassed",true).put("selectedReservedPassed",false);
        var rows=PayloadReport057.groups058(data);assertEquals("0/1",rows.getJSONArray(0).getString(1));assertEquals("selected-reserved",rows.getJSONArray(1).getString(0));assertFalse(Design058.eligible(data));
    }
}

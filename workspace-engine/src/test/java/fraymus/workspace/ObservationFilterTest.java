package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ObservationFilterTest {
    static JSONObject measurement(double x,double y){return new JSONObject().put("box",new PixelTracker.Box(x-.05,y-.05,.1,.1).json());}
    @Test public void predictsBeforeMeasurementAndLearnsVelocity(){
        var f=new ObservationFilter(0,new PixelTracker.Box(.2,.3,.1,.1));double first=0,last=0;
        for(int i=1;i<=20;i++){var e=f.step(i,measurement(.25+i*.005,.35));assertTrue(e.getBoolean("accepted"));if(i==1)first=e.getDouble("innovation");last=e.getDouble("innovation");}
        assertTrue(last<first/5);
        var prediction=f.step(21,new JSONObject().put("box",JSONObject.NULL));assertFalse(prediction.getBoolean("accepted"));assertEquals(.355,prediction.getJSONArray("position").getDouble(0),.003);
    }
    @Test public void noMeasurementGrowsUncertaintyAndDoesNotPretendToObserve(){
        var f=new ObservationFilter(0,new PixelTracker.Box(.2,.3,.1,.1));double before=f.step(1,measurement(.25,.35)).getDouble("positionSigma");
        var missing=f.step(10,new JSONObject().put("box",JSONObject.NULL));assertEquals("predicted",missing.getString("status"));assertFalse(missing.has("measurement"));assertTrue(missing.getDouble("positionSigma")>before*3);
    }
    @Test public void abruptWrongIdentityIsRejected(){
        var f=new ObservationFilter(0,new PixelTracker.Box(.1,.1,.1,.1));var e=f.step(1,measurement(.85,.85));assertFalse(e.getBoolean("accepted"));assertEquals("rejected",e.getString("status"));assertEquals(.15,e.getJSONArray("position").getDouble(0),1e-12);
    }
    @Test public void duplicateDelayedAndExcessiveGapsRefused(){
        var f=new ObservationFilter(10,new PixelTracker.Box(.1,.1,.1,.1));for(int t:new int[]{10,9,41})assertThrows(IllegalArgumentException.class,()->f.step(t,measurement(.2,.2)));assertTrue(f.step(11,measurement(.2,.2)).getBoolean("accepted"));
    }
}

package fraymus.workspace;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlaneCalibrationTest {
    static JSONObject mat(){return new JSONObject().put("mode","homography").put("cameraKey","test-fixed").put("note","Labelled analytic control, not instrument footage").put("anchorSigmaM",.002).put("scaleRelativeSigma",.01).put("imagePoints",new JSONArray("[[0.1,0.2],[0.9,0.15],[0.8,0.85],[0.2,0.8]]")).put("worldPoints",new JSONArray("[[0,0],[0.3048,0],[0.3048,0.3048],[0,0.3048]]"));}
    @Test public void fourCornersRecoverMetricPlaneAndRejectExtrapolation(){var p=new PlaneCalibration(mat());for(int i=0;i<4;i++){var src=mat().getJSONArray("imagePoints").getJSONArray(i);assertArrayEquals(PlaneCalibration.point(mat().getJSONArray("worldPoints").getJSONArray(i)),p.project(src.getDouble(0),src.getDouble(1)),1e-10);}assertTrue(p.inPlane(.5,.5));assertFalse(p.inPlane(.95,.95));assertTrue(p.sigma(.5,.5,.0001,.0001)>0);}
    @Test public void rulerAccountsForRectangularPixelsAndRotation(){var d=mat();d.put("mode","ruler").put("imagePoints",new JSONArray("[[0.2,0.4],[0.8,0.4]]")).put("lengthM",.3).put("imageAspect",2);d.remove("worldPoints");var p=new PlaneCalibration(d);assertArrayEquals(new double[]{.3,0},p.project(.8,.4),1e-12);assertArrayEquals(new double[]{0,.1},p.project(.2,.8),1e-12);}
    @Test public void rejectsDegenerateCrossedNonfiniteOrUndeclaredAuthority(){assertThrows(IllegalArgumentException.class,()->new PlaneCalibration(mat().put("imagePoints",new JSONArray("[[0,0],[1,1],[1,0],[0,1]]"))));assertThrows(IllegalArgumentException.class,()->new PlaneCalibration(mat().put("imagePoints",new JSONArray("[[0,0],[0.1,0.1],[0.2,0.2],[0.3,0.3]]"))));assertThrows(IllegalArgumentException.class,()->new PlaneCalibration(mat().put("nativeWrite",true)));assertThrows(IllegalArgumentException.class,()->new PlaneCalibration(mat().put("anchorSigmaM",-1)));}
    @Test public void tsaiGroundPlaneProjectionMatchesIndependentSimpleCamera(){var d=mat();d.remove("imagePoints");d.remove("worldPoints");d.put("mode","tsai").put("tsai",new JSONObject("{focal:20,kappa1:0,cx:360,cy:288,sx:1,dpx:0.02,dpy:0.02,tx:0,ty:0,tz:1000,rx:0,ry:0,rz:0,width:720,height:576,metresPerUnit:0.001}"));var p=new PlaneCalibration(d);assertArrayEquals(new double[]{.1,.05},p.project(460./720,338./576),1e-12);}
}

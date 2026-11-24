package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;

import static org.junit.Assert.*;

public class MetricHeightStudyTest {
    @Rule public TemporaryFolder temp=new TemporaryFolder();

    @Test public void choosesByGeometryBeforeReadingTheSecondObservation() throws Exception {
        var reference=json("independent-mesh-measurements");
        JSONObject first=null;
        for (String name:List.of("low","tall")) {
            var study=new MetricHeightStudy(bytes(name));
            var plan=study.plan();
            assertFalse(plan.getBoolean("heightMeasured"));
            assertEquals("side",plan.getString("selectedView"));
            assertEquals(0,plan.getJSONArray("candidateScores").getJSONObject(0).getDouble("separationLowerPx"),1e-9);
            if (first!=null) assertTrue(first.similar(plan));
            first=plan;
            var measured=study.verify(plan);
            assertTrue(measured.toString(),measured.getBoolean("passed"));
            double truth=reference.getJSONObject(name).getDouble("meshHeightM");
            assertEquals(truth,measured.getDouble("heightM"),.005);
            assertTrue(measured.getDouble("lowM")<=truth && truth<=measured.getDouble("highM"));
            assertTrue(measured.getDouble("withheldErrorPx")<=2);
        }
        var changed=json("low");
        var views=changed.getJSONArray("views");
        for (int i=0;i<views.length();i++) if (views.getJSONObject(i).getString("id").equals("side")) { views.remove(i);break; }
        assertEquals("inclined",new MetricHeightStudy(changed.toString().getBytes(StandardCharsets.UTF_8)).plan().getString("selectedView"));
    }

    @Test public void refusesAnOcclusionAndAnIndependentlyDisagreeingFrame() throws Exception {
        var occluded=new MetricHeightStudy(bytes("occluded"));
        var plan=occluded.plan();
        assertTrue(plan.similar(new MetricHeightStudy(bytes("low")).plan()));
        var missing=occluded.verify(plan);
        assertFalse(missing.getBoolean("passed"));
        assertTrue(missing.getString("reason").contains("missing"));

        var mismatch=json("low");
        view(mismatch,"withheld").put("observationPng",view(json("tall"),"withheld").getString("observationPng"));
        var swapped=new MetricHeightStudy(mismatch.toString().getBytes(StandardCharsets.UTF_8));
        assertTrue(plan.similar(swapped.plan()));
        var failed=swapped.verify(plan);
        assertFalse(failed.getBoolean("passed"));
        assertTrue(failed.getString("reason").contains("withheld"));
        assertTrue(failed.getDouble("withheldErrorPx")>2);
        assertThrows(IllegalStateException.class,()->swapped.collider(failed,"invalid"));
    }

    @Test public void invisibleMarkerPixelsCannotAuthorizeAHeight() throws Exception {
        var source=json("low");
        var side=view(source,"side");
        byte[] bytes=Base64.getDecoder().decode(side.getString("observationPng"));
        var raster=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        var alpha=new java.awt.image.BufferedImage(raster.getWidth(),raster.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y=0;y<raster.getHeight();y++) for (int x=0;x<raster.getWidth();x++) {
            int rgb=raster.getRGB(x,y);
            boolean marker=((rgb>>16)&255)>160 && ((rgb>>8)&255)<90 && (rgb&255)>160;
            alpha.setRGB(x,y,marker?rgb&0xffffff:rgb);
        }
        var output=new java.io.ByteArrayOutputStream();
        assertTrue(javax.imageio.ImageIO.write(alpha,"png",output));
        side.put("observationPng",Base64.getEncoder().encodeToString(output.toByteArray()));
        var study=new MetricHeightStudy(source.toString().getBytes(StandardCharsets.UTF_8));
        var measured=study.verify(study.plan());
        assertFalse(measured.getBoolean("passed"));
        assertTrue(measured.getString("reason").contains("missing"));
    }

    @Test public void aConnectedButPartiallyHiddenMarkerCannotAuthorizeAHeight() throws Exception {
        var source=json("low");
        var side=view(source,"side");
        byte[] bytes=Base64.getDecoder().decode(side.getString("observationPng"));
        var raster=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        int left=raster.getWidth();
        for (int y=0;y<raster.getHeight();y++) for (int x=0;x<raster.getWidth();x++)
            if ((raster.getRGB(x,y)&0xffffff)==0xff2cff) left=Math.min(left,x);
        for (int y=0;y<raster.getHeight();y++) for (int x=left;x<left+4;x++) {
            int rgb=raster.getRGB(x,y);
            if (((rgb>>16)&255)>160 && ((rgb>>8)&255)<90 && (rgb&255)>160) raster.setRGB(x,y,0xff535353);
        }
        var output=new java.io.ByteArrayOutputStream();
        assertTrue(javax.imageio.ImageIO.write(raster,"png",output));
        side.put("observationPng",Base64.getEncoder().encodeToString(output.toByteArray()));
        var study=new MetricHeightStudy(source.toString().getBytes(StandardCharsets.UTF_8));
        var measured=study.verify(study.plan());
        assertFalse(measured.getBoolean("passed"));
        assertTrue(measured.getString("reason").contains("top marker"));
    }

    @Test public void restoresTheExactReviewAndRechecksOriginalBytesBeforeIdempotentColliderImport() throws Exception {
        var root=temp.getRoot().toPath();
        var pre=new HeightStudyStore(root);
        assertTrue(pre.latest().isEmpty());
        var reviewed=pre.review(new JSONObject().put("base64",Base64.getEncoder().encodeToString(bytes("low"))));
        assertEquals(1,reviewed.getJSONObject("previews").length());
        var reopened=new HeightStudyStore(root);
        assertTrue(reviewed.similar(reopened.latest()));
        String id=reviewed.getString("id");
        assertThrows(IllegalStateException.class,()->reopened.approve(new JSONObject().put("id",id).put("planSha256","wrong")));
        assertThrows(IllegalStateException.class,()->reopened.approvedCollider(
                new JSONObject().put("id",id).put("colliderApprovalSha256","not-yet")));
        var verified=reopened.approve(new JSONObject().put("id",id).put("planSha256",reviewed.getString("planSha256")));
        assertEquals("verified",verified.getString("status"));
        assertEquals(3,verified.getJSONObject("previews").length());
        var later=new HeightStudyStore(root);
        assertTrue(verified.similar(later.latest()));
        var approval=new JSONObject().put("id",id).put("colliderApprovalSha256",verified.getString("colliderApprovalSha256"));
        String sceneId;
        try(var spatial=new SpatialStore(root)) {
            var scene=spatial.importHeightCollider(later.approvedCollider(approval));
            sceneId=scene.getString("id");
            var prop=scene.getJSONArray("objects").getJSONObject(1);
            assertEquals(verified.getJSONObject("measurement").getDouble("highM"),prop.getJSONArray("size").getDouble(1),1e-12);
            assertTrue(scene.similar(spatial.importHeightCollider(later.approvedCollider(approval))));
            assertEquals(1,spatial.list().length());
        }
        Files.write(root.resolve("spatial/height-studies").resolve(id+".source"),bytes("tall"));
        assertThrows(IllegalStateException.class,()->later.approvedCollider(approval));
        try(var spatial=new SpatialStore(root)) {
            assertThrows(IllegalStateException.class,()->spatial.read(sceneId));
        }
    }

    static JSONObject view(JSONObject study,String id) {
        for (Object raw:study.getJSONArray("views")) {
            var item=(JSONObject)raw;
            if (item.getString("id").equals(id)) return item;
        }
        throw new AssertionError("Test view unavailable: "+id);
    }

    static JSONObject json(String key) throws Exception {
        return new JSONObject(new String(bytes(key),StandardCharsets.UTF_8));
    }

    static byte[] bytes(String key) throws Exception {
        try(var in=MetricHeightStudyTest.class.getResourceAsStream("/spatial/height-studies/"+key+".json")) {
            assertNotNull(key,in);
            return in.readAllBytes();
        }
    }
}

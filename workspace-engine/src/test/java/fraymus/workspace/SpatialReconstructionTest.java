package fraymus.workspace;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.Assert.*;

public class SpatialReconstructionTest {
    @Rule public TemporaryFolder folder=new TemporaryFolder();

    @Test public void reconstructedMeshRoundTripsThroughTheSpatialFormat() throws Exception {
        var result=SpatialReconstruction.build(fixture());
        var proof=result.evidence();
        assertEquals(650,proof.getInt("voxels"));
        assertEquals(.08125,proof.getDouble("volumeM3"),1e-10);
        for (String axis:new String[]{"x","y","z"})
            assertEquals(1,proof.getJSONObject("inputProjectionIou").getDouble(axis),0);
        var scene=SpatialScene.parse(result.definition().toString(),UUID.randomUUID().toString());
        var glb=SpatialGlb.verifiedExport(scene);
        var imported=SpatialGlb.importScene(glb);
        var returned=SpatialScene.parse(imported.toString(),UUID.randomUUID().toString());
        var original=scene.getJSONArray("objects").getJSONObject(1);
        var copy=returned.getJSONArray("objects").getJSONObject(1);
        assertTrue(SpatialGlb.asset(scene,original).closeTo(SpatialGlb.asset(returned,copy)));
        assertTrue(SpatialScene.vector(copy.getJSONArray("size"),40)[0]>.4);
    }

    @Test public void keepsASealedSourceAndRefusesAnEditedOriginal() throws Exception {
        String id;
        var project=folder.getRoot().toPath();
        try (var store=new SpatialStore(project)) {
            var scene=store.reconstruct(fixture());
            id=scene.getString("id");
            assertEquals(id,store.reconstruct(fixture()).getString("id"));
            assertEquals(1,store.list().length());
        }
        try (var reopened=new SpatialStore(project)) {
            assertTrue(reopened.read(id).has("reconstruction"));
            Files.writeString(project.resolve("spatial/reconstruction-sources").resolve(id+".json"),"changed");
            assertThrows(IllegalStateException.class,()->reopened.read(id));
        }
    }

    @Test public void refusesMissingViewsAndUndeclaredCoordinates() throws Exception {
        var original=new JSONObject(new String(fixture(),StandardCharsets.UTF_8));
        original.getJSONObject("views").remove("y");
        assertThrows(IllegalArgumentException.class,
                ()->SpatialReconstruction.build(original.toString().getBytes(StandardCharsets.UTF_8)));
        var suppliedGeometry=new JSONObject(new String(fixture(),StandardCharsets.UTF_8));
        suppliedGeometry.put("geometry",new JSONObject());
        assertThrows(IllegalArgumentException.class,
                ()->SpatialReconstruction.build(suppliedGeometry.toString().getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] fixture() throws Exception {
        try (var source=SpatialReconstructionTest.class.getResourceAsStream("/spatial/reconstruction/bracket.json")) {
            assertNotNull(source);
            return source.readAllBytes();
        }
    }
}

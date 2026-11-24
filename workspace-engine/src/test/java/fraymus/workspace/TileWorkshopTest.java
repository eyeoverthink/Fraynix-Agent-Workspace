
package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.Base64;

import static org.junit.Assert.*;

public class TileWorkshopTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void reusesSpriteRecordsAcrossLevelsAndChecksOriginalsOnReopen() throws Exception {
        var project = temp.newFolder().toPath();
        var media = new TileWorkshop(project);
        var redSource = source("amber.png", 0xffff964d);
        String amber = media.add(redSource).getString("id");
        assertEquals(amber, media.add(redSource).getString("id"));
        String blue = media.add(source("blue.png", 0xff2781b3)).getString("id");
        String first = media.save(new JSONObject().put("layout", layout("First", blue, amber)))
                .getString("id");
        String second = media.save(new JSONObject().put("layout", layout("Second", blue, amber)))
                .getString("id");
        assertNotEquals(first, second);
        var revived = new TileWorkshop(project);
        assertEquals(2, revived.library(new JSONObject()).getJSONArray("assets").length());
        assertEquals(second, revived.open(new JSONObject()).getString("id"));
        revived.open(new JSONObject().put("id", first));
        assertEquals(first, new TileWorkshop(project).open(new JSONObject()).getString("id"));

        var export = revived.export(new JSONObject().put("id", first).put("format","json"));
        var receiver = new TileWorkshop(temp.newFolder().toPath());
        var imported = receiver.importLevel(new JSONObject().put("base64", export.getString("base64")));
        assertEquals(first, imported.getString("id"));
        assertEquals(2, receiver.library(new JSONObject()).getJSONArray("assets").length());
        Files.writeString(project.resolve("media/tiles/sprites/" + amber + ".json"), "damaged");
        assertThrows(IllegalArgumentException.class, () -> revived.open(new JSONObject()));
        assertThrows(IllegalArgumentException.class,
                () -> revived.export(new JSONObject().put("id", first).put("format","html")));
        assertEquals(2, revived.library(new JSONObject()).getJSONArray("assets").length());
    }

    @Test
    public void refusesSpawnOverlapAndDuplicateTerrainWithoutChangingLatest() throws Exception {
        var media = new TileWorkshop(temp.newFolder().toPath());
        String id = media.add(source("sprite.png", 0xfffea642)).getString("id");
        var layout = layout("Valid", id, id);
        String saved = media.save(new JSONObject().put("layout", layout)).getString("id");
        var terrain = layout.getJSONArray("objects").getJSONObject(0);
        layout.getJSONArray("objects").put(new JSONObject(terrain.toString()).put("id","copy"));
        assertThrows(IllegalArgumentException.class, () -> media.save(new JSONObject().put("layout",layout)));
        layout.getJSONArray("objects").remove(2);
        terrain.put("x",48).put("y",288);
        assertThrows(IllegalArgumentException.class, () -> media.save(new JSONObject().put("layout",layout)));
        assertEquals(saved, media.open(new JSONObject()).getString("id"));
    }

    private static JSONObject source(String name,int color) throws Exception {
        var image = new BufferedImage(12,12,BufferedImage.TYPE_INT_ARGB);
        for (int y=2;y<10;y++) for (int x=2;x<10;x++) image.setRGB(x,y,color);
        var data = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(image,"PNG",data));
        return new JSONObject().put("source", new JSONObject().put("filename",name)
                .put("png",Base64.getEncoder().encodeToString(data.toByteArray())));
    }

    private static JSONObject layout(String name, String terrain, String token) {
        var objects = new JSONArray()
                .put(new JSONObject().put("id","wall").put("assetId",terrain).put("kind","tile").put("layer","terrain")
                        .put("x",216).put("y",288).put("size",24)
                        .put("collider",new JSONObject().put("x",0).put("y",0).put("w",24).put("h",24)))
                .put(new JSONObject().put("id","token").put("assetId",token).put("kind","sprite").put("layer","pickup")
                        .put("x",400).put("y",280).put("size",32));
        return new JSONObject().put("title",name).put("objects",objects);
    }
}

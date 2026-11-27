package fraymus.workspace;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashSet;
import java.util.UUID;

/** Durable local portrait-to-level workflow. Only checked levels can publish exports. */
final class AvatarLevelMission {
    static final String FORMAT = "fraynix.avatar-level-mission-v1";
    static final String RELAY = "relay", JUMP = "jump";
    private static final int MAX_PLAY_TICKS = 620;
    private final Path root, exports;
    private final AvatarWorkshop avatars;
    private final TileWorkshop tiles;

    AvatarLevelMission(Path project, AvatarWorkshop avatars, TileWorkshop tiles) {
        this.root = project.resolve("media/avatar-level-missions");
        this.exports = project.resolve("exports/avatar-level");
        this.avatars = avatars;
        this.tiles = tiles;
    }

    synchronized JSONObject start(JSONObject input) throws IOException {
        SpatialScene.keys(input,"request","title","course","pause");
        String course = course(input.getString("course"));
        String request = SpatialScene.text(input,"request",1200).strip();
        String title = input.optString("title");
        if (title.length()>70 || title.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Use a level name up to 70 characters.");
        if (!(input.get("pause") instanceof Boolean)) throw new IllegalArgumentException("Choose whether to pause.");
        String id = UUID.randomUUID().toString();
        var mission = new JSONObject().put("format",FORMAT).put("id",id).put("request",request)
                .put("title",title.strip()).put("course",course).put("pause",input.getBoolean("pause"))
                .put("stage","awaiting-image");
        save(mission);
        SpriteWorkshop.replace(root.resolve("latest"),id.getBytes(StandardCharsets.US_ASCII));
        return mission;
    }

    synchronized JSONObject read(JSONObject input) throws IOException {
        SpatialScene.keys(input,"id");
        if (input.has("id")) return load(input.getString("id"));
        Path latest = root.resolve("latest");
        if (!Files.exists(latest,LinkOption.NOFOLLOW_LINKS)) return new JSONObject();
        return load(new String(SpriteWorkshop.read(latest,36),StandardCharsets.US_ASCII));
    }

    synchronized JSONObject build(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","avatarId");
        var mission = load(input.getString("id"));
        String avatarId = input.getString("avatarId");
        if (!"awaiting-image".equals(mission.getString("stage"))) {
            if (!avatarId.equals(mission.getString("avatarId")))
                throw new IllegalArgumentException("This mission already uses a different saved avatar.");
            checkedLevel(mission);
            return mission;
        }
        var source = avatars.sourceProof(avatarId);
        var player = avatars.player(new JSONObject().put("id",avatarId)).getJSONObject("player");
        String title = mission.getString("title");
        if (title.isEmpty()) title = mission.getString("course").equals(JUMP) ? "Avatar Jump Run" : "Avatar Relay";
        var level = tiles.saveMissionLayout(courseLayout(title,mission.getString("course"),player));
        return save(mission.put("title",title).put("avatarId",avatarId).put("source",source)
                .put("levelId",level.getString("id")).put("stage","built"));
    }

    synchronized JSONObject prepare(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id");
        var mission = load(input.getString("id"));
        if (!"built".equals(mission.getString("stage"))) throw new IllegalArgumentException("Build this level before testing.");
        var level = checkedLevel(mission);
        return new JSONObject().put("mission",mission).put("level",level)
                .put("candidate",tiles.export(new JSONObject().put("id",mission.getString("levelId")).put("format","html")))
                .put("maxTicks",MAX_PLAY_TICKS);
    }

    synchronized JSONObject verify(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id","runId","baselineId","htmlSha256","frameSha256","witnessRows");
        var mission = load(input.getString("id"));
        if (!"built".equals(mission.getString("stage"))) throw new IllegalArgumentException("This level is outside its testing stage.");
        var level = checkedLevel(mission);
        var run = tiles.run(new JSONObject().put("runId",input.getString("runId")),true);
        var rows = run.getJSONArray("rows");
        if (!run.getString("levelId").equals(mission.getString("levelId")) || !rows.similar(input.getJSONArray("witnessRows")))
            throw new IllegalArgumentException("The standalone run differs from the recorded desktop run.");
        var candidate = tiles.export(new JSONObject().put("id",mission.getString("levelId")).put("format","html"));
        if (!candidate.getString("sha256").equals(input.getString("htmlSha256")))
            throw new IllegalArgumentException("The tested HTML differs from the checked level export.");
        String frame = input.getString("frameSha256");
        if (!frame.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("A rendered standalone frame is required.");
        var verdict = checkCourse(mission,level.getJSONObject("layout"),run);
        if (mission.getString("course").equals(JUMP)) {
            var baseline = tiles.run(new JSONObject().put("runId",input.getString("baselineId")),true);
            if (!baseline.getString("levelId").equals(run.getString("levelId"))
                    || !baseline.getString("rendererSha256").equals(run.getString("rendererSha256"))
                    || !baseline.getString("reason").equals("stopped")
                    || baseline.getJSONArray("rows").length()!=MAX_PLAY_TICKS+1)
                throw new IllegalArgumentException("The jump route needs a blocked straight-walk comparison.");
            var flat = baseline.getJSONArray("rows");
            boolean hit = false;
            for (int i=1;i<flat.length();i++) {
                var row = flat.getJSONArray(i);
                if (row.getInt(18)!=1 || row.getBoolean(16))
                    throw new IllegalArgumentException("The comparison must use a continuous right input without jumping.");
                hit |= row.getString(6).equals("hurdle-1");
            }
            if (!hit) throw new IllegalArgumentException("The straight-walk comparison did not show the first hurdle blocking travel.");
            verdict.put("baselineRunId",baseline.getString("id")).put("straightWalkBlocked",true);
        }
        verdict.put("runId",run.getString("id")).put("htmlSha256",candidate.getString("sha256"))
                .put("standaloneFrameSha256",frame).put("rendererSha256",run.getString("rendererSha256"));
        return save(mission.put("evidence",verdict).put("stage","verified"));
    }

    synchronized JSONObject finish(JSONObject input) throws Exception {
        SpatialScene.keys(input,"id");
        var mission = load(input.getString("id"));
        String stage = mission.getString("stage");
        if (!stage.equals("verified") && !stage.equals("exported"))
            throw new IllegalArgumentException("Complete the playtest before exporting.");
        var level = checkedLevel(mission);
        var proof = mission.getJSONObject("evidence");
        String levelId = mission.getString("levelId");
        var run = tiles.run(new JSONObject().put("runId",proof.getString("runId")),true);
        checkCourse(mission,level.getJSONObject("layout"),run);
        var html = tiles.export(new JSONObject().put("id",levelId).put("format","html"));
        var json = tiles.export(new JSONObject().put("id",levelId).put("format","json"));
        if (!html.getString("sha256").equals(proof.getString("htmlSha256")))
            throw new IllegalArgumentException("Level or renderer changed. Start a new mission and test that export.");
        Path folder = exports.resolve(mission.getString("id"));
        var files = new JSONObject()
                .put("html",publish(folder.resolve("playable.html"),Base64.getDecoder().decode(html.getString("base64"))))
                .put("level",publish(folder.resolve("editable-level.json"),Base64.getDecoder().decode(json.getString("base64"))))
                .put("playtest",publish(folder.resolve("playtest.json"),MapCheckpoint.canonical(run).getBytes(StandardCharsets.UTF_8)));
        var receipt = new JSONObject().put("format",FORMAT).put("id",mission.getString("id"))
                .put("course",mission.getString("course")).put("title",mission.getString("title"))
                .put("levelId",levelId).put("source",mission.getJSONObject("source")).put("checks",proof);
        files.put("evidence",publish(folder.resolve("evidence.json"),MapCheckpoint.canonical(receipt).getBytes(StandardCharsets.UTF_8)));
        if (stage.equals("exported") && !files.similar(mission.getJSONObject("files")))
            throw new IllegalArgumentException("The export evidence changed.");
        return save(mission.put("files",files).put("stage","exported"));
    }

    private static JSONObject publish(Path path, byte[] bytes) throws IOException {
        Files.createDirectories(path.getParent());
        String sha = MapDefinition.sha(bytes);
        if (!Files.exists(path,LinkOption.NOFOLLOW_LINKS)) SpriteWorkshop.replace(path,bytes);
        SpriteWorkshop.read(path,bytes.length,sha);
        return new JSONObject().put("path",path.toAbsolutePath().toString()).put("sha256",sha).put("bytes",bytes.length);
    }

    private JSONObject checkedLevel(JSONObject mission) throws Exception {
        JSONObject source;
        try {
            source=avatars.sourceProof(mission.getString("avatarId"));
        } catch (IOException | IllegalArgumentException invalid) {
            throw new IllegalArgumentException("The saved avatar source could not be verified.",invalid);
        }
        if (!source.similar(mission.getJSONObject("source")))
            throw new IllegalArgumentException("The avatar image no longer matches the mission.");
        var level = tiles.inspect(new JSONObject().put("id",mission.getString("levelId")));
        var layout = level.getJSONObject("layout");
        var player = layout.getJSONObject("player");
        if (!player.getString("avatarId").equals(mission.getString("avatarId"))
                || !layout.getString("course").equals(mission.getString("course")))
            throw new IllegalArgumentException("The level no longer matches the saved avatar and course.");
        return level;
    }

    private static JSONObject checkCourse(JSONObject mission, JSONObject layout, JSONObject run) {
        if (!run.getString("levelId").equals(mission.getString("levelId")) || !run.getString("reason").equals("won"))
            throw new IllegalArgumentException("The avatar did not finish this level.");
        var rows = run.getJSONArray("rows");
        var last = rows.getJSONArray(rows.length()-1);
        var required = new HashSet<String>();
        var hurdles = new JSONArray();
        for (Object value : layout.getJSONArray("objects")) {
            var item=(JSONObject)value;
            if (item.getString("layer").equals("pickup")) required.add(item.getString("id"));
            if (item.has("collider")) hurdles.put(item);
        }
        int expectedScore = 500+100*required.size();
        var picked = new HashSet<String>();
        var cleared = new HashSet<String>();
        int previousJumps = 0;
        for (int i=0;i<rows.length();i++) {
            var row=rows.getJSONArray(i);
            if (row.getInt(12)!=0) throw new IllegalArgumentException("The playtest reset before the exit.");
            for (Object value : row.getJSONArray(17)) {
                var event=(JSONArray)value;
                if ("pickup".equals(event.getString(0))) picked.add(event.getString(1));
                if ("jump".equals(event.getString(0))) previousJumps++;
            }
            for (Object value : hurdles) {
                var hurdle=(JSONObject)value;
                double x=row.getDouble(1), feet=row.getDouble(2)+32;
                if (x>=hurdle.getInt("x") && x<hurdle.getInt("x")+hurdle.getInt("size")
                        && feet<=hurdle.getInt("y")+.01)
                    cleared.add(hurdle.getString("id"));
            }
        }
        if (!picked.equals(required) || cleared.size()!=hurdles.length() || last.getInt(15)!=expectedScore
                || last.getDouble(1)+24<672 || !"floor".equals(last.getString(5)))
            throw new IllegalArgumentException("The course proof must show each pickup, hurdle clearance and a grounded finish.");
        return new JSONObject().put("won",true).put("score",expectedScore).put("ticks",rows.length()-1)
                .put("pickups",picked.size()).put("hurdles",cleared.size()).put("jumps",previousJumps)
                .put("standaloneReplay",true);
    }

    private JSONObject courseLayout(String title,String course,JSONObject player) throws Exception {
        var assets = new JSONArray();
        String token = tiles.add(new JSONObject().put("source",source("Avatar coin.png",true))).getString("id");
        int[] coins = JUMP.equals(course) ? new int[]{150,405,594} : new int[]{144,352,558};
        for (int i=0;i<coins.length;i++) assets.put(item("coin-"+(i+1),token,"pickup",coins[i],287,24));
        if (JUMP.equals(course)) {
            String rail = tiles.add(new JSONObject().put("source",source("Avatar hurdle.png",false))).getString("id");
            assets.put(hurdle("hurdle-1",rail,258)).put(hurdle("hurdle-2",rail,480));
        }
        return new JSONObject().put("title",title).put("player",player).put("course",course).put("objects",assets);
    }

    private static JSONObject hurdle(String id,String asset,int x) {
        return item(id,asset,"terrain",x,264,48).put("collider",new JSONObject().put("x",0).put("y",0).put("w",48).put("h",48));
    }

    private static JSONObject item(String id,String asset,String layer,int x,int y,int size) {
        return new JSONObject().put("id",id).put("assetId",asset).put("kind","sprite")
                .put("layer",layer).put("x",x).put("y",y).put("size",size);
    }

    private static JSONObject source(String filename,boolean coin) throws IOException {
        var image = new BufferedImage(24,24,BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        if (coin) {
            g.setColor(new Color(234,167,75)); g.fillOval(1,1,22,22);
            g.setColor(new Color(255,232,154)); g.fillOval(5,5,14,14);
            g.setColor(new Color(234,167,75)); g.fillRect(11,7,2,10);
        } else {
            g.setColor(new Color(42,110,136)); g.fillRect(1,1,22,22);
            g.setColor(new Color(123,222,191)); g.fillRect(1,1,22,4);
            g.setColor(new Color(26,70,94)); g.fillRect(6,8,12,12);
        }
        g.dispose();
        var bytes = new ByteArrayOutputStream();
        if (!ImageIO.write(image,"png",bytes)) throw new IOException("PNG encoder unavailable.");
        return new JSONObject().put("filename",filename).put("png",Base64.getEncoder().encodeToString(bytes.toByteArray()));
    }

    static String course(String course) {
        if (!course.equals(RELAY) && !course.equals(JUMP)) throw new IllegalArgumentException("Choose a relay or a jump course.");
        return course;
    }

    private JSONObject load(String id) throws IOException {
        var mission=AgentFiles042.loadChecked(file(id));
        if (!FORMAT.equals(mission.getString("format")) || !id.equals(mission.getString("id")))
            throw new IOException("The avatar level mission identity changed.");
        return mission;
    }

    private JSONObject save(JSONObject mission) throws IOException {
        AgentFiles042.saveChecked(file(mission.getString("id")),mission);
        return mission;
    }

    private Path file(String id) {
        if (!id.matches("[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}"))
            throw new IllegalArgumentException("Invalid avatar level mission ID.");
        return root.resolve(id+".json");
    }
}

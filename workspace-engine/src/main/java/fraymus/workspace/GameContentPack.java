package fraymus.workspace;

import fraymus.neural.RelayCourse;
import java.nio.file.*;
import org.json.JSONObject;

/** Pinned original pack, separate from historical source-derived candidates. */
final class GameContentPack {
    static final String ID = RelayCourse.GARDEN.id();
    static final String ROOT = RelayCourse.ASSETS;
    private final Path repo;

    GameContentPack(Path repo) { this.repo = repo; }

    JSONObject verify() throws Exception { return verify(RelayCourse.GARDEN); }

    JSONObject verify(RelayCourse course) throws Exception {
        Path manifestPath = repo.resolve(course.manifest());
        JSONObject manifest = new JSONObject(GameLevels114.read(manifestPath, 32_000));
        if (!course.id().equals(manifest.getString("id")) || !course.level().equals(manifest.getString("level")))
            throw new IllegalStateException("Unknown original content pack");
        Path installed = repo.resolve("build/install/Fraynix-Studio");
        String sha = GameLevels114.hash(manifestPath);
        if (!sha.equals(GameLevels114.hash(installed.resolve(course.manifest()))))
            throw new IllegalStateException("Installed original content manifest differs");
        JSONObject files = manifest.getJSONObject("files");
        if (!files.keySet().equals(course.files()))
            throw new IllegalStateException("Incomplete original content pack");
        for (String name : files.keySet()) {
            for (Path base : new Path[]{repo, installed}) {
                Path file = base.resolve(name);
                if (!Files.isRegularFile(file) || Files.size(file) > 2_000_000
                        || !files.getString(name).equals(GameLevels114.hash(file)))
                    throw new IllegalStateException("Original content bytes changed: " + name);
            }
        }
        return manifest.put("manifestSha256", sha).put("checked", true);
    }
}

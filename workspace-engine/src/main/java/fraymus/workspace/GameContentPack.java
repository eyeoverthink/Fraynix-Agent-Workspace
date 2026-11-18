package fraymus.workspace;

import java.nio.file.*;
import org.json.JSONObject;

/** Pinned original pack, separate from historical source-derived candidates. */
final class GameContentPack {
    static final String ID = "relay-garden-v1";
    static final String ROOT = "assets/original/relay-garden/";
    private final Path repo;

    GameContentPack(Path repo) { this.repo = repo; }

    JSONObject verify() throws Exception {
        Path manifestPath = repo.resolve(ROOT + "manifest.json");
        JSONObject manifest = new JSONObject(GameLevels114.read(manifestPath, 32_000));
        if (!ID.equals(manifest.getString("id")) || !(ROOT + "level.json").equals(manifest.getString("level")))
            throw new IllegalStateException("Unknown original content pack");
        Path installed = repo.resolve("build/install/Fraynix-Studio");
        String sha = GameLevels114.hash(manifestPath);
        if (!sha.equals(GameLevels114.hash(installed.resolve(ROOT + "manifest.json"))))
            throw new IllegalStateException("Installed original content manifest differs");
        JSONObject files = manifest.getJSONObject("files");
        if (files.length() != 18 || !files.has(ROOT + "level.json"))
            throw new IllegalStateException("Incomplete original content pack");
        for (String name : files.keySet()) {
            if (!name.startsWith(ROOT) || name.contains("..") || name.contains("\\"))
                throw new IllegalStateException("Original content path refused");
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

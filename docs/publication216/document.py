"""Publish the checked blueprint, Blender exchange, and edition 216 guide."""
import importlib.util
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("documentation210", Path("docs/publication210/document.py"))
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
read, write, write_json, prepend = helpers.read, helpers.write, helpers.write_json, helpers.prepend

ROOT = Path("docs/readmes/216")
TITLE = "Take an editable scene from image to Blender and back"
DATE = "2026-11-23"
NOTE = "224-spatial-scene-exchange"
SUMMARY = ("Convert a labelled plan into an editable 3D scene, exchange it with Blender via GLB, "
           "and move a returned object using the existing verified agent tool.")
NEXT = ("Bring a detailed original mesh and a separate authored collider into the same scene. Verify that the agent can "
        "copy and transform the visual prop while physics uses the reviewed collider.")
SHOTS = {
    "blueprint-spatial.png": "Top-down Spatial view of the original labelled tile layout",
    "blender-return.png": "A Blender-edited copy reopens as individual editable objects",
    "native-move.png": "Fraynix completes and checks the native move on the returned object",
}
MODULES = (
    ("Binary interchange, transforms and collider validation", "workspace-engine/src/main/java/fraymus/workspace/SpatialGlb.java"),
    ("Original image to native geometry", "workspace-engine/src/main/java/fraymus/workspace/SpatialBlueprint.java"),
    ("Retained input bytes and scene binding", "workspace-engine/src/main/java/fraymus/workspace/SpatialStore.java"),
    ("Spatial controls and top-down camera", "workspace-desktop/renderer/spatial-panel.js"),
    ("Independent Blender and agent tests", "workspace-desktop/test/spatial-exchange-integration.cjs"),
)


def item(file):
    path = Path(file)
    if path.suffix != ".glb":
        return helpers.item(path)
    data = path.read_bytes()
    return {"path": path.as_posix(), "bytes": len(data), "sha256": sha256(data).hexdigest(), "hashKind": "binary"}


def publish(live, matrix_file):
    proof = json.loads(read(live / "result.json"))
    matrix = json.loads(read(matrix_file))
    assert matrix["conclusion"] == "success" and len(matrix["targets"]) == 4
    assert all(row["tests"] == 2 and row["failures"] == row["errors"] == 0 for row in matrix["targets"])
    assert proof["passed"] and proof["blender"]["version"].startswith("Blender ")
    assert abs(proof["blender"]["importedX"] - proof["blender"]["originalX"] - .15) < 1e-5
    assert proof["metrics"]["passed"] and proof["metrics"]["grabs"] == proof["metrics"]["releases"] == 1
    assert proof["browserChecks"] == 8 and len(proof["checks"]) == 5
    ROOT.mkdir(parents=True, exist_ok=True)
    for name in (*SHOTS, "reference.png", "blueprint.glb", "blender-edited.glb"):
        shutil.copyfile(live / name, ROOT / name)
    for name in (*SHOTS, "reference.png"):
        assert (ROOT / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
    for name, expected in (("reference.png", proof["reference"]["sha256"]), ("blueprint.glb", proof["glbSha256"]),
                           ("blender-edited.glb", proof["blender"]["sourceSha256"])):
        assert sha256((ROOT / name).read_bytes()).hexdigest() == expected
    write_json(ROOT / "release-proof.json", {"date": DATE, "windowsDesktopAndBlender": proof, "portableJava": matrix,
                                            "screenshots": [item(ROOT / name) for name in SHOTS]})

    dim = proof["reference"]["width"] * proof["reference"]["height"]
    count = proof["boxedObjects"]
    version = proof["blender"]["version"]
    tests = (f"The Windows desktop test checked all {dim} source cells against {count} native boxes. {version} moved the marker’s parent "
             "by 0.15 m. The reimport retained that offset and the original scene stayed intact. The agent run passed eight browser assertions "
             "alongside the native grab, move and release checks.")
    platforms = ("Java’s format, hierarchy and image-source tests passed on Windows x64, Linux x64, Apple silicon macOS and Intel macOS.")
    write("README-216.md", f"""# Fraynix Agent Workspace · Edition 216

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 215](README-215.md) · [Offline walkthrough](docs/readmes/216/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Top-down editor view shows the copied wall and floor layout](docs/readmes/216/blueprint-spatial.png)

## Try the full scene workflow

You can open either sample GLB directly with **Import Fraynix GLB**.

1. Import an opaque, labelled PNG into **Maps** and declare which tile colours mean floor. For the sample, choose #d9efe5 as floor. [Reference used in testing](docs/readmes/216/reference.png)
2. Open **Spatial → Scene exchange and image blueprints**, choose the saved image, set the metre width of one tile, and select **Build 3D copy**.
3. Use **Top down** to compare the scene with the source. **Export GLB** saves the box scene, positions, and Fraynix physics properties as a single file. [Original GLB](docs/readmes/216/blueprint.glb)
4. In Blender, keep the metadata empty and the box custom properties. Export as GLB with **Custom Properties** enabled. Use **Import Fraynix GLB** to open the edited copy. [File edited in Blender](docs/readmes/216/blender-edited.glb)
5. Select the movable marker. Use the existing **Grab, move & release** action to forecast the route, run it, and inspect the verified result.

![The editable copy returned by Blender is open in Fraynix](docs/readmes/216/blender-return.png)

## What was proved

- {tests}
- Reopening a copy rechecks the original GLB bytes. An altered retained source was refused, and the valid scene and native goal were still available after restart.
- {platforms} [CI results]({matrix["url"]}) · [Release proof](docs/readmes/216/release-proof.json)

![The native motion report for the Blender-edited object](docs/readmes/216/native-move.png)

This workflow covers static box geometry with Fraynix metadata and a labelled top-down image. The tile size and wall height are declared. Ordinary photos, high-detail visual meshes, materials, rigs and animation are outside this edition. [Scene contract and implementation plan](docs/publication216/plan.md)

## Next acceptance test

{NEXT}

[Source and progress map](docs/obsidian/{NOTE}.canvas)
""")
    home = read("README.md").split("[Edition ", 1)[0]
    write("README.md", home + f"[Edition 216 — {TITLE}](README-216.md) · [Previous edition](README-215.md)"
          " · [Offline walkthrough](docs/readmes/216/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    index = read("docs/readmes/INDEX.md")
    if "README-216.md" not in index:
        write("docs/readmes/INDEX.md", index.replace("[Edition 215", f"[Edition 216 — {TITLE}](../../README-216.md)"
              " · [Walkthrough](216/index.html)\n\n[Edition 215", 1))
    record = (f"## Edition 216 — {TITLE}\n\n{DATE}. {SUMMARY} {tests} {platforms} "
              "Retain imported bytes, distinguish declared tile and collider dimensions from measured geometry, and reuse existing "
              "agent verification. See README-216.md and docs/publication216/plan.md.\n\n")
    for file in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(file, record)
    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 216 — {TITLE}]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{tests}\n\n{NEXT}\n\n[Edition 216](../../README-216.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication216/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in MODULES]
    nodes = [module["source"] for module in modules] + ["README-216.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": file, "x": (i % 3) * 380, "y": (i // 3) * 290,
                   "width": 350, "height": 250} for i, file in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "toNode": str(i)} for i in range(1, len(nodes))],
    })
    gallery = [{"path": (ROOT / name).as_posix(), "title": caption} for name, caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT / "reference.png").as_posix(), "title": "Original labelled blueprint"},
        {"path": (ROOT / "blueprint.glb").as_posix(), "title": "Original GLB"},
        {"path": (ROOT / "blender-edited.glb").as_posix(), "title": "GLB edited in Blender"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Desktop, Blender and portable test evidence"},
        {"path": "docs/publication216/plan.md", "title": "References, next test and reproduction"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source_commit = matrix["headSha"]
    files = subprocess.check_output(["git", "diff", "--name-only", "readme-215", source_commit], text=True).splitlines()
    inventory = [item(file) for file in sorted(set(files) | {note, canvas, "docs/publication216/document.py"})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/215/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 216 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", lambda _: f"<h1>{TITLE}</h1><p>{SUMMARY}</p>", guide, count=1)
    guide = guide.replace("../../../README-215.md", "../../../README-216.md").replace(
        '../214/index.html">Edition 214', '../215/index.html">Edition 215').replace(
        '<a href="right/index.html">Right experiment</a><a href="jump/index.html">Jump experiment</a>',
        '<a href="reference.png">Original PNG</a><a href="blueprint.glb" download>Original GLB</a>'
        '<a href="blender-edited.glb" download>Blender-edited GLB</a>')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)
    manifest = json.loads(subprocess.check_output(["git", "show", "readme-215:docs/readmes/editions.json"]))
    assert manifest["latest"] == 215
    assets = [item(file) for file in sorted(ROOT.rglob("*")) if file.is_file()]
    frozen = [{"path": row["path"], "sha256": row["sha256"], "kind": "binary" if row["hashKind"] == "binary" else "text"}
              for row in assets]
    manifest["latest"] = 216
    manifest["editions"].append({"number": 216, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-216.md", "markdownSha256": item("README-216.md")["sha256"],
                                 "companion": (ROOT / "index.html").as_posix(), "tag": "readme-216",
                                 "sourceBaseline": source_commit, "snapshotDirectory": ROOT.as_posix(), "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print("Verified the live Blender test and published edition 216 drafts.")


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

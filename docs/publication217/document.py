"""Publish the original Blender prop, native collision evidence and returned scene."""
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
spec = importlib.util.spec_from_file_location("documentation216", Path("docs/publication216/document.py"))
old = importlib.util.module_from_spec(spec)
spec.loader.exec_module(old)
read, write, write_json, prepend, item = old.read, old.write, old.write_json, old.prepend, old.item
ROOT = Path("docs/readmes/217")
TITLE = "Copy a detailed prop with its own collider"
DATE = "2026-11-23"
NOTE = "225-detailed-prop"
SUMMARY = ("Import a Blender prop as a detailed triangle surface with a separate box collider. "
           "Use Chat to review a transformed copy, verify contacts in two native worlds, and return it to Blender.")
NEXT = ("Edit a part of one copy and verify which visual asset and collider changed. Then place a prop measured "
        "from calibrated multi-view observations and review its collider before acting.")
SHOTS = {
    "review.png": "The local agent shows the target pose and both native collision checks before approval",
    "verified-copy.png": "Two rendered props with their box colliders and the retained contact verdict",
}
MODULES = (
    ("Triangle data and shared visual assets", "workspace-engine/src/main/java/fraymus/workspace/SpatialVisualMesh.java"),
    ("GLB visual and collider exchange", "workspace-engine/src/main/java/fraymus/workspace/SpatialGlb.java"),
    ("Reviewed transform and native contact witness", "workspace-engine/src/main/java/fraymus/workspace/SpatialPropCopy.java"),
    ("Spatial copy review and collider overlay", "workspace-desktop/renderer/spatial-panel.js"),
    ("Real Blender authoring and export verification", "workspace-desktop/test/spatial-prop-integration.cjs"),
)


def publish(live, matrix_file):
    proof = json.loads(read(live / "result.json"))
    matrix = json.loads(read(matrix_file))
    assert matrix["conclusion"] == "success" and len(matrix["targets"]) == 4
    assert all(row["tests"] == 2 and row["failures"] == row["errors"] == row["skipped"] == 0 for row in matrix["targets"])
    assert proof["passed"] and proof["native"]["passed"] and proof["blender"]["passed"]
    assert proof["native"]["coreContacts"] > 0 and proof["native"]["visualOnlyContacts"] == 0
    assert proof["native"]["support"] == "floor" and proof["native"]["visualOverhangTested"]
    assert abs(proof["blender"]["yawDeg"] - 35) < .0001 and proof["blender"]["localErrorM"] < 1e-5
    ROOT.mkdir(parents=True, exist_ok=True)
    for name in (*SHOTS, "source.glb", "copied.glb"):
        shutil.copyfile(live / name, ROOT / name)
    write(ROOT / "blender-verification.json", read(live / "blender-verification.json"))
    for name in SHOTS:
        assert (ROOT / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
    for name, key in (("source.glb", "inputSha256"), ("copied.glb", "outputSha256")):
        assert sha256((ROOT / name).read_bytes()).hexdigest() == proof[key]
    write_json(ROOT / "release-proof.json", {"date": DATE, "windowsDesktopBlenderAndNative": proof,
                                            "portableJava": matrix, "screenshots": [item(ROOT / name) for name in SHOTS]})
    native = proof["native"]
    blender = proof["blender"]
    tests = (f"The original Blender beacon has {native['visualTriangles']} visual triangles. Two fresh native worlds "
             f"each reported {native['coreContacts']} contact inside the box, zero at a sampled point on the visual overhang, "
             "and support from the floor. Java refused an overlapping placement before committing the scene.")
    platforms = "The Java import, approval, native collision and saved-receipt checks passed on Windows x64, Linux x64, Apple silicon and Intel macOS."
    write("README-217.md", f"""# Fraynix Agent Workspace · Edition 217

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 216](README-216.md) · [Offline walkthrough](docs/readmes/217/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The original and transformed copy show the detailed surface and the collider overlay](docs/readmes/217/verified-copy.png)

## Try it

1. Open **Spatial → Scene exchange and image blueprints → Import Fraynix GLB**. [Original Blender beacon](docs/readmes/217/source.glb)
2. Select **Signal beacon**, then ask **“Copy and transform the selected prop.”** Enter the desired name, position offset and yaw.
3. Choose **Review copy**, confirm its target and select **Approve and verify**. Use **Show prop colliders** to see collision geometry in the viewport.
4. Keep the copied scene selected and choose **Export GLB**. Blender can reopen the export with both the original and the copy. [Tested return file](docs/readmes/217/copied.glb)

![The agent’s reviewed transform before running native collision checks](docs/readmes/217/review.png)

## What the test measured

- {tests}
- Blender {blender['version']} reimported the independently exported file and measured the requested **1.8 m X offset**, **−0.9 m Z offset** in Fraynix coordinates, and **35° yaw**. Corresponding local visual vertices agreed within **{blender['localErrorM']:.2g} m**. [Blender report](docs/readmes/217/blender-verification.json)
- Both copies share one immutable visual asset in the edited scene. Each has its own object ID and collider. Restart recovers the verified scene, and reapproving the same transform opens the same copy.
- {platforms} [Portable CI]({matrix["url"]}) · [Release evidence](docs/readmes/217/release-proof.json)

The current exchange covers one indexed triangle surface and one box collider per detailed prop. The Chat path uses a reviewed local procedure. [Blender authoring, collision tests and next step](docs/publication217/plan.md)

## Next acceptance test

{NEXT}

[Source and progress map](docs/obsidian/{NOTE}.canvas)
""")
    home = read("README.md").split("[Edition ", 1)[0]
    write("README.md", home + f"[Edition 217 — {TITLE}](README-217.md) · [Previous edition](README-216.md)"
          " · [Offline walkthrough](docs/readmes/217/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    index = read("docs/readmes/INDEX.md")
    if "README-217.md" not in index:
        write("docs/readmes/INDEX.md", index.replace("[Edition 216", f"[Edition 217 — {TITLE}](../../README-217.md)"
              " · [Walkthrough](217/index.html)\n\n[Edition 216", 1))
    record = (f"## Edition 217 — {TITLE}\n\n{DATE}. {SUMMARY} {tests} {platforms} "
              "Preserve the geometry asset, explicit collider role, exact approval and independent native evidence. "
              "See README-217.md and docs/publication217/plan.md.\n\n")
    for file in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(file, record)
    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 217 — {TITLE}]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{tests}\n\n{NEXT}\n\n[Edition 217](../../README-217.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication217/plan.md)\n")
    modules = [{"title": label, "source": file, "note": note} for label, file in MODULES]
    nodes = [row["source"] for row in modules]+["README-217.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type":"file", "file": file, "x": (i%3)*380, "y": (i//3)*290,
                   "width":350, "height":250} for i,file in enumerate(nodes)],
        "edges": [{"id":f"edge-{i}", "fromNode":str(i-1), "toNode":str(i)} for i in range(1,len(nodes))],
    })
    gallery = [{"path": (ROOT/name).as_posix(), "title": caption} for name,caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT/"source.glb").as_posix(), "title": "Original Blender beacon"},
        {"path": (ROOT/"copied.glb").as_posix(), "title": "Verified transformed copy"},
        {"path": (ROOT/"release-proof.json").as_posix(), "title": "Native collision receipts and portable tests"},
        {"path": "docs/publication217/plan.md", "title": "Detailed prop contract and reproduction"},
    ]}
    write_json(ROOT/"system-map.json",system)
    source=matrix["headSha"]
    files=subprocess.check_output(["git","diff","--name-only","readme-216",source],text=True).splitlines()
    inventory=[item(file) for file in sorted(set(files)|{note,canvas,"docs/publication217/document.py"})]
    write_json(ROOT/"file-inventory.json",inventory)
    guide=read("docs/readmes/216/index.html")
    guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 217 · {escape(TITLE)}</title>",guide,count=1)
    guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",lambda _:f"<h1>{TITLE}</h1><p>{SUMMARY}</p>",guide,count=1)
    guide=guide.replace("../../../README-216.md","../../../README-217.md").replace(
        '../215/index.html">Edition 215','../216/index.html">Edition 216').replace(
        '<a href="reference.png">Original PNG</a><a href="blueprint.glb" download>Original GLB</a>'
        '<a href="blender-edited.glb" download>Blender-edited GLB</a>',
        '<a href="source.glb" download>Original prop</a><a href="copied.glb" download>Verified copied scene</a>')
    images="\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                     f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide=re.sub(r"<figure>.*?(?=<h2)",lambda _:images+"\n",guide,count=1,flags=re.S)
    for key,data in (("system",system),("inventory",inventory)):
        pattern=f'<script id="{key}-data" type="application/json">.*?</script>'
        payload=json.dumps(data,ensure_ascii=True).replace("</","<\\/")
        guide=re.sub(pattern,lambda _:f'<script id="{key}-data" type="application/json">{payload}</script>',
                     guide,count=1,flags=re.S)
    write(ROOT/"index.html",guide)
    manifest=json.loads(subprocess.check_output(["git","show","readme-216:docs/readmes/editions.json"]))
    assert manifest["latest"]==216
    assets=[item(file) for file in sorted(ROOT.iterdir()) if file.is_file()]
    frozen=[{"path":row["path"],"sha256":row["sha256"],"kind":"binary" if row["hashKind"]=="binary" else "text"} for row in assets]
    manifest["latest"]=217
    manifest["editions"].append({"number":217,"date":DATE,"title":TITLE,"status":"validated-local",
                                 "markdown":"README-217.md","markdownSha256":item("README-217.md")["sha256"],
                                 "companion":(ROOT/"index.html").as_posix(),"tag":"readme-217","sourceBaseline":source,
                                 "snapshotDirectory":ROOT.as_posix(),"snapshotFiles":frozen})
    write_json("docs/readmes/editions.json",manifest)
    print("Prepared edition 217 with the real Blender-evaluated geometry and native receipts.")


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

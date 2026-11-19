"""Publish Edition 192 using the captured desktop and regression results."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


ROOT = Path("docs/readmes/192")
BASE = "6a40111a7b7647e469d593eb83694e60e711b355"
TITLE = "Teach Fraynix from a checked repair"
LINK_TITLE = "Teach and reuse a verified Blender repair"
SUMMARY = ("Save a verified Blender repair as a named lesson. Apply it to different terrain through Chat "
           "and require fresh playtests before accepting the result.")
PUBLISHED = "2026-11-19"


def read(path):
    return Path(path).read_text(encoding="utf-8-sig")


def write(path, text):
    Path(path).write_text(text, encoding="utf-8", newline="\n")


def write_json(path, value):
    write(path, json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def digest(path):
    return sha256(Path(path).read_bytes()).hexdigest()


def item(path):
    path = Path(path)
    if path.suffix in {".png", ".blend"}:
        return {"path": path.as_posix(), "bytes": path.stat().st_size, "sha256": digest(path), "hashKind": "binary"}
    data = read(path).encode()
    return {"path": path.as_posix(), "bytes": len(data), "sha256": sha256(data).hexdigest(), "hashKind": "utf8-lf"}


def prepend(path, text):
    old = read(path)
    if not old.startswith(text):
        write(path, text + old)


def main(live):
    source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    app = json.loads(read(live / "result.json"))
    tests = json.loads(read("build/edition192-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    assert app["checks"]["teacherBrowserChecks"] == 3 and app["checks"]["freshBrowserChecks"] == 4
    assert digest(ROOT / "narrower-gap-course.blend") == app["freshBlendSha256"]
    assert digest("docs/readmes/191/wide-gap-course.blend") == app["sourceBlendSha256"]
    assert json.loads(read(live / "repair-evidence.json"))["audit"] == app["audit"]
    assert app["audit"]["lesson"] == app["lesson"]
    shutil.copyfile(live / "repair-evidence.json", ROOT / "repair-evidence.json")

    labels = {
        "teach-checked-repair.png": "Name and save the lesson in Mission",
        "apply-lesson-to-fresh-level.png": "Select the verified lesson and prepare a fresh Chat request",
        "fresh-level-verified-report.png": "The new repair report passes its own browser checks",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name, expected = shot["name"], shot["sha256"]
        assert name in labels and digest(live / name) == expected
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})

    teacher, audit = app["teacherAudit"], app["audit"]
    first, second = (value["diagnosis"] for value in (teacher, audit))
    edits = len(second["edits"])
    ticks = audit["playtest"]["ticks"]
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    result = (f"The teaching run repaired {len(first['edits'])} ground tiles across a "
              f"{first['originalSpan']:.2f}-unit gap. After restart, the new level stalled on a "
              f"{second['originalSpan']:.2f}-unit gap. Java moved {edits} tiles, leaving at most "
              f"{second['remainingSpan']:.2f} units unsupported in that span. Two new native runs "
              f"reached the beacon at tick {ticks}. The saved report passed four Chromium checks.")
    write("README-192.md", f"""# Fraynix Agent Workspace · Edition 192

[Edition 191](README-191.md) · [Offline walkthrough](docs/readmes/192/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Save a lesson from the checked teaching mission](docs/readmes/192/teach-checked-repair.png)

## Teach and apply

1. Import the [teaching level](docs/readmes/191/wide-gap-course.blend) through **Game → Blender level candidates**. Prepare and start its direct repair request.
2. After it passes, open **Mission**, give the example a short name such as `restore-ground` and choose **Save checked lesson**.
3. Import the [new test level](docs/readmes/192/narrower-gap-course.blend). Select the saved lesson, choose **Prepare request with lesson**, then start the Chat task.
4. Open **Mission → Open saved result**. The report includes the lesson used, the new gap measurements and a download with the new native traces.

![A new level and its saved lesson selected in Game Lab](docs/readmes/192/apply-lesson-to-fresh-level.png)

## What passed

{result}

The app refused premature teaching, reuse on the original level, teaching recursively from a reuse result, and a wider target beyond a smaller lesson's limit. It detected an altered lesson checkpoint and an altered teaching receipt. The imported test level stayed byte-identical.

![The verified report from the second level](docs/readmes/192/fresh-level-verified-report.png)

The lesson stores the verified failure type, course layout and repair limits. Java derives new edits from each target's own failed motion. This edition supports grounded-stall gap repairs up to the example's observed limits. {counts}

[Native evidence](docs/readmes/192/repair-evidence.json) · [Verification results](docs/readmes/192/verification.json) · [Reproduction and next phase](docs/publication192/plan.md)
""")
    previous = "[Edition 191 — durable Blender repair from Chat](README-191.md)"
    lead = f"[Edition 192 — {LINK_TITLE}](README-192.md) · [Previous edition](README-191.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        old_end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/192/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[old_end:])
    index = read("docs/readmes/INDEX.md")
    if "README-192.md" not in index:
        start = index.index("[Edition 191")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 192 — {LINK_TITLE}](../../README-192.md)"
              + " · [Walkthrough](192/index.html)\n\n" + index[start:])
    record = f"## Edition 192 — {TITLE}\n\n{PUBLISHED}. {SUMMARY} {result} {counts} See README-192.md and docs/publication192/plan.md.\n\n"
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/200-verified-blender-lessons.md"
    canvas = "docs/obsidian/200-verified-blender-lessons.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 192 — {LINK_TITLE}](200-verified-blender-lessons.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n[Edition 192](../../README-192.md) · "
          "[Workflow map](200-verified-blender-lessons.canvas) · [Implementation plan](../publication192/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Verified lesson bank", java + "BlenderRepairLessons.java"),
        ("Fresh diagnosis and playtest", java + "BlenderGapRepair.java"),
        ("Game Lab controls", "workspace-desktop/renderer/brain-game.js"),
    )]
    nodes = [module["source"] for module in modules] + ["README-192.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": str(ROOT / "verification.json").replace("\\", "/"), "title": "Verification record"},
        {"path": str(ROOT / "repair-evidence.json").replace("\\", "/"), "title": "New native runs and diagnosis"},
        {"path": str(ROOT / "narrower-gap-course.blend").replace("\\", "/"), "title": "New Blender level"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source_paths = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(source_paths) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/191/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 192 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-191.md", "../../../README-192.md").replace(
        '../190/index.html">Edition 190', '../191/index.html">Edition 191')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in screenshots)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)

    manifest = json.loads(subprocess.check_output(["git", "show", f"{BASE}:docs/readmes/editions.json"]))
    assert manifest["latest"] == 191
    assets = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 192
    manifest["editions"].append({"number": 192, "date": PUBLISHED, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-192.md", "markdownSha256": item("README-192.md")["sha256"],
                                 "companion": "docs/readmes/192/index.html", "tag": "readme-192",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/192",
                                 "snapshotFiles": assets})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(assets)}))


if __name__ == "__main__":
    main(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition192-live"))

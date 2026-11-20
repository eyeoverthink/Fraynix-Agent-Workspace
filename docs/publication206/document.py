"""Freeze the measured change-story release and its offline guide."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "19f30198f1045fae3b15e1ca848f4664f39fb466"
ROOT = Path("docs/readmes/206")
TITLE = "Turn checked comparisons into interactive change stories"
LINK_TITLE = "Interactive change stories"
DATE = "2026-11-20"
SUMMARY = ("Ask Fraynix to create an interactive change story from a verified comparison. "
           "Inspect the source readings behind a bar and export a single HTML file for offline use.")


def read(path):
    return Path(path).read_text(encoding="utf-8-sig")


def write(path, text):
    Path(path).write_text(text, encoding="utf-8", newline="\n")


def write_json(path, value):
    write(path, json.dumps(value, ensure_ascii=False, indent=2) + "\n")


def item(path):
    path = Path(path)
    binary = path.suffix == ".png"
    data = path.read_bytes() if binary else read(path).encode()
    return {"path": path.as_posix(), "bytes": len(data), "sha256": sha256(data).hexdigest(),
            "hashKind": "binary" if binary else "utf8-lf"}


def prepend(path, text):
    original = read(path)
    if not original.startswith(text):
        write(path, text + original)


def publish(live, mac, summary):
    source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    evidence = json.loads(read(summary))
    app = json.loads(read(live / "result.json"))
    matrix = evidence["matrix"]
    targets = matrix["targets"]
    assert set(targets) == {"windows-x64", "linux-x64", "macos-x64", "macos-arm64"}
    assert all(row["status"] == "success" and row["story"]["passed"] for row in targets.values())
    assert app["passed"] and app["proof"]["browserChecks"] == 24
    assert evidence["legacyExplorer"]["passed"] and evidence["legacyExplorer"]["browserChecks"] == 24
    assert all(row["story"]["changes"] == app["changes"] for row in targets.values())
    proof = json.loads(read(live / "verification.json"))
    assert proof["import"]["sha256"] == sha256((live / "matched-comparison.csv").read_bytes()).hexdigest()
    assert app["proof"]["htmlSha256"] == sha256((live / "change-story.html").read_bytes()).hexdigest()
    assert proof["presentation"] == "change-story-v1"

    ROOT.mkdir(exist_ok=True)
    for name in ("change-story.html", "matched-comparison.csv", "import-receipt.json", "summary-proof.json", "verification.json"):
        shutil.copyfile(live / name, ROOT / name)
    screenshots = []
    for frame, filename, caption in (
        (mac / "desktop-story.png", "mac-story.png", "Chat creates the verified story inside Fraynix on Apple silicon"),
        (live / "offline-story.png", "offline-story.png", "The exported file on Windows shows the source readings behind a selected loss"),
    ):
        assert frame.read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(frame, ROOT / filename)
        screenshots.append({"path": (ROOT / filename).as_posix(), "title": caption})
    write_json(ROOT / "release-proof.json", {
        **evidence, "sourceBaseline": source, "windowsStory": app,
        "screenshots": [item(row["path"]) for row in screenshots],
    })
    tests = evidence["tests"]
    counts = (f"{tests['rootJava']} root Java, {tests['workspaceJava']} focused workspace Java, "
              f"{tests['desktop']} desktop and {tests['archive']} archive tests passed on Windows.")
    result = ("The desktop built and rechecked the story on Windows x64, Apple silicon, Intel Mac and Linux x64. "
              "All four opened the exported HTML with HTTP and HTTPS blocked.")
    write("README-206.md", f"""# Fraynix Agent Workspace · Edition 206

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 205](README-205.md) · [Offline walkthrough](docs/readmes/206/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Fraynix showing an interactive change story on Apple silicon macOS](docs/readmes/206/mac-story.png)

## Try it in Chat

1. Export a verified matched comparison CSV for two routed datasets. See [matched comparison exports](README-201.md) for that step.
2. Send `Summarize the last verified matched comparison CSV.`
3. Send `Build a change story from the verified comparison totals.` Select a bar to see both source readings. Use the unit and status filters to explore the groups.
4. After restarting, send `Recheck the verified change story.`
5. Send `Export the verified change story.` Chat shows where the checked HTML was saved. Open that file in a browser to explore the result offline and download its original evidence.

Each unit has a fixed scale for the full comparison. Losses extend left, gains extend right, and zero has its own mark. Groups without a comparison stay in the source table. The chart works without a native Game Lab display or a local model.

## What passed

| Synthetic release case | Measured result |
| --- | --- |
| Temperature | +18 °F, −9 °F and 0 °F. Net +9 °F across three matches. |
| Length | +25.4 mm and −12.7 mm. Net +12.7 mm across two matches. |
| Other groups | One added, one missing and one duplicate. All three remained visible in the table. |
| Restart and export | The original story passed 24 fresh browser checks after a newer export appeared. Changed source bytes blocked export. An edition 204 lesson reopened with its original HTML hash. |

The tests checked actual bar positions and widths, keyboard selection, filtering and reset, and the downloaded CSV bytes. {result} {counts} [GitHub Actions results]({matrix['url']})

![The exported story shows the exact earlier and later readings for a selected loss](docs/readmes/206/offline-story.png)

```mermaid
flowchart LR
    A[Verified matched CSV] --> B[Java rechecks the comparison and totals]
    B --> C[Canvas story and browser checks]
    C --> D[Saved story pinned to its original export]
    D --> E[Recheck and save single-file HTML]
```

[Download the offline story from GitHub](https://raw.githubusercontent.com/eyeoverthink/Fraynix-Agent-Workspace/readme-206/docs/readmes/206/change-story.html) · [Story source proof](docs/readmes/206/verification.json) · [Platform and test evidence](docs/readmes/206/release-proof.json) · [Reproduction and next candidate](docs/publication206/plan.md)
""")
    main = read("README.md")
    if "README-206.md" not in main:
        position = main.index("[Edition 205")
        end = main.index("\n", main.index("\n\n", position) + 2)
        write("README.md", main[:position] + f"[Edition 206 — {LINK_TITLE}](README-206.md) · [Previous edition](README-205.md)"
              + " · [Offline walkthrough](docs/readmes/206/index.html) · [Full history](docs/readmes/INDEX.md)\n\n"
              + SUMMARY + main[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-206.md" not in index:
        begin = index.index("[Edition 205")
        write("docs/readmes/INDEX.md", index[:begin] + f"[Edition 206 — {LINK_TITLE}](../../README-206.md)"
              + " · [Walkthrough](206/index.html)\n\n" + index[begin:])
    record = (f"## Edition 206 — {TITLE}\n\n{DATE}. {SUMMARY} {result} "
              "The story has its own durable identity beside earlier explorers. Recheck and export reuse its original source, "
              "and an altered comparison was refused before export. See README-206.md and docs/publication206/plan.md.\n\n")
    for path in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(path, record)

    note_name = "214-change-story"
    note = f"docs/obsidian/{note_name}.md"
    canvas = f"docs/obsidian/{note_name}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 206 — {LINK_TITLE}]({note_name}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 206](../../README-206.md) · "
          f"[Workflow map]({note_name}.canvas) · [Reproduction](../publication206/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Chat commands", "workspace-desktop/renderer/classification-routing.js"),
        ("Verified change story", "workspace-engine/src/main/java/fraymus/workspace/RoutedComparisonStory.java"),
        ("Offline chart", "workspace-engine/src/main/resources/workbench/routing-story.js"),
        ("Desktop and export verification", "workspace-desktop/test/change-story-integration.cjs"),
    )]
    nodes = [module["source"] for module in modules] + ["README-206.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "change-story.html").as_posix(), "title": "Open the exported offline story"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Platform and test evidence"},
        {"path": "docs/publication206/plan.md", "title": "Reproduction and next candidate"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/205/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 206 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-205.md", "../../../README-206.md").replace(
        '../204/index.html">Edition 204', '../205/index.html">Edition 205')
    guide = guide.replace('href="verification.json">Verification', 'href="release-proof.json">Verification')
    guide = guide.replace('<a href="#system">System files</a>', '<a href="change-story.html">Open the story</a><a href="#system">System files</a>')
    gallery = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                        f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in screenshots)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: gallery + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)

    manifest = json.loads(subprocess.check_output(["git", "show", f"{BASE}:docs/readmes/editions.json"]))
    assert manifest["latest"] == 205
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()) if path.is_file())]
    manifest["latest"] = 206
    manifest["editions"].append({"number": 206, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-206.md", "markdownSha256": item("README-206.md")["sha256"],
                                 "companion": "docs/readmes/206/index.html", "tag": "readme-206",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/206",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

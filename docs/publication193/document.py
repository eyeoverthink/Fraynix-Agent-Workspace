"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "991541b98290fc2470afcfcd7c08aaf7a6e645ab"
ROOT = Path("docs/readmes/193")
TITLE = "Teach Fraynix to build a checked tool from data"
LINK_TITLE = "Teach and reuse a CSV-to-tool lesson"
SUMMARY = ("Teach a named CSV conversion lesson in Chat, attach fresh rows and build an offline browser tool. "
           "Java verifies the generated tool with both the input data and unseen measurements.")
DATE = "2026-11-19"


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


def publish(live):
    source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    app = json.loads(read(live / "result.json"))
    tests = json.loads(read("build/edition193-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    assert app["checks"]["restartCount"] == 4
    assert app["fresh"]["lesson"]["teacherId"] == app["teacherId"]
    assert all(evidence["passed"] and all(case["java"] == case["browser"] for case in evidence["cases"])
               for evidence in (app["original"], app["fresh"], app["length"]))
    receipt = json.loads(read(live / "fresh-browser-receipt.json"))
    assert receipt["passed"] and len(receipt["checks"]) == 10
    assert sha256((live / "offline-converter.html").read_bytes()).hexdigest() == app["fresh"]["htmlSha256"]
    ROOT.mkdir(exist_ok=True)
    for name in ("fresh-browser-receipt.json", "offline-converter.html"):
        shutil.copyfile(live / name, ROOT / name)

    labels = {
        "chat-teaches-batch-tool.png": "Chat saves the named lesson from a verified CSV tool",
        "fresh-csv-reviewed.png": "Workbench reviews a different CSV under the saved lesson",
        "fresh-batch-verified.png": "Canvas shows the new verified tool and its results",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})

    fresh = app["fresh"]["inputCount"]
    lengths = app["length"]["inputCount"]
    holdouts = len(app["fresh"]["cases"][1]["input"].splitlines()) - 1
    result = (f"After restart, the temperature lesson generated a new tool from {fresh} fresh CSV rows. "
              f"Chromium matched Java on every row and {holdouts} additional measurements. "
              f"A separate {lengths}-row inches tool passed the same gate.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    write("README-193.md", f"""# Fraynix Agent Workspace · Edition 193

[Edition 192](README-192.md) · [Offline walkthrough](docs/readmes/193/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Chat saves the named lesson](docs/readmes/193/chat-teaches-batch-tool.png)

## Try it

1. Send `Build a data-to-tool converter from Celsius to Fahrenheit.` in Chat.
2. In the Workbench, attach a UTF-8 CSV with the header `celsius` and one decimal measurement per row. Choose **Build and verify in browser**.
3. Send `Teach data-to-tool lesson temperature-batch` to teach from the last verified original CSV tool. **Reopen last review** also offers **Prepare teaching request**.
4. Send `Use data-to-tool lesson temperature-batch`, attach new data and confirm. The new CSV must contain a measurement absent from the example.
5. Reopen the verified tool from the Workbench, or use **Save output to project** for a standalone HTML file. Paste new rows into the tool to convert them locally.

![Workbench shows the reviewed lesson and fresh data](docs/readmes/193/fresh-csv-reviewed.png)

## What passed

{result} The desktop run included four restarts. One followed a real artifact-write failure. Fraynix finished the save from the original browser checks after storage was restored. The completed HTML also ran from a local file and converted two new values.

Java rejected an unverified teacher, a teaching attempt from a reuse result, the wrong column, out-of-range input and a copy of the teaching values with new formatting and row order. It also refused a browser receipt borrowed from the teacher and detected modifications to both receipts and the lesson checkpoint.

![The new batch converter and its verified results](docs/readmes/193/fresh-batch-verified.png)

This edition supports single-column CSV batch tools for Celsius to Fahrenheit or inches to millimetres, with 1–24 decimal rows per file. {counts}

[Try the exported tool](docs/readmes/193/offline-converter.html) · [Browser receipt](docs/readmes/193/fresh-browser-receipt.json) · [Verification results](docs/readmes/193/verification.json) · [Implementation and reproduction](docs/publication193/plan.md)
""")
    previous = "[Edition 192 — Teach and reuse a verified Blender repair](README-192.md)"
    lead = f"[Edition 193 — {LINK_TITLE}](README-193.md) · [Previous edition](README-192.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/193/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-193.md" not in index:
        start = index.index("[Edition 192")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 193 — {LINK_TITLE}](../../README-193.md)"
              + " · [Walkthrough](193/index.html)\n\n" + index[start:])
    record = (f"## Edition 193 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "The lesson, pending review, interrupted artifact save and completed proof passed restart checks. "
              "See README-193.md and docs/publication193/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/201-verified-data-tool-lessons.md"
    canvas = "docs/obsidian/201-verified-data-tool-lessons.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 193 — {LINK_TITLE}](201-verified-data-tool-lessons.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 193](../../README-193.md) · "
          "[Workflow map](201-verified-data-tool-lessons.canvas) · [Implementation plan](../publication193/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("CSV tool verifier", java + "FrayDataTool.java"),
        ("Teaching and recovery", java + "RequestWorkbench.java"),
        ("Workbench controls", "workspace-desktop/renderer/request-workbench.js"),
    )]
    nodes = [module["source"] for module in modules] + ["README-193.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "fresh-browser-receipt.json").as_posix(), "title": "Original Chromium receipt"},
        {"path": (ROOT / "offline-converter.html").as_posix(), "title": "Runnable offline tool"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/192/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 193 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-192.md", "../../../README-193.md").replace(
        '../191/index.html">Edition 191', '../192/index.html">Edition 192')
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
    assert manifest["latest"] == 192
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 193
    manifest["editions"].append({"number": 193, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-193.md", "markdownSha256": item("README-193.md")["sha256"],
                                 "companion": "docs/readmes/193/index.html", "tag": "readme-193",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/193",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition193-live"))

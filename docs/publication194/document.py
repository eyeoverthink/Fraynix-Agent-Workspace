"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "7647b1387540c22cdc5aa1c63c85431eeaa55b57"
ROOT = Path("docs/readmes/194")
TITLE = "Teach a converter to find a named CSV column"
LINK_TITLE = "Named-column CSV lessons"
SUMMARY = ("Choose a named measurement column in Chat and teach a conversion from its verified result. "
           "Reuse the lesson on reordered CSV and append the checked result without losing the other cells.")
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
    tests = json.loads(read("build/edition194-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    assert app["checks"]["restartCount"] == 3 and len(app["legacy"]) == 2
    assert app["fresh"]["lesson"]["teacherId"] == app["teacherId"]
    assert all(evidence["passed"] and all(case["java"] == case["browser"] for case in evidence["cases"])
               for evidence in (app["teacher"], app["fresh"], app["length"]))
    receipt = json.loads(read(live / "browser-receipt.json"))
    assert receipt["passed"] and len(receipt["checks"]) == 16
    assert sha256((live / "offline-table-tool.html").read_bytes()).hexdigest() == app["fresh"]["htmlSha256"]
    ROOT.mkdir(exist_ok=True)
    for name in ("browser-receipt.json", "offline-table-tool.html"):
        shutil.copyfile(live / name, ROOT / name)

    labels = {
        "chat-teaches-named-column.png": "Chat saves a lesson for the probe_c column",
        "review-reordered-table.png": "Workbench shows the selected header for the reordered file",
        "reordered-table-verified.png": "Canvas preserves the existing cells and displays the appended result",
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
    result = (f"The temperature lesson converted {fresh} fresh rows when the measurement column moved from third "
              f"to first. Java checked the full output table and the browser passed 16 checks. "
              f"A separate eight-column inches tool converted {lengths} rows.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    timing = app["timing"]
    table = "\n".join(
        f"| {label} | {run['rows']} × {run['columns']} | {run['attachMs']} ms | {run['buildVerifyMs']} ms |"
        for key, label in (("teacher", "Teaching example"), ("reordered", "Reused lesson"), ("boundary", "Width and row limit"))
        for run in [timing[key]]
    )
    write("README-194.md", f"""# Fraynix Agent Workspace · Edition 194

[Edition 193](README-193.md) · [Offline walkthrough](docs/readmes/194/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Chat saves the named-column lesson](docs/readmes/194/chat-teaches-named-column.png)

## Try it

1. Send `Build a data-to-tool converter from Celsius to Fahrenheit using column probe_c.` in Chat.
2. Attach a UTF-8 CSV with a `probe_c` header and any other unique headers. Choose **Build and verify in browser**.
3. Send `Teach data-to-tool lesson probe-batch`, then `Use data-to-tool lesson probe-batch`. Attach a fresh CSV. The column can move to a different position.
4. Reopen the verified tool or choose **Save output to project** for standalone HTML. Paste a table into the exported tool to append `fahrenheit` or `millimetres`.

![Workbench shows the measurement column of the reordered CSV](docs/readmes/194/review-reordered-table.png)

## What passed

{result} Java and Chromium compared complete tables, including text with commas, quotes, embedded line breaks and Unicode. The app rejected missing and duplicate headers, uneven row widths, an output-column conflict and a file containing only the teaching example's values. The lesson, pending review and completed proof survived three separate restarts. The exported HTML converted a new row offline. Two verified edition 193 artifacts rechecked against their original evidence.

![The verified output includes the existing cells and the new result column](docs/readmes/194/reordered-table-verified.png)

## Local timings

| Scenario | Rows × input columns | Attach and check | Build, save and verify |
| --- | ---: | ---: | ---: |
{table}

Rechecking the completed lesson after restart took {timing['restartVerifyMs']} ms. Each row reports one observed run on the development machine with synthetic inputs.

The workflow supports either registered conversion on 1–24 rows with up to eight input columns. The measured column name is case-sensitive. {counts}

[Run the offline example](docs/readmes/194/offline-table-tool.html) · [Browser receipt](docs/readmes/194/browser-receipt.json) · [Verification and timings](docs/readmes/194/verification.json) · [Implementation and reproduction](docs/publication194/plan.md)
""")
    previous = "[Edition 193 — Teach and reuse a CSV-to-tool lesson](README-193.md)"
    lead = f"[Edition 194 — {LINK_TITLE}](README-194.md) · [Previous edition](README-193.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/194/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-194.md" not in index:
        start = index.index("[Edition 193")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 194 — {LINK_TITLE}](../../README-194.md)"
              + " · [Walkthrough](194/index.html)\n\n" + index[start:])
    record = (f"## Edition 194 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              f"The three measured builds took {timing['teacher']['buildVerifyMs']}, {timing['reordered']['buildVerifyMs']} and {timing['boundary']['buildVerifyMs']} ms. Two edition 193 results reverified. "
              "See README-194.md and docs/publication194/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/202-named-column-lessons.md"
    canvas = "docs/obsidian/202-named-column-lessons.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 194 — {LINK_TITLE}](202-named-column-lessons.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 194](../../README-194.md) · "
          "[Workflow map](202-named-column-lessons.canvas) · [Implementation plan](../publication194/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Named column parser", java + "FrayTableInput.java"),
        ("CSV tool verifier", java + "FrayDataTool.java"),
        ("Teaching and recovery", java + "RequestWorkbench.java"),
        ("Workbench controls", "workspace-desktop/renderer/request-workbench.js"),
    )]
    nodes = [module["source"] for module in modules] + ["README-194.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "browser-receipt.json").as_posix(), "title": "Original Chromium receipt"},
        {"path": (ROOT / "offline-table-tool.html").as_posix(), "title": "Runnable offline tool"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/193/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 194 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-193.md", "../../../README-194.md").replace(
        '../192/index.html">Edition 192', '../193/index.html">Edition 193')
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
    assert manifest["latest"] == 193
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 194
    manifest["editions"].append({"number": 194, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-194.md", "markdownSha256": item("README-194.md")["sha256"],
                                 "companion": "docs/readmes/194/index.html", "tag": "readme-194",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/194",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition194-live"))

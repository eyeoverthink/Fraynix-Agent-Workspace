"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "bdadcdca5818b5efacae0f505dd6736e197ba419"
ROOT = Path("docs/readmes/204")
TITLE = "Teach and reuse a verified comparison explorer"
LINK_TITLE = "Reusable comparison explorer"
SUMMARY = ("Save a verified explorer as a named lesson and reuse it on fresh comparison data. "
           "Check the lesson, the original teaching evidence and each new result.")
DATE = "2026-11-20"


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
    tests = json.loads(read("build/edition204-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values()) and app["offlineViews"] == 30
    lesson = app["lesson"]
    assert lesson["name"] == "field-audit" and lesson["teacherArtifact"] == app["teacher"]["artifact"]
    ROOT.mkdir(exist_ok=True)
    write_json(ROOT / "lesson.json", lesson)
    for case, run in app["runs"].items():
        proof, report, browser = run["proof"], run["report"], run["browserReceipt"]
        receipt, summary = proof["import"], proof["summary"]
        assert proof["lesson"] == lesson and receipt["sha256"] != lesson["teacherCsvSha256"]
        assert report["comparisonExportId"] == receipt["id"] and browser["passed"]
        assert report["browserChecks"] == proof["browserChecks"] == len(browser["checks"]) == 24
        directory = ROOT / case
        directory.mkdir(exist_ok=True)
        for name, digest in (("matched-comparison.csv", receipt["sha256"]),
                             ("comparison-explorer.html", report["htmlSha256"]),
                             ("summary-proof.json", summary["proofSha256"])):
            assert sha256((live / case / name).read_bytes()).hexdigest() == digest
        assert json.loads(read(live / case / "import-receipt.json")) == receipt
        assert json.loads(read(live / case / "verification.json")) == proof
        for name in ("comparison-explorer.html", "matched-comparison.csv", "import-receipt.json",
                     "summary-proof.json", "verification.json"):
            shutil.copyfile(live / case / name, directory / name)
    labels = {
        "temperature-only.png": "The reused lesson shows zero net temperature change and eighteen degrees of total movement",
        "mixed-unmatched.png": "The reused lesson keeps the small temperature change and the unmatched length groups separate",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused workspace Java tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    result = "Two fresh comparisons each passed 24 browser checks. Independent Python calculations agreed with Chromium on all 30 offline filter views."

    write("README-204.md", f"""# Fraynix Agent Workspace · Edition 204

[Edition 203](README-203.md) · [Offline walkthrough](docs/readmes/204/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The named lesson reuses the explorer on a Fahrenheit-only comparison](docs/readmes/204/temperature-only.png)

## Try it

1. Build an original explorer through [edition 203](README-203.md).
2. Send `Teach comparison explorer lesson field-audit`. Fraynix reruns the explorer checks and saves the procedure with its teaching artifact and source hashes.
3. Export a fresh matched comparison CSV and send `Use comparison explorer lesson field-audit`. One request checks both the lesson and the new source, builds or reuses its verified summary, then creates and checks the new explorer.
4. After restart, send `Recheck comparison explorer lesson field-audit` to reopen that named lesson's last result with its original comparison export.

The lesson uses the newest export when you apply it. A new copy of the teaching CSV has the same content hash and is refused. The saved lesson binds its name to the verified original artifact. Reuse records the lesson alongside the new CSV, receipt and summary. Java selects a populated unit and status to test on each new input.

## What passed

{result}

| Fresh case | Verified result |
| --- | --- |
| Fahrenheit only | Five groups. A rise of 9 °F and a fall of 9 °F cancel, leaving a **0 °F** net change and **18 °F** total absolute change. |
| Mixed units | Six groups. The explorer preserves **+0.00018 °F**. The millimetre groups have no matched pairs, so the length totals are unavailable. |

The altered teaching receipt was rejected before creating a summary or explorer. Java also refused a changed CSV whose on-disk receipt and manifest were rewritten to match. After a newer export appeared, a restart rechecked the previous result against its original input and retained both the saved lesson and the earlier browser receipt.

![The mixed case preserves the tiny temperature change and the millimetre groups without matches](docs/readmes/204/mixed-unmatched.png)

The screenshots show the Java desktop on {DATE} with synthetic data. The two packaged explorers ran offline at a narrow viewport, with exact downloads of their original inputs. {counts}

```mermaid
flowchart LR
    A[Original verified explorer] --> B[Named lesson and teaching proof]
    B --> C[Check fresh comparison and original lesson]
    D[Fresh verified comparison CSV] --> C
    C --> E[Rebuild or reuse the verified summary]
    E --> F[Check the new explorer and save its evidence]
```

[Saved lesson](docs/readmes/204/lesson.json) · [Temperature-only explorer](docs/readmes/204/temperature-only/comparison-explorer.html) · [Mixed-unit explorer](docs/readmes/204/mixed-unmatched/comparison-explorer.html) · [Verification evidence](docs/readmes/204/verification.json) · [Reproduction and next candidate](docs/publication204/plan.md)
""")
    previous = "[Edition 203 — Verified comparison explorer](README-203.md)"
    lead = f"[Edition 204 — {LINK_TITLE}](README-204.md) · [Previous edition](README-203.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/204/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-204.md" not in index:
        start = index.index("[Edition 203")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 204 — {LINK_TITLE}](../../README-204.md)"
              + " · [Walkthrough](204/index.html)\n\n" + index[start:])
    record = (f"## Edition 204 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "The stored lesson rejected a copied teaching CSV and changed evidence. The original run rechecked after restart. "
              "See README-204.md and docs/publication204/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/212-reusable-comparison-explorer.md"
    canvas = "docs/obsidian/212-reusable-comparison-explorer.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 204 — {LINK_TITLE}](212-reusable-comparison-explorer.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 204](../../README-204.md) · "
          "[Workflow map](212-reusable-comparison-explorer.canvas) · [Implementation plan](../publication204/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Verified explorer lessons", java + "ComparisonExplorerLessons.java"),
        ("Fresh source and teaching verification", java + "RoutedDashboard.java"),
        ("Offline explorer", "workspace-engine/src/main/resources/workbench/routing-comparison-explorer.html"),
    )]
    nodes = [module["source"] for module in modules] + ["README-204.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "lesson.json").as_posix(), "title": "Saved field-audit lesson"},
        {"path": (ROOT / "temperature-only" / "comparison-explorer.html").as_posix(), "title": "Temperature-only explorer"},
        {"path": (ROOT / "mixed-unmatched" / "comparison-explorer.html").as_posix(), "title": "Mixed-unit explorer with no length matches"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/203/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 204 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-203.md", "../../../README-204.md").replace(
        '../202/index.html">Edition 202', '../203/index.html">Edition 203')
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
    assert manifest["latest"] == 203
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(path for path in ROOT.rglob('*') if path.is_file()))]
    manifest["latest"] = 204
    manifest["editions"].append({"number": 204, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-204.md", "markdownSha256": item("README-204.md")["sha256"],
                                 "companion": "docs/readmes/204/index.html", "tag": "readme-204",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/204",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition204-live"))

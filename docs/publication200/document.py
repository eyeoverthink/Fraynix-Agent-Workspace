"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "d96c265656bf7905681366cd71e29c054485d7e7"
ROOT = Path("docs/readmes/200")
TITLE = "Compare verified routed rows by key and unit"
LINK_TITLE = "Verified key and unit comparison"
SUMMARY = ("Compare rows from two verified routing exports in Chat. Match an original key and output unit, "
           "then recheck the exact rows and source files after restart.")
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
    tests = json.loads(read("build/edition200-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    proof, receipt, saved = app["proof"], app["browserReceipt"], app["saved"]
    assert receipt["passed"] and len(receipt["checks"]) == proof["browserChecks"] == saved["browserChecks"] == 9
    assert proof["keyColumn"] == saved["keyColumn"] == "record ID"
    sources = {
        "earlier-routed-results.csv": saved["earlierCsvSha256"],
        "later-routed-results.csv": saved["csvSha256"],
        "key-comparison.html": saved["htmlSha256"],
    }
    ROOT.mkdir(exist_ok=True)
    for name, digest in sources.items():
        assert sha256((live / name).read_bytes()).hexdigest() == digest
        shutil.copyfile(live / name, ROOT / name)
    write_json(ROOT / "comparison-browser-receipt.json", receipt)
    labels = {
        "matched-key-and-unit.png": "The desktop compares rows using record ID and each row's output unit",
        "matched-rechecked-after-restart.png": "The same table passes fresh Java and browser checks after desktop restart",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})
    totals = proof["counts"]
    assert totals == {"Matched": 4, "Added": 2, "Missing": 2, "Duplicate": 2}
    assert proof["earlier"]["rows"] == proof["later"]["rows"] == 9
    result = (f"Two fresh nine-row exports produced {totals['Matched']} matched, {totals['Added']} added, "
              f"{totals['Missing']} missing and {totals['Duplicate']} duplicate key-unit groups.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")

    write("README-200.md", f"""# Fraynix Agent Workspace · Edition 200

[Edition 199](README-199.md) · [Offline walkthrough](docs/readmes/200/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The matched report keeps duplicate source rows and separates units](docs/readmes/200/matched-key-and-unit.png)

## Try it

1. Choose **Export verified CSV** in two completed routing plans. The comparison uses the latest two exports from distinct plans.
2. Send `Compare the last two verified routed CSVs by key "record ID"`. Use the original column's exact name. Quote names that contain spaces. Blank key values cause the comparison to stop.
3. Read the **Matched**, **Added**, **Missing** and **Duplicate** groups. Each side shows the source row number and its verified value. A difference appears for a key and unit with exactly one row on each side.
4. After restarting, send `Recheck the matched routed CSV comparison`. Fraynix rechecks the original files and key against Java.

Keys are case-sensitive. The report preserves earlier key order, then appends groups first seen in the later file. A measurement that changes unit appears in two separate groups. A duplicate on either side retains all rows for that group and leaves the change blank.

## What passed

{result} The later export moved both rows and columns. The same key **rail** matched independently in both units, with changes of **−12.7 mm** and **+4.95 °F**. The key **switch** was missing in Fahrenheit and added in millimetres. The report retained both earlier readings for **tank** and both later readings for **late-dup**.

Java revalidated both sources through their approved routing plans and checked every rendered cell against the browser's independent results. The same saved artifact passed nine fresh browser checks after restart and kept its original receipt, even after a newer export was written. The test rejected changes to either CSV, a substituted key, and an altered duplicate reading in the browser results. The edition 199 comparison also passed a fresh recheck.

![The original key and exports passed nine fresh checks after desktop restart](docs/readmes/200/matched-rechecked-after-restart.png)

The two screenshots show the actual desktop workbench on {DATE} with synthetic fixtures. The sample opened offline and both downloads matched the verified files. {counts}

```mermaid
flowchart LR
    CSV[Two approved routing exports] --> J[Java source and row checks]
    CSV --> B[Independent browser comparison]
    J --> P[Verified report and receipt]
    B --> P
    P --> R[Recheck the same key and exports after restart]
```

[Sample comparison](docs/readmes/200/key-comparison.html) · [Earlier CSV](docs/readmes/200/earlier-routed-results.csv) · [Later CSV](docs/readmes/200/later-routed-results.csv) · [Browser receipt](docs/readmes/200/comparison-browser-receipt.json) · [Verification record](docs/readmes/200/verification.json) · [Reproduction and next candidate](docs/publication200/plan.md)
""")
    previous = "[Edition 199 — Verified routing comparison](README-199.md)"
    lead = f"[Edition 200 — {LINK_TITLE}](README-200.md) · [Previous edition](README-199.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/200/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-200.md" not in index:
        start = index.index("[Edition 199")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 200 — {LINK_TITLE}](../../README-200.md)"
              + " · [Walkthrough](200/index.html)\n\n" + index[start:])
    record = (f"## Edition 200 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "After restart, the saved key and original pair passed nine fresh browser checks even after a new export was saved. "
              "See README-200.md and docs/publication200/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/208-verified-routed-row-comparison.md"
    canvas = "docs/obsidian/208-verified-routed-row-comparison.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 200 — {LINK_TITLE}](208-verified-routed-row-comparison.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 200](../../README-200.md) · "
          "[Workflow map](208-verified-routed-row-comparison.canvas) · [Implementation plan](../publication200/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Key and unit matching", java + "RoutedKeyComparisonReport.java"),
        ("Comparison and source verification", java + "RoutedDashboard.java"),
        ("Offline browser comparison", "workspace-engine/src/main/resources/workbench/routing-key-comparison.html"),
    )]
    nodes = [module["source"] for module in modules] + ["README-200.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "key-comparison.html").as_posix(), "title": "Open the sample comparison"},
        {"path": (ROOT / "comparison-browser-receipt.json").as_posix(), "title": "Original comparison browser receipt"},
        {"path": (ROOT / "earlier-routed-results.csv").as_posix(), "title": "Earlier verified CSV"},
        {"path": (ROOT / "later-routed-results.csv").as_posix(), "title": "Later verified CSV"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/199/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 200 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-199.md", "../../../README-200.md").replace(
        '../198/index.html">Edition 198', '../199/index.html">Edition 199')
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
    assert manifest["latest"] == 199
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 200
    manifest["editions"].append({"number": 200, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-200.md", "markdownSha256": item("README-200.md")["sha256"],
                                 "companion": "docs/readmes/200/index.html", "tag": "readme-200",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/200",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition200-live"))

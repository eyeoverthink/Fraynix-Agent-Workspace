"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "1bcb9aa848349fc6767c663ad738332a70f95e05"
ROOT = Path("docs/readmes/203")
TITLE = "Build an offline explorer from verified comparison totals"
LINK_TITLE = "Verified comparison explorer"
SUMMARY = ("Turn a checked comparison summary into a working offline tool. Filter source rows by status and unit, "
           "then recheck the same inputs after restart.")
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
    tests = json.loads(read("build/edition203-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    report, proof, browser = app["report"], app["proof"], app["browserReceipt"]
    receipt, summary = proof["import"], proof["summary"]
    assert report["comparisonExportId"] == receipt["id"]
    assert report["browserChecks"] == proof["browserChecks"] == len(browser["checks"]) == 24
    assert app["offlineViews"] == 15 and browser["passed"]
    assert [view["groups"] for view in proof["views"]] == [10, 4, 1, 0]
    assert json.loads(read(live / "verification.json")) == proof
    assert json.loads(read(live / "import-receipt.json")) == receipt
    assert json.loads(read(live / "summary-proof.json"))["import"] == receipt
    for name, digest in (("matched-comparison.csv", receipt["sha256"]),
                         ("comparison-explorer.html", report["htmlSha256"]),
                         ("summary-proof.json", summary["proofSha256"])):
        assert sha256((live / name).read_bytes()).hexdigest() == digest
    ROOT.mkdir(exist_ok=True)
    for name in ("matched-comparison.csv", "comparison-explorer.html", "import-receipt.json", "summary-proof.json"):
        shutil.copyfile(live / name, ROOT / name)
    shutil.copyfile(live / "verification.json", ROOT / "explorer-proof.json")
    labels = {
        "comparison-explorer-duplicates.png": "After a deliberately blocked test save, the recovered explorer filters the tank readings",
        "comparison-explorer-rechecked-after-restart.png": "The saved explorer passes fresh checks with its original source after restart",
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
    result = "The workbench recovered a blocked save. The offline explorer matched an independent decimal check for all 15 filter combinations."

    write("README-203.md", f"""# Fraynix Agent Workspace · Edition 203

[Edition 202](README-202.md) · [Offline walkthrough](docs/readmes/203/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The recovered explorer filters the duplicate length group after a deliberately blocked test save](docs/readmes/203/comparison-explorer-duplicates.png)

## Try it

1. Run the verified CSV summary from [edition 202](README-202.md).
2. Send `Build an explorer from the verified comparison totals`. Fraynix uses the last verified summary from Chat and checks the report, original CSV and receipts before saving the explorer.
3. Filter by **All units**, **°F**, or **mm**, then choose a status. The table shows the exact source row numbers. The totals change to match the selection and stay separate by unit.
4. Use **Reset** to restore every group. Under **Verified inputs**, download the original CSV, import receipt and summary proof.
5. After restart, send `Recheck the verified comparison explorer`. Fraynix checks the same explorer and source again.

## What passed

{result} The recorded fixture had ten groups. The **mm** filter showed four. Adding **Duplicate** left one group and preserved all three source readings for that key. Selecting **Missing** while still on **mm** showed an empty result. Reset restored the full comparison.

| Filter | Visible groups |
| --- | ---: |
| All units and statuses | 10 |
| mm, all statuses | 4 |
| mm, Duplicate | 1 |
| mm, Missing | 0 |

The workbench restarted after a deliberately blocked write and completed the pending save with its original browser evidence. A later restart reran all 24 checks with the original export even after a newer summary had been recorded. The tests rejected a changed parent browser receipt, modified CSV bytes and an incorrect value in the filtered browser output.

![The verified explorer reopened on its original input after restart](docs/readmes/203/comparison-explorer-rechecked-after-restart.png)

The screenshots show the Java desktop running with synthetic data on {DATE}. The standalone HTML ran offline at a narrow viewport. All three evidence downloads matched the stored bytes. {counts}

```mermaid
flowchart LR
    A[Verified summary, original CSV and receipt] --> B[Check original source and summary proof]
    B --> C[Create interactive offline explorer]
    C --> D[Check filters, rows and totals against Java]
    D --> E[Save and recheck the same inputs after restart]
```

[Sample explorer](docs/readmes/203/comparison-explorer.html) · [Comparison CSV](docs/readmes/203/matched-comparison.csv) · [Import receipt](docs/readmes/203/import-receipt.json) · [Summary proof](docs/readmes/203/summary-proof.json) · [Verification evidence](docs/readmes/203/verification.json) · [Reproduction and next candidate](docs/publication203/plan.md)
""")
    previous = "[Edition 202 — Verified comparison CSV totals](README-202.md)"
    lead = f"[Edition 203 — {LINK_TITLE}](README-203.md) · [Previous edition](README-202.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/203/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-203.md" not in index:
        start = index.index("[Edition 202")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 203 — {LINK_TITLE}](../../README-203.md)"
              + " · [Walkthrough](203/index.html)\n\n" + index[start:])
    record = (f"## Edition 203 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "A blocked artifact save recovered after restart. The original source passed 24 fresh browser checks after a second restart. "
              "See README-203.md and docs/publication203/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/211-verified-comparison-explorer.md"
    canvas = "docs/obsidian/211-verified-comparison-explorer.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 203 — {LINK_TITLE}](211-verified-comparison-explorer.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 203](../../README-203.md) · "
          "[Workflow map](211-verified-comparison-explorer.canvas) · [Implementation plan](../publication203/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Explorer filters", java + "RoutedComparisonExplorer.java"),
        ("Comparison and source verification", java + "RoutedDashboard.java"),
        ("Offline explorer", "workspace-engine/src/main/resources/workbench/routing-comparison-explorer.html"),
    )]
    nodes = [module["source"] for module in modules] + ["README-203.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "comparison-explorer.html").as_posix(), "title": "Open the offline explorer"},
        {"path": (ROOT / "matched-comparison.csv").as_posix(), "title": "Original comparison CSV"},
        {"path": (ROOT / "import-receipt.json").as_posix(), "title": "Original import receipt"},
        {"path": (ROOT / "summary-proof.json").as_posix(), "title": "Parent summary proof"},
        {"path": (ROOT / "explorer-proof.json").as_posix(), "title": "Checked filter views"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/202/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 203 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-202.md", "../../../README-203.md").replace(
        '../201/index.html">Edition 201', '../202/index.html">Edition 202')
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
    assert manifest["latest"] == 202
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 203
    manifest["editions"].append({"number": 203, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-203.md", "markdownSha256": item("README-203.md")["sha256"],
                                 "companion": "docs/readmes/203/index.html", "tag": "readme-203",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/203",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition203-live"))

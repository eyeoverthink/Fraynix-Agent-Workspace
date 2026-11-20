"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "02ee28cbf9ad8052e3654695a416e68443b01112"
ROOT = Path("docs/readmes/202")
TITLE = "Use a verified comparison CSV in a follow-on Chat task"
LINK_TITLE = "Verified comparison CSV totals"
SUMMARY = ("Attach a verified matched comparison CSV from the project and calculate changes separately by output unit. "
           "Check the receipt and original sources again after restart.")
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
    tests = json.loads(read("build/edition202-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    report, proof, browser = app["report"], app["proof"], app["browserReceipt"]
    receipt = proof["import"]
    assert report["comparisonExportId"] == receipt["id"] and receipt["source"]["groups"] == 10
    assert report["browserChecks"] == proof["browserChecks"] == len(browser["checks"]) == 13
    assert browser["passed"] and json.loads(read(live / "verification.json")) == proof
    assert json.loads(read(live / "import-receipt.json")) == receipt
    for name, digest in (("matched-comparison.csv",receipt["sha256"]),("comparison-totals.html",report["htmlSha256"])):
        assert sha256((live / name).read_bytes()).hexdigest() == digest
    ROOT.mkdir(exist_ok=True)
    for name in ("matched-comparison.csv","comparison-totals.html","import-receipt.json"):
        shutil.copyfile(live / name, ROOT / name)
    shutil.copyfile(live / "verification.json", ROOT / "totals-proof.json")
    labels = {
        "comparison-totals-by-unit.png": "The desktop calculates temperature and length changes in separate groups",
        "comparison-totals-rechecked-after-restart.png": "The original import passes a fresh check after desktop restart",
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
    assert [row["netDelta"] for row in proof["totals"]] == ["7.2", "-12.7"]
    result = "The recorded import contains ten groups with net changes of +7.2 °F and −12.7 mm."
    table = "\n".join(
        f"| {row['unit']} | {row['matched']} | {row['added']} | {row['missing']} | {row['duplicate']} | "
        + ("+" if not row["netDelta"].startswith("-") else "") + f"{row['netDelta']} | {row['absoluteDelta']} |"
        for row in proof["totals"]
    )

    write("README-202.md", f"""# Fraynix Agent Workspace · Edition 202

[Edition 201](README-201.md) · [Offline walkthrough](docs/readmes/202/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The desktop summarizes the imported CSV by output unit](docs/readmes/202/comparison-totals-by-unit.png)

## Try it

1. Export a matched comparison from [edition 201](README-201.md).
2. Send `Summarize the last verified matched comparison CSV`. Fraynix attaches the newest eligible export in the current project and opens the verified report.
3. Under **Source and receipt**, find the export ID, file hash and links to both original files.
4. After restart, send `Recheck the matched comparison CSV summary`. The recheck opens the saved report with its original export, even if a newer export exists.

The report counts matched, added, missing and duplicate groups for each unit. Net change sums the signed differences. Total absolute change sums their magnitudes. Only matched groups contribute to either total. A unit with zero matches shows no total.

## What passed

{result} The desktop checked the file and export manifest. Java reconstructed the export from the original verified comparison and routing plans. Chromium independently parsed the CSV and calculated the totals. A separate Python decimal check agreed with the report.

| Unit | Matched | Added | Missing | Duplicate | Net change | Total absolute change |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
{table}

After restart, the original export and artifact passed 13 fresh browser checks. The test rejected a conflicting receipt, a changed parent browser receipt, a forged browser-evidence hash, changed CSV bytes with updated hashes, and a browser result with mixed units.

![The same export passes fresh verification after restart](docs/readmes/202/comparison-totals-rechecked-after-restart.png)

The screenshots show the Java desktop running with synthetic data on {DATE}. Both source downloads matched their stored bytes in the offline report. {counts}

```mermaid
flowchart LR
    A[Matched CSV and export receipt] --> B[Verify original comparison and inputs]
    B --> C[Java and Chromium calculate totals]
    C --> D[Save report with original CSV and receipt]
```

[Open the sample totals](docs/readmes/202/comparison-totals.html) · [Imported CSV](docs/readmes/202/matched-comparison.csv) · [Import receipt](docs/readmes/202/import-receipt.json) · [Verification evidence](docs/readmes/202/verification.json) · [Reproduction and next candidate](docs/publication202/plan.md)
""")
    previous = "[Edition 201 — Verified matched CSV export](README-201.md)"
    lead = f"[Edition 202 — {LINK_TITLE}](README-202.md) · [Previous edition](README-201.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/202/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-202.md" not in index:
        start = index.index("[Edition 201")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 202 — {LINK_TITLE}](../../README-202.md)"
              + " · [Walkthrough](202/index.html)\n\n" + index[start:])
    record = (f"## Edition 202 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "The original import and artifact passed 13 new browser checks after restart. "
              "See README-202.md and docs/publication202/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/210-verified-comparison-csv-totals.md"
    canvas = "docs/obsidian/210-verified-comparison-csv-totals.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 202 — {LINK_TITLE}](210-verified-comparison-csv-totals.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 202](../../README-202.md) · "
          "[Workflow map](210-verified-comparison-csv-totals.canvas) · [Implementation plan](../publication202/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Unit totals", java + "RoutedComparisonTotals.java"),
        ("Comparison and source verification", java + "RoutedDashboard.java"),
        ("Independent browser totals", "workspace-engine/src/main/resources/workbench/routing-comparison-totals.html"),
    )]
    nodes = [module["source"] for module in modules] + ["README-202.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "comparison-totals.html").as_posix(), "title": "Open the offline totals report"},
        {"path": (ROOT / "matched-comparison.csv").as_posix(), "title": "Imported comparison CSV"},
        {"path": (ROOT / "import-receipt.json").as_posix(), "title": "Verified import receipt"},
        {"path": (ROOT / "totals-proof.json").as_posix(), "title": "Totals proof"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/201/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 202 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-201.md", "../../../README-202.md").replace(
        '../200/index.html">Edition 200', '../201/index.html">Edition 201')
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
    assert manifest["latest"] == 201
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 202
    manifest["editions"].append({"number": 202, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-202.md", "markdownSha256": item("README-202.md")["sha256"],
                                 "companion": "docs/readmes/202/index.html", "tag": "readme-202",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/202",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition202-live"))

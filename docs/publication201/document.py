"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "9c5c19293ab05cad561816f987f44c93f7f6a436"
ROOT = Path("docs/readmes/201")
TITLE = "Export a verified matched comparison from Chat"
LINK_TITLE = "Verified matched CSV export"
SUMMARY = ("Save a checked key-and-unit comparison as a CSV, with every status and source row. "
           "After restart, Fraynix rechecks the same comparison and reproduces the exact file.")
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
    tests = json.loads(read("build/edition201-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    first, second, report = app["first"], app["second"], app["report"]
    proof = json.loads(read(live / "source-comparison-proof.json"))
    assert first["id"] != second["id"] and first["source"] == second["source"]
    assert second["source"]["comparisonId"] == report["id"]
    assert second["source"]["browserChecks"] == report["browserChecks"] == 9
    assert second["source"]["groups"] == len(proof["outcomes"]) == 10
    csv_bytes = (live / "matched-comparison.csv").read_bytes()
    assert len(csv_bytes) == first["bytes"] == second["bytes"]
    assert sha256(csv_bytes).hexdigest() == first["sha256"] == second["sha256"]
    ROOT.mkdir(exist_ok=True)
    for name in ("matched-comparison.csv", "source-comparison-proof.json"):
        shutil.copyfile(live / name, ROOT / name)
    write_json(ROOT / "export-receipt.json", second)
    labels = {
        "comparison-csv-export.png": "Chat saves a verified CSV from the matched report",
        "comparison-csv-export-restarted.png": "Chat repeats the export after restarting and rechecking the original comparison",
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
    result = ("The exported file contains four matched, two added, two missing and two duplicate groups "
              "from the two synthetic routing exports.")

    write("README-201.md", f"""# Fraynix Agent Workspace · Edition 201

[Edition 200](README-200.md) · [Offline walkthrough](docs/readmes/201/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The desktop reports the location of the verified CSV](docs/readmes/201/comparison-csv-export.png)

## Try it

1. Build a matched comparison in Chat. For the sample use `Compare the last two verified routed CSVs by key "record ID"`.
2. Send `Export the matched routed CSV comparison`. Chat shows where the CSV was saved in the current project's exports folder. The matching receipt sits beside it.
3. After restarting, send the same export request. Fraynix reruns the checks with the original key and pair and saves a new copy.

## CSV format

| Column | Meaning |
| --- | --- |
| `key_json` | Exact key as a JSON string so values such as `0007` keep their original form |
| `unit` | Verified output unit |
| `status` | Matched, Added, Missing or Duplicate |
| `earlier_readings` and `later_readings` | JSON arrays with each original source row number and exact decimal value |
| `delta` | Later value minus earlier value for a unique match. Other groups have an empty cell. |

The CSV follows the comparison's group order. Every duplicate reading remains with its original row number. The receipt records the artifact, original key, both export IDs and hashes, the fresh browser evidence hash, and the CSV hash.

## What passed

{result} An independent CSV reader checked all ten groups against the saved comparison. The two exports had identical SHA-256 hashes after restart. The second came from the same comparison even after a newer source export was saved.

Java checked the saved comparison and nine fresh browser results before each export. The test refused missing browser evidence, a changed browser result, a modified retained receipt, a tampered source with a rewritten receipt, and changed CSV bytes in transit. The refusals produced no additional CSV.

![After restart the same comparison exports to a new verified file](docs/readmes/201/comparison-csv-export-restarted.png)

The screenshots show the Java desktop running with synthetic data on {DATE}. {counts}

```mermaid
flowchart LR
    A[Saved matched comparison] --> B[Recheck original sources and live browser results]
    B --> C[Java writes and checks CSV round trip]
    C --> D[Desktop writes verified bytes and receipt]
```

[Download the sample CSV](docs/readmes/201/matched-comparison.csv) · [Export receipt](docs/readmes/201/export-receipt.json) · [Source comparison proof](docs/readmes/201/source-comparison-proof.json) · [Verification record](docs/readmes/201/verification.json) · [Reproduction and next candidate](docs/publication201/plan.md)
""")
    previous = "[Edition 200 — Verified key and unit comparison](README-200.md)"
    lead = f"[Edition 201 — {LINK_TITLE}](README-201.md) · [Previous edition](README-200.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/201/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-201.md" not in index:
        start = index.index("[Edition 200")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 201 — {LINK_TITLE}](../../README-201.md)"
              + " · [Walkthrough](201/index.html)\n\n" + index[start:])
    record = (f"## Edition 201 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "The two exports had identical bytes after restart and both referenced the original comparison. "
              "See README-201.md and docs/publication201/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/209-verified-matched-csv-export.md"
    canvas = "docs/obsidian/209-verified-matched-csv-export.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 201 — {LINK_TITLE}](209-verified-matched-csv-export.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 201](../../README-201.md) · "
          "[Workflow map](209-verified-matched-csv-export.canvas) · [Implementation plan](../publication201/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Comparison CSV format", java + "RoutedKeyCsv.java"),
        ("Comparison and source verification", java + "RoutedDashboard.java"),
        ("Verified desktop write", "workspace-desktop/exports.cjs"),
    )]
    nodes = [module["source"] for module in modules] + ["README-201.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "matched-comparison.csv").as_posix(), "title": "Download the matched comparison CSV"},
        {"path": (ROOT / "export-receipt.json").as_posix(), "title": "Export receipt after restart"},
        {"path": (ROOT / "source-comparison-proof.json").as_posix(), "title": "Original checked row comparison"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/200/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 201 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-200.md", "../../../README-201.md").replace(
        '../199/index.html">Edition 199', '../200/index.html">Edition 200')
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
    assert manifest["latest"] == 200
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 201
    manifest["editions"].append({"number": 201, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-201.md", "markdownSha256": item("README-201.md")["sha256"],
                                 "companion": "docs/readmes/201/index.html", "tag": "readme-201",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/201",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition201-live"))

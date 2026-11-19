"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "d98dda94985c606ecc8d29969853c3300fe6bae9"
ROOT = Path("docs/readmes/197")
TITLE = "Export routed results in their original CSV row order"
LINK_TITLE = "Verified routing CSV export"
SUMMARY = ("Export one CSV from a completed classification routing plan. "
           "It preserves source rows and duplicates, adds units to each result and records the verified sources.")
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
    tests = json.loads(read("build/edition197-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    receipt = app["export"]
    assert app["checks"]["restartCount"] == 3 and app["legacy"]["rows"] == 3
    assert app["complete"]["status"] == "verified" and len(app["complete"]["rows"]) == receipt["rows"] == 5
    assert sha256((live / "verified-routed-results.csv").read_bytes()).hexdigest() == receipt["sha256"]
    assert receipt["source"]["reportSha256"] == receipt["sha256"]
    ROOT.mkdir(exist_ok=True)
    shutil.copyfile(live / "verified-routed-results.csv", ROOT / "verified-routed-results.csv")
    write_json(ROOT / "export-receipt.json", receipt)
    labels = {
        "partial-plan-after-restart.png": "After restart, the unfinished plan can resume",
        "verified-plan-ready-to-export.png": "The completed plan offers a single verified CSV",
        "verified-csv-saved.png": "The desktop confirms where the five verified rows were saved",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})

    result = ("Five interleaved rows were exported in their original order. "
              "The same value, 37.25, produced 99.05 °F and 946.15 mm on the respective routes.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    write("README-197.md", f"""# Fraynix Agent Workspace · Edition 197

[Edition 196](README-196.md) · [Offline walkthrough](docs/readmes/197/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The completed plan offers a single verified CSV](docs/readmes/197/verified-plan-ready-to-export.png)

## Export a plan

1. Complete a reviewed classification routing plan, or send `Resume classification routing.` to reopen the previous plan.
2. When every procedure verifies, choose **Export verified CSV**. The plan displays the local path.
3. The file retains the original cells and appends the logical CSV row number, classification, lesson, input column and value, converted value and both units. The supporting receipt includes the exact CSV hash, source classification and verified child artifacts.

![The paused plan is still incomplete after restart](docs/readmes/197/partial-plan-after-restart.png)

## What passed

{result} An exact duplicate row appeared twice and kept two distinct row numbers. An existing `route_source_row` column kept its original values. The new columns used the `route_2_` prefix. The export also preserved a multiline quoted cell.

Three desktop restarts passed. The app refused export after one batch had completed and again when the last child had verified but its result was still pending acceptance into the plan. It refused altered classifier or child evidence. Once complete, the desktop saved a single CSV whose bytes matched Java's verified output. A repeated verification after restart produced byte-identical output. An edition 196 plan also exported through the same checks.

![The desktop confirms the verified CSV location](docs/readmes/197/verified-csv-saved.png)

{counts}

[Sample verified CSV](docs/readmes/197/verified-routed-results.csv) · [Export receipt](docs/readmes/197/export-receipt.json) · [Verification evidence](docs/readmes/197/verification.json) · [Implementation and reproduction](docs/publication197/plan.md)
""")
    previous = "[Edition 196 — Reviewed classification routing](README-196.md)"
    lead = f"[Edition 197 — {LINK_TITLE}](README-197.md) · [Previous edition](README-196.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/197/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-197.md" not in index:
        start = index.index("[Edition 196")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 197 — {LINK_TITLE}](../../README-197.md)"
              + " · [Walkthrough](197/index.html)\n\n" + index[start:])
    record = (f"## Edition 197 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "Partial and unaccepted outcomes were refused after restart. The final file matched the receipt and prior plans remained exportable. "
              "See README-197.md and docs/publication197/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/205-verified-routed-csv.md"
    canvas = "docs/obsidian/205-verified-routed-csv.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 197 — {LINK_TITLE}](205-verified-routed-csv.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 197](../../README-197.md) · "
          "[Workflow map](205-verified-routed-csv.canvas) · [Implementation plan](../publication197/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Routed CSV verifier", java + "RoutedCsvReport.java"),
        ("Verified routing state", java + "ClassifiedProcedurePlan.java"),
        ("Routing preview and approval", "workspace-desktop/renderer/classification-routing.js"),
    )]
    nodes = [module["source"] for module in modules] + ["README-197.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "export-receipt.json").as_posix(), "title": "CSV export receipt and source proofs"},
        {"path": (ROOT / "verified-routed-results.csv").as_posix(), "title": "Verified CSV with mixed units"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/196/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 197 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-196.md", "../../../README-197.md").replace(
        '../195/index.html">Edition 195', '../196/index.html">Edition 196')
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
    assert manifest["latest"] == 196
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 197
    manifest["editions"].append({"number": 197, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-197.md", "markdownSha256": item("README-197.md")["sha256"],
                                 "companion": "docs/readmes/197/index.html", "tag": "readme-197",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/197",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition197-live"))

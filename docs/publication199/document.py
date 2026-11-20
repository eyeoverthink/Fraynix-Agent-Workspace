"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "7deea80e30602c8992d98085bf7799382a7f265d"
ROOT = Path("docs/readmes/199")
TITLE = "Compare verified routing exports by output unit"
LINK_TITLE = "Verified routing comparison"
SUMMARY = ("Compare the latest two verified routing exports in Chat, with separate °F and mm summaries. "
           "Recheck the same two exports against Java and the browser after restart.")
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
    tests = json.loads(read("build/edition199-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    proof = app["proof"]
    receipt = app["browserReceipt"]
    saved = app["saved"]
    assert receipt["passed"] and len(receipt["checks"]) == proof["browserChecks"] == saved["browserChecks"] == 12
    sources = {
        "earlier-routed-results.csv": saved["earlierCsvSha256"],
        "later-routed-results.csv": saved["csvSha256"],
        "comparison.html": saved["htmlSha256"],
    }
    ROOT.mkdir(exist_ok=True)
    for name, digest in sources.items():
        assert sha256((live / name).read_bytes()).hexdigest() == digest
        shutil.copyfile(live / name, ROOT / name)
    write_json(ROOT / "comparison-browser-receipt.json", receipt)
    labels = {
        "comparison-separate-units.png": "The saved comparison shows changes separately for Fahrenheit and millimetres",
        "comparison-rechecked-after-restart.png": "Chat confirms fresh Java and browser verification for the original pair after restart",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})
    groups = {group["unit"]: group for group in proof["comparisons"]}
    temperature, length = groups["°F"], groups["mm"]
    assert temperature["meanChange"] == "23.175" and length["meanChange"] == "441.325"
    result = (f"The sample means changed by **+{temperature['meanChange']} °F** "
              f"and **+{length['meanChange']} mm** across the two exports.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    table = "\n".join(
        f"| {unit} | {group['earlier']['count']} | {group['earlier']['mean']} | "
        f"{group['later']['count']} | {group['later']['mean']} | +{group['meanChange']} |"
        for unit, group in groups.items()
    )
    write("README-199.md", f"""# Fraynix Agent Workspace · Edition 199

[Edition 198](README-198.md) · [Offline walkthrough](docs/readmes/199/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The comparison keeps changes for °F and mm separate](docs/readmes/199/comparison-separate-units.png)

## Try it

1. In each of two completed routing plans, choose **Export verified CSV**. The app picks the latest two exports from distinct plans. A second copy of a plan's CSV still counts as one plan.
2. Send `Compare the last two verified routed CSVs`. Chat opens the comparison after verification. The report shows the source exports, row counts, ranges, means and signed mean changes.
3. After restarting, send `Recheck the routed CSV comparison`. The app rechecks the original pair even if a newer export now exists.

## What passed

{result} The change is the later mean minus the earlier mean, calculated before rounding each displayed value. A unit that appears in only one export has no numerical change. The comparison summarizes each export's available rows.

| Unit | Earlier rows | Earlier mean | Later rows | Later mean | Mean change |
| --- | ---: | ---: | ---: | ---: | ---: |
{table}

Java checked both CSVs against the approved routing plans and compared the browser's independently computed output with its own. After restart, the same artifact passed 12 fresh browser checks and retained the original receipt. The app refused a pair from one plan, tampering with either source even after its receipt was rewritten, and a browser result with mixed units. The edition 198 single-export dashboard also passed a fresh recheck.

![Chat rechecks the original pair after restart](docs/readmes/199/comparison-rechecked-after-restart.png)

The sample comparison ran offline and retains both downloadable source CSVs. {counts}

[Sample comparison](docs/readmes/199/comparison.html) · [Earlier CSV](docs/readmes/199/earlier-routed-results.csv) · [Later CSV](docs/readmes/199/later-routed-results.csv) · [Browser receipt](docs/readmes/199/comparison-browser-receipt.json) · [Verification record](docs/readmes/199/verification.json) · [Reproduction](docs/publication199/plan.md)
""")
    previous = "[Edition 198 — Verified routing dashboard](README-198.md)"
    lead = f"[Edition 199 — {LINK_TITLE}](README-199.md) · [Previous edition](README-198.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/199/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-199.md" not in index:
        start = index.index("[Edition 198")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 199 — {LINK_TITLE}](../../README-199.md)"
              + " · [Walkthrough](199/index.html)\n\n" + index[start:])
    record = (f"## Edition 199 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "After restart, the original pair passed 12 fresh browser checks even after a new export was saved. "
              "See README-199.md and docs/publication199/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/207-verified-routing-comparison.md"
    canvas = "docs/obsidian/207-verified-routing-comparison.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 199 — {LINK_TITLE}](207-verified-routing-comparison.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 199](../../README-199.md) · "
          "[Workflow map](207-verified-routing-comparison.canvas) · [Implementation plan](../publication199/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Same-unit comparison", java + "RoutedComparisonReport.java"),
        ("Comparison and source verification", java + "RoutedDashboard.java"),
        ("Offline browser comparison", "workspace-engine/src/main/resources/workbench/routing-comparison.html"),
    )]
    nodes = [module["source"] for module in modules] + ["README-199.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "comparison.html").as_posix(), "title": "Open the sample comparison"},
        {"path": (ROOT / "comparison-browser-receipt.json").as_posix(), "title": "Original comparison browser receipt"},
        {"path": (ROOT / "earlier-routed-results.csv").as_posix(), "title": "Earlier verified CSV"},
        {"path": (ROOT / "later-routed-results.csv").as_posix(), "title": "Later verified CSV"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/198/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 199 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-198.md", "../../../README-199.md").replace(
        '../197/index.html">Edition 197', '../198/index.html">Edition 198')
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
    assert manifest["latest"] == 198
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 199
    manifest["editions"].append({"number": 199, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-199.md", "markdownSha256": item("README-199.md")["sha256"],
                                 "companion": "docs/readmes/199/index.html", "tag": "readme-199",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/199",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition199-live"))

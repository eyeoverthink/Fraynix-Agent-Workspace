"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "785525b953fd231ac9e444d65541806a81899c56"
ROOT = Path("docs/readmes/198")
TITLE = "Build a verified routing dashboard in Chat"
LINK_TITLE = "Verified routing dashboard"
SUMMARY = ("Turn the latest verified routed CSV into a dashboard with separate °F and mm summaries. "
           "Recheck the saved artifact against Java and a fresh browser run after restart.")
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
    tests = json.loads(read("build/edition198-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    proof = app["evidence"]
    receipt = app["browserReceipt"]
    saved = app["saved"]
    assert app["checks"]["restartCount"] == 2 and saved["browserChecks"] == 15
    assert receipt["passed"] and len(receipt["checks"]) == proof["browserChecks"] == 15
    assert sha256((live / "routed-results.csv").read_bytes()).hexdigest() == proof["csvSha256"]
    assert sha256((live / "dashboard.html").read_bytes()).hexdigest() == saved["htmlSha256"]
    assert proof["rowOrder"] == ["2", "3", "4", "5", "6"]
    ROOT.mkdir(exist_ok=True)
    for file in ("routed-results.csv", "dashboard.html"):
        shutil.copyfile(live / file, ROOT / file)
    write_json(ROOT / "dashboard-browser-receipt.json", receipt)
    labels = {
        "dashboard-separate-units.png": "The recovered dashboard keeps temperature and length summaries separate",
        "dashboard-length-filter.png": "The length filter shows only the two millimetre rows",
        "dashboard-rechecked-after-restart.png": "Chat confirms a fresh Java and browser check after restart",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})

    groups = {group["unit"]: group for group in proof["groups"]}
    result = (f"The dashboard kept {groups['°F']['count']} temperature rows and {groups['mm']['count']} length rows "
              f"in separate summaries. Their means were {groups['°F']['mean']} °F and {groups['mm']['mean']} mm.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    write("README-198.md", f"""# Fraynix Agent Workspace · Edition 198

[Edition 197](README-197.md) · [Offline walkthrough](docs/readmes/198/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The reopened dashboard passed 15 fresh browser checks](docs/readmes/198/dashboard-rechecked-after-restart.png)

## Try it

1. In a completed routing plan, choose **Export verified CSV**.
2. Send `Build a dashboard from the verified routed CSV.` Chat opens the completed dashboard after Java checks the disk export and actual browser results.
3. Filter to **°F** or **mm**. The rows keep their original order and duplicate entries keep their distinct row numbers.
4. After restart, send `Recheck the routed CSV dashboard.` The app runs the checks again and opens the same artifact.

![Separate summaries for °F and mm](docs/readmes/198/dashboard-separate-units.png)

## What passed

{result} Java checked the count, minimum, maximum and mean of each unit against independent browser calculations. The browser also checked both filters, the full source-ordered list and the source CSV link.

The desktop recovered from a blocked file write after restart using the retained checks. After a second restart, the reopened dashboard passed 15 fresh browser checks. The original receipt and artifact ID stayed intact. Java rejected a summary with the wrong units, a modified CSV with a rewritten receipt, and changes to either the dashboard receipt or a source-child receipt.

![The length filter shows the two rows measured in millimetres](docs/readmes/198/dashboard-length-filter.png)

The artifact retains the original CSV and verification record. The sample HTML also rendered and calculated its summary offline. {counts}

[Open the sample dashboard](docs/readmes/198/dashboard.html) · [Original routed CSV](docs/readmes/198/routed-results.csv) · [Browser receipt](docs/readmes/198/dashboard-browser-receipt.json) · [Verification evidence](docs/readmes/198/verification.json) · [Reproduction](docs/publication198/plan.md)
""")
    previous = "[Edition 197 — Verified routing CSV export](README-197.md)"
    lead = f"[Edition 198 — {LINK_TITLE}](README-198.md) · [Previous edition](README-197.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/198/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-198.md" not in index:
        start = index.index("[Edition 197")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 198 — {LINK_TITLE}](../../README-198.md)"
              + " · [Walkthrough](198/index.html)\n\n" + index[start:])
    record = (f"## Edition 198 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "A blocked save recovered from the original checks. After the next restart, the same artifact passed 15 fresh checks. "
              "See README-198.md and docs/publication198/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/206-verified-routing-dashboard.md"
    canvas = "docs/obsidian/206-verified-routing-dashboard.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 198 — {LINK_TITLE}](206-verified-routing-dashboard.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 198](../../README-198.md) · "
          "[Workflow map](206-verified-routing-dashboard.canvas) · [Implementation plan](../publication198/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Dashboard statistics", java + "RoutedDashboardReport.java"),
        ("Dashboard and source proof", java + "RoutedDashboard.java"),
        ("Offline browser dashboard", "workspace-engine/src/main/resources/workbench/routing-dashboard.html"),
    )]
    nodes = [module["source"] for module in modules] + ["README-198.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "dashboard-browser-receipt.json").as_posix(), "title": "Original dashboard browser receipt"},
        {"path": (ROOT / "routed-results.csv").as_posix(), "title": "Verified dashboard source CSV"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/197/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 198 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-197.md", "../../../README-198.md").replace(
        '../196/index.html">Edition 196', '../197/index.html">Edition 197')
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
    assert manifest["latest"] == 197
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 198
    manifest["editions"].append({"number": 198, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-198.md", "markdownSha256": item("README-198.md")["sha256"],
                                 "companion": "docs/readmes/198/index.html", "tag": "readme-198",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/198",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition198-live"))

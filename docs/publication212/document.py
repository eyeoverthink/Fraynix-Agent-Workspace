"""Freeze the edition 212 guide and the verified synthetic chart example."""
import importlib.util
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("documentation210", Path("docs/publication210/document.py"))
helpers = importlib.util.module_from_spec(spec)
spec.loader.exec_module(helpers)
read, write, write_json, item, prepend = helpers.read, helpers.write, helpers.write_json, helpers.item, helpers.prepend

ROOT = Path("docs/readmes/212")
TITLE = "Explore exact changes in a named comparison"
DATE = "2026-11-23"
SUMMARY = ("Name a verified comparison, then ask Chat to plot gains, losses or exact rows. "
           "Fraynix gives each unit its own scale and lets you inspect both original readings.")
NOTE = "220-named-comparison-chart"
SHOTS = {
    "desktop-length-only.png": "Chat charts a single named row in millimetres in the desktop",
    "offline-readings.png": "The offline chart places original readings beside the selected bar",
}
MODULES = (
    ("Names and durable chart intent", "workspace-engine/src/main/java/fraymus/workspace/ComparisonChartBook.java"),
    ("Exact row and unit projection", "workspace-engine/src/main/java/fraymus/workspace/RoutedComparisonChart.java"),
    ("Keyboard inspection and local downloads", "workspace-engine/src/main/resources/workbench/routing-named-chart.html"),
    ("Chat routing", "workspace-desktop/renderer/classification-routing.js"),
    ("Desktop, browser and restart proof", "workspace-desktop/test/named-comparison-chart.cjs"),
)


def actual(proof):
    return (proof["passed"] and proof["mixed"] == 20 and proof["single"] == 15 and proof["empty"] == 10
            and proof["units"] == ["°F", "mm"] and len(proof["rows"]) == 3 and len(proof["checks"]) == 4)


def publish(live, matrix_file):
    evidence = json.loads(read(live / "result.json"))
    matrix = json.loads(read(matrix_file))
    assert actual(evidence) and matrix["conclusion"] == "success" and len(matrix["targets"]) == 4
    for target in matrix["targets"]:
        result = target["report"]
        assert target["conclusion"] == "success" and actual(result)
        assert result["sourceSha256"] == evidence["sourceSha256"]
        assert result["browserChartSha256"] == evidence["browserChartSha256"] and result["rows"] == evidence["rows"]

    ROOT.mkdir(exist_ok=True)
    for name in SHOTS:
        assert (live / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(live / name, ROOT / name)
    for source, destination in (("index.html", "chart.html"), ("chart-data.json", "chart-data.json"),
                                ("matched-comparison.csv", "matched-comparison.csv"), ("summary-proof.json", "summary-proof.json")):
        shutil.copyfile(live / source, ROOT / destination)
    chart = json.loads(read(ROOT / "chart-data.json"))
    assert chart["browserChartSha256"] == evidence["browserChartSha256"]
    assert sha256((ROOT / "matched-comparison.csv").read_bytes()).hexdigest() == evidence["sourceSha256"]
    write_json(ROOT / "release-proof.json", {"date": DATE, "windowsDesktop": evidence, "portableWorkspace": matrix,
                                            "screenshots": [item(ROOT / name) for name in SHOTS]})
    matrix_note = ("Windows x64, Apple silicon macOS, Intel macOS and Linux x64 passed 20 browser checks for the mixed chart, "
                   "15 for the one-row chart and 10 for the empty chart.")
    recovery = ("The named source and row selection survived a restart before rendering and a restart after saving. "
                "The checks also refused a name collision, an unknown row in the requested units, changed original CSV bytes, "
                "and a changed browser proof.")
    write("README-212.md", f"""# Fraynix Agent Workspace · Edition 212

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 211](README-211.md) · [Offline walkthrough](docs/readmes/212/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Fraynix plots the requested millimetre gain from a named comparison](docs/readmes/212/desktop-length-only.png)

## From a saved comparison to a chart

1. Export a matched comparison and run **Summarize the last verified matched comparison CSV**. See the [matched comparison workflow](README-201.md).
2. Say BACKName the last verified comparison "Workshop audit"BACK. Use BACKName comparison export <export ID> as "Workshop audit"BACK to name an older verified summary.
3. Ask BACKChart gains from "Workshop audit" in millimetres for key "length gain"BACK. You can ask for losses, gains and losses, or all matched changes. Use **both units** to give Fahrenheit and millimetres separate scales. Quote each original key you want.
4. Click a bar to see the earlier and later source row numbers and exact readings. The chart includes downloads for the selected rows, the original CSV, and the checked summary.
5. After restarting, say **Open my last comparison chart**. The saved chart keeps its original source, units and selection even if newer comparisons exist.

The example uses synthetic records. The three requested rows contain a gain of **18 °F**, a loss of **9 °F**, and a gain of **25.4 mm**. Each unit scales to its own largest selected magnitude. The generated page works offline with its accompanying evidence files. [Open the sample chart](docs/readmes/212/chart.html) · [Selected rows and scales](docs/readmes/212/chart-data.json)

![Selecting a negative Fahrenheit bar reveals source readings of 50 and 41](docs/readmes/212/offline-readings.png)

## Verification

- {matrix_note}
- {recovery}
- The offline browser used the keyboard to open the 9 °F loss. The selected-row download matched the saved file byte for byte. The existing explorer, change story, and briefing checks also passed. [CI run]({matrix["url"]}) · [Release proof](docs/readmes/212/release-proof.json)

## How the source stays attached

BACKBACKBACKmermaid
flowchart LR
    Original["Verified CSV and original pair"] --> Alias["Saved comparison name"]
    Chat["Name, units, direction and keys"] --> Selection["Exact saved selection"]
    Alias --> Selection
    Selection --> Verify["Recheck CSV, routing and summary"]
    Verify --> Chart["Render one scale per unit"]
    Chart --> Receipt["Check live bars and source readings"]
    Receipt --> Save["Save chart and its evidence"]
    Save --> Resume["Resume or recheck the same chart"]
BACKBACKBACK

## Next acceptance test

Build a briefing from a named chart in the same conversation. Check every number and row citation against its selected data and use separate evidence for claims about causes.

[Reproduction and verification plan](docs/publication212/plan.md) · [Obsidian map](docs/obsidian/220-named-comparison-chart.canvas)
""".replace("BACK", chr(96)))
    home = read("README.md").split("[Edition ", 1)[0]
    write("README.md", home + "[Edition 212 — Chart named comparisons](README-212.md) · [Previous edition](README-211.md)"
          " · [Offline walkthrough](docs/readmes/212/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    index = read("docs/readmes/INDEX.md")
    if "README-212.md" not in index:
        write("docs/readmes/INDEX.md", index.replace("[Edition 211", "[Edition 212 — Chart named comparisons](../../README-212.md)"
              " · [Walkthrough](212/index.html)\n\n[Edition 211", 1))
    record = (f"## Edition 212 — {TITLE}\n\n{DATE}. {SUMMARY} {recovery} {matrix_note} "
              "Use the original matched pair and summary proof. Keep the named alias unique, resolve the requested keys "
              "before replacing the last chart plan, and verify each scale in the browser receipt. "
              "See README-212.md and docs/publication212/plan.md.\n\n")
    for path in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(path, record)

    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 212 — Chart named comparisons]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{recovery}\n\n[Edition 212](../../README-212.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication212/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in MODULES]
    nodes = [row["source"] for row in modules] + ["README-212.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": file, "x": (i % 3) * 380, "y": (i // 3) * 290,
                   "width": 350, "height": 250} for i, file in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "toNode": str(i)} for i in range(1, len(nodes))],
    })
    gallery = [{"path": (ROOT / name).as_posix(), "title": caption} for name, caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT / "chart.html").as_posix(), "title": "Try the verified interactive chart"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Desktop and portable CI evidence"},
        {"path": "docs/publication212/plan.md", "title": "Reproduce the chart workflow"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source = matrix["headSha"]
    files = subprocess.check_output(["git", "diff", "--name-only", "readme-211", source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas, "docs/publication212/plan.md"})]
    write_json(ROOT / "file-inventory.json", inventory)

    guide = read("docs/readmes/211/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 212 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{TITLE}</h1><p>{SUMMARY}</p>", guide, count=1)
    guide = guide.replace("../../../README-211.md", "../../../README-212.md").replace(
        '../210/index.html">Edition 210', '../211/index.html">Edition 211').replace(
        '../../publication211/plan.md">Try the chart request', 'chart.html">Open the interactive example')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)
    manifest = json.loads(subprocess.check_output(["git", "show", "readme-211:docs/readmes/editions.json"]))
    assert manifest["latest"] == 211
    assets = [item(path) for path in sorted(ROOT.iterdir()) if path.is_file()]
    frozen = [{"path": row["path"], "sha256": row["sha256"], "kind": "binary" if row["hashKind"] == "binary" else "text"}
              for row in assets]
    manifest["latest"] = 212
    manifest["editions"].append({"number": 212, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-212.md", "markdownSha256": item("README-212.md")["sha256"],
                                 "companion": (ROOT / "index.html").as_posix(), "tag": "readme-212",
                                 "sourceBaseline": source, "snapshotDirectory": ROOT.as_posix(), "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(f"Edition 212 prepared with {len(gallery)} screenshots and {len(inventory)} inventory entries.")


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

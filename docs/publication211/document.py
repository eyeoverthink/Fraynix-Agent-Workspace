"""Publish the edition 211 guide from standalone desktop and portable CI proofs."""
import importlib.util
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

ROOT = Path("docs/readmes/211")
TITLE = "Chart the verified result you meant"
DATE = "2026-11-23"
SUMMARY = ("Ask “Make a chart of that.” Fraynix uses a single verified source or asks you to choose. "
           "The question, selection and checked chart survive restart.")
NOTE = "219-verified-chart"
SHOTS = {
    "recorded-chart.png": "All 49 mapped samples from the recorded-motion source appear in the canvas",
    "needs-source-choice.png": "Fraynix asks which of two verified results to chart",
    "choice-after-restart.png": "The original choices are available after restart with the selected ramp highlighted",
    "simulated-chart.png": "Two verified ramp traces are plotted on the same clock and labelled as simulated",
    "changed-source-refusal.png": "Fraynix refuses to reopen the chart after its original report changes",
}
MODULES = (
    ("Durable chart request", "workspace-engine/src/main/java/fraymus/workspace/ReferenceChart.java"),
    ("Exact sample projection", "workspace-engine/src/main/java/fraymus/workspace/ChartProjection.java"),
    ("Desktop source choice", "workspace-desktop/renderer/chart-followup.js"),
    ("Isolated browser verification", "workspace-desktop/chart-followup.cjs"),
    ("Restart and refusal test", "workspace-desktop/test/chart-followup-integration.cjs"),
)


def checked(proof):
    return (proof["passed"] and not proof["errors"] and proof["noSourceRefused"]
            and proof["archivedSourceReverified"] and proof["changedSourceRefused"]
            and proof["motion"]["samples"] == [49] and proof["ramp"]["samples"] == [18, 18]
            and proof["motion"]["browserChecks"] == proof["ramp"]["browserChecks"] == 6
            and proof["recovery"]["compactWindowReopened"]
            and all(proof["recovery"].values()) and proof["recovery"]["choicesReopened"] == 2)


def publish(live, matrix_file):
    evidence = json.loads(read(live / "summary.json"))
    matrix = json.loads(read(matrix_file))
    assert checked(evidence)
    assert set(evidence["screenshots"]) == set(SHOTS)
    assert len(matrix["targets"]) == 4 and matrix["conclusion"] == "success"
    for target in matrix["targets"]:
        assert target["conclusion"] == "success" and checked(target["report"])
        for kind in ("motion", "ramp"):
            assert target["report"][kind]["sha256"] == evidence[kind]["sha256"]

    ROOT.mkdir(exist_ok=True)
    for name in SHOTS:
        assert (live / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(live / name, ROOT / name)
    write_json(ROOT / "release-proof.json", {"date": DATE, "localDesktop": evidence, "portableWorkspace": matrix,
                                            "screenshots": [item(ROOT / name) for name in SHOTS]})
    tests = ("Windows x64, Apple silicon macOS, Intel macOS and Linux x64 each verified 49 recorded samples "
             "and two simulated traces of 18 samples each. Each saved chart passed six browser checks and a "
             "comparison of every rendered point against the Java projection.")
    recovery = ("The test reopens the unanswered question in a compact window, reopens the pinned selection after a second restart, "
                "and rechecks the same saved artifact after another restart.")
    write("README-211.md", f"""# Fraynix Agent Workspace · Edition 211

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 210](README-210.md) · [Offline walkthrough](docs/readmes/211/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Recorded motion chart in Fraynix with 49 original mapped samples](docs/readmes/211/recorded-chart.png)

## Try it in Chat

1. Finish a verified motion summary or a two-ramp comparison.
2. Ask **“Make a chart of that.”** With one eligible verified source, the saved source is rechecked and the chart opens in the canvas.
3. With more than one distinct verified source, choose one from the question. **Chart request** restores the original options after restart. A result completed later appears when you start a new chart request.
4. After saving, use **Last chart** to recheck the evidence and reopen the same chart. Use **Download original samples and provenance** in the chart to save the plotted values and their source and evidence hashes.

| Source | Chart |
| --- | --- |
| Verified motion summary | Mapped displacement in metres against the original timestamps in seconds. The label distinguishes recorded from synthetic data. |
| Verified two-ramp comparison | Both original simulated travel traces on a shared time axis in seconds. |

The chart uses the saved input version and the original completed task's evidence. It plots the original samples without smoothing or resampling. Archived verified motion summaries can still supply the chart. When the selected source fails verification, Fraynix reports the refusal and keeps the selected source on the request.

## What was verified

- {tests}
- {recovery}
- With no verified source, Chat asks for a verified result. The test also rejects a newly completed source that was absent from the original choices, a request with no browser evidence, and reopening a chart after editing the original ramp report. None of those refusals creates a new chart.
- The desktop workflow ran without a local model. [Portable workspace CI]({matrix["url"]}) · [Test evidence](docs/readmes/211/release-proof.json)

[Source question](docs/readmes/211/needs-source-choice.png) · [Choice after restart](docs/readmes/211/choice-after-restart.png) · [Simulated ramp chart](docs/readmes/211/simulated-chart.png) · [Changed source refusal](docs/readmes/211/changed-source-refusal.png)

## How the reference stays attached

BACKBACKBACKmermaid
flowchart LR
    Chat["Chart of that"] --> Saved["Saved verified chat results"]
    Saved --> Choice["Select one source or ask"]
    Choice --> Pin["Save exact source and revision"]
    Pin --> Source["Recheck original evidence"]
    Source --> Plot["Project originals and check browser coordinates"]
    Plot --> Chart["Versioned chart and provenance"]
    Chart --> Reopen["Recheck source and receipt on reopen"]
BACKBACKBACK

## Next acceptance test

Resolve an explicitly named source and build a requested chart from a verified table comparison. Carry the exact row selection and units through restart and recheck them when the project contains newer results.

[Reproduction and verification plan](docs/publication211/plan.md) · [Obsidian map](docs/obsidian/219-verified-chart.canvas)
""".replace("BACK", chr(96)))
    home = read("README.md").split("[Edition ", 1)[0]
    write("README.md", home + "[Edition 211 — Chart a verified result](README-211.md) · [Previous edition](README-210.md)"
          " · [Offline walkthrough](docs/readmes/211/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    index = read("docs/readmes/INDEX.md")
    if "README-211.md" not in index:
        write("docs/readmes/INDEX.md", index.replace("[Edition 210", "[Edition 211 — Chart a verified result](../../README-211.md)"
              " · [Walkthrough](211/index.html)\n\n[Edition 210", 1))
    record = (f"## Edition 211 — {TITLE}\n\n{DATE}. {SUMMARY} {recovery} {tests} "
              "The Apple silicon CI failure exposed a hidden compact-window footer. The recovery control now stays "
              "visible there and the test forces a compact restart. Keep the chosen original source pinned. "
              "A later result or a changed report cannot silently replace it. "
              "See README-211.md and docs/publication211/plan.md.\n\n")
    for path in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(path, record)

    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 211 — Chart a verified result]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{recovery}\n\n[Edition 211](../../README-211.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication211/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in MODULES]
    nodes = [row["source"] for row in modules] + ["README-211.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": file, "x": (i % 3) * 380, "y": (i // 3) * 290,
                   "width": 350, "height": 250} for i, file in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "toNode": str(i)} for i in range(1, len(nodes))],
    })
    gallery = [{"path": (ROOT / name).as_posix(), "title": caption} for name, caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Desktop and portable CI evidence"},
        {"path": "docs/publication211/plan.md", "title": "Reproduce the chart workflow"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source = matrix["headSha"]
    files = subprocess.check_output(["git", "diff", "--name-only", "readme-210", source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas, "docs/publication211/plan.md"})]
    write_json(ROOT / "file-inventory.json", inventory)

    guide = read("docs/readmes/210/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 211 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{TITLE}</h1><p>{SUMMARY}</p>", guide, count=1)
    guide = guide.replace("../../../README-210.md", "../../../README-211.md").replace(
        '../209/index.html">Edition 209', '../210/index.html">Edition 210').replace(
        '../../publication210/plan.md">Try the requests', '../../publication211/plan.md">Try the chart request')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)
    manifest = json.loads(subprocess.check_output(["git", "show", "readme-210:docs/readmes/editions.json"]))
    assert manifest["latest"] == 210
    assets = [item(path) for path in sorted(ROOT.iterdir()) if path.is_file()]
    frozen = [{"path": row["path"], "sha256": row["sha256"], "kind": "binary" if row["hashKind"] == "binary" else "text"}
              for row in assets]
    manifest["latest"] = 211
    manifest["editions"].append({"number": 211, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-211.md", "markdownSha256": item("README-211.md")["sha256"],
                                 "companion": (ROOT / "index.html").as_posix(), "tag": "readme-211",
                                 "sourceBaseline": source, "snapshotDirectory": ROOT.as_posix(), "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(f"Edition 211 prepared with {len(gallery)} screenshots and {len(inventory)} inventory entries.")


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

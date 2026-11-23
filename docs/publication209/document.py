"""Freeze the measured selected-source briefing release and its offline guide."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "7af38ebdcf3edfe496fc6ef859981062053edb58"
ROOT = Path("docs/readmes/209")
TITLE = "Choose older comparisons and sections for each source"
LINK_TITLE = "Pick briefing sources and sections"
DATE = "2026-11-23"
SUMMARY = ("Choose two saved verified comparisons and set the sections for each. "
           "One approval preserves the selected order and the original evidence through restart and offline export.")


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


def publish(live, mac, summary):
    source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    evidence = json.loads(read(summary))
    app = json.loads(read(live / "result.json"))
    sample = live / "selected"
    matrix = evidence["matrix"]
    targets = matrix["targets"]
    assert set(targets) == {"windows-x64", "linux-x64", "macos-x64", "macos-arm64"}
    assert all(row["status"] == "success" and row["browserChecks"] == 18 for row in targets.values())
    selected = app["paired"]["selected"]
    assert app["passed"] and selected["proof"]["rechecked"] and selected["proof"]["browserChecks"] == 18
    assert selected["review"]["approved"]
    proof = json.loads(read(sample / "verification.json"))
    assert proof["sources"] == selected["review"]["sources"] == selected["proof"]["comparisonSources"]
    assert proof["presentation"] == "selected-paired-briefing-v1"
    assert [binding["sections"] for binding in proof["sources"]] == [["rows"], ["totals", "chart"]]
    csv_hashes = [binding["sourceSha256"] for binding in proof["sources"]]
    assert all(row["sourceCsvHashes"] == csv_hashes for row in targets.values())
    assert evidence["legacyBriefing"]["passed"] and evidence["legacyBriefing"]["browserChecks"] == 24
    assert selected["proof"]["htmlSha256"] == sha256((sample / "selected-briefing.html").read_bytes()).hexdigest()

    ROOT.mkdir(exist_ok=True)
    assets = ["selected-briefing.html", "verification.json"]
    for index, binding in enumerate(proof["sources"], start=1):
        csv = f"source-{index}-comparison.csv"
        assert binding["sourceSha256"] == sha256((sample / csv).read_bytes()).hexdigest()
        assets += [csv, f"source-{index}-receipt.json", f"source-{index}-summary.json"]
    for name in assets:
        shutil.copyfile(sample / name, ROOT / name)
    screenshots = []
    for frame, filename, caption in (
        (mac / "selected-picker.png", "mac-picker.png", "Apple silicon macOS shows different section choices for two older comparisons"),
        (live / "selected-offline.png", "offline-selection.png", "The Windows offline file shows totals and chart on Source 2"),
    ):
        assert frame.read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(frame, ROOT / filename)
        screenshots.append({"path": (ROOT / filename).as_posix(), "title": caption})
    write_json(ROOT / "release-proof.json", {**evidence, "sourceBaseline": source, "windowsSelectedBriefing": selected,
                                             "screenshots": [item(row["path"]) for row in screenshots]})
    tests = evidence["tests"]
    counts = (f"{tests['rootJava']} root Java, {tests['workspaceJava']} focused workspace Java, "
              f"{tests['desktop']} desktop and {tests['archive']} archive tests passed on Windows.")
    result = ("The workflow passed 18 browser checks on Windows x64, Apple silicon, Intel Mac and Linux x64. "
              "Each run preserved the expected output order, sections and six original evidence files.")
    write("README-209.md", f"""# Fraynix Agent Workspace · Edition 209

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 208](README-208.md) · [Offline walkthrough](docs/readmes/209/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The source picker shows rows on Source 1 and totals and chart on Source 2](docs/readmes/209/mac-picker.png)

## Pick two older comparisons

1. Export and summarize two distinct matched comparisons. See the [matched comparison workflow](README-201.md).
2. Send `Review a two-source comparison briefing.` The picker reads the project's saved verified summaries. Use **Load older comparisons** to browse earlier entries.
3. Choose **Source 1** and **Source 2** in the order you want them exported. Check the sections for each, click **Review selections**, then choose **Approve and build**.
4. After restarting, send `Resume the comparison briefing.` for a pending review or `Recheck the comparison briefing.` for a completed one. Send `Export the comparison briefing.` for one offline HTML file.

The review pins the two source IDs, original hashes and selected sections. The offline tabs and numbered downloads follow the selected order. Both sources retain their original CSV, receipt and summary proof even when the row table is hidden.

![The exported briefing shows totals and a chart on the second source](docs/readmes/209/offline-selection.png)

## What passed

| Synthetic test | Result |
| --- | --- |
| Browse history | The picker paged past 24 newer exports without verified summaries. It found earlier saved summaries after restart. |
| Source 1 | The second-oldest comparison appeared first, showing five source rows. |
| Source 2 | The oldest comparison appeared second, showing totals and five chart marks. Its row table stayed hidden. |
| Restart and export | The pending review and completed briefing kept the approved order, section choices and original evidence. All six offline downloads matched. |
| Compatibility | The original edition 208 briefing passed 24 fresh checks with its existing HTML hash. |

{result} {counts} [Cross-platform results]({matrix['url']})

```mermaid
flowchart LR
    A[Saved verified summaries] --> B[Choose two sources and order]
    B --> C[Choose sections per source]
    C --> D[Review and approve pinned evidence]
    D --> E[Recheck and export offline HTML]
```

[Offline briefing sample](https://raw.githubusercontent.com/eyeoverthink/Fraynix-Agent-Workspace/readme-209/docs/readmes/209/selected-briefing.html) · [Source proof](docs/readmes/209/verification.json) · [Release evidence](docs/readmes/209/release-proof.json) · [Reproduction and next candidate](docs/publication209/plan.md)
""")
    main = read("README.md")
    if "README-209.md" not in main:
        position = main.index("[Edition 208")
        end = main.index("\n", main.index("\n\n", position) + 2)
        write("README.md", main[:position] + f"[Edition 209 — {LINK_TITLE}](README-209.md) · [Previous edition](README-208.md)"
              + " · [Offline walkthrough](docs/readmes/209/index.html) · [Full history](docs/readmes/INDEX.md)\n\n"
              + SUMMARY + main[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-209.md" not in index:
        begin = index.index("[Edition 208")
        write("docs/readmes/INDEX.md", index[:begin] + f"[Edition 209 — {LINK_TITLE}](../../README-209.md)"
              + " · [Walkthrough](209/index.html)\n\n" + index[begin:])
    record = (f"## Edition 209 — {TITLE}\n\n{DATE}. {SUMMARY} {result} "
              "The picker reads saved verified summaries and browses beyond the recent Chat history. "
              "Section choices, original summaries and source order are bound to one approval. See README-209.md and docs/publication209/plan.md.\n\n")
    for path in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(path, record)

    note_name = "217-selected-briefing"
    note = f"docs/obsidian/{note_name}.md"
    canvas = f"docs/obsidian/{note_name}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 209 — {LINK_TITLE}]({note_name}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 209](../../README-209.md) · "
          f"[Workflow map]({note_name}.canvas) · [Reproduction](../publication209/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Briefing source picker", "workspace-desktop/renderer/briefing-picker.js"),
        ("Approved source order and sections", "workspace-engine/src/main/java/fraymus/workspace/PairedComparisonBriefing.java"),
        ("Per-source offline sections", "workspace-engine/src/main/resources/workbench/routing-briefing-sections.js"),
        ("Desktop and export verification", "workspace-desktop/test/selected-briefing-integration.cjs"),
    )]
    nodes = [module["source"] for module in modules] + ["README-209.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "selected-briefing.html").as_posix(), "title": "Open the offline briefing with selected sections"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Platform and test evidence"},
        {"path": "docs/publication209/plan.md", "title": "Reproduction and next candidate"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/208/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 209 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-208.md", "../../../README-209.md").replace(
        '../207/index.html">Edition 207', '../208/index.html">Edition 208')
    guide = guide.replace('<a href="paired-briefing.html">Open the paired briefing</a>',
                          '<a href="selected-briefing.html">Open the selected briefing</a>')
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
    assert manifest["latest"] == 208
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()) if path.is_file())]
    manifest["latest"] = 209
    manifest["editions"].append({"number": 209, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-209.md", "markdownSha256": item("README-209.md")["sha256"],
                                 "companion": "docs/readmes/209/index.html", "tag": "readme-209",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/209",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

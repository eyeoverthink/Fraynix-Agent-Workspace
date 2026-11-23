"""Freeze the measured two-source briefing release and its offline guide."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "3dfb9b822bbfa84486ec9c4280d47c6f58fa4dc9"
ROOT = Path("docs/readmes/208")
TITLE = "Approve one briefing from two verified comparisons"
LINK_TITLE = "Two-source comparison briefings"
DATE = "2026-11-23"
SUMMARY = ("Review two verified comparisons in Chat and export one offline briefing. "
           "Each source keeps its own totals, chart, rows and proof.")


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
    matrix = evidence["matrix"]
    targets = matrix["targets"]
    assert set(targets) == {"windows-x64", "linux-x64", "macos-x64", "macos-arm64"}
    assert all(row["status"] == "success" and row["browserChecks"] == 24 for row in targets.values())
    paired = app["paired"]
    assert app["passed"] and paired["proof"]["browserChecks"] == 24 and paired["proof"]["rechecked"]
    assert paired["review"]["approved"] and paired["review"]["sections"] == ["totals", "chart", "rows"]
    proof = json.loads(read(live / "paired-verification.json"))
    assert proof["sources"] == paired["review"]["sources"] == paired["proof"]["comparisonSources"]
    csv_hashes = [binding["sourceSha256"] for binding in proof["sources"]]
    assert all(row["sourceCsvHashes"] == csv_hashes for row in targets.values())
    assert evidence["legacyBriefing"]["passed"] and evidence["legacyBriefing"]["browserChecks"] == 22
    assert proof["presentation"] == "paired-comparison-briefing-v1"
    assert paired["proof"]["htmlSha256"] == sha256((live / "paired-briefing.html").read_bytes()).hexdigest()

    ROOT.mkdir(exist_ok=True)
    assets = ["paired-briefing.html"]
    for index, binding in enumerate(proof["sources"], start=1):
        csv = f"source-{index}-comparison.csv"
        assert binding["sourceSha256"] == sha256((live / csv).read_bytes()).hexdigest()
        assets += [csv, f"source-{index}-receipt.json", f"source-{index}-summary.json"]
    for name in assets:
        shutil.copyfile(live / name, ROOT / name)
    shutil.copyfile(live / "paired-verification.json", ROOT / "verification.json")
    screenshots = []
    for frame, filename, caption in (
        (mac / "paired-review.png", "mac-review.png", "Apple silicon macOS shows both comparison IDs in one review"),
        (live / "offline-paired.png", "offline-paired.png", "The Windows offline file shows Source 2 and its original readings"),
    ):
        assert frame.read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(frame, ROOT / filename)
        screenshots.append({"path": (ROOT / filename).as_posix(), "title": caption})
    write_json(ROOT / "release-proof.json", {**evidence, "sourceBaseline": source, "windowsPairedBriefing": paired,
                                             "screenshots": [item(row["path"]) for row in screenshots]})
    tests = evidence["tests"]
    counts = (f"{tests['rootJava']} root Java, {tests['workspaceJava']} focused workspace Java, "
              f"{tests['desktop']} desktop and {tests['archive']} archive tests passed on Windows.")
    result = ("Windows x64, Apple silicon, Intel Mac and Linux x64 each passed 24 browser checks. "
              "They resumed the same review, rechecked the original pair after restart and matched all six offline downloads.")
    write("README-208.md", f"""# Fraynix Agent Workspace · Edition 208

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 207](README-207.md) · [Offline walkthrough](docs/readmes/208/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![One review shows the two proposed sources on Apple silicon macOS](docs/readmes/208/mac-review.png)

## Build a two-source briefing

1. Export a verified matched comparison and send `Summarize the last verified matched comparison CSV.` Repeat with the second comparison.
2. Send `Review a two-source comparison briefing with totals, chart and rows.` Fraynix proposes the two most recently summarized exports. Confirm the source order, then choose **Approve and build**. The two exports must have different CSV contents.
3. Use **Source 1** and **Source 2** to switch the totals, chart, rows and evidence downloads in the canvas. Each source keeps its own unit scales.
4. After restarting, send `Resume the comparison briefing.` for a pending review, or `Recheck the comparison briefing.` for a completed one. A newer comparison cannot replace either pinned source.
5. Send `Export the comparison briefing.` to save one HTML file with both sets of evidence.

![The standalone Windows file shows the second source, its totals, chart and selected original readings](docs/readmes/208/offline-paired.png)

## What passed

| Synthetic source | Independently measured result |
| --- | --- |
| Source 1 | Eight groups, five matches. Net +9 °F and +12.7 mm. |
| Source 2 | Five groups, three matches. Net −16.2 °F and −6.35 mm. |
| Changed source | Editing either CSV gave the correct source number and blocked recheck and export. A changed Source 2 summary receipt also blocked export. |
| Offline file | The two tabs kept their own readings and downloaded all six evidence files byte for byte. On Windows, damaging either embedded CSV suppressed the chart and downloads. |

A stale approval, a missing source and an identical CSV under a new export ID were refused. The pending review survived a restart before approval, and the completed briefing survived another before rechecking. The original edition 207 briefing passed 22 fresh checks against the same HTML hash. {result} {counts} [Cross-platform results]({matrix['url']})

```mermaid
flowchart LR
    A[Verified comparison and summary 1] --> C[One review pins both sources]
    B[Verified comparison and summary 2] --> C
    C --> D[Approve and check each source]
    D --> E[One offline briefing with independent tabs]
```

[Offline briefing sample](https://raw.githubusercontent.com/eyeoverthink/Fraynix-Agent-Workspace/readme-208/docs/readmes/208/paired-briefing.html) · [Source proof](docs/readmes/208/verification.json) · [Release evidence](docs/readmes/208/release-proof.json) · [Reproduction and next candidate](docs/publication208/plan.md)
""")
    main = read("README.md")
    if "README-208.md" not in main:
        position = main.index("[Edition 207")
        end = main.index("\n", main.index("\n\n", position) + 2)
        write("README.md", main[:position] + f"[Edition 208 — {LINK_TITLE}](README-208.md) · [Previous edition](README-207.md)"
              + " · [Offline walkthrough](docs/readmes/208/index.html) · [Full history](docs/readmes/INDEX.md)\n\n"
              + SUMMARY + main[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-208.md" not in index:
        begin = index.index("[Edition 207")
        write("docs/readmes/INDEX.md", index[:begin] + f"[Edition 208 — {LINK_TITLE}](../../README-208.md)"
              + " · [Walkthrough](208/index.html)\n\n" + index[begin:])
    record = (f"## Edition 208 — {TITLE}\n\n{DATE}. {SUMMARY} {result} "
              "The ordered pair is tied to one approval and its original summaries. "
              "A changed source produces a source-specific refusal before recheck or export. See README-208.md and docs/publication208/plan.md.\n\n")
    for path in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(path, record)

    note_name = "216-paired-comparisons"
    note = f"docs/obsidian/{note_name}.md"
    canvas = f"docs/obsidian/{note_name}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 208 — {LINK_TITLE}]({note_name}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 208](../../README-208.md) · "
          f"[Workflow map]({note_name}.canvas) · [Reproduction](../publication208/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Chat commands", "workspace-desktop/renderer/classification-routing.js"),
        ("Paired comparison briefing", "workspace-engine/src/main/java/fraymus/workspace/PairedComparisonBriefing.java"),
        ("Offline source tabs", "workspace-engine/src/main/resources/workbench/routing-paired-briefing.js"),
        ("Desktop and export verification", "workspace-desktop/test/paired-briefing-integration.cjs"),
    )]
    nodes = [module["source"] for module in modules] + ["README-208.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "paired-briefing.html").as_posix(), "title": "Open the exported offline briefing"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Platform and test evidence"},
        {"path": "docs/publication208/plan.md", "title": "Reproduction and next candidate"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/207/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 208 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-207.md", "../../../README-208.md").replace(
        '../206/index.html">Edition 206', '../207/index.html">Edition 207')
    guide = guide.replace('<a href="comparison-briefing.html">Open the briefing</a>',
                          '<a href="paired-briefing.html">Open the paired briefing</a>')
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
    assert manifest["latest"] == 207
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()) if path.is_file())]
    manifest["latest"] = 208
    manifest["editions"].append({"number": 208, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-208.md", "markdownSha256": item("README-208.md")["sha256"],
                                 "companion": "docs/readmes/208/index.html", "tag": "readme-208",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/208",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

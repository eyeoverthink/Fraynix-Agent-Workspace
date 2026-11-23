"""Freeze the measured briefing release and its offline guide."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "96e0f4277024f2fd2635f65b05edd51410090858"
ROOT = Path("docs/readmes/207")
TITLE = "Approve and share a verified comparison briefing"
LINK_TITLE = "Reviewed comparison briefings"
DATE = "2026-11-23"
SUMMARY = ("Choose the totals, chart or source rows for an offline briefing in Chat. "
           "Approve the source and the selected sections before Fraynix builds it.")


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
    assert all(row["status"] == "success" and row["briefing"]["passed"] for row in targets.values())
    briefing = app["briefing"]
    assert app["passed"] and briefing["proof"]["browserChecks"] == 22 and briefing["rowsOnly"]["browserChecks"] == 18
    assert briefing["review"]["approved"] and briefing["review"]["sections"] == ["totals", "chart"]
    proof = json.loads(read(live / "briefing-verification.json"))
    assert proof["import"]["sha256"] == sha256((live / "matched-comparison.csv").read_bytes()).hexdigest()
    assert briefing["proof"]["htmlSha256"] == sha256((live / "comparison-briefing.html").read_bytes()).hexdigest()
    assert proof["presentation"] == "comparison-briefing-v1"

    ROOT.mkdir(exist_ok=True)
    for name in ("comparison-briefing.html", "matched-comparison.csv", "import-receipt.json", "summary-proof.json"):
        shutil.copyfile(live / name, ROOT / name)
    shutil.copyfile(live / "briefing-verification.json", ROOT / "verification.json")
    screenshots = []
    for frame, filename, caption in (
        (mac / "briefing-review.png", "mac-review.png", "Review the proposed sections and comparison on Apple silicon macOS"),
        (live / "offline-briefing.png", "offline-briefing.png", "The stand-alone file on Windows shows the reviewed totals and chart"),
    ):
        assert frame.read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(frame, ROOT / filename)
        screenshots.append({"path": (ROOT / filename).as_posix(), "title": caption})
    write_json(ROOT / "release-proof.json", {**evidence, "sourceBaseline": source, "windowsBriefing": app,
                                             "screenshots": [item(row["path"]) for row in screenshots]})
    tests = evidence["tests"]
    counts = (f"{tests['rootJava']} root Java, {tests['workspaceJava']} focused workspace Java, "
              f"{tests['desktop']} desktop and {tests['archive']} archive tests passed on Windows.")
    result = ("Windows x64, Apple silicon, Intel Mac and Linux x64 passed closed-output recovery and the reviewed briefing workflow. "
              "Each platform completed 22 browser checks for totals and chart, and 18 for rows only.")
    write("README-207.md", f"""# Fraynix Agent Workspace · Edition 207

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 206](README-206.md) · [Offline walkthrough](docs/readmes/207/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Fraynix shows the proposed sections and source on Apple silicon](docs/readmes/207/mac-review.png)

## Build a briefing in Chat

1. Export a matched comparison and send `Summarize the last verified matched comparison CSV.`
2. Send `Review a comparison briefing with totals and chart.` You can choose any combination of `totals`, `chart` and `rows`. The review displays your sections and the comparison it will use.
3. Click **Approve and build**. To return after restarting, send `Resume the comparison briefing.`
4. Send `Recheck the comparison briefing.` to rerun the checks, or `Export the comparison briefing.` to save a single HTML file.

The example shows changes of −9 °F, +18 °F and 0 °F alongside −12.7 mm and +25.4 mm. You can inspect the earlier and later readings from a bar. The source CSV and original proofs remain available in the evidence panel regardless of which sections you choose.

![The checked offline briefing shows only totals and the interactive chart](docs/readmes/207/offline-briefing.png)

## Verification

| Check | Result |
| --- | --- |
| Approval | A stale digest and an unapproved build created no artifact. The exact review reopened after restart. |
| Section selection | Totals and chart passed 22 browser checks. Rows only passed 18 and produced a distinct artifact. |
| Offline file | HTTP and HTTPS were blocked. The selected sections rendered and the downloaded CSV matched the original bytes. |
| Source integrity | Altering the original comparison blocked both story and briefing export. |
| Desktop recovery | Electron continued through the guided proof after both output streams received a simulated broken-pipe error. |

{result} {counts} [Cross-platform results]({matrix['url']})

The Windows error dialog came from a test process that outlived its launcher. New builds handle the closed output pipe. Startup failures also record a diagnostic in `desktop-startup.log` in the project directory.

```mermaid
flowchart LR
    A[Verified summary and original comparison] --> B[Review selected sections]
    B --> C[Approve the saved review]
    C --> D[Build and check the selected sections]
    D --> E[Recheck and export offline HTML]
```

[Offline briefing sample](https://raw.githubusercontent.com/eyeoverthink/Fraynix-Agent-Workspace/readme-207/docs/readmes/207/comparison-briefing.html) · [Source proof](docs/readmes/207/verification.json) · [Release evidence](docs/readmes/207/release-proof.json) · [Reproduction and next candidate](docs/publication207/plan.md)
""")
    main = read("README.md")
    if "README-207.md" not in main:
        position = main.index("[Edition 206")
        end = main.index("\n", main.index("\n\n", position) + 2)
        write("README.md", main[:position] + f"[Edition 207 — {LINK_TITLE}](README-207.md) · [Previous edition](README-206.md)"
              + " · [Offline walkthrough](docs/readmes/207/index.html) · [Full history](docs/readmes/INDEX.md)\n\n"
              + SUMMARY + main[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-207.md" not in index:
        begin = index.index("[Edition 206")
        write("docs/readmes/INDEX.md", index[:begin] + f"[Edition 207 — {LINK_TITLE}](../../README-207.md)"
              + " · [Walkthrough](207/index.html)\n\n" + index[begin:])
    record = (f"## Edition 207 — {TITLE}\n\n{DATE}. {SUMMARY} {result} "
              "Fraynix now handles a closed Electron output pipe without raising a second main-process error. "
              "A pending review reopens after restart, and changed comparison bytes prevent export. See README-207.md and docs/publication207/plan.md.\n\n")
    for path in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(path, record)

    note_name = "215-comparison-briefing"
    note = f"docs/obsidian/{note_name}.md"
    canvas = f"docs/obsidian/{note_name}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 207 — {LINK_TITLE}]({note_name}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 207](../../README-207.md) · "
          f"[Workflow map]({note_name}.canvas) · [Reproduction](../publication207/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Chat commands", "workspace-desktop/renderer/classification-routing.js"),
        ("Reviewed comparison briefing", "workspace-engine/src/main/java/fraymus/workspace/ComparisonBriefing.java"),
        ("Desktop output recovery", "workspace-desktop/process-stdio.cjs"),
        ("Desktop and export verification", "workspace-desktop/test/change-story-integration.cjs"),
    )]
    nodes = [module["source"] for module in modules] + ["README-207.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "comparison-briefing.html").as_posix(), "title": "Open the exported offline briefing"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Platform and test evidence"},
        {"path": "docs/publication207/plan.md", "title": "Reproduction and next candidate"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/206/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 207 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-206.md", "../../../README-207.md").replace(
        '../205/index.html">Edition 205', '../206/index.html">Edition 206')
    guide = guide.replace('<a href="change-story.html">Open the story</a>',
                          '<a href="comparison-briefing.html">Open the briefing</a>')
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
    assert manifest["latest"] == 206
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()) if path.is_file())]
    manifest["latest"] = 207
    manifest["editions"].append({"number": 207, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-207.md", "markdownSha256": item("README-207.md")["sha256"],
                                 "companion": "docs/readmes/207/index.html", "tag": "readme-207",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/207",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

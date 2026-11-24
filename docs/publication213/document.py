"""Freeze the edition 213 guide and its independently checked synthetic briefing."""
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

ROOT = Path("docs/readmes/213")
TITLE = "Brief me on that chart"
DATE = "2026-11-23"
SUMMARY = ("Turn an open named comparison chart into a briefing. Each claim carries the original earlier and later row "
           "citations, and the totals stay separate by unit.")
NOTE = "221-cited-chart-briefing"
SHOTS = {
    "desktop-briefing.png": "A plain-language follow-up opens the cited briefing in Fraynix",
    "offline-citation.png": "The offline briefing opens the 50 °F and 41 °F citations behind a 9 °F loss",
}
MODULES = (
    ("Cited claims and exact decimals", "workspace-engine/src/main/java/fraymus/workspace/RoutedChartBriefing.java"),
    ("Chart and briefing source binding", "workspace-engine/src/main/java/fraymus/workspace/RoutedDashboard.java"),
    ("Saved chart and briefing plans", "workspace-engine/src/main/java/fraymus/workspace/ComparisonChartBook.java"),
    ("Offline citation controls", "workspace-engine/src/main/resources/workbench/routing-chart-briefing.html"),
    ("Restart, refusal and offline proof", "workspace-desktop/test/chart-briefing-integration.cjs"),
)


def verified(proof):
    return (proof["passed"] and proof["mixedChecks"] == 20 and proof["emptyChecks"] == 13
            and len(proof["claims"]) == 3 and len(proof["checks"]) == 6
            and [(unit["unit"], unit["netDelta"]) for unit in proof["units"]] == [("°F", "9"), ("mm", "25.4")])


def publish(live, matrix_file):
    evidence = json.loads(read(live / "result.json"))
    matrix = json.loads(read(matrix_file))
    assert verified(evidence) and matrix["conclusion"] == "success" and len(matrix["targets"]) == 4
    for target in matrix["targets"]:
        result = target["report"]
        assert target["conclusion"] == "success" and verified(result)
        assert result["claims"] == evidence["claims"] and result["units"] == evidence["units"]
        assert result["sourceSha256"] == evidence["sourceSha256"]
        assert result["browserBriefingSha256"] == evidence["browserBriefingSha256"]

    ROOT.mkdir(exist_ok=True)
    for name in SHOTS:
        assert (live / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(live / name, ROOT / name)
    artifacts = ("claim-evidence.json", "named-chart.html", "chart-data.json", "matched-comparison.csv", "summary-proof.json")
    shutil.copyfile(live / "index.html", ROOT / "briefing.html")
    for name in artifacts:
        shutil.copyfile(live / name, ROOT / name)
    claims = json.loads(read(ROOT / "claim-evidence.json"))
    assert claims["browserBriefingSha256"] == evidence["browserBriefingSha256"] and claims["rows"] == evidence["claims"]
    assert claims["binding"]["chartDataSha256"] == sha256((ROOT / "chart-data.json").read_bytes()).hexdigest()
    assert sha256((ROOT / "matched-comparison.csv").read_bytes()).hexdigest() == evidence["sourceSha256"]
    write_json(ROOT / "release-proof.json", {"date": DATE, "windowsDesktop": evidence, "portableWorkspace": matrix,
                                            "screenshots": [item(ROOT / name) for name in SHOTS]})

    platforms = ("Windows x64, Apple silicon macOS, Intel macOS, and Linux x64 passed 20 browser checks for the mixed "
                 "briefing and 13 for the empty briefing.")
    recovery = ("After restart, the saved briefing reopens against its original chart even when a newer chart exists. "
                "A pending briefing can resume before any output exists.")
    refusal = ("A changed receipt, changed comparison bytes, and a falsified citation were refused. "
               "A request for causes asks for independent evidence.")
    write("README-213.md", f"""# Fraynix Agent Workspace · Edition 213

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 212](README-212.md) · [Offline walkthrough](docs/readmes/213/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![A named chart becomes a three-claim briefing in the desktop](docs/readmes/213/desktop-briefing.png)

## Ask for a briefing

1. Open a verified named comparison chart through [edition 212](README-212.md).
2. Ask **“Brief me on that chart.”** The visible chart supplies the exact selection, including when you reopen an older chart. **Brief me on the last comparison chart** explicitly selects the latest named chart.
3. Expand the citation below a claim to read both original rows and their exact values.
4. After restart, ask **Open my last chart briefing**. The saved request keeps its original chart and selection.
5. Open the original chart from the briefing or download the claim evidence. The sample files work together offline.

## Synthetic example

| Selected key | Earlier source row | Later source row | Verified delta |
| --- | --- | --- | --- |
| BACKrise <probe>BACK | Row 2 at 32 °F | Row 3 at 50 °F | +18 °F |
| BACKfallBACK | Row 3 at 50 °F | Row 2 at 41 °F | −9 °F |
| BACKlength gainBACK | Row 5 at 25.4 mm | Row 5 at 50.8 mm | +25.4 mm |

The selected temperature rows net to **+9 °F**. The length row nets to **+25.4 mm**. Java recomputes each difference and verifies the browser’s claims, totals, and citations against those values. The request for causes is handled separately. [Open the sample briefing](docs/readmes/213/briefing.html) · [Claim evidence](docs/readmes/213/claim-evidence.json)

![A keyboard-opened citation shows the two exact source readings](docs/readmes/213/offline-citation.png)

## Verification

- {platforms}
- {recovery}
- {refusal} The offline browser opened the row citations by keyboard, downloaded the exact claim evidence, and followed the original chart link. [CI run]({matrix["url"]}) · [Release proof](docs/readmes/213/release-proof.json)

## How the briefing stays grounded

BACKBACKBACKmermaid
flowchart LR
    Chat["Brief me on that chart"] --> Bind["Bind the displayed chart and its selection"]
    Bind --> Source["Recheck comparison and chart receipts"]
    Source --> Claims["Recompute changes and cite both rows"]
    Claims --> Check["Verify browser claims and citations"]
    Check --> Save["Save briefing, chart and original evidence"]
    Save --> Resume["Reopen and recheck the same briefing"]
BACKBACKBACK

## Next acceptance test

Attach a separate observation and audit one proposed explanation for a change. Record its date, assumptions, and uncertainty. Test the claim with missing or conflicting evidence.

[Reproduction and verification plan](docs/publication213/plan.md) · [Obsidian map](docs/obsidian/221-cited-chart-briefing.canvas)
""".replace("BACK", chr(96)))
    home = read("README.md").split("[Edition ", 1)[0]
    write("README.md", home + "[Edition 213 — Brief me on that chart](README-213.md) · [Previous edition](README-212.md)"
          " · [Offline walkthrough](docs/readmes/213/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    index = read("docs/readmes/INDEX.md")
    if "README-213.md" not in index:
        write("docs/readmes/INDEX.md", index.replace("[Edition 212", "[Edition 213 — Brief me on that chart](../../README-213.md)"
              " · [Walkthrough](213/index.html)\n\n[Edition 212", 1))
    record = (f"## Edition 213 — {TITLE}\n\n{DATE}. {SUMMARY} {recovery} {refusal} {platforms} "
              "Preserve the chart artifact, comparison CSV hash, exact selected keys and both original citations for every claim. "
              "See README-213.md and docs/publication213/plan.md.\n\n")
    for path in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(path, record)

    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 213 — Brief me on that chart]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{recovery}\n\n[Edition 213](../../README-213.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication213/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in MODULES]
    nodes = [row["source"] for row in modules] + ["README-213.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": file, "x": (i % 3) * 380, "y": (i // 3) * 290,
                   "width": 350, "height": 250} for i, file in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "toNode": str(i)} for i in range(1, len(nodes))],
    })
    gallery = [{"path": (ROOT / name).as_posix(), "title": caption} for name, caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT / "briefing.html").as_posix(), "title": "Try the cited offline briefing"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Desktop and portable CI evidence"},
        {"path": "docs/publication213/plan.md", "title": "Reproduce the cited briefing"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source = matrix["headSha"]
    files = subprocess.check_output(["git", "diff", "--name-only", "readme-212", source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas, "docs/publication213/plan.md"})]
    write_json(ROOT / "file-inventory.json", inventory)

    guide = read("docs/readmes/212/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 213 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{TITLE}</h1><p>{SUMMARY}</p>", guide, count=1)
    guide = guide.replace("../../../README-212.md", "../../../README-213.md").replace(
        '../211/index.html">Edition 211', '../212/index.html">Edition 212').replace(
        'chart.html">Open the interactive example', 'briefing.html">Open the cited briefing')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)
    manifest = json.loads(subprocess.check_output(["git", "show", "readme-212:docs/readmes/editions.json"]))
    assert manifest["latest"] == 212
    assets = [item(path) for path in sorted(ROOT.iterdir()) if path.is_file()]
    frozen = [{"path": row["path"], "sha256": row["sha256"], "kind": "binary" if row["hashKind"] == "binary" else "text"}
              for row in assets]
    manifest["latest"] = 213
    manifest["editions"].append({"number": 213, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-213.md", "markdownSha256": item("README-213.md")["sha256"],
                                 "companion": (ROOT / "index.html").as_posix(), "tag": "readme-213",
                                 "sourceBaseline": source, "snapshotDirectory": ROOT.as_posix(), "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(f"Edition 213 prepared with {len(gallery)} screenshots and {len(inventory)} inventory entries.")


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

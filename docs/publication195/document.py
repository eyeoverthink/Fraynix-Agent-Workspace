"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "3c6d6352501a16574f1d470437f455e43b3aadaa"
ROOT = Path("docs/readmes/195")
TITLE = "Teach a checked classifier from rules in a CSV"
LINK_TITLE = "Verified CSV range lessons"
SUMMARY = ("Teach a classifier from explicit CSV ranges, then reuse the checked rules on new measurements. "
           "Java checks every browser result, including unseen values and exact range boundaries.")
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
    tests = json.loads(read("build/edition195-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    assert app["checks"]["restartCount"] == 3 and len(app["legacy"]) == 2
    assert app["fresh"]["lesson"]["teacherId"] == app["teacherId"]
    assert all(evidence["passed"] and all(case["java"] == case["browser"] for case in evidence["cases"])
               for evidence in (app["teacher"], app["fresh"], app["exhausted"]))
    receipt = json.loads(read(live / "browser-receipt.json"))
    assert receipt["passed"] and len(receipt["checks"]) == 20
    assert sha256((live / "offline-classifier.html").read_bytes()).hexdigest() == app["fresh"]["htmlSha256"]
    assert sha256((live / "sensor-rules.csv").read_bytes()).hexdigest() == app["teacher"]["rulesSha256"]
    assert app["teacher"]["ruleProgramSha256"] == app["fresh"]["ruleProgramSha256"]
    assert app["teacher"]["unseenValidCases"] > 0 and app["exhausted"]["unseenValidCases"] == 0
    ROOT.mkdir(exist_ok=True)
    write(ROOT / "example-rules.csv", app["teacher"]["ruleTable"] + "\n")
    for name in ("browser-receipt.json", "offline-classifier.html"):
        shutil.copyfile(live / name, ROOT / name)

    labels = {
        "chat-teaches-ranges.png": "Chat saves the named lesson after the original classifier verifies",
        "rules-and-example-reviewed.png": "Workbench binds the exact rules CSV and measurement CSV",
        "fresh-range-result.png": "Canvas shows the checked intervals and seven fresh classifications",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})

    fresh = app["fresh"]["inputCount"]
    result = (f"The saved lesson classified {fresh} fresh rows using four ranges. Java compared the full output "
              f"and the browser passed 20 checks, including both sides of shared boundaries and the first gap.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    timing = app["timing"]
    write("README-195.md", f"""# Fraynix Agent Workspace · Edition 195

[Edition 194](README-194.md) · [Offline walkthrough](docs/readmes/195/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The checked rules and example input in Workbench](docs/readmes/195/rules-and-example-reviewed.png)

## Try it

1. Send `Build a data-to-tool classifier using column reading.`
2. Attach a [rules CSV](docs/readmes/195/example-rules.csv). Its headers are `min_inclusive,max_exclusive,label`. Then attach a measurement CSV with a `reading` column and choose **Build and verify in browser**.
3. Send `Teach data-to-tool lesson sample-ranges` after the original verifies. The lesson keeps the exact rule file hash and the checked rule definitions.
4. Send `Use data-to-tool lesson sample-ranges` and attach fresh measurements. The browser tool appends `classification` and preserves the other cells. Use **Save output to project** to export it.

![Chat saves the named lesson](docs/readmes/195/chat-teaches-ranges.png)

## What passed

{result} A reading of `9.9999` was normal and `10` was warm. Values in an undeclared gap or at an exclusive upper boundary without a following rule were refused. Changing the original rule file invalidated the prior review. Reuse rejected a replacement rules upload and refused altered teaching evidence. A two-value example could build a tool, but Java refused to teach from it because no unseen valid measurement remained.

The verified lesson, the pending review and the completed proof survived three restarts. The exported HTML classified a new row on its own. Two edition 194 converters reverified against their original evidence.

![The offline classifier shows the checked rule intervals and outputs](docs/readmes/195/fresh-range-result.png)

The original four-row build took {timing['teacher']['buildVerifyMs']} ms. The seven-row reuse took {timing['fresh']['buildVerifyMs']} ms. Both observations include browser checks, save and Java verification on the development machine. {counts}

The rules are limited to two to six nonoverlapping numeric ranges with a fixed label per range. This edition produces and verifies labels.

[Run the offline classifier](docs/readmes/195/offline-classifier.html) · [Browser receipt](docs/readmes/195/browser-receipt.json) · [Verification](docs/readmes/195/verification.json) · [Implementation and next phase](docs/publication195/plan.md)
""")
    previous = "[Edition 194 — Named-column CSV lessons](README-194.md)"
    lead = f"[Edition 195 — {LINK_TITLE}](README-195.md) · [Previous edition](README-194.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/195/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-195.md" not in index:
        start = index.index("[Edition 194")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 195 — {LINK_TITLE}](../../README-195.md)"
              + " · [Walkthrough](195/index.html)\n\n" + index[start:])
    record = (f"## Edition 195 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "The teaching source, pending review and completed proof survived restart. Two edition 194 results reverified. "
              "See README-195.md and docs/publication195/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/203-verified-range-lessons.md"
    canvas = "docs/obsidian/203-verified-range-lessons.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 195 — {LINK_TITLE}](203-verified-range-lessons.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 195](../../README-195.md) · "
          "[Workflow map](203-verified-range-lessons.canvas) · [Implementation plan](../publication195/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Range rules", java + "FrayRangeRules.java"),
        ("CSV tool verifier", java + "FrayDataTool.java"),
        ("Teaching and recovery", java + "RequestWorkbench.java"),
        ("Workbench controls", "workspace-desktop/renderer/request-workbench.js"),
    )]
    nodes = [module["source"] for module in modules] + ["README-195.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "browser-receipt.json").as_posix(), "title": "Original Chromium receipt"},
        {"path": (ROOT / "offline-classifier.html").as_posix(), "title": "Runnable offline tool"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/194/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 195 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-194.md", "../../../README-195.md").replace(
        '../193/index.html">Edition 193', '../194/index.html">Edition 194')
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
    assert manifest["latest"] == 194
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 195
    manifest["editions"].append({"number": 195, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-195.md", "markdownSha256": item("README-195.md")["sha256"],
                                 "companion": "docs/readmes/195/index.html", "tag": "readme-195",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/195",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition195-live"))

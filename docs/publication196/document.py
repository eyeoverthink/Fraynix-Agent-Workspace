"""Freeze the measured desktop run and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "966b4618ade5ac0f7244ec19aa79b3a60ba16e54"
ROOT = Path("docs/readmes/196")
TITLE = "Route verified classifications to approved procedures"
LINK_TITLE = "Reviewed classification routing"
SUMMARY = ("Attach a mapping from classification labels to verified conversion lessons. "
           "Review the reason and exact input for each choice, then approve the plan.")
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
    tests = json.loads(read("build/edition196-regression/summary.json"))
    assert app["passed"] and tests["passed"] and all(app["checks"].values())
    assert app["checks"]["restartCount"] == 2 and len(app["legacy"]) == 2
    assert len(app["reviewed"]["rows"]) == 3 and app["reviewed"]["status"] == "review"
    assert app["paused"]["status"] == "approved" and "result" not in app["paused"]["steps"][1]
    assert app["done"]["status"] == "verified" and all("result" in step for step in app["done"]["steps"])
    ROOT.mkdir(exist_ok=True)
    shutil.copyfile(live / "example-mapping.csv", ROOT / "example-mapping.csv")
    write_json(ROOT / "routing-evidence.json", app["done"])
    labels = {
        "routing-preview-and-reasons.png": "The plan previews the classification, range and exact input for each row",
        "routing-one-result-saved.png": "The first conversion is verified while the other is pending",
        "routing-resumed-and-verified.png": "Both verified artifacts are available after desktop restart",
    }
    screenshots = []
    for shot in app["screenshots"]:
        name = shot["name"]
        assert name in labels and item(live / name)["sha256"] == shot["sha256"]
        shutil.copyfile(live / name, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": labels[name]})
    assert {Path(shot["path"]).name for shot in screenshots} == set(labels)
    write_json(ROOT / "verification.json", {**app, "source": source, "tests": tests})

    result = ("Three classified rows led to two confirmed conversion batches. After two restarts, "
              "Java finished the same plan with both original browser receipts.")
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed locally.")
    write("README-196.md", f"""# Fraynix Agent Workspace · Edition 196

[Edition 195](README-195.md) · [Offline walkthrough](docs/readmes/196/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The routing preview shows reasons and exact inputs](docs/readmes/196/routing-preview-and-reasons.png)

## Try it

1. Verify and teach two CSV conversion lessons. The example uses `temperature-check` for `probe_c` and `length-check` for `distance_in`.
2. Verify a range classifier on a CSV with the classification column and the inputs required by the lessons. For the example, use a `reading` column and label the ranges `temperature` and `length`.
3. Send `Route the last verified classification.` Attach a [mapping CSV](docs/readmes/196/example-mapping.csv), review the choices and choose **Confirm and run**.
4. To stop between batches, choose **Pause after the next verified procedure**. Send `Resume classification routing.` after restart. Each completed batch has a button that opens its verified artifact.

![One saved outcome and one pending procedure](docs/readmes/196/routing-one-result-saved.png)

## What passed

{result} The temperature batch converted `37.25` and `−40` °C to `99.05` and `−40` °F. The length batch converted `2.5` inches to `63.5` millimetres. The second batch kept its ID after a restart before execution and kept its original artifact after a restart before the plan recorded completion.

Java refused a missing mapping, an unknown lesson, an unverified classifier, a stale approval and altered classifier or child receipts. The desktop created zero child reviews before approval and exactly two new verified artifacts afterward. Two edition 195 classifier artifacts also reverified.

![The recovered plan with both result buttons](docs/readmes/196/routing-resumed-and-verified.png)

This edition routes to saved Celsius-to-Fahrenheit and inches-to-millimetres lessons. Each approved batch produces its own artifact. {counts}

[Routing evidence](docs/readmes/196/routing-evidence.json) · [Verification results](docs/readmes/196/verification.json) · [Implementation and next phase](docs/publication196/plan.md)
""")
    previous = "[Edition 195 — Verified CSV range lessons](README-195.md)"
    lead = f"[Edition 196 — {LINK_TITLE}](README-196.md) · [Previous edition](README-195.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index(previous)
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + lead + " · [Offline walkthrough](docs/readmes/196/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-196.md" not in index:
        start = index.index("[Edition 195")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 196 — {LINK_TITLE}](../../README-196.md)"
              + " · [Walkthrough](196/index.html)\n\n" + index[start:])
    record = (f"## Edition 196 — {TITLE}\n\n{DATE}. {SUMMARY} {result} {counts} "
              "Missing mappings and stale approvals created no child work. Two edition 195 artifacts reverified. "
              "See README-196.md and docs/publication196/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note = "docs/obsidian/204-classification-routing.md"
    canvas = "docs/obsidian/204-classification-routing.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 196 — {LINK_TITLE}](204-classification-routing.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{result}\n\n[Edition 196](../../README-196.md) · "
          "[Workflow map](204-classification-routing.canvas) · [Implementation plan](../publication196/plan.md)\n")
    java = "workspace-engine/src/main/java/fraymus/workspace/"
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Classification routing plan", java + "ClassifiedProcedurePlan.java"),
        ("Verified child procedures", java + "RequestWorkbench.java"),
        ("Routing preview and approval", "workspace-desktop/renderer/classification-routing.js"),
    )]
    nodes = [module["source"] for module in modules] + ["README-196.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": (ROOT / "verification.json").as_posix(), "title": "Verification record"},
        {"path": (ROOT / "routing-evidence.json").as_posix(), "title": "Approved routing and verified child results"},
        {"path": (ROOT / "example-mapping.csv").as_posix(), "title": "Example mapping"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/195/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 196 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-195.md", "../../../README-196.md").replace(
        '../194/index.html">Edition 194', '../195/index.html">Edition 195')
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
    assert manifest["latest"] == 195
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()))]
    manifest["latest"] = 196
    manifest["editions"].append({"number": 196, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-196.md", "markdownSha256": item("README-196.md")["sha256"],
                                 "companion": "docs/readmes/196/index.html", "tag": "readme-196",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/196",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition196-live"))

"""Freeze the verified public example and the edition 214 documentation."""
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

ROOT = Path("docs/readmes/214")
DATE = "2026-11-23"
TITLE = "Put an explanation to the test"
SUMMARY = ("Ask Fraynix to audit a proposed explanation for a cited change. "
           "Attach separate trial observations to see the assumptions, interval and outcome in a verified offline report.")
NEXT = ("Let the local model propose one observation that distinguishes two competing explanations. Review its expected outcomes "
        "and cost, then use Game Lab to run a bounded experiment and audit the observed result.")
NOTE = "222-explanation-audit"
SHOTS = {
    "desktop-draft.png": "Chat opens a saved proposal and waits for separate trial observations",
    "desktop-mixed.png": "Fraynix shows a conflicting trial alongside a consistent one",
    "offline-audit.png": "The offline report shows the assessed range and expands a dated paired trial",
}
MODULES = (
    ("Exact trial effects and error bounds", "workspace-engine/src/main/java/fraymus/workspace/ExplanationEvidence.java"),
    ("Durable proposal and source binding", "workspace-engine/src/main/java/fraymus/workspace/ExplanationAuditBook.java"),
    ("Report and independent browser checks", "workspace-engine/src/main/java/fraymus/workspace/RoutedExplanationAudit.java"),
    ("Desktop review and observation attachment", "workspace-desktop/renderer/explanation-audit.js"),
    ("Restart and changed-evidence tests", "workspace-desktop/test/explanation-audit-integration.cjs"),
)


def verified(proof):
    expected = ["conflicting", "mixed", "inconclusive", "inconclusive"]
    return (proof["passed"] and proof["browserChecks"] == 14 and len(proof["checks"]) == 7
            and proof["outcome"]["status"] == "supports-prediction"
            and [row["status"] for row in proof["otherOutcomes"]] == expected
            and proof["evidence"]["prediction"] == {"low": "-10", "high": "-8"}
            and [row["effect"] for row in proof["evidence"]["trials"]] == ["-9", "-9"])


def publish(live, matrix_file):
    proof = json.loads(read(live / "result.json"))
    matrix = json.loads(read(matrix_file))
    assert verified(proof) and matrix["conclusion"] == "success" and len(matrix["targets"]) == 4
    for target in matrix["targets"]:
        result = target["report"]
        assert target["conclusion"] == "success" and verified(result)
        for key in ("claim", "proposal", "trials", "observationsSha256", "originalComparisonCsvSha256"):
            assert result["evidence"][key] == proof["evidence"][key]

    ROOT.mkdir(exist_ok=True)
    for name in SHOTS:
        assert (live / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(live / name, ROOT / name)
    shutil.copyfile(live / "index.html", ROOT / "audit.html")
    for name in ("observations.csv", "claim-evidence.json", "audit-evidence.json"):
        shutil.copyfile(live / name, ROOT / name)
    assert json.loads(read(ROOT / "audit-evidence.json")) == proof["evidence"]
    assert sha256((ROOT / "observations.csv").read_bytes()).hexdigest() == proof["evidence"]["observationsSha256"]
    write_json(ROOT / "release-proof.json", {"date": DATE, "windowsDesktop": proof, "portableWorkspace": matrix,
                                            "screenshots": [item(ROOT / name) for name in SHOTS]})

    platforms = "Windows x64, Apple silicon macOS, Intel macOS, and Linux x64 each passed the audit suite."
    recovery = "The draft and completed audit reopen after separate desktop restarts. The app refuses changed original files, receipts, trial attachments or browser evidence."
    scope = "The result measures numerical agreement under the declared assumptions. The references, trial design, and error bounds come from the attached observations and proposal."
    write("README-214.md", f"""# Fraynix Agent Workspace · Edition 214

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 213](README-213.md) · [Offline walkthrough](docs/readmes/214/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Fraynix displays mixed trial evidence against one prediction](docs/readmes/214/desktop-mixed.png)

## Try it in Chat

1. Open a [verified chart briefing](README-213.md) and ask **“Audit a proposed explanation for that briefing.”**
2. Select one change. Enter the proposed explanation, assumptions, and the allowed difference from the original change.
3. Attach a [separate trial CSV](docs/readmes/214/observations.csv) and select **Run audit**. If the observations are pending, select **Save draft**.
4. Expand each trial to inspect the source reference, timestamp, treatment and control readings, and adjusted effect.
5. Say **“Resume my last explanation audit.”** The draft or artifact reopens with its original briefing after restart. Say **“Audit an explanation for the last chart briefing”** to deliberately select the latest briefing.

![A saved audit draft waits for observation data](docs/readmes/214/desktop-draft.png)

## See the reasoning

For the synthetic temperature example, the original measured change is **−9 °F**. A tolerance of **1 °F** sets a prediction of **−10 to −8 °F**. Each trial supplies treatment and control readings before and after.

| Trial | Treatment change | Control change | Adjusted effect | Declared error bound | Assessed interval |
| --- | --- | --- | --- | --- | --- |
| A | −11 °F | −2 °F | −9 °F | ±0.25 °F | −9.25 to −8.75 °F |
| B | −9 °F | 0 °F | −9 °F | ±0.5 °F | −9.5 to −8.5 °F |

Both intervals fit inside the prediction. The app reports **Trials support the prediction**. Separate tested inputs produce **Mixed trial evidence**, **Trials conflict with the prediction**, and **Trial evidence is inconclusive**. {scope} [Open the sample audit](docs/readmes/214/audit.html)

![The offline sample expands to show the exact treatment and control values](docs/readmes/214/offline-audit.png)

## Verification

- {platforms} Each completed fixture passed 14 live browser checks against Java. The report works without a model.
- {recovery}
- The standalone audit opens with network access blocked and exports the original trial CSV byte for byte. [CI run]({matrix["url"]}) · [Release proof](docs/readmes/214/release-proof.json)

## Next acceptance test

{NEXT}

[Reproduction and verification plan](docs/publication214/plan.md) · [Obsidian map](docs/obsidian/{NOTE}.canvas)
""")
    home = read("README.md").split("[Edition ", 1)[0]
    write("README.md", home + f"[Edition 214 — {TITLE}](README-214.md) · [Previous edition](README-213.md)"
          " · [Offline walkthrough](docs/readmes/214/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    index = read("docs/readmes/INDEX.md")
    if "README-214.md" not in index:
        write("docs/readmes/INDEX.md", index.replace("[Edition 213", f"[Edition 214 — {TITLE}](../../README-214.md)"
              " · [Walkthrough](214/index.html)\n\n[Edition 213", 1))
    record = (f"## Edition 214 — {TITLE}\n\n{DATE}. {SUMMARY} {recovery} {platforms} {scope} "
              "Keep the sealed audit and all existing source and browser checks. See README-214.md and docs/publication214/plan.md.\n\n")
    for file in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(file, record)

    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 214 — {TITLE}]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{recovery}\n\n{NEXT}\n\n[Edition 214](../../README-214.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication214/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in MODULES]
    nodes = [row["source"] for row in modules] + ["README-214.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": file, "x": (i % 3) * 380, "y": (i // 3) * 290,
                   "width": 350, "height": 250} for i, file in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "toNode": str(i)} for i in range(1, len(nodes))],
    })
    gallery = [{"path": (ROOT / name).as_posix(), "title": caption} for name, caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT / "audit.html").as_posix(), "title": "Open the verified offline audit"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Desktop and portable CI evidence"},
        {"path": "docs/publication214/plan.md", "title": "Reproduce the explanation audit"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source = matrix["headSha"]
    files = subprocess.check_output(["git", "diff", "--name-only", "readme-213", source], text=True).splitlines()
    inventory = [item(file) for file in sorted(set(files) | {note, canvas, "docs/publication214/plan.md"})]
    write_json(ROOT / "file-inventory.json", inventory)

    guide = read("docs/readmes/213/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 214 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{TITLE}</h1><p>{SUMMARY}</p>", guide, count=1)
    guide = guide.replace("../../../README-213.md", "../../../README-214.md").replace(
        '../212/index.html">Edition 212', '../213/index.html">Edition 213').replace(
        'briefing.html">Open the cited briefing', 'audit.html">Open the verified audit')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)

    manifest = json.loads(subprocess.check_output(["git", "show", "readme-213:docs/readmes/editions.json"]))
    assert manifest["latest"] == 213
    assets = [item(file) for file in sorted(ROOT.iterdir()) if file.is_file()]
    frozen = [{"path": row["path"], "sha256": row["sha256"], "kind": "binary" if row["hashKind"] == "binary" else "text"}
              for row in assets]
    manifest["latest"] = 214
    manifest["editions"].append({"number": 214, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-214.md", "markdownSha256": item("README-214.md")["sha256"],
                                 "companion": (ROOT / "index.html").as_posix(), "tag": "readme-214",
                                 "sourceBaseline": source, "snapshotDirectory": ROOT.as_posix(), "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(f"Edition 214 prepared with {len(gallery)} screenshots and {len(inventory)} inventory entries.")


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

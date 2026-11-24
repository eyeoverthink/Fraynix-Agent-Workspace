"""Publish the edition 215 evidentiary examples, screenshots and archive."""
import importlib.util
from hashlib import sha256
from html import escape
import base64
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

ROOT = Path("docs/readmes/215")
TITLE = "Test two competing explanations"
DATE = "2026-11-23"
NOTE = "223-hypothesis-experiment"
SUMMARY = ("A local model suggests one observation to distinguish two Game Lab explanations. "
           "You review its predictions and budget before Java runs the original course, replays it and reports the result.")
NEXT = ("Offer two credible probes for the same competing explanations, then measure which one separates predictions at lower "
        "cost across different starting conditions.")
MODULES = (
    ("Proposal, approval and native replay", "workspace-engine/src/main/java/fraymus/workspace/HypothesisExperiment.java"),
    ("Review and result in Chat", "workspace-desktop/renderer/hypothesis-experiment.js"),
    ("Offline report with checked Game Lab frame", "workspace-engine/src/main/resources/workbench/game-hypothesis-experiment.html"),
    ("Live local-model desktop tests", "workspace-desktop/test/hypothesis-experiment-integration.cjs"),
    ("Proposal refusals and game replay tests", "workspace-engine/src/test/java/fraymus/workspace/HypothesisExperimentTest.java"),
)
SHOTS = {
    "review.png": "A pending review shows the local model’s proposal, predicted ranges and runtime budget",
    "right.png": "Fraynix shows a measured horizontal displacement and separate verdicts",
    "jump.png": "Fraynix chooses a vertical measurement for the Jump question",
}


def publish(right, jump, matrix_file):
    matrix = json.loads(read(matrix_file))
    assert matrix["conclusion"] == "success" and len(matrix["targets"]) == 4
    assert all(row["conclusion"] == "success" and row["hypothesisConclusion"] == "success" for row in matrix["targets"])
    artifacts, rows = {}, []
    for key, source, probe, ticks in (("right", right, "right-24", 48), ("jump", jump, "jump-12", 36)):
        live = json.loads(read(source / "result.json"))
        data = json.loads(read(source / "experiment-evidence.json"))
        assert live["passed"] and live["model"] == "gpt-oss:20b"
        assert live["review"]["proposal"]["probe"] == probe
        assert live["result"] == data["result"] and live["result"]["primary"] == live["result"]["replay"]
        assert live["result"]["a"] == "Conflicts with prediction" and live["result"]["b"] == "Supports prediction"
        assert data["original"]["observed"]["tick"] == data["replay"]["observed"]["tick"] == ticks
        assert data["original"]["sessionId"] != data["replay"]["sessionId"]
        # The sample contains fixture claims and a loopback model receipt.
        assert re.fullmatch(r"https?://(?:127[.]0[.]0[.]1|localhost):\d+", live["review"]["provider"]["endpoint"])
        match = re.search('src="data:image/png;base64,([^"]+)"', read(source / "index.html"))
        assert match and sha256(base64.b64decode(match[1])).hexdigest() == data["original"]["frameSha256"]
        (ROOT / key).mkdir(parents=True, exist_ok=True)
        for file in ("index.html", "experiment-evidence.json"):
            shutil.copyfile(source / file, ROOT / key / file)
        shutil.copyfile(source / "result.png", ROOT / f"{key}.png")
        artifacts[key] = live
        rows.append(f"| {key.title()} | {ticks} | {live['result']['primary']} | {live['result']['replay']} |")
    jump_a = artifacts["jump"]["review"]["proposal"]["a"]
    range_note = ""
    if jump_a != {"low": "-0.01", "high": "0.01"}:
        range_note = ("In the Jump example, the starting explanation gave −0.01 to 0.01 game units for A. "
                      f"The model proposed {jump_a['low']} to {jump_a['high']}. The review showed that range, "
                      "and Java used the approved numbers.")
    shutil.copyfile(right / "review.png", ROOT / "review.png")
    for name in SHOTS:
        assert (ROOT / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
    write_json(ROOT / "release-proof.json", {"date": DATE, "windowsLocalModel": artifacts,
                                            "portableGameAndReplay": matrix,
                                            "screenshots": [item(ROOT / name) for name in SHOTS]})

    native = [row["name"] for row in matrix["targets"] if row["native"]["nativeGame"] == "verified-replay"]
    display = [row["name"] for row in matrix["targets"] if row["native"]["nativeGame"] == "unavailable-opengl"]
    assert {"Windows x64", "Linux x64"}.issubset(native)
    platforms = ("Native approval and replay tests with a supplied proposal passed on " + " and ".join(native) + ".")
    if display:
        platforms += (" Hosted " + " and ".join(display)
                      + " runners reported no OpenGL 4.1 display and verified that the review stopped without a result.")
    tests = ("Two Windows desktop runs used the real gpt-oss:20b local model. Both reviews survived restart and "
             "a changed measurement checkpoint was refused.")
    write("README-215.md", f"""# Fraynix Agent Workspace · Edition 215

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 214](README-214.md) · [Offline walkthrough](docs/readmes/215/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![The local model’s proposal is ready for approval](docs/readmes/215/review.png)

## Try it in Chat

1. Select a tool-capable local model and ask **“What observation would distinguish these two explanations?”**
2. Describe two explanations that make different predictions in the original Relay Ridge scene. You can supply expected ranges in game units for clearer comparisons.
3. Review the model’s proposed observation, ranges, original course and tick budget. Select **Approve and run**.
4. Inspect the result for each explanation, the original and replay positions, and the captured Game Lab frame.
5. Say **“Resume my last hypothesis experiment”** after restart to reopen the review or recheck the completed report.

## What the model chose and Java measured

| Question | Measured tick | Original displacement | Fresh replay |
| --- | --- | --- | --- |
{chr(10).join(rows)}

For the horizontal question, the local model chose to hold Right for 24 ticks and measure X. For the jump question, it chose to press Jump without horizontal input for 12 ticks and measure Y. In both examples, the measured interval fit the enabled-input prediction and fell outside the disabled-input prediction. The verdict applies to the approved ranges and original simulated scene. {range_note} [Right report](docs/readmes/215/right/index.html) · [Jump report](docs/readmes/215/jump/index.html)

![The desktop shows the measured result and each explanation’s verdict](docs/readmes/215/jump.png)

## Verification

- {tests}
- {platforms}
- Each run starts after an explicit approval of the exact saved plan. The original course, installed engine, clock, player identity, measured positions and native game frame are checked. Replay runs in a second process. Java checks six browser assertions against the displayed verdict and native measurements. The active Game Lab session is separate. [CI run]({matrix["url"]}) · [Release proof](docs/readmes/215/release-proof.json)

## Next acceptance test

{NEXT}

[Reproduction and verification plan](docs/publication215/plan.md) · [Obsidian map](docs/obsidian/{NOTE}.canvas)
""")
    home = read("README.md").split("[Edition ", 1)[0]
    write("README.md", home + f"[Edition 215 — {TITLE}](README-215.md) · [Previous edition](README-214.md)"
          " · [Offline walkthrough](docs/readmes/215/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    index = read("docs/readmes/INDEX.md")
    if "README-215.md" not in index:
        write("docs/readmes/INDEX.md", index.replace("[Edition 214", f"[Edition 215 — {TITLE}](../../README-215.md)"
              " · [Walkthrough](215/index.html)\n\n[Edition 214", 1))
    record = (f"## Edition 215 — {TITLE}\n\n{DATE}. {SUMMARY} {tests} {platforms} "
              "Keep proposal authorization separate from game observations. Pin original inputs and replay in fresh sessions. "
              "See README-215.md and docs/publication215/plan.md.\n\n")
    for file in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(file, record)
    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 215 — {TITLE}]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{tests}\n\n{NEXT}\n\n[Edition 215](../../README-215.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication215/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in MODULES]
    nodes = [module["source"] for module in modules] + ["README-215.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": file, "x": (i % 3) * 380, "y": (i // 3) * 290,
                   "width": 350, "height": 250} for i, file in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "toNode": str(i)} for i in range(1, len(nodes))],
    })
    gallery = [{"path": (ROOT / name).as_posix(), "title": caption} for name, caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT / "right/index.html").as_posix(), "title": "Open the verified Right report"},
        {"path": (ROOT / "jump/index.html").as_posix(), "title": "Open the verified Jump report"},
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Live model proof and portable Java tests"},
        {"path": "docs/publication215/plan.md", "title": "Reproduce the experiment"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source_commit = matrix["headSha"]
    files = subprocess.check_output(["git", "diff", "--name-only", "readme-214", source_commit], text=True).splitlines()
    inventory = [item(file) for file in sorted(set(files) | {note, canvas, "docs/publication215/document.py"})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/214/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 215 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{TITLE}</h1><p>{SUMMARY}</p>", guide, count=1)
    guide = guide.replace("../../../README-214.md", "../../../README-215.md").replace(
        '../213/index.html">Edition 213', '../214/index.html">Edition 214').replace(
        '<a href="audit.html">Open the verified audit</a>',
        '<a href="right/index.html">Right experiment</a><a href="jump/index.html">Jump experiment</a>')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)
    manifest = json.loads(subprocess.check_output(["git", "show", "readme-214:docs/readmes/editions.json"]))
    assert manifest["latest"] == 214
    assets = [item(file) for file in sorted(ROOT.rglob("*")) if file.is_file()]
    frozen = [{"path": row["path"], "sha256": row["sha256"], "kind": "binary" if row["hashKind"] == "binary" else "text"}
              for row in assets]
    manifest["latest"] = 215
    manifest["editions"].append({"number": 215, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-215.md", "markdownSha256": item("README-215.md")["sha256"],
                                 "companion": (ROOT / "index.html").as_posix(), "tag": "readme-215",
                                 "sourceBaseline": source_commit, "snapshotDirectory": ROOT.as_posix(), "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print("Verified both live model examples and created edition 215.")


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

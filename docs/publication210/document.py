"""Publish the edition 210 walkthrough from verified desktop results."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path("docs/readmes/210")
DATE = "2026-11-23"
TITLE = "Ask the workspace in everyday language"
SUMMARY = ("Ask Chat about a recorded object, compare two saved ramp setups, convert a file or build an offline click counter. "
           "Java binds each input and checks the result.")
NOTE = "218-plain-language"
SHOTS = {
    "recorded-motion.png": "Chat answers from a 49-sample recorded motion dataset",
    "ramp-comparison.png": "Mission shows an independently replayed ramp comparison from an everyday question",
    "file-conversion-review.png": "Reviewing the exact CSV before conversion",
    "local-model-review.png": "Mission waits for approval of the local model's proposal",
    "created-counter.png": "The locally generated click counter passed the Java-owned browser checks",
}


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
    before = read(path)
    if not before.startswith(text):
        write(path, text + before)


def render(markdown, path, before, after):
    text = read(path)
    if markdown not in text:
        write(path, text.replace(before, after, 1))


def publish(live, matrix_file):
    local = json.loads(read(live / "summary.json"))
    matrix = json.loads(read(matrix_file))
    assert local["passed"] and local["model"] and not local["errors"]
    assert set(local["routes"]) == {"ramps", "recording", "file", "review", "counter"}
    assert local["routes"]["review"]["modelCalls"] == 1
    assert local["routes"]["counter"]["independentChecks"] == 9
    assert set(local["screenshots"]) == set(SHOTS)

    ROOT.mkdir(exist_ok=True)
    for name in SHOTS:
        assert (live / name).read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(live / name, ROOT / name)
    write_json(ROOT / "release-proof.json", {"date": DATE, "localDesktop": local, "portableWorkspace": matrix,
                                            "screenshots": [item(ROOT / name) for name in SHOTS]})
    targets = matrix["targets"]
    passed = [row["name"] for row in targets if row["conclusion"] == "success"]
    portable = ("The model-free desktop proof passed on " + ", ".join(passed[:-1]) + " and " + passed[-1] + "."
                if len(passed) > 1 else f"Model-free hosted proof passed on {passed[0]}." if passed else
                "See the hosted run for the status of the model-free matrix.")
    record = local["routes"]["recording"]
    model = local["model"]
    ramp = local["routes"]["ramps"]["distancesM"]
    docs_gate = ("On Windows, the real local model proposed a motion report. After restart, the original proposal "
                 "still required approval and the approved run used the original recorded source without a second model call.")
    write("README-210.md", f"""# Fraynix Agent Workspace · Edition 210

[Install on Mac, Windows or Linux](docs/INSTALL.md) · [Edition 209](README-209.md) · [Offline walkthrough](docs/readmes/210/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Chat gives the computed travel and average speed of the selected recording](docs/readmes/210/recorded-motion.png)

## Ask for an outcome

| Say or type in Chat | Give Fraynix | What it verifies |
| --- | --- | --- |
| “Which of these two ramps will the block go farther down?” | Open the Ramp input page for two saved setups with the same duration. | Java simulates both and verifies the comparison against independent replays. |
| “How far did it travel in the selected motion recording?” | Select the existing recording in Motion data. | Java reports recorded travel and average speed, checks the source and verifies the report. |
| “Turn this CSV into JSON, please.” | Attach the file in Workbench and choose **Confirm and verify**. | Java converts the table, reads it back and checks that cell text and column order survive. JSON to CSV uses the same review. |
| “Could you whip up a little offline browser app that counts clicks and resets?” | Select an installed local model. | Java sets the click and reset checks before the model writes the app. Open the result in Mission. |

For spoken input, focus the Chat field and use system dictation. On Windows 11, Voice Access can work offline after setup. On macOS, use the Dictation shortcut configured under Keyboard. Review the transcript before starting the task. [Windows Voice Access](https://support.microsoft.com/en-us/accessibility/windows/voice-access/set-up-voice-access) · [macOS Dictation](https://support.apple.com/guide/mac-help/mh40584/mac)

## What the tests measured

- The [Delfys validation recording](docs/datasets/delfys75-013/validation.csv) has {record["samples"]} samples over {record["timeS"]:.2f} s. The desktop independently calculated {record["travelM"]:.3f} m of total travel and {record["meanSpeedMps"]:.3f} m/s average speed from the source rows. The report retains quality flags.
- The two simulated ramp setups travelled {ramp["first"]:.3f} m and {ramp["second"]:.3f} m. Both replays and the nine browser checks passed.
- CSV conversion preserved a comma inside a quoted field, the text BACK001.20BACK and an empty cell. Missing inputs and a mixed request refused action.
- {docs_gate} The Windows test also used BACK{model}BACK to generate the click counter. An independent browser session checked startup, two clicks, reset and a click after reset across nine steps.
- {portable} [Hosted test run]({matrix["url"]})

[Open the ramp comparison screen](docs/readmes/210/ramp-comparison.png) · [Open the file review](docs/readmes/210/file-conversion-review.png) · [Open the model approval screen](docs/readmes/210/local-model-review.png) · [Open the finished counter](docs/readmes/210/created-counter.png)

## How a request becomes a checked result

BACKBACKBACKmermaid
flowchart LR
    Chat["Everyday request"] --> Java["Java matches a supported procedure"]
    Java --> Input["Bind the selected source"]
    Java --> Model["Local model proposes a motion report"]
    Model --> Review["Confirm in Mission"]
    Review --> Input
    Input --> Run["Execute the registered procedure"]
    Run --> Check["Independent source and outcome checks"]
    Check --> Result["Chat answer and saved evidence"]
BACKBACKBACK

Use one explicit outcome and name the source when a selection is required. The simulated ramp values describe those tested configurations. The motion report describes the recorded samples. The creation example supports a click counter with fixed increment and reset checks. Model-assisted motion reports pause in Mission before execution.

## Next capability

Resolve follow-ups like “Make a chart of that” to one verified source and carry that source through a durable plan. Test new, independently written requests across data, browser, world and creation, recording both successful choices and safe clarifications.

[Reproduction and acceptance plan](docs/publication210/plan.md) · [Saved release evidence](docs/readmes/210/release-proof.json) · [Obsidian map](docs/obsidian/218-plain-language.canvas)
""".replace("BACK", chr(96)))
    home = read("README.md")
    head = home.split("[Edition ", 1)[0]
    write("README.md", head + "[Edition 210 — Everyday language](README-210.md) · [Previous edition](README-209.md)"
          " · [Offline walkthrough](docs/readmes/210/index.html) · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + "\n")
    render("README-210.md", "docs/readmes/INDEX.md", "[Edition 209",
           "[Edition 210 — Everyday language](../../README-210.md) · [Walkthrough](210/index.html)\n\n[Edition 209")
    entry = (f"## Edition 210 — {TITLE}\n\n{DATE}. {SUMMARY} {docs_gate} {portable} "
             "Keep recorded measurements and simulations distinct. Freeze counter behavior before generation. "
             "See README-210.md and docs/publication210/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, entry)
    note = f"docs/obsidian/{NOTE}.md"
    canvas = f"docs/obsidian/{NOTE}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 210 — Everyday language]({NOTE}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{docs_gate}\n\n[Edition 210](../../README-210.md) · "
          f"[Workflow map]({NOTE}.canvas) · [Reproduction](../publication210/plan.md)\n")
    modules = [{"title": title, "source": path, "note": note} for title, path in (
        ("Java plain-language routes", "workspace-engine/src/main/java/fraymus/workspace/PlainWorkflowLanguage.java"),
        ("Durable approval and task execution", "workspace-engine/src/main/java/fraymus/workspace/Agency043.java"),
        ("Source binding and checked outcomes", "workspace-engine/src/main/java/fraymus/workspace/Domain044.java"),
        ("Reviewed file conversions", "workspace-engine/src/main/java/fraymus/workspace/RequestWorkbench.java"),
        ("Desktop verification", "workspace-desktop/test/plain-language-integration.cjs"),
    )]
    nodes = [row["source"] for row in modules] + ["README-210.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": file, "x": (i % 3) * 380, "y": (i // 3) * 290,
                   "width": 350, "height": 250} for i, file in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "toNode": str(i)} for i in range(1, len(nodes))],
    })
    gallery = [{"path": (ROOT / name).as_posix(), "title": caption} for name, caption in SHOTS.items()]
    system = {"summary": SUMMARY, "modules": modules, "screenshots": gallery, "evidence": [
        {"path": (ROOT / "release-proof.json").as_posix(), "title": "Desktop test measurements"},
        {"path": "docs/publication210/plan.md", "title": "Reproduce the workflow"},
    ]}
    write_json(ROOT / "system-map.json", system)
    source = matrix["headSha"]
    files = subprocess.check_output(["git", "diff", "--name-only", "readme-209", source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas, "docs/publication210/plan.md"})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/209/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 210 · {escape(TITLE)}</title>", guide, count=1)
    navigation = f"<h1>{TITLE}</h1><p>{SUMMARY}</p>"
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", navigation, guide, count=1)
    guide = guide.replace("../../../README-209.md", "../../../README-210.md").replace(
        '../208/index.html">Edition 208', '../209/index.html">Edition 209').replace(
        '<a href="selected-briefing.html">Open the selected briefing</a>',
        '<a href="../../publication210/plan.md">Try the requests</a>')
    images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                       f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in gallery)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: images + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)
    manifest = json.loads(subprocess.check_output(["git", "show", "readme-209:docs/readmes/editions.json"]))
    assert manifest["latest"] == 209
    assets = [item(path) for path in sorted(ROOT.iterdir()) if path.is_file()]
    frozen = [{"path": row["path"], "sha256": row["sha256"], "kind": "binary" if row["hashKind"] == "binary" else "text"}
              for row in assets]
    manifest["latest"] = 210
    manifest["editions"].append({"number": 210, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-210.md", "markdownSha256": item("README-210.md")["sha256"],
                                 "companion": (ROOT / "index.html").as_posix(), "tag": "readme-210",
                                 "sourceBaseline": source, "snapshotDirectory": ROOT.as_posix(), "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(f"Edition 210 published locally with {len(gallery)} screenshots and {len(inventory)} inventory entries.")


if __name__ == "__main__":
    publish(*(Path(argument) for argument in sys.argv[1:]))

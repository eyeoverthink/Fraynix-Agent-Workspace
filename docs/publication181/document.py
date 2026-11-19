"""Build the linked Edition 181 release documentation from the verified local run."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import shutil
import subprocess

root = Path.cwd()
snapshot = root / "docs/readmes/181"
snapshot.mkdir(exist_ok=True)
live = root / "build/edition181-publish"
baseline_sha = "fb5fdaadd21fcfaffb9519c19503e44a64b3cb96"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
app = json.loads((live / "result.json").read_bytes())
tests = json.loads((root / "build/edition181-regression/summary.json").read_bytes())
assert app["passed"] and app["reopened"] and app["remembered"] and tests["passed"]
assert app["originalBrowserSteps"] == 23 and len(app["proof"]["browserCases"]) == 12
title = "A verified Fray browser tool from chat"
summary = ("Ask for an inches-to-millimetres browser tool in Chat. Fraynix offers a review, then checks "
           "the offline HTML tool in Chromium against the Java reference from the existing Fray compiler.")
counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
          f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed.")


def write(path, data):
    (root / path).write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def prepend(path, text):
    p = root / path
    old = p.read_text(encoding="utf-8-sig")
    if not old.startswith(text):
        p.write_text(text + old, encoding="utf-8")


def entry(path):
    data = (root / path).read_bytes()
    kind = "binary"
    try:
        data = data.decode("utf-8-sig").replace("\r\n", "\n").encode()
        kind = "utf8-lf"
    except UnicodeError:
        pass
    return {"path": path, "bytes": len(data), "sha256": sha256(data).hexdigest(), "hashKind": kind}


labels = {"chat-review.png": "The real Chat request awaiting approval",
          "browser-converter.png": "The verified converter showing 12 inches as 304.8 mm"}
shots = []
for row in app["captures"]:
    original = live / row["name"]
    assert sha256(original.read_bytes()).hexdigest() == row["sha256"]
    shutil.copyfile(original, snapshot / row["name"])
    shots.append({"path": f"docs/readmes/181/{row['name']}", "title": labels[row["name"]]})
assert set(labels) == {Path(row["path"]).name for row in shots}

receipt = json.loads((live / "fray-browser-receipt.json").read_bytes())
assert receipt["passed"] and len(receipt["checks"]) == 23
write(snapshot / "browser-receipt.json", receipt)
app["source"] = source
app["tests"] = tests
write(snapshot / "verification.json", app)
fence = chr(96)*3
cases = "\n".join(f"| {row['input'] if row['input'].strip() else 'Blank'} | {row['java']} | {row['browser']} |"
                  for row in app["proof"]["browserCases"])
(root / "README-181.md").write_text(f"""# Fraynix Agent Workspace · Edition 181

[Edition 180](README-180.md) · [Offline walkthrough](docs/readmes/181/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The checked converter in the real workspace](docs/readmes/181/browser-converter.png)

## Try it

1. In **Chat**, enter “Make me a little converter for inches to millimetres and give me a browser version.” Select **Start task**.
2. Review the proposed tool. Select **Build and verify in browser**. Java saves the result after checking the Chromium observations.
3. In **Creation Canvas**, enter a signed decimal number of inches. The result updates as you type. For example, 12 inches displays 304.8 mm.
4. In **Request workbench**, reopen the last review. **Export output** saves a single HTML file that runs from disk. **Remember this wording** keeps the confirmed phrase for future reviews.

[Chat approval screenshot](docs/readmes/181/chat-review.png) · [Original Chromium receipt](docs/readmes/181/browser-receipt.json)

## How verification works

{fence}mermaid
flowchart LR
    Chat[Chat] --> Review[Java Workbench review]
    Review --> Fray[Fray compiled reference]
    Review --> Chromium[Chromium tests the HTML input]
    Fray --> Check[Java compares actual results]
    Chromium --> Check
    Check --> Artifact[Saved HTML and evidence]
{fence}

Java reads the constant from the original Fray compiler's bytecode for **inches × 25.4**. The browser uses integer decimal arithmetic. Chromium exercised the actual input and visible result. Java compared the observations with the reference and bound the proof to the saved HTML. The workbench rereads that proof when opening or exporting the result.

The tool accepts magnitudes up to 1,000,000 inches with up to four decimal places. The receipt includes the approved Fray source, its compiler revision, bytecode, HTML hash and the browser results. This edition supports one registered Fray program. The existing Workbench remains available for other file and creation requests.

## Results on November 19, 2026

The real desktop run opened the review from Chat with model discovery disabled. It passed **23 Chromium checks across 12 scenarios**. The installed app reopened the same artifact after a fresh Java process started. The exported HTML reported **9.525 mm for 0.375 inches** when opened directly from disk. A remembered phrase still opened a new review. Java regression tests also rejected an incorrect browser reading, a stale approval and a changed saved artifact.

| Input in inches | Java expected | Chromium observed |
| --- | --- | --- |
{cases}

{counts} The initial integration attempts exposed a filename restriction in artifact storage. The revised Fray source filename passes through the same artifact checks.

[Full verification record](docs/readmes/181/verification.json) · [Implementation plan and next acceptance checks](docs/publication181/plan.md)
""", encoding="utf-8")

readme = root / "README.md"
old = readme.read_text(encoding="utf-8-sig")
previous = ("[Edition 180 — teachable requests and verified conversion](README-180.md) · [Previous edition](README-179.md)"
            " · [Offline walkthrough](docs/readmes/180/index.html) · [Full history](docs/readmes/INDEX.md)")
link = ("[Edition 181 — a verified Fray browser tool from chat](README-181.md) · [Previous edition](README-180.md)"
        " · [Offline walkthrough](docs/readmes/181/index.html) · [Full history](docs/readmes/INDEX.md)")
if previous in old:
    readme.write_text(old.replace(previous, link, 1).replace(
        "Fraynix can turn everyday requests into reviewed local work, learn a confirmed phrase, convert tables in both directions, inspect original file bytes and pass creation briefs to the Canvas.",
        summary, 1), encoding="utf-8")
index = root / "docs/readmes/INDEX.md"
old = index.read_text(encoding="utf-8-sig")
if "README-181.md" not in old:
    marker = "[Edition 180"
    at = old.index(marker)
    index.write_text(old[:at] + "[Edition 181 — a verified Fray browser tool from chat](../../README-181.md)"
                     " · [Walkthrough](181/index.html)\n\n" + old[at:], encoding="utf-8")

record = (f"## Edition 181 — {title}\n\n2026-11-19. {summary} {counts} "
          "Chromium passed 23 checks across 12 scenarios. The original artifact reopened after restart, "
          "the exported HTML ran from disk and the recalled wording still required review. The source attachment "
          "uses the existing single-extension artifact rule. Next, qualify a second Fray program with a different "
          "instruction sequence. See README-181.md and docs/publication181/plan.md.\n\n")
for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name, record)

note = "docs/obsidian/189-fray-browser-tool.md"
canvas = "docs/obsidian/189-fray-browser-tool.canvas"
prepend("docs/obsidian/00-home.md", "[Edition 181 — a verified Fray browser tool from chat](189-fray-browser-tool.md)\n\n")
(root / note).write_text(f"# {title}\n\n{summary}\n\n"
    "[Edition 181](../../README-181.md) · [Workflow map](189-fray-browser-tool.canvas) · "
    "[Implementation plan](../publication181/plan.md)\n", encoding="utf-8")

java = "workspace-engine/src/main/java/fraymus/workspace/"
modules = [{"title": label, "source": path, "note": note} for label, path in [
    ("Reviewed language routing", java + "RequestLanguage.java"),
    ("Fray reference and independent checks", java + "FrayConversion.java"),
    ("Durable Workbench approval and proof", java + "RequestWorkbench.java"),
    ("Offline browser converter", "workspace-engine/src/main/resources/workbench/inch-converter.html")]]
nodes = [row["source"] for row in modules] + ["README-181.md"]
write(canvas, {"nodes": [{"id": str(i), "type": "file", "file": path, "x": 370*i, "y": 0,
                          "width": 345, "height": 240} for i, path in enumerate(nodes)],
               "edges": [{"id": f"step-{i}", "fromNode": str(i-1), "fromSide": "right",
                          "toNode": str(i), "toSide": "left"} for i in range(1,len(nodes))]})
system = {"summary": summary, "modules": modules, "screenshots": shots, "evidence": [
    {"path": "docs/readmes/181/verification.json", "title": "Verification record"},
    {"path": "docs/readmes/181/browser-receipt.json", "title": "Original Chromium receipt"}]}
write(snapshot / "system-map.json", system)
paths = set(subprocess.check_output(["git", "diff", "--name-only", baseline_sha, source], text=True).splitlines())
paths.update([note, canvas])
inventory = [entry(name) for name in sorted(paths)]
write(snapshot / "file-inventory.json", inventory)
images = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                   f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in shots)
datasets = "\n".join(f'<script id="{key}-data" type="application/json">'
    + json.dumps(data, ensure_ascii=True).replace("</","<\\/") + "</script>"
    for key, data in [("system", system), ("inventory", inventory)])
(snapshot / "index.html").write_text(f"""<!doctype html>
<html lang="en"><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Fraynix 181 · Verified Fray browser tool</title>
<style>
body {{background:#101923;color:#e7f0f5;font:16px/1.55 system-ui;margin:0}}
main {{max-width:68rem;margin:auto;padding:clamp(16px,5vw,45px)}}
a {{color:#9be4d3}} h1 {{line-height:1.12;font-size:clamp(2rem,5vw,3rem)}}
nav {{display:flex;gap:1rem;flex-wrap:wrap}} figure {{margin:1.35rem 0}}
img {{max-width:100%;height:auto;border:1px solid #3b5064;border-radius:8px}}
input,select {{font:inherit;max-width:100%;margin:0.3rem 0;padding:0.4rem}}
li {{overflow-wrap:anywhere}}
</style>
<main><h1>{escape(title)}</h1><p>{escape(summary)}</p>
<nav><a href="../../../README-181.md">App guide</a><a href="verification.json">Verification</a>
<a href="../180/index.html">Edition 180</a><a href="#system">System files</a></nav>
{images}
<h2 id="system">System files</h2>
<label>Module <select id="modules"><option value="">Choose a module</option></select></label>
<p><a id="module-link" hidden>Open source</a></p>
<label>Filter file inventory <input id="filter" type="search"></label><ul id="files"></ul>
</main>
{datasets}
<script>
const system = JSON.parse(document.querySelector('#system-data').textContent);
const inventory = JSON.parse(document.querySelector('#inventory-data').textContent);
const select = document.querySelector('#modules');
const link = document.querySelector('#module-link');
const filter = document.querySelector('#filter');
for (const item of system.modules) select.add(new Option(item.title,item.source));
select.addEventListener('change',() => {{
  link.hidden=!select.value;
  if(select.value) link.href='../../../'+select.value;
}});
function render() {{
  const match=filter.value.toLowerCase();
  const rows=inventory.filter(row=>row.path.toLowerCase().includes(match)).map(row=>{{
    const item=document.createElement('li');
    const file=document.createElement('a');
    file.href='../../../'+row.path;
    file.textContent=row.path;
    item.append(file);
    return item;
  }});
  document.querySelector('#files').replaceChildren(...rows);
}}
filter.addEventListener('input',render);
render();
</script></html>
""", encoding="utf-8")

prior = json.loads(subprocess.check_output(["git","show",f"{baseline_sha}:docs/readmes/editions.json"]))
assert prior["latest"] == 180
assets = []
for path in sorted(snapshot.iterdir()):
    row = entry(path.relative_to(root).as_posix())
    assets.append({"path": row["path"], "kind": "binary" if path.suffix == ".png" else "text",
                   "sha256": row["sha256"]})
prior["latest"] = 181
prior["editions"].append({"number":181, "date":"2026-11-19", "title":title, "status":"validated-local",
    "markdown":"README-181.md", "markdownSha256":entry("README-181.md")["sha256"],
    "companion":"docs/readmes/181/index.html", "tag":"readme-181", "sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/181", "snapshotFiles":assets})
write("docs/readmes/editions.json", prior)
print(json.dumps({"verified":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

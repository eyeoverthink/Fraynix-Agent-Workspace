"""Publish Edition 182 documentation from the actual browser and regression results."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess

root = Path.cwd()
snapshot = root / "docs/readmes/182"
snapshot.mkdir(exist_ok=True)
live = root / "build/edition182-publish"
baseline = "68a88f61b854a05dcccf97f871909f390d456f52"
source = subprocess.check_output(["git","rev-parse","HEAD"], text=True).strip()
app = json.loads((live/"result.json").read_bytes())
tests = json.loads((root/"build/edition182-regression/summary.json").read_bytes())
assert app["passed"] and app["reopened"] and app["remembered"] and app["prior"]["matched"]
assert tests["passed"] and app["originalBrowserSteps"] == 23
assert len(app["proof"]["browserCases"]) == 12 and len(app["holdouts"]) == 8
assert all(row["expected"] == row["observed"] for row in app["holdouts"])
title = "A verified Fray temperature converter"
summary = ("Ask for a Celsius-to-Fahrenheit browser tool in Chat. The Workbench checks the offline tool "
           "in Chromium against the original Fray program running in Java.")
counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
          f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed.")

def read(path):
    return (root/path).read_text(encoding="utf-8-sig")

def write(path, content):
    (root/path).write_text(content, encoding="utf-8", newline="\n")

def json_file(path, data):
    write(path, json.dumps(data, ensure_ascii=False, indent=2)+"\n")

def prepend(path, text):
    old = read(path)
    if not old.startswith(text):
        write(path, text+old)

def item(path):
    data = (root/path).read_bytes()
    try:
        data = data.decode("utf-8-sig").replace("\r\n","\n").encode()
        kind = "utf8-lf"
    except UnicodeDecodeError:
        kind = "binary"
    return {"path":path,"bytes":len(data),"sha256":sha256(data).hexdigest(),"hashKind":kind}

labels = {"chat-review.png":"The Celsius-to-Fahrenheit Chat request awaiting approval",
          "browser-converter.png":"The verified converter displays 37.25 Celsius as 99.05 Fahrenheit"}
shots = []
for shot in app["captures"]:
    name = shot["name"]
    original = live/name
    assert sha256(original.read_bytes()).hexdigest() == shot["sha256"]
    shutil.copyfile(original, snapshot/name)
    shots.append({"path":f"docs/readmes/182/{name}","title":labels[name]})
assert set(labels) == {Path(shot["path"]).name for shot in shots}
receipt = json.loads((live/"fray-browser-receipt.json").read_bytes())
assert receipt["passed"] and len(receipt["checks"]) == 23
json_file(snapshot/"browser-receipt.json", receipt)
app.update({"source":source, "tests":tests})
json_file(snapshot/"verification.json", app)
rows = "\n".join(f"| {row['input'] if row['input'].strip() else 'Blank'} | {row['java']} | {row['browser']} |"
                 for row in app["proof"]["browserCases"])
extra = "\n".join(f"| {row['input']} | {row['observed']} |" for row in app["holdouts"])
fence = chr(96)*3
write("README-182.md",f"""# Fraynix Agent Workspace · Edition 182

[Edition 181](README-181.md) · [Offline walkthrough](docs/readmes/182/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The checked temperature converter in the real workspace](docs/readmes/182/browser-converter.png)

## Try it

1. In **Chat**, enter “Build me a browser tool to convert Celsius to Fahrenheit.” Select **Start task**.
2. Review the action in **Request workbench**, then select **Build and verify in browser**.
3. In **Creation Canvas**, enter a Celsius value. The answer updates as you type. Try **37.25 °C**, which displays **99.05 °F**.
4. Reopen the last review to export a single HTML file or remember the wording for a later review.

[Real approval screenshot](docs/readmes/182/chat-review.png) · [Original Chromium receipt](docs/readmes/182/browser-receipt.json)

## From Chat to evidence

{fence}mermaid
flowchart LR
    Chat[Chat] --> Review[Workbench review]
    Review --> Program[Fray program in Java]
    Review --> Browser[Live Chromium input]
    Program --> Compare[Compare expected and actual]
    Browser --> Compare
    Compare --> Save[Save HTML and receipt]
{fence}

The original FraynixBoot compiler produces the load, multiply, divide, add, and return instructions for **celsius × 9 ÷ 5 + 32**. The same bounded Java reader runs the two registered converter programs. The browser independently calculates Fahrenheit with scaled integers and shows up to five decimal places. The Java gate checks the selected HTML hash and the observed inputs and outputs before it saves the artifact.

The temperature tool accepts signed decimals up to 1,000,000 in magnitude with up to four decimal places. For example, you can enter .05 or -40. The single-file export runs locally with all its controls and styles embedded.

## Results on November 19, 2026

With model discovery disabled, the desktop passed **23 Chromium checks across 12 scenarios**. The original inches-to-millimetres receipt reopened and matched its saved Edition 181 proof before and after restarting Java. The temperature tool also reopened with the original proof. Its exported file converted **12.125 °C to 53.825 °F** directly from disk. Java rejected a preview hash and a browser trace from the other converter when offered for a temperature review.

| Input in °C | Java expected | Chromium observed |
| --- | --- | --- |
{rows}

Eight additional browser cases used fixed expected results outside the saved verification suite.

| Input in °C | Observed result |
| --- | --- |
{extra}

{counts}

[Full verification record](docs/readmes/182/verification.json) · [Implementation plan and next acceptance checks](docs/publication182/plan.md)
""")

old = read("README.md")
previous = ("[Edition 181 — a verified Fray browser tool from chat](README-181.md) · [Previous edition](README-180.md)"
            " · [Offline walkthrough](docs/readmes/181/index.html) · [Full history](docs/readmes/INDEX.md)")
link = ("[Edition 182 — a verified Fray temperature converter](README-182.md) · [Previous edition](README-181.md)"
        " · [Offline walkthrough](docs/readmes/182/index.html) · [Full history](docs/readmes/INDEX.md)")
if link not in old:
    assert previous in old
    intro = old.index("\n\n", old.index(previous))+2
    write("README.md",old[:intro].replace(previous,link)+summary+old[old.index("\n",intro):])
index = read("docs/readmes/INDEX.md")
if "README-182.md" not in index:
    at = index.index("[Edition 181")
    write("docs/readmes/INDEX.md",index[:at]+"[Edition 182 — a verified Fray temperature converter](../../README-182.md)"
          " · [Walkthrough](182/index.html)\n\n"+index[at:])
record = (f"## Edition 182 — {title}\n\n2026-11-19. {summary} {counts} "
          "Chromium passed 23 checks across 12 cases and eight additional inputs. Java refused a mismatched "
          "recipe hash or browser trace. The original Edition 181 proof matched before and after restart. "
          "The exported HTML ran directly from disk. See README-182.md and docs/publication182/plan.md. "
          "Next, qualify composition with checked intermediate results.\n\n")
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)

note = "docs/obsidian/190-fray-temperature-tool.md"
canvas = "docs/obsidian/190-fray-temperature-tool.canvas"
prepend("docs/obsidian/00-home.md","[Edition 182 — Fray temperature converter](190-fray-temperature-tool.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 182](../../README-182.md) · "
      "[Workflow map](190-fray-temperature-tool.canvas) · [Implementation plan](../publication182/plan.md)\n")
java = "workspace-engine/src/main/java/fraymus/workspace/"
modules = [{"title":label,"source":path,"note":note} for label,path in [
    ("Reviewed language routing",java+"RequestLanguage.java"),
    ("Durable approval and evidence",java+"RequestWorkbench.java"),
    ("Fray bytecode arithmetic",java+"FrayFormula.java"),
    ("Recipe and browser checks",java+"FrayConversion.java"),
    ("Offline temperature calculator","workspace-engine/src/main/resources/workbench/temperature-converter.html")]]
nodes = [m["source"] for m in modules]+["README-182.md"]
json_file(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                  for i,p in enumerate(nodes)],
                  "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                  for i in range(1,len(nodes))]})
system = {"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/182/verification.json","title":"Verification record"},
    {"path":"docs/readmes/182/browser-receipt.json","title":"Original Chromium receipt"}]}
json_file(snapshot/"system-map.json",system)
paths = set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory = [item(p) for p in sorted(paths)]
json_file(snapshot/"file-inventory.json",inventory)

guide = read("docs/readmes/181/index.html")
guide = re.sub(r"<title>.*?</title>",f"<title>Fraynix 182 · {escape(title)}</title>",guide,count=1)
guide = re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide = guide.replace("../../../README-181.md","../../../README-182.md").replace(
    '../180/index.html">Edition 180','../181/index.html">Edition 181')
images = "\n".join(f'<figure><img src="{Path(r["path"]).name}" alt="{escape(r["title"])}">'
                   f'<figcaption>{escape(r["title"])}</figcaption></figure>' for r in shots)
guide = re.sub(r"<figure>.*?(?=<h2)",lambda m:images+"\n",guide,count=1,flags=re.S)
for key,data in [("system",system),("inventory",inventory)]:
    pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
    payload = json.dumps(data,ensure_ascii=True).replace("</","<\\/")
    guide = re.sub(pattern,lambda m:f'<script id="{key}-data" type="application/json">{payload}</script>',guide,
                   count=1,flags=re.S)
write(snapshot/"index.html",guide)

prior = json.loads(subprocess.check_output(["git","show",f"{baseline}:docs/readmes/editions.json"]))
assert prior["latest"] == 181
assets = []
for asset in sorted(snapshot.iterdir()):
    row = item(asset.relative_to(root).as_posix())
    assets.append({"path":row["path"],"kind":"binary" if asset.suffix==".png" else "text","sha256":row["sha256"]})
prior["latest"] = 182
prior["editions"].append({"number":182,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-182.md","markdownSha256":item("README-182.md")["sha256"],
    "companion":"docs/readmes/182/index.html","tag":"readme-182","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/182","snapshotFiles":assets})
json_file("docs/readmes/editions.json",prior)
print(json.dumps({"verified":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

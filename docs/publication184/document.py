"""Freeze Edition 184 from the verified desktop and regression runs."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess

root = Path.cwd()
snap = Path("docs/readmes/184")
snap.mkdir(exist_ok=True)
live = Path("build/edition184-publish")
baseline = "7a66e7ddc619b4f8e267db06b2fb2a18c8c28e94"
source = subprocess.check_output(["git","rev-parse","HEAD"],text=True).strip()
app = json.loads((live/"result.json").read_bytes())
tests = json.loads(Path("build/edition184-regression/summary.json").read_bytes())
assert app["passed"] and app["reopened"] and app["remembered"] and app["staleInputRefused"]
assert tests["passed"] and app["priorEditions"] == [181,182,183]
assert app["restart"]["forced"] and app["restart"]["firstReceiptUnchanged"]
assert app["restart"]["secondPendingAtRestart"] and len(app["aliases"]) == 2
assert len(app["proof"]["firstChecks"]) == len(app["proof"]["secondChecks"]) == 5
assert len(app["refusals"]) == 2 and len(app["holdouts"]) == 6
title = "Resume a verified Fray handoff after desktop restart"
summary = ("Convert Celsius to Fahrenheit and then Rankine in one Chat request. The Workbench can save "
           "the verified intermediate and resume it after the desktop stops.")
counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
          f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed.")

def read(path):
    return Path(path).read_text(encoding="utf-8-sig")

def write(path,text):
    Path(path).write_text(text,encoding="utf-8",newline="\n")

def js(path,data):
    write(path,json.dumps(data,ensure_ascii=False,indent=2)+"\n")

def prepend(path,text):
    old=read(path)
    if not old.startswith(text):
        write(path,text+old)

def item(path):
    data=Path(path).read_bytes()
    try:
        data=data.decode("utf-8-sig").replace("\r\n","\n").encode()
        kind="utf8-lf"
    except UnicodeDecodeError:
        kind="binary"
    return {"path":Path(path).as_posix(),"bytes":len(data),"sha256":sha256(data).hexdigest(),"hashKind":kind}

labels={"saved-handoff.png":"The saved Fahrenheit handoff recovered after restarting the desktop",
        "verified-temperature-chain.png":"The completed temperature chain and its exact intermediate",
        "alias-refusals.png":"Chat refuses millimetres and kelvins as substitutes for Fahrenheit"}
shots=[]
for shot in app["captures"]:
    name=shot["name"]
    original=live/name
    assert sha256(original.read_bytes()).hexdigest()==shot["sha256"]
    shutil.copyfile(original,snap/name)
    shots.append({"path":(snap/name).as_posix(),"title":labels[name]})
assert set(labels)=={Path(shot["path"]).name for shot in shots}
receipt=json.loads((live/"browser-receipt.json").read_bytes())
assert receipt["passed"] and len(receipt["checks"]) == 10
js(snap/"browser-receipt.json",receipt)
app.update({"source":source,"tests":tests})
js(snap/"verification.json",app)

fence=chr(96)*3
rows="\n".join(f"| {row['input']} | {row['fahrenheit']} | {row['rankine']} |" for row in app["holdouts"])
write("README-184.md",f"""# Fraynix Agent Workspace · Edition 184

[Edition 183](README-183.md) · [Offline walkthrough](docs/readmes/184/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The original Fahrenheit result recovered after a forced desktop restart](docs/readmes/184/saved-handoff.png)

## Try it

1. In **Chat**, enter “Convert -17.7778℃ to ℉, then °F to degrees Rankine.” Select **Start task**.
2. Review the signed value and three units. Select **Verify both conversions**. Enable **Pause after the first verified conversion** to stop at the handoff.
3. After restarting the app, open **Request workbench** and select **Resume interrupted chain**. Confirm **Resume verified handoff**.
4. Open the verified converter in **Creation Canvas** or export it as a single offline HTML file.

[Completed chain](docs/readmes/184/verified-temperature-chain.png) · [Refusals seen in Chat](docs/readmes/184/alias-refusals.png)

## What gets checked

{fence}mermaid
flowchart LR
    Chat[Chat quantity and unit aliases] --> Review[Typed unit review]
    Review --> First[Java verifies Fahrenheit]
    First --> Saved[Persist original browser receipt]
    Saved --> Restart[Reopen after desktop restart]
    Restart --> Match[Match rebuilt browser state to original receipt]
    Match --> Finish[Verify Rankine and save]
{fence}

The temperature chain accepts Celsius, centigrade, degree names, abbreviations and symbols such as ℃ and ℉. The Java planner checks that the receiving step expects the same unit. It refuses a Fahrenheit result offered to a millimetre or kelvin input before it creates a review. Both independent browser calculations use scaled integers and Java checks them against the original Fray programs.

The exact intermediate, including its sign and unit, is stored before the second step runs. On recovery the browser rebuilds its first result and Java compares it with the original saved observations before releasing the second step. The tool accepts temperatures from −273.15 to 1,000,000 °C with up to four decimal places.

## Verification on November 19, 2026

The test forcibly stopped the desktop and its Java process tree with **−0.00004 °F** verified and the second stage pending. A new desktop process reopened the same project. Its recovery control found the saved handoff from disk even after the browser’s last-review pointer was cleared. Java retained the original receipt and verified the final **459.66996 °R**. The file export produced **558.72 °R** for **37.25 °C** directly from disk.

Both stages passed five Chromium checks each. Two incompatible units were refused, two alternate prompt spellings produced the same typed plan, and all three earlier edition receipts reopened. The browser also cleared stale intermediates on input changes and refused a value below absolute zero.

| New input in °C | Observed Fahrenheit | Observed Rankine |
| --- | --- | --- |
{rows}

{counts}

[Original browser receipt](docs/readmes/184/browser-receipt.json) · [Full verification record](docs/readmes/184/verification.json) · [Implementation plan](docs/publication184/plan.md)
""")
entry=read("README.md")
previous=("[Edition 183 — a verified two-step Fray conversion](README-183.md) · [Previous edition](README-182.md)"
          " · [Offline walkthrough](docs/readmes/183/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 184 — resume a verified Fray handoff](README-184.md) · [Previous edition](README-183.md)"
        " · [Offline walkthrough](docs/readmes/184/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-184.md" not in index:
    start=index.index("[Edition 183")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 184 — resume a verified Fray handoff](../../README-184.md)"
          " · [Walkthrough](184/index.html)\n\n"+index[start:])
record=(f"## Edition 184 — {title}\n\n2026-11-19. {summary} {counts} "
        "The forced desktop restart preserved the original Fahrenheit receipt. The second step stayed pending "
        "until recovery. Two wrong units were refused, six additional inputs passed, and the original "
        "Edition 181–183 receipts reopened. See README-184.md and docs/publication184/plan.md.\n\n")
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/192-fray-handoff-recovery.md"
canvas="docs/obsidian/192-fray-handoff-recovery.canvas"
prepend("docs/obsidian/00-home.md","[Edition 184 — Fray handoff recovery](192-fray-handoff-recovery.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 184](../../README-184.md) · "
      "[Workflow map](192-fray-handoff-recovery.canvas) · [Implementation plan](../publication184/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Fray chain and handoff",java+"FrayChain.java"),("Durable stage verification",java+"RequestWorkbench.java"),
    ("Fray arithmetic",java+"FrayFormula.java"),
    ("Offline chain","workspace-engine/src/main/resources/workbench/temperature-chain.html")]]
nodes=[m["source"] for m in modules]+["README-184.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/184/verification.json","title":"Verification record"},
    {"path":"docs/readmes/184/browser-receipt.json","title":"Original Chromium receipt"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/183/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 184 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-183.md","../../../README-184.md").replace(
    '../182/index.html">Edition 182','../183/index.html">Edition 183')
images="\n".join(f'<figure><img src="{Path(r["path"]).name}" alt="{escape(r["title"])}">'
                 f'<figcaption>{escape(r["title"])}</figcaption></figure>' for r in shots)
guide=re.sub(r"<figure>.*?(?=<h2)",lambda _:images+"\n",guide,count=1,flags=re.S)
for key,data in [("system",system),("inventory",inventory)]:
    payload=json.dumps(data,ensure_ascii=True).replace("</","<\\/")
    pattern=f'<script id="{key}-data" type="application/json">.*?</script>'
    guide=re.sub(pattern,lambda _:f'<script id="{key}-data" type="application/json">{payload}</script>',
                 guide,count=1,flags=re.S)
write(snap/"index.html",guide)

manifest=json.loads(subprocess.check_output(["git","show",f"{baseline}:docs/readmes/editions.json"]))
assert manifest["latest"] == 183
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix==".png" else "text","sha256":item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=184
manifest["editions"].append({"number":184,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-184.md","markdownSha256":item("README-184.md")["sha256"],
    "companion":"docs/readmes/184/index.html","tag":"readme-184","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/184","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

"""Freeze Edition 183 from the verified desktop and regression runs."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess

root = Path.cwd()
snap = Path("docs/readmes/183")
snap.mkdir(exist_ok=True)
live = Path("build/edition183-live")
baseline = "6cfdd74f738b9523ef3da89a0a2f8bd2f63a621a"
source = subprocess.check_output(["git","rev-parse","HEAD"],text=True).strip()
app = json.loads((live/"result.json").read_bytes())
tests = json.loads(Path("build/edition183-regression/summary.json").read_bytes())
assert app["passed"] and app["reopened"] and app["remembered"] and app["staleInputRefused"]
assert tests["passed"] and app["priorEditions"] == [181,182]
assert len(app["proof"]["firstChecks"]) == len(app["proof"]["secondChecks"]) == 5
assert len(app["refusals"]) == 2 and len(app["holdouts"]) == 6
title = "A verified two-step Fray conversion"
summary = ("One Chat request converts inches to millimetres and then centimetres. "
           "Java verifies the intermediate value and its unit before the browser starts the second step.")
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

labels={"chat-review.png":"Both Fray stages in the approval review",
        "verified-chain.png":"The chain shows 313.57824 mm and 31.357824 cm",
        "rejected-handoffs.png":"Chat refuses incompatible input units"}
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
rows="\n".join(f"| {row['input']} | {row['millimetres']} | {row['centimetres']} |" for row in app["holdouts"])
write("README-183.md",f"""# Fraynix Agent Workspace · Edition 183

[Edition 182](README-182.md) · [Offline walkthrough](docs/readmes/183/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The two checked results in the real workspace](docs/readmes/183/verified-chain.png)

## Try it

1. In **Chat**, enter “Convert 12.3456 inches to millimetres, then millimetres to centimetres.” Select **Start task**.
2. Review the starting measurement and both stages. Select **Verify both conversions**.
3. The verified tool opens in **Creation Canvas**. Select **Convert to millimetres**, then **Convert to centimetres** to try it again. The tool shows the exact intermediate alongside the answer.
4. Reopen the review to export the tool as one offline HTML file or remember the request wording.

[Approval screenshot](docs/readmes/183/chat-review.png) · [Refused handoffs screenshot](docs/readmes/183/rejected-handoffs.png)

## How the handoff works

{fence}mermaid
flowchart LR
    Chat[Chat request] --> Review[Review quantity and units]
    Review --> First[First Fray program and Chromium checks]
    First --> Gate[Java saves verified millimetres]
    Gate --> Second[Second Fray program and Chromium checks]
    Second --> Artifact[Save HTML and both receipts]
{fence}

The first Fray program multiplies inches by 25.4. The second divides millimetres by ten. Java validates the actual browser observations and passes the first verified decimal with its unit. Each stage has five Chromium checks. The browser calculates both results with scaled integers. Editing the input clears the results and locks the second button until the new millimetres are ready.

The accepted route is inches → millimetres → centimetres, with signed values up to 1,000,000 inches and up to four decimal places. The intermediate can carry five decimals and the final result six.

## Results on November 19, 2026

The approved input **12.3456 in** passed through **313.57824 mm** to **31.357824 cm**. Two real Chat requests tried to hand millimetres to a Celsius or inches input. Java rejected both before creating a review or artifact. Regression tests also rejected an early second step, a rounded intermediate and an altered unit in the evidence.

The first-stage receipt survived a Java store reopen in a recovery test. The completed chain and the exact Edition 181 and 182 receipts reopened after restarting the desktop app. The exported HTML converted **7.125 in to 18.0975 cm** directly from disk.

| New input in inches | Observed intermediate | Observed final result |
| --- | --- | --- |
{rows}

{counts}

[Original Chromium receipt](docs/readmes/183/browser-receipt.json) · [Full verification record](docs/readmes/183/verification.json) · [Implementation plan](docs/publication183/plan.md)
""")
entry=read("README.md")
previous=("[Edition 182 — a verified Fray temperature converter](README-182.md) · [Previous edition](README-181.md)"
          " · [Offline walkthrough](docs/readmes/182/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 183 — a verified two-step Fray conversion](README-183.md) · [Previous edition](README-182.md)"
        " · [Offline walkthrough](docs/readmes/183/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-183.md" not in index:
    start=index.index("[Edition 182")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 183 — a verified two-step Fray conversion](../../README-183.md)"
          " · [Walkthrough](183/index.html)\n\n"+index[start:])
record=(f"## Edition 183 — {title}\n\n2026-11-19. {summary} {counts} "
        "Chromium passed 10 checks. Two wrong unit handoffs were refused before review, and six extra inputs "
        "showed both results. The chain and both earlier converter proofs reopened after restart. "
        "The first-stage receipt survived a Java recovery test. See README-183.md and docs/publication183/plan.md.\n\n")
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/191-fray-length-chain.md"
canvas="docs/obsidian/191-fray-length-chain.canvas"
prepend("docs/obsidian/00-home.md","[Edition 183 — two-step Fray conversion](191-fray-length-chain.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 183](../../README-183.md) · "
      "[Workflow map](191-fray-length-chain.canvas) · [Implementation plan](../publication183/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Fray chain and handoff",java+"FrayChain.java"),("Durable stage verification",java+"RequestWorkbench.java"),
    ("Fray arithmetic",java+"FrayFormula.java"),
    ("Offline chain","workspace-engine/src/main/resources/workbench/length-chain.html")]]
nodes=[m["source"] for m in modules]+["README-183.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/183/verification.json","title":"Verification record"},
    {"path":"docs/readmes/183/browser-receipt.json","title":"Original Chromium receipt"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/182/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 183 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-182.md","../../../README-183.md").replace(
    '../181/index.html">Edition 181','../182/index.html">Edition 182')
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
assert manifest["latest"] == 182
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix==".png" else "text","sha256":item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=183
manifest["editions"].append({"number":183,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-183.md","markdownSha256":item("README-183.md")["sha256"],
    "companion":"docs/readmes/183/index.html","tag":"readme-183","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/183","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

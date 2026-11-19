"""Freeze Edition 186 from the verified desktop and regression runs."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess

root = Path.cwd()
snap = Path("docs/readmes/186")
snap.mkdir(exist_ok=True)
live = Path("build/edition186-publish")
baseline = "23fb5d756370f83da5bdff3fd892cbeb939fb8ab"
source = subprocess.check_output(["git","rev-parse","HEAD"],text=True).strip()
app = json.loads((live/"result.json").read_bytes())
tests = json.loads(Path("build/edition186-regression/summary.json").read_bytes())
assert app["passed"] and tests["passed"] and app["priorEditions"] == [181,182,183,184,185]
interruption=app["interruption"]
assert interruption["forced"] and interruption["retainedChecks"] == [5,5]
assert not interruption["registeredBeforeRestart"] and not interruption["previewReplayed"]
assert interruption["artifactsAdded"]==1 and interruption["pendingAfterSave"]==59 and interruption["siblingUnchanged"]
assert [row["pending"] for row in app["chooser"]]==[3,24,60]
assert all(len(row["samples"])==7 for row in app["chooser"])
title = "Recover an interrupted verified artifact save"
summary = ("Fraynix can finish saving a Fray artifact after the desktop stops during the write. "
           "Java restores the exact review and verifies both retained browser receipts before publishing the result.")
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

labels={"large-chooser.png":"The real Workbench lists 60 verified pending chains",
        "interrupted-save.png":"The same review is ready to finish its interrupted artifact save",
        "recovered-artifact.png":"The recovered artifact opens with both stages verified"}
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
rows=chr(10).join(f"| {r['pending']} | {len(r['samples'])} | {r['medianMs']} ms | {r['maxMs']} ms |"
                 for r in app["chooser"])
write("README-186.md",f"""# Fraynix Agent Workspace · Edition 186

[Edition 185](README-185.md) · [Offline walkthrough](docs/readmes/186/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The verified save recovered after restart](docs/readmes/186/interrupted-save.png)

## Recover a save

1. If Fraynix stops after verifying both conversions, restart the desktop and open **Request workbench**.
2. Select **Resume interrupted chain** and choose the review marked **ready to save**.
3. Select **Finish verified save**. Java rechecks the stored observations, the original HTML, and the expected artifact before saving. The result opens in **Creation Canvas**.

[Chooser with 60 pending reviews](docs/readmes/186/large-chooser.png) · [Recovered artifact screenshot](docs/readmes/186/recovered-artifact.png)

## What the interruption test did

{fence}mermaid
flowchart LR
    Checks[Both browser stages verified] --> Intent[Durable receipts and artifact hash]
    Intent --> Files[Version files written]
    Files --> Stop[Stop process before checkpoint registration]
    Stop --> Resume[Restart and finish the same save]
    Resume --> Verify[One artifact with its original evidence]
{fence}

The test held the world checkpoint writer lock, waited until all three version files existed and confirmed that the registered checkpoint still matched its original bytes. It forcibly stopped the isolated desktop and Java processes at that boundary. After restart the review still had both original five-check receipts and no published artifact. Recovery preserved the stranded files and finalized one version-one artifact under the same ID. It opened the final artifact directly, with no browser preview replay. The selected result was **4.60375 cm** and the neighboring review stayed byte-identical.

Separate Java coverage exercised the later boundary where the artifact and its browser receipt were already written. Recovery retained the receipt and artifact ID. A changed expected artifact hash was refused. The archived Edition 181–185 proofs also reopened.

## Chooser on November 19, 2026

| Verified pending chains | Runs | Median click-to-ready | Slowest sample |
| ---: | ---: | ---: | ---: |
{rows}

All pending rows came from real browser-verified first stages across both Fray recipes. The timing includes the UI click, Java listing and verification, transport and rendering. These measurements are from one local Windows run. The forced-stop test left 59 siblings pending after completing the selected save.

{counts} Validation ran locally.

[Original ten-step browser receipt](docs/readmes/186/browser-receipt.json) · [Full measurements and verification](docs/readmes/186/verification.json) · [Implementation plan](docs/publication186/plan.md)
""")
entry=read("README.md")
previous=("[Edition 185 — choose among interrupted Fray chains](README-185.md) · [Previous edition](README-184.md)"
          " · [Offline walkthrough](docs/readmes/185/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 186 — recover an interrupted artifact save](README-186.md) · [Previous edition](README-185.md)"
        " · [Offline walkthrough](docs/readmes/186/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-186.md" not in index:
    start=index.index("[Edition 185")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 186 — recover an interrupted artifact save](../../README-186.md)"
          " · [Walkthrough](186/index.html)"+chr(10)*2+index[start:])
record=(f"## Edition 186 — {title}"+chr(10)*2+f"2026-11-19. {summary} {counts} "
        "A forced stop during the checkpoint boundary left both browser receipts available. Restart recovered "
        "one artifact under the same ID without replaying the preview. The chooser was measured at 3, 24, and 60 "
        "real paused chains. See README-186.md and docs/publication186/plan.md."+chr(10)*2)
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/194-fray-artifact-save.md"
canvas="docs/obsidian/194-fray-artifact-save.canvas"
prepend("docs/obsidian/00-home.md","[Edition 186 — Recover an artifact save](194-fray-artifact-save.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 186](../../README-186.md) · "
      "[Workflow map](194-fray-artifact-save.canvas) · [Implementation plan](../publication186/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Artifact save recovery",java+"WorkspaceStore.java"),("Durable review checks",java+"RequestWorkbench.java"),
    ("Workbench recovery","workspace-desktop/renderer/request-workbench.js")]]
nodes=[m["source"] for m in modules]+["README-186.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/186/verification.json","title":"Verification record"},
    {"path":"docs/readmes/186/browser-receipt.json","title":"Original Chromium receipt"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/185/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 186 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-185.md","../../../README-186.md").replace(
    '../184/index.html">Edition 184','../185/index.html">Edition 185')
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
assert manifest["latest"] == 185
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix==".png" else "text","sha256":item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=186
manifest["editions"].append({"number":186,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-186.md","markdownSha256":item("README-186.md")["sha256"],
    "companion":"docs/readmes/186/index.html","tag":"readme-186","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/186","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

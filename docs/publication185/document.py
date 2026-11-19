"""Freeze Edition 185 from the verified desktop and regression runs."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess

root = Path.cwd()
snap = Path("docs/readmes/185")
snap.mkdir(exist_ok=True)
live = Path("build/edition185-live")
baseline = "72743074a2db74caa0e830e4caaca19f5580ff67"
source = subprocess.check_output(["git","rev-parse","HEAD"],text=True).strip()
app = json.loads((live/"result.json").read_bytes())
tests = json.loads(Path("build/edition185-regression/summary.json").read_bytes())
assert app["passed"] and app["restart"]["forced"] and app["siblingReceiptsUnchanged"]
assert tests["passed"] and app["priorEditions"] == [181,182,183,184]
assert app["unapprovedExcluded"] and len(app["pending"]) == len(app["completed"]) == 3
assert len(app["rejectedHandoffs"]) == 2 and app["completed"][0]["name"] == "length"
title = "Choose among interrupted Fray chains"
summary = ("The Workbench lists saved Fray handoffs by input and verified value. Choose which chain "
           "to resume when several are pending after a desktop restart.")
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

labels={"three-pending.png":"Three durable chains after the desktop restarts",
        "selected-length.png":"The selected length chain finishes first",
        "all-resolved.png":"The Workbench confirms that every saved handoff has been resolved"}
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
pending = {row["name"]: row for row in app["pending"]}
rows_list = []
for row in app["completed"]:
    item = pending[row["name"]]
    start, handoff = item["input"], item["intermediate"]
    rows_list.append(f"| {start['value']} {start['unit']} | {handoff['value']} {handoff['unit']} "
                     f"| {row['result']} | {row['rebuildMs']} ms | {row['secondStepMs']} ms |")
rows = chr(10).join(rows_list)
write("README-185.md",f"""# Fraynix Agent Workspace · Edition 185

[Edition 184](README-184.md) · [Offline walkthrough](docs/readmes/185/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The real Workbench showing three interrupted chains](docs/readmes/185/three-pending.png)

## Try it

1. In **Chat**, start an inches-to-centimetres or Celsius-to-Rankine chain. Enable **Pause after the first verified conversion** before confirming. Repeat with another request.
2. After starting the desktop again, open **Request workbench** and select **Resume interrupted chain**.
3. Select a row and **Open selected chain**. The chooser shows the starting value, the verified intermediate, and the review identifier.
4. Select **Resume verified handoff**. Java checks the exact review and original browser observations before releasing the next conversion. Repeat for the remaining rows.

[First selected result](docs/readmes/185/selected-length.png) · [All saved handoffs resolved](docs/readmes/185/all-resolved.png)

## How selection and verification work

{fence}mermaid
flowchart LR
    Disk[Durable first-stage receipts] --> List[Verified pending handoffs]
    List --> Choose[Pick an exact review]
    Choose --> Rebuild[Rebuild and compare browser state]
    Rebuild --> Finish[Verify the second conversion]
    Finish --> Save[Save that result]
{fence}

Java checks that the selected review is still pending and repeats the comparisons for its own saved quantity and unit. A completed review leaves the chooser. A draft with no verified intermediate stays available through normal review. If only one verified handoff is pending, the recovery button opens it directly.

## Local verification on November 19, 2026

The desktop and Java process tree stopped with three persisted handoffs. After restarting the same project, the test reopened the **middle** review first. The other two saved review files stayed byte-identical until explicitly selected. Each run retained its original first-stage receipt and passed ten browser checks. Java refused both a handoff from another unit and a different value from a review with the correct unit. All four historical Edition 181–184 proofs reopened.

| Starting input | Saved intermediate | Verified result | Rebuild | Second stage and save |
| --- | --- | --- | ---: | ---: |
{rows}

Rebuild includes loading and hashing the reviewed HTML, Chromium’s five first-stage checks, and Java’s comparison with the original receipt. Second-stage timing ends when Java saves the artifact. These are single local samples for the listed tasks.

{counts} GitHub Actions credits were unavailable. This release was validated locally.

[Browser receipt for the first recovered chain](docs/readmes/185/browser-receipt.json) · [Full measurements and verification](docs/readmes/185/verification.json) · [Implementation plan](docs/publication185/plan.md)
""")
entry=read("README.md")
previous=("[Edition 184 — resume a verified Fray handoff](README-184.md) · [Previous edition](README-183.md)"
          " · [Offline walkthrough](docs/readmes/184/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 185 — choose among interrupted Fray chains](README-185.md) · [Previous edition](README-184.md)"
        " · [Offline walkthrough](docs/readmes/185/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-185.md" not in index:
    start=index.index("[Edition 184")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 185 — choose among interrupted Fray chains](../../README-185.md)"
          " · [Walkthrough](185/index.html)"+chr(10)*2+index[start:])
record=(f"## Edition 185 — {title}"+chr(10)*2+f"2026-11-19. {summary} {counts} "
        "Three first-stage receipts survived a forced restart. The selected middle review finished first, "
        "and siblings stayed byte-identical until chosen. Wrong-unit and wrong-value handoffs were rejected. "
        "The report retains per-chain rebuild and completion timings. Actions credits were unavailable, so "
        "validation ran locally. See README-185.md and docs/publication185/plan.md."+chr(10)*2)
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/193-fray-pending-recovery.md"
canvas="docs/obsidian/193-fray-pending-recovery.canvas"
prepend("docs/obsidian/00-home.md","[Edition 185 — Choose a saved Fray handoff](193-fray-pending-recovery.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 185](../../README-185.md) · "
      "[Workflow map](193-fray-pending-recovery.canvas) · [Implementation plan](../publication185/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Pending review verification",java+"RequestWorkbench.java"),("Chooser","workspace-desktop/renderer/request-workbench.js"),
    ("Measured review execution","workspace-desktop/main.cjs")]]
nodes=[m["source"] for m in modules]+["README-185.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/185/verification.json","title":"Verification record"},
    {"path":"docs/readmes/185/browser-receipt.json","title":"Original Chromium receipt"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/184/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 185 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-184.md","../../../README-185.md").replace(
    '../183/index.html">Edition 183','../184/index.html">Edition 184')
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
assert manifest["latest"] == 184
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix==".png" else "text","sha256":item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=185
manifest["editions"].append({"number":185,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-185.md","markdownSha256":item("README-185.md")["sha256"],
    "companion":"docs/readmes/185/index.html","tag":"readme-185","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/185","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

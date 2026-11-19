"""Freeze Edition 188 from the verified desktop and regression runs."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

snap = Path("docs/readmes/188")
snap.mkdir(exist_ok=True)
live = Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition188-live")
baseline = "2fddc639512975cbe25fdf843fd4a67a69e4eb8f"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
app = json.loads((live / "result.json").read_bytes())
tests = json.loads(Path("build/edition188-regression/summary.json").read_bytes())
assert app["passed"] and tests["passed"] and app["priorEdition187Proofs"] == 2
assert app["copiedVerifiedReviews"] == 123 and app["reviewFilesAtCapacity"] == 128
assert app["pendingBefore"] == 125 and app["pendingAfter"] == 124 and app["unchangedReviews"] == 127
recovery = app["recovery"]
assert recovery["restarted"] and recovery["finalReviewBlocked"] and recovery["receiptAlreadySaved"]
assert recovery["retainedChecks"] == [5, 5] and recovery["verified"]["artifact"]["version"] == 1
assert recovery["addedArtifacts"] == 1 and recovery["receiptBytesPreserved"] and not recovery["previewReplayed"]
assert sha256((live / "browser-receipt.json").read_bytes()).hexdigest() == recovery["originalReceiptSha256"]
assert app["refusals"] == {"changedReceiptDuringRecovery": True, "changedReceiptAfterCompletion": True,
                           "missingReceiptAfterCompletion": True, "restoredProofMatches": True}
title = "Recover the final review write and recheck its stored receipt"
summary = ("Fraynix can finish a Fray review after storage rejects the final write, even when the artifact "
           "and its browser receipt were already saved. Completed chain verification checks the current receipt on disk.")
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



labels={"final-review-write-blocked.png":"Windows rejected the final review write",
        "ready-after-restart.png":"The same verified review is ready to finish after restart",
        "verified-artifact.png":"The recovered artifact opens with its browser checks passed"}
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
shutil.copyfile(live/"browser-receipt.json",snap/"browser-receipt.json")
app.update({"source":source,"tests":tests})
js(snap/"verification.json",app)



write("README-188.md",f"""# Fraynix Agent Workspace · Edition 188

[Edition 187](README-187.md) · [Offline walkthrough](docs/readmes/188/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The Workbench reports the final review write failure](docs/readmes/188/final-review-write-blocked.png)

## Recover the final review write

Restore write access to the project. In the Workbench, select **Finish verified save**. After a desktop restart, open **Request workbench**, select **Resume interrupted chain**, and open the row marked **ready to save**. Both verified steps stay attached to the review. Java checks the original artifact and matching on-disk browser receipt before it publishes the completed review.

![The same review is ready to finish after restart](docs/readmes/188/ready-after-restart.png)

## What passed locally

| Check | Result |
| --- | --- |
| Windows denies the final review replacement | Original review bytes survive while the artifact and receipt are already present |
| Desktop restarts and retries the save | Same version-one artifact and byte-identical receipt, with zero preview replays |
| Wrong browser result in the stored receipt | Recovery and completed verification both refuse it |
| Completed receipt missing | Verification refuses completion until the original receipt is restored |

The test used the 128-review limit with 125 browser-verified pending chains and three unapproved drafts. The run finished one selected review. All 127 other review files were byte-identical and 124 verified chains stayed pending. Both published Edition 187 recovery proofs also reopened. {counts}

![The original artifact opens with its checks passed](docs/readmes/188/verified-artifact.png)

[Original ten-check browser receipt](docs/readmes/188/browser-receipt.json) · [Verification results](docs/readmes/188/verification.json) · [Implementation plan](docs/publication188/plan.md)
""")
entry=read("README.md")
previous=("[Edition 187 — recover failed disk writes](README-187.md) · [Previous edition](README-186.md)"
          " · [Offline walkthrough](docs/readmes/187/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 188 — recover the final review write](README-188.md) · [Previous edition](README-187.md)"
        " · [Offline walkthrough](docs/readmes/188/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-188.md" not in index:
    start=index.index("[Edition 187")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 188 — recover the final review write](../../README-188.md)"
          " · [Walkthrough](188/index.html)"+chr(10)*2+index[start:])
record=(f"## Edition 188 — {title}"+chr(10)*2+f"2026-11-19. {summary} {counts} "
        "At 125 verified pending chains and 128 reviews, Windows sharing denied the final review replacement. "
        "Restart reused the existing artifact and original receipt. An altered or missing completed receipt was refused. "
        "All 127 other reviews were byte-identical. See README-188.md and docs/publication188/plan.md."+chr(10)*2)
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/196-fray-review-write.md"
canvas="docs/obsidian/196-fray-review-write.canvas"
prepend("docs/obsidian/00-home.md","[Edition 188 — Recover the final review write](196-fray-review-write.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 188](../../README-188.md) · "
      "[Workflow map](196-fray-review-write.canvas) · [Implementation plan](../publication188/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Final review recovery",java+"RequestWorkbench.java"),("Stored receipt verification",java+"WorkspaceStore.java"),
    ("Workbench recovery","workspace-desktop/renderer/request-workbench.js")]]
nodes=[m["source"] for m in modules]+["README-188.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/188/verification.json","title":"Verification record"},
    {"path":"docs/readmes/188/browser-receipt.json","title":"Original Chromium receipt"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/187/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 188 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-187.md","../../../README-188.md").replace(
    '../186/index.html">Edition 186','../187/index.html">Edition 187')
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
assert manifest["latest"] == 187
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix==".png" else "text","sha256":item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=188
manifest["editions"].append({"number":188,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-188.md","markdownSha256":item("README-188.md")["sha256"],
    "companion":"docs/readmes/188/index.html","tag":"readme-188","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/188","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

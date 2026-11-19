"""Freeze Edition 187 from the verified desktop and regression runs."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

snap = Path("docs/readmes/187")
snap.mkdir(exist_ok=True)
live = Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition187-live")
baseline = "2b6db7e8c7debc1401f1ae8905126e9d4c41de28"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
app = json.loads((live / "result.json").read_bytes())
tests = json.loads(Path("build/edition187-regression/summary.json").read_bytes())
assert app["passed"] and tests["passed"] and app["priorEditions"] == list(range(181, 187))
assert app["copiedVerifiedReviews"] == 59 and app["reviewFilesAtCapacity"] == 128
assert app["completed"] == {"added": 2, "pending": 123, "unchangedReviews": 126}
artifact, browser = app["failures"]
assert artifact["kind"] == "artifact-directory" and artifact["restarted"]
assert not artifact["registeredBeforeRetry"]
assert browser["kind"] == "receipt-directory" and browser["sameRegisteredVersion"]
assert browser["registeredBeforeRetry"] and not browser["restarted"]
assert all(row["retainedChecks"] == [5, 5] and row["artifact"]["version"] == 1
           and not row["previewReplayed"] for row in (artifact, browser))
assert artifact["artifact"]["id"] != browser["artifact"]["id"]
chooser = app["chooser"]
assert chooser["pending"] == 125 and len(chooser["samples"]) == 7
title = "Recover failed disk writes with a near-capacity Fray bank"
summary = ("The Workbench can retry a verified Fray save after project storage rejects an artifact or browser receipt write. "
           "Java uses the original review, evidence and artifact ID to finish the save.")
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


labels={"near-capacity-chooser.png":"125 verified pending chains in the Workbench",
        "artifact-write-failure.png":"Recovery action after the artifact destination rejects a write",
        "receipt-write-failure.png":"Recovery action after the browser receipt destination rejects a write"}
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


write("README-187.md",f"""# Fraynix Agent Workspace · Edition 187

[Edition 186](README-186.md) · [Offline walkthrough](docs/readmes/187/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![Workbench chooser listing 125 interrupted verified chains](docs/readmes/187/near-capacity-chooser.png)

## Finish a verified save

When the Workbench reports a storage error, restore write access to the project and select **Finish verified save**. If you restarted the desktop, open **Request workbench**, choose **Resume interrupted chain**, and select the original review. Java verifies the retained browser receipts and expected artifact contents before it reports completion. The result opens in **Creation Canvas**.

## Real write failures and verified recovery

| Blocked write | State after the failure | Recovery observed |
| --- | --- | --- |
| Artifact destination | Both five-check browser receipts retained and checkpoint bytes unchanged | Restart completed one version-one artifact under the original ID |
| Browser receipt destination | Version-one artifact registered and completion refused while the receipt was absent | Same-session retry recorded the receipt on the same artifact bytes and ID |

The test occupied each destination with a regular file in an isolated Windows project. The operating system rejected directory creation during the normal save. Both retries opened the final artifact directly, with zero browser preview replays. The verification endpoint refused both incomplete writes and accepted the results after recovery.

![Failed artifact write and same-review recovery](docs/readmes/187/artifact-write-failure.png)

![Failed browser receipt write and same-review recovery](docs/readmes/187/receipt-write-failure.png)

## Pending bank and local measurements

The 128-review limit was reached with 125 browser-verified pending chains across both recipes and three unapproved drafts. Java refused an extra draft and the chooser showed only the 125 resumable chains. After both saves, 123 chains were still pending and all 126 other review files were byte-identical.

| Verified pending chains | Samples | Median click-to-ready | Slowest sample |
| ---: | ---: | ---: | ---: |
| {chooser['pending']} | {len(chooser['samples'])} | {chooser['medianMs']} ms | {chooser['maxMs']} ms |

These timings include the UI action, Java listing, transport, rendering and the automation wait in one local run. The desktop also reopened archived proofs from Editions 181–186. {counts} Validation ran locally.

[Recovered ten-check browser receipt](docs/readmes/187/browser-receipt.json) · [Full measurements and verification](docs/readmes/187/verification.json) · [Implementation plan](docs/publication187/plan.md)
""")
entry=read("README.md")
previous=("[Edition 186 — recover an interrupted artifact save](README-186.md) · [Previous edition](README-185.md)"
          " · [Offline walkthrough](docs/readmes/186/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 187 — recover failed disk writes](README-187.md) · [Previous edition](README-186.md)"
        " · [Offline walkthrough](docs/readmes/187/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-187.md" not in index:
    start=index.index("[Edition 186")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 187 — recover failed disk writes](../../README-187.md)"
          " · [Walkthrough](187/index.html)"+chr(10)*2+index[start:])
record=(f"## Edition 187 — {title}"+chr(10)*2+f"2026-11-19. {summary} {counts} "
        "Tested real artifact-directory and browser-receipt write errors at 125 verified pending chains "
        "and 128 total reviews. Restart and same-session recovery kept the original browser checks and artifact IDs. "
        "All 126 unaffected reviews were byte-identical. See README-187.md and docs/publication187/plan.md."+chr(10)*2)
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/195-fray-write-failures.md"
canvas="docs/obsidian/195-fray-write-failures.canvas"
prepend("docs/obsidian/00-home.md","[Edition 187 — Recover failed disk writes](195-fray-write-failures.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 187](../../README-187.md) · "
      "[Workflow map](195-fray-write-failures.canvas) · [Implementation plan](../publication187/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Failed save recovery",java+"RequestWorkbench.java"),("Durable artifact storage",java+"WorkspaceStore.java"),
    ("Workbench recovery","workspace-desktop/renderer/request-workbench.js")]]
nodes=[m["source"] for m in modules]+["README-187.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/187/verification.json","title":"Verification record"},
    {"path":"docs/readmes/187/browser-receipt.json","title":"Original Chromium receipt"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/186/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 187 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-186.md","../../../README-187.md").replace(
    '../185/index.html">Edition 185','../186/index.html">Edition 186')
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
assert manifest["latest"] == 186
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix==".png" else "text","sha256":item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=187
manifest["editions"].append({"number":187,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-187.md","markdownSha256":item("README-187.md")["sha256"],
    "companion":"docs/readmes/187/index.html","tag":"readme-187","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/187","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))

"""Publish measured portability evidence and its offline walkthrough."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys


BASE = "9a9b2b78397e925202ffb934d7bf5b65404ff276"
ROOT = Path("docs/readmes/205")
TITLE = "Run Fraynix on Mac, Linux and Windows"
LINK_TITLE = "Mac, Linux and Windows setup"
SUMMARY = ("Download the current workspace without the full evidence archive, "
           "prepare the local app and run the guided proof on your computer.")
DATE = "2026-11-20"
PLATFORMS = {
    "macos-arm64": "macOS 15, Apple silicon",
    "macos-x64": "macOS 15, Intel",
    "linux-x64": "Ubuntu 24.04, x64",
    "windows-x64": "Windows, x64",
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
    original = read(path)
    if not original.startswith(text):
        write(path, text + original)


def publish(live, windows, mac):
    source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
    evidence = json.loads(read(live))
    tests = evidence["localTests"]
    run = evidence["matrix"]
    results = run["targets"]
    assert set(results) == set(PLATFORMS)
    assert all(result["status"] == "success" and result["proof"]["passed"] for result in results.values())
    for target, result in results.items():
        platform, arch = target.split("-")
        result_proof = result["proof"]
        assert result_proof["platform"] == {"macos": "darwin", "linux": "linux", "windows": "win32"}[platform]
        assert result_proof["architecture"] == arch
        assert len(result_proof["checks"]) == (4 if result_proof["nativeGame"] == "verified" else 3)
        assert result_proof["nativeGame"] == "verified" or (
            platform == "macos" and result_proof["nativeGame"] == "display-unavailable")
    assert evidence["localWindows"]["nativeGame"] == "verified"
    assert all(tests[name] > 0 for name in ("rootJava", "focusedJava", "desktop", "archive"))

    ROOT.mkdir(exist_ok=True)
    screenshots = []
    images = (
        (mac / "workspace.png", "mac-workspace.png", "The guided proof completed on Apple silicon macOS"),
        (windows / "game-frame.png", "relay-garden.png", "Relay Garden rendered after 40 simulation ticks on Windows"),
    )
    for source_image, name, title in images:
        assert source_image.read_bytes().startswith(b"\x89PNG\r\n\x1a\n")
        shutil.copyfile(source_image, ROOT / name)
        screenshots.append({"path": (ROOT / name).as_posix(), "title": title})
    write_json(ROOT / "verification.json", {
        **evidence, "sourceBaseline": source, "screenshots": [item(shot["path"]) for shot in screenshots]})
    counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused workspace Java tests, "
              f"{tests['desktop']} desktop tests and {tests['archive']} archive tests passed locally.")
    rows = []
    for target, platform in PLATFORMS.items():
        result = results[target]
        game = ("Rendered and moved 40 ticks" if result["proof"]["nativeGame"] == "verified"
                else "OpenGL display unavailable on hosted runner")
        rows.append(f"| {platform} | Passed | Passed | Passed | {game} |")
    table = "\n".join(rows)
    mac_gap = any(results[target]["proof"]["nativeGame"] != "verified" for target in ("macos-arm64", "macos-x64"))
    limitation = ("A physical Mac is still needed to confirm the native Game Lab renderer. "
                  "The affected hosted runs verified that the app reports an unavailable OpenGL display."
                  if mac_gap else "The hosted Mac jobs exercised the embedded renderer on both architectures.")

    write("README-205.md", f"""# Fraynix Agent Workspace · Edition 205

[Download and run](docs/INSTALL.md) · [Edition 204](README-204.md) · [Offline walkthrough](docs/readmes/205/index.html) · [Full edition history](docs/readmes/INDEX.md)

{SUMMARY}

![Fraynix completing its guided proof on Apple silicon](docs/readmes/205/mac-workspace.png)

## Run it on a Mac

1. Install **JDK 21** and **Node.js 24 LTS** for your Mac. The Electron desktop requires **macOS 13 Ventura or newer**.
2. Use the sparse Git download in the [installation guide](docs/INSTALL.md). A test checkout fetched about **15 MiB of Git objects** before downloading Java and Electron dependencies.
3. In Terminal, inside the checkout, run the first-time preparation and then open the app.

       ./Launch-Agent-Workspace.sh --prepare
       ./Launch-Agent-Workspace.sh

4. Click **Run guided proof**. It opens a local browser page, creates an artifact and verifies the result through Java. You can run it offline after the first preparation.

Linux x64 uses the same launcher. Windows x64 uses the batch launcher. An Ollama model is optional for setup and for the guided proof.

## Verified on GitHub Actions

| Runner | Desktop proof | Bullet ramp | Project restart | Native Game Lab |
| --- | --- | --- | --- | --- |
{table}

{limitation} The optional neural OpenGL 4.3 experiment uses the CPU backend on macOS. {counts} [GitHub Actions run]({run['url']})

![An actual frame of Relay Garden after movement in the embedded game on Windows](docs/readmes/205/relay-garden.png)

The native game check loaded the original Relay Garden course, advanced 40 fixed ticks and checked that the player moved and the 960 × 540 PNG changed. The restart check opened the same project in a persistent Java process and read its saved artifact.

[Verification record](docs/readmes/205/verification.json) · [Reproduction and packaging plan](docs/publication205/plan.md)
""")
    lead = f"[Edition 205 — {LINK_TITLE}](README-205.md)"
    entry = read("README.md")
    if lead not in entry:
        start = entry.index("[Edition 204")
        end = entry.index("\n", entry.index("\n\n", start) + 2)
        write("README.md", entry[:start] + "[Download and run on Mac, Linux or Windows](docs/INSTALL.md)\n\n"
              + lead + " · [Previous edition](README-204.md) · [Offline walkthrough](docs/readmes/205/index.html)"
              + " · [Full history](docs/readmes/INDEX.md)\n\n" + SUMMARY + entry[end:])
    index = read("docs/readmes/INDEX.md")
    if "README-205.md" not in index:
        start = index.index("[Edition 204")
        write("docs/readmes/INDEX.md", index[:start] + f"[Edition 205 — {LINK_TITLE}](../../README-205.md)"
              + " · [Walkthrough](205/index.html)\n\n" + index[start:])
    record = (f"## Edition 205 — {TITLE}\n\n{DATE}. {SUMMARY} "
              "The Java launcher picks the operating system's executable and the build stages the matching native libraries. "
              "All four hosted jobs passed the guided proof, native Bullet ramp and project restart. "
              f"{limitation} {counts} See README-205.md, docs/INSTALL.md and docs/publication205/plan.md.\n\n")
    for name in ("AGENTS.md", "README-CONTRIBUTORS.md", "docs/PROJECT_MEMORY.md", "docs/DEVELOPMENT_LOG.md",
                 "docs/ARCHITECTURE.md", "docs/INTEGRATED_AGENT_ROADMAP.md"):
        prepend(name, record)

    note_name = "213-portable-workspace"
    note = f"docs/obsidian/{note_name}.md"
    canvas = f"docs/obsidian/{note_name}.canvas"
    prepend("docs/obsidian/00-home.md", f"[Edition 205 — {LINK_TITLE}]({note_name}.md)\n\n")
    write(note, f"# {TITLE}\n\n{SUMMARY}\n\n{limitation}\n\n[Edition 205](../../README-205.md) · "
          f"[Installation](../INSTALL.md) · [Workflow map]({note_name}.canvas) · "
          "[Implementation plan](../publication205/plan.md)\n")
    modules = [{"title": label, "source": path, "note": note} for label, path in (
        ("Platform native selection", "gradle/desktop-platform.gradle"),
        ("Mac and Linux launcher", "Launch-Agent-Workspace.sh"),
        ("Windows launcher", "Launch-Agent-Workspace.bat"),
        ("Build, desktop and verification checks", ".github/workflows/portable-workspace.yml"),
    )]
    nodes = [module["source"] for module in modules] + ["README-205.md"]
    write_json(canvas, {
        "nodes": [{"id": str(i), "type": "file", "file": path, "x": i * 370, "y": 0,
                   "width": 345, "height": 240} for i, path in enumerate(nodes)],
        "edges": [{"id": f"edge-{i}", "fromNode": str(i - 1), "fromSide": "right", "toNode": str(i),
                   "toSide": "left"} for i in range(1, len(nodes))],
    })
    system = {"summary": SUMMARY, "modules": modules, "screenshots": screenshots, "evidence": [
        {"path": "docs/INSTALL.md", "title": "Installation and offline instructions"},
        {"path": (ROOT / "verification.json").as_posix(), "title": "Platform checks and test record"},
        {"path": "docs/publication205/plan.md", "title": "Reproduction and packaging plan"},
    ]}
    write_json(ROOT / "system-map.json", system)
    files = subprocess.check_output(["git", "diff", "--name-only", BASE, source], text=True).splitlines()
    inventory = [item(path) for path in sorted(set(files) | {note, canvas})]
    write_json(ROOT / "file-inventory.json", inventory)
    guide = read("docs/readmes/204/index.html")
    guide = re.sub(r"<title>.*?</title>", f"<title>Fraynix 205 · {escape(TITLE)}</title>", guide, count=1)
    guide = re.sub(r"<h1>.*?</h1><p>.*?</p>", f"<h1>{escape(TITLE)}</h1><p>{escape(SUMMARY)}</p>", guide, count=1)
    guide = guide.replace("../../../README-204.md", "../../../README-205.md").replace(
        '../203/index.html">Edition 203', '../204/index.html">Edition 204')
    gallery = "\n".join(f'<figure><img src="{Path(row["path"]).name}" alt="{escape(row["title"])}">'
                        f'<figcaption>{escape(row["title"])}</figcaption></figure>' for row in screenshots)
    guide = re.sub(r"<figure>.*?(?=<h2)", lambda _: gallery + "\n", guide, count=1, flags=re.S)
    for key, data in (("system", system), ("inventory", inventory)):
        payload = json.dumps(data, ensure_ascii=True).replace("</", "<\\/")
        pattern = f'<script id="{key}-data" type="application/json">.*?</script>'
        guide = re.sub(pattern, lambda _: f'<script id="{key}-data" type="application/json">{payload}</script>',
                       guide, count=1, flags=re.S)
    write(ROOT / "index.html", guide)

    manifest = json.loads(subprocess.check_output(["git", "show", f"{BASE}:docs/readmes/editions.json"]))
    assert manifest["latest"] == 204
    frozen = [{"path": asset["path"], "sha256": asset["sha256"],
               "kind": "binary" if asset["hashKind"] == "binary" else "text"}
              for asset in (item(path) for path in sorted(ROOT.iterdir()) if path.is_file())]
    manifest["latest"] = 205
    manifest["editions"].append({"number": 205, "date": DATE, "title": TITLE, "status": "validated-local",
                                 "markdown": "README-205.md", "markdownSha256": item("README-205.md")["sha256"],
                                 "companion": "docs/readmes/205/index.html", "tag": "readme-205",
                                 "sourceBaseline": source, "snapshotDirectory": "docs/readmes/205",
                                 "snapshotFiles": frozen})
    write_json("docs/readmes/editions.json", manifest)
    print(json.dumps({"passed": True, "screenshots": len(screenshots), "inventory": len(inventory), "frozen": len(frozen)}))


if __name__ == "__main__":
    publish(*(Path(arg) for arg in sys.argv[1:]))

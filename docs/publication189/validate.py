"""Run the focused Java, desktop and archive regressions and retain test counts."""
from pathlib import Path
import json
import re
import subprocess
import xml.etree.ElementTree as et

root = Path.cwd()
out = root / "build/edition189-regression"
out.mkdir(exist_ok=True)
gradle = [str(root / "gradlew.bat"), "--offline", "--console=plain", "--max-workers=1"]
classes = [
    "SpatialTest", "SpatialProjection040Test", "SpatialRuntime040Test", "SpatialGame040Test",
    "EmbeddedGameTest", "OriginalGameContentTest", "WorkspaceServerBoundaryTest", "FrayChainTest",
]

def run(args, logfile, cwd=root):
    path = out / logfile
    with path.open("wb") as log:
        subprocess.run(args, cwd=cwd, stdout=log, stderr=subprocess.STDOUT, check=True)
    return path.read_text(encoding="utf-8")


def count(path):
    reports = [et.parse(file).getroot() for file in path.glob("TEST-*.xml")]
    assert reports
    assert all(not int(suite.get(k, "0")) for suite in reports for k in ("errors", "failures", "skipped"))
    return sum(int(suite.get("tests")) for suite in reports)


run([*gradle, ":test"], "root.log")
root_tests = count(root / "build/test-results/test")
focused = [*gradle, ":workspace-engine:test"]
for name in classes:
    focused.extend(["--tests", "fraymus.workspace." + name])
run(focused, "workspace.log")
workspace_tests = count(root / "workspace-engine/build/test-results/test")
desktop = run(["npm.cmd", "test"], "desktop.log", root / "workspace-desktop")
assert re.search(r"^.\s+fail 0$", desktop, re.M)
archive = run(["python", "-m", "unittest", "discover", "-s", "scripts", "-p", "test_*archive.py", "-v"],
              "archive.log")
assert archive.strip().endswith("OK")
result = {"passed": True, "rootJava": root_tests, "focusedJava": workspace_tests,
          "desktop": int(re.search(r"^.\s+tests (\d+)$", desktop, re.M)[1]),
          "archive": int(re.search(r"Ran (\d+) tests", archive)[1])}
(out / "summary.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
print(json.dumps(result))

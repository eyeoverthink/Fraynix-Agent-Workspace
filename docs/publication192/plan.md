# Teach a checked workflow from a real outcome

1. **Teach.** In Mission, save an original Blender gap repair under a lesson name. Java rechecks the failed baseline, the derived candidate, both native playtests, the report and its recorded browser checks before writing the lesson.
2. **Apply.** Select a different imported level and a lesson in Game Lab. The desktop prepares one explicit Chat request. Java binds the exact lesson to that mission and runs the target's baseline. It derives tile edits from the target's observed motion and terrain.
3. **Check.** Java limits reuse to the same original course layout, failure type and observed limits. It records a separate candidate, two matching native wins and the report on success. The browser checks the report and its lesson label. Both the teacher and the new result are checked again on replay.

Lessons are project-local, named, immutable checkpoints. Re-saving the same example under the same name is idempotent. Applying a lesson cannot make its own result a teaching example. The lesson records limits and references to evidence, so each application spends work checking new input. The lesson mechanism leaves original direct repair requests available.

## Reproduce

From the repository root with Java 21, Blender and the desktop dependencies installed, run the commands below. The desktop validator opens a separate test project and uses real Blender, native Game Lab runs and Chromium checks.

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication192/validate.py
node docs/publication192/validate-desktop.cjs build/edition192-live
node docs/publication192/validate-guide.cjs
```

To regenerate the fresh Blender variant from the previous edition's wide-gap fixture, pass it as the background input to Blender and run `docs/publication192/make-fresh-level.py` with the output path after `--`. The script restores two columns on the far side of the gap. Re-generated Blender files can differ in container bytes. The desktop validator ties the exact imported file to the measured report.

## Next phase

Connect the same teaching steps to a second creation procedure. Define its input contract, a fresh-input verifier and a refusal condition first. A useful test is to teach a data-to-browser-tool workflow, then rerun it on unfamiliar values and compare the resulting tool behavior with Java.

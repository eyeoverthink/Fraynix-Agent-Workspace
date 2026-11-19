# Teach a verified data-to-tool workflow

1. **Build an example.** Send `Build a data-to-tool converter from Celsius to Fahrenheit.` in Chat. Attach a UTF-8 CSV headed `celsius` in the Workbench and confirm the build. Inches to millimetres uses the `inches` header.
2. **Teach in Chat.** Send `Teach data-to-tool lesson temperature-batch`. The desktop selects the latest verified original CSV tool opened in that project. Java rereads the input, checked artifact and recorded browser receipt before saving the named lesson.
3. **Reuse.** Send `Use data-to-tool lesson temperature-batch`. Attach matching CSV with at least one measurement absent from the example. Java compares numeric values, so a change in formatting or row order alone cannot qualify.
4. **Verify.** Java binds the source bytes, lesson and review. It uses the existing Fray evaluator to derive expected results. The browser uses fixed-point integer arithmetic to compute the same conversion. It checks the attached input, unseen values, an invalid header and a malformed number, then restores the attached rows. The tool becomes verified after its browser receipt and artifact are retained.
5. **Recover.** Reopen the last review after restart. A waiting review preserves its attached data and lesson. If the browser checks finished before saving was interrupted, Java can finish from their original checkpoint. A verified review opens or exports only while its input, artifact, lesson and receipts recheck.

## Input contract

One case-sensitive header followed by 1–24 lines of plain decimals. Up to four decimal places and a range from −1,000,000 to 1,000,000. Blank interior rows, extra columns and scientific notation are refused. Saving a lesson requires an independently verified original, so reuse cannot teach from its own result.

## Local reproduction

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication193/validate.py
node docs/publication193/validate-desktop.cjs build/edition193-live
node docs/publication193/validate-guide.cjs
```

The desktop validator uses an isolated project, the desktop's actual browser and the Java engine. Its CSV measurements are synthetic. An exported HTML example runs from a local file and accepts new rows without an engine connection.

## Follow-on

Extend the checked input contract to named columns in a wider table. Measure the full build and verification cost and test missing, duplicate and reordered columns before exposing it in Chat.

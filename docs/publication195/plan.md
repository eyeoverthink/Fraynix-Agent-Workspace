# Teach an offline CSV classifier from explicit rules

1. Send `Build a data-to-tool classifier using column reading.` in Chat. Attach a rules CSV with the exact headers `min_inclusive,max_exclusive,label`. Attach the measurement CSV after the rules pass validation.
2. Java checks the rule table and the selected measurement column. It computes the expected label for each row and compares the complete result with Chromium's separately computed output. Each rule's lower threshold and last representable value below its upper threshold are exercised. The browser also checks the final excluded boundary and the first internal gap when present.
3. Send `Teach data-to-tool lesson sample-ranges` once the original tool verifies. Saving the lesson requires an unseen valid value in the checked browser cases. The lesson pins the canonical rule table, exact rule file hash, column name and original evidence.
4. Send `Use data-to-tool lesson sample-ranges`, attach fresh measurements and confirm. Reuse verifies the teaching source again and applies the original rules to the selected header, wherever it appears. It saves a separate result with independent browser receipts.

## File contract

The rules file has two to six nonoverlapping ranges. Lower bounds are inclusive and upper bounds exclusive. Limits and measurements use up to four decimal places within −1,000,000 to 1,000,000. Labels are unique, start with a letter, and can contain letters, digits, interior spaces, underscores or hyphens. Gaps are allowed. A row outside all ranges is refused. The data may contain 1–24 rows and up to 8 input columns with unique names. Each file is limited to 2 KiB. The tool preserves the existing cell values and appends `classification`.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication195/validate.py
node docs/publication195/validate-desktop.cjs build/edition195-live
node docs/publication195/validate-guide.cjs
```

Supply a previous edition 194 test project as the desktop validator's third argument to recheck its saved artifacts. The release used `build/edition194-release/project`. All fixtures are synthetic. Build timing spans confirmation through browser checks, save and Java proof.

## Follow-on

Let a checked classification select the next verified procedure from an explicit mapping. Start by previewing the choices and reasons, then test missing mappings and interrupted execution before allowing confirmed tasks to run.

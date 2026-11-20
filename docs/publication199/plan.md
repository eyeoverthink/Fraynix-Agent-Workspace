# Compare two verified routing exports

1. Export two completed routing plans and send `Compare the last two verified routed CSVs.` The most recent saved copy for each distinct plan counts toward the selection. The last two distinct plans supply the earlier and later exports.
2. Java checks each local CSV and receipt against its approved routing plan. Chromium independently reads both CSVs and computes their hashes, counts, ranges and means. The report keeps temperature and length separate and retains both source files.
3. The change is the later mean minus the earlier mean. The exact means are used before rounding the change to five decimal places. An output unit that is present on only one side has no numerical change. The report compares summaries of each export's rows.
4. After restart, send `Recheck the routed CSV comparison.` The app reopens the last comparison's original pair. It reruns the browser checks against Java and leaves the original artifact and receipt intact.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication199/validate.py
node docs/publication199/validate-desktop.cjs build/edition199-live build/edition198-release/project
node docs/publication199/validate-guide.cjs
```

The desktop validator copies the synthetic edition 198 project, with its previously checked dashboard and the retained edition 196 and 197 routing plans. It exports both plans through the ordinary desktop action, repeats one export and creates the comparison through Chat.

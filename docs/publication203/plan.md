
# Create an offline explorer from a verified comparison

1. Export and summarize a matched comparison in Chat. Send `Build an explorer from the verified comparison totals.` Fraynix attaches the verified summary, the CSV behind it and its export receipt.
2. Java checks both routing plans, the original comparison and the summary artifact and browser receipt. Only that verified source can be used to build the explorer.
3. The offline explorer reads the CSV and lets the user filter by unit and status. The table shows exact keys, source row numbers and values. Net and total absolute change recalculate from matched groups within each unit.
4. Chromium verifies interactions with the explorer against Java before the artifact is saved. A separate Python decimal check covers all 15 filter combinations in the offline copy.
5. After restarting, send `Recheck the verified comparison explorer.` Fraynix uses the original source and reruns the browser checks.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication203/validate.py
node docs/publication203/validate-desktop.cjs build/edition203-live build/edition202-release/project
node docs/publication203/validate-guide.cjs
.\scripts\Verify-Documentation.ps1
```

The validator copies the synthetic [edition 202 project](../publication202/plan.md) into an isolated runtime directory. It causes one blocked artifact write, restarts to complete the save, then makes a newer summary and checks that another restart preserves the explorer's original source.

## Next candidate

Make the explorer a reusable creation procedure and apply it to verified comparison CSVs with different row and unit distributions. Verify each new source, filter and result before accepting the artifact.

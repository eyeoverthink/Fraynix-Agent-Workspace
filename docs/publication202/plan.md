
# Reuse a verified comparison CSV in Chat

1. Export a matched comparison using edition 201. Send `Summarize the last verified matched comparison CSV.` Fraynix attaches the newest comparison export from this project's export manifest.
2. The desktop checks the disk CSV and adjacent receipt against the manifest. Java reloads both routing plans and the saved comparison, checks its browser receipt and reconstructs the expected export. The CSV bytes and provenance must agree with that result.
3. Java and Chromium separately tally each status and calculate net and total absolute change for matched groups within each output unit. The saved artifact retains the CSV, receipt, source IDs and checked totals.
4. After restart, send `Recheck the matched comparison CSV summary.` Fraynix reattaches the original export by ID, reruns its checks and reopens the same artifact.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication202/validate.py
node docs/publication202/validate-desktop.cjs build/edition202-live build/edition201-verified/project
python docs/publication202/verify-totals.py docs/readmes/202/matched-comparison.csv docs/readmes/202/totals-proof.json
node docs/publication202/validate-guide.cjs
.\scripts\Verify-Documentation.ps1
```

The desktop validator copies the synthetic [edition 201 project](../publication201/plan.md) into a separate runtime directory. Pass its project path as the second argument if that validation used a different output directory.

## Next candidate

Let a later creation request use these verified totals to build a report or tool, with the attached CSV receipt and independent output checks carried into the new artifact.

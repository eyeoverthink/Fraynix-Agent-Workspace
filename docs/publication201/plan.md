
# Export a verified matched comparison

1. Build a row comparison in Chat from two approved routing exports. For the edition 200 sample, send `Compare the last two verified routed CSVs by key "record ID".`
2. Send `Export the matched routed CSV comparison.` The workbench reopens the saved key and pair, rechecks both original routing plans and independently reruns the comparison in Chromium.
3. Java checks the fresh browser results and produces one CSV row per key and unit. It parses the generated CSV back and verifies every group against the checked comparison before returning its SHA-256.
4. The desktop checks the transfer and the bytes written to disk. The export receipt links the CSV to the original artifact, the key, both source export IDs and hashes, and the browser evidence hash.
5. Restart the desktop and run the export command again. The same checked comparison produces identical CSV bytes in a new export with its own receipt.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication201/validate.py
node docs/publication201/validate-desktop.cjs build/edition201-live build/edition200-first/project
node docs/publication201/validate-guide.cjs
.\scripts\Verify-Documentation.ps1
```

The desktop validator copies the synthetic Java workspace from [edition 200](../publication200/plan.md) into a separate runtime directory. If that validation used a different directory, pass its project path as the second argument.

## Next candidate

Let Chat attach the verified comparison CSV to a follow-on data task. Carry its receipt and unit information into the task and independently check any derived totals.

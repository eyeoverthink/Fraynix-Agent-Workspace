# Build a verified dashboard from a routed CSV in Chat

1. Export a completed routing plan. Send `Build a dashboard from the verified routed CSV.` Chat reads the latest local routed export and its receipt without asking for a second copy of the data.
2. Java rebuilds the routed export from the original approved plan and its browser receipts. The disk CSV must match the rebuilt bytes and the receipt must match the approved source and child artifacts.
3. The dashboard shows count, minimum, maximum and mean separately for °F and mm. Java uses exact decimals and the browser uses scaled integers. Means round to five fractional places, with ties away from zero.
4. Chromium checks the summary for each unit, the full source-ordered list, each filter and the link to the original CSV. Java accepts the actual check results and saves one artifact with the original CSV and verification record.
5. Send `Recheck the routed CSV dashboard.` after restart. Java rereads the export, original evidence and saved dashboard. Chromium then reruns its checks on the opened artifact. The original receipt stays intact. If a save was interrupted after browser checks, the build command can finish from those retained checks.

## Reproduction

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication198/validate.py
node docs/publication198/validate-desktop.cjs build/edition198-live build/edition197-final/project
node docs/publication198/validate-guide.cjs
```

The desktop validator copies the named edition 197 project, including its export and evidence. To regenerate that fixture, run the edition 197 desktop validator in a fresh folder and use its `project` subfolder. The documented release used the same verified CSV bytes as edition 197. All validation data is synthetic.

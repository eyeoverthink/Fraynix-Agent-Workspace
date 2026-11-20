
# Compare routed rows by key and unit

1. Export two distinct, completed routing plans. In Chat send `Compare the last two verified routed CSVs by key "record ID".` Substitute the exact original header shared by both files.
2. Java reconstructs each CSV from its approved routing plan and verified child results. It checks the disk exports and receipts against that source. It reads the named column independently in each CSV so reordered columns work.
3. Match exact case-sensitive key values within the same output unit. One reading on each side produces an exact decimal difference. Zero readings on one side produce an added or missing group. More than one reading on either side produces a duplicate group with every original row number and value.
4. Chromium independently parses both CSVs and calculates every visible result. Java checks the selected key, both source labels, the counts, all six columns of every visible group and both download links.
5. After restart, send `Recheck the matched routed CSV comparison.` The app uses the saved key and both export IDs. A fresh browser run checks the saved artifact against Java while the original receipt and artifact remain available.

## Reproduce locally

Use the JDK and desktop dependencies already configured for the Java workspace.

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication200/validate.py
node docs/publication200/validate-desktop.cjs build/edition200-live build/edition199-first/project
node docs/publication200/validate-guide.cjs
.\scripts\Verify-Documentation.ps1
```

The desktop validator copies the synthetic edition 199 project. It rechecks the earlier aggregate comparison and builds two fresh classification and routing plans using approved temperature and length lessons. The edition 199 reproduction guide explains how to create the prerequisite project. If that run used a different directory, pass its project path as the second argument. The test copies project data into a separate runtime directory.

## Next candidate

Export the matched comparison as a verified CSV. Preserve the source row references and verify every exported status and difference against the saved comparison.

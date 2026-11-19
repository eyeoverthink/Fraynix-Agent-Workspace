# Route a verified classification to reviewed procedures

1. Verify a range classifier in the Workbench. Its measurement CSV also contains the input columns required by the procedures you want to run.
2. Verify independent CSV conversion tools and give them lesson names in Chat. The supported procedures are Celsius to Fahrenheit and inches to millimetres.
3. Send `Route the last verified classification.` and attach a CSV with `label,lesson` headers. Java rereads the classifier, its rules and receipt. It checks named lessons and previews the range, classification, selected procedure and exact input for each row.
4. Choose **Confirm and run**. The approved plan binds the classifier, mapping bytes, selected lessons and inputs. Rows for the same lesson run as one ordered batch. Each batch uses the existing browser tool verifier and is accepted only after a fresh independent proof.
5. Use **Pause after the next verified procedure** to stop between batches. After restart, send `Resume classification routing.` Java rechecks the original source and all accepted results. A child already verified before the plan recorded its result is recovered under the same ID and receipt.

## Scope

The mapping uses declared labels and up to six project-local lesson names. Lesson names identify already verified conversions and their required input columns. Every label present in the classifier output needs a mapping. Batches must contain a value absent from the lesson's example. The preview keeps row order and only passes the exact input column to each converter. Approved mappings are immutable. Complete plans retain original result references and let the user reopen each verified artifact.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication196/validate.py
node docs/publication196/validate-desktop.cjs build/edition196-live
node docs/publication196/validate-guide.cjs
```

An optional last argument supplies a previous edition 195 test project to copy and verify. The release used `build/edition195-first/project`. The tests use synthetic data and the actual desktop browser, Workbench and Java proof gates.

## Follow-on

Add an independently checked report that restores the original row order and combines the verified batch outputs into one CSV. Exercise mixed units, duplicate values and partial completion before exposing report export.

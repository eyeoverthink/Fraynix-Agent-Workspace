
# Teach and reuse the verified comparison explorer

1. Build an original explorer from a verified summary. Send `Teach comparison explorer lesson field-audit.` Fraynix reruns the explorer checks, then Java saves a named record tied to the original artifact, input hash and browser evidence.
2. Produce a fresh matched comparison CSV through the routing and comparison workflow. Send `Use comparison explorer lesson field-audit.` Fraynix checks the original teaching evidence and the fresh comparison. It builds or reuses the checked summary and creates a new explorer tied to that lesson and input.
3. Each new run checks the full table, then selects a populated unit and status from the actual input. A separate independent check exercises all unit and status combinations in the two release fixtures.
4. After restart, send `Recheck comparison explorer lesson field-audit.` The app reloads the last input used by that named lesson and reruns the checks. The saved lesson and original browser receipt remain available.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication204/validate.py
node docs/publication204/validate-desktop.cjs build/edition204-live build/edition203-final/project
node docs/publication204/validate-guide.cjs
.\scripts\Verify-Documentation.ps1
```

The desktop validator copies the synthetic [edition 203 project](../publication203/plan.md) into an isolated runtime directory. It teaches through Chat, creates four new verified routing plans, and applies the lesson to two new comparisons. The measured cases distinguish a true zero net change from a unit with no comparable pairs. Changed inputs and changed teaching evidence are tested in that copy.

## Next candidate

Use a single named procedure on a batch of verified comparisons. Show a result or refusal for each input and resume the batch from its last verified output after interruption.

# Verify a tool that selects a named CSV column

1. **Choose the input.** Add `using column probe_c` to the registered Celsius-to-Fahrenheit request in Chat. The Workbench asks for a CSV and shows the chosen header. The inches-to-millimetres request accepts the same form.
2. **Bind the lesson.** The verified original fixes the conversion, the exact measurement header and the reference to the original evidence. A fresh file may order columns differently. It must still contain a measurement value absent from the example.
3. **Build and check.** Java locates the column by name and computes expected values with Fray. The offline tool parses the CSV independently, uses fixed-point arithmetic and appends a result column. Verification compares the entire converted table, including unchanged cells. It also moves the target column to a different position in an unseen test and checks rejected inputs.
4. **Recover and export.** Reopen an attached review after restart. A completed result still requires the original source, lesson and browser receipt to verify before export. Older single-column lessons and their original receipts use their existing contract.

## Input and output

Use UTF-8 CSV up to 2 KiB, with 1–8 unique nonempty headers and 1–24 measurements. Header matching is case-sensitive. Quoted text, commas, double quotes, Unicode and embedded line breaks are supported in other cells. Every row must have the declared width. The selected measurement cells use plain decimals from −1,000,000 to 1,000,000 with up to four fractional digits. The new column is `fahrenheit` or `millimetres`. An existing output header is refused. The result preserves the values and order of existing cells. CSV quoting and line endings can be normalized.

## Reproduction

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication194/validate.py
node docs/publication194/validate-desktop.cjs build/edition194-live
node docs/publication194/validate-guide.cjs
```

Pass a previously generated edition 193 validation project directory as the last argument to the desktop validator to copy and recheck its checked work. The release run used `build/edition193-release/project`. The benchmark records attachment time and confirmation through browser checks, durable save and Java proof separately. The figures are individual desktop observations with synthetic inputs.

## Follow-on

Let a user teach a checked procedure that selects one of several verified results using explicit rules in a table. Start with simple ranges and classifications, and exercise boundary values and unfamiliar rows before saving the procedure for reuse.

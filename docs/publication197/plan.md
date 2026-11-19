# Export a verified routing plan in its original row order

1. Open an approved routing plan after all its procedures have verified. **Export verified CSV** uses the workspace's local export directory.
2. Java rereads the approved mapping, classifier and saved browser receipts. It uses each source row's ordinal and the next output in its approved lesson's batch to rebuild the original order. It checks the converted value again with exact decimal arithmetic and reparses the final CSV to verify its cells.
3. The saved CSV keeps the original columns and appends the logical CSV row number, classification, lesson, input column, input value and unit, output value and unit. Logical row numbers start at 2 because the header is row 1. When a source header collides with a routing header, Java selects the next available common prefix, such as `route_2_`.
4. The export receipt records the approval digest, source binding, child artifact references, row count and exact CSV hash. A plan with an unfinished or unaccepted result cannot export. Reopening the completed plan checks the same evidence again.

The demonstration uses both the named-column temperature converter and the single-column inches converter. It includes interleaved units, the same numeric value in both units, duplicate IDs, an exact duplicate source row, a quoted multiline cell and a header collision.

## Reproduce locally

```powershell
.\gradlew.bat --offline --console=plain --max-workers=1 :workspace-engine:installDist
python docs/publication197/validate.py
node docs/publication197/validate-desktop.cjs build/edition197-live
node docs/publication197/validate-guide.cjs
```

An optional final argument supplies a previous edition 196 test project. The published run used `build/edition196-release/project` to recheck and export the earlier verified plan. All fixtures are synthetic.

# Raw method-lock storage correction

The first committed-file release audit refused the method-lock hash before any146push or tag. Git had normalized CRLF to LF; the executed source manifest and harness correctly bind the raw CRLF bytes. The new docs/.gitattributes rule stores the original executed bytes exactly. Canonical content and all90 frozen source hashes remain unchanged. This is a publication representation correction, not a new method lock or relabelled experiment. Original failed log and before/after hashes are retained in this folder.

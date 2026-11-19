# From a request to verified work

Fraynix already has typed tools, known sources, an inspectable game and physics runtime, and verified Canvas outcomes. The workbench makes those capabilities easier to reach from everyday language alongside the saved Edition 179 route and the existing Creation Canvas.

## What Edition 180 adds

- Match a bounded set of everyday, slang and multilingual commands without a model. Ask an installed model to classify unfamiliar phrasing into a fixed menu. An action proposed by the model still needs a human review.
- Choose the intended action when the suggestion is incomplete or wrong. After a successful file task, retain the confirmed wording against that verified result. An exact normalized match can propose the same action on a later request, with a fresh source and approval.
- Convert between UTF-8 CSV and the workbench JSON table. Accept equivalent flat JSON records when every row has the same string-valued fields. Preserve cells and column order. CSV record objects use a deterministic sorted order. Retain a source hash and compare the decoded inverse.
- Read the exact bytes of an explicitly selected file. Report the full file size and checksum, a header-signature hint, and offsets, hex, decimal and bits for the first 64 bytes. The input is treated as data and remains local.
- Hand a reviewed tool or app brief to the existing Canvas, where behavior checks and the original creation worker own generation and repair.

## Where to take Babel next

| Phase | Build | Proof required |
| --- | --- | --- |
| Typed data | Add reviewed column schemas, explicit null handling, exact decimal encodings and unit transformations. | Verify both directions on held-out tables and show any irreversible mapping before approval. |
| Code conversion | Define a small checked intermediate representation for standalone utilities. Start with Java and JavaScript emitters, then add another target when representative fixtures pass. | Compile or load each output and compare actual behavior against the same inputs, edge cases and expected results. |
| Binary structure | Add documented, read-only format readers for formats Fraynix actually produces, beginning with Java class metadata and bounded archive inventories. | Check decoded structures against known compiled fixtures and handle malformed lengths. Use source offsets and retain explicit unknowns. |
| Broader language | Test ordinary requests, play language, short commands, mixed languages, references to recent work and user corrections. | Hold out new prompts and compare local models and remembered phrases on both accuracy and complete workflow time. |

Current model candidates are the installed Gemma 4 and gpt-oss models. Ollama's function calling defines the review interface. The CSV and JSON specifications explain why attaching types requires an explicit agreement. W3C's tabular-data model and the JVM class-file specification give the next phases testable contracts. [Ollama tool calling](https://docs.ollama.com/capabilities/tool-calling) · [Gemma 4 tool formatting](https://ai.google.dev/gemma/docs/core/prompt-formatting-gemma4) · [RFC 4180](https://www.rfc-editor.org/info/rfc4180/) · [RFC 8259](https://www.rfc-editor.org/info/rfc8259/) · [W3C tabular data](https://www.w3.org/TR/tabular-data-model/) · [JVM class file specification](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html)

The supplied FRAYMUS-RE dossier distinguishes tool-read addresses, independently checked imports and model annotations. Carry that evidence structure forward as a source hash, exact byte offsets and separately identified interpretations. Edition 180 establishes the source hash and direct byte observations in Java.

Qwen 3.6 is a published coding-model candidate for the next comparison. The same language and behavior fixtures provide a way to test whether it earns its local runtime cost. [Official Qwen 3.6 repository](https://github.com/AlibabaCloud-Official/Qwen3.6)

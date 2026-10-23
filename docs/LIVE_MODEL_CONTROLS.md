# Live model controls and chat repair

Implemented October 23, 2026 in independent Fraynix Studio.

## Diagnosis

The screenshot's reply came through local Ollama, but the application supplied a generic editor SYSTEM prompt that replaced the selected model's own identity. It combined the user's message and complete scene JSON into one displayed/persisted prompt, and replayed previous successful exchanges including their old scene dumps. There was no incremental UI response or useful runtime configuration surface.

A fresh unpredictable nonce sent directly to `fraynix-ultimate:8b-64k` returned correctly in ten HTTP frames. The greeting was a model response, rather than a fabricated replacement from the editor.

The secondary `eyeoverthink/fraymus-bicameral:latest` recipe has a repetition penalty of 1.618 and custom stop strings including the DNA emoji. Reviewed runs included empty completed output and incorrect nonce copying. A compatibility request with temperature 0, repetition penalty 1 and custom stops disabled returned the nonce correctly. This supports the compatibility profile; it does not isolate the cause of every previous failure.

## Desktop controls

Open **Fraynix > Models**:

- **Test connection** sends a new nonce and displays the actual response. Its diagnostic settings use temperature 0, repetition penalty 1, no custom stop strings and 256 output tokens. ASCII case changes are accepted. Evidence records the expected nonce, model, endpoint, stream frames, timings and hashes.
- **Load model / Unload model** send actual Ollama requests. The following `/api/ps` refresh reports observed residency.
- **Runtime settings** save context, output allowance, CPU threads, GPU layers, response-idle timeout, keep-alive, temperature, repetition penalty and custom-stop behavior by project, exact tag and endpoint.
- **Cancel** cancels this client's asynchronous HTTP exchange. It does not kill shared Ollama or assert that server-side generation has terminated.

Defaults are 8K context for smaller models and 4K above 20 GB, 1,024 output tokens, automatic threads/GPU layers, 900 seconds of response-idle allowance, ten minutes of keep-alive, temperature 0.4 and repetition penalty 1.1. These are editable settings, not hardware-capacity guarantees. A measured compatibility profile was saved for the bicameral model in the user's project. Its original recipe and weights remain unchanged.

In **Chat**:

- Answers stream into the window. Reasoning and answer text are handled separately.
- The selected model's SYSTEM instructions are preserved. The generic Fraynix/editor identity is no longer injected.
- Scene attachment is off by default. An attached complete observation is saved separately and displayed collapsed.
- Legacy observation suffixes are separated on read without rewriting the original records.
- Legacy conversations start a fresh thread after upgrade. **Resume latest saved chat** retrieves the previous conversation; **New conversation** retains prior records while starting another thread.
- Recent history uses a character-based estimate and whole exchange pairs. It excludes old scene attachments. This is not an exact tokenizer budget: large current observations or long model SYSTEM prompts still require an appropriate context profile.
- **Request evidence** exposes actual server-reported model/completion, frame count, token/timing metrics and request/response SHA-256 values. The response hash covers nonblank NDJSON lines normalized with LF endings. This is transport evidence, not a signed weight attestation.

Empty answers, interrupted streams without a completion frame, HTTP errors and cancellation produce explicit failures. Partial output is labeled incomplete. No canned fallback answer is substituted. Chat remains text conversation; **Create** owns validated scene changes and **Tools** owns the five compiled functions.

## Persistence

Project files:

```text
chat/exchanges.jsonl          Replies, attachments, reasoning and request evidence
models/runtime-profiles.json Saved model/endpoint settings
models/operations.jsonl       Connection/load/unload results
```

The user's project was backed up and the old app closed normally before replacement. Its saved checkpoint stayed byte-identical through the update. Private conversations, backups and weights are not checked into Git.

## Verification

- The full check passed: **58 editor JUnit tests and six executable module harnesses**.
- Controlled HTTP tests cover Unicode NDJSON, callbacks before completion, reasoning, request options, completion/truncation, active/pre-connect cancellation, load/unload, profile restart, legacy attachment migration and history/thread isolation.
- The GUI controller test persists an attachment and response evidence through actual fixture HTTP, then reopens the project to verify them and its settings.
- Two native editor processes used `fraynix-ultimate:8b-64k` on port 11434 and `eyeoverthink/fraymus-bicameral:latest` on port 11435. Both returned streamed replies and passed fresh nonce checks. All six tabs rendered; eight fixture entities and physics bodies retained their identities; chat survived restart.
- Actual load/unload handlers were tested on the bicameral model. Ollama residency was present after load and absent after unload; another fresh nonce verified inference.
- Native framebuffer captures were visually inspected. Numeric controls were widened after review. External Windows screenshot capture timed out, so the app's framebuffer supplied visual evidence.
- The normal project was reopened with the new Models controls and an actual startup connection check.

See [sanitized evidence](evidence/live-model-controls-2026-10-23.json). Raw logs/test projects/private backups remain in ignored `build/`.

## Boundaries

Profiles apply to Chat and explicit loading. Create retains its own context/turn controls and reviewed scene protocol; its older adapter's read allowance increased to 900 seconds. This does not add arbitrary tool execution to Chat or implement the entire agent factory.

Ollama's server-side load timeout is separate. Historical 120B measurements remain evidence, but a cold 120B run was not repeated for this release. Same-tag store selection still follows catalog order; digest-specific selection remains planned. Authenticated Base44/MCP integration remains planned.

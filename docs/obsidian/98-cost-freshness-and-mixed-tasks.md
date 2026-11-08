# Cost freshness and mixed tasks

Edition090 implemented, validated and published to main. Forced memory was faster on 6 of six tasks and slower on 0. The guard marked 3 tasks outside scene coverage and 1 tasks with repeated cost disagreement. These are observed results, not predetermined success labels. Guarded selection took 104.08 s longer than simple control across these six task calls, before charging the shared fit. 31 actual native goals / 18 mixed goals; 256 distinct Java cases with explicit source-qualified invocation records. No weight training or physical accuracy claim. Fetched release verification is recorded in docs/PUBLICATION_090.json.

[[README-090.md]] · [[README-089.md]]

| Policy | Success | Complete Java tasks | Task calls | Native steps | Memory choices |
| --- | ---: | ---: | ---: | ---: | ---: |
| simple | 6/6 | 568.69 s | 570.95 s | 149,684 | 0/6 |
| memory | 6/6 | 513.80 s | 516.09 s | 126,428 | 6/6 |
| guarded090 | 6/6 | 671.37 s | 675.03 s | 141,932 | 2/6 |

| Task condition | Forced-memory saving vs simple | Guarded saving vs simple | Guard status |
| --- | ---: | ---: | --- |
| 090-mixed-1 · occupied | 7.07 s | -12.60 s | consistent-so-far |
| 090-mixed-2 · relocated-obstacles | 10.05 s | -3.91 s | outside-coverage |
| 090-mixed-3 · occupied | 8.30 s | -22.41 s | consistent-so-far |
| 090-mixed-4 · relocated-obstacles | 6.36 s | -8.55 s | outside-coverage |
| 090-mixed-5 · occupied | 12.30 s | -51.84 s | suspect |
| 090-mixed-6 · relocated-obstacles | 10.78 s | -4.76 s | outside-coverage |

Bounded source verification, context coverage and persistent warnings are Java-owned. The live045 private project remains untouched.

## Shorter-trip follow-up

Separately declared shorter-trip diagnostics: forced memory was faster on 0/3 and slower on 3/3. These cases follow the observed long-trip timeout; they are not independent held-out confirmation. Warmup, copying and startup costs are separate.

| Policy | Success | Complete Java | Task calls | Native steps |
| --- | ---: | ---: | ---: | ---: |
| simple | 3/3 | 157.91 s | 159.48 s | 33,648 |
| memory | 3/3 | 168.84 s | 171.01 s | 33,648 |
| guarded090 | 3/3 | 180.88 s | 182.82 s | 33,648 |

| Diagnostic | Memory saving vs simple | Guard saving vs simple | Memory extra native steps |
| --- | ---: | ---: | ---: |
| 090-short-1 | -5.16 s | -9.33 s | 0 |
| 090-short-2 | -5.26 s | -8.99 s | 0 |
| 090-short-3 | -1.11 s | -5.02 s | 0 |

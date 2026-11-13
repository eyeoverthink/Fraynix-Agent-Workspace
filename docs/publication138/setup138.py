from pathlib import Path
r=Path.cwd()
s=(r/'docs/publication137/validate137.py').read_text(encoding='utf8').replace('137','138')
for name in ['Persistence','ReferenceMemory','VerifiedMemoryFlow','AppearanceAudit']:s=s.replace(name+'138Test',name+'137Test')
s=s.replace("tests=['AppearanceFlow135Test'","tests=['DriftGuard138Test','AppearanceFlow135Test'")
(r/'docs/publication138/validate138.py').write_text(s,encoding='utf8')
note='''## Edition 138 active — 2026-11-13

Continue J:/Codex-Work/fraynix-workspace-edition138 from5ee114d6. CI checked FIRST, run43478086624 pending. Read docs/EDITION_138_CONTRACT.md. Implemented opt-in immutable-source drift guard and monitor-only control, complete original-pixel decision replay and independent warning/outcome scoring. Defaults unchanged; target predictions never enter anchor memory. Four targeted tests pass; first test compile typo and log preserved. Previously exposed137 Tiger2: wrong overlaps70→0, correct25 unchanged; Box75correct/20wrong unchanged. Development only. Freeze before downloading/viewing fresh Skating2/Suv; source-only windows/visibility/other boxes fixed before predictions; no outcome tuning. No publication yet.

No agents/overlapping heavy jobs. Shared desktop dependency junction is read-only. Preserve private projects and frozen001–137. Publish only Fraynix-Agent-Workspace and audit fetched Git. No relabelled old proofs or invented promotion.

'''
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
 p=r/name;p.write_text(note+p.read_text(encoding='utf8'),encoding='utf8')

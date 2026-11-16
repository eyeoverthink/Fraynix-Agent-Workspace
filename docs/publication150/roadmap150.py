"""Update current roadmaps after measured results exist; preserve historical edition snapshots."""
from pathlib import Path
import json
r=Path.cwd();v=json.loads((r/'docs/readmes/150/verification.json').read_bytes());assert v['passed']
finding=(r/'build/findings150.md').read_text(encoding='utf8').strip();next_steps=(r/'build/next150.md').read_text(encoding='utf8').strip()
updates={
 'docs/INTEGRATED_AGENT_ROADMAP.md':'## Edition 150 — verified starting references and measured returns\n\n[Measured release](../README-150.md) · [Evidence and source domains](readmes/150/comparison.json)\n\n'+finding+'\n\n'+next_steps+'\n\n',
 'docs/PLAYABLE_LEVEL_CREATION_PLAN.md':'## Edition 150 — observation evidence in the connected application\n\n[Edition 150](../README-150.md) retains an explicitly verified starting appearance even when novelty filtering correctly admits no learned examples. Generated recovery and restart checks remain separate from physical-camera outcomes. The new displaced-return recording still has 31 missing observations out of 42 visible/partial samples for all compared methods; no tracker is promoted.\n\nThe persistent Java world, Game Lab, browser and creation workflows remain the architectural foundation. Future observation-driven world or artifact requests must carry original evidence links and distinguish measured, predicted and unknown state. Unresolved image correspondence does not authorize a physical action. Continue the existing level-construction/playtesting plan below alongside independently qualified perception work.\n\n'
}
for name,prefix in updates.items():
 p=r/name;old=p.read_text(encoding='utf-8-sig')
 if not old.startswith(prefix):p.write_bytes((prefix+old).encode())
print('Updated integrated and Game Lab roadmaps from measured150 results.')

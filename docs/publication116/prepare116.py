"""Prepare edition-specific publication helpers; never rewrite a published snapshot."""
from pathlib import Path
import subprocess
r=Path.cwd()
parent=subprocess.check_output(['git','rev-parse','2eae6260'],text=True).strip()
for stem in ['audit-publication','freeze','release']:
    s=(r/f'docs/publication115/{stem}115.py').read_text(encoding='utf8').replace('115','116')
    if stem=='freeze':
        s=s.replace('56d4423a4c206bc6cb083b65985624023756f7f7',parent).replace("prior['latest']==114","prior['latest']==115").replace("'2026-11-10'","'2026-11-11'").replace('Durable chat construction, playtesting and checked references','Measured landing control and portable reference reconstruction')
    if stem=='release':
        s=s.replace('123-durable-level-tasks','124-measured-game-control').replace('durable chat, repairs and reference reconstruction','measured control, shader portability and fresh reference images').replace('durable chat construction, verified playtests, bounded repairs and reference-image reconstruction','measured input-only landing control and fresh reference reconstruction')
    (r/f'docs/publication116/{stem}116.py').write_bytes(s.encode('utf8'))
s=(r/'docs/publication115/visual115.cjs').read_text(encoding='utf8').replace('115','116').replace("selectOption('116')","selectOption('117')").replace('/fresh images/i','/power.up/i')
(r/'docs/publication116/visual116.cjs').write_bytes(s.encode('utf8'))

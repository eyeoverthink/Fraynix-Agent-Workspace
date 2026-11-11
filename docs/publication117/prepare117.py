from pathlib import Path
import subprocess
r=Path.cwd();parent=subprocess.check_output(['git','rev-parse','e015b692'],text=True).strip()
for stem in ['audit-publication','freeze','release','ci']:
    s=(r/f'docs/publication116/{stem}116.py').read_text(encoding='utf8').replace('116','117')
    if stem=='freeze':s=s.replace('2eae6260488d243ba8d1de825c1a3e751c1a7d22',parent).replace("prior['latest']==115","prior['latest']==116").replace('Measured landing control and portable reference reconstruction','Wider-gap completion and measured power-up collection')
    if stem=='release':s=s.replace('124-measured-game-control','125-safe-game-pursuit').replace('measured control, shader portability and fresh reference images','wider-gap control, measured pickups and retained verification').replace('measured input-only landing control and fresh reference reconstruction','input-only gap completion and measured safe collection')
    (r/f'docs/publication117/{stem}117.py').write_bytes(s.encode())
s=(r/'docs/publication116/visual116.cjs').read_text(encoding='utf8').replace('116','117').replace("selectOption('117')","selectOption('118')").replace('/power.up/i','/obstacle/i')
(r/'docs/publication117/visual117.cjs').write_bytes(s.encode())

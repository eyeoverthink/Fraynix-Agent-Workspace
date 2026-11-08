from pathlib import Path
r=Path.cwd();old=Path('D:/Codex-Work/fraynix-workspace-edition092/build')
for name in ['freeze','audit-publication','release','archive-visual','push-receipt']:
 s=(old/f'{name}092.py').read_text(encoding='utf8').replace('092','093').replace('087266c2c764a25b313739a1a53c02e310f5f52c','726ac0c30a2b9815c9ec91f681f2eab6460fadfb')
 for a,b in [("prior['latest']==91","prior['latest']==92"),("current['latest']==92","current['latest']==93"),("'number':92","'number':93"),("prior['latest']=92","prior['latest']=93"),('Growing banks and restarted tasks','Forecast from recovered state'),('growing banks and restarted tasks','recovered-state forecasts'),('100-growing-bank-restarts','101-recovered-state-forecast')]:s=s.replace(a,b)
 (r/'build'/f'{name}093.py').write_text(s,encoding='utf8')

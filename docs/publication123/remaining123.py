from pathlib import Path
import subprocess
r=Path.cwd()
for args in [
 ['run123.py','reserved123','-Dstudy123.partition=reserved','-Dstudy123.output=build/pickup123-reserved'],
 ['validate123.py','regression'],['validate123.py','full'],['software123.py']]:
 subprocess.run(['python',str(r/'docs/publication123'/args[0]),*args[1:]],check=True)

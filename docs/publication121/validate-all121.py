import subprocess
for group in ['regression','full','reserved']:
 subprocess.run(['python','docs/publication121/validate121.py',group],check=True)

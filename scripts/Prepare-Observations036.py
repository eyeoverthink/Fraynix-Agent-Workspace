"""Fetch the declared research clips using original verified ZIP members.

Original footage/masks stay in the user's private research folder. Java prepares
clean runtime pixels and separate references. Reserved bytes require a committed
candidate source lock. No new application dependency is installed.
"""
import argparse, base64, concurrent.futures, hashlib, json, pathlib, runpy, subprocess

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--split', choices=['development', 'reserved'], required=True)
    parser.add_argument('--output', type=pathlib.Path, required=True)
    args = parser.parse_args()
    root = pathlib.Path(__file__).resolve().parent.parent
    protocol = json.loads((root/'workspace-engine/src/main/resources/observation-036-contract.json').read_text(encoding='utf-8'))
    if args.split == 'reserved':
        lock = root/'docs/OBSERVATION_036_CANDIDATE_LOCK.json'
        subprocess.run(['git','cat-file','-e','HEAD:docs/OBSERVATION_036_CANDIDATE_LOCK.json'], cwd=root, check=True)
        for name, expected in json.loads(lock.read_text(encoding='utf-8'))['sources'].items():
            actual = hashlib.sha256((root/name).read_bytes().replace(b'\r\n', b'\n')).hexdigest()
            if actual != expected:
                raise ValueError('Locked candidate source changed: '+name)
    old = runpy.run_path(str(root/'scripts/Prepare-TrackingResearch.py'))
    entries = old['index']()
    args.output.mkdir(parents=True, exist_ok=True)
    for name in ['README.md','SOURCES.md']:
        data = old['member'](entries, 'DAVIS/'+name)
        target = args.output/('DAVIS-'+name)
        if target.exists() and target.read_bytes() != data:
            raise ValueError('Existing source/license metadata differs; preserve original')
        target.write_bytes(data)
    exceptions = (args.output/'DAVIS-SOURCES.md').read_text(encoding='utf-8')
    for case in protocol['cases']:
        if case['split'] != args.split:
            continue
        sequence = case['sequence']
        if any(line.split()[1] == sequence for line in exceptions.splitlines() if line.startswith('* ')):
            raise ValueError('Declared clip has an external-source license exception: '+sequence)
        names = sorted(n for n in entries if n.startswith('DAVIS/JPEGImages/480p/'+sequence+'/') and n.endswith('.jpg'))[:case['frames']]
        if len(names) != case['frames']:
            raise ValueError('Declared complete footage count is unavailable; do not substitute')
        folder = args.output/sequence
        folder.mkdir(exist_ok=True)
        def fetch(name):
            import binascii
            mask = name.replace('/JPEGImages/', '/Annotations/').replace('.jpg','.png')
            def obtain(source, suffix):
                target = folder/(pathlib.PurePosixPath(name).stem+suffix)
                data = target.read_bytes() if target.exists() else old['member'](entries,source)
                entry = entries[source]
                if len(data) != entry['bytes'] or binascii.crc32(data) != entry['crc']:
                    raise ValueError('Original source member checksum changed')
                if not target.exists():
                    target.write_bytes(data)
                return data
            image, annotation = obtain(name,'.jpg'), obtain(mask,'.png')
            return dict(sourceFrame=int(pathlib.PurePosixPath(name).stem),imageName=name,maskName=mask,imageSha256=hashlib.sha256(image).hexdigest(),maskSha256=hashlib.sha256(annotation).hexdigest(),image=base64.b64encode(image).decode(),mask=base64.b64encode(annotation).decode())
        with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
            frames = list(pool.map(fetch,names))
        pack = dict(schema='fraynix-tracking-sequence-v1',title='DAVIS '+sequence+' · real footage036',sequence=sequence,split=args.split,objectId=case['objectId'],source=old['URL'],license='CC BY-NC4.0; DAVIS authors; research use',notes='Original consecutive source frames. Capture FPS/metric scale unknown. Runtime receives only first rectangle and clean pixels.',frames=frames)
        target = args.output/(sequence+'.ftrack.json')
        encoded = json.dumps(pack,separators=(',',':'))
        if target.exists() and target.read_text(encoding='utf-8') != encoded:
            raise ValueError('Existing immutable research package differs')
        target.write_text(encoded,encoding='utf-8')
        print(json.dumps({'path':str(target.resolve()),'frames':len(frames),'bytes':target.stat().st_size,'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}),flush=True)

if __name__ == '__main__':
    main()

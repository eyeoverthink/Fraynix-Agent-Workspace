"""Presentation-only spacing in the generated guide; frozen inputs remain unchanged."""
from pathlib import Path

root = Path.cwd()
replacements = {
    'completed165': 'completed edition 165',
    'fresh166': 'new edition 166',
    'match165': 'match edition 165',
    '69 edition164': '69 edition 164',
    'frozen001': 'frozen 001',
    'on2026': 'on 2026',
    'about14.6GB': 'about 14.6 GB',
    'at128MiB,16MiB': 'at 128 MiB, 16 MiB',
    'with32MiB': 'with 32 MiB',
    'found311': 'found 311',
}
for name in ['README-166.md', 'README.md', 'docs/readmes/166/index.html',
             'docs/readmes/166/system-map.json', 'docs/readmes/166/attempts.json',
             'docs/obsidian/174-domain-and-archive.md']:
    path = root/name
    text = path.read_text(encoding='utf8')
    for before, after in replacements.items():
        text = text.replace(before, after)
    path.write_text(text, encoding='utf8')

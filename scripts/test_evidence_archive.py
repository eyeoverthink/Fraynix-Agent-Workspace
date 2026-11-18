"""Independent archive boundary tests; no network or private recordings."""
import copy
import json
from pathlib import Path
import tempfile
import unittest
import zipfile
import evidence_archive as archive


class EvidenceArchiveTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.source = self.root / 'source.json'
        self.source.write_text('{"passed":true,"value":0.125}\n', encoding='utf8')
        self.zip = self.root / 'evidence.zip'
        self.manifest = self.root / 'manifest.json'

    def pack(self):
        return archive.pack([
            {'logicalPath': 'run/a.json', 'source': str(self.source)},
            {'logicalPath': 'run/b.json', 'source': str(self.source)},
        ], self.zip, self.manifest)

    def test_exact_restore_and_deduplication(self):
        value = self.pack()
        self.assertEqual(archive.original(value, self.zip, 'run/a.json'), self.source.read_bytes())
        self.assertTrue(archive.verify(value, self.zip)['passed'])
        with zipfile.ZipFile(self.zip) as source:
            self.assertEqual(len(source.namelist()), 2)
        with self.assertRaises(AssertionError):
            self.pack()

    def test_corrupt_archive_and_forged_catalog(self):
        value = self.pack()
        raw = self.zip.read_bytes()
        self.zip.write_bytes(raw[:-1] + bytes([raw[-1] ^ 1]))
        with self.assertRaises(AssertionError):
            archive.verify(value, self.zip)
        self.zip.write_bytes(raw)
        forged = copy.deepcopy(value)
        forged['files'][0]['bytes'] += 1
        with self.assertRaises(AssertionError):
            archive.verify(forged, self.zip)
        with zipfile.ZipFile(self.zip, 'a') as target:
            target.writestr('unexpected.txt', b'not evidence')
        value['archive'].update(bytes=self.zip.stat().st_size, sha256=archive.digest(self.zip.read_bytes()))
        with self.assertRaises(AssertionError):
            archive.verify(value, self.zip)

    def test_private_payload_and_unsafe_names(self):
        for name in ['../escape', '/absolute', 'C:/absolute', 'x\\y', './x']:
            with self.assertRaises(AssertionError):
                archive.logical_name(name)
        self.source.write_text(json.dumps({'image': 'private pixels'}), encoding='utf8')
        with self.assertRaises(AssertionError):
            self.pack()
        self.assertFalse(self.zip.exists())

    def test_duplicate_logical_names(self):
        value = self.pack()
        value['files'][1]['logicalPath'] = value['files'][0]['logicalPath']
        with self.assertRaises(AssertionError):
            archive.verify(value, self.zip)


if __name__ == '__main__':
    unittest.main()

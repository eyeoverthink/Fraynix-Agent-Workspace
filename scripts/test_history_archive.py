import json
import gzip
from pathlib import Path
import subprocess
import tempfile
import unittest
import os

import evidence_archive as archive
import history_archive as history


class HistoryArchiveTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.repo = self.root / 'repo'
        self.repo.mkdir()
        def git(*args):
            return subprocess.check_output(['git', *args], cwd=self.repo, stderr=subprocess.DEVNULL)
        git('init', '-b', 'main')
        git('config', 'user.email', 'fixture@example.invalid')
        git('config', 'user.name', 'Archive test')
        git('config', 'core.autocrlf', 'false')
        (self.repo / 'README-001.md').write_bytes(b'# Original\r\n')
        (self.repo / 'docs/readmes/001').mkdir(parents=True)
        (self.repo / 'docs/readmes/001/picture.png').write_bytes(b'fixture-binary\x00\xff')
        edition = {'number': 1, 'markdown': 'README-001.md',
                   'markdownSha256': history.digest(b'# Original\n'), 'snapshotFiles': [
                       {'path': 'docs/readmes/001/picture.png', 'kind': 'binary',
                        'sha256': history.digest(b'fixture-binary\x00\xff')}]}
        (self.repo / 'docs/readmes/editions.json').write_text(json.dumps({'editions': [edition]}))
        git('add', '.')
        git('-c', 'core.autocrlf=false', 'commit', '-m', 'fixture')
        self.commit = git('rev-parse', 'HEAD').decode().strip()
        with history.GitEvidence(self.repo, self.commit) as source:
            self.catalog = history.catalog(source)
        self.names = [row['path'] for row in self.catalog['files']]
        self.zip = self.root / 'evidence.zip'
        self.manifest = archive.pack(
            [{'logicalPath': n, 'source': self.repo/n} for n in self.names],
            self.zip, self.root/'manifest.json')

    def test_git_and_archive_restore_exact_originals(self):
        with history.GitEvidence(self.repo, self.commit) as source:
            history.restore(self.catalog, self.names, self.root/'git-copy', git=source)
        history.restore(self.catalog, self.names, self.root/'zip-copy',
                        archive=self.zip, manifest=self.manifest)
        for name in self.names:
            self.assertEqual((self.root/'git-copy'/name).read_bytes(),
                             (self.root/'zip-copy'/name).read_bytes())

    def test_corrupt_and_missing_archives_do_not_fall_back(self):
        raw = self.zip.read_bytes()
        self.zip.write_bytes(raw[:-1] + bytes([raw[-1] ^ 1]))
        with self.assertRaises(AssertionError):
            history.restore(self.catalog, self.names, self.root/'bad',
                            archive=self.zip, manifest=self.manifest)
        self.assertFalse((self.root/'bad').exists())
        with self.assertRaises(FileNotFoundError):
            history.restore(self.catalog, self.names, self.root/'missing',
                            archive=self.root/'absent.zip', manifest=self.manifest)

    def test_catalog_and_frozen_identity_are_checked(self):
        path = self.root/'catalog.json'
        path.write_text(json.dumps(self.catalog))
        checksum = history.digest(path.read_bytes())
        self.assertEqual(history.load_catalog(path, checksum), self.catalog)
        compressed = self.root/'catalog.json.gz'
        compressed.write_bytes(gzip.compress(path.read_bytes(), mtime=0))
        self.assertEqual(history.load_catalog(compressed, history.digest(compressed.read_bytes())), self.catalog)
        path.write_text(path.read_text()+' ')
        with self.assertRaises(ValueError):
            history.load_catalog(path, checksum)
        self.catalog['files'][0]['canonicalSha256'] = '0'*64
        with self.assertRaises(ValueError):
            history.restore(self.catalog, self.names, self.root/'wrong',
                            archive=self.zip, manifest=self.manifest)
        self.assertFalse((self.root/'wrong').exists())

    def test_no_traversal_overwrite_or_mixed_provider(self):
        with self.assertRaises(AssertionError):
            history.restore(self.catalog, ['../outside'], self.root/'escape',
                            archive=self.zip, manifest=self.manifest)
        with self.assertRaises(ValueError):
            history.restore(self.catalog, self.names, self.repo,
                            archive=self.zip, manifest=self.manifest)
        with history.GitEvidence(self.repo, self.commit) as source:
            with self.assertRaises(ValueError):
                history.restore(self.catalog, self.names, self.root/'mixed', git=source,
                                archive=self.zip, manifest=self.manifest)

    def test_working_tree_changes_cannot_redefine_originals(self):
        (self.repo/'README-001.md').write_text('changed')
        with history.GitEvidence(self.repo, self.commit) as source:
            self.assertEqual(history.catalog(source), self.catalog)
            history.restore(self.catalog, self.names, self.root/'original', git=source)
        self.assertEqual((self.root/'original/README-001.md').read_bytes(), b'# Original\r\n')

    def test_linked_parent_is_pinned_but_destination_link_is_refused(self):
        physical = self.root / 'physical'
        physical.mkdir()
        alias = self.root / 'alias'
        if os.name == 'nt':
            subprocess.run(['powershell', '-NoProfile', '-Command',
                            'New-Item -ItemType Junction -Path $env:ARCHIVE_TEST_ALIAS -Target $env:ARCHIVE_TEST_TARGET | Out-Null'],
                           env=dict(os.environ, ARCHIVE_TEST_ALIAS=str(alias),
                                    ARCHIVE_TEST_TARGET=str(physical)), check=True, capture_output=True)
        else:
            alias.symlink_to(physical, target_is_directory=True)
        try:
            result = history.restore(self.catalog, self.names, alias/'restored',
                                     archive=self.zip, manifest=self.manifest)
            self.assertTrue(result['passed'])
            self.assertEqual((physical/'restored/README-001.md').read_bytes(), b'# Original\r\n')
            with self.assertRaises(ValueError):
                history.restore(self.catalog, self.names, alias,
                                archive=self.zip, manifest=self.manifest)
        finally:
            if os.name == 'nt':
                os.rmdir(alias)  # Remove only the junction, never its target tree.
            else:
                alias.unlink()


if __name__ == '__main__':
    unittest.main()

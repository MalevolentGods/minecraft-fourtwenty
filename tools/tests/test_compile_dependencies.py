import hashlib
import importlib.util
import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

spec = importlib.util.spec_from_file_location(
    'compile_dependencies', Path(__file__).parents[1] / 'download_compile_dependencies.py')
downloader = importlib.util.module_from_spec(spec)
spec.loader.exec_module(downloader)


class CompileDependencyTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.destination = Path(self.temporary.name) / 'dependencies'
        self.artifacts = [
            {'file': name, 'sha256': hashlib.sha256(payload).hexdigest()}
            for name, payload in [('first.jar', b'first'), ('second.jar', b'second')]
        ]
        self.urls = {entry['file']: 'https://example.invalid/' + entry['file']
                     for entry in self.artifacts}

    def test_verified_downloads_preserve_unrelated_files(self):
        self.destination.mkdir()
        (self.destination / 'fixture.jar').write_bytes(b'fixture')
        with patch.object(downloader.urllib.request, 'urlopen',
                          side_effect=[io.BytesIO(b'first'), io.BytesIO(b'second')]):
            downloader.download_dependencies(self.artifacts, self.urls, self.destination)
        self.assertEqual((self.destination / 'first.jar').read_bytes(), b'first')
        self.assertEqual((self.destination / 'second.jar').read_bytes(), b'second')
        self.assertEqual((self.destination / 'fixture.jar').read_bytes(), b'fixture')

    def test_later_checksum_failure_preserves_all_existing_inputs(self):
        self.destination.mkdir()
        (self.destination / 'first.jar').write_bytes(b'existing')
        with patch.object(downloader.urllib.request, 'urlopen',
                          side_effect=[io.BytesIO(b'first'), io.BytesIO(b'wrong')]):
            with self.assertRaisesRegex(ValueError, 'Checksum mismatch for second.jar'):
                downloader.download_dependencies(self.artifacts, self.urls, self.destination)
        self.assertEqual((self.destination / 'first.jar').read_bytes(), b'existing')
        self.assertFalse((self.destination / 'second.jar').exists())

    def test_network_failure_does_not_create_destination(self):
        with patch.object(downloader.urllib.request, 'urlopen', side_effect=OSError('offline')):
            with self.assertRaisesRegex(OSError, 'offline'):
                downloader.download_dependencies(self.artifacts, self.urls, self.destination)
        self.assertFalse(self.destination.exists())

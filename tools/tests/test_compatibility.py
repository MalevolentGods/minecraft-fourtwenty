import importlib.util
import json
import tempfile
import unittest
import zipfile
from pathlib import Path

spec = importlib.util.spec_from_file_location('compatibility', Path(__file__).parents[1] / 'compatibility.py')
compat = importlib.util.module_from_spec(spec)
spec.loader.exec_module(compat)


class PreparationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.mods = self.root / 'reference/mods'
        self.mods.mkdir(parents=True)
        self.original_root, self.original_lock = compat.ROOT, compat.LOCK
        compat.ROOT, compat.LOCK = self.root, self.root / 'lock.json'
        self.jar('library.jar', 'library')
        self.lock()

    def tearDown(self):
        compat.ROOT, compat.LOCK = self.original_root, self.original_lock
        self.temp.cleanup()

    def jar(self, name, mod_id):
        with zipfile.ZipFile(self.mods / name, 'w') as z:
            z.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nImplementation-Version: 1.\n 2.3\n')
            z.writestr('META-INF/neoforge.mods.toml', f'modLoader="javafml"\nloaderVersion="[4,)"\n[[mods]]\nmodId="{mod_id}"\nversion="${{file.jarVersion}}"\n')

    def lock(self):
        compat.LOCK.write_text(json.dumps({'minecraft': '1.21.1', 'neoforge': '21.1.218', 'java': 21, 'artifacts': compat.inventory(self.mods), 'core': [], 'sources': {}}))

    def test_manifest_version_is_unfolded(self):
        self.assertEqual(compat.inventory(self.mods)[0]['mods'][0]['version'], '1.2.3')

    def test_changed_checksum_rejected_before_copy(self):
        with zipfile.ZipFile(self.mods / 'library.jar', 'a') as z: z.writestr('changed', 'yes')
        destination = self.root / 'run'
        with self.assertRaisesRegex(ValueError, 'inventory changed'):
            compat.prepare(self.mods, destination, 'standalone', None, None, False)
        self.assertFalse(destination.exists())

    def test_duplicate_mod_id_rejected(self):
        self.jar('duplicate.jar', 'library')
        with self.assertRaisesRegex(ValueError, 'Duplicate mod ID'): compat.inventory(self.mods)

    def test_existing_destination_never_merged(self):
        destination = self.root / 'run'
        destination.mkdir()
        marker = destination / 'keep'; marker.write_text('existing')
        with self.assertRaisesRegex(ValueError, 'already exists'):
            compat.prepare(self.mods, destination, 'standalone', None, None, False)
        self.assertEqual(marker.read_text(), 'existing')

    def test_launch_rejects_stale_jar(self):
        destination = self.root / 'run'
        compat.prepare(self.mods, destination, 'standalone', None, None, False)
        compat.verify_run(destination, 'standalone', False)
        import shutil
        shutil.copy2(self.mods / 'library.jar', destination / 'mods/library.jar')
        with self.assertRaisesRegex(ValueError, 'inventory changed'):
            compat.verify_run(destination, 'standalone', False)

    def test_launch_mode_must_match(self):
        destination = self.root / 'run'
        compat.prepare(self.mods, destination, 'standalone', None, None, False)
        with self.assertRaisesRegex(ValueError, 'mode differs'):
            compat.verify_run(destination, 'standalone', True)


if __name__ == '__main__': unittest.main()

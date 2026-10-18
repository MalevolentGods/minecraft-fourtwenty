import copy
import gzip
import importlib.util
import unittest
from pathlib import Path
spec = importlib.util.spec_from_file_location('dispensary', Path(__file__).parents[1] / 'import_dispensary_blueprints.py')
g = importlib.util.module_from_spec(spec)
spec.loader.exec_module(g)


class BlueprintTests(unittest.TestCase):
    """The bundled Dispensary (imported from the Sensi Heights style pack) is a valid, upgradable hut."""

    def test_every_bundled_level_is_valid(self):
        for level in range(1, 6):
            with self.subTest(level=level):
                root = g.bundled(level)
                self.assertEqual(g.validate(root, level), [])
                self.assertEqual(g.size(root), (11, 16, 24), 'levels share one footprint so upgrades line up')
                self.assertTrue(any(n == 'minecolonies:blockminecoloniesrack' or 'rack' in n for n in g.names(root)))

    def test_bundled_files_round_trip_exactly(self):
        for level in range(1, 6):
            with self.subTest(level=level):
                # Compare the NBT payload: gzip output differs between zlib builds (e.g. Windows vs Linux CI).
                raw = gzip.decompress((g.BUNDLED / f'dispensary{level}.blueprint').read_bytes())
                self.assertEqual(gzip.decompress(g.dump(g.load(g.BUNDLED / f'dispensary{level}.blueprint'))), raw)

    def test_conversion_replaces_optional_mod_blocks_and_repoints_the_hut(self):
        root = g.bundled(5)
        root['palette'][1][1].append({'Name': (g.STRING, 'projectred_illumination:white_inverted_illumar_lamp')})
        hut = g.hut_tile(root)['blueprintDataProvider'][1]
        hut['pack'] = (g.STRING, 'Sensi Heights')
        converted = g.convert(root, 5)
        self.assertNotIn('projectred_illumination:white_inverted_illumar_lamp', g.names(converted))
        self.assertEqual(g.hut_tile(converted)['blueprintDataProvider'][1]['pack'][1], 'FourTwenty')
        self.assertNotIn('projectred_illumination', converted['required_mods'][1][1])

    def test_validation_catches_broken_designs(self):
        root = g.bundled(1)
        sx, sy, sz = g.size(root)
        offset = root['optional_data'][1]['structurize'][1]['primary_offset'][1]
        ax, ay, az = offset['x'][1], offset['y'][1], offset['z'][1]
        names = g.names(root)

        def with_block(x, y, z, name):
            broken = copy.deepcopy(root)
            palette = broken['palette'][1][1]
            palette.append({'Name': (g.STRING, name)})
            blocks = g.unpack(broken)
            blocks[(y * sz + z) * sx + x] = len(palette) - 1
            broken['blocks'] = (g.INTS, g.pack(blocks))
            return broken

        self.assertTrue(any('above the hut' in p for p in g.validate(with_block(ax, ay + 1, az, 'minecraft:stone'), 1)))
        self.assertTrue(any('optional mods' in p for p in g.validate(with_block(0, 0, 0, 'cb_multipart:multipart'), 1)))
        self.assertTrue(any('counter' in p for p in g.validate(with_block(ax, ay, az - 1, 'minecraft:stone'), 1)))
        self.assertIn('fourtwenty:dispensary', names)


if __name__ == '__main__':
    unittest.main()

import gzip
import importlib.util
import unittest
from pathlib import Path
spec = importlib.util.spec_from_file_location('dispensary', Path(__file__).parents[1] / 'generate_dispensary.py')
g = importlib.util.module_from_spec(spec)
spec.loader.exec_module(g)


class BlueprintTests(unittest.TestCase):
    def test_progression_and_committed_payloads(self):
        for level, footprint in enumerate(g.FOOTPRINTS,1):
            with self.subTest(level=level):
                root,blocks,markers,approaches=g.design(level)
                self.assertEqual((root['size_x'][1],root['size_z'][1]),footprint)
                self.assertEqual(sum(tag=='sit_in' for _,tag in markers),level*2)
                # Gzip headers/compression can vary by runtime; compare the exact NBT payload.
                committed = (g.PACK/f'dispensary/dispensary{level}.blueprint').read_bytes()
                self.assertEqual(gzip.decompress(g.encoded(level)),gzip.decompress(committed))
                self.assertEqual(g.encoded(level),g.encoded(level))
                self.assertTrue(any(s['Name'][1]=='minecolonies:blockminecoloniesrack' for s in blocks.values()))
                self.assertEqual(blocks[footprint[0]//2,1,3]['Name'][1],'fourtwenty:dispensary')
                self.assertNotIn((footprint[0]//2,1,1),blocks)

    def test_blocked_route_and_missing_roof_are_rejected(self):
        root,blocks,markers,approaches=g.design(1)
        size=tuple(root['size_'+axis][1] for axis in 'xyz')
        blocked=dict(blocks);blocked[4,1,1]={'Name':g.s('minecraft:stone')}
        with self.assertRaisesRegex(AssertionError,'Blocked'):g.validate(size,(4,1,3),blocked,markers,approaches)
        exposed=dict(blocks)
        for y in range(3,size[1]):exposed.pop((6,y,5),None)
        with self.assertRaisesRegex(AssertionError,'rain'):g.validate(size,(4,1,3),exposed,markers,approaches)

"""The Python markup reader agrees with the samples (and so with the game's reader, which checks the same files)."""
import json
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from handbook_markup import parse  # noqa: E402

SAMPLES = ROOT / 'src/test/resources/handbook/markup-samples'


class MarkupSamples(unittest.TestCase):
    def test_samples(self):
        mds = sorted(SAMPLES.glob('*.md'))
        self.assertTrue(mds)
        for md in mds:
            with self.subTest(md.name):
                front, elements, errors = parse(md.read_text(encoding='utf-8'))
                expected = json.loads(md.with_suffix('.expected.json').read_text(encoding='utf-8'))
                self.assertEqual(expected['front'], front)
                self.assertEqual(expected['elements'], elements)
                self.assertEqual(expected['errors'], errors)


if __name__ == '__main__':
    unittest.main()

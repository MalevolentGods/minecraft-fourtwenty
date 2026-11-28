"""Every hut block model in the mod is free of z-fighting: no two boxes draw a face in the same plane facing the same way."""
import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from hut_model_check import fights

MODELS = Path(__file__).resolve().parents[2] / 'src/main/resources/assets/fourtwenty/models/block'
HUTS = ('dispensary', 'budbarn', 'weedcollege', 'tiedyeshack')          # add every new hut block here (see the generate_<hut>_hut_model.py scripts)


class HutModelTest(unittest.TestCase):
    def test_no_hut_model_z_fights(self):
        for hut in HUTS:
            model = json.loads((MODELS / f'{hut}.json').read_text(encoding='utf-8'))
            elements = [e for child in model['children'].values() for e in child['elements']]
            self.assertEqual([], fights(elements), hut)

    def test_the_checker_sees_a_fight(self):
        a = {'name': 'a', 'from': [0, 0, 0], 'to': [4, 4, 4], 'faces': {'north': {}}}
        b = {'name': 'b', 'from': [2, 2, 0], 'to': [6, 6, 8], 'faces': {'north': {}}}
        self.assertEqual(1, len(fights([a, b])))
        b['faces'] = {'south': {}}
        self.assertEqual([], fights([a, b]))


if __name__ == '__main__':
    unittest.main()

import importlib.util
import json
import unittest
from pathlib import Path
spec = importlib.util.spec_from_file_location('sensi', Path(__file__).parents[1] / 'import_sensi_heights.py')
s = importlib.util.module_from_spec(spec)
spec.loader.exec_module(s)


class SensiHeightsPackTests(unittest.TestCase):
    """The bundled Sensi Heights style pack loads in Structurize and every hut links to its own file."""

    def test_bundled_pack_is_valid(self):
        problems, optional = s.validate()
        self.assertEqual(problems, [])
        self.assertLessEqual(set(optional), s.OPTIONAL)

    def test_pack_metadata(self):
        pack = json.loads((s.BUNDLED / 'pack.json').read_text(encoding='utf-8'))
        self.assertEqual(pack['name'], 'Sensi Heights')
        self.assertIn('fourtwenty', pack['mods'])
        self.assertTrue((s.BUNDLED / pack['icon']).exists())

    def test_core_buildings_are_bundled(self):
        names = {p.relative_to(s.BUNDLED).as_posix() for p in s.blueprints()}
        for level in range(1, 6):
            self.assertIn(f'fundamentals/residence{level}.blueprint', names)
            self.assertIn(f'craftsmanship/luxury/dispensary{level}.blueprint', names)
            self.assertIn(f'decorations/misc/workshop{level}.blueprint', names)
        self.assertIn('decorations/supplies/supplycamp.blueprint', names)


if __name__ == '__main__':
    unittest.main()

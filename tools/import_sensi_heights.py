#!/usr/bin/env python3
"""Bundle the Sensi Heights style pack into FourTwenty.

The Sensi Heights pack (a MineColonies style built for FourTwenty) is designed in its own repo and
copied into the mod at src/main/resources/blueprints/fourtwenty/sensiheights/, where Structurize
loads it from the jar like the FourTwenty Dispensary pack.

Usage:  python3 tools/import_sensi_heights.py <Sensi Heights repo>/sensiheights

Copies pack.json, icons and every blueprint (except EXCLUDE), then validates the bundled copy (see
validate()).
"""
import shutil
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import import_dispensary_blueprints as bp   # NBT helpers

ROOT = Path(__file__).resolve().parents[1]
BUNDLED = ROOT / 'src/main/resources/blueprints/fourtwenty/sensiheights'
FOREIGN = ('projectred', 'cb_multipart', 'cb_microblock')
# Blocks from mods FourTwenty doesn't depend on that the pack may use: missing when that mod isn't
# installed (the block is just left out), so they're reported but allowed.
OPTIONAL = {'farmersdelight', 'handcrafted'}
# The supply camp is Workshop level 1: its controller deliberately links to workshop1 so it upgrades.
LINK_EXCEPTIONS = {'decorations/supplies/supplycamp.blueprint': 'decorations/misc/workshop1.blueprint'}
# Blueprints the pack has that the public mod leaves out (third-party characters).
EXCLUDE = {'decorations/signs/gokuleaf.blueprint'}


def blueprints(root=BUNDLED):
    return sorted(p for p in root.rglob('*.blueprint'))


def validate(root=BUNDLED):
    """Problems with the bundled pack (empty when it's fine), and the optional mods it uses."""
    problems, optional = [], {}
    if not (root / 'pack.json').exists():
        problems.append('no pack.json')
    for path in blueprints(root):
        rel = path.relative_to(root).as_posix()
        data = bp.load(path)
        mods = {n.split(':')[0] for n in bp.names(data)}
        foreign = sorted(m for m in mods if m.startswith(FOREIGN))
        if foreign:
            problems.append(f'{rel}: uses {foreign}')
        foreign_tiles = sorted({te['id'][1] for te in data['tile_entities'][1][1]
                                if te.get('id', (0, ''))[1].startswith(FOREIGN)})
        if foreign_tiles:
            problems.append(f'{rel}: tile entities from {foreign_tiles}')
        for m in sorted(mods & OPTIONAL):
            optional.setdefault(m, []).append(rel)
        # A hut or decoration controller must link to the file it's saved as, or upgrades break.
        for te in data['tile_entities'][1][1]:
            provider = te.get('blueprintDataProvider')
            if not provider or 'path' not in provider[1]:
                continue
            linked = provider[1]['path'][1]
            pack = provider[1].get('pack', (0, ''))[1]
            if not linked:
                continue   # a tag anchor: tags only, no link
            if LINK_EXCEPTIONS.get(rel) == linked and pack == 'Sensi Heights':
                continue
            # A hut nested in another stands unbuilt at level 0: no such file, it builds its level 1 beside the parent.
            if (pack == 'Sensi Heights' and linked.rsplit('/', 1)[0] == rel.rsplit('/', 1)[0] and linked.endswith('0.blueprint')
                    and (root / linked.replace('0.blueprint', '1.blueprint')).exists()):
                continue
            is_hut = te.get('id', (0, ''))[1] != 'minecolonies:decorationcontroller'
            if pack != 'Sensi Heights' or linked != rel:
                problems.append(f'{rel}: linked to {pack!r} / {linked!r}')
            # Huts must sit directly in a category folder (category/name.blueprint), or the build tool hides them.
            if is_hut and rel.count('/') > 2:
                problems.append(f'{rel}: hut nested too deep for the build tool')
    return problems, optional


def main(source):
    source = Path(source)
    assert (source / 'pack.json').exists(), f'{source} is not a style pack (no pack.json)'
    problems, optional = validate(source)
    if problems:
        sys.exit('not importing:\n  ' + '\n  '.join(problems))
    if BUNDLED.exists():
        shutil.rmtree(BUNDLED)
    shutil.copytree(source, BUNDLED, ignore=shutil.ignore_patterns('*.py', '__pycache__', '.gitkeep'))
    for rel in EXCLUDE:
        (BUNDLED / rel).unlink(missing_ok=True)
    for folder in sorted((p for p in BUNDLED.rglob('*') if p.is_dir()), reverse=True):
        if not any(folder.iterdir()):
            folder.rmdir()                      # e.g. a category left empty by EXCLUDE
    problems, optional = validate()
    assert not problems, problems
    print(f'imported {len(blueprints())} blueprints into {BUNDLED.relative_to(ROOT).as_posix()}')
    for mod, files in optional.items():
        print(f'  uses optional {mod} in: {", ".join(files)}')


if __name__ == '__main__':
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    main(sys.argv[1])

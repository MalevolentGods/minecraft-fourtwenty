#!/usr/bin/env python3
"""RETIRED as an importer: the FourTwenty pack is gone and the Dispensary ships in Sensi Heights. Kept for the NBT helpers import_sensi_heights.py uses.

Bundle the Sensi Heights Dispensary (levels 1-5) as FourTwenty's default hut blueprints.

The Sensi Heights style pack (the Malevolent Colonies modpack) designs the Dispensary; this copies
its five levels into src/main/resources/blueprints/fourtwenty/dispensary/dispensary/ for the mod:

- the hut block's pack/path become FourTwenty's (``FourTwenty`` / ``dispensary/dispensaryN.blueprint``)
  so upgrades find the next level inside the mod;
- blocks from mods FourTwenty doesn't depend on become FourTwenty's own equivalents: ProjectRed
  illumar lamps become FourTwenty lamps of the same colour and type (inverted stays inverted, lit
  state kept), and CB Multipart lamp covers become that colour's Lamp Trim on the same face;
- ``required_mods`` is recomputed from what's left.

Usage:  python3 tools/import_dispensary_blueprints.py <Sensi Heights pack dir>/craftsmanship/luxury
Then:   python3 -m unittest discover -s tools/tests   (validates the bundled levels)

        python3 tools/import_dispensary_blueprints.py --lamps-only FILE.blueprint...
        rewrites any blueprints in place with only the lamp/cover replacement (e.g. a style pack).
"""
import re
import gzip
import io
import struct
import sys
from collections import deque
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BUNDLED = ROOT / 'src/main/resources/blueprints/fourtwenty/sensiheights/craftsmanship/luxury'
PACK, HUT = 'Sensi Heights', 'fourtwenty:dispensary'
# Mods every FourTwenty Dispensary may rely on: MineColonies and its own dependencies.
ALLOWED = {'minecraft', 'minecolonies', 'structurize', 'domum_ornamentum', 'fourtwenty'}
ILLUMAR = re.compile(r'projectred_illumination:(?P<color>[a-z_]+?)_(?P<inverted>inverted_)?illumar_lamp')
# CB Multipart cover sides, by the low nibble of a face part's "shape".
CB_SIDES = ('down', 'up', 'north', 'south', 'west', 'east')
PASSABLE = ('_door', 'fence_gate', 'carpet', 'pressure_plate', 'button', 'torch', '_sign', 'rail', 'flower', 'vine', 'lever', 'snow', 'tripwire', 'redstone_wire')
END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTES, STRING, LIST, COMPOUND, INTS, LONGS = range(13)


# --- Minimal typed NBT (values are (type, value) pairs, so files round-trip exactly) -----------
def _read(s, t):
    if t == BYTE: return struct.unpack('>b', s.read(1))[0]
    if t == SHORT: return struct.unpack('>h', s.read(2))[0]
    if t == INT: return struct.unpack('>i', s.read(4))[0]
    if t == LONG: return struct.unpack('>q', s.read(8))[0]
    if t == FLOAT: return struct.unpack('>f', s.read(4))[0]
    if t == DOUBLE: return struct.unpack('>d', s.read(8))[0]
    if t == BYTES: return s.read(struct.unpack('>i', s.read(4))[0])
    if t == STRING: return s.read(struct.unpack('>H', s.read(2))[0]).decode('utf-8')
    if t == LIST:
        kind, n = s.read(1)[0], struct.unpack('>i', s.read(4))[0]
        return kind, [_read(s, kind) for _ in range(n)]
    if t == COMPOUND:
        out = {}
        while (kind := s.read(1)[0]) != END:
            name = s.read(struct.unpack('>H', s.read(2))[0]).decode('utf-8')
            out[name] = (kind, _read(s, kind))
        return out
    if t == INTS: return list(struct.unpack(f'>{(n := struct.unpack(">i", s.read(4))[0])}i', s.read(4 * n)))
    if t == LONGS: return list(struct.unpack(f'>{(n := struct.unpack(">i", s.read(4))[0])}q', s.read(8 * n)))
    raise ValueError(f'NBT type {t}')


def _write(s, t, v):
    if t == BYTE: s.write(struct.pack('>b', v))
    elif t == SHORT: s.write(struct.pack('>h', v))
    elif t == INT: s.write(struct.pack('>i', v))
    elif t == LONG: s.write(struct.pack('>q', v))
    elif t == FLOAT: s.write(struct.pack('>f', v))
    elif t == DOUBLE: s.write(struct.pack('>d', v))
    elif t == BYTES: s.write(struct.pack('>i', len(v)) + v)
    elif t == STRING: b = v.encode('utf-8'); s.write(struct.pack('>H', len(b)) + b)
    elif t == LIST:
        kind, items = v
        s.write(bytes([kind if items else (kind or END)]) + struct.pack('>i', len(items)))
        for item in items: _write(s, kind, item)
    elif t == COMPOUND:
        for name, (kind, value) in v.items():
            b = name.encode('utf-8'); s.write(bytes([kind]) + struct.pack('>H', len(b)) + b); _write(s, kind, value)
        s.write(bytes([END]))
    elif t == INTS: s.write(struct.pack(f'>i{len(v)}i', len(v), *v))
    elif t == LONGS: s.write(struct.pack(f'>i{len(v)}q', len(v), *v))
    else: raise ValueError(f'NBT type {t}')


def load(path):
    s = io.BytesIO(gzip.decompress(Path(path).read_bytes()))
    assert s.read(1)[0] == COMPOUND
    s.read(struct.unpack('>H', s.read(2))[0])
    return _read(s, COMPOUND)


def dump(root):
    s = io.BytesIO(); s.write(bytes([COMPOUND]) + struct.pack('>H', 0)); _write(s, COMPOUND, root)
    return gzip.compress(s.getvalue(), mtime=0)


# --- Blueprint helpers --------------------------------------------------------------------------
def size(root): return root['size_x'][1], root['size_y'][1], root['size_z'][1]


def unpack(root):
    """Palette index per position, in Structurize's y, z, x order (two shorts per int)."""
    sx, sy, sz = size(root)
    out = []
    for v in root['blocks'][1]:
        v &= 0xffffffff
        out += [v >> 16, v & 0xffff]
    return out[:sx * sy * sz]


def pack(indices):
    if len(indices) % 2: indices = indices + [0]
    ints = []
    for hi, lo in zip(indices[0::2], indices[1::2]):
        v = (hi << 16) | lo
        ints.append(v - (1 << 32) if v >= 1 << 31 else v)
    return ints


def names(root): return [entry['Name'][1] for entry in root['palette'][1][1]]


def hut_tile(root):
    for te in root['tile_entities'][1][1]:
        if te.get('id', (0, ''))[1] == HUT: return te
    return None


def lamp_for(name):
    """FourTwenty's block for a ProjectRed illumar lamp id, or None."""
    match = ILLUMAR.fullmatch(name)
    if not match: return None
    return f"fourtwenty:{match['color']}_{'inverted_' if match['inverted'] else ''}lamp"


def trim_for(material):
    """FourTwenty's lamp trim for a CB cover material such as 'projectred_illumination:lime_inverted_illumar_lamp//lit.true'."""
    match = ILLUMAR.match(material or '')
    return f"fourtwenty:{match['color']}_lamp_trim" if match else 'fourtwenty:lime_lamp_trim'


def replace_lighting(root):
    """Swap ProjectRed lamps and CB lamp covers for FourTwenty's lamps and lamp trims (in place)."""
    sx, sy, sz = size(root)
    palette = root['palette'][1][1]
    blocks = unpack(root)
    # The face and material of each CB cover, by position (from its saved parts).
    covers = {}
    for te in root['tile_entities'][1][1]:
        if te.get('id', (0, ''))[1].startswith('cb_multipart:'):
            parts = te.get('parts', (LIST, (COMPOUND, [])))[1][1]
            faces = [part for part in parts if part.get('id', (0, ''))[1] == 'cb_microblock:face']
            side = CB_SIDES[faces[0]['shape'][1] & 0x0F] if faces else 'down'
            material = faces[0].get('material', (STRING, ''))[1] if faces else ''
            covers[(te['x'][1], te['y'][1], te['z'][1])] = (side, trim_for(material))
    new_palette, seen = [], {}

    def index(entry):
        key = repr(sorted((k, v) for k, v in entry.items()))
        if key not in seen:
            seen[key] = len(new_palette)
            new_palette.append(entry)
        return seen[key]

    remap = {}
    for i, entry in enumerate(palette):
        lamp = lamp_for(entry['Name'][1])
        remap[i] = index(dict(entry, Name=(STRING, lamp)) if lamp else entry)   # keeps the lit property
    converted = []
    for i, b in enumerate(blocks):
        if palette[b]['Name'][1] == 'cb_multipart:multipart':
            y, z, x = i // (sx * sz), (i % (sx * sz)) // sx, i % sx
            side, trim = covers.get((x, y, z), ('down', 'fourtwenty:lime_lamp_trim'))
            converted.append(index({'Name': (STRING, trim), 'Properties': (COMPOUND, {'facing': (STRING, side)})}))
        else:
            converted.append(remap[b])
    # Keep only palette entries still in use (the replaced ones would still list their mods).
    used = sorted(set(converted))
    compact = {old: new for new, old in enumerate(used)}
    root['palette'] = (LIST, (COMPOUND, [new_palette[i] for i in used]))
    root['blocks'] = (INTS, pack([compact[i] for i in converted]))
    # Drop tile data of substituted blocks (CB Multipart's saved parts); lamps keep theirs under
    # FourTwenty's id, or Structurize would load a ProjectRed tile entity into a FourTwenty lamp.
    kept = [te for te in root['tile_entities'][1][1] if not te.get('id', (0, ''))[1].startswith('cb_multipart:')]
    for te in kept:
        if lamp_for(te.get('id', (0, ''))[1]):
            te['id'] = (STRING, 'fourtwenty:lamp')
    root['tile_entities'] = (LIST, (COMPOUND, kept))
    mods = sorted({n.split(':')[0] for n in names(root)} - {'minecraft'})
    root['required_mods'] = (LIST, (STRING, mods))
    return root


def convert(root, level):
    """Rewrite one Sensi Heights level into FourTwenty's bundled form (in place)."""
    replace_lighting(root)
    provider = hut_tile(root)['blueprintDataProvider'][1]
    provider['pack'] = (STRING, PACK)
    provider['path'] = (STRING, f'craftsmanship/luxury/dispensary{level}.blueprint')
    provider['schematicName'] = (STRING, f'dispensary{level}')
    root['name'] = (STRING, f'dispensary{level}')
    mods = sorted({n.split(':')[0] for n in names(root)} - {'minecraft'})
    root['required_mods'] = (LIST, (STRING, mods))
    return root


# --- Validation (mirrors DispensaryPresentationGameTests, allowing stairs and upper floors) -----
def validate(root, level):
    """Return a list of problems with a bundled level (empty when it is fine)."""
    problems = []
    sx, sy, sz = size(root)
    pal = names(root)
    blocks = unpack(root)

    def at(x, y, z):
        if not (0 <= x < sx and 0 <= y < sy and 0 <= z < sz): return 'minecraft:air'
        return pal[blocks[(y * sz + z) * sx + x]]

    def air(x, y, z):
        name = at(x, y, z)
        # Things you walk through: carpets, plates, buttons, torches, signs, rails, flowers, vines.
        return name in ('minecraft:air', 'structurize:blocksubstitution') or any(
            part in name for part in PASSABLE)

    def solid(x, y, z): return not air(x, y, z) and at(x, y, z) != 'structurize:blocksolidsubstitution'

    offset = root['optional_data'][1]['structurize'][1]['primary_offset'][1]
    anchor = offset['x'][1], offset['y'][1], offset['z'][1]
    huts = [i for i, b in enumerate(blocks) if pal[b] == HUT]
    if len(huts) != 1: problems.append(f'{len(huts)} hut blocks')
    if at(*anchor) != HUT: problems.append('anchor is not the hut')
    if not air(anchor[0], anchor[1] + 1, anchor[2]): problems.append('no air above the hut (its model is two blocks tall)')
    foreign = sorted({n.split(':')[0] for n in pal} - ALLOWED)
    if foreign: problems.append(f'blocks from optional mods: {foreign}')
    provider = hut_tile(root)['blueprintDataProvider'][1]
    if provider['pack'][1] != PACK or provider['path'][1] != f'craftsmanship/luxury/dispensary{level}.blueprint':
        problems.append(f"hut points at {provider['pack'][1]}/{provider['path'][1]}")

    # Walkable: two blocks of air over something to stand on; steps of one block up or down.
    def walkable(x, y, z): return air(x, y, z) and air(x, y + 1, z) and solid(x, y - 1, z)
    entrance = (anchor[0], anchor[1], anchor[2] - 3)
    reach, queue = {entrance}, deque([entrance])
    while queue:
        x, y, z = queue.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1):
                n = (x + dx, y + dy, z + dz)
                if n not in reach and walkable(*n) and (dy != 1 or air(x, y + 2, z)):
                    reach.add(n); queue.append(n)
                    break

    def beside(x, y, z):
        # Citizens sit, work and use racks from up to two blocks away (seats behind tables, racks behind counters).
        return any((x + dx, y + dy, z + dz) in reach for dx in range(-2, 3) for dz in range(-2, 3) for dy in (-1, 0, 1))
    if (anchor[0], anchor[1], anchor[2] - 1) not in reach: problems.append('counter (front of the hut) not reachable from the entrance')
    seats = 0
    for entry in provider['posTagMap'][1][1]:
        p = entry['tagPos'][1]
        pos = (anchor[0] + p['x'][1], anchor[1] + p['y'][1], anchor[2] + p['z'][1])
        for tag in (t['tagName'][1] for t in entry['tagNameList'][1][1]):
            if tag == 'sit_in':
                seats += 1
                if 'stairs' not in at(*pos) and 'couch' not in at(*pos): problems.append(f'sit_in at {pos} is not a seat')
                if not air(pos[0], pos[1] + 1, pos[2]): problems.append(f'sit_in at {pos} has no headroom')
                if not any(not air(pos[0], y, pos[2]) for y in range(pos[1] + 2, sy)): problems.append(f'sit_in at {pos} has no roof')
                if not beside(*pos): problems.append(f'sit_in at {pos} not reachable')
            elif tag in ('sit', 'sit_out'):
                if not beside(*pos): problems.append(f'{tag} at {pos} not reachable')
            elif tag in ('work', 'stand_in'):
                if not beside(*pos): problems.append(f'{tag} at {pos} not reachable')
    if seats < 2: problems.append(f'only {seats} indoor seats')
    for i, b in enumerate(blocks):
        if 'rack' in pal[b] and pal[b].startswith('minecolonies:'):
            y, z, x = i // (sx * sz), (i % (sx * sz)) // sx, i % sx
            if not beside(x, y, z): problems.append(f'rack at {(x, y, z)} not reachable')
    return problems


def bundled(level): return load(BUNDLED / f'dispensary{level}.blueprint')


def main(source):
    source = Path(source)
    for level in range(1, 6):
        root = convert(load(source / f'dispensary{level}.blueprint'), level)
        problems = validate(root, level)
        (BUNDLED / f'dispensary{level}.blueprint').write_bytes(dump(root))
        print(f'level {level}: {size(root)}, mods {root["required_mods"][1][1]}, '
              + ('OK' if not problems else 'PROBLEMS: ' + '; '.join(problems)))


def lamps_only(files):
    for path in files:
        root = replace_lighting(load(path))
        Path(path).write_bytes(dump(root))
        left = sorted({n.split(':')[0] for n in names(root)} & {'projectred_illumination', 'cb_multipart'})
        print(f'{path}: mods {root["required_mods"][1][1]}' + (f' STILL USES {left}' if left else ''))


if __name__ == '__main__':
    if len(sys.argv) >= 3 and sys.argv[1] == '--lamps-only':
        lamps_only(sys.argv[2:])
    elif len(sys.argv) == 2:
        main(sys.argv[1])
    else:
        sys.exit(__doc__)

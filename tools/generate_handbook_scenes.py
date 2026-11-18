#!/usr/bin/env python3
"""The Handbook's scene illustrations (specs/016-weedcraft-handbook FR-011, research R8), rendered from real builds.

    python3 tools/generate_handbook_scenes.py

Each scene is a build in the test world, scanned with Structurize to tools/art/handbook_scenes/handbook_<name>.blueprint.
Preferred: the game's own render of it (the user, 2026-11-18): in a dev client near the builds, with
tools/art/handbook_scenes/scenes.json copied to config/fourtwenty-handbook-scenes.json, run /handbook_render, then crop
and scale the PNGs from the game folder's handbook-renders/ into tools/art/handbook_scenes/renders/ (1200 wide). Without
a render, this draws the scan in isometric view with each block's own textures (from the mod and the Minecraft 1.21.1 client jar):
full cubes as textured cubes, crops and flowers standing up, Wicks flat on the ground in their colour. Writes
textures/gui/handbook/scenes/<name>.png.
"""
import io
import json
import sys
from pathlib import Path

from PIL import Image, ImageEnhance

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
import nbtt as N  # noqa: E402
import build_handbook_pdf as P  # noqa: E402  (texture lookup shared with the PDF)

SCANS = ROOT / 'tools/art/handbook_scenes'
OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui/handbook/scenes'
AIR = {'minecraft:air', 'minecraft:cave_air', 'minecraft:void_air', 'structurize:blocksubstitution', 'minecraft:structure_void'}
CROSS = ('crop', 'wild_weed', 'short_grass', 'fern', 'flower', 'sapling', 'poppy', 'dandelion', 'tulip', 'bush')
FLAT = ('_wick', 'carpet', 'rail', 'pressure_plate', 'lily_pad')
WICK = {'red': (190, 40, 30), 'black': (40, 36, 34), 'none': (190, 170, 120), 'white': (230, 230, 230), 'lime': (120, 200, 60),
        'green': (60, 120, 40), 'blue': (50, 70, 190), 'yellow': (230, 200, 60), 'orange': (230, 130, 40), 'gray': (110, 110, 110)}
UPSCALE = 4
T = 16                      # texels per block face; the iso tile is 32 wide, 16 high on top, 18 for the sides


class Scan:
    def __init__(self, path):
        _, r, _ = N.load(path)
        self.X, self.Y, self.Z = r['size_x'][1], r['size_y'][1], r['size_z'][1]
        self.pal = r['palette'][1][1]
        shorts = []
        for i in r['blocks'][1]:
            i &= 0xffffffff
            shorts += [i >> 16, i & 0xffff]
        self.shorts = shorts

    def state(self, x, y, z):
        if not (0 <= x < self.X and 0 <= y < self.Y and 0 <= z < self.Z):
            return None
        e = self.pal[self.shorts[(y * self.Z + z) * self.X + x]]
        props = {k: v[1] for k, v in e.get('Properties', (N.COMP, {}))[1].items()}
        return e['Name'][1], props


# ---- textures

def _json(ns, path):
    if ns == 'fourtwenty':
        f = P.ASSETS / path
        return json.loads(f.read_text(encoding='utf-8')) if f.exists() else None
    j = P.jar()
    try:
        return json.loads(j.read(f'assets/{ns}/{path}')) if j else None
    except KeyError:
        return None


def _split(ref):
    return ref.split(':', 1) if ':' in ref else ('minecraft', ref)


def model_textures(model_ref, depth=0):
    """The texture map of a model, with its parents' merged under it."""
    ns, path = _split(model_ref)
    m = _json(ns, f'models/{path}.json')
    if not m or depth > 6:
        return {}, ''
    tex, parent_chain = {}, path
    if 'parent' in m:
        ptex, pchain = model_textures(m['parent'], depth + 1)
        tex.update(ptex)
        parent_chain = path + '>' + pchain
    tex.update(m.get('textures', {}))
    return tex, parent_chain


def resolve(tex, key):
    seen = 0
    v = tex.get(key)
    while v and v.startswith('#') and seen < 8:
        v = tex.get(v[1:])
        seen += 1
    return v


def block_model(name, props):
    ns, path = _split(name)
    bs = _json(ns, f'blockstates/{path}.json')
    if not bs:
        return f'{ns}:block/{path}'
    if 'variants' in bs:
        best = None
        for key, v in bs['variants'].items():
            conds = dict(kv.split('=') for kv in key.split(',') if '=' in kv)
            if all(props.get(k) == val for k, val in conds.items()):
                best = v
                break
        if best is None:
            best = next(iter(bs['variants'].values()))
        if isinstance(best, list):
            best = best[0]
        return best['model']
    for part in bs.get('multipart', []):
        a = part['apply']
        return (a[0] if isinstance(a, list) else a)['model']
    return f'{ns}:block/{path}'


_cache = {}
TINTED = {'minecraft:grass_block': (124, 189, 80), 'minecraft:oak_leaves': (90, 160, 50), 'minecraft:short_grass': (124, 189, 80),
          'minecraft:fern': (124, 189, 80)}


def tint(img, rgb):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            px[x, y] = (r * rgb[0] // 255, g * rgb[1] // 255, b * rgb[2] // 255, a)
    return out


def textures(name, props):
    """(top, side, chain) images for a block state."""
    key = (name, tuple(sorted(props.items())))
    if key in _cache:
        return _cache[key]
    model = block_model(name, props)
    tex, chain = model_textures(model)

    def img(*keys):
        for k in keys:
            ref = resolve(tex, k)
            if ref:
                ns, p = _split(ref)
                im = P._read(ns, f'textures/{p}.png')
                if im:
                    if im.height > im.width:
                        im = im.crop((0, 0, im.width, im.width))
                    return im.resize((T, T), Image.NEAREST)
        return None

    top = img('top', 'up', 'end', 'all', 'texture', 'cross', 'crop', 'plant', 'particle', 'side', 'front')
    if top is not None and name in TINTED:      # biome-coloured textures are grey in the files
        top = tint(top, TINTED[name])
    side = img('front', 'side', 'north', 'south', 'all', 'texture', 'cross', 'crop', 'particle', 'top')
    if top is None and side is None:
        fallback = Image.new('RGBA', (T, T), (150, 140, 120, 255))
        top = side = fallback
    _cache[key] = (top or side, side or top, chain)
    return _cache[key]


# ---- drawing

def face_top(tex):
    return tex.transform((33, 17), Image.AFFINE, (0.5, 1, -8, -0.5, 1, 8), Image.NEAREST, fillcolor=(0, 0, 0, 0))


def face_left(tex):
    k = T / 18
    return tex.transform((17, 35), Image.AFFINE, (1, 0, 0, -0.5 * k, k, -8 * k), Image.NEAREST, fillcolor=(0, 0, 0, 0))


def face_right(tex):
    k = T / 18
    return tex.transform((17, 35), Image.AFFINE, (1, 0, 0, 0.5 * k, k, -16 * k), Image.NEAREST, fillcolor=(0, 0, 0, 0))


def shade(img, f):
    rgb, a = img.convert('RGB'), img.getchannel('A')
    out = ImageEnhance.Brightness(rgb).enhance(f).convert('RGBA')
    out.putalpha(a)
    return out


def solid(name, chain):
    return not (any(c in name for c in CROSS + FLAT) or any(k in chain for k in ('cross', 'crop', 'fence', 'pane', 'slab', 'stairs', 'torch',
                                                                                 'lantern', 'door', 'trapdoor', 'carpet'))
                or name.endswith(('water', 'glass')))


def render(scan):
    W = (scan.X + scan.Z) * 16 + 40
    H = (scan.X + scan.Z) * 8 + scan.Y * 18 + 60
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ox, oy = scan.Z * 16 + 4, scan.Y * 18 + 20
    cells = [(x, y, z) for y in range(scan.Y) for z in range(scan.Z) for x in range(scan.X)]
    cells.sort(key=lambda c: (c[0] + c[2], c[1], c[0]))

    def opaque(x, y, z):
        s = scan.state(x, y, z)
        if s is None or s[0] in AIR:
            return False
        return solid(s[0], textures(*s)[2])

    for (x, y, z) in cells:
        name, props = scan.state(x, y, z)
        if name in AIR:
            continue
        sx, sy = ox + (x - z) * 16, oy + (x + z) * 8 - y * 18
        top, side, chain = textures(name, props)
        if name.endswith('water'):
            t = Image.new('RGBA', (T, T), (60, 110, 220, 170))
            img.alpha_composite(face_top(t), (sx, sy + 3))
        elif any(c in name for c in FLAT):
            colour = WICK.get(props.get('colour', 'none'), WICK['none']) if '_wick' in name else None
            t = Image.new('RGBA', (T, T), colour + (255,)) if colour else top
            if colour:      # a wick: a thin cord across the tile, not a full carpet
                t = Image.new('RGBA', (T, T), (0, 0, 0, 0))
                for i in range(T):
                    for w in (6, 7, 8, 9):
                        t.putpixel((i, w), colour + (255,))
                        t.putpixel((w, i), colour + (255,))
            img.alpha_composite(face_top(t), (sx, sy + 16))
        elif any(c in name for c in CROSS) or 'cross' in chain or 'crop' in chain:
            sprite = side.resize((24, 24), Image.NEAREST)
            img.alpha_composite(sprite, (sx + 4, sy - 2))
        else:
            small = any(k in chain or k in name for k in ('fence', 'lantern', 'torch'))
            if small:
                t, s_ = top.resize((T, T), Image.NEAREST), side
                post = Image.new('RGBA', (34, 36), (0, 0, 0, 0))
                post.alpha_composite(face_top(t), (0, 0))
                post.alpha_composite(shade(face_left(s_), 0.82), (0, 0))
                post.alpha_composite(shade(face_right(s_), 0.66), (16, 0))
                post = post.resize((14, 16), Image.NEAREST)
                img.alpha_composite(post, (sx + 9, sy + 16))
                continue
            if not opaque(x, y + 1, z):
                img.alpha_composite(face_top(top), (sx, sy))
            if not opaque(x, y, z + 1):
                img.alpha_composite(shade(face_left(side), 0.82), (sx, sy))
            if not opaque(x + 1, y, z):
                img.alpha_composite(shade(face_right(side), 0.66), (sx + 16, sy))
    box = img.getbbox()
    return img.crop(box) if box else img


RENDERS = SCANS / 'renders'


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for f in sorted(SCANS.glob('handbook_*.blueprint')):
        name = f.stem[len('handbook_'):]
        rendered = RENDERS / f'{name}.png'
        if rendered.exists():
            # the game's own render (/handbook_render in a dev client, scenes.json for the boxes), cropped and 1200 wide
            im = Image.open(rendered).convert('RGBA')
            im.crop(im.getbbox()).save(OUT / f'{name}.png', optimize=True)
            print(name, 'game render', im.size)
            continue
        im = render(Scan(f))
        # 4x, each texture pixel a sharp 4x4 square: the book scales it down smoothly, so it stays crisp at any size
        im = im.resize((im.width * UPSCALE, im.height * UPSCALE), Image.NEAREST)
        im.save(OUT / f'{name}.png', optimize=True)
        print(name, im.size)


if __name__ == '__main__':
    main()

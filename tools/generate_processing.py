#!/usr/bin/env python3
"""Art for weed processing: the rough and trimmed bud icons, and the Trim Tray and Curing Rack.

    python3 tools/generate_processing.py [--sheet out.png]

Rough and trimmed buds are drawn from the Cured Bud's own art, so all three read as stages of one bud: rough has big fan
leaves still on it, trimmed has its stem leaves cut away and is a touch frostier. Run tools/generate_strain_items.py
afterwards for the strain variants. Block models are boxes in a 16 grid; their textures are drawn here at 32 px.
"""
import json
import math
import random
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
SIZE = 256
INK = (6, 38, 6, 255)


def leaf(draw, x, y, angle, length, width, fill, ink):
    """One blade, in the bud's inked style: a pointed shape with a midrib."""
    ca, sa = math.cos(angle), math.sin(angle)

    def at(u, v):
        return x + u * ca - v * sa, y + u * sa + v * ca

    side = [(u / 10 * length, width * math.sin(math.pi * (u / 10)) ** 0.8) for u in range(11)]
    shape = [at(u, v) for u, v in side] + [at(u, -v) for u, v in reversed(side)]
    draw.polygon(shape, fill=ink)
    inner = [(u * 0.92 + length * 0.03, v * 0.62) for u, v in side]
    draw.polygon([at(u, v) for u, v in inner] + [at(u, -v) for u, v in reversed(inner)], fill=fill)
    draw.line([at(length * 0.08, 0), at(length * 0.82, 0)], fill=ink, width=max(1, int(length / 42)))


def fan(draw, x, y, angle, length, fill, ink):
    """A fan leaf: five blades."""
    for off, scale in ((-1.05, 0.62), (1.05, 0.62), (-0.52, 0.86), (0.52, 0.86), (0, 1.0)):
        leaf(draw, x, y, angle + off, length * scale, length * 0.15, fill, ink)


def rough(base):
    s = 1024
    img = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    fill = (46, 138, 22, 255)
    for x, y, angle, length in ((330, 690, 3.75, 360), (520, 300, -1.75, 330), (700, 650, 0.55, 340), (760, 300, -0.75, 280), (300, 430, 3.3, 300)):
        fan(draw, x, y, angle, length, fill, INK)
    img.alpha_composite(base.resize((s, s), Image.LANCZOS).convert('RGBA'))
    return img


def trimmed(base):
    img = base.resize((1024, 1024), Image.LANCZOS).convert('RGBA')
    a = np.array(img).astype(np.float32)
    h, w = a.shape[:2]
    yy, xx = np.mgrid[0:h, 0:w]
    # cut the stem leaves away: everything beyond a soft oval round the bud's body
    cx, cy, rx, ry, turn = 530, 440, 395, 450, math.radians(32)
    u = (xx - cx) * math.cos(turn) + (yy - cy) * math.sin(turn)
    v = -(xx - cx) * math.sin(turn) + (yy - cy) * math.cos(turn)
    d = (u / rx) ** 2 + (v / ry) ** 2
    a[..., 3] *= d <= 1
    # ink the new edge, then frost: pale specks on the green
    cut = Image.fromarray(a.astype(np.uint8), 'RGBA')
    alpha = cut.getchannel('A')
    grown = alpha.filter(ImageFilter.MaxFilter(25))
    edge = Image.new('RGBA', cut.size, INK)
    edge.putalpha(grown)
    edge.alpha_composite(cut)
    a = np.array(edge).astype(np.float32)
    rng = np.random.default_rng(420)
    specks = np.array(Image.fromarray((rng.random((128, 128)) > 0.93).astype(np.uint8) * 255).resize((1024, 1024), Image.NEAREST)) > 0
    green = (a[..., 1] > a[..., 0] * 1.3) & (a[..., 1] > 90) & (a[..., 3] > 200)
    a[..., :3] = np.where((specks & green)[..., None], a[..., :3] * 0.45 + np.array([238, 250, 225]) * 0.55, a[..., :3])
    return Image.fromarray(a.round().astype(np.uint8), 'RGBA')


def solid(img):
    a = np.array(img)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    return Image.fromarray(a, 'RGBA')


def wood(colour, seed, size=32):
    rng = random.Random(seed)
    img = Image.new('RGBA', (size, size), colour + (255,))
    px = img.load()
    for y in range(size):
        band = rng.randint(-5, 5)
        for x in range(size):
            shade = band + rng.randint(-3, 3) + int(4 * math.sin(x / 4 + y))
            px[x, y] = tuple(max(0, min(255, c + shade)) for c in colour) + (255,)
    d = ImageDraw.Draw(img)
    for y in range(0, size, 8):
        d.line([(0, y), (size, y)], fill=tuple(int(c * 0.72) for c in colour) + (255,))
    return img


def mat(size=32):
    img = Image.new('RGBA', (size, size), (34, 84, 52, 255))
    d = ImageDraw.Draw(img)
    for i in range(0, size, 8):
        d.line([(i, 0), (i, size)], fill=(64, 128, 84, 255))
        d.line([(0, i), (size, i)], fill=(64, 128, 84, 255))
    # the scissors lying on the mat
    d.line([(7, 25), (22, 8)], fill=(206, 212, 218, 255), width=2)
    d.line([(7, 8), (22, 25)], fill=(172, 178, 186, 255), width=2)
    d.ellipse([3, 24, 9, 30], outline=(42, 42, 46, 255), width=2)
    d.ellipse([20, 24, 26, 30], outline=(42, 42, 46, 255), width=2)
    # a few clippings
    for x, y in ((24, 9), (26, 13), (6, 6), (12, 4)):
        d.point([(x, y), (x + 1, y), (x, y + 1)], fill=(120, 196, 60, 255))
    return img


def twine(size=32):
    img = Image.new('RGBA', (size, size), (198, 170, 118, 255))
    d = ImageDraw.Draw(img)
    for i in range(-size, size, 4):
        d.line([(i, 0), (i + size, size)], fill=(150, 122, 78, 255))
    return img


def box(a, b, tex, **faces):
    all_faces = {side: {'uv': [0, 0, 16, 16], 'texture': f'#{tex}'} for side in ('north', 'south', 'east', 'west', 'up', 'down')}
    for side, t in faces.items():
        all_faces[side]['texture'] = f'#{t}'
    return {'from': a, 'to': b, 'faces': all_faces}


def coplanar(elements):
    """Faces of two boxes that lie in one plane and overlap, which z-fight: [(i, j, axis, plane)]."""
    out = []
    for i, a in enumerate(elements):
        for j, b in enumerate(elements[:i]):
            for axis in range(3):
                others = [k for k in range(3) if k != axis]
                touch = all(min(a['to'][k], b['to'][k]) > max(a['from'][k], b['from'][k]) for k in others)
                planes = {a['from'][axis], a['to'][axis]} & {b['from'][axis], b['to'][axis]}
                # same-facing faces only: boxes meeting end to end hide each other
                same = [p for p in planes if (p == a['from'][axis]) == (p == b['from'][axis])]
                if touch and same:
                    out.append((j, i, 'xyz'[axis], same))
    return out


def block(name, textures, elements, particle):
    clash = coplanar(elements)
    assert not clash, f'{name} z-fights: {clash}'
    model = {'parent': 'block/block', 'textures': {'particle': f'fourtwenty:block/processing/{particle}', **{k: f'fourtwenty:block/processing/{v}' for k, v in textures.items()}},
             'elements': elements}
    (ASSETS / f'models/block/{name}.json').write_text(json.dumps(model, indent=1) + '\n')
    (ASSETS / f'models/item/{name}.json').write_text(json.dumps({'parent': f'fourtwenty:block/{name}'}, indent=2) + '\n')
    variants = {f'facing={f}': {'model': f'fourtwenty:block/{name}', **({'y': y} if y else {})}
                for f, y in (('north', 0), ('east', 90), ('south', 180), ('west', 270))}
    (ASSETS / f'blockstates/{name}.json').write_text(json.dumps({'variants': variants}, indent=2) + '\n')


def main():
    base = Image.open(ASSETS / 'textures/item/weed_bud.png').convert('RGBA')
    icons = {'rough_bud': rough(base), 'trimmed_bud': trimmed(base)}
    for name, img in icons.items():
        solid(img.resize((SIZE, SIZE), Image.LANCZOS)).save(ASSETS / f'textures/item/{name}.png', optimize=True)
        path = ASSETS / f'models/item/{name}.json'
        if not path.exists():
            path.write_text(json.dumps({'parent': 'item/generated', 'textures': {'layer0': f'fourtwenty:item/{name}'}}, indent=2) + '\n')
    tex = ASSETS / 'textures/block/processing'
    tex.mkdir(parents=True, exist_ok=True)
    wood((122, 84, 48), 1).save(tex / 'wood_dark.png')
    wood((176, 132, 84), 2).save(tex / 'wood_light.png')
    mat().save(tex / 'mat.png')
    twine().save(tex / 'twine.png')
    block('trim_tray', {'wood': 'wood_dark', 'mat': 'mat'}, [
        box([0, 0, 0], [16, 1, 16], 'wood'),
        box([1, 1, 1], [15, 2, 15], 'mat'),
        box([0, 1, 0], [16, 4, 1], 'wood'), box([0, 1, 15], [16, 4, 16], 'wood'),
        box([0, 1, 1], [1, 4, 15], 'wood'), box([15, 1, 1], [16, 4, 15], 'wood'),
    ], 'wood_dark')
    block('curing_rack', {'post': 'wood_dark', 'rail': 'wood_light', 'twine': 'twine'}, [
        box([1, 1, 7], [3, 16, 9], 'post'), box([13, 1, 7], [15, 16, 9], 'post'),
        box([1, 0, 3], [3, 1, 13], 'post'), box([13, 0, 3], [15, 1, 13], 'post'),
        box([3, 14, 7], [13, 16, 9], 'rail'),
        box([3, 7, 7], [13, 9, 9], 'rail'),
        box([3, 12, 7.75], [13, 13, 8.25], 'twine'), box([3, 5, 7.75], [13, 6, 8.25], 'twine'),
    ], 'wood_dark')
    if '--sheet' in sys.argv:
        out = sys.argv[sys.argv.index('--sheet') + 1]
        sheet = Image.new('RGBA', (768, 256), (70, 70, 78, 255))
        for i, im in enumerate((icons['rough_bud'], icons['trimmed_bud'], base)):
            sheet.alpha_composite(solid(im.resize((256, 256), Image.LANCZOS)), (i * 256, 0))
        sheet.save(out)
    print('ok')


if __name__ == '__main__':
    main()

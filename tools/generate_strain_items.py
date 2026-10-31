#!/usr/bin/env python3
"""Strain variants of the weed bud and joint icons: the green parts of the art shifted part of the way toward each
strain's colour (paper and shading kept), chosen in game by the item property fourtwenty:strain.

    python3 tools/generate_strain_items.py [--sheet out.png]

Writes textures/item/strain/<item>_<strain>.png (256 px), models/item/strain/<item>_<strain>.json, and the overrides in
models/item/<item>.json (Landrace keeps the original art).
"""
import colorsys
import json
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
ITEMS = ('rough_bud', 'trimmed_bud', 'weed_bud', 'weed_joint')
SIZE = 256
SHIFT = 0.6      # how far the green's saturation moves toward the strain's
PAPER = 0.22     # how much colour a joint's paper takes

# Strain order matches domain/Strain (ordinal = index); Landrace (0) keeps the original texture.
STRAINS = [('landrace', '#7DAA3C'), ('myr_myr', '#B8A897'), ('myr_lim', '#E8D7B0'), ('myr_pin', '#C9C9C7'), ('myr_lav', '#B7C0C8'),
           ('lim_myr', '#A0673A'), ('lim_lim', '#F2C80F'), ('lim_pin', '#D7262E'), ('lim_lav', '#F07F1A'),
           ('pin_myr', '#8C8C2E'), ('pin_lim', '#9BD42A'), ('pin_pin', '#2EAA4A'), ('pin_lav', '#17A398'),
           ('lav_myr', '#8A3FD1'), ('lav_lim', '#F05AA0'), ('lav_pin', '#2F6BE0'), ('lav_lav', '#2BB5E8')]


def hsv(hexc):
    r, g, b = (int(hexc[i:i + 2], 16) / 255 for i in (1, 3, 5))
    return colorsys.rgb_to_hsv(r, g, b)


PAPER_ON = False


def recolour(img, hexc, paper_on=False):
    global PAPER_ON
    PAPER_ON = paper_on
    a = np.asarray(img.convert('RGBA')).astype(np.float32) / 255
    rgb, alpha = a[..., :3], a[..., 3:]
    mx, mn = rgb.max(-1), rgb.min(-1)
    v = mx; d = mx - mn
    s = np.where(mx > 0, d / np.maximum(mx, 1e-6), 0)
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    h = np.zeros_like(mx)
    m = d > 1e-6
    hr = ((g - b) / np.where(m, d, 1)) % 6
    hg = (b - r) / np.where(m, d, 1) + 2
    hb = (r - g) / np.where(m, d, 1) + 4
    h = np.where(mx == r, hr, np.where(mx == g, hg, hb)) / 6
    h = np.where(m, h, 0)
    # the plant: green-ish hues with some colour (paper, ash and outlines are left alone)
    green = (h > 0.13) & (h < 0.47) & (s > 0.18)
    th, ts, tv = hsv(hexc)
    lh, ls, lv = hsv(STRAINS[0][1])
    nh = np.full_like(h, th)                              # the strain's own hue (a part-way turn lands on the wrong colour)
    ns = np.clip(s * (1 - SHIFT + SHIFT * ts / ls), 0, 1)   # its saturation only part of the way: a hint, not a repaint
    nv = np.clip(v * (1 - SHIFT * 0.5 + SHIFT * 0.5 * tv / lv), 0, 1)
    # the paper (a joint): pale, nearly grey: a slight wash of the strain's colour
    paper = (s < 0.25) & (v > 0.55) & (alpha[..., 0] > 0.5)
    nh = np.where(paper, th, nh); ns = np.where(paper, np.minimum(1, s + PAPER * ts), ns); nv = np.where(paper, v, nv)
    green = green | (paper & PAPER_ON)
    h2 = np.where(green, nh, h); s2 = np.where(green, ns, s); v2 = np.where(green, nv, v)
    i = np.floor(h2 * 6).astype(int) % 6; f = h2 * 6 - np.floor(h2 * 6)
    p, q, t = v2 * (1 - s2), v2 * (1 - f * s2), v2 * (1 - (1 - f) * s2)
    out = np.choose(i[..., None], [np.stack(c, -1) for c in ((v2, t, p), (q, v2, p), (p, v2, t), (p, q, v2), (t, p, v2), (v2, p, q))])
    return Image.fromarray((np.concatenate([out, alpha], -1) * 255).round().astype(np.uint8), 'RGBA')


def solid(img):
    """Every pixel fully opaque or fully clear: an item's edges are extruded per pixel, and a half-clear edge shows its
    sides as see-through slivers in the hand."""
    a = np.array(img)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    return Image.fromarray(a, 'RGBA')


def main():
    sheet_rows = []
    for item in ITEMS:
        base = Image.open(ASSETS / f'textures/item/{item}.png').convert('RGBA')
        model_path = ASSETS / f'models/item/{item}.json'
        model = json.loads(model_path.read_text())
        overrides = []
        row = [base.resize((128, 128), Image.LANCZOS)]
        for idx, (sid, hexc) in enumerate(STRAINS):
            if idx == 0:
                continue
            tex = solid(recolour(base, hexc, paper_on=(item == 'weed_joint')).resize((SIZE, SIZE), Image.LANCZOS))
            (ASSETS / 'textures/item/strain').mkdir(parents=True, exist_ok=True)
            tex.save(ASSETS / f'textures/item/strain/{item}_{sid}.png', optimize=True)
            (ASSETS / 'models/item/strain').mkdir(parents=True, exist_ok=True)
            child = {'parent': f'fourtwenty:item/{item}', 'textures': {'layer0': f'fourtwenty:item/strain/{item}_{sid}'}}
            (ASSETS / f'models/item/strain/{item}_{sid}.json').write_text(json.dumps(child, indent=2) + '\n')
            overrides.append({'predicate': {'fourtwenty:strain': round(idx / 16, 4)}, 'model': f'fourtwenty:item/strain/{item}_{sid}'})
            row.append(tex.resize((128, 128), Image.LANCZOS))
        model['overrides'] = overrides
        model_path.write_text(json.dumps(model, indent=2) + '\n')
        sheet_rows.append(row)
        print(item, len(overrides), 'strain variants')
    if '--sheet' in sys.argv:
        out = sys.argv[sys.argv.index('--sheet') + 1]
        W = 128 * len(sheet_rows[0])
        sheet = Image.new('RGBA', (W, 128 * len(sheet_rows)), (40, 40, 46, 255))
        for r, row in enumerate(sheet_rows):
            for c, im in enumerate(row):
                sheet.alpha_composite(im, (c * 128, r * 128))
        sheet.save(out)


if __name__ == '__main__':
    main()

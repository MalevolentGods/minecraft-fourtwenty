#!/usr/bin/env python3
"""FourTwenty's own 64x64 bed textures (seeded, so a re-run gives the same images).

Writes textures/block/bed/:
- <wood>_post.png / <wood>_rail.png: the frame wood with its grain running along the piece
  (vertical for posts, horizontal for rails), one per wood in WOODS;
- <wood>_panel.png: a headboard/footboard panel, an inset centre in a bevelled frame;
- sheet.png: white linen; pillow.png: a soft cotton pillow with a seam;
- blanket_<colour>.png: a hotel duvet (tone-on-tone satin stripe) in each of the 16 dye colours;
- runner_<colour>.png: a hotel bed runner to match, a deeper shade with light edge stripes.
Needs Pillow. Run again after edits; tools/generate_beds.py uses these.
"""
import math
import random
from pathlib import Path

from PIL import Image, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/block/bed'
S = 64

WOODS = {'oak': (178, 140, 88), 'spruce': (112, 82, 50), 'birch': (196, 180, 124), 'jungle': (158, 112, 78),
         'acacia': (170, 92, 52), 'dark_oak': (70, 45, 22), 'mangrove': (118, 52, 46), 'cherry': (222, 170, 162),
         'crimson': (104, 48, 72), 'warped': (44, 106, 100)}
COLORS = {'white': (232, 234, 232), 'orange': (236, 120, 28), 'magenta': (186, 72, 176), 'light_blue': (64, 172, 214),
          'yellow': (244, 196, 48), 'lime': (118, 186, 34), 'pink': (234, 142, 172), 'gray': (68, 74, 78),
          'light_gray': (146, 146, 140), 'cyan': (28, 136, 144), 'purple': (120, 48, 170), 'blue': (56, 62, 156),
          'brown': (116, 74, 44), 'green': (86, 110, 32), 'red': (160, 44, 38), 'black': (30, 31, 36)}


def smooth_noise(rng, cells_x, cells_y, size=S):
    """Value noise in [0,1]: a small random image scaled up smoothly."""
    small = Image.new('L', (cells_x, cells_y))
    small.putdata([rng.randrange(256) for _ in range(cells_x * cells_y)])
    return small.resize((size, size), Image.BICUBIC)


def clamp(v): return max(0, min(255, int(round(v))))


def shade(rgb, f): return tuple(clamp(c * f) for c in rgb)


def wood(base, seed):
    """Vertical grain: long streaks, a gentle wave, a few darker grain lines and fine fibre noise."""
    rng = random.Random(seed)
    broad = smooth_noise(rng, 4, 3)
    wave = smooth_noise(rng, 2, 6)
    streaks = Image.new('L', (S, 1))
    streaks.putdata([rng.randrange(256) for _ in range(S)])
    streaks = streaks.resize((S, 2), Image.NEAREST).resize((S, S), Image.NEAREST).filter(ImageFilter.GaussianBlur(0.6))
    lines = {rng.randrange(S) for _ in range(4)}
    img = Image.new('RGB', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            w = (wave.getpixel((x, y)) / 255 - 0.5) * 2
            xx = int(x + w) % S
            f = 1 + 0.12 * (streaks.getpixel((xx, y)) / 255 - 0.5) + 0.05 * (broad.getpixel((x, y)) / 255 - 0.5)
            f += 0.02 * math.sin((xx + w) * 1.3)
            if xx in lines:
                f -= 0.07
            f += rng.uniform(-0.025, 0.025)
            px[x, y] = shade(base, f)
    return img


def panel(base, seed):
    """An inset panel: a bevelled frame (light top/left, dark bottom/right) round a recessed centre."""
    grain_v, grain_h = wood(base, seed), wood(base, seed + 1).rotate(90)
    img = Image.new('RGB', (S, S))
    px, gv, gh = img.load(), grain_v.load(), grain_h.load()
    border = 9
    for y in range(S):
        for x in range(S):
            inside = border <= x < S - border and border <= y < S - border
            frame_side = x < border or x >= S - border
            c = gv[x, y] if (frame_side or inside) else gh[x, y]
            f = 1.0
            if inside:
                f = 0.93                                         # the recessed centre sits a touch darker
                if x in (border, border + 1) or y in (border, border + 1):
                    f = 0.78                                     # shadow under the frame's top/left lip
                elif x in (S - border - 1, S - border - 2) or y in (S - border - 1, S - border - 2):
                    f = 1.08                                     # lit far edge
            else:
                if x < 2 or y < 2:
                    f = 1.10                                     # outer bevel catches the light
                elif x >= S - 2 or y >= S - 2:
                    f = 0.80
            px[x, y] = shade(c, f)
    return img


def linen(base, seed):
    rng = random.Random(seed)
    broad = smooth_noise(rng, 5, 5)
    img = Image.new('RGB', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            weave = 0.015 if (x + y) % 2 == 0 else -0.015
            f = 1 + weave + 0.03 * (broad.getpixel((x, y)) / 255 - 0.5) + rng.uniform(-0.012, 0.012)
            px[x, y] = shade(base, f)
    return img


def pillow(seed):
    img = linen((240, 240, 236), seed)
    px = img.load()
    for y in range(S):
        for x in range(S):
            dx, dy = (x - 31.5) / 32, (y - 31.5) / 32
            f = 1 - 0.10 * (dx * dx + dy * dy)                 # plumper in the middle
            if x in (4, S - 5) or y in (4, S - 5):
                f -= 0.07                                       # the seam
            elif x < 4 or y < 4 or x >= S - 4 or y >= S - 4:
                f -= 0.04                                       # the flat border beyond it
            px[x, y] = shade(px[x, y], f)
    return img


def blanket(base, seed):
    """A hotel duvet: a fine tone-on-tone satin stripe that catches the light, large soft folds and
    a close cotton weave. Subtle on purpose; the runner carries the contrast."""
    rng = random.Random(seed)
    folds = smooth_noise(rng, 3, 4)
    img = Image.new('RGB', (S, S))
    px = img.load()
    lum = sum(base) / 3
    sheen = 0.035 if lum > 60 else 0.08          # dark colours need a little more to show the stripe
    for y in range(S):
        for x in range(S):
            stripe = (x // 4) % 2                 # 4-pixel satin stripes
            f = 1 + (sheen if stripe else -sheen / 2)
            if x % 4 == 0:
                f -= sheen * 0.6                   # the fine line where two stripes meet
            f += 0.06 * (folds.getpixel((x, y)) / 255 - 0.5)
            f += 0.012 if (x + y) % 2 == 0 else -0.012
            f += rng.uniform(-0.01, 0.01)
            px[x, y] = shade(base, f)
    return img


def runner(base, seed):
    """A hotel bed runner: a deeper shade of the blanket in a soft herringbone, with a light stripe
    just in from each long edge (the long edges are the top and bottom of the texture)."""
    rng = random.Random(seed)
    deep = shade(base, 0.62 if sum(base) / 3 > 60 else 1.6)
    light = tuple(clamp(c + (255 - c) * 0.55) for c in base)
    img = Image.new('RGB', (S, S))
    px = img.load()
    for y in range(S):
        for x in range(S):
            if 7 <= y <= 9 or 54 <= y <= 56:
                c, f = light, 1.0
            else:
                c = deep
                f = 1 + (0.05 if ((x + (y % 8 if (y // 8) % 2 else 7 - y % 8)) // 2) % 2 else -0.03)  # herringbone
                if y < 3 or y > 60:
                    f -= 0.08                       # the edges roll under
            f += rng.uniform(-0.012, 0.012)
            px[x, y] = shade(c, f)
    return img


def generate():
    OUT.mkdir(parents=True, exist_ok=True)
    for i, (name, base) in enumerate(WOODS.items()):
        wood(base, 100 + i).save(OUT / f'{name}_post.png')
        wood(base, 200 + i).rotate(90).save(OUT / f'{name}_rail.png')
        panel(base, 300 + i).save(OUT / f'{name}_panel.png')
    linen((236, 236, 232), 7).save(OUT / 'sheet.png')
    pillow(8).save(OUT / 'pillow.png')
    for i, (name, base) in enumerate(COLORS.items()):
        blanket(base, 400 + i).save(OUT / f'blanket_{name}.png')
        runner(base, 500 + i).save(OUT / f'runner_{name}.png')
    print(f'{len(WOODS) * 3 + 2 + 2 * len(COLORS)} textures written to {OUT.relative_to(ROOT).as_posix()}')


if __name__ == '__main__':
    generate()

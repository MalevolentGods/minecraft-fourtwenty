"""Item icons from generated icon sheets (the art pass, 2026-11-15).

    python tools/import_item_sheet.py items_a [sheet.png]
    python tools/import_item_sheet.py items_a1 [sheet.png]

Each sheet (tools/art/<name>_sheet.png) is a grid of icons on a flat magenta background, in the order SHEETS lists,
read left to right, top to bottom. The sheet is cut into equal grid cells; in each cell everything that isn't magenta
is the icon (so an icon in several pieces, like kief, stays whole). The icon is cropped to its pieces, the magenta
turned transparent (its blended fringe too, so no pink halo), centred on a square and scaled to SIZE. SIZE is a
multiple of 16: anything else lowers the atlas's mipmap levels and every block shimmers at a distance.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
ITEMS = ROOT / 'src/main/resources/assets/fourtwenty/textures/item'
SIZE = 256
MARGIN = 0.04   # transparent border kept round each icon, as a share of SIZE

# sheet name -> (columns, rows, item ids in reading order; None leaves a cell unused)
SHEETS = {
    # two takes of the same sheet; the user picked the Dab Cell from the first and the rest from the second. The
    # pipe on both is skipped: the user's hand-made pipe stays.
    'items_a': (4, 3, ['hash', 'kief', 'resin', 'rosin',
                       'oil', 'ash', 'weed_brownie', None,
                       None, 'bit_chit', 'skip_me_bro', 'buzz_bot']),
    'items_a1': (4, 3, [None, None, None, None,
                        None, None, None, None,
                        'dab_cell', None, None, None]),
    'items_b': (5, 2, ['hotbox_helm', 'puff_plate', 'couchlock_greaves', 'moon_boots', 'blazer',
                       'pave_pen', 'pave_pen_pro', 'inspoweedometer', 'sticky_finger', 'twister']),
}


def magentaness(rgb):
    """0 for a colour nothing like magenta, 1 for pure background magenta."""
    r, g, b = rgb[..., 0], rgb[..., 1], rgb[..., 2]
    m = np.minimum(r, b) - g          # how far red and blue both stand above green
    return np.clip((m - 60) / 120, 0, 1)


# per icon: near-white, unsaturated pixels that are really background the generator painted white (the rosin's gap
# between the slab and its chip)
WHITE_IS_CLEAR = {'rosin'}


def cut(cell, item):
    a = np.asarray(cell.convert('RGB')).astype(float)
    mag = magentaness(a)
    if item in WHITE_IS_CLEAR:
        white = (a.min(axis=2) > 200) & (a.max(axis=2) - a.min(axis=2) < 30)
        mag = np.where(white, 1.0, mag)
    # pieces touching the cell's edge belong to a neighbour that reaches over the grid line (the Dab Cell's cap
    # pokes up under the oil); everything else in the cell is this icon
    labels, n = ndimage.label(mag < 0.5)
    edge = set(np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))) - {0}
    if edge:
        mag = np.where(np.isin(labels, list(edge)), 1.0, mag)
    ys, xs = np.nonzero(mag < 0.5)
    if len(xs) == 0:
        return None
    x0, x1, y0, y1 = xs.min(), xs.max() + 1, ys.min(), ys.max() + 1
    a, mag = a[y0:y1, x0:x1], mag[y0:y1, x0:x1]
    alpha = 1 - mag
    # take the magenta back out of the blended fringe: c = alpha*icon + (1-alpha)*magenta
    bg = np.array([255.0, 0.0, 255.0])
    with np.errstate(divide='ignore', invalid='ignore'):
        rgb = np.where(alpha[..., None] > 0.05, (a - (1 - alpha[..., None]) * bg) / alpha[..., None], 0)
    rgb = np.clip(rgb, 0, 255)
    rgba = np.dstack([rgb, alpha * 255]).astype(np.uint8)
    icon = Image.fromarray(rgba, 'RGBA')
    w, h = icon.size
    side = int(max(w, h) / (1 - 2 * MARGIN))
    square = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    square.alpha_composite(icon, ((side - w) // 2, (side - h) // 2))
    return solid(square.resize((SIZE, SIZE), Image.LANCZOS))


def solid(img):
    """Every pixel fully opaque or fully clear: an item's edges are extruded per pixel, and a half-clear edge shows its
    sides as see-through slivers in the hand (as tools/generate_strain_items.py)."""
    a = np.array(img)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    return Image.fromarray(a, 'RGBA')


def splits(sheet, n, axis):
    """Where to cut the sheet into n columns (axis 0) or rows (axis 1): at each even grid line, moved to the line with
    the least icon on it within a sixth of a cell, so an icon reaching over the grid line (the Buzz Bot's antennae)
    isn't cut through."""
    mag = magentaness(np.asarray(sheet).astype(float))
    ink = (mag < 0.5).sum(axis=axis)          # per column (axis 0) or per row (axis 1)
    size = len(ink)
    cuts = [0]
    for k in range(1, n):
        nominal = size * k // n
        reach = size // n // 6
        lo, hi = nominal - reach, nominal + reach
        window = ink[lo:hi]
        # the middle of the widest run of the emptiest lines, well clear of the icons on both sides
        low = window <= window.min()
        best, start, run = (0, 0), None, 0
        for j, v in enumerate(list(low) + [False]):
            if v and start is None:
                start = j
            if not v and start is not None:
                if j - start > best[1] - best[0]:
                    best = (start, j)
                start = None
        cuts.append(lo + (best[0] + best[1]) // 2)
    return cuts + [size]


def main():
    name = sys.argv[1]
    cols, rows, ids = SHEETS[name]
    sheet = Image.open(sys.argv[2] if len(sys.argv) > 2 else ROOT / f'tools/art/{name}_sheet.png').convert('RGB')
    W, H = sheet.size
    xs, ys = splits(sheet, cols, axis=0), splits(sheet, rows, axis=1)
    for i, item in enumerate(ids):
        if item is None:
            continue
        c, r = i % cols, i // cols
        cell = sheet.crop((xs[c], ys[r], xs[c + 1], ys[r + 1]))
        icon = cut(cell, item)
        if icon is None:
            print(f'{item}: empty cell, skipped')
            continue
        icon.save(ITEMS / f'{item}.png')
        print(f'{item}: {SIZE}x{SIZE}')


if __name__ == '__main__':
    main()

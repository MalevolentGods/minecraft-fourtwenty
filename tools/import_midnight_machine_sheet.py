"""The Midnight Machine's painted textures, from one generated texture sheet (spec 014, the user's art pass 2026-11-14).

    python tools/import_midnight_machine_sheet.py [sheet.png]

The sheet (tools/art/midnight_machine_sheet.png) has seven panels on a flat magenta background: top row front,
back, side, rail, strap; bottom row top, knob. Each is found as a connected non-magenta region, trimmed of its
magenta-blended edge and fitted to its texture size, which stays close to the sheet's own resolution. Written to
textures/block/midnight_machine/; tools/generate_midnight_machine_model.py lays them on the model.
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
SHEET = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / 'tools/art/midnight_machine_sheet.png'
TEX = ROOT / 'src/main/resources/assets/fourtwenty/textures/block/midnight_machine'

# panel order on the sheet (top row left to right, then bottom row) and each texture's size: about the sheet's own
# resolution (the user wants it high-res), rounded to multiples of 16. Any size that isn't a multiple of 16 quietly
# lowers the mipmap levels of the whole block atlas, and every block in the game shimmers at a distance.
PANELS = [('front', (384, 704)), ('back', (304, 704)), ('side', (144, 704)), ('rail', (80, 704)), ('strap', (112, 704)),
          ('top', (736, 240)), ('knob', (160, 160))]
TRIM = 3   # sheet pixels trimmed off each panel edge (blended with the magenta)


def panels(img):
    a = np.asarray(img).astype(int)
    magenta = (a[:, :, 0] > 200) & (a[:, :, 1] < 80) & (a[:, :, 2] > 200)
    labels, _ = ndimage.label(~magenta)
    boxes = []
    for sl in ndimage.find_objects(labels):
        h, w = sl[0].stop - sl[0].start, sl[1].stop - sl[1].start
        if h * w > 2000:
            boxes.append((sl[1].start, sl[0].start, sl[1].stop, sl[0].stop))
    rows = sorted(boxes, key=lambda b: b[1])
    top_row = sorted([b for b in rows if b[1] < rows[0][1] + 100], key=lambda b: b[0])
    bottom_row = sorted([b for b in rows if b[1] >= rows[0][1] + 100], key=lambda b: b[0])
    found = top_row + bottom_row
    if len(found) != len(PANELS):
        raise SystemExit(f'expected {len(PANELS)} panels on the sheet, found {len(found)}')
    return found


def main():
    img = Image.open(SHEET).convert('RGB')
    TEX.mkdir(parents=True, exist_ok=True)
    for (name, size), (x0, y0, x1, y1) in zip(PANELS, panels(img)):
        crop = img.crop((x0 + TRIM, y0 + TRIM, x1 - TRIM, y1 - TRIM))
        assert size[0] % 16 == 0 and size[1] % 16 == 0, name
        crop.resize(size, Image.LANCZOS).save(TEX / f'{name}.png')
        print(f'{name}: {x1 - x0}x{y1 - y0} on the sheet -> {size[0]}x{size[1]}')


if __name__ == '__main__':
    main()

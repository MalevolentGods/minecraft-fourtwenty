#!/usr/bin/env python3
"""Cuts the user's sheets of chapter-opener paintings (tools/art/chapter_openers_<n>.png, made with an image generator from the two
prompts in specs/016-weedcraft-handbook) into one picture per chapter: textures/gui/handbook/scenes/chapter_<chapter>.png.

    python3 tools/slice_chapter_openers.py

Each painting has soft, ragged watercolour edges on cream paper. The cream around it becomes transparent (softly, so the ragged edge
stays), and the painting is cropped to itself. Pictures are found as the large painted areas on the sheet, in reading order (rows top
to bottom, then left to right). A sheet that isn't there yet is skipped.
"""
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/gui/handbook/scenes'
SHEETS = [('chapter_openers_1.png', ['getting_started', 'scouting', 'growing', 'processing']),
          ('chapter_openers_2.png', ['colours', 'terpinator', 'power', 'electronics', 'colony'])]
MARGIN = 6


def slice_sheet(path, chapters):
    im = Image.open(path).convert('RGB')
    a = np.asarray(im).astype(float)
    background = np.median(np.concatenate([a[:6].reshape(-1, 3), a[-6:].reshape(-1, 3)]), axis=0)
    diff = np.abs(a - background).sum(axis=2)
    painted = ndimage.binary_opening(ndimage.binary_fill_holes(ndimage.binary_closing(diff > 40, iterations=4)), iterations=3)
    lab, n = ndimage.label(painted)
    sizes = ndimage.sum(painted, lab, range(1, n + 1))
    boxes = ndimage.find_objects(lab)
    keep = [int(l) for l in np.argsort(sizes)[-len(chapters):] + 1]
    if len(keep) < len(chapters) or min(sizes[l - 1] for l in keep) < 0.02 * painted.size:
        raise SystemExit(f'{path.name}: found {len(keep)} paintings, expected {len(chapters)}')
    row_h = im.height / 4
    keep.sort(key=lambda l: (round(boxes[l - 1][0].start / row_h), boxes[l - 1][1].start))
    # soft alpha: cream is clear, paint is solid, the ragged edge in between
    alpha = np.clip((diff - 18) / 50, 0, 1)
    OUT.mkdir(parents=True, exist_ok=True)
    for chapter, l in zip(chapters, keep):
        ys, xs = boxes[l - 1]
        y0, y1 = max(0, ys.start - MARGIN), min(im.height, ys.stop + MARGIN)
        x0, x1 = max(0, xs.start - MARGIN), min(im.width, xs.stop + MARGIN)
        region = ndimage.binary_dilation(lab == l, iterations=MARGIN)[y0:y1, x0:x1]
        al = np.where(region, alpha[y0:y1, x0:x1], 0)
        rgba = np.dstack([a[y0:y1, x0:x1], al * 255]).astype('uint8')
        Image.fromarray(rgba, 'RGBA').save(OUT / f'chapter_{chapter}.png', optimize=True)
        print(chapter, x0, y0, x1 - x0, y1 - y0)


def main():
    for name, chapters in SHEETS:
        path = ROOT / 'tools/art' / name
        if path.exists():
            slice_sheet(path, chapters)
        else:
            print('not yet:', name)


if __name__ == '__main__':
    main()

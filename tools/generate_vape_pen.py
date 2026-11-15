"""The Vape Pen's icon, drawn to match the painted icon sheets (thick dark outline, cel shading, light from the top
left): a slim gunmetal pen, a glass cartridge of golden oil, a cream mouthpiece and a lime button, held diagonally.

    python tools/generate_vape_pen.py

Drawn large and lying flat, then turned to the diagonal and scaled to 256 (a multiple of 16, as every item texture
here must be); every pixel ends fully opaque or fully clear (see tools/import_item_sheet.py).
"""
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/fourtwenty/textures/item/vape_pen.png'

S = 4                      # drawing scale over the final 256
W, H = 300 * S, 64 * S     # the pen lying flat, mouthpiece to the right
INK = (22, 18, 24, 255)


def rr(d, box, r, fill):
    d.rounded_rectangle([v * S for v in box], r * S, fill=fill)


def shaded(d, box, r, base, light, dark):
    """A rounded bar: base colour, a highlight band along the top, a shadow band along the bottom."""
    x0, y0, x1, y1 = box
    rr(d, box, r, base)
    h = y1 - y0
    rr(d, (x0 + 2, y0 + 3, x1 - 2, y0 + 3 + h * 0.22), r * 0.5, light)
    rr(d, (x0 + 2, y1 - 3 - h * 0.2, x1 - 2, y1 - 3), r * 0.5, dark)


def pen():
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # body: bottom cap, battery, collar, cartridge, mouthpiece (left to right)
    shaded(d, (14, 14, 40, 50), 8, (70, 74, 82, 255), (128, 134, 146, 255), (40, 42, 48, 255))      # end cap
    shaded(d, (36, 12, 168, 52), 7, (52, 56, 64, 255), (104, 110, 122, 255), (30, 32, 38, 255))     # battery
    # the lime button and a little charge strip
    d.ellipse([92 * S, 22 * S, 112 * S, 42 * S], fill=(120, 230, 60, 255))
    d.ellipse([96 * S, 25 * S, 104 * S, 32 * S], fill=(214, 255, 160, 255))
    rr(d, (124, 27, 156, 37), 4, (34, 120, 30, 255))
    rr(d, (124, 27, 146, 37), 4, (126, 236, 70, 255))
    shaded(d, (164, 10, 178, 54), 4, (196, 160, 70, 255), (250, 222, 140, 255), (130, 98, 30, 255))  # gold collar
    # the glass cartridge, golden oil inside
    rr(d, (176, 12, 250, 52), 6, (196, 222, 226, 255))
    rr(d, (182, 20, 244, 48), 4, (232, 160, 24, 255))
    rr(d, (182, 20, 244, 29), 3, (255, 208, 80, 255))
    rr(d, (186, 15, 240, 19), 2, (246, 252, 252, 255))           # glass shine
    d.ellipse([200 * S, 34 * S, 206 * S, 40 * S], fill=(255, 226, 140, 255))   # a bubble
    d.ellipse([222 * S, 38 * S, 226 * S, 42 * S], fill=(255, 226, 140, 255))
    shaded(d, (246, 14, 258, 50), 4, (196, 160, 70, 255), (250, 222, 140, 255), (130, 98, 30, 255))  # top collar
    # the mouthpiece, tapering
    d.polygon([(256 * S, 16 * S), (286 * S, 22 * S), (286 * S, 42 * S), (256 * S, 48 * S)], fill=(236, 226, 204, 255))
    d.polygon([(258 * S, 19 * S), (284 * S, 24 * S), (284 * S, 29 * S), (258 * S, 26 * S)], fill=(255, 252, 240, 255))
    d.polygon([(258 * S, 40 * S), (284 * S, 38 * S), (284 * S, 41 * S), (258 * S, 46 * S)], fill=(184, 170, 146, 255))
    return img


def outline(img, width):
    a = np.asarray(img)[..., 3] > 0
    grown = Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(width * 2 + 1))
    out = Image.new('RGBA', img.size, (0, 0, 0, 0))
    out.paste(Image.new('RGBA', img.size, INK), (0, 0), grown)
    out.alpha_composite(img)
    return out


def main():
    img = outline(pen(), 5 * S // 2 * 2 + 1)
    img = img.rotate(40, resample=Image.BICUBIC, expand=True)
    box = img.getbbox()
    img = img.crop(box)
    side = int(max(img.size) / 0.92)
    square = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    square.alpha_composite(img, ((side - img.size[0]) // 2, (side - img.size[1]) // 2))
    out = square.resize((256, 256), Image.LANCZOS)
    a = np.array(out)
    a[..., 3] = np.where(a[..., 3] >= 128, 255, 0)
    Image.fromarray(a, 'RGBA').save(OUT)
    print('wrote', OUT.relative_to(ROOT))


if __name__ == '__main__':
    main()

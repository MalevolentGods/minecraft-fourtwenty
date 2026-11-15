"""The Midnight Machine, concept B "Synth Machine" (picked by the user 2026-11-14), as a Minecraft model (spec 014 T017).

    python tools/generate_midnight_machine_model.py [album dir]

Writes assets/fourtwenty/models/midnight_machine/body.json (a standalone model, no item or block). Its painted
textures come from the user's texture sheet via tools/import_midnight_machine_sheet.py; this writes the rest (album
art, antenna, white for the VU bars). The six VU bars are not in the model: BoomboxLayer draws them in the painted
meter window, each its own band's height, so they move with the music.

Model space: units are pixels; x 3..13 across her back (8 = her middle), y 0 = the bottom of her body going up, and
z mirrored (z = -concept z) so the face away from her back is the model's north face. BoomboxLayer turns it into the
body part's space.
"""
import json
import os
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ALBUM = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r'C:\Users\Ignus\OneDrive\Desktop\ATLemental - Midnight Machine')
MODEL = ROOT / 'src/main/resources/assets/fourtwenty/models/midnight_machine/body.json'
TEX = ROOT / 'src/main/resources/assets/fourtwenty/textures/block/midnight_machine'

MIDNIGHT = (20, 1, 25)
GRAPHITE = (35, 17, 33)
HOT = (237, 46, 101)
PINK = (243, 70, 89)
CREAM = (251, 251, 225)
GOLD = (240, 203, 115)


def textures():
    """The parts not on the painted sheet (tools/import_midnight_machine_sheet.py writes front, back, side, rail, strap,
    top and knob): the album art for the art window, the antenna and its tip, and plain white for the VU bars."""
    TEX.mkdir(parents=True, exist_ok=True)
    for old in ('shell', 'trim', 'frame'):   # the flat placeholder textures the painted sheet replaced
        (TEX / f'{old}.png').unlink(missing_ok=True)
    im = Image.new('RGB', (16, 16), (210, 210, 220))
    ImageDraw.Draw(im).line([(7, 0), (7, 15)], fill=(245, 245, 250))
    im.save(TEX / 'antenna.png')
    im = Image.new('RGB', (16, 16), PINK)
    ImageDraw.Draw(im).rectangle([5, 5, 10, 10], fill=(255, 190, 200))
    im.save(TEX / 'tip.png')
    # the album art for the art window: Art.png at 32x32, sharpened so the phoenix still reads
    art = Image.open(ALBUM / 'Art.png').convert('RGB').resize((32, 32), Image.LANCZOS).filter(ImageFilter.SHARPEN)
    art.save(TEX / 'art.png')
    Image.new('RGB', (16, 16), (255, 255, 255)).save(TEX / 'white.png')


def element(frm, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), lit=False, uv=(0, 0, 16, 16), name=None,
            face_tex=None):
    e = {'from': [round(v, 3) for v in frm], 'to': [round(v, 3) for v in to], 'faces': {}}
    if name:
        e['name'] = name
    for f in faces:
        e['faces'][f] = {'texture': f'#{(face_tex or {}).get(f, tex)}', 'uv': list(uv)}
    if lit:
        e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    return e


# The painted rack (the user's sheet, 2026-11-14): 10 wide, 18 tall, 4 deep, standing off her back by a quilted
# pad so its top half clears her head (head: model x 4..12, z up to -4). Model space: x 3..13, y 0 = the bottom of
# her body, z mirrored (north = away from her).
RACK_Z = (-8.25, -4.25)
RACK_H = 18
# windows painted on the front panel, as fractions measured on the sheet (from the top / from the left)
ART_V = (0.068, 0.389)
ART_U = (0.080, 0.923)


def model():
    els = []
    z0, z1 = RACK_Z
    sides = ('north', 'south', 'east', 'west')
    els.append(element([3, 0, z0], [13, RACK_H, z1], 'front', name='rack',
                       face_tex={'south': 'back', 'east': 'side', 'west': 'side', 'up': 'top', 'down': 'top'}))
    # glowing neon rails up both sides
    els.append(element([2.6, 0, z0], [3, RACK_H, z1], 'rail', lit=True, name='rail_left'))
    els.append(element([13, 0, z0], [13.4, RACK_H, z1], 'rail', lit=True, name='rail_right'))
    # the album art over the painted art window, cropped so the square art isn't stretched
    ax0, ax1 = 3 + 10 * ART_U[0], 3 + 10 * ART_U[1]
    ay0, ay1 = RACK_H * (1 - ART_V[1]), RACK_H * (1 - ART_V[0])
    aspect = (ax1 - ax0) / (ay1 - ay0)
    v = 16 / aspect
    els.append(element([ax0, ay0, z0 - 0.1], [ax1, ay1, z0], 'art', faces=('north',), lit=True,
                       uv=(0, (16 - v) / 2, 16, (16 + v) / 2), name='art'))
    # the quilted pad between her back and the rack
    els.append(element([3.5, 0, z1], [12.5, 12, -2], 'back', faces=('east', 'west', 'up', 'down'), name='pad'))
    # antenna with a glowing tip on the back-right corner of the top, clear of her head
    els.append(element([12.5, RACK_H, z0 + 0.5], [13, RACK_H + 5, z0 + 1], 'antenna', faces=sides, name='antenna'))
    els.append(element([12.25, RACK_H + 5, z0 + 0.25], [13.25, RACK_H + 6, z0 + 1.25], 'tip', faces=sides + ('up',), lit=True,
                       name='antenna_tip'))
    # shoulder straps: from the pad over the top of her body and down her chest
    for i, x in enumerate((5, 9)):
        els.append(element([x, 11, -2], [x + 2, 12, 2.2], 'strap', name=f'strap_over{i}'))
        els.append(element([x, 1, 2.2], [x + 2, 12, 2.6], 'strap', name=f'strap_chest{i}'))
    names = ('front', 'back', 'side', 'rail', 'strap', 'top', 'knob', 'art', 'antenna', 'tip')
    return {
        'credit': 'Midnight Machine (concept B, Synth Machine), painted by the user (texture sheet); '
                  'generated by tools/generate_midnight_machine_model.py',
        'textures': {k: f'fourtwenty:block/midnight_machine/{k}' for k in names} | {'particle': 'fourtwenty:block/midnight_machine/front'},
        'elements': els,
    }


def main():
    textures()
    MODEL.parent.mkdir(parents=True, exist_ok=True)
    MODEL.write_text(json.dumps(model(), indent=1) + '\n', encoding='utf-8')
    print('wrote', MODEL.relative_to(ROOT), 'and', len(list(TEX.glob('*.png'))), 'textures')


if __name__ == '__main__':
    main()

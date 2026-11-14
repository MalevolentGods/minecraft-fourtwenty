"""The Midnight Machine, concept B "Synth Machine" (picked by the user 2026-11-14), as a Minecraft model (spec 014 T017).

    python tools/generate_midnight_machine_model.py [album dir]

Writes assets/fourtwenty/models/midnight_machine/body.json (a standalone model, no item or block) and its textures in
textures/block/midnight_machine/ (the block atlas): the shell, trim, knob, strap and antenna, and the album art cut
from Art.png for the top panel. The six VU bars are not in the model: BoomboxLayer draws them, each its own band's
height, so they move with the music.

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
    TEX.mkdir(parents=True, exist_ok=True)
    # shell: midnight with a faint panel seam and corner screws
    im = Image.new('RGB', (16, 16), MIDNIGHT)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], outline=(44, 10, 58))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        d.point((x, y), fill=(90, 60, 100))
    d.line([(0, 8), (15, 8)], fill=(30, 4, 38))
    im.save(TEX / 'shell.png')
    # trim: hot pink with a brighter core line (drawn full-bright)
    im = Image.new('RGB', (16, 16), HOT)
    ImageDraw.Draw(im).line([(7, 0), (7, 15)], fill=(255, 140, 170))
    im.save(TEX / 'trim.png')
    Image.new('RGB', (16, 16), GRAPHITE).save(TEX / 'strap.png')
    im = Image.new('RGB', (16, 16), CREAM)
    ImageDraw.Draw(im).rectangle([6, 0, 9, 15], fill=(200, 196, 170))
    im.save(TEX / 'knob.png')
    im = Image.new('RGB', (16, 16), (210, 210, 220))
    im.save(TEX / 'antenna.png')
    im = Image.new('RGB', (16, 16), PINK)
    ImageDraw.Draw(im).rectangle([5, 5, 10, 10], fill=(255, 190, 200))
    im.save(TEX / 'tip.png')
    # the album art for the top panel: Art.png at 32x32, sharpened so the phoenix still reads
    art = Image.open(ALBUM / 'Art.png').convert('RGB').resize((32, 32), Image.LANCZOS).filter(ImageFilter.SHARPEN)
    art.save(TEX / 'art.png')
    # a lit frame round the art
    im = Image.new('RGB', (16, 16), GOLD)
    im.save(TEX / 'frame.png')


def element(frm, to, tex, faces=('north', 'south', 'east', 'west', 'up', 'down'), lit=False, uv_whole=False, name=None):
    e = {'from': [round(v, 3) for v in frm], 'to': [round(v, 3) for v in to], 'faces': {}}
    if name:
        e['name'] = name
    for f in faces:
        face = {'texture': f'#{tex}'}
        if uv_whole:
            face['uv'] = [0, 0, 16, 16]
        e['faces'][f] = face
    if lit:
        e['neoforge_data'] = {'block_light': 15, 'sky_light': 15}
    return e


def model():
    els = []
    # the rack: concept x -5..5, y 12..25, z 2..6 → model x 3..13, y 0..13, z -6..-2
    els.append(element([3, 0, -6], [13, 13, -2], 'shell', name='rack'))
    # glowing side rails, full height
    els.append(element([2.6, 0, -6], [3, 13, -2], 'trim', lit=True, name='rail_left'))
    els.append(element([13, 0, -6], [13.4, 13, -2], 'trim', lit=True, name='rail_right'))
    # the album art up top on the outward (north) face, in a lit gold frame
    els.append(element([4.25, 6.75, -6.35], [11.75, 12.25, -6.0], 'frame', faces=('north',), lit=True, name='art_frame'))
    els.append(element([4.5, 7, -6.6], [11.5, 12, -6.35], 'art', faces=('north',), lit=True, uv_whole=True, name='art'))
    # three knobs under the art
    for i, x in enumerate((5, 8, 11)):
        els.append(element([x - 0.5, 5.2, -6.8], [x + 0.5, 6, -6], 'knob', name=f'knob{i}'))
    # the VU window: a dark recess the bars rise in (BoomboxLayer draws the bars)
    els.append(element([3.4, 0.6, -6.2], [12.6, 4.9, -6.0], 'strap', faces=('north',), name='vu_window'))
    # antenna with a glowing tip
    els.append(element([11.5, 13, -4], [12, 19, -3.5], 'antenna', name='antenna'))
    els.append(element([11.25, 19, -4.25], [12.25, 20, -3.25], 'tip', lit=True, name='antenna_tip'))
    # shoulder straps: over the top of her body and down the chest (concept z -2.6..2.2 → model z -2.2..2.6)
    for i, x in enumerate((5, 9)):
        els.append(element([x, 11, -2.2], [x + 2, 12, 2.2], 'strap', name=f'strap_over{i}'))
        els.append(element([x, 1, 2.2], [x + 2, 12, 2.6], 'strap', name=f'strap_chest{i}'))
    return {
        'credit': 'Midnight Machine (concept B, Synth Machine), generated by tools/generate_midnight_machine_model.py',
        'textures': {k: f'fourtwenty:block/midnight_machine/{k}' for k in
                     ('shell', 'trim', 'strap', 'knob', 'antenna', 'tip', 'art', 'frame')} | {'particle': 'fourtwenty:block/midnight_machine/shell'},
        'elements': els,
    }


def main():
    textures()
    MODEL.parent.mkdir(parents=True, exist_ok=True)
    MODEL.write_text(json.dumps(model(), indent=1) + '\n', encoding='utf-8')
    print('wrote', MODEL.relative_to(ROOT), 'and', len(list(TEX.glob('*.png'))), 'textures')


if __name__ == '__main__':
    main()

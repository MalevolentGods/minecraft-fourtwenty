#!/usr/bin/env python3
"""Art for the Stoner Badge Sash (specs/016-weedcraft-handbook FR-035 to FR-040): one round patch per badge, the sash cloth, the item.

    python3 tools/generate_handbook_patches.py

Writes textures/gui/handbook/patches/<badge>.png (16x16 pixel-art patches, one colour and one emblem each), textures/entity/stoner_sash.png
(the cloth, 16x16) and textures/item/stoner_badge_sash.png. The patches show on the player's sash and, large, in the book.
"""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty/textures'

# badge id: (patch colour, emblem). Emblems are 7x7 pixel pictures drawn below.
BADGES = {
    'ganja_gardening': ((88, 160, 56), 'leaf'), 'plant_breeding': ((140, 90, 170), 'two'), 'bud_processing': ((200, 150, 60), 'scissors'),
    'kitchen_chemistry': ((60, 150, 170), 'flask'), 'munchies': ((200, 100, 70), 'cookie'), 'safe_sesh': ((110, 120, 200), 'cloud'),
    'colour_theory': ((210, 80, 140), 'drop'), 'terpination': ((70, 170, 130), 'vial'), 'inspiration': ((230, 190, 50), 'bolt'),
    'inspoweednetics': ((50, 110, 200), 'chip'), 'bits_and_bytes': ((90, 90, 110), 'bits'), 'hempcraft': ((120, 150, 70), 'block'),
    'community_sesh': ((200, 130, 90), 'house'), 'lifted_building': ((150, 200, 230), 'wings'),
}

EMBLEMS = {
    'leaf': ['...#...', '..###..', '.#####.', '..###..', '.#####.', '...#...', '...#...'],
    'two': ['.##.##.', '#..#..#', '#.....#', '.#...#.', '..#.#..', '...#...', '.......'],
    'scissors': ['#.....#', '.#...#.', '..#.#..', '...#...', '..#.#..', '.##.##.', '##...##'],
    'flask': ['..###..', '...#...', '...#...', '..###..', '.#####.', '#######', '.#####.'],
    'cookie': ['..###..', '.#.#.#.', '#######', '#.#.#.#', '#######', '.#.#.#.', '..###..'],
    'cloud': ['.......', '..##...', '.####.#', '#######', '#######', '.#####.', '.......'],
    'drop': ['...#...', '..###..', '..###..', '.#####.', '#######', '#######', '.#####.'],
    'vial': ['.#####.', '..#.#..', '..#.#..', '..#.#..', '.#...#.', '.#####.', '.......'],
    'bolt': ['....##.', '...##..', '..##...', '.#####.', '...##..', '..##...', '.##....'],
    'chip': ['.#.#.#.', '#######', '.#...#.', '##...##', '.#...#.', '#######', '.#.#.#.'],
    'bits': ['#.#..#.', '#.#.##.', '.#..#..', '###.###', '..#.#..', '.#..##.', '#...#.#'],
    'block': ['.#####.', '#.....#', '#.###.#', '#.#.#.#', '#.###.#', '#.....#', '.#####.'],
    'house': ['...#...', '..###..', '.#####.', '#######', '.##.##.', '.##.##.', '.##.##.'],
    'wings': ['#.....#', '##...##', '###.###', '#######', '.#####.', '..###..', '...#...'],
}


def patch(colour, emblem):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    dark = tuple(int(c * 0.55) for c in colour) + (255,)
    light = tuple(min(255, int(c * 1.25)) for c in colour) + (255,)
    d.ellipse((0, 0, 15, 15), fill=dark)
    d.ellipse((1, 1, 14, 14), fill=colour + (255,))
    d.ellipse((2, 2, 13, 13), outline=light)
    for y, row in enumerate(EMBLEMS[emblem]):
        for x, ch in enumerate(row):
            if ch == '#':
                img.putpixel((4 + x, 4 + y), (255, 255, 255, 255))
    return img


def main():
    out = ASSETS / 'gui/handbook/patches'
    out.mkdir(parents=True, exist_ok=True)
    for bid, (colour, emblem) in BADGES.items():
        patch(colour, emblem).save(out / f'{bid}.png')
    (ASSETS / 'entity').mkdir(parents=True, exist_ok=True)
    cloth = Image.new('RGBA', (16, 16), (46, 98, 44, 255))
    d = ImageDraw.Draw(cloth)
    d.rectangle((0, 0, 15, 1), fill=(214, 178, 84, 255))
    d.rectangle((0, 14, 15, 15), fill=(214, 178, 84, 255))
    cloth.save(ASSETS / 'entity/stoner_sash.png')
    item = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(item)
    for i in range(13):                          # a green sash across the icon, gold edged
        d.line((2 + i, 1 + i, 5 + i, 1 + i), fill=(46, 98, 44, 255))
        item.putpixel((1 + i, 1 + i), (214, 178, 84, 255))
        item.putpixel((6 + i if 6 + i < 16 else 15, 1 + i), (214, 178, 84, 255))
    for (x, y, c) in ((5, 4, (230, 190, 50)), (7, 7, (210, 80, 140)), (9, 10, (50, 110, 200))):
        d.ellipse((x - 1, y - 1, x + 2, y + 2), fill=c + (255,))
    item.save(ASSETS / 'item/stoner_badge_sash.png')
    print(len(BADGES), 'patches, the cloth and the item')


if __name__ == '__main__':
    main()

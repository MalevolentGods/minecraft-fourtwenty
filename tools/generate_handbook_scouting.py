#!/usr/bin/env python3
"""The Handbook's ranks and Stoner Badges (specs/016-weedcraft-handbook US6, requirements-draft.md, approved by the user 2026-11-23).

    python3 tools/generate_handbook_scouting.py

Writes assets/fourtwenty/handbook/scouting/<rank|badge>_<id>.json, scouting/index.json, the book entries that show them
(entries/scouting/*.md and the front matter), and the lang keys for every name and requirement. The wording lives here, in one place.

A requirement is (n, kind, text, deed, subject, count): kind 'T' is tracked by the game, 'H' is on your honour.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
SCOUT = ASSETS / 'handbook/scouting'
ENTRIES = ASSETS / 'handbook/en_us/entries/scouting'
LANG = ASSETS / 'lang/en_us.json'

F = 'fourtwenty:'
STASH = '|'.join(F + s for s in ('stash_jar', 'stash_box', 'mother_stash', 'the_vault'))
TRIMMERS = '|'.join(F + s for s in ('snip_station', 'bud_barber', 'trim_reaper'))
CURERS = '|'.join(F + s for s in ('dry_box', 'cure_closet', 'the_humidor'))


def T(n, text, deed, subject=None, count=1):
    return (n, 'T', text, deed, subject or {}, count)


def H(n, text):
    return (n, 'H', text, None, {}, 1)


# id, name, tagline, needs, teaches (a book entry), requirements
RANKS = [
    ('tenderbud', 'Tenderbud', 'Every great Weedcrafter started here, holding a seed, wondering which end goes down.', 'getting_started/welcome', [
        H('1', "Say the Weedcrafter's Oath, out loud if you dare."),
        T('2a', 'Hold a Weed Seed in your hand.', 'obtain', {'item': F + 'weed_seeds'}),
        T('2b', 'Plant a seed.', 'plant'),
        T('2c', 'Harvest a fully grown Landrace plant.', 'harvest', {'grade': '0'}),
        T('3a', 'Trim a Rough Bud.', 'trim'),
        T('3b', 'Cure a Trimmed Bud.', 'cure'),
        T('4a', 'Roll a Weed Joint.', 'craft', {'item': F + 'weed_joint'}),
        T('4b', 'Smoke it.', 'smoke', {'kind': 'joint'}),
        H('5', 'Explain to a friend why you always keep one seed back.')]),
    ('second_toke', 'Second Toke', 'Two smokes in: enough to have opinions, not yet enough to be right about them.', 'growing/strains', [
        T('1', 'Catch a colour at an Inspiration Infuser.', 'infuse', {'kind': 'catch'}),
        T('2', 'Infuse a Landrace seed into a family strain.', 'infuse', {'kind': 'seed'}),
        T('3', 'Grow and harvest a family strain.', 'harvest', {'grade': '1'}),
        T('4', 'Light a Hookah.', 'light'),
        T('5', 'Run a machine on Boundless Inspiration.', 'power'),
        T('6', 'Make a vial of terpene in a Terpinator.', 'terpinate'),
        T('7', 'Earn the Ganja Gardening badge.', 'earn', {'id': 'ganja_gardening'}),
        T('8', 'Have earned two Stoner Badges.', 'earn', {'kind': 'badge'}, 2),
        H('9', 'Show someone how to read a Wick with an Inspoweedometer.')]),
    ('first_hit', 'First Hit', 'Two strains walk into a Pheno Chamber.', 'growing/breeding', [
        T('1', 'Place a Pheno Chamber.', 'place', {'block': F + 'pheno_chamber'}),
        T('2', 'Breed a Cross.', 'pheno', {'grade': '2'}),
        T('3', 'Grow and harvest a Cross.', 'harvest', {'grade': '2'}),
        T('4', 'Trim a bud in a powered trimmer.', 'trim', {'block': TRIMMERS}),
        T('5', 'Cure a bud in a powered curer.', 'cure', {'block': CURERS}),
        T('6', 'Refine something at a Rosin Rig.', 'extract'),
        T('7', 'Take a hit from a Bong.', 'smoke', {'kind': 'bong'}),
        T('8', 'Earn the Plant Breeding badge.', 'earn', {'id': 'plant_breeding'}),
        T('9', 'Earn the Bud Processing badge.', 'earn', {'id': 'bud_processing'}),
        T('10', 'Have earned four Stoner Badges.', 'earn', {'kind': 'badge'}, 4)]),
    ('starseed', 'Starseed', "You've outgrown the dirt.", 'growing/grow_ops', [
        T('1', 'Place a Hydro Bed.', 'place', {'block': F + 'hydro_bed'}),
        T('2', 'Grow and harvest a Double Cross.', 'harvest', {'grade': '3'}),
        T('3', 'Get high on a Double Cross.', 'high', {'grade': '3'}),
        T('4', 'Place a Stash.', 'place', {'block': STASH}),
        T('5', 'Place a Hit Box.', 'place', {'block': F + 'hit_box'}),
        T('6', 'Earn the Inspiration badge.', 'earn', {'id': 'inspiration'}),
        T('7', 'Have earned six Stoner Badges.', 'earn', {'kind': 'badge'}, 6),
        H('8', 'Teach a new Weedcrafter one thing from this book.')]),
    ('high_life', 'High Life', 'Your workshop has a sofa now, and somebody is always on it.', 'growing/grow_ops', [
        T('1', 'Place an Aero Pod.', 'place', {'block': F + 'aero_pod'}),
        T('2', 'Harvest a crop in an Aero Pod.', 'harvest', {'block': F + 'aero_pod'}),
        T('3', 'Build a Buzz Bot.', 'craft', {'item': F + 'buzz_bot'}),
        T('4', 'Grow and harvest an Ultra.', 'harvest', {'grade': '4'}),
        T('5', 'Get high on an Ultra.', 'high', {'grade': '4'}),
        T('6', 'Build a circuit from the book that works.', 'circuit'),
        T('7', 'Earn the Inspoweednetics badge.', 'earn', {'id': 'inspoweednetics'}),
        T('8', 'Have earned eight Stoner Badges.', 'earn', {'kind': 'badge'}, 8)]),
    ('golden_eagle', 'Golden Eagle', 'The highest honour. It glows.', 'growing/strains', [
        T('1', 'Place a Cosmic Kush Pod.', 'place', {'block': F + 'cosmic_kush_pod'}),
        T('2', 'Grow and harvest a Golden God.', 'harvest', {'grade': '5'}),
        T('3', 'Get high on a Golden God.', 'high', {'grade': '5'}),
        T('4', 'Have earned ten Stoner Badges.', 'earn', {'kind': 'badge'}, 10),
        H('5', 'Share a joint with a friend and pass it left.'),
        H('6', 'Say the Weedcrafter\'s Law, and live up to it for a whole day.')]),
]

BADGES = [
    ('ganja_gardening', 'Ganja Gardening', None, 'growing/planting', [
        T('1', 'Plant 4 seeds.', 'plant', {}, 4),
        T('2', 'Harvest 10 plants.', 'harvest', {}, 10),
        T('3', 'Harvest a plant from a Closet Grow.', 'harvest', {'block': F + 'closet_grow'}),
        T('4', 'Grow a plant in a Pot Pot and harvest it.', 'harvest', {'block': F + 'potted_pot_pot_*'}),
        H('5', 'Tell a friend your best tip for a bigger harvest.')]),
    ('plant_breeding', 'Plant Breeding', None, 'growing/breeding', [
        T('1', 'Place a Pheno Chamber.', 'place', {'block': F + 'pheno_chamber'}),
        T('2', 'Breed a Cross.', 'pheno', {'grade': '2'}),
        T('3', 'Build a Buzz Bot.', 'craft', {'item': F + 'buzz_bot'}),
        T('4', 'Pollinate a crop.', 'pollinate'),
        T('5', 'Grow a Double Cross.', 'harvest', {'grade': '3'}),
        H('6', 'Tell your friends which of your seeds has the best traits, and why you think so.')]),
    ('bud_processing', 'Bud Processing', None, 'processing/trim_and_cure', [
        T('1', 'Trim 10 buds.', 'trim', {}, 10),
        T('2', 'Cure 10 buds.', 'cure', {}, 10),
        T('3', 'Trim a bud at a Snip Station or better.', 'trim', {'block': TRIMMERS}),
        T('4', 'Cure a bud at a Dry Box or better.', 'cure', {'block': CURERS}),
        T('5', 'Craft a Trim Tray.', 'craft', {'item': F + 'trim_tray'}),
        T('6', 'Craft a Curing Rack.', 'craft', {'item': F + 'curing_rack'})]),
    ('kitchen_chemistry', 'Kitchen Chemistry', None, 'processing/extracts', [
        T('1', 'Make Kief.', 'extract', {'item': F + 'kief'}),
        T('2', 'Make Hash.', 'extract', {'item': F + 'hash'}),
        T('3', 'Make Resin.', 'extract', {'item': F + 'resin'}),
        T('4', 'Make Rosin.', 'extract', {'item': F + 'rosin'}),
        T('5', 'Make Oil.', 'extract', {'item': F + 'oil'}),
        T('6', 'Craft a Vape Pen.', 'craft', {'item': F + 'vape_pen'})]),
    ('munchies', 'Munchies', None, 'processing/edibles', [
        T('1', 'Craft a Weed Cookie.', 'craft', {'item': F + 'weed_cookie'}),
        T('2', 'Craft a Weed Brownie.', 'craft', {'item': F + 'weed_brownie'}),
        T('3', 'Eat a Weed Cookie.', 'smoke', {'kind': 'cookie'}),
        T('4', 'Eat a Weed Brownie.', 'smoke', {'kind': 'brownie'}),
        H('5', 'Share your snacks. The Law says so.')]),
    ('safe_sesh', 'Safe Sesh', None, 'processing/smoking', [
        T('1', 'Smoke a joint.', 'smoke', {'kind': 'joint'}),
        T('2', 'Smoke a pipe.', 'smoke', {'kind': 'pipe'}),
        T('3', 'Take a hit from a Bong.', 'smoke', {'kind': 'bong'}),
        T('4', 'Light a Hookah.', 'light'),
        T('5', 'Take a hit from a Vape Pen.', 'smoke', {'kind': 'vape'}),
        T('6', 'Take a hit from a specialty bong.', 'smoke', {'kind': 'specialty_bong'}),
        H('7', 'Pass it left.')]),
    ('colour_theory', 'Colour Theory', None, 'colours/terpenes', [
        T('1', 'Catch your first colour at an Infuser.', 'infuse', {'kind': 'catch'}),
        T('2', 'Discover 8 terpenes.', 'discover', {}, 8),
        T('3', 'Discover 16 terpenes.', 'discover', {}, 16),
        T('4', 'Infuse a strain seed.', 'infuse', {'kind': 'seed'}),
        T('5', 'Dye a hemp block with a terpene.', 'craft', {'item': F + 'hemp_*'})]),
    ('terpination', 'Terpination', None, 'terpinator/using', [
        T('1', 'Place a Terpinator.', 'place', {'block': F + 'terpinator|' + F + 'terpinator_lab'}),
        T('2', 'Make a vial of terpene.', 'terpinate'),
        T('3', 'Make 10 vials.', 'terpinate', {}, 10),
        T('4', 'Run a Terpinator on Boundless Inspiration.', 'power', {'block': F + 'terpinator*'}),
        T('5', 'Dye 8 hemp blocks.', 'craft', {'item': F + 'hemp_*'}, 8)]),
    ('inspiration', 'Inspiration', None, 'power/basics', [
        T('1', 'Place a Hookah.', 'place', {'block': F + 'weed_bong'}),
        T('2', 'Run a machine on Boundless Inspiration.', 'power'),
        T('3', 'Place a Stash.', 'place', {'block': STASH}),
        T('4', 'Place a Hit Box.', 'place', {'block': F + 'hit_box'}),
        T('5', 'Burn out a Wick. (Everyone does.)', 'overload'),
        T('6', 'Place a Double Hose.', 'place', {'block': F + 'double_hose'})]),
    ('inspoweednetics', 'Inspoweednetics', None, 'electronics/welcome', [
        T('1', 'Hold an Inspoweedometer.', 'measure'),
        T('2', 'Build the switched light.', 'circuit', {'id': 'light'}),
        T('3', 'Build the inverter.', 'circuit', {'id': 'inverter'}),
        T('4', 'Build the two-switch lock.', 'circuit', {'id': 'and_lock'}),
        T('5', 'Build the one-bit memory.', 'circuit', {'id': 'latch'}),
        T('6', 'Build the blinking clock.', 'circuit', {'id': 'clock'}),
        T('7', 'Build the half adder.', 'circuit', {'id': 'adder'})]),
    ('bits_and_bytes', 'Bits & Bytes', None, 'electronics/binary', [
        T('1', 'Place a Lit Bit.', 'place', {'block': F + 'lit_bit'}),
        T('2', 'Place a Weedweeno.', 'place', {'block': F + 'weedweeno'}),
        T('3', 'Place a Stacker Nug.', 'place', {'block': F + 'stacker_nug'}),
        T('4', 'Place a Numb Nug.', 'place', {'block': F + 'numb_nug'}),
        H('5', 'Write the number 13 in binary, on paper, from memory.'),
        H('6', 'Explain to a friend what an AND gate does.')]),
    ('hempcraft', 'Hempcraft', None, 'colours/terpenes', [
        T('1', 'Craft 4 hemp blocks.', 'craft', {'item': F + 'hemp_*'}, 4),
        T('2', 'Place a lamp.', 'place', {'block': F + '*lamp*'}),
        T('3', 'Place hemp glass.', 'place', {'block': F + 'glass_*'}),
        H('4', "Build something you're proud of from hemp, and show someone.")]),
    ('community_sesh', 'Community Sesh', 'minecolonies', 'colony/dispensary', [
        H('1', 'Hire a Budtender.'),
        T('2', 'Have a colonist enjoy a dose.', 'colony_dose'),
        T('3', 'Sell a joint to a customer.', 'colony_sale'),
        H('4', 'Ask a colonist how they are feeling.')]),
    ('lifted_building', 'Lifted Building', 'minecolonies', 'colony/lifted_builders', [
        T('1', 'Have a Lifted Builder.', 'colony_lifted'),
        T('2', 'Hear the Midnight Machine.', 'colony_music'),
        H('3', 'Watch a Lifted Builder finish a building.'),
        H('4', 'Dance.')]),
]
# ranks carry no 'needs'; badges' third field is theirs
BADGE_NEEDS = {b[0]: b[2] for b in BADGES}


def write_all():
    SCOUT.mkdir(parents=True, exist_ok=True)
    ENTRIES.mkdir(parents=True, exist_ok=True)
    for old in SCOUT.glob('*.json'):
        old.unlink()
    lang = {}
    index = []
    total = tracked = 0

    def req_json(did, rank, reqs):
        nonlocal total, tracked
        out = []
        for (n, kind, text, deed, subject, count) in reqs:
            key = f"{'rank' if rank else 'badge'}.{did}.{n}"
            lang[f'handbook.fourtwenty.req.{key}'] = text
            r = {'n': n, 'kind': 'tracked' if kind == 'T' else 'honour', 'text': f'handbook.fourtwenty.req.{key}'}
            if kind == 'T':
                r['deed'] = deed
                if subject:
                    r['subject'] = subject
                if count > 1:
                    r['count'] = count
            out.append(r)
            total += 1
            tracked += kind == 'T'
        return out

    for order, (rid, name, tagline, teaches, reqs) in enumerate(RANKS, 1):
        lang[f'handbook.fourtwenty.rank.{rid}'] = name
        lang[f'handbook.fourtwenty.rank.{rid}.tagline'] = tagline
        d = {'id': rid, 'kind': 'rank', 'order': order, 'name': f'handbook.fourtwenty.rank.{rid}', 'entry': teaches,
             'requirements': req_json(rid, True, reqs)}
        (SCOUT / f'rank_{rid}.json').write_text(json.dumps(d, indent=2) + '\n', encoding='utf-8')
        index.append(f'rank_{rid}')
    for bid, name, needs, teaches, reqs in BADGES:
        lang[f'handbook.fourtwenty.badge.{bid}'] = name
        d = {'id': bid, 'kind': 'badge', 'name': f'handbook.fourtwenty.badge.{bid}', 'entry': teaches,
             'requirements': req_json(bid, False, reqs)}
        if needs:
            d['needs'] = needs
        (SCOUT / f'badge_{bid}.json').write_text(json.dumps(d, indent=2) + '\n', encoding='utf-8')
        index.append(f'badge_{bid}')
    (SCOUT / 'index.json').write_text(json.dumps({'defs': index}, indent=2) + '\n', encoding='utf-8')

    # the book entries that show them
    for rid, name, tagline, teaches, reqs in RANKS:
        (ENTRIES / f'rank_{rid}.md').write_text(
            f"---\ntitle: {name}\nicon: fourtwenty:handbook\nterms: [{name}]\n---\n*{tagline}*\n\n{{scout:rank/{rid}}}\n", encoding='utf-8')
    for bid, name, needs, teaches, reqs in BADGES:
        head = f"---\ntitle: {name}\nicon: fourtwenty:handbook\nterms: [{name}]\n" + (f"needs: {needs}\n" if needs else '') + "---\n"
        (ENTRIES / f'badge_{bid}.md').write_text(head + f"{{scout:badge/{bid}}}\n", encoding='utf-8')

    # the front matter (motto, oath, law) and the overview
    (ENTRIES / 'motto.md').write_text("""---
title: Motto and Slogan
icon: fourtwenty:handbook
terms: [motto, slogan, Keep One Back]
---
Every Weedcrafter carries two pieces of advice, and they are the best two there are. The first is printed inside the front cover of this book, so you see it every time you open it.

# The Motto
**Keep One Back.**

One seed in your pocket when you roll the rest. One charcoal for tomorrow's Hookah. One spare Wick for when this one goes up in smoke, which it will. Every disaster in the history of Weedcraft started with somebody using the last one.

# The Slogan
**Smoke Weed Every Day.**

(Responsibly. With a snack nearby. And, see above, with one kept back.)
""", encoding='utf-8')
    (ENTRIES / 'oath.md').write_text("""---
title: The Weedcrafter's Oath
icon: fourtwenty:handbook
terms: [Oath]
---
Say it aloud, with a hand on your heart or your Hookah, whichever is nearer:

*On my honour I will do my best*
*to grow my best,*
*to share my stash,*
*to keep my fire lit and my neighbours fed,*
*and to keep one seed back for tomorrow.*

You don't have to mean it yet. You'll get there.
""", encoding='utf-8')
    (ENTRIES / 'law.md').write_text("""---
title: The Weedcrafter's Law
icon: fourtwenty:handbook
terms: [Law]
---
A Weedcrafter is:

- **Chill.** Panicking has never once helped a plant.
- **Generous.** Pass it left, and don't be the one who holds.
- **Patient.** The plant takes the time it takes.
- **Curious.** Read the book. Then try the other thing.
- **Tidy.** Clean your tray. Put your Wicks away.
- **Helpful.** Leave every workshop a little better than you found it.
- **Respectful of Elders.** The old strains earned their place on the ladder.
- **Kind to Creepers (from a distance).** A long distance.
- **Thrifty with Seeds.** One back, always.
- **Prepared.** Bring snacks.
- **Cheerful.** Even when it's smoking.
- **Lifted.** Lightly, and in good company.
""", encoding='utf-8')
    (ENTRIES / 'sash.md').write_text("""---
title: The Stoner Badge Sash
icon: fourtwenty:stoner_badge_sash
terms: [Stoner Badge Sash, sash, patches]
---
Every badge you earn deserves somewhere to be seen.

{item:fourtwenty:stoner_badge_sash|Worn across the chest, it shows your badges}

{recipe:fourtwenty:stoner_badge_sash}

Right-click it to put it on. It has a place of its own, just above your off-hand in your inventory, so it goes over your armour, not instead of it: click that place with the sash to put it on, or with an empty hand to take it off. It drops where you fall, like armour. It does nothing but look good: no armour, no effects. It carries no badges of its own, so it always shows the badges of whoever's wearing it. Lend it to a friend and it shows theirs. (Earn your own.)

The newest eight show on the sash itself. Every badge you've earned is in colour below, and the rest are waiting.

{scout:sash}
""", encoding='utf-8')
    (ENTRIES / 'overview.md').write_text("""---
title: Your Progress
icon: fourtwenty:handbook
terms: [ranks, Stoner Badges, progress]
---
Six **ranks**, earned in order from Tenderbud to Golden Eagle, and a pocketful of **Stoner Badges**, earned in any order. Most requirements tick themselves off as you do them, with a small notice. The rest you sign on your honour: click the box.

Turn the page for each rank and badge in full.

{scout:overview}
""", encoding='utf-8')
    # the chapter in book.json, after Getting Started
    book_path = ASSETS / 'handbook/book.json'
    book = json.loads(book_path.read_text(encoding='utf-8'))
    book['chapters'] = [c for c in book['chapters'] if c['id'] != 'scouting']
    chapter = {'id': 'scouting', 'title': 'handbook.fourtwenty.chapter.scouting', 'icon': 'fourtwenty:handbook',
               'entries': ['motto', 'oath', 'law', 'overview', 'sash'] + [f'rank_{r[0]}' for r in RANKS] + [f'badge_{b[0]}' for b in BADGES]}
    at = next((i for i, c in enumerate(book['chapters']) if c['id'] == 'getting_started'), -1) + 1
    book['chapters'].insert(at, chapter)
    book_path.write_text(json.dumps(book, indent=2) + '\n', encoding='utf-8')

    # merge the lang keys, keeping the file's order and adding ours at the end
    cur = json.loads(LANG.read_text(encoding='utf-8'))
    for k in [k for k in cur if k.startswith(('handbook.fourtwenty.req.', 'handbook.fourtwenty.rank.', 'handbook.fourtwenty.badge.'))]:
        del cur[k]
    cur.update(lang)
    LANG.write_text('{\n' + ',\n'.join(f'  {json.dumps(k, ensure_ascii=False)}: {json.dumps(v, ensure_ascii=False)}' for k, v in cur.items()) + '\n}\n',
                    encoding='utf-8')
    print(f'{len(RANKS)} ranks, {len(BADGES)} badges, {total} requirements, {tracked} tracked ({100 * tracked // total}%)')


if __name__ == '__main__':
    write_all()

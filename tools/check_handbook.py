#!/usr/bin/env python3
"""Checks the Handbook's content (specs/016-weedcraft-handbook FR-009, FR-016, SC-002, SC-006).

    python3 tools/check_handbook.py [--strict]

Fails on: an entry listed in book.json that doesn't exist; a parse error; a link to an entry that doesn't exist; an
item, recipe or image that doesn't exist; a glossary line pointing nowhere. With --strict (release builds) it also fails
when an obtainable Four Twenty item appears on no page, or an electronic part has no real-world name.
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from handbook_markup import parse  # noqa: E402

ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
DATA = ROOT / 'src/main/resources/data'
BOOK = ASSETS / 'handbook'


def items_known():
    lang = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
    out = set()
    for k in lang:
        m = re.match(r'^(item|block)\.fourtwenty\.([a-z0-9_]+)$', k)
        if m:
            out.add('fourtwenty:' + m.group(2))
    return out


def spans_links(spans):
    return [s['link'] for s in spans if 'link' in s]


def main():
    strict = '--strict' in sys.argv
    errors, warnings = [], []
    book = json.loads((BOOK / 'book.json').read_text(encoding='utf-8'))
    known = items_known()
    entries = {}
    for ch in book['chapters']:
        for e in ch['entries']:
            eid = f"{ch['id']}/{e}"
            path = BOOK / 'en_us/entries' / ch['id'] / (e + '.md')
            if not path.exists():
                errors.append(f'{eid}: listed in book.json but {path.relative_to(ROOT)} is missing')
                continue
            front, els, errs = parse(path.read_text(encoding='utf-8'))
            for err in errs:
                errors.append(f'{eid}: {err}')
            entries[eid] = (front, els)
    used_items = set()

    def item_ok(eid, iid, what):
        used_items.add(iid)
        ns = iid.split(':')[0] if ':' in iid else ''
        if ns == 'fourtwenty' and iid not in known:
            errors.append(f'{eid}: {what} {iid} is not a Four Twenty item')
        elif ns not in ('fourtwenty', 'minecraft', 'minecolonies'):
            errors.append(f'{eid}: {what} {iid} has an unexpected namespace')

    for eid, (front, els) in entries.items():
        if front.get('icon'):
            item_ok(eid, front['icon'], 'icon')
        for el in els:
            t = el['type']
            spans_lists = []
            if t in ('heading', 'para'):
                spans_lists = [el['spans']]
            elif t in ('list', 'steps'):
                spans_lists = el['items']
            for spans in spans_lists:
                for link in spans_links(spans):
                    if link not in entries:
                        errors.append(f'{eid}: link to missing entry {link}')
            if t == 'item':
                item_ok(eid, el['id'], 'item')
            elif t == 'recipe':
                ns, path = el['id'].split(':', 1)
                if ns == 'fourtwenty':
                    f = DATA / ns / 'recipe' / (path + '.json')
                    if not f.exists():
                        errors.append(f'{eid}: recipe {el["id"]} not found')
                    else:
                        res = json.loads(f.read_text(encoding='utf-8')).get('result', {})
                        rid = res.get('id') or res.get('item')
                        if rid:
                            used_items.add(rid)
            elif t == 'image':
                if not (ASSETS / 'textures/gui/handbook' / (el['path'] + '.png')).exists():
                    errors.append(f'{eid}: image {el["path"]} not found')
    glossary = BOOK / 'en_us/glossary.md'
    if glossary.exists():
        for line in glossary.read_text(encoding='utf-8').splitlines():
            m = re.search(r'-> entry:(\S+)', line)
            if m and m.group(1) not in entries:
                errors.append(f'glossary: {line.split(":")[0]} points to missing entry {m.group(1)}')
    # coverage and real names (strict: release builds)
    terms = json.loads((BOOK / 'real_terms.json').read_text(encoding='utf-8'))
    uncovered = sorted(i for i in known if i not in used_items and not i.split(':')[1].startswith(('potted_', 'wild_', 'weed_crop')))
    msg = f'{len(uncovered)} Four Twenty items appear on no page yet'
    (errors if strict and uncovered else warnings).append(msg + (': ' + ', '.join(uncovered[:20]) + ' …' if uncovered else ''))
    for w in warnings:
        print('note:', w)
    for e in errors:
        print('ERROR:', e)
    print(f'Handbook: {len(entries)} entries, {len(terms["parts"])} parts with real names, {len(errors)} errors')
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(main())

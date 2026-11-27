"""What the printed Handbook is made from, as one fingerprint (specs/016-weedcraft-handbook T064, book/PDF parity).

The PDF builder records the fingerprint of the sources it printed in docs/handbook/pdf-sources.json; tools/check_handbook.py
recomputes it and fails when they differ, so the committed PDF can never fall behind the in-game book. Sources: everything
under assets/fourtwenty/handbook (book.json, entries, glossary, scouting), the Handbook's pictures, and the Handbook's lang
keys. Text is hashed with Unix line endings, so a Windows checkout and CI agree.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
RECORD = ROOT / 'docs/handbook/pdf-sources.json'
TEXT = {'.md', '.json', '.txt'}


def fingerprint():
    h = hashlib.sha256()
    files = sorted(p for d in (ASSETS / 'handbook', ASSETS / 'textures/gui/handbook') for p in d.rglob('*') if p.is_file())
    for p in files:
        data = p.read_bytes()
        if p.suffix in TEXT:
            data = data.replace(b'\r\n', b'\n')
        h.update(p.relative_to(ASSETS).as_posix().encode() + b'\0' + hashlib.sha256(data).digest())
    lang = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
    for k in sorted(k for k in lang if k.startswith(('handbook.', 'item.fourtwenty.', 'block.fourtwenty.'))):
        h.update(f'{k}={lang[k]}\n'.encode())
    return h.hexdigest()


def record():
    RECORD.write_text(json.dumps({'fingerprint': fingerprint()}, indent=2) + '\n', encoding='utf-8')


def recorded():
    try:
        return json.loads(RECORD.read_text(encoding='utf-8')).get('fingerprint')
    except (OSError, ValueError):
        return None

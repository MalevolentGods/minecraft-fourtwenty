"""The Handbook's entry markup (specs/016-weedcraft-handbook, contracts/book-markup.md), read the same way the game
reads it (domain/handbook/Markup.java). Both are checked against src/test/resources/handbook/markup-samples.

    from handbook_markup import parse        # -> (front, elements, errors)
    python3 tools/handbook_markup.py <file.md>   # prints the canonical JSON
"""
import json
import re
import sys

BLOCK_TAG = re.compile(r'^\{(item|recipe|image|truth|table|page)(?::([^}]*))?\}$')
STEP = re.compile(r'^(\d+)\.\s+(.*)$')
GATES = ['NOT', 'AND', 'OR', 'NAND', 'NOR', 'XOR', 'XNOR']
COLOURS = ['lime', 'myr', 'lav', 'pin', 'cream', 'gold', 'red', 'gray']


def _list(value):
    v = value.strip()
    if v.startswith('[') and v.endswith(']'):
        v = v[1:-1]
    return [s.strip() for s in v.split(',') if s.strip()]


def parse(text):
    errors = []
    lines = text.replace('\r\n', '\n').replace('\r', '\n').split('\n')
    i = 0
    front = {'title': None, 'icon': None, 'terms': [], 'needs': None}
    if lines and lines[0].strip() == '---':
        i = 1
        while i < len(lines) and lines[i].strip() != '---':
            line = lines[i]
            i += 1
            if ':' not in line:
                if line.strip():
                    errors.append('front matter line without a key: ' + line)
                continue
            key, value = line.split(':', 1)
            key, value = key.strip(), value.strip()
            if ' #' in value:
                value = value[:value.index(' #')].strip()
            if key == 'title':
                front['title'] = value
            elif key == 'icon':
                front['icon'] = value
            elif key == 'needs':
                front['needs'] = value or None
            elif key == 'terms':
                front['terms'] = _list(value)
            else:
                errors.append('unknown front matter key: ' + key)
        if i >= len(lines):
            errors.append('front matter not closed')
        i += 1
    out, para = [], []
    lst = steps = None
    step_start = 1
    while i < len(lines):
        line = lines[i].strip()
        is_list = line.startswith('- ')
        step = STEP.match(line)
        if not is_list and lst is not None:
            out.append({'type': 'list', 'items': lst})
            lst = None
        if not step and steps is not None:
            out.append({'type': 'steps', 'start': step_start, 'items': steps})
            steps = None
        block = (not line or line.startswith('#') or is_list or step or (line.startswith('{') and BLOCK_TAG.match(line))
                 or line == '{/table}' or line.startswith('{table'))
        if block and para:
            out.append({'type': 'para', 'spans': spans(' '.join(para), errors)})
            para = []
        if not line:
            i += 1
            continue
        if line.startswith('# '):
            out.append({'type': 'heading', 'spans': spans(line[2:].strip(), errors)})
        elif is_list:
            lst = lst or []
            lst.append(spans(line[2:].strip(), errors))
        elif step:
            if steps is None:
                steps, step_start = [], int(step.group(1))
            steps.append(spans(step.group(2).strip(), errors))
        elif BLOCK_TAG.match(line):
            m = BLOCK_TAG.match(line)
            name, arg = m.group(1), m.group(2) or ''
            parts = arg.split('|')
            if name == 'item':
                out.append({'type': 'item', 'id': parts[0], 'caption': parts[1] if len(parts) > 1 else ''})
            elif name == 'recipe':
                out.append({'type': 'recipe', 'id': parts[0]})
            elif name == 'image':
                out.append({'type': 'image', 'path': parts[0], 'caption': parts[1] if len(parts) > 1 else '',
                            'size': parts[2] if len(parts) > 2 else 'normal'})
            elif name == 'truth':
                if arg not in GATES:
                    errors.append('unknown gate: ' + arg)
                out.append({'type': 'truth', 'gate': arg})
            elif name == 'page':
                out.append({'type': 'page'})
            elif name == 'table':
                if arg:
                    if arg not in ('parts', 'units'):
                        errors.append('unknown table: ' + arg)
                    out.append({'type': 'table', 'special': arg, 'rows': []})
                else:
                    rows = []
                    i += 1
                    while i < len(lines) and lines[i].strip() != '{/table}':
                        row = lines[i].strip()
                        i += 1
                        if not row:
                            continue
                        if not row.startswith('|'):
                            errors.append('table row without |: ' + row)
                            continue
                        inner = row[1:-1] if row.endswith('|') and len(row) > 1 else row[1:]
                        rows.append([c.strip() for c in inner.split('|')])
                    if i >= len(lines):
                        errors.append('table not closed')
                    out.append({'type': 'table', 'special': '', 'rows': rows})
        elif line == '{/table}':
            errors.append('{/table} without {table}')
        elif line.startswith('{') and not line.startswith('{c:') and not line.startswith('{spoiler:'):
            errors.append('unknown tag line: ' + line)
        else:
            para.append(line)
        i += 1
    if para:
        out.append({'type': 'para', 'spans': spans(' '.join(para), errors)})
    if lst is not None:
        out.append({'type': 'list', 'items': lst})
    if steps is not None:
        out.append({'type': 'steps', 'start': step_start, 'items': steps})
    if front['title'] is None:
        errors.append('no title')
    return front, out, errors


def spans(text, errors):
    out, cur = [], []
    st = {'b': False, 'i': False, 'c': None, 's': None}

    def flush(link=None):
        if not cur:
            return
        s = {'t': ''.join(cur)}
        if st['b']:
            s['b'] = True
        if st['i']:
            s['i'] = True
        if st['c'] is not None:
            s['c'] = st['c']
        if link is not None:
            s['link'] = link
        if st['s'] is not None:
            s['spoiler'] = st['s']
        out.append(s)
        cur.clear()

    i = 0
    while i < len(text):
        c = text[i]
        if text.startswith('**', i):
            flush()
            st['b'] = not st['b']
            i += 2
        elif c == '*':
            flush()
            st['i'] = not st['i']
            i += 1
        elif text.startswith('{c:', i):
            end = text.find('}', i)
            if end < 0:
                errors.append('unclosed {c:')
                cur.append(text[i:])
                break
            flush()
            st['c'] = text[i + 3:end]
            if st['c'] not in COLOURS:
                errors.append('unknown colour: ' + st['c'])
            i = end + 1
        elif text.startswith('{/c}', i):
            flush()
            if st['c'] is None:
                errors.append('{/c} without {c:}')
            st['c'] = None
            i += 4
        elif text.startswith('{spoiler:terpene:', i):
            end = text.find('}', i)
            if end < 0:
                errors.append('unclosed {spoiler:')
                cur.append(text[i:])
                break
            flush()
            st['s'] = text[i + len('{spoiler:terpene:'):end]
            i = end + 1
        elif text.startswith('{/spoiler}', i):
            flush()
            if st['s'] is None:
                errors.append('{/spoiler} without {spoiler:}')
            st['s'] = None
            i += len('{/spoiler}')
        elif c == '[':
            mid = text.find('](entry:', i)
            end = text.find(')', mid) if mid >= 0 else -1
            if mid < 0 or end < 0:
                cur.append(c)
                i += 1
                continue
            flush()
            cur.append(text[i + 1:mid])
            flush(text[mid + len('](entry:'):end])
            i = end + 1
        else:
            cur.append(c)
            i += 1
    flush()
    if st['b']:
        errors.append('unclosed **')
    if st['i']:
        errors.append('unclosed *')
    if st['c'] is not None:
        errors.append('unclosed {c:' + st['c'] + '}')
    if st['s'] is not None:
        errors.append('unclosed {spoiler:}')
    return out


def canonical(elements):
    """The same JSON the game's Element.json writes."""
    return json.dumps(elements, separators=(',', ':'), ensure_ascii=False)


if __name__ == '__main__':
    front, els, errs = parse(open(sys.argv[1], encoding='utf-8').read())
    print(json.dumps({'front': front, 'elements': els, 'errors': errs}, indent=1, ensure_ascii=False))

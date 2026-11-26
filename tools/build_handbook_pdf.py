#!/usr/bin/env python3
"""The PDF edition of A Handbook of Weedcraft, Scouting, and Weed-craft (specs/016-weedcraft-handbook FR-030 to FR-034).

    python3 tools/build_handbook_pdf.py [out.pdf]        # default docs/handbook/A-Handbook-of-Weedcraft.pdf

Built from the same files as the in-game book (assets/fourtwenty/handbook, read by tools/handbook_markup.py), so the
two never disagree: the user's cover, DON'T PANIC alone on the inside cover, the title page, a clickable contents
page, every entry with its pictures and real recipes, and PDF bookmarks. Terpene names stay hidden (FR-032).

Item pictures: tools/art/handbook_icons/<namespace>_<id>.png when exported from the game (research R6), else the item's
own texture (a block's first texture), else a blank tile. Vanilla textures are read from the Minecraft 1.21.1 client jar
(MINECRAFT_JAR, or the CurseForge install).
"""
import io
import json
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import inch
from reportlab.lib.utils import ImageReader
from reportlab.platypus import (BaseDocTemplate, Flowable, Frame, KeepTogether, NextPageTemplate, PageBreak, PageTemplate,
                                Paragraph, Spacer, Table, TableStyle)
from reportlab.platypus.tableofcontents import TableOfContents

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / 'tools'))
from handbook_markup import parse  # noqa: E402

ASSETS = ROOT / 'src/main/resources/assets/fourtwenty'
DATA = ROOT / 'src/main/resources/data'
BOOK = ASSETS / 'handbook'
ICONS = ROOT / 'tools/art/handbook_icons'
COVER = ROOT / 'tools/art/handbook_cover.png'
OUT = ROOT / 'docs/handbook/A-Handbook-of-Weedcraft.pdf'
MC_JAR = Path(os.environ.get('MINECRAFT_JAR', r'C:/Users/Ignus/curseforge/minecraft/Install/versions/1.21.1/1.21.1.jar'))

PAGE_W, PAGE_H = 6 * inch, 9 * inch
PAPER = colors.HexColor('#F4EAD0')
PAPER_EDGE = colors.HexColor('#E2D4B2')
INK = colors.HexColor('#3A2E20')
HEADING = colors.HexColor('#2F5A1A')
LINKC = colors.HexColor('#2E7A1A')
FAINT = colors.HexColor('#8A7A5A')
GOLD = colors.HexColor('#C9A24A')
LEAF = colors.HexColor('#7CC540')
PALETTE = {'lime': '#3E7D1B', 'myr': '#6B3E8E', 'lav': '#7B5EA7', 'pin': '#2E6B5A', 'cream': '#9C8A5A',
           'gold': '#A07818', 'red': '#A03020', 'gray': '#6E6A60'}

LANG = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))

# ---- type

BODY = ParagraphStyle('body', fontName='Times-Roman', fontSize=10.5, leading=14, textColor=INK, spaceAfter=5,
                      allowWidows=0, allowOrphans=0)        # never one line alone at a page's foot or head
H1 = ParagraphStyle('h1', fontName='Times-Bold', fontSize=19, leading=23, textColor=HEADING, spaceAfter=4)
H2 = ParagraphStyle('h2', fontName='Times-Bold', fontSize=12.5, leading=16, textColor=HEADING, spaceBefore=6, spaceAfter=3,
                    keepWithNext=1)
CHAPTER = ParagraphStyle('chapter', fontName='Times-Bold', fontSize=26, leading=30, textColor=HEADING, alignment=TA_CENTER)
CAPTION = ParagraphStyle('caption', parent=BODY, fontName='Times-Italic', fontSize=9.5, leading=12, textColor=FAINT)
LISTS = ParagraphStyle('list', parent=BODY, leftIndent=16, bulletIndent=4, spaceAfter=2)
CELL = ParagraphStyle('cell', parent=BODY, fontSize=9.5, leading=12, spaceAfter=0)
CELLB = ParagraphStyle('cellb', parent=CELL, fontName='Times-Bold', textColor=HEADING)


def esc(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def anchor(entry_id):
    return 'e_' + entry_id.replace('/', '_')


def markup(spans):
    """Spans to reportlab paragraph markup. Terpene spoilers are never printed (FR-032)."""
    out = []
    for s in spans:
        if 'spoiler' in s:
            out.append('<i>(a terpene you discover in game)</i>')
            continue
        t = esc(s['t'])
        if s.get('b'):
            t = f'<b>{t}</b>'
        if s.get('i'):
            t = f'<i>{t}</i>'
        if 'c' in s and s['c'] in PALETTE:
            t = f'<font color="{PALETTE[s["c"]]}">{t}</font>'
        if 'link' in s:
            t = f'<a href="#{anchor(s["link"])}" color="#2E7A1A"><u>{t}</u></a>'
        out.append(t)
    return ''.join(out)


# ---- item pictures

_jar = None


def jar():
    global _jar
    if _jar is None:
        _jar = zipfile.ZipFile(MC_JAR) if MC_JAR.exists() else False
    return _jar


def _read(ns, path):
    if ns == 'fourtwenty':
        f = ASSETS / path
        return Image.open(f).convert('RGBA') if f.exists() else None
    j = jar()
    if not j:
        return None
    try:
        return Image.open(io.BytesIO(j.read(f'assets/{ns}/{path}'))).convert('RGBA')
    except KeyError:
        return None


def _model(ns, path):
    if ns == 'fourtwenty':
        f = ASSETS / 'models' / (path + '.json')
        return json.loads(f.read_text(encoding='utf-8')) if f.exists() else None
    j = jar()
    try:
        return json.loads(j.read(f'assets/{ns}/models/{path}.json')) if j else None
    except KeyError:
        return None


def _texture_of_model(ns, path, depth=0):
    m = _model(ns, path)
    if not m or depth > 4:
        return None
    tex = m.get('textures', {})
    for key in ('layer0', 'top', 'front', 'all', 'side', 'texture', 'particle'):
        if key in tex and not tex[key].startswith('#'):
            tns, tpath = tex[key].split(':', 1) if ':' in tex[key] else ('minecraft', tex[key])
            img = _read(tns, f'textures/{tpath}.png')
            if img:
                return img
    for v in tex.values():
        if not v.startswith('#'):
            tns, tpath = v.split(':', 1) if ':' in v else ('minecraft', v)
            img = _read(tns, f'textures/{tpath}.png')
            if img:
                return img
    parent = m.get('parent')
    if parent:
        pns, ppath = parent.split(':', 1) if ':' in parent else ('minecraft', parent)
        return _texture_of_model(pns, ppath, depth + 1)
    return None


_icon_cache = {}


def icon(item_id):
    """A crisp 128px picture of an item."""
    if item_id in _icon_cache:
        return _icon_cache[item_id]
    ns, name = item_id.split(':', 1) if ':' in item_id else ('minecraft', item_id)
    exported = ICONS / f'{ns}_{name}.png'
    img = Image.open(exported).convert('RGBA') if exported.exists() else None
    if img is None:
        img = _texture_of_model(ns, f'item/{name}') or _read(ns, f'textures/item/{name}.png') or _read(ns, f'textures/block/{name}.png')
    if img is None:
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    if img.height > img.width:          # an animated strip: its first frame
        img = img.crop((0, 0, img.width, img.width))
    big = img.resize((128, 128), Image.NEAREST)
    buf = io.BytesIO()
    big.save(buf, 'PNG')
    buf.seek(0)
    _icon_cache[item_id] = ImageReader(buf)
    return _icon_cache[item_id]


def item_name(item_id):
    ns, name = item_id.split(':', 1)
    for k in (f'item.{ns}.{name}', f'block.{ns}.{name}'):
        if k in LANG:
            return LANG[k]
    return name.replace('_', ' ').title()


# ---- flowables

class ItemLine(Flowable):
    """An item picture with its caption beside it."""

    def __init__(self, item_id, caption):
        super().__init__()
        self.item_id = item_id
        self.caption = Paragraph(esc(caption) or esc(item_name(item_id)), CAPTION)

    def wrap(self, aw, ah):
        self.aw = aw
        _, h = self.caption.wrap(aw - 34, ah)
        self.h = max(26, h + 4)
        return aw, self.h

    def draw(self):
        c = self.canv
        c.drawImage(icon(self.item_id), 2, self.h - 24, 22, 22, mask='auto')
        _, h = self.caption.wrap(self.aw - 34, self.h)
        self.caption.drawOn(c, 32, (self.h - h) / 2)


def recipe_grid(recipe_id):
    """(3x3 cells of item ids or None, result id, count, kind) from the recipe file; None if it can't be read."""
    ns, path = recipe_id.split(':', 1)
    f = DATA / ns / 'recipe' / (path + '.json')
    if not f.exists():
        return None
    r = json.loads(f.read_text(encoding='utf-8'))
    t = r.get('type', '')

    def first(ing):
        if isinstance(ing, list):
            ing = ing[0]
        if 'item' in ing:
            return ing['item']
        if 'tag' in ing:
            return {'minecraft:planks': 'minecraft:oak_planks', 'minecraft:logs': 'minecraft:oak_log', 'minecraft:wool': 'minecraft:white_wool'}.get(ing['tag'], 'minecraft:barrier')
        return None

    grid = [[None] * 3 for _ in range(3)]
    if 'pattern' in r:
        for y, row in enumerate(r['pattern'][:3]):
            for x, ch in enumerate(row[:3]):
                if ch != ' ' and ch in r['key']:
                    grid[y][x] = first(r['key'][ch])
    elif 'ingredients' in r:
        for i, ing in enumerate(r['ingredients'][:9]):
            grid[i // 3][i % 3] = first(ing)
    elif 'ingredient' in r:
        grid[1][0] = first(r['ingredient'])
    res = r.get('result', {})
    if isinstance(res, str):
        res = {'id': res}
    return grid, res.get('id') or res.get('item'), res.get('count', 1), t


class RecipeBox(Flowable):
    """A crafting grid, arrow and result, drawn from the recipe file."""
    SLOT = 22

    def __init__(self, recipe_id):
        super().__init__()
        self.data = recipe_grid(recipe_id)

    def wrap(self, aw, ah):
        self.aw = aw
        return aw, self.SLOT * 3 + 10 if self.data else 14

    def draw(self):
        c = self.canv
        if not self.data:
            c.setFillColor(FAINT)
            c.setFont('Times-Italic', 9)
            c.drawString(0, 2, 'See the recipe book in game.')
            return
        grid, result, count, _ = self.data
        s = self.SLOT
        total = s * 3 + 34 + s
        x0 = (self.aw - total) / 2
        y0 = 4
        for y in range(3):
            for x in range(3):
                px, py = x0 + x * s, y0 + (2 - y) * s
                c.setFillColor(PAPER_EDGE)
                c.setStrokeColor(colors.HexColor('#B9A57A'))
                c.rect(px, py, s - 2, s - 2, stroke=1, fill=1)
                if grid[y][x]:
                    c.drawImage(icon(grid[y][x]), px + 2, py + 2, s - 6, s - 6, mask='auto')
        ax, ay = x0 + s * 3 + 6, y0 + s + s / 2 - 1
        c.setFillColor(FAINT)
        p = c.beginPath()
        p.moveTo(ax, ay - 2); p.lineTo(ax + 14, ay - 2); p.lineTo(ax + 14, ay - 6); p.lineTo(ax + 22, ay)
        p.lineTo(ax + 14, ay + 6); p.lineTo(ax + 14, ay + 2); p.lineTo(ax, ay + 2); p.close()
        c.drawPath(p, fill=1, stroke=0)
        rx, ry = x0 + s * 3 + 34, y0 + s
        c.setFillColor(PAPER_EDGE)
        c.rect(rx, ry, s - 2, s - 2, stroke=1, fill=1)
        if result:
            c.drawImage(icon(result), rx + 2, ry + 2, s - 6, s - 6, mask='auto')
            if count > 1:
                c.setFillColor(INK)
                c.setFont('Times-Bold', 8)
                c.drawRightString(rx + s - 3, ry + 2, str(count))


class DontPanic(Flowable):
    def wrap(self, aw, ah):
        self.aw, self.ah = aw, ah
        return aw, ah - 1

    def draw(self):
        c = self.canv
        c.setFillColor(LINKC)
        for i, word in enumerate(("DON'T", 'PANIC')):
            size = 66
            c.setFont('Helvetica-Bold', size)
            w = c.stringWidth(word, 'Helvetica-Bold', size)
            c.drawString((self.aw - w) / 2, self.ah / 2 + 10 - i * (size + 6), word)


class Marker(Flowable):
    """An invisible point that sets an anchor, a bookmark and a contents line."""

    def __init__(self, key, title, level):
        super().__init__()
        self.key, self.title, self.level = key, title, level

    def wrap(self, aw, ah):
        return 0, 0

    def draw(self):
        self.canv.bookmarkPage(self.key)
        self.canv.addOutlineEntry(self.title, self.key, level=max(self.level, 0), closed=self.level == 0)


class Doc(BaseDocTemplate):
    pages = {}

    def afterFlowable(self, f):
        if isinstance(f, Marker):
            Doc.pages[f.key] = self.page
            if f.level >= 0:
                self.notify('TOCEntry', (f.level, f.title, self.page, f.key))


# ---- pages

def paper(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(PAPER)
    canvas.rect(0, 0, PAGE_W, PAGE_H, stroke=0, fill=1)
    canvas.setStrokeColor(GOLD)
    canvas.setLineWidth(0.8)
    m = 22
    for (x, y, sx, sy) in ((m, m, 1, 1), (PAGE_W - m, m, -1, 1), (m, PAGE_H - m, 1, -1), (PAGE_W - m, PAGE_H - m, -1, -1)):
        canvas.line(x, y, x + sx * 26, y)
        canvas.line(x, y, x, y + sy * 26)
        canvas.setFillColor(LEAF)
        canvas.circle(x + sx * 7, y + sy * 7, 1.6, stroke=0, fill=1)
    if doc.page > 3:
        canvas.setFillColor(FAINT)
        canvas.setFont('Times-Roman', 9)
        canvas.drawCentredString(PAGE_W / 2, 30, str(doc.page))
    canvas.restoreState()


def cover(canvas, doc):
    canvas.saveState()
    canvas.drawImage(str(COVER), 0, 0, PAGE_W, PAGE_H)
    canvas.restoreState()


def elements_to_flow(els):
    flow = []
    for el in els:
        t = el['type']
        if t == 'heading':
            flow.append(Paragraph(markup(el['spans']), H2))
        elif t == 'para':
            flow.append(Paragraph(markup(el['spans']), BODY))
        elif t == 'list':
            for it in el['items']:
                flow.append(Paragraph(markup(it), LISTS, bulletText='•'))
        elif t == 'steps':
            for i, it in enumerate(el['items']):
                flow.append(Paragraph(markup(it), LISTS, bulletText=f'{el["start"] + i}.'))
        elif t == 'item':
            flow.append(ItemLine(el['id'], el['caption']))
        elif t == 'recipe':
            flow.append(Spacer(1, 3))
            flow.append(RecipeBox(el['id']))
            flow.append(Spacer(1, 5))
        elif t == 'image':
            f = ASSETS / 'textures/gui/handbook' / (el['path'] + '.png')
            if f.exists():
                from reportlab.platypus import Image as RLImage
                w = {'wide': 1.0, 'small': 0.5}.get(el['size'], 0.75) * (PAGE_W - 108)
                im = Image.open(f)
                flow.append(RLImage(str(f), w, w * im.height / im.width))
                if el['caption']:
                    flow.append(Paragraph(esc(el['caption']), ParagraphStyle('ic', parent=CAPTION, alignment=TA_CENTER)))
        elif t in ('table', 'truth'):
            rows = el['rows'] if t == 'table' else truth(el['gate'])
            if t == 'table' and el.get('special'):
                rows = special_rows(el['special'])
            if not rows:
                continue
            data = [[Paragraph(esc(c), CELLB if r == 0 else CELL) for c in row] for r, row in enumerate(rows)]
            tbl = Table(data, hAlign='LEFT')
            tbl.setStyle(TableStyle([('LINEBELOW', (0, 0), (-1, 0), 0.6, GOLD), ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor('#B9A57A')),
                                     ('TOPPADDING', (0, 0), (-1, -1), 2), ('BOTTOMPADDING', (0, 0), (-1, -1), 2)]))
            flow.append(Spacer(1, 3))
            flow.append(tbl)
            flow.append(Spacer(1, 6))
        elif t == 'scout':
            flow.extend(scout_flow(el['ref']))
        elif t == 'page':
            flow.append(PageBreak())
    return flow


class Box(Flowable):
    """An empty check-off box to tick by hand (FR-032)."""

    def wrap(self, aw, ah):
        return 10, 10

    def draw(self):
        self.canv.setStrokeColor(INK)
        self.canv.setLineWidth(0.8)
        self.canv.rect(0, 0, 9, 9, stroke=1, fill=0)


def scout_flow(ref):
    """A rank's or badge's requirements as a printable check-off list; the overview as two lists of boxes."""
    scouting = ASSETS / 'handbook/scouting'
    if ref == 'sash':
        from reportlab.platypus import Image as RLImage
        cells, row = [], []
        for f in sorted(scouting.glob('badge_*.json')):
            d = json.loads(f.read_text(encoding='utf-8'))
            patch = ASSETS / 'textures/gui/handbook/patches' / (d['id'] + '.png')
            row.append([RLImage(str(patch), 30, 30), Paragraph(esc(LANG[d['name']]), ParagraphStyle('pc', parent=CELL, alignment=TA_CENTER, fontSize=8.5, leading=10))])
            if len(row) == 4:
                cells.append(row)
                row = []
        if row:
            cells.append(row + [''] * (4 - len(row)))
        t = Table(cells, hAlign='CENTER')
        t.setStyle(TableStyle([('ALIGN', (0, 0), (-1, -1), 'CENTER'), ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('TOPPADDING', (0, 0), (-1, -1), 4)]))
        return [Spacer(1, 6), t]
    if ref == 'overview':
        flow = []
        for kind, title in (('rank', 'Ranks'), ('badge', 'Stoner Badges')):
            flow.append(Paragraph(title, H2))
            rows = []
            for f in sorted(scouting.glob(f'{kind}_*.json'), key=lambda f: json.loads(f.read_text(encoding='utf-8')).get('order', 0)):
                d = json.loads(f.read_text(encoding='utf-8'))
                needs = ' <i>(needs MineColonies)</i>' if d.get('needs') else ''
                rows.append([Box(), Paragraph(esc(LANG[d['name']]) + needs, CELL)])
            t = Table(rows, colWidths=[16, None], hAlign='LEFT')
            t.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'), ('TOPPADDING', (0, 0), (-1, -1), 1), ('BOTTOMPADDING', (0, 0), (-1, -1), 3)]))
            flow.append(t)
        return flow
    kind, _, did = ref.partition('/')
    d = json.loads((scouting / f'{kind}_{did}.json').read_text(encoding='utf-8'))
    from reportlab.platypus import Image as RLImage
    emblem = ASSETS / 'textures/gui/handbook' / ('ranks' if kind == 'rank' else 'patches') / f'{did}.png'
    head = [RLImage(str(emblem), 54, 54, hAlign='LEFT'), Spacer(1, 4)] if emblem.exists() else []
    rows = []
    for r in d['requirements']:
        text = esc(LANG[r['text']])
        if r.get('count', 1) > 1:
            text += f" (0/{r['count']})"
        if r['kind'] == 'honour':
            text += ' <i>(on my honour)</i>'
        rows.append([Box(), Paragraph(f"<b>{r['n']}.</b> {text}", BODY)])
    t = Table(rows, colWidths=[16, None], hAlign='LEFT')
    t.setStyle(TableStyle([('VALIGN', (0, 0), (-1, -1), 'TOP'), ('TOPPADDING', (0, 0), (-1, -1), 2), ('BOTTOMPADDING', (0, 0), (-1, -1), 4)]))
    sign = Paragraph('<i>Signed on my honour: ____________________   Date: __________</i>', CAPTION)
    return [Spacer(1, 4)] + head + [t, Spacer(1, 10), sign]


def special_rows(kind):
    """The generated back-of-the-book tables from real_terms.json (FR-015)."""
    terms = json.loads((BOOK / 'real_terms.json').read_text(encoding='utf-8'))
    if kind == 'units':
        return [['In the mod', 'Measures', 'Real unit']] + [[u['symbol'], u['measures'], u['real']] for u in terms['units']]
    rows = [(item_name('fourtwenty:' + pid), real) for pid, real in terms['parts'].items()]
    return [['Part', 'Real name']] + [[a, b] for a, b in sorted(rows, key=lambda r: r[0].lower())]


def truth(gate):
    if gate == 'NOT':
        return [['A', 'Q'], ['0', '1'], ['1', '0']]
    ops = {'AND': lambda a, b: a & b, 'OR': lambda a, b: a | b, 'NAND': lambda a, b: 1 - (a & b), 'NOR': lambda a, b: 1 - (a | b),
           'XOR': lambda a, b: a ^ b, 'XNOR': lambda a, b: 1 - (a ^ b)}
    return [['A', 'B', 'Q']] + [[str(a), str(b), str(ops[gate](a, b))] for a in (0, 1) for b in (0, 1)]


def build(out):
    book = json.loads((BOOK / 'book.json').read_text(encoding='utf-8'))
    out.parent.mkdir(parents=True, exist_ok=True)
    doc = Doc(str(out), pagesize=(PAGE_W, PAGE_H), title=LANG['handbook.fourtwenty.title'], author='Simon Stalwart')
    frame = Frame(54, 54, PAGE_W - 108, PAGE_H - 112, id='f')
    full = Frame(0, 0, PAGE_W, PAGE_H, id='full', leftPadding=0, rightPadding=0, topPadding=0, bottomPadding=0)
    doc.addPageTemplates([PageTemplate('cover', [full], onPage=cover), PageTemplate('paper', [frame], onPage=paper)])
    story = [NextPageTemplate('paper'), PageBreak(), DontPanic(), PageBreak()]
    # title page
    story += [Spacer(1, 120),
              Paragraph('A Handbook of', ParagraphStyle('t1', parent=CHAPTER, fontSize=16, leading=20, textColor=INK)),
              Paragraph('Weedcraft,', ParagraphStyle('t2', parent=CHAPTER, fontSize=36, leading=42)),
              Paragraph('Scouting, and Weed-craft', ParagraphStyle('t3', parent=CHAPTER, fontSize=18, leading=24, textColor=INK)),
              Spacer(1, 24),
              Paragraph('<i>by Simon Stalwart</i>', ParagraphStyle('by', parent=CHAPTER, fontSize=12, textColor=FAINT)),
              PageBreak()]
    toc = TableOfContents()
    toc.levelStyles = [ParagraphStyle('toc0', fontName='Times-Bold', fontSize=12, leading=16, textColor=HEADING, spaceBefore=6),
                       ParagraphStyle('toc1', fontName='Times-Roman', fontSize=10.5, leading=13, textColor=INK, leftIndent=14)]
    story += [Paragraph(LANG['handbook.fourtwenty.contents'], H1), toc, PageBreak()]
    index_terms = []
    for ch in book['chapters']:
        title = LANG.get(ch['title'], ch['title'])
        key = 'c_' + ch['id']
        opener = ASSETS / 'textures/gui/handbook/scenes' / f"chapter_{ch['id']}.png"
        if opener.exists():                         # the chapter's painting above its title
            from reportlab.platypus import Image as RLImage
            w = PAGE_W - 108
            with Image.open(opener) as im:
                h = w * im.height / im.width
            story += [Marker(key, title, 0), Spacer(1, 40), RLImage(str(opener), w, h), Spacer(1, 24), Paragraph(esc(title), CHAPTER), Spacer(1, 10)]
        else:
            story += [Marker(key, title, 0), Spacer(1, 150), Paragraph(esc(title), CHAPTER), Spacer(1, 10)]
        if ch.get('needs'):
            story.append(Paragraph('<i>Needs MineColonies</i>', ParagraphStyle('needs', parent=CAPTION, alignment=TA_CENTER)))
        story.append(PageBreak())
        for e in ch['entries']:
            eid = f"{ch['id']}/{e}"
            front, els, errs = parse((BOOK / 'en_us/entries' / ch['id'] / (e + '.md')).read_text(encoding='utf-8'))
            head = [Marker(anchor(eid), front['title'], 1), Paragraph(f'<a name="{anchor(eid)}"/>' + esc(front['title']), H1)]
            if front.get('needs'):
                head.append(Paragraph('<i>Needs MineColonies</i>', CAPTION))
            # entries run on, like a textbook's sections: a little space, then the heading kept with its first block
            flow = elements_to_flow(els)
            story += [Spacer(1, 16), KeepTogether(head + flow[:1])] + flow[1:]
            index_terms.extend((t, eid, front['title']) for t in front.get('terms', []))
        story.append(PageBreak())                   # each chapter ends its pages; the next opens on its own title page
    # the back of the book: glossary and index (FR-006)
    if not any(c['id'] == 'back' for c in book['chapters']):
        story += [Marker('c_back', 'Back of the Book', 0), Spacer(1, 150), Paragraph('Back of the Book', CHAPTER), PageBreak()]
    gl = BOOK / 'en_us/glossary.md'
    if gl.exists():
        story += [Marker('e_back_glossary', 'Glossary', 1), Paragraph('<a name="e_back_glossary"/>Glossary', H1)]
        for line in gl.read_text(encoding='utf-8').splitlines():
            if ':' not in line:
                continue
            term, rest = line.split(':', 1)
            meaning, _, target = rest.partition('-> entry:')
            link = f'<a href="#{anchor(target.strip())}" color="#2E7A1A"><b>{esc(term.strip())}</b></a>' if target.strip() else f'<b>{esc(term.strip())}</b>'
            story.append(Paragraph(f'{link}: {esc(meaning.strip())}', ParagraphStyle('g', parent=BODY, spaceAfter=3)))
        story.append(PageBreak())
    story += [Marker('e_back_index', 'Index', 1), Paragraph('<a name="e_back_index"/>Index', H1)]
    letter = None
    for term, eid, title in sorted(set(index_terms), key=lambda t: (t[0].lower(), t[1])):
        first = term[0].upper()
        if first != letter:
            letter = first
            story.append(Paragraph(letter, H2))
        page = Doc.pages.get(anchor(eid), '')
        story.append(Paragraph(f'<a href="#{anchor(eid)}" color="#3A2E20">{esc(term)}</a> <font color="#8A7A5A">. . . {page}</font>',
                               ParagraphStyle('ix', parent=BODY, fontSize=9.5, leading=11.5, spaceAfter=0)))
    doc.multiBuild(story)
    print('wrote', out, f'({doc.page} pages)')
    if out.resolve() == OUT.resolve():             # the committed PDF: note what it was printed from (book/PDF parity)
        from handbook_sources import record
        record()


if __name__ == '__main__':
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else OUT
    build(target)        # the first pass learns each entry's page, for the index
    build(target)

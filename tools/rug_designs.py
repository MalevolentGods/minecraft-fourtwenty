"""The rugs' designs: a field and a border for every colour, drawn as smooth shapes (generate_rugs.py).

Coordinates are model pixels from the block's centre: X east, Z south, the block from -8 to 8. A field
is drawn once, as layers of shapes; the generator repeats it under the square's symmetries (turns and
mirrors about the block's centre, and across its edges into the blocks round it), so every block of
a rug is the same, every quarter turns into the next, and motifs at a block's corners and edges join
up with the neighbours' into whole ones. Draw a motif at the centre or a corner with a multiple of
four petals (or a shape that's the same turned a quarter), one on an edge's middle mirrored about
that edge, and one anywhere else as it is: the symmetries add its seven mirror images.

A layer is (mask, role); later layers paint over earlier ones. Roles: FIELD, ACCENT, DEEP, LIGHT,
LEAF, STEM (dark leaf: veins and stems), EXTRA (a second motif colour), RIM (the border's outer band).

A border motif is drawn along the rug's edge: d in from the edge (0 to BW), s along it (0 to 8, from a
block's corner to its middle; mirrored, so keep motifs symmetric about s = 0 and s = 8).
"""
import numpy as np

FIELD, ACCENT, DEEP, LIGHT, LEAF, STEM, EXTRA, RIM = range(8)
TAU = 2 * np.pi


# ---- shapes ----------------------------------------------------------------------------------

def polar(X, Z, cx=0.0, cz=0.0, rot=0.0):
    dx, dz = X - cx, Z - cz
    return np.hypot(dx, dz), np.arctan2(dz, dx) - rot


def local(X, Z, x0, z0, ang):
    """Coordinates along (s) and across (t) a direction ang (radians, 0 = east, pi/2 = south) from (x0, z0)."""
    dx, dz = X - x0, Z - z0
    c, si = np.cos(ang), np.sin(ang)
    return dx * c + dz * si, -dx * si + dz * c


def disc(X, Z, cx, cz, r):
    return np.hypot(X - cx, Z - cz) < r


def ring(X, Z, cx, cz, r, w):
    return np.abs(np.hypot(X - cx, Z - cz) - r) < w / 2


def ellipse(X, Z, cx, cz, rx, rz, ang=0.0):
    s, t = local(X, Z, cx, cz, ang)
    return (s / rx) ** 2 + (t / rz) ** 2 < 1


def flower(X, Z, cx, cz, n, R, rot=0.0, depth=0.45):
    """n rounded petals out to R."""
    r, th = polar(X, Z, cx, cz, rot)
    return r < R * ((1 - depth) + depth * np.abs(np.cos(n * th / 2)))


def pointed(X, Z, cx, cz, n, R, rot=0.0, inner=0.35):
    """n pointed petals (a daisy or star flower) out to R."""
    r, th = polar(X, Z, cx, cz, rot)
    return r < R * (inner + (1 - inner) * np.abs(np.cos(n * th / 2)) ** 3)


def leaf(X, Z, x0, z0, ang, L, W, bend=0.0, serrate=0.0):
    """A leaf from its base (x0, z0) toward ang, L long and W wide at its widest; bend curves it, serrate
    toothes its edge. Returns (blade, vein)."""
    s, t = local(X, Z, x0, z0, ang)
    t = t - bend * (s / L) ** 2 * L
    u = np.clip(s / L, 0, 1)
    half = W / 2 * np.sin(np.pi * u) ** 0.8 * (1 + serrate * np.sin(s * 9.0 / max(L, 1e-6) * np.pi))
    blade = (s > 0) & (s < L) & (np.abs(t) < half)
    vein = blade & (np.abs(t) < 0.12) & (s < L * 0.9)
    return blade, vein


def capsule(X, Z, ax, az, bx, bz, w):
    """A line from a to b, w wide, with round ends."""
    px, pz, vx, vz = X - ax, Z - az, bx - ax, bz - az
    h = np.clip((px * vx + pz * vz) / (vx * vx + vz * vz + 1e-9), 0, 1)
    return np.hypot(px - vx * h, pz - vz * h) < w / 2


def arc(X, Z, cx, cz, r, w, a0, a1):
    """A ring's arc from angle a0 to a1 (radians)."""
    rr, th = polar(X, Z, cx, cz)
    th = np.mod(th - a0, TAU)
    return (np.abs(rr - r) < w / 2) & (th < np.mod(a1 - a0, TAU))


def sparkle(X, Z, cx, cz, R, rot=0.0):
    """A four-pointed star."""
    s, t = local(X, Z, cx, cz, rot)
    return (np.abs(s) ** 0.6 + np.abs(t) ** 0.6) < R ** 0.6


def heart(X, Z, cx, cz, R, ang=0.0):
    """A heart R across, pointing toward ang."""
    s, t = local(X, Z, cx, cz, ang)
    x, y = t / R * 1.25, -s / R * 1.25 + 0.15
    return (x * x + y * y - 1) ** 3 - x * x * y ** 3 < 0


def teardrop(X, Z, x0, z0, ang, L, W):
    """A drop (or flame) from its round end at (x0, z0), its point L away toward ang."""
    s, t = local(X, Z, x0, z0, ang)
    u = np.clip((s + W / 2) / (L + W / 2), 0, 1)
    return (u > 0) & (u < 1) & (np.abs(t) < W / 2 * np.sqrt(np.clip(1 - ((u - 0.3) / 0.7) ** 2, 0, 1) * (u > 0.3) + (u <= 0.3) * np.sqrt(np.clip(1 - ((0.3 - u) / 0.3) ** 2, 0, 1))))


def spiral(X, Z, cx, cz, R, turns, w, rot=0.0):
    """An Archimedean spiral out to R in the given turns, w wide."""
    r, th = polar(X, Z, cx, cz, rot)
    a = R / (turns * TAU)
    th = np.mod(th, TAU)
    out = np.zeros_like(X, bool)
    for k in range(int(np.ceil(turns)) + 1):
        rr = a * (th + TAU * k)
        out |= (np.abs(r - rr) < w / 2) & (rr <= R) & (rr > w)
    return out


def boteh(X, Z, x0, z0, ang, L, W, curl=0.45):
    """A paisley: a teardrop whose point sweeps round into a curl."""
    s, t = local(X, Z, x0, z0, ang)
    u = np.clip((s + W / 2) / (L + W / 2), 0, 1)
    t = t - curl * L * u ** 2.6
    prof = np.where(u > 0.3, np.sqrt(np.clip(1 - ((u - 0.3) / 0.7) ** 2, 0, 1)), np.sqrt(np.clip(1 - ((0.3 - u) / 0.3) ** 2, 0, 1)))
    return (s > -W / 2) & (s < L) & (np.abs(t) < W / 2 * prof)


def bands(V, period, width, offset=0.0):
    return np.abs(np.mod(V - offset + period / 2, period) - period / 2) < width / 2


def wavy(X, Z, cx, cz, r, amp, n, w, rot=0.0):
    """A ring whose radius waves n times round."""
    rr, th = polar(X, Z, cx, cz, rot)
    return np.abs(rr - r - amp * np.cos(n * th)) < w / 2


def smooth_noise(X, Z, terms):
    """A sum of cosines over the block's lattice (periodic and symmetric under the square's symmetries)."""
    out = np.zeros_like(X)
    for kx, kz, a, ph in terms:
        out += a * (np.cos(np.pi * kx * X / 8 + ph) * np.cos(np.pi * kz * Z / 8 + ph) + np.cos(np.pi * kz * X / 8 + ph) * np.cos(np.pi * kx * Z / 8 + ph))
    return out


# ---- motifs (each returns layers) -----------------------------------------------------------------

def rose(X, Z, cx, cz, R, petal=ACCENT, line=DEEP, centre=None):
    """A rose seen from above: a cupped bloom with petal edges spiralling in."""
    r, th = polar(X, Z, cx, cz)
    bloom = flower(X, Z, cx, cz, 4, R, np.pi / 4, 0.18)
    swirl = bloom & (np.abs(np.mod(r - 0.5 * R * th / TAU + R, R * 0.36) - R * 0.18) < R * 0.045) & (r > R * 0.12)
    out = [(bloom, petal), (swirl, line)]
    if centre is not None:
        out.append((disc(X, Z, cx, cz, R * 0.14), centre))
    return out


def sprig(X, Z, x0, z0, ang, L, leaves, lw, stem=STEM, leafc=LEAF, bud=None):
    """A stem with leaves in pairs along it (a fern frond, a lavender or sage sprig, an olive twig)."""
    s, t = local(X, Z, x0, z0, ang)
    stalk = (s > 0) & (s < L) & (np.abs(t) < 0.16)
    out = [(stalk, stem)]
    for i in range(leaves):
        f = (i + 1) / (leaves + 1)
        sx, sz = x0 + np.cos(ang) * L * f, z0 + np.sin(ang) * L * f
        size = lw * (1.15 - 0.6 * f)
        for side in (-1, 1):
            b, v = leaf(X, Z, sx, sz, ang + side * 0.95, size * 1.6, size * 0.75)
            out += [(b, leafc), (v, stem)]
    if bud is not None:
        ex, ez = x0 + np.cos(ang) * L, z0 + np.sin(ang) * L
        out.append((ellipse(X, Z, ex, ez, lw * 0.9, lw * 0.5, ang), bud))
    return out


def fan_leaf(X, Z, x0, z0, ang, L, color=LEAF, vein=STEM):
    """A seven-fingered fan leaf (Four Twenty's own), from its stem at (x0, z0) toward ang."""
    out = []
    for k, rel in enumerate((0, -0.42, 0.42, -0.85, 0.85, -1.25, 1.25)):
        size = L * (1.0, 0.86, 0.86, 0.66, 0.66, 0.42, 0.42)[k]
        b, v = leaf(X, Z, x0, z0, ang + rel, size, size * 0.24, serrate=0.18)
        out += [(b, color), (v, vein)]
    return out


def mushroom(X, Z, x0, z0, ang, H, cap=ACCENT, spots=LIGHT, stalk=LIGHT, gill=DEEP):
    """A toadstool standing on (x0, z0), its cap toward ang."""
    s, t = local(X, Z, x0, z0, ang)
    stem = (s > 0) & (s < H * 0.6) & (np.abs(t) < H * (0.13 + 0.03 * s / H))
    cs = s - H * 0.55
    capm = (cs > 0) & ((cs / (H * 0.45)) ** 2 + (t / (H * 0.42)) ** 2 < 1)
    rim = (np.abs(cs) < H * 0.05) & (np.abs(t) < H * 0.42)
    dots = capm & ((np.hypot(cs - H * 0.22, t) < H * 0.08) | (np.hypot(cs - H * 0.12, t - H * 0.22) < H * 0.06)
                   | (np.hypot(cs - H * 0.12, t + H * 0.22) < H * 0.06))
    return [(stem, stalk), (capm, cap), (rim, gill), (dots, spots)]


def flame(X, Z, x0, z0, ang, L, W, outer=ACCENT, inner=LIGHT):
    return [(teardrop(X, Z, x0, z0, ang, L, W), outer), (teardrop(X, Z, x0 + np.cos(ang) * W * 0.12, z0 + np.sin(ang) * W * 0.12, ang, L * 0.55, W * 0.5), inner)]


def cloud(X, Z, cx, cz, R, body=LIGHT, line=None, ang=0.0):
    """A puffy cloud of round lobes (a fine line inside its edge if line is given)."""
    c, si = np.cos(ang), np.sin(ang)
    lobes = ((0, 0, 0.55), (-0.6, 0.15, 0.4), (0.6, 0.15, 0.4), (-0.25, -0.3, 0.42), (0.3, -0.25, 0.4), (-0.95, 0.35, 0.26), (0.95, 0.35, 0.26))
    pts = [(cx + (a * c - b * si) * R, cz + (a * si + b * c) * R, rr * R) for a, b, rr in lobes]
    blobs = np.zeros_like(X, bool)
    inner = np.zeros_like(X, bool)
    for px, pz, rr in pts:
        blobs |= disc(X, Z, px, pz, rr)
        inner |= disc(X, Z, px, pz, rr - 0.18)
    out = [(blobs, body)]
    if line is not None:
        out.append((blobs & ~inner, line))
    return out


def crescent(X, Z, cx, cz, R, ang=0.0):
    return disc(X, Z, cx, cz, R) & ~disc(X, Z, cx + np.cos(ang) * R * 0.45, cz + np.sin(ang) * R * 0.45, R * 0.85)


def grapes(X, Z, x0, z0, ang, R, fruit=ACCENT, shine=LIGHT):
    out = []
    pts = [(0, 0), (1, -0.55), (1, 0.55), (2, -1.1), (2, 0), (2, 1.1), (3, -0.55), (3, 0.55), (4, 0)]
    for a, b in pts:
        px = x0 + np.cos(ang) * a * R * 0.85 - np.sin(ang) * b * R
        pz = z0 + np.sin(ang) * a * R * 0.85 + np.cos(ang) * b * R
        out += [(disc(X, Z, px, pz, R * 0.5), fruit), (disc(X, Z, px - R * 0.15, pz - R * 0.15, R * 0.13), shine)]
    return out


def citrus(X, Z, cx, cz, R, peel=ACCENT, flesh=LIGHT, seg=EXTRA):
    r, th = polar(X, Z, cx, cz)
    out = [(r < R, peel), (r < R * 0.86, flesh)]
    segs = (r < R * 0.8) & (r > R * 0.12) & (np.abs(np.mod(th * 8 / TAU + 0.5, 1) - 0.5) > 0.09)
    out.append((segs, seg))
    return out


def pine(X, Z, x0, z0, ang, H, body=LEAF, trunk=STEM):
    s, t = local(X, Z, x0, z0, ang)
    out = [((s > 0) & (s < H * 0.25) & (np.abs(t) < H * 0.07), trunk)]
    for k in range(3):
        b = H * (0.18 + 0.24 * k)
        h = H * 0.42
        tri = (s > b) & (s < b + h) & (np.abs(t) < (b + h - s) / h * H * (0.38 - 0.07 * k))
        out.append((tri, body))
    return out


# ---- fields -------------------------------------------------------------------------------------
# Every field is drawn once and turned a quarter about the block's centre three times: a motif at the
# centre or a corner should turn into itself (4 or 8 petals, a ring); anything else is tossed (drawn
# once at an angle, its turned copies making a loose, printed-fabric spread). Leave the field room.

def toss_leaf(X, Z, x0, z0, a, L, W, c=LEAF, v=STEM, bend=0.12, serrate=0.0):
    b, vein = leaf(X, Z, x0, z0, a, L, W, bend=bend, serrate=serrate)
    return [(b, c), (vein, v)]


def blossom(X, Z, cx, cz, n, R, rot, depth=0.5, petal=ACCENT, heart=LIGHT, eye=DEEP):
    return [(flower(X, Z, cx, cz, n, R, rot, depth), petal), (pointed(X, Z, cx, cz, n, R * 0.55, rot, 0.2), heart), (disc(X, Z, cx, cz, R * 0.17), eye)]


def f_roses(X, Z, P):
    out = rose(X, Z, 0, 0, 2.6, centre=P.get('centre'))
    out += toss_leaf(X, Z, 1.6, 1.4, 0.7, 2.6, 1.3) + toss_leaf(X, Z, 1.9, -1.0, -0.4, 2.2, 1.1)
    out += rose(X, Z, 5.4, 4.6, 1.5)
    out += toss_leaf(X, Z, 4.6, 6.0, 2.3, 1.8, 0.9) + toss_leaf(X, Z, 6.6, 3.4, -0.9, 1.6, 0.8)
    out += [(ellipse(X, Z, 6.6, -2.4, 0.55, 0.35, 0.9), ACCENT)] + toss_leaf(X, Z, 6.2, -2.0, 2.2, 1.0, 0.5)
    return out


def f_blossoms(X, Z, P):
    """Tossed blossoms (cherry, magnolia, begonia) with leaves and buds, plenty of field round them."""
    n, R, dp = P.get('n', 5), P.get('R', 2.0), P.get('depth', 0.5)
    out = blossom(X, Z, 3.4, 1.6, n, R, 0.3, dp)
    out += blossom(X, Z, 6.0, 6.2, n, R * 0.7, 1.4, dp)
    out += toss_leaf(X, Z, 4.6, 3.2, 1.1, 2.2, 1.0, bend=0.2) + toss_leaf(X, Z, 1.6, 0.6, 3.6, 1.8, 0.8)
    out += [(ellipse(X, Z, 6.6, 2.6, 0.55, 0.38, 0.5), ACCENT), (disc(X, Z, 0, 0, 0.5), P.get('dot', EXTRA))]
    return out


def f_daisies(X, Z, P):
    out = []
    for (cx, cz, R, rot) in ((3.6, 2.0, 2.0, 0.2), (6.2, 6.0, 1.3, 0.5), (0, 0, 1.0, 0)):
        out += [(pointed(X, Z, cx, cz, P.get('n', 12), R, rot, 0.25), ACCENT), (disc(X, Z, cx, cz, R * 0.32), P.get('eye', EXTRA))]
    out += toss_leaf(X, Z, 5.2, 3.6, 0.9, 2.0, 0.8) + toss_leaf(X, Z, 1.8, 4.6, 2.6, 1.7, 0.7)
    return out


def f_retro(X, Z, P):
    """Flower power: big round seventies blooms, petals in rings."""
    out = []
    for (cx, cz, R) in ((0, 0, 3.0), (5.2, 5.0, 2.2), (6.4, -0.6, 1.0)):
        out += [(flower(X, Z, cx, cz, 8, R, 0, 0.3), ACCENT), (flower(X, Z, cx, cz, 8, R * 0.62, np.pi / 8, 0.3), LIGHT),
                (disc(X, Z, cx, cz, R * 0.3), EXTRA)]
    return out


def f_sunflowers(X, Z, P):
    out = []
    for (cx, cz, R) in ((0, 0, 3.2), (5.4, 5.0, 1.8)):
        out += [(pointed(X, Z, cx, cz, 16, R, 0, 0.45), ACCENT), (pointed(X, Z, cx, cz, 16, R * 0.85, np.pi / 16, 0.5), LIGHT),
                (disc(X, Z, cx, cz, R * 0.42), DEEP)]
        r, th = polar(X, Z, cx, cz)
        out.append(((r < R * 0.38) & (np.mod(r * 3 + th * 2, 1.2) < 0.35), EXTRA))
    out += toss_leaf(X, Z, 3.0, 2.4, 0.5, 2.8, 1.5, serrate=0.08) + toss_leaf(X, Z, 6.4, 1.2, -1.2, 2.0, 1.1, serrate=0.08)
    return out


def f_hibiscus(X, Z, P):
    out = []
    for (cx, cz, R, rot) in ((3.8, 2.4, 2.6, 0.2), (6.8, 6.8, 1.4, 1.0)):
        tip = (cx + np.cos(rot + 0.6) * R * 0.8, cz + np.sin(rot + 0.6) * R * 0.8)
        out += [(flower(X, Z, cx, cz, 5, R, rot, 0.35), ACCENT), (flower(X, Z, cx, cz, 5, R * 0.5, rot, 0.4), DEEP),
                (capsule(X, Z, cx, cz, tip[0], tip[1], 0.2), LIGHT), (disc(X, Z, tip[0], tip[1], 0.3), EXTRA)]
    out += toss_leaf(X, Z, 0.6, 0.4, 0.2, 3.0, 1.8, serrate=0.1) + toss_leaf(X, Z, 5.4, 5.6, 2.6, 2.0, 1.2, serrate=0.1)
    return out


def f_lotus(X, Z, P):
    out = []
    for k in range(8):
        a = k * TAU / 8
        out += [(teardrop(X, Z, np.cos(a) * 0.5, np.sin(a) * 0.5, a, 3.0 if k % 2 == 0 else 2.2, 1.7), ACCENT if k % 2 == 0 else LIGHT)]
    out += [(disc(X, Z, 0, 0, 0.9), EXTRA)]
    for (cx, cz, R, rot) in ((5.4, 3.8, 1.9, 0.4), (2.6, 6.4, 1.2, 2.0)):
        s, t = local(X, Z, cx, cz, rot)
        pad = disc(X, Z, cx, cz, R) & ~((np.abs(t) < 0.18 * R) & (s > 0))
        out += [(pad, LEAF), (pad & ring(X, Z, cx, cz, R * 0.55, 0.12), STEM)]
    out += [(teardrop(X, Z, 6.8, 0.4, -0.6, 1.4, 0.8), LIGHT)]
    return out


def f_lavender(X, Z, P):
    """Lavender sprigs tossed about: slim stems, leaves low down, little buds in whorls up the top."""
    out = []
    for (x0, z0, a, L) in ((1.2, 6.2, -1.0, 5.6), (6.2, 4.4, 2.5, 3.8)):
        out += sprig(X, Z, x0, z0, a, L * 0.98, 1, 0.5)
        for k in range(8):
            f = 0.5 + k * 0.065
            px, pz = x0 + np.cos(a) * L * f, z0 + np.sin(a) * L * f
            w = 0.34 * (1 - 0.06 * k)
            for side in (-1, 1):
                out.append((ellipse(X, Z, px + side * np.cos(a + np.pi / 2) * 0.2, pz + side * np.sin(a + np.pi / 2) * 0.2, w * 0.75, w * 0.5, a + side * 0.7),
                            ACCENT if (k + (side > 0)) % 2 else LIGHT))
    out += [(flower(X, Z, 0, 0, 4, 0.9, np.pi / 4, 0.4), LIGHT)]
    return out


def f_leaves(X, Z, P):
    """Tossed leaves, some veined, in a couple of greens."""
    out = []
    for (x0, z0, a, L, W, c) in ((0.8, 0.4, 0.5, 3.4, 1.8, ACCENT), (5.0, 1.4, 2.2, 2.8, 1.5, LIGHT), (4.4, 5.2, -0.4, 2.6, 1.3, EXTRA)):
        out += toss_leaf(X, Z, x0, z0, a, L, W, c, DEEP, bend=P.get('bend', 0.12), serrate=P.get('serrate', 0.0))
    if P.get('berries'):
        out += [(disc(X, Z, 3.0, 3.4, 0.45), P['berries']), (disc(X, Z, 3.6, 3.9, 0.35), P['berries'])]
    return out


def f_olive(X, Z, P):
    out = sprig(X, Z, 0.6, 1.0, 0.35, 5.6, 4, 0.85, stem=DEEP, leafc=ACCENT)
    for (cx, cz) in ((3.4, 3.4), (5.0, 3.6)):
        out += [(ellipse(X, Z, cx, cz, 0.6, 0.42, 0.6), EXTRA), (disc(X, Z, cx - 0.18, cz - 0.14, 0.13), LIGHT)]
    out += sprig(X, Z, 6.8, 6.4, 3.6, 2.6, 2, 0.6, stem=DEEP, leafc=LIGHT)
    return out


def f_ferns(X, Z, P):
    out = []
    for (x0, z0, a, L) in ((0.6, 0.6, 0.5, 7.0), (6.0, 6.6, -2.0, 3.6)):
        out += sprig(X, Z, x0, z0, a, L, 7 if L > 5 else 4, 0.6, stem=DEEP, leafc=ACCENT)
    return out


def f_fanleaf(X, Z, P):
    """Fan leaves tossed about, a big one and a small one, a seed or two."""
    out = fan_leaf(X, Z, 2.4, 2.0, 0.75, 3.6, color=ACCENT, vein=DEEP)
    out += fan_leaf(X, Z, 6.0, 6.8, 3.4, 2.0, color=LIGHT, vein=DEEP)
    out += [(ellipse(X, Z, 6.4, 1.4, 0.4, 0.28, 0.6), EXTRA), (flower(X, Z, 0, 0, 4, 0.7, np.pi / 4, 0.4), EXTRA)]
    return out


def f_pines(X, Z, P):
    """A ring of pines round a clearing, their tops outward, a small one tossed beyond."""
    out = [(ring(X, Z, 0, 0, 1.2, 0.3), EXTRA)]
    for (x0, z0, a, H) in ((0.0, -2.0, -np.pi / 2, 4.4), (3.6, -3.6, -np.pi / 4, 3.0), (6.6, 6.4, 0.6, 2.2)):
        out += pine(X, Z, x0, z0, a, H, ACCENT if H > 4 else LIGHT, DEEP)
    return out


def f_grapes(X, Z, P):
    """Grape bunches on the vine: a bunch, its leaf, curling tendrils."""
    out = grapes(X, Z, 3.0, 1.4, 0.9, 0.8)
    out += toss_leaf(X, Z, 2.8, 1.2, -0.6, 2.8, 2.6, serrate=0.12)
    out += [(spiral(X, Z, 5.8, 5.6, 1.0, 1.4, 0.16), STEM), (spiral(X, Z, 1.2, 5.6, 0.6, 1.2, 0.14, 2.0), STEM)]
    out += [(flower(X, Z, 0, 0, 4, 1.0, np.pi / 4, 0.4), LIGHT)]
    return out


def f_citrus(X, Z, P):
    out = citrus(X, Z, 3.6, 2.2, 2.2) + citrus(X, Z, 6.6, 6.6, 1.3)
    out += toss_leaf(X, Z, 5.2, 4.2, 0.9, 2.4, 1.2) + toss_leaf(X, Z, 1.0, 4.6, 2.4, 2.0, 1.0)
    out += [(flower(X, Z, 0, 0, 4, 0.9, np.pi / 4, 0.4), LIGHT)]
    return out


def f_scales(X, Z, P):
    """Fish scales (seigaiha): overlapping discs of rings, the centre's over the corners' over the edges'."""
    out = []
    period = P.get('ring', 1.1)
    for (cx, cz, R) in ((8, 0, 4.4), (8, 8, 5.4), (0, 0, 5.4)):
        r = np.hypot(X - cx, Z - cz)
        out += [(r < R, FIELD), ((r < R) & bands(r, period, period * 0.42, R), ACCENT), (r < 0.7, LIGHT)]
    return out


def f_ripples(X, Z, P):
    out = []
    for k, (r, c) in enumerate(((1.2, LIGHT), (2.6, ACCENT), (4.0, LIGHT))):
        out.append((wavy(X, Z, 0, 0, r, 0.18 + 0.05 * k, 8, 0.3), c))
    out += [(ring(X, Z, 6.0, 5.4, 0.9, 0.22), LIGHT), (ring(X, Z, 6.0, 5.4, 1.6, 0.18), ACCENT), (disc(X, Z, 6.6, 1.6, 0.35), LIGHT)]
    return out


def f_bubbles(X, Z, P):
    out = []
    for (cx, cz, r) in ((2.8, 1.6, 1.3), (5.6, 0.8, 0.6), (4.6, 4.4, 0.9), (6.8, 6.6, 0.45), (1.2, 4.6, 0.5), (0, 0, 0.7)):
        out += [(ring(X, Z, cx, cz, r, max(0.2, r * 0.22)), LIGHT), (arc(X, Z, cx, cz, r * 0.62, max(0.14, r * 0.14), 3.6, 4.6), EXTRA)]
    return out


def f_waves(X, Z, P):
    """Curling waves tossed about, foam on their lips, spray between."""
    out = []
    for (cx, cz, R, rot) in ((3.0, 2.0, 2.2, 0.4), (6.4, 6.4, 1.3, 2.4)):
        sx, tx = local(X, Z, cx, cz, rot)
        crest = disc(X, Z, cx, cz, R) & (tx > -R * 0.2)
        out += [(crest, ACCENT), (spiral(X, Z, cx, cz, R * 0.92, 1.6, R * 0.16, rot), LIGHT),
                (ring(X, Z, cx, cz, R, R * 0.14) & (tx > -R * 0.2), DEEP)]
        for k in range(3):
            fa = rot + 2.0 + k * 0.4
            out.append((disc(X, Z, cx + np.cos(fa) * R * 1.05, cz + np.sin(fa) * R * 1.05, R * (0.16 - 0.03 * k)), LIGHT))
    for (cx, cz, rr) in ((6.4, 2.4, 0.3), (0.6, 5.8, 0.25), (5.0, 4.8, 0.18)):
        out.append((disc(X, Z, cx, cz, rr), EXTRA))
    out.append((pointed(X, Z, 0, 0, 8, 1.0, 0, 0.3), LIGHT))
    return out


def f_clouds(X, Z, P):
    out = cloud(X, Z, 3.4, 2.0, 2.2, LIGHT, P.get('line'), 0.3) + cloud(X, Z, 6.4, 6.6, 1.3, LIGHT, P.get('line'), 1.9)
    if P.get('rain'):
        for (x0, z0) in ((3.0, 4.6), (4.4, 4.8), (1.8, 4.2), (6.6, 3.4)):
            out.append((teardrop(X, Z, x0, z0, np.pi / 2 + 0.3, 0.8, 0.45), ACCENT))
    else:
        out += [(sparkle(X, Z, 6.6, 2.6, 0.7), EXTRA), (disc(X, Z, 0, 0, 0.35), EXTRA)]
    return out


def f_night(X, Z, P):
    """Stars, sparkles and a moon on the dark."""
    out = [(sparkle(X, Z, 0, 0, 1.8), LIGHT)]
    for (cx, cz, R, c) in ((3.6, 1.4, 0.8, ACCENT), (6.4, 5.0, 0.55, LIGHT), (1.4, 5.4, 0.6, ACCENT), (7.0, 1.0, 1.1, LIGHT)):
        out.append((sparkle(X, Z, cx, cz, R, 0.3 * cx), c))
    for (cx, cz) in ((2.8, 3.6), (5.0, 6.8), (5.6, 2.8), (0.6, 7.4), (4.4, 0.2)):
        out.append((disc(X, Z, cx, cz, 0.14), LIGHT))
    if P.get('moon'):
        out.append((crescent(X, Z, 4.6, 4.4, 1.3, -2.2), EXTRA))
    if P.get('planet'):
        s, t = local(X, Z, 4.6, 4.2, -0.5)
        band = ellipse(X, Z, 4.6, 4.2, 1.9, 0.4, -0.5) & ~ellipse(X, Z, 4.6, 4.2, 1.5, 0.2, -0.5) & ~(disc(X, Z, 4.6, 4.2, 1.0) & (t < 0))
        out += [(disc(X, Z, 4.6, 4.2, 1.0), EXTRA), (band, ACCENT)]
    return out


def f_sunburst(X, Z, P):
    r, th = polar(X, Z)
    rays = (np.abs(np.mod(th * 16 / TAU + 0.5, 1) - 0.5) < 0.16 * (1 - r / 8) + 0.03) & (r > 2.6) & (r < 6.6)
    out = [(rays, ACCENT), (disc(X, Z, 0, 0, 2.2), LIGHT), (ring(X, Z, 0, 0, 2.6, 0.25), DEEP)]
    out += [(pointed(X, Z, 8, 8, 8, 1.4, 0, 0.4), EXTRA)]
    return out


def f_campfire(X, Z, P):
    """A campfire from above: crossed logs, a ring of flames leaning out from the glow, embers drifting."""
    out = [(capsule(X, Z, -3.2, -1.2, 3.2, 1.2, 0.9), STEM), (capsule(X, Z, -3.2, -1.2, 3.2, 1.2, 0.3) & ~disc(X, Z, 0, 0, 2.2), DEEP)]
    for (a, L, W) in ((0.15, 2.8, 1.5), (0.95, 2.0, 1.1)):
        out += flame(X, Z, np.cos(a) * 0.6, np.sin(a) * 0.6, a, L, W)
    out += [(disc(X, Z, 0, 0, 1.0), LIGHT), (disc(X, Z, 0, 0, 0.5), EXTRA)]
    for (cx, cz, rr) in ((4.8, 2.6, 0.28), (5.8, 5.4, 0.2), (2.6, 5.8, 0.24), (6.8, 0.8, 0.16)):
        out.append((disc(X, Z, cx, cz, rr), EXTRA))
    return out


def f_smoke(X, Z, P):
    """Curling smoke: long thin wisps swirling out, the odd smoke ring."""
    out = []
    for (x0, z0, a, L, W, bend, c) in ((0.4, 0.4, 0.3, 6.0, 1.0, 0.35, ACCENT), (3.0, 4.6, -1.6, 4.0, 0.7, -0.4, LIGHT)):
        b, v = leaf(X, Z, x0, z0, a, L, W, bend=bend)
        out.append((b, c))
    out += [(ring(X, Z, 6.2, 5.6, 0.9, 0.26), EXTRA), (ring(X, Z, 6.8, 1.6, 0.5, 0.2), LIGHT)]
    return out


def f_mushrooms(X, Z, P):
    """A fairy ring: toadstools round a circle, caps outward, a small one tossed beyond."""
    out = [(ring(X, Z, 0, 0, 2.2, 0.2), LEAF)]
    for (x0, z0, a, H) in ((0.0, -2.0, -np.pi / 2, 3.4), (2.2 * 0.71, -2.2 * 0.71, -np.pi / 4, 2.4), (6.0, 6.4, 0.9, 2.0)):
        out += mushroom(X, Z, x0, z0, a, H)
    out += toss_leaf(X, Z, 5.0, 3.4, 0.2, 1.8, 0.8)
    return out


def f_hearts(X, Z, P):
    """Tossed hearts round a little four-petal bloom."""
    out = [(flower(X, Z, 0, 0, 4, 1.4, np.pi / 4, 0.5), LIGHT), (disc(X, Z, 0, 0, 0.4), ACCENT)]
    for (cx, cz, sz, a, c) in ((3.6, 2.0, 2.4, 0.5, ACCENT), (6.4, 6.2, 1.5, 2.2, LIGHT), (1.6, 5.6, 1.0, -0.6, EXTRA), (6.6, 2.4, 0.8, 1.2, LIGHT)):
        out.append((heart(X, Z, cx, cz, sz, a), c))
    return out


def f_medallion(X, Z, P):
    """A Persian medallion: a lobed star in layers, scrolling corners."""
    r, th = polar(X, Z)
    lobes = r < 4.4 * (0.78 + 0.22 * np.abs(np.cos(4 * th))) + 0.5 * np.abs(np.cos(8 * th)) ** 8
    out = [(lobes, ACCENT), (r < 3.2 * (0.8 + 0.2 * np.abs(np.cos(4 * th + np.pi / 4))), LIGHT), (flower(X, Z, 0, 0, 8, 2.1, 0, 0.3), DEEP),
           (pointed(X, Z, 0, 0, 8, 1.5, np.pi / 8, 0.3), EXTRA), (disc(X, Z, 0, 0, 0.5), LIGHT)]
    out += [(arc(X, Z, 8, 8, 3.0, 0.3, 0, TAU), DEEP), (flower(X, Z, 8, 8, 4, 2.0, np.pi / 4, 0.4), ACCENT), (disc(X, Z, 8, 8, 0.7), LIGHT)]
    out += toss_leaf(X, Z, 5.4, 0.8, 0.6, 1.8, 0.8)
    return out


def f_paisley(X, Z, P):
    """Boteh: paisleys swirling round, each outlined, with a flower in its belly."""
    out = []
    for (x0, z0, a, L, W) in ((2.4, 1.6, 0.6, 3.8, 2.6), (6.4, 6.4, 2.7, 2.2, 1.5)):
        body = boteh(X, Z, x0, z0, a, L, W)
        out += [(body, ACCENT), (body & ~boteh(X, Z, x0 + np.cos(a) * 0.25, z0 + np.sin(a) * 0.25, a, L * 0.82, W * 0.7), DEEP),
                (flower(X, Z, x0, z0, 5, W * 0.3, a, 0.5), LIGHT), (disc(X, Z, x0, z0, W * 0.09), EXTRA)]
    out += [(pointed(X, Z, 0, 0, 8, 1.1, 0, 0.3), EXTRA), (disc(X, Z, 5.6, 3.4, 0.3), EXTRA), (disc(X, Z, 4.6, 5.0, 0.2), LIGHT)]
    return out


def f_kilim(X, Z, P):
    """Stepped diamonds and hooks, woven-looking."""
    step = P.get('step', 1.0)
    D = np.abs(X) + np.abs(Z)
    Q = np.floor(np.maximum(np.abs(X), np.abs(Z)) / step) * step + np.floor(np.minimum(np.abs(X), np.abs(Z)) / step) * step
    out = [(Q < 5.0, ACCENT), (Q < 3.0, DEEP), (Q < 1.2, LIGHT)]
    Qc = np.floor(np.abs(X - 8) / step) * step + np.floor(np.abs(Z - 8) / step) * step
    out += [(Qc < 3.0, EXTRA), (Qc < 1.2, LIGHT)]
    out.append(((np.abs(Q - 6.0) < 0.5) & (D < 9.5), DEEP))
    return out


def f_checker(X, Z, P):
    """A soft checkerboard: rounded tiles, a fine line round each."""
    size = P.get('size', 4.0)
    gx, gz = np.mod(X + 8, size * 2) - size, np.mod(Z + 8, size * 2) - size
    on = (gx * gz) > 0
    tile_x, tile_z = np.abs(np.mod(X + 8, size) - size / 2), np.abs(np.mod(Z + 8, size) - size / 2)
    inner = np.hypot(np.maximum(tile_x - size / 2 + 0.9, 0), np.maximum(tile_z - size / 2 + 0.9, 0)) < 0.6
    return [(on & inner, ACCENT), (~on & inner & (np.hypot(tile_x, tile_z) < size * 0.18), LIGHT)]


def f_plaid(X, Z, P):
    """Tartan: broad bands both ways, darker where they cross, fine stripes over."""
    period, width = P.get('period', 8), P.get('width', 3.2)
    bx, bz = bands(X, period, width, period / 2), bands(Z, period, width, period / 2)
    out = [(bx | bz, ACCENT), (bx & bz, DEEP)]
    out += [(bands(X, period, 0.3, 0) | bands(Z, period, 0.3, 0), LIGHT), (bands(X, period, 0.22, period / 2) | bands(Z, period, 0.22, period / 2), EXTRA)]
    if P.get('fine', True):
        out += [((bands(X, 2, 0.12, 1) | bands(Z, 2, 0.12, 1)) & ~(bx | bz), DEEP)]
    return out


def f_camo(X, Z, P):
    n1 = smooth_noise(X, Z, [(1, 2, 1.0, 0.3), (3, 1, 0.7, 1.1), (2, 3, 0.5, 2.0), (4, 1, 0.35, 0.7)])
    n2 = smooth_noise(X, Z, [(2, 1, 1.0, 1.7), (1, 3, 0.6, 0.4), (3, 4, 0.4, 2.6)])
    return [(n1 > 0.35, ACCENT), (n2 > 0.75, DEEP), (n1 < -1.0, LIGHT)]


def f_rings(X, Z, P):
    """Interlocking circles: every ring linked to its neighbours."""
    out = []
    for (cx, cz, c) in ((0, 0, ACCENT), (8, 8, ACCENT), (8, 0, LIGHT)):
        out.append((ring(X, Z, cx, cz, 4.2, 0.55), c))
    out += [(disc(X, Z, 0, 0, 0.6), LIGHT), (disc(X, Z, 4, 4, 0.5), EXTRA)]
    return out


def f_coffee(X, Z, P):
    """Coffee beans tossed about a cup seen from above."""
    out = []
    for (cx, cz, a, sz) in ((3.4, 1.6, 0.6, 1.3), (6.2, 5.4, 2.2, 1.0), (1.4, 5.0, -0.5, 0.9), (6.8, 1.0, 1.4, 0.75)):
        sx, tx = local(X, Z, cx, cz, a)
        out += [(ellipse(X, Z, cx, cz, sz * 1.15, sz * 0.8, a), ACCENT),
                ((np.abs(tx - 0.25 * sz * np.sin(sx / sz * 2.2)) < 0.12 * sz) & ellipse(X, Z, cx, cz, sz, sz * 0.7, a), DEEP)]
    out += [(ring(X, Z, 0, 0, 1.4, 0.26), LIGHT), (disc(X, Z, 0, 0, 0.9), EXTRA)]
    return out


def f_cotton(X, Z, P):
    """Cotton bolls: five outlined puffs round dark star-shaped bracts, a leaf or two."""
    out = []
    for (cx, cz, R, rot) in ((0, 0, 1.7, 0.0), (4.6, 2.6, 1.5, 0.7), (6.4, 6.6, 1.0, 1.9)):
        out.append((pointed(X, Z, cx, cz, 5, R * 1.25, rot + np.pi / 5, 0.15), STEM))
        for k in range(5):
            a = k * TAU / 5 + rot
            px, pz = cx + np.cos(a) * R * 0.45, cz + np.sin(a) * R * 0.45
            out += [(disc(X, Z, px, pz, R * 0.5), DEEP), (disc(X, Z, px, pz, R * 0.42), LIGHT)]
    out += toss_leaf(X, Z, 2.2, 5.0, 0.3, 2.2, 1.5, ACCENT, DEEP, serrate=0.1)
    return out

def f_wheat(X, Z, P):
    """Wheat ears tossed about, their awns fine."""
    out = []
    for (x0, z0, a, L) in ((0.8, 6.2, -1.0, 6.0), (6.6, 6.8, -2.6, 3.6)):
        s, t = local(X, Z, x0, z0, a)
        out.append(((s > 0) & (s < L * 0.55) & (np.abs(t) < 0.12), DEEP))
        for k in range(7):
            f = 0.5 + k * 0.07
            px, pz = x0 + np.cos(a) * L * f, z0 + np.sin(a) * L * f
            for side in (-1, 1):
                kx, kz = px + side * np.cos(a + np.pi / 2) * 0.28, pz + side * np.sin(a + np.pi / 2) * 0.28
                out.append((ellipse(X, Z, kx, kz, 0.42, 0.24, a + side * 0.5), ACCENT))
                out.append((capsule(X, Z, kx, kz, kx + np.cos(a + side * 0.5) * 1.3, kz + np.sin(a + side * 0.5) * 1.3, 0.07), DEEP))
    out += [(pointed(X, Z, 0, 0, 8, 1.0, 0, 0.3), LIGHT)]
    return out


def f_minimal(X, Z, P):
    """Quiet: a small medallion and scattered sprigs, tone on tone."""
    out = [(ring(X, Z, 0, 0, 2.4, 0.18), ACCENT), (pointed(X, Z, 0, 0, 8, 1.5, 0, 0.3), ACCENT), (disc(X, Z, 0, 0, 0.4), LIGHT)]
    out += sprig(X, Z, 4.4, 3.0, 0.5, 2.2, 2, 0.5, stem=ACCENT, leafc=ACCENT)
    out += [(disc(X, Z, 6.8, 6.6, 0.25), ACCENT)]
    return out


# ---- the Minecraft colours: blocky things from the game -----------------------------------------------

def rbox(X, Z, cx, cz, w, h, ang=0.0, r=0.35):
    """A box w x h centred on (cx, cz), turned ang, its corners rounded by r."""
    s, t = local(X, Z, cx, cz, ang)
    qx, qz = np.abs(s) - w / 2 + r, np.abs(t) - h / 2 + r
    return np.hypot(np.maximum(qx, 0), np.maximum(qz, 0)) + np.minimum(np.maximum(qx, qz), 0) < r


def pixels(X, Z, cx, cz, size, rows, ang=0.0):
    """A pixel-art sprite centred on (cx, cz), size across, turned ang: {char: mask} for each character in rows."""
    s, t = local(X, Z, cx, cz, ang)
    n = len(rows)
    i, j = np.floor((t / size + 0.5) * n).astype(int), np.floor((s / size + 0.5) * n).astype(int)
    inside = (i >= 0) & (i < n) & (j >= 0) & (j < n)
    grid = np.array([list(r) for r in rows])
    ch = np.where(inside, grid[np.clip(i, 0, n - 1), np.clip(j, 0, n - 1)], '.')
    return {c: ch == c for c in set(''.join(rows)) if c != '.'}


def gem(X, Z, cx, cz, R, ang=0.0):
    """A cut gem from above: a rhombus. Returns (stone, facets)."""
    s, t = local(X, Z, cx, cz, ang)
    stone = np.abs(s) / R + np.abs(t) / (R * 0.8) < 1
    table = np.abs(s) / (R * 0.45) + np.abs(t) / (R * 0.36) < 1
    return stone, stone & ~table & ((s > 0) ^ (t > 0))


def f_bones(X, Z, P):
    """Bones tossed about, bone meal sprinkled between."""
    out = []
    for (x0, z0, a, L) in ((3.6, 2.2, 0.5, 4.2), (6.2, 6.4, 2.0, 2.6)):
        c, si = np.cos(a), np.sin(a)
        ax, az, bx, bz = x0 - c * L / 2, z0 - si * L / 2, x0 + c * L / 2, z0 + si * L / 2
        knob = L * 0.12
        out.append((capsule(X, Z, ax, az, bx, bz, L * 0.16), ACCENT))
        for ex, ez in ((ax, az), (bx, bz)):
            for side in (-1, 1):
                out.append((disc(X, Z, ex - si * side * knob, ez + c * side * knob, knob * 1.15), ACCENT))
        out.append((capsule(X, Z, ax + c * 0.4, az + si * 0.4, bx - c * 0.4, bz - si * 0.4, L * 0.05), LIGHT))
    for (cx, cz) in ((0, 0), (1.2, 5.6), (6.8, 2.0), (4.8, 0.2), (2.6, 7.2)):
        out.append((rbox(X, Z, cx, cz, 0.5, 0.5, 0.4, 0.1), DEEP))
    return out


def f_gravel(X, Z, P):
    """Gravel: a scatter of rounded pebbles in three greys."""
    rng = np.random.default_rng(P.get('seed', 7))
    out = []
    for _ in range(26):
        cx, cz = rng.uniform(0.3, 7.7, 2)
        R = rng.uniform(0.35, 0.9)
        out.append((ellipse(X, Z, cx, cz, R, R * rng.uniform(0.6, 0.9), rng.uniform(0, np.pi)), (ACCENT, DEEP, LIGHT)[rng.integers(3)]))
    return out


def f_cobble(X, Z, P):
    """Cobblestone: chunky stones with mortar between, a little moss on one."""
    stones = ((1.9, 1.7, 3.2, 2.8, 0.1), (5.9, 1.5, 3.6, 2.4, -0.08), (1.4, 5.3, 2.2, 3.6, 0.05), (4.6, 4.9, 3.4, 2.6, 0.12),
              (7.0, 4.5, 1.2, 3.0, 0), (3.7, 7.2, 2.6, 1.0, 0), (6.6, 7.1, 2.2, 1.0, 0))
    out = []
    for k, (cx, cz, w, h, a) in enumerate(stones):
        out.append((rbox(X, Z, cx, cz, w, h, a, 0.6), LIGHT if k % 2 else ACCENT))
        out.append((rbox(X, Z, cx - 0.25, cz - 0.25, w * 0.45, h * 0.35, a, 0.3), ACCENT if k % 2 else EXTRA))
    out.append((rbox(X, Z, 4.6, 4.9, 3.4, 2.6, 0.12, 0.6) & (smooth_noise(X, Z, [(3, 2, 1.0, 0.4)]) > 0.6), LEAF))
    return out


def f_obsidian(X, Z, P):
    """Obsidian: dark glassy facets, purple glints along the cracks."""
    out = []
    for (cx, cz, w, h, a, c) in ((2.4, 2.0, 3.4, 1.6, 0.6, ACCENT), (5.8, 5.6, 2.6, 1.4, -0.7, ACCENT), (6.4, 1.6, 1.6, 0.9, 0.2, DEEP),
                                 (1.6, 6.0, 1.8, 1.0, 1.2, DEEP)):
        out.append((rbox(X, Z, cx, cz, w, h, a, 0.15), c))
    for (ax, az, bx, bz) in ((0.0, 4.0, 3.6, 3.6), (3.6, 3.6, 4.6, 7.4), (3.6, 3.6, 7.6, 3.2)):
        out.append((capsule(X, Z, ax, az, bx, bz, 0.16), EXTRA))
    out += [(sparkle(X, Z, 0, 0, 0.9), LIGHT), (sparkle(X, Z, 6.6, 7.0, 0.6, 0.4), LIGHT), (disc(X, Z, 4.6, 1.0, 0.18), LIGHT)]
    return out


def f_dirt(X, Z, P):
    """A dirt block's specks with grass tufts poking up."""
    out = []
    rng = np.random.default_rng(3)
    for _ in range(22):
        cx, cz = rng.uniform(0.3, 7.7, 2)
        out.append((rbox(X, Z, cx, cz, 0.6, 0.6, 0, 0.12), (DEEP, LIGHT)[rng.integers(2)]))
    for (x0, z0, a) in ((3.4, 2.6, -1.9), (6.4, 6.2, -1.3)):
        for k, rel in enumerate((-0.5, 0, 0.5)):
            b, v = leaf(X, Z, x0, z0, a + rel, (1.9, 2.4, 1.7)[k], 0.55, bend=0.1 * (k - 1))
            out.append((b, LEAF))
    return out


def f_redstone(X, Z, P):
    """Redstone dust wired across the rug, bright where it meets, a torch at the junctions."""
    out = [(capsule(X, Z, -8, 0, 8, 0, 0.7) | capsule(X, Z, 0, -8, 0, 8, 0.7), DEEP),
           (capsule(X, Z, -8, 0, 8, 0, 0.3) | capsule(X, Z, 0, -8, 0, 8, 0.3), ACCENT)]
    out += [(rbox(X, Z, 0, 0, 2.0, 2.0, np.pi / 4, 0.3), ACCENT), (disc(X, Z, 0, 0, 0.6), LIGHT)]
    out += [(capsule(X, Z, 8, 4.4, 4.4, 4.4, 0.7) | capsule(X, Z, 4.4, 4.4, 4.4, 8, 0.7), DEEP),
            (capsule(X, Z, 8, 4.4, 4.4, 4.4, 0.3) | capsule(X, Z, 4.4, 4.4, 4.4, 8, 0.3), ACCENT)]
    out += [(rbox(X, Z, 4.4, 4.4, 1.0, 1.0), STEM), (disc(X, Z, 4.4, 4.4, 0.42), LIGHT)]
    for (cx, cz) in ((2.4, 1.4), (6.6, 2.0), (1.6, 6.2)):
        out.append((disc(X, Z, cx, cz, 0.22), ACCENT))
    return out


def f_blaze(X, Z, P):
    """Blaze rods tossed round a fire, sparks flying."""
    out = []
    for (x0, z0, a, L) in ((3.2, 2.4, 0.4, 4.6), (6.4, 6.0, 2.1, 2.8)):
        c, si = np.cos(a) * L / 2, np.sin(a) * L / 2
        out += [(capsule(X, Z, x0 - c, z0 - si, x0 + c, z0 + si, 0.9), DEEP), (capsule(X, Z, x0 - c, z0 - si, x0 + c, z0 + si, 0.55), LIGHT)]
    for k in range(4):
        a = k * TAU / 4 + np.pi / 4
        out += flame(X, Z, np.cos(a) * 0.4, np.sin(a) * 0.4, a, 1.6, 1.1, ACCENT, LIGHT)
    for (cx, cz, R) in ((5.6, 2.2, 0.35), (1.6, 5.4, 0.3), (7.2, 3.8, 0.22)):
        out.append((rbox(X, Z, cx, cz, R * 2, R * 2, np.pi / 4, 0.05), EXTRA))
    return out


def f_nuggets(X, Z, P):
    """Gold nuggets, chunky and lumpy, with a glint."""
    out = []
    for (cx, cz, R, a) in ((3.4, 2.2, 1.3, 0.3), (6.2, 6.2, 0.9, 1.1), (1.4, 5.8, 0.7, -0.4), (6.8, 1.2, 0.55, 0.7)):
        lump = rbox(X, Z, cx, cz, R * 1.6, R * 1.2, a, R * 0.3) | rbox(X, Z, cx + R * 0.35, cz - R * 0.35, R * 1.0, R * 1.0, a, R * 0.25)
        out += [(lump, DEEP), (rbox(X, Z, cx - R * 0.05, cz - R * 0.05, R * 1.3, R * 0.9, a, R * 0.25), ACCENT),
                (rbox(X, Z, cx - R * 0.3, cz - R * 0.25, R * 0.4, R * 0.3, a, 0.05), LIGHT)]
    out += [(sparkle(X, Z, 0, 0, 1.4), LIGHT), (sparkle(X, Z, 4.8, 4.4, 0.6, 0.4), LIGHT)]
    return out


SLIME = ('........', '.bb..bb.', '.bb..bb.', '........', '........', '....m...', '........', '........')
CREEPER = ('........', '.ee..ee.', '.ee..ee.', '...mm...', '..mmmm..', '..mmmm..', '..m..m..', '........')


def f_slime(X, Z, P):
    """Slime cubes: jelly outside, a smaller cube and a face inside."""
    out = []
    for (cx, cz, sz, a) in ((3.6, 2.4, 3.6, 0.2), (6.4, 6.4, 2.0, -0.5), (0, 0, 1.2, 0)):
        out += [(rbox(X, Z, cx, cz, sz, sz, a, sz * 0.12), ACCENT), (rbox(X, Z, cx, cz, sz * 0.7, sz * 0.7, a, sz * 0.08), LIGHT)]
        if sz > 1.5:
            px = pixels(X, Z, cx, cz, sz * 0.7, SLIME, a)
            out += [(px['b'], DEEP), (px['m'], DEEP)]
    return out


def f_creeper(X, Z, P):
    """Creeper faces, pixel for pixel, mottled like the skin."""
    out = []
    for (cx, cz, sz, a) in ((3.6, 2.4, 4.0, 0.15), (6.6, 6.6, 2.2, -0.4)):
        head = rbox(X, Z, cx, cz, sz, sz, a, 0.1)
        px = pixels(X, Z, cx, cz, sz, CREEPER, a)
        mottle = head & (smooth_noise(X, Z, [(5, 3, 1.0, 0.2), (2, 7, 0.6, 1.0)]) > 0.7)
        out += [(head, LEAF), (mottle, LIGHT), (px['e'], DEEP), (px['m'], DEEP)]
    out.append((rbox(X, Z, 0, 0, 1.0, 1.0, np.pi / 4, 0.1), LEAF))
    return out


def f_diamonds(X, Z, P):
    """Diamonds tossed about, facets catching the light."""
    out = []
    for (cx, cz, R, a) in ((3.6, 2.2, 2.0, 0.3), (6.4, 6.4, 1.2, 1.2), (1.4, 5.8, 0.9, -0.6), (0, 0, 1.0, np.pi / 4)):
        stone, facets = gem(X, Z, cx, cz, R, a)
        out += [(stone, LIGHT), (facets, ACCENT), (gem(X, Z, cx, cz, R * 0.3, a)[0], EXTRA)]
    out += [(sparkle(X, Z, 6.8, 1.6, 0.6), EXTRA), (sparkle(X, Z, 4.4, 6.0, 0.45, 0.5), EXTRA)]
    return out


def f_skyblock(X, Z, P):
    """Square clouds drifting by a lone floating grass block."""
    out = []
    for (cx, cz, w, h) in ((3.8, 2.0, 4.0, 1.6), (4.6, 1.0, 2.0, 1.2), (6.6, 6.2, 2.4, 1.2), (6.2, 5.4, 1.2, 1.0)):
        out.append((rbox(X, Z, cx, cz, w, h, 0, 0.2), LIGHT))
    out += [(rbox(X, Z, 0, 0, 2.4, 2.4, 0, 0.1), STEM), (rbox(X, Z, 0, 0, 1.9, 1.9, 0, 0.1), LEAF), (rbox(X, Z, 0.3, -0.3, 0.4, 0.4, 0, 0.05), EXTRA)]
    out += [(rbox(X, Z, 1.6, 5.8, 0.8, 0.8, 0, 0.1), LIGHT)]
    return out


def f_lapis(X, Z, P):
    """Lapis: clusters of blue flecks set with gold, like the ore."""
    out = []
    for (cx, cz) in ((3.4, 2.4), (6.2, 6.2), (0, 0)):
        for (dx, dz, s) in ((0, 0, 1.0), (0.9, 0.5, 0.7), (-0.7, 0.8, 0.6), (0.4, -0.9, 0.6), (-0.9, -0.5, 0.5)):
            out.append((rbox(X, Z, cx + dx, cz + dz, s, s, 0, 0.1), ACCENT))
        out.append((rbox(X, Z, cx + 0.15, cz + 0.15, 0.4, 0.4, 0, 0.05), LIGHT))
        out.append((rbox(X, Z, cx - 0.45, cz + 0.85, 0.25, 0.25, 0, 0.03), EXTRA))
    out += [(rbox(X, Z, 6.8, 1.6, 0.3, 0.3, 0, 0.05), EXTRA), (rbox(X, Z, 1.6, 6.6, 0.3, 0.3, 0, 0.05), EXTRA)]
    return out


def f_portal(X, Z, P):
    """A nether portal's swirl, its sparks drifting out."""
    out = [(spiral(X, Z, 0, 0, 5.4, 2.2, 0.9), ACCENT), (spiral(X, Z, 0, 0, 5.4, 2.2, 0.35), LIGHT), (disc(X, Z, 0, 0, 0.8), LIGHT)]
    out += [(spiral(X, Z, 8, 8, 2.6, 1.3, 0.5, 1.0), ACCENT)]
    for (cx, cz, R) in ((5.8, 2.4, 0.3), (2.4, 6.4, 0.25), (6.8, 5.0, 0.2)):
        out.append((rbox(X, Z, cx, cz, R * 2, R * 2, 0, 0.05), EXTRA))
    return out


def f_alliums(X, Z, P):
    """Alliums: round heads of tiny stars on tall stems."""
    out = []
    for (x0, z0, a, L, R) in ((1.0, 5.8, -1.0, 4.0, 1.5), (5.0, 7.6, -2.2, 2.6, 1.0)):
        hx, hz = x0 + np.cos(a) * L, z0 + np.sin(a) * L
        out.append((capsule(X, Z, x0, z0, hx, hz, 0.22), STEM))
        out += toss_leaf(X, Z, x0, z0, a - 0.6, L * 0.5, 0.5)
        out.append((disc(X, Z, hx, hz, R), ACCENT))
        for k in range(7):
            th = k * TAU / 7
            out.append((pointed(X, Z, hx + np.cos(th) * R * 0.55, hz + np.sin(th) * R * 0.55, 6, R * 0.32, th, 0.3), LIGHT))
        out.append((pointed(X, Z, hx, hz, 6, R * 0.32, 0, 0.3), LIGHT))
    out.append((pointed(X, Z, 0, 0, 8, 0.9, 0, 0.3), ACCENT))
    return out


def f_sheep(X, Z, P):
    """Fluffy pink sheep grazing, seen from above."""
    out = []
    for (cx, cz, R, a) in ((3.4, 2.6, 2.0, 0.3), (6.4, 6.6, 1.2, 2.0)):
        c, si = np.cos(a), np.sin(a)
        out += cloud(X, Z, cx, cz, R * 1.1, LIGHT, ACCENT, a)
        hx, hz = cx + c * R * 1.15, cz + si * R * 1.15
        out += [(rbox(X, Z, hx, hz, R * 0.55, R * 0.5, a, R * 0.1), EXTRA)]
        for side in (-1, 1):
            out.append((disc(X, Z, hx + c * R * 0.12 - si * side * R * 0.14, hz + si * R * 0.12 + c * side * R * 0.14, R * 0.06), DEEP))
    for (cx, cz) in ((0, 0), (1.4, 6.6), (7.0, 2.6)):
        out.append((pointed(X, Z, cx, cz, 5, 0.6, 0.3, 0.3), ACCENT))
    return out


FIELDS = {name[2:]: fn for name, fn in globals().items() if name.startswith('f_')}


# ---- borders ----------------------------------------------------------------------------------------

def b_vine(d, s, P):
    """A wavy vine with leaves on alternate sides and the odd berry."""
    mid = 0.6 + 1.0 + 0.45 * np.cos(np.pi * s / 4)
    stem = np.abs(d - mid) < 0.15
    lf = np.zeros_like(d, bool)
    for c in (2.0, 6.0):
        side = 1 if c == 2.0 else -1
        lx, lz = s - c, d - (0.6 + 1.0 + 0.45 * np.cos(np.pi * c / 4))
        u = lx / 1.2
        lf |= (np.abs(u) < 1) & (np.abs(lz - side * 0.45) < 0.38 * np.sqrt(np.clip(1 - u * u, 0, 1)))
    berry = np.hypot(s - 4.0, d - 1.6) < 0.32
    berry |= np.hypot(s - 0.0, d - 1.6) < 0.32
    return [(stem, STEM), (lf, LEAF), (berry, P.get('berry', ACCENT))]


def b_scallops(d, s, P):
    r = np.hypot(np.mod(s + 1, 2) - 1, d - 0.6)
    return [((r < 1.3) & (d > 0.6), LIGHT), ((r < 0.8) & (d > 0.6), P.get('inner', ACCENT)), (np.abs(d - 2.45) < 0.12, LIGHT)]


def b_waves(d, s, P):
    """Running waves (a Greek wave scroll, softened)."""
    ph = np.pi * s / 2
    crest = np.abs(d - (1.55 + 0.55 * np.sin(ph))) < 0.22
    curl = np.hypot(np.mod(s + 1, 4) - 2.6, d - 1.75) < 0.42
    return [(crest | curl, LIGHT), (np.abs(d - 2.5) < 0.1, LIGHT)]


def b_dots(d, s, P):
    big = np.hypot(np.mod(s, 2) - 1, d - 1.6) < 0.45
    small = np.hypot(np.mod(s + 1, 2) - 1, d - 1.6) < 0.2
    return [(big, LIGHT), (small, P.get('small', LIGHT)), (np.abs(d - 0.95) < 0.08, LIGHT), (np.abs(d - 2.3) < 0.08, LIGHT)]


def b_florets(d, s, P):
    x, z = np.mod(s, 4) - 2, d - 1.6
    r, th = np.hypot(x, z), np.arctan2(z, x)
    petal = r < 0.85 * (0.55 + 0.45 * np.abs(np.cos(2.5 * th)))
    dot = np.hypot(np.mod(s + 2, 4) - 2, d - 1.6) < 0.22
    return [(petal, LIGHT), (r < 0.25, ACCENT), (dot, LIGHT)]


def b_braid(d, s, P):
    a = np.abs(d - (1.6 + 0.55 * np.cos(np.pi * s / 2))) < 0.24
    b = np.abs(d - (1.6 - 0.55 * np.cos(np.pi * s / 2))) < 0.24
    return [(a, LIGHT), (b, P.get('second', ACCENT))]


def b_zigzag(d, s, P):
    tri = np.abs(np.mod(s, 2) - 1)
    return [(np.abs(d - (0.95 + 1.2 * tri)) < 0.26, LIGHT), (np.abs(d - 2.45) < 0.1, LIGHT)]


def b_plain(d, s, P):
    return [(np.abs(d - 1.3) < 0.1, LIGHT), (np.abs(d - 2.1) < 0.18, LIGHT)]


def b_stars(d, s, P):
    x, z = np.mod(s, 4) - 2, d - 1.6
    st = (np.abs(x) ** 0.6 + np.abs(z) ** 0.6) < 0.75 ** 0.6
    dot = np.hypot(np.mod(s + 2, 4) - 2, d - 1.6) < 0.16
    return [(st, LIGHT), (dot, LIGHT)]


def b_leaves(d, s, P):
    """A garland: leaves laid head to tail along the edge."""
    x = np.mod(s, 2) - 1
    blade = (np.abs(d - 1.6) < 0.5 * np.sqrt(np.clip(1 - x * x, 0, 1))) & (np.abs(x) < 0.95)
    vein = blade & (np.abs(d - 1.6) < 0.07)
    return [(blade, LEAF), (vein, STEM)]


def b_tassels(d, s, P):
    """Fringe-like stripes, short, with a band over (a woven kilim edge)."""
    stripes = (np.mod(s, 1) < 0.45) & (d > 0.9) & (d < 1.7)
    return [(stripes, LIGHT), (np.abs(d - 2.1) < 0.25, P.get('band', DEEP))]


def b_blocks(d, s, P):
    """A row of little blocks along the edge in two shades, pixel-crisp (the Minecraft colours)."""
    x = np.mod(s, 2)
    blk = (x > 0.3) & (x < 1.7) & (d > 1.0) & (d < 2.4)
    alt = np.mod(np.floor(s / 2), 2) == 1
    return [(blk & ~alt, LIGHT), (blk & alt, P.get('second', ACCENT))]


BORDERS = {name[2:]: fn for name, fn in globals().items() if name.startswith('b_')}


# ---- every colour's design ----------------------------------------------------------------------------
# (field, field params, border, border params, colour overrides). Colour overrides name a role and a
# palette code (or 'self', 'pale', 'bright', 'dusky', 'deep': this hue's tones).

GREEN_LEAF = {'leaf': 'pin_lim_lav', 'stem': 'pin_lim_myr'}
SPRING_LEAF = {'leaf': 'pin_lim_pin', 'stem': 'pin_lim_myr'}
DARK_LEAF = {'leaf': 'pin_pin_lav', 'stem': 'pin_pin_myr'}

DESIGNS = {
    # neutrals: warm
    'myr_myr_lim': ('minimal', {}, 'plain', {}, {}),                                           # Uncarved Block
    'myr_myr_pin': ('mushrooms', {}, 'vine', {}, {'accent': 'lim_myr_pin', 'extra': 'lim_myr_lav', **GREEN_LEAF}),   # Mushroom Mind
    'myr_myr_lav': ('plaid', {}, 'plain', {}, {'accent': 'lim_myr_lav', 'extra': 'lim_lav_lav'}),   # Dad's Recliner
    'myr_myr_myr': ('coffee', {}, 'dots', {}, {'accent': 'lim_myr_pin', 'deep': 'lim_myr_myr', 'light': 'myr_myr_pin', 'extra': 'lim_myr_lav'}),   # Espresso Epiphany
    'myr_lim_lim': ('cotton', {}, 'scallops', {}, {'light': 'myr_pin_lim', 'accent': 'pin_lim_lav', 'deep': 'pin_lim_myr', 'stem': 'lim_myr_lav'}),   # Cottonmouth White
    'myr_lim_pin': ('daisies', {'eye': EXTRA}, 'florets', {}, {'accent': 'myr_pin_lim', 'extra': 'lim_lim_pin', **GREEN_LEAF}),   # Buttercream Brain
    'myr_lim_lav': ('kilim', {}, 'zigzag', {}, {'accent': 'lim_lav_lav', 'extra': 'pin_lav_lav'}),   # Dusty Dashboard
    'myr_lim_myr': ('campfire', {}, 'tassels', {}, {'accent': 'lim_lav_pin', 'light': 'lim_lim_pin', 'extra': 'lim_lav_lim', 'stem': 'lim_myr_pin', 'deep': 'lim_myr_myr'}),   # Last Night's Fire Pit
    # neutrals: grey
    'myr_pin_lim': ('fanleaf', {}, 'plain', {}, {'accent': 'pin_lim_lav', 'light': 'pin_lim_lim', 'deep': 'pin_lim_myr', 'extra': 'pin_lim_pin', **GREEN_LEAF}),   # Rolling Paper White
    'myr_pin_pin': ('roses', {}, 'vine', {}, {'accent': 'lav_lim_lav', 'deep': 'lav_lim_myr', 'extra': 'lav_lim_lim', **DARK_LEAF}),   # Touch of Grey
    'myr_pin_lav': ('checker', {}, 'braid', {}, {'accent': 'myr_pin_pin', 'light': 'myr_lav_pin', 'deep': 'myr_pin_myr'}),                                           # Middle Way Grey
    'myr_pin_myr': ('smoke', {}, 'waves', {}, {'accent': 'myr_pin_lav', 'light': 'myr_pin_pin', 'extra': 'myr_lav_lav'}),   # Dave's Not Here
    # neutrals: cool
    'myr_lav_lim': ('smoke', {}, 'plain', {}, {'accent': 'myr_lav_pin', 'light': 'myr_pin_lim', 'extra': 'lav_pin_lim'}),   # First Exhale
    'myr_lav_pin': ('clouds', {'line': ACCENT}, 'scallops', {}, {'light': 'myr_lav_lim', 'extra': 'lav_pin_lim'}),   # Soundcheck Fog
    'myr_lav_lav': ('plaid', {}, 'braid', {}, {'accent': 'lav_pin_lav', 'extra': 'lav_lav_pin'}),   # Monday Morning Slate
    'myr_lav_myr': ('night', {'moon': True}, 'stars', {}, {'light': 'myr_lav_lim', 'accent': 'lav_pin_lim', 'extra': 'lim_lim_lim'}),   # The Void Stares Back
    # browns
    'lim_myr_lim': ('wheat', {}, 'braid', {}, {'accent': 'lim_lim_lav', 'deep': 'lim_myr_lav', 'light': 'myr_lim_lim'}),   # Graham Cracker Grin
    'lim_myr_pin': ('paisley', {}, 'dots', {}, {'accent': 'lim_lav_lav', 'extra': 'lim_lim_lav'}),   # Munchie Caramel
    'lim_myr_lav': ('paisley', {}, 'florets', {}, {'accent': 'lim_pin_lav', 'extra': 'pin_lav_lav', 'light': 'lim_myr_lim'}),   # Grandpa's Stash Box
    'lim_myr_myr': ('mushrooms', {}, 'leaves', {}, {'accent': 'lim_lav_lav', 'extra': 'lim_myr_pin', **DARK_LEAF}),   # Everything Returns to Soil
    # yellows
    'lim_lim_lim': ('daisies', {}, 'florets', {}, {'accent': 'myr_pin_lim', 'extra': 'lim_lim_pin', **SPRING_LEAF}),   # Butter Fingers
    'lim_lim_pin': ('sunflowers', {}, 'leaves', {}, {'accent': 'lim_lim_pin', 'light': 'lim_lim_lim', 'deep': 'lim_myr_myr', 'extra': 'lim_myr_pin', **GREEN_LEAF}),   # Highlighter
    'lim_lim_lav': ('retro', {}, 'zigzag', {}, {'accent': 'lim_lav_lav', 'light': 'lim_lim_lim', 'extra': 'lim_myr_myr', 'deep': 'lim_lim_myr'}),   # Mustard on the Couch
    'lim_lim_myr': ('sunburst', {}, 'braid', {}, {'accent': 'lim_lim_pin', 'light': 'lim_lim_lim', 'extra': 'lim_lav_pin'}),   # Old Gold Hippie
    # reds
    'lim_pin_lim': ('blossoms', {'n': 5}, 'vine', {}, {'accent': 'myr_pin_lim', 'deep': 'lim_pin_lav', 'extra': 'lav_lim_pin', **DARK_LEAF}),   # Blushing at the Dispensary
    'lim_pin_pin': ('blossoms', {'n': 5, 'R': 2.6, 'depth': 0.35}, 'leaves', {}, {'accent': 'lim_pin_myr', 'light': 'lim_lim_pin', 'extra': 'lim_lim_pin', **DARK_LEAF}),   # Scarlet Begonias
    'lim_pin_lav': ('kilim', {}, 'tassels', {}, {'accent': 'lim_pin_myr', 'extra': 'lim_myr_lim', 'light': 'lim_pin_lim'}),   # Red-Eye Flight
    'lim_pin_myr': ('hearts', {}, 'vine', {}, {'accent': 'lim_pin_pin', 'light': 'lim_pin_lim', 'extra': 'lav_lim_pin', **DARK_LEAF}),   # Heart of the Matter
    # oranges
    'lim_lav_lim': ('citrus', {}, 'leaves', {}, {'accent': 'lim_lav_pin', 'light': 'lim_lim_lim', 'extra': 'lim_lav_lav', **GREEN_LEAF}),   # Peach Fuzz Daydream
    'lim_lav_pin': ('smoke', {}, 'braid', {}, {'accent': 'lim_lav_lim', 'light': 'lim_lim_lim', 'extra': 'lim_lav_myr'}),   # Up in Smoke
    'lim_lav_lav': ('kilim', {'step': 1.4}, 'zigzag', {}, {'accent': 'lim_lav_myr', 'extra': 'pin_lav_lav', 'light': 'lim_lav_lim'}),   # Rusty Grinder
    'lim_lav_myr': ('sunburst', {}, 'tassels', {}, {'accent': 'lim_lav_pin', 'light': 'lim_lim_pin', 'extra': 'lim_pin_pin'}),   # Last Light at 4:20
    # olives
    'pin_myr_lim': ('olive', {}, 'leaves', {}, {'accent': 'pin_myr_lav', 'light': 'pin_myr_pin', 'deep': 'pin_myr_myr', 'extra': 'pin_lim_lav', **GREEN_LEAF}),   # Pistachio Pause
    'pin_myr_pin': ('olive', {}, 'braid', {}, {'accent': 'pin_myr_myr', 'light': 'pin_myr_lim', 'extra': 'lav_myr_myr', **GREEN_LEAF}),   # Dank Olive
    'pin_myr_lav': ('camo', {}, 'plain', {}, {'accent': 'pin_myr_myr', 'deep': 'lim_myr_myr', 'light': 'pin_myr_pin'}),   # Army Surplus Jacket
    'pin_myr_myr': ('bubbles', {}, 'dots', {}, {'light': 'pin_myr_lim', 'accent': 'pin_myr_lav', 'extra': 'pin_myr_pin'}),   # Ancient Bong Water
    # limes
    'pin_lim_lim': ('citrus', {}, 'vine', {}, {'accent': 'pin_lim_pin', 'light': 'lim_lim_lim', 'extra': 'pin_lim_lav', 'leaf': 'pin_lim_lav', 'stem': 'pin_lim_myr'}),   # Key Lime Contact High
    'pin_lim_pin': ('fanleaf', {}, 'leaves', {}, {'accent': 'pin_lim_myr', 'light': 'pin_lim_lim', 'deep': 'pin_pin_myr', 'extra': 'lim_lim_pin', 'leaf': 'pin_lim_myr', 'stem': 'pin_pin_myr'}),   # Grandma's Glaucoma Green
    'pin_lim_lav': ('leaves', {'bend': 0.18}, 'vine', {}, {'accent': 'pin_lim_lim', 'light': 'pin_myr_lim', 'deep': 'pin_lim_myr', 'extra': 'pin_pin_lav', 'leaf': 'pin_lim_lim', 'stem': 'pin_lim_myr'}),   # Sage Advice
    'pin_lim_myr': ('fanleaf', {}, 'vine', {}, {'accent': 'pin_lim_pin', 'light': 'pin_lim_lim', 'deep': 'pin_pin_myr', 'extra': 'lim_lim_pin', 'leaf': 'pin_lim_pin', 'stem': 'pin_lim_lim'}),   # Leaf of Faith
    # greens
    'pin_pin_lim': ('leaves', {'serrate': 0.12}, 'leaves', {}, {'accent': 'pin_pin_pin', 'light': 'pin_pin_lav', 'deep': 'pin_pin_myr', 'extra': 'pin_lav_pin', 'leaf': 'pin_pin_pin', 'stem': 'pin_pin_myr'}),   # Mint Condition Mind
    'pin_pin_pin': ('blossoms', {'n': 8, 'R': 2.0}, 'leaves', {}, {'accent': 'pin_pin_lim', 'light': 'lim_lim_lim', 'deep': 'pin_pin_myr', 'extra': 'lim_lim_pin', 'leaf': 'pin_pin_myr', 'stem': 'pin_pin_myr'}),   # Kind Bud Green
    'pin_pin_lav': ('ferns', {}, 'leaves', {}, {'accent': 'pin_pin_lim', 'deep': 'pin_pin_myr', 'leaf': 'pin_pin_lim', 'stem': 'pin_pin_myr'}),   # Patience of Plants
    'pin_pin_myr': ('pines', {}, 'leaves', {}, {'accent': 'pin_pin_pin', 'light': 'pin_pin_lav', 'deep': 'lim_myr_myr', 'extra': 'pin_pin_lim', 'leaf': 'pin_pin_lav', 'stem': 'lim_myr_lav'}),   # Forest for the Trees
    # teals
    'pin_lav_lim': ('ripples', {}, 'scallops', {}, {'accent': 'pin_lav_pin', 'deep': 'pin_lav_lav', 'light': 'myr_pin_lim', 'extra': 'pin_lav_pin'}),   # Seafoam Sesh
    'pin_lav_pin': ('bubbles', {}, 'scallops', {}, {'light': 'pin_lav_lim', 'accent': 'pin_lav_lav', 'extra': 'myr_pin_lim'}),   # Hippie Hot Tub
    'pin_lav_lav': ('lotus', {}, 'braid', {}, {'accent': 'pin_lav_lim', 'light': 'myr_lim_pin', 'deep': 'pin_lav_myr', 'extra': 'lim_lim_lav', **DARK_LEAF}),   # Patina of Time
    'pin_lav_myr': ('scales', {}, 'waves', {}, {'accent': 'pin_lav_pin', 'light': 'pin_lav_lim'}),   # Deep End of the Session
    # purples
    'lav_myr_lim': ('lavender', {}, 'leaves', {}, {'accent': 'lav_myr_pin', 'light': 'lav_myr_lav', 'deep': 'lav_myr_myr', **GREEN_LEAF}),   # Lavender Laughing Fit
    'lav_myr_pin': ('lotus', {}, 'florets', {}, {'accent': 'lav_lim_pin', 'light': 'lav_myr_lim', 'deep': 'lav_myr_myr', 'extra': 'lim_lim_pin', **DARK_LEAF}),   # Ego Death Violet
    'lav_myr_lav': ('grapes', {}, 'vine', {'berry': ACCENT}, {'accent': 'lav_myr_myr', 'light': 'lav_myr_lim', **GREEN_LEAF}),   # Grape Ape Nap
    'lav_myr_myr': ('night', {}, 'stars', {}, {'light': 'lim_lim_lim', 'accent': 'lav_myr_lim', 'extra': 'lav_myr_pin'}),   # Dark Star Indigo
    # pinks
    'lav_lim_lim': ('blossoms', {'n': 6, 'R': 2.6, 'depth': 0.3}, 'vine', {}, {'accent': 'myr_pin_lim', 'deep': 'lav_lim_lav', 'light': 'lav_lim_pin', 'extra': 'lav_lim_pin', **GREEN_LEAF}),   # Sugar Magnolia
    'lav_lim_pin': ('hibiscus', {}, 'leaves', {}, {'accent': 'lav_lim_lim', 'deep': 'lav_lim_myr', 'light': 'lim_lim_lim', 'extra': 'lim_lim_pin', **DARK_LEAF}),   # Paranoid Pink
    'lav_lim_lav': ('roses', {}, 'scallops', {}, {'accent': 'lav_lim_lim', 'deep': 'lav_lim_myr', 'extra': 'lav_lim_pin', **GREEN_LEAF}),   # Mauve Over, Man
    'lav_lim_myr': ('medallion', {}, 'braid', {}, {'accent': 'lav_lim_pin', 'light': 'lav_lim_lim', 'deep': 'lav_myr_myr', 'extra': 'lim_lim_lav', **DARK_LEAF}),   # Wine Before Noon
    # blues
    'lav_pin_lim': ('clouds', {}, 'scallops', {'inner': EXTRA}, {'light': 'myr_pin_lim', 'accent': 'lav_pin_pin', 'extra': 'lim_lim_lim'}),   # Cloud Watching Blue
    'lav_pin_pin': ('scales', {}, 'scallops', {}, {'accent': 'lav_pin_lim', 'light': 'myr_pin_lim'}),   # Big Blue Thought
    'lav_pin_lav': ('plaid', {'period': 4, 'width': 1.2, 'fine': False}, 'plain', {}, {'accent': 'lav_pin_myr', 'light': 'myr_lav_lim', 'extra': 'lim_lim_lav', 'deep': 'lav_pin_myr'}),   # Denim on Denim
    'lav_pin_myr': ('waves', {}, 'waves', {}, {'accent': 'lav_pin_pin', 'deep': 'lav_pin_lav', 'light': 'myr_pin_lim', 'extra': 'lav_pin_lim'}),   # Wake of the Flood
    'lav_lav_lim': ('night', {'planet': True}, 'stars', {}, {'light': 'myr_pin_lim', 'accent': 'lav_lav_pin', 'extra': 'lim_lav_pin'}),   # Space Cadet Sky
    'lav_lav_pin': ('rings', {}, 'braid', {}, {'accent': 'lav_lav_myr', 'light': 'lav_lav_lim', 'extra': 'lim_lim_pin'}),   # Everything Is Connected, Man
    'lav_lav_lav': ('clouds', {'rain': True, 'line': DEEP}, 'dots', {}, {'light': 'myr_lav_lim', 'accent': 'lav_lav_lim'}),   # Overcast Overthinking
    'lav_lav_myr': ('night', {'moon': True}, 'dots', {}, {'light': 'lim_lim_lim', 'accent': 'lav_lav_pin', 'extra': 'lim_lim_lim'}),   # 3 A.M. Deep Thoughts
    # the Minecraft colours (overrides may be hex colours)
    'mc_white': ('bones', {}, 'blocks', {}, {'accent': '#E4DFD0', 'light': '#FFFFFF', 'deep': '#B9B4A8', 'band': '#D8D4C8'}),   # Bone Meal Haze
    'mc_orange': ('blaze', {}, 'blocks', {'second': DEEP}, {'accent': '#FFB43A', 'light': '#FFE45C', 'deep': '#9A4A06', 'extra': '#FFF2A0'}),   # Blaze Rod Glow
    'mc_magenta': ('alliums', {}, 'blocks', {}, {'accent': '#A93AB0', 'light': '#F2A6F4', 'extra': '#F2A6F4', **GREEN_LEAF}),   # Allium Afterglow
    'mc_light_blue': ('skyblock', {}, 'blocks', {}, {'light': '#F4FBFF', 'accent': '#1F8FC8', 'leaf': '#61A83C', 'stem': '#7A5232', 'extra': '#8CD05A'}),   # Sky Block Sesh
    'mc_yellow': ('nuggets', {}, 'blocks', {}, {'accent': '#F2B712', 'deep': '#B07A08', 'light': '#FFFBD0'}),   # Gold Nugget Giggles
    'mc_lime': ('slime', {}, 'blocks', {}, {'accent': '#A6DC6A', 'light': '#6FA531', 'deep': '#355A16'}),   # Slime Time
    'mc_pink': ('sheep', {}, 'blocks', {}, {'light': '#FFC8D6', 'accent': '#D7738B', 'extra': '#E8C9A8', 'deep': '#3A2A2A'}),   # Pink Sheep Shuffle
    'mc_gray': ('cobble', {}, 'blocks', {'second': EXTRA}, {'accent': '#6E6E6E', 'light': '#8A8A8A', 'extra': '#5E5E5E', 'leaf': '#5C7A34'}),   # Cobble Thoughts
    'mc_light_gray': ('gravel', {}, 'blocks', {}, {'accent': '#86827E', 'deep': '#6E6A66', 'light': '#C6C4C0'}),   # Gravel Gaze
    'mc_cyan': ('diamonds', {}, 'blocks', {}, {'light': '#B9F6F2', 'accent': '#4ED8D2', 'extra': '#FFFFFF'}),   # Diamond Hands
    'mc_purple': ('portal', {}, 'blocks', {}, {'accent': '#A65AF0', 'light': '#E2B8FF', 'extra': '#E2B8FF', 'deep': '#3E1670'}),   # Portal Daze
    'mc_blue': ('lapis', {}, 'blocks', {}, {'accent': '#4F66E8', 'light': '#9AAAF8', 'extra': '#F4C430'}),   # Lapis Lounge
    'mc_brown': ('dirt', {}, 'blocks', {'second': LEAF}, {'deep': '#5A3B26', 'light': '#9A7050', 'leaf': '#61A83C'}),   # Dirt Block Daydream
    'mc_green': ('creeper', {}, 'blocks', {'second': LEAF}, {'leaf': '#7FB238', 'light': '#A7D069', 'deep': '#101010'}),   # Creeper Kush
    'mc_red': ('redstone', {}, 'blocks', {}, {'accent': '#FF2A1E', 'light': '#FFB0A0', 'deep': '#5E0F0C', 'stem': '#8A8A8A'}),   # Redstone Rush
    'mc_black': ('obsidian', {}, 'blocks', {'second': EXTRA}, {'accent': '#2C2240', 'deep': '#0E0C14', 'extra': '#7A3FC0', 'light': '#C8A8FF'}),   # Obsidian Couchlock
    # Natural Hemp
    'natural': ('fanleaf', {}, 'tassels', {}, {'accent': 'pin_lim_lav', 'deep': 'pin_lim_myr', 'light': 'myr_lim_lim', 'extra': 'lim_myr_pin', **GREEN_LEAF}),
}

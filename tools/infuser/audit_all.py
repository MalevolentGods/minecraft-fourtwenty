"""Choose and audit recipes for all 80 colours (16 Minecraft + 64 hemp).

For each colour: candidate recipes (1-3 resins, small whole-number parts) from the resins available at
that stage, ranked by drift from the colour + complexity; the first candidate a GREEDY feedback-only
player can find (starting with a glob of one resin, best single bead each step, Swell allowed) wins.
"""
import itertools, json, sys
from multiprocessing import Pool
import numpy as np
from scipy.optimize import minimize
import tint_model as T
from targets import lab_fast, de2000

STARTER = ['white', 'black', 'lemon', 'orange', 'scarlet', 'crimson', 'ultramarine', 'phthalo_blue']
FAMILY_RESIN = {'myr': 'oxide_brown', 'lim': 'gold', 'pin': 'phthalo_green', 'lav': 'violet'}
SIZES = (128, 4, 2, 1)
NOMINAL = 3.0
MAXPART = 8      # every part but the biggest
MAXBIG = 32      # the biggest part (white-heavy pastels)
MINSEP = 4.0     # every target at least this far (dE) from every other


def lab_parts(idx, parts, total=NOMINAL):
    a = np.zeros(len(T.NAMES)); p = np.array(parts, float); a[list(idx)] = p / p.sum() * total
    return lab_fast(a)


def complexity(parts):
    p = sorted(parts)
    return 1.2 * (len(p) - 1) + 0.18 * (sum(p[:-1]) - (len(p) - 1)) + 0.02 * p[-1]


def candidates(tgt, pool, maxr):
    out = []
    idx_all = [T.NAMES.index(n) for n in pool]
    for r in range(1, maxr + 1):
        for combo in itertools.combinations(idx_all, r):
            if r == 1:
                d = de2000(lab_parts(combo, [1]), tgt); out.append((d + complexity([1]), d, combo, (1,))); continue
            def obj(c): return de2000(lab_parts(combo, np.maximum(c, 1e-4)), tgt)
            best = (1e9, None)
            for x0 in ([1.0] * r, [0.25] + [1.0] * (r - 1), [1.0] * (r - 1) + [0.25]):
                res = minimize(obj, np.array(x0), method='L-BFGS-B', bounds=[(1e-4, 50)] * r)
                if res.fun < best[0]: best = (res.fun, res.x)
            if best[0] > 12: continue
            ratio = best[1] / best[1].max()
            seen = set()
            for p in itertools.product(range(1, 5), repeat=r):      # every simple recipe, parts 1-4
                g = np.gcd.reduce(np.array(p)); p = tuple(int(v // g) for v in p)
                if p in seen: continue
                seen.add(p)
                d = de2000(lab_parts(combo, p), tgt)
                if d < 10: out.append((d + complexity(p), d, combo, p))
            for top in list(range(1, 17)) + [20, 24, 28, 32]:
                base = np.maximum(1, np.round(ratio * top)).astype(int)
                for delta in itertools.product((-1, 0, 1), repeat=r):
                    p = np.maximum(1, base + np.array(delta))
                    if p.max() > MAXBIG or sorted(p)[-2] > MAXPART: continue
                    g = np.gcd.reduce(p); p = tuple(int(v // g) for v in p)
                    if p in seen: continue
                    seen.add(p)
                    d = de2000(lab_parts(combo, p), tgt)
                    out.append((d + complexity(p), d, combo, p))
    out.sort()
    return out


def apply(q, m):
    q = q.copy()
    if m == 'swell': q *= 2
    else: q[m[0]] += m[1] / 128
    return q


def greedy(L0, avail, start, budget=120):
    """Starts with a glob of `start`; each step keeps the single bead (or Swell) that helps most."""
    q = np.zeros(len(T.NAMES)); q[start] = 1.0
    cur = de2000(lab_fast(q), L0); n = 1
    moves = [(i, s) for i in avail for s in SIZES] + ['swell']
    while cur > 1.0 and n < budget:
        best = None
        for m in moves:
            q1 = apply(q, m)
            if q1.sum() > T.CAP + 1e-9: continue
            d = de2000(lab_fast(q1), L0)
            if best is None or d < best[0]: best = (d, m)
        if best is None or best[0] >= cur - 1e-6: break
        cur, m = best; q = apply(q, m); n += 1
    return cur, n, q


def solve(job):
    cid, name, hexc, pool, maxr = job
    tgt = T.lab_of_hex(hexc)
    avail = [T.NAMES.index(n) for n in pool]
    tried = 0; passing = []
    for score, d, combo, parts in candidates(tgt, pool, maxr):
        if d > 10 or tried >= 150 or len(passing) >= 50: break
        tried += 1
        L0 = lab_parts(combo, parts)
        results = [greedy(L0, avail, st)[:2] + (T.NAMES[st],) for st in combo]   # natural starts: the recipe's own resins
        ok = [r for r in results if r[0] <= 1.0]
        if ok:
            best = min(ok, key=lambda r: r[1])
            passing.append((score + 0.25 * best[1] + (6.0 if (cid in T.VANILLA and best[1] > 10) else 0.0), d, combo, parts, L0, best[1], [r[2] for r in ok]))
    out = []
    for sc, d, combo, parts, L0, moves, starts in sorted(passing, key=lambda x: x[0]):
        a = np.zeros(len(T.NAMES)); a[list(combo)] = np.array(parts, float) / sum(parts) * NOMINAL
        out.append(dict(score=round(sc, 3), hex=T.hex_of(T.reflectance(a)), lab=[float(v) for v in L0],
                        parts={T.NAMES[i]: int(parts[k]) for k, i in enumerate(combo)}, drift=round(d, 2), moves=moves, starts=starts))
    return dict(id=cid, name=name, colour=hexc, cands=out, tried=tried)


def jobs():
    out = []
    for k, h in T.VANILLA.items():
        out.append((k, k.replace('_', ' ').title(), h, STARTER, {'white': 1, 'black': 2, 'gray': 2, 'light_gray': 2}.get(k, 3)))
    for c in json.load(open(r'C:/Users/Ignus/minecraft-fourtwenty/tools/terpenes.json'))['colors']:
        out.append((c['code'], c['name'], c['hex'], STARTER + [FAMILY_RESIN[c['code'][:3]]], 3))
    return out


if __name__ == '__main__':
    J = jobs()
    with Pool(14) as p:
        res = list(p.imap(solve, J))
    # Twins: original colours within TWIN of each other share one target; catching it unlocks all of them.
    TWIN = 5.1
    srcl = {r['id']: T.lab_of_hex(r['colour']) for r in res}
    leader = {}
    for r in res:
        if r['id'] in leader: continue
        leader[r['id']] = r['id']
        for o in res:
            if o['id'] not in leader and de2000(srcl[r['id']], srcl[o['id']]) < TWIN: leader[o['id']] = r['id']
    chosen = []
    for r in res:                                # Minecraft 16 first, then hemp in codex order
        lead = leader[r['id']]
        if lead != r['id']:
            c = next(x for x in chosen if x['id'] == lead)
            tw = dict(c, id=r['id'], name=r['name'], colour=r['colour'], twin_of=lead,
                      drift=round(de2000(srcl[r['id']], np.array(c['lab'])), 2))
            chosen.append(tw)
            print(f"{r['id']:14} {r['name'][:22]:22} twin of {lead}: shares {c['hex']} (drift {tw['drift']})", flush=True)
            continue
        others = [o for o in chosen if leader[o['id']] != r['id']]
        members = [m for m in leader if leader[m] == r['id']]
        def group_score(c):   # a shared target should suit every twin: judge it by the worst drift in the group
            worst = max(de2000(srcl[m], np.array(c['lab'])) for m in members)
            return c['score'] - c['drift'] + worst
        best = None
        for c in sorted(r['cands'], key=group_score):
            sep = min([de2000(np.array(c['lab']), np.array(o['lab'])) for o in others] or [99])
            if sep >= MINSEP: best = c; break
        if best is None and r['cands']:
            best = max(r['cands'], key=lambda c: min([de2000(np.array(c['lab']), np.array(o['lab'])) for o in others] or [99]))
            best = dict(best, warn='crowded')
        if best is None:
            print(f"{r['id']:14} FAILED ({r['tried']} tried)"); continue
        best = dict(best, id=r['id'], name=r['name'], colour=r['colour'])
        best['sep'] = round(min([de2000(np.array(best['lab']), np.array(o['lab'])) for o in others] or [99]), 1)
        chosen.append(best)
        print(f"{r['id']:14} {r['name'][:22]:22} {r['colour']}->{best['hex']} drift {best['drift']:4.2f} sep {best['sep']:4.1f} moves {best['moves']:2} {best['parts']} {best.get('warn','')}", flush=True)
    for c in chosen:
        c['twins'] = [o['id'] for o in chosen if leader[o['id']] == leader[c['id']] and o['id'] != c['id']]
    json.dump(chosen, open('audit_all.json', 'w'), indent=1)
    print(len(chosen), 'chosen;', sum(1 for c in chosen if c.get('warn')), 'crowded')

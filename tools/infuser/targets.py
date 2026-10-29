"""Pick each colour's target as a SIMPLE findable recipe: few resins, amounts in whole drops (4 specks),
as close to the Minecraft colour as possible; the target is that recipe's colour, so it is exactly reachable."""
import itertools, json, sys
import numpy as np
from scipy.optimize import minimize
import tint_model as T

M = np.array([[3.2404542, -1.5371385, -0.4985314], [-0.9692660, 1.8760108, 0.0415560], [0.0556434, -0.2040259, 1.0572252]])
WX = T.WHITE_XYZ
W = T._W / T._N

def lab_fast(a):
    K = T.BASE_K + a @ T.KM; S = T.BASE_S + a @ T.SM; ks = K / S
    R = 1 + ks - np.sqrt(ks * ks + 2 * ks); xyz = R @ W
    f = lambda t: np.where(t > 216 / 24389, np.cbrt(t), (24389 / 27 * t + 16) / 116)
    fx, fy, fz = f(xyz / WX)
    return np.array([116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)])

def de2000(l1, l2):
    L1, a1, b1 = l1; L2, a2, b2 = l2
    C1, C2 = np.hypot(a1, b1), np.hypot(a2, b2); Cb = (C1 + C2) / 2
    G = 0.5 * (1 - np.sqrt(Cb ** 7 / (Cb ** 7 + 25 ** 7)))
    a1p, a2p = (1 + G) * a1, (1 + G) * a2
    C1p, C2p = np.hypot(a1p, b1), np.hypot(a2p, b2)
    h1p = np.degrees(np.arctan2(b1, a1p)) % 360; h2p = np.degrees(np.arctan2(b2, a2p)) % 360
    dLp, dCp = L2 - L1, C2p - C1p
    dhp = h2p - h1p
    if C1p * C2p == 0: dhp = 0
    elif dhp > 180: dhp -= 360
    elif dhp < -180: dhp += 360
    dHp = 2 * np.sqrt(C1p * C2p) * np.sin(np.radians(dhp / 2))
    Lbp, Cbp = (L1 + L2) / 2, (C1p + C2p) / 2
    hbp = h1p + h2p
    if C1p * C2p != 0:
        if abs(h1p - h2p) > 180: hbp += 360 if h1p + h2p < 360 else -360
        hbp /= 2
    Tt = 1 - 0.17 * np.cos(np.radians(hbp - 30)) + 0.24 * np.cos(np.radians(2 * hbp)) + 0.32 * np.cos(np.radians(3 * hbp + 6)) - 0.2 * np.cos(np.radians(4 * hbp - 63))
    dTh = 30 * np.exp(-((hbp - 275) / 25) ** 2); Rc = 2 * np.sqrt(Cbp ** 7 / (Cbp ** 7 + 25 ** 7))
    Sl = 1 + 0.015 * (Lbp - 50) ** 2 / np.sqrt(20 + (Lbp - 50) ** 2); Sc = 1 + 0.045 * Cbp; Sh = 1 + 0.015 * Cbp * Tt
    Rt = -np.sin(np.radians(2 * dTh)) * Rc
    return float(np.sqrt((dLp / Sl) ** 2 + (dCp / Sc) ** 2 + (dHp / Sh) ** 2 + Rt * (dCp / Sc) * (dHp / Sh)))

STEP = 4   # whole drops

def fit(tgt, idx):
    def obj(c):
        a = np.zeros(len(T.NAMES)); a[idx] = c
        return de2000(lab_fast(a), tgt) + 50 * max(0, c.sum() - T.CAP)
    best = (1e9, None)
    for x0 in ([1.0] * len(idx), [0.2] * len(idx), [3.0] * len(idx)):
        r = minimize(obj, np.array(x0), method='L-BFGS-B', bounds=[(0.03, T.CAP)] * len(idx))
        if r.fun < best[0]: best = (r.fun, r.x)
    q = np.maximum(1, np.round(best[1] * 128 / STEP)).astype(int)   # in drops, every chosen resin used
    f = lambda q: obj(q * STEP / 128) if q.min() >= 1 and q.sum() * STEP <= T.CAP * 128 else 1e9
    cur = f(q); imp = True
    while imp:
        imp = False
        for i in range(len(idx)):
            for d in (-8, -4, -2, -1, 1, 2, 4, 8):
                q2 = q.copy(); q2[i] += d; v = f(q2)
                if v < cur - 1e-9: q, cur, imp = q2, v, True
    return cur, q

def choose(name, hexc, pool, max_r, penalty=1.6):
    tgt = T.lab_of_hex(hexc); idx_all = [T.NAMES.index(n) for n in pool]
    best = None
    for r in range(1, max_r + 1):
        for combo in itertools.combinations(idx_all, r):
            d, q = fit(tgt, list(combo))
            score = d + penalty * (r - 1)
            if best is None or score < best[0]: best = (score, d, combo, q)
    _, d, combo, q = best
    a = np.zeros(len(T.NAMES)); a[list(combo)] = q * STEP / 128
    R = T.reflectance(a)
    return dict(id=name, vanilla=hexc, hex=T.hex_of(R), lab=[round(float(v), 3) for v in lab_fast(a)],
                drift=round(d, 2), recipe={T.NAMES[i]: int(q[k]) for k, i in enumerate(combo)}, globs=round(q.sum() * STEP / 128, 2))

STARTER = ['white', 'black', 'lemon', 'orange', 'scarlet', 'crimson', 'ultramarine', 'phthalo_blue']
MAXR = {'white': 1, 'black': 2, 'gray': 2, 'light_gray': 2}

if __name__ == '__main__':
    out = []
    for k, h in T.VANILLA.items():
        t = choose(k, h, STARTER, MAXR.get(k, 3))
        out.append(t)
        print(f"{k:11} {h} -> {t['hex']}  drift {t['drift']:5.2f}  {t['globs']:5.2f} globs  {t['recipe']}", flush=True)
    json.dump(out, open('targets.json', 'w'), indent=1)

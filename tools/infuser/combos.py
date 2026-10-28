"""For every target: the resin sets (1-3 resins of that stage) that can make it on their own, with the best ratio."""
import itertools, json
from multiprocessing import Pool
import numpy as np
from scipy.optimize import minimize
import tint_model as T
from targets import lab_fast, de2000

d = json.load(open('tint_data.json'))
STARTER = d['starter']

def lab_ratio(idx, w):
    a = np.zeros(12); w = np.maximum(np.array(w, float), 1e-6); a[list(idx)] = w / w.sum() * 3.0
    return lab_fast(a)

def work(t):
    pool = STARTER + ([t['extra']] if t.get('extra') else [])
    idx_all = [T.NAMES.index(n) for n in pool]
    L0 = np.array(t['lab']); out = []
    for r in (1, 2, 3):
        for combo in itertools.combinations(idx_all, r):
            if r == 1:
                v = de2000(lab_ratio(combo, [1]), L0); w = [1.0]
            else:
                best = (1e9, None)
                for x0 in ([1.0] * r, [0.1] + [1.0] * (r - 1), [1.0] * (r - 1) + [0.1], [3.0] + [0.3] * (r - 1)):
                    res = minimize(lambda c: de2000(lab_ratio(combo, c), L0), np.array(x0), method='L-BFGS-B', bounds=[(1e-4, 40)] * r)
                    if res.fun < best[0]: best = (res.fun, res.x)
                v, w = best[0], list(best[1] / best[1].max())
            if v <= 0.8 and min(w) >= 1 / 40:          # a real recipe: matches, no part below 1/40 of the biggest
                out.append(dict(r=[T.NAMES[i] for i in combo], w=[round(float(x), 4) for x in w], de=round(float(v), 2)))
    # drop supersets of a valid smaller set only if they add nothing (keep them: they are alternatives) -> keep all
    return t['id'], out

if __name__ == '__main__':
    with Pool(14) as p:
        res = dict(p.map(work, d['targets']))
    for t in d['targets']:
        t['combos'] = res[t['id']]
    json.dump(d, open('tint_data.json', 'w'), separators=(',', ':'))
    for t in d['targets'][:16]:
        print(f"{t['name']:12}", len(t['combos']), 'valid sets:', '; '.join('+'.join(c['r']) for c in t['combos'][:8]))
    import statistics
    print('sets per colour: min', min(len(t['combos']) for t in d['targets']), 'median', statistics.median(len(t['combos']) for t in d['targets']), 'max', max(len(t['combos']) for t in d['targets']))
    print('none:', [t['name'] for t in d['targets'] if not t['combos']])

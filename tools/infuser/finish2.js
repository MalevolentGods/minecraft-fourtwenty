/* The green/red indicator: can this pool still become a REASONABLE match? Reasonable = one of the colour's valid resin
   sets (1-3 resins that make it on their own), with a little wiggle room for anything else (WIGGLE of the pool),
   finished by adding only that set's resins within the space left. Resin can't be taken out, only added. */
const WIGGLE = 0.04;
function finishWith(q, lab0, idx, w, free) {
  const ev = add => de2000(toLab(mixXyz(q.map((v, i) => v + (add[i] || 0)))), lab0);
  const sumW = w.reduce((a, b) => a + b, 0);
  let s = 0; idx.forEach((i, k) => { s = Math.max(s, q[i] / w[k]); });
  const starts = [];
  for (const scale of [s, s * 1.3 + 2, s * 2 + 8, (free + idx.reduce((a, i) => a + q[i], 0)) / sumW]) {
    const add = new Array(N).fill(0); let used = 0;
    idx.forEach((i, k) => { add[i] = Math.max(0, scale * w[k] - q[i]); used += add[i]; });
    if (used > free) { const f = free / used; idx.forEach(i => { add[i] *= f; }); }
    starts.push(add);
  }
  let best = Infinity;
  for (const st of starts) {
    let add = st.slice(), cur = ev(add), step = Math.max(1, free / 2);
    while (step >= 0.25 && cur > 0.6) {
      let improved = false;
      const moves = [];
      for (const i of idx) for (const d of [1, -1]) moves.push([[i, d]]);
      for (const i of idx) for (const j of idx) if (i < j) for (const [di, dj] of [[1, 1], [1, -1], [-1, 1]]) moves.push([[i, di], [j, dj]]);
      for (const mv of moves) {
        const a2 = add.slice(); for (const [i, d] of mv) a2[i] = Math.max(0, a2[i] + d * step);
        if (a2.reduce((x, y) => x + y, 0) > free + 1e-9) continue;
        const v = ev(a2);
        if (v < cur - 1e-6) { add = a2; cur = v; improved = true; }
      }
      if (!improved) step /= 2;
    }
    best = Math.min(best, cur);
    if (best <= 1.0) break;
  }
  return best;
}
function canFinish(q, t, capSpecks) {
  const tot = q.reduce((a, b) => a + b, 0), free = capSpecks - tot;
  for (const c of t.combos) {
    const idx = c.r.map(n => D.names.indexOf(n));
    const stray = q.reduce((a, v, i) => a + (idx.includes(i) ? 0 : v), 0);
    if (stray > WIGGLE * tot + 1e-9) continue;
    if (finishWith(q, t.lab, idx, c.w, free) <= 1.0) return true;
  }
  return false;
}

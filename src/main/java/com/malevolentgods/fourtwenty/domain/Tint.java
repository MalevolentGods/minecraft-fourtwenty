package com.malevolentgods.fourtwenty.domain;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Inspiration Infuser's colour model: resins dropped into a pool, mixed with two-constant Kubelka-Munk over 31
 * bands and seen in D65 daylight; colour difference is CIEDE2000. The numbers (resin spectra, the 80 targets, their
 * recipes and valid resin sets) come from assets/fourtwenty/infuser/tint.json, made by tools/infuser (the same model the
 * prototype and the recipe audit use). Pure: no game classes, so the client draws with it and the server checks
 * catches with it.
 *
 * Amounts are in specks: 128 specks = 1 glob; the pool holds {@link #capSpecks()}.
 */
public final class Tint {
    public static final String RESOURCE = "/assets/fourtwenty/infuser/tint.json";
    public static final int SPECKS_PER_GLOB = 128;
    /** A match: the pool within this CIEDE2000 distance of the target. */
    public static final double MATCH = 1.0;
    /** "Spot on". */
    public static final double SPOT_ON = 0.5;
    /** Stray resin a reasonable recipe forgives, as a share of the pool. */
    public static final double WIGGLE = 0.04;

    public record Combo(int[] resins, double[] weights) {}

    public record Target(String id, String name, String family, String extra, int rgb, double[] lab,
                         Map<String, Integer> parts, List<Combo> combos, List<String> twins, int moves) {
        public boolean minecraft() { return "Minecraft".equals(family); }
    }

    private final int cap;
    private final List<String> resins;
    private final double[][] k, s, w;
    private final double[] baseK, baseS, white;
    private final List<String> starter;
    private final Map<String, int[]> swatch;
    private final List<Target> targets;
    private final Map<String, Target> byId = new LinkedHashMap<>();
    private final int emptyRgb;

    private static Tint instance;

    /** The bundled data (loaded once). */
    public static synchronized Tint get() {
        if (instance == null) {
            try (InputStream in = Tint.class.getResourceAsStream(RESOURCE)) {
                if (in == null) throw new IllegalStateException("missing " + RESOURCE);
                instance = load(in);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return instance;
    }

    public static Tint load(InputStream in) {
        return new Tint(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
    }

    private static double[] doubles(JsonArray a) {
        double[] out = new double[a.size()];
        for (int i = 0; i < out.length; i++) out[i] = a.get(i).getAsDouble();
        return out;
    }

    private static double[][] matrix(JsonArray a) {
        double[][] out = new double[a.size()][];
        for (int i = 0; i < out.length; i++) out[i] = doubles(a.get(i).getAsJsonArray());
        return out;
    }

    private static int hex(String h) { return Integer.parseInt(h.substring(1), 16); }

    private Tint(JsonObject j) {
        cap = (int) Math.round(j.get("cap").getAsDouble() * SPECKS_PER_GLOB);
        List<String> names = new ArrayList<>();
        j.getAsJsonArray("names").forEach(e -> names.add(e.getAsString()));
        resins = Collections.unmodifiableList(names);
        k = matrix(j.getAsJsonArray("K"));
        s = matrix(j.getAsJsonArray("S"));
        w = matrix(j.getAsJsonArray("W"));
        baseK = doubles(j.getAsJsonArray("baseK"));
        baseS = doubles(j.getAsJsonArray("baseS"));
        white = doubles(j.getAsJsonArray("white"));
        List<String> st = new ArrayList<>();
        j.getAsJsonArray("starter").forEach(e -> st.add(e.getAsString()));
        starter = Collections.unmodifiableList(st);
        swatch = new LinkedHashMap<>();
        for (var e : j.getAsJsonObject("swatch").entrySet()) {
            JsonArray pair = e.getValue().getAsJsonArray();
            swatch.put(e.getKey(), new int[]{hex(pair.get(0).getAsString()), hex(pair.get(1).getAsString())});
        }
        emptyRgb = hex(j.get("empty").getAsString());
        List<Target> ts = new ArrayList<>();
        for (JsonElement el : j.getAsJsonArray("targets")) {
            JsonObject t = el.getAsJsonObject();
            Map<String, Integer> parts = new LinkedHashMap<>();
            for (var e : t.getAsJsonObject("parts").entrySet()) parts.put(e.getKey(), e.getValue().getAsInt());
            List<Combo> combos = new ArrayList<>();
            for (JsonElement ce : t.getAsJsonArray("combos")) {
                JsonObject c = ce.getAsJsonObject();
                JsonArray r = c.getAsJsonArray("r");
                int[] idx = new int[r.size()];
                for (int i = 0; i < idx.length; i++) idx[i] = names.indexOf(r.get(i).getAsString());
                combos.add(new Combo(idx, doubles(c.getAsJsonArray("w"))));
            }
            List<String> twins = new ArrayList<>();
            if (t.has("twins")) t.getAsJsonArray("twins").forEach(e -> twins.add(e.getAsString()));
            String extra = t.has("extra") && !t.get("extra").isJsonNull() ? t.get("extra").getAsString() : null;
            Target target = new Target(t.get("id").getAsString(), t.get("name").getAsString(), t.get("family").getAsString(), extra,
                    hex(t.get("hex").getAsString()), doubles(t.getAsJsonArray("lab")), Collections.unmodifiableMap(parts),
                    List.copyOf(combos), List.copyOf(twins), t.get("moves").getAsInt());
            ts.add(target);
            byId.put(target.id(), target);
        }
        targets = Collections.unmodifiableList(ts);
    }

    public int capSpecks() { return cap; }
    public List<String> resins() { return resins; }
    public List<String> starter() { return starter; }
    public List<Target> targets() { return targets; }
    public Target target(String id) { return byId.get(id); }
    public int emptyRgb() { return emptyRgb; }
    /** A resin's colour neat (2 globs alone) and with white (2 + 4 globs). */
    public int[] swatch(String resin) { return swatch.get(resin); }

    // ------------------------------------------------------------------ the mix

    /** XYZ of a pool (amounts in specks per resin, in {@link #resins()} order; fractions allowed). */
    public double[] xyz(double[] q) {
        double x = 0, y = 0, z = 0;
        for (int b = 0; b < w.length; b++) {
            double kk = baseK[b], ss = baseS[b];
            for (int i = 0; i < q.length; i++) if (q[i] != 0) { double c = q[i] / SPECKS_PER_GLOB; kk += c * k[i][b]; ss += c * s[i][b]; }
            double ks = kk / ss, r = 1 + ks - Math.sqrt(ks * ks + 2 * ks);
            x += r * w[b][0]; y += r * w[b][1]; z += r * w[b][2];
        }
        return new double[]{x, y, z};
    }

    public double[] lab(double[] q) { return labOf(xyz(q)); }

    public double[] labOf(double[] xyz) {
        double fx = f(xyz[0] / white[0]), fy = f(xyz[1] / white[1]), fz = f(xyz[2] / white[2]);
        return new double[]{116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    private static double f(double t) { return t > 216.0 / 24389 ? Math.cbrt(t) : (24389.0 / 27 * t + 16) / 116; }

    /** sRGB (0xRRGGBB) of a pool. */
    public int rgb(double[] q) { return rgbOf(xyz(q)); }

    public static int rgbOf(double[] xyz) {
        double x = xyz[0], y = xyz[1], z = xyz[2];
        double[] lin = {3.2404542 * x - 1.5371385 * y - 0.4985314 * z, -0.9692660 * x + 1.8760108 * y + 0.0415560 * z, 0.0556434 * x - 0.2040259 * y + 1.0572252 * z};
        int out = 0;
        for (double v : lin) {
            v = Math.min(1, Math.max(0, v));
            int c = (int) Math.round(255 * (v <= 0.0031308 ? 12.92 * v : 1.055 * Math.pow(v, 1 / 2.4) - 0.055));
            out = (out << 8) | c;
        }
        return out;
    }

    public static double[] labToXyz(double[] lab, double[] white) {
        double fy = (lab[0] + 16) / 116, fx = fy + lab[1] / 500, fz = fy - lab[2] / 200;
        return new double[]{inv(fx) * white[0], inv(fy) * white[1], inv(fz) * white[2]};
    }

    public double[] whitePoint() { return white.clone(); }

    private static double inv(double t) { return t * t * t > 216.0 / 24389 ? t * t * t : (116 * t - 16) / (24389.0 / 27); }

    /** CIEDE2000. */
    public static double de2000(double[] a, double[] b) {
        double l1 = a[0], a1 = a[1], b1 = a[2], l2 = b[0], a2 = b[1], b2 = b[2], rad = Math.PI / 180;
        double c1 = Math.hypot(a1, b1), c2 = Math.hypot(a2, b2), cb = (c1 + c2) / 2;
        double g = 0.5 * (1 - Math.sqrt(Math.pow(cb, 7) / (Math.pow(cb, 7) + Math.pow(25, 7))));
        double a1p = (1 + g) * a1, a2p = (1 + g) * a2, c1p = Math.hypot(a1p, b1), c2p = Math.hypot(a2p, b2);
        double h1p = hue(a1p, b1), h2p = hue(a2p, b2), dLp = l2 - l1, dCp = c2p - c1p, dhp = 0;
        if (c1p * c2p != 0) { dhp = h2p - h1p; if (dhp > 180) dhp -= 360; else if (dhp < -180) dhp += 360; }
        double dHp = 2 * Math.sqrt(c1p * c2p) * Math.sin(dhp / 2 * rad), lbp = (l1 + l2) / 2, cbp = (c1p + c2p) / 2;
        double hbp = h1p + h2p;
        if (c1p * c2p != 0) { if (Math.abs(h1p - h2p) > 180) hbp += h1p + h2p < 360 ? 360 : -360; hbp /= 2; }
        double t = 1 - 0.17 * Math.cos((hbp - 30) * rad) + 0.24 * Math.cos(2 * hbp * rad) + 0.32 * Math.cos((3 * hbp + 6) * rad) - 0.20 * Math.cos((4 * hbp - 63) * rad);
        double dTh = 30 * Math.exp(-Math.pow((hbp - 275) / 25, 2)), rc = 2 * Math.sqrt(Math.pow(cbp, 7) / (Math.pow(cbp, 7) + Math.pow(25, 7)));
        double sl = 1 + 0.015 * Math.pow(lbp - 50, 2) / Math.sqrt(20 + Math.pow(lbp - 50, 2)), sc = 1 + 0.045 * cbp, sh = 1 + 0.015 * cbp * t;
        double rt = -Math.sin(2 * dTh * rad) * rc;
        return Math.sqrt(Math.pow(dLp / sl, 2) + Math.pow(dCp / sc, 2) + Math.pow(dHp / sh, 2) + rt * (dCp / sc) * (dHp / sh));
    }

    private static double hue(double x, double y) {
        if (x == 0 && y == 0) return 0;
        double t = Math.toDegrees(Math.atan2(y, x));
        return t < 0 ? t + 360 : t;
    }

    public double distance(double[] q, Target t) { return de2000(lab(q), t.lab()); }

    public static double[] specks(int[] q) {
        double[] out = new double[q.length];
        for (int i = 0; i < q.length; i++) out[i] = q[i];
        return out;
    }

    // ------------------------------------------------------------------ green or red

    /**
     * Can this pool still become a reasonable match for the target? Reasonable = one of the target's valid resin sets
     * (1-3 resins that make it on their own), with stray resin outside the set up to {@link #WIGGLE} of the pool,
     * finished by adding only that set's resins within the space left. Resin can't be taken out, only added.
     */
    public boolean canFinish(int[] pool, Target t) {
        double[] q = specks(pool);
        double tot = 0;
        for (double v : q) tot += v;
        double free = cap - tot;
        for (Combo c : t.combos()) {
            double stray = 0;
            for (int i = 0; i < q.length; i++) if (!contains(c.resins(), i)) stray += q[i];
            if (stray > WIGGLE * tot + 1e-9) continue;
            if (finishWith(q, t.lab(), c, free) <= MATCH) return true;
        }
        return false;
    }

    private static boolean contains(int[] a, int v) {
        for (int x : a) if (x == v) return true;
        return false;
    }

    private double finishWith(double[] q, double[] lab0, Combo c, double free) {
        int[] idx = c.resins();
        double[] wts = c.weights();
        double sumW = 0;
        for (double x : wts) sumW += x;
        double sc = 0;
        for (int k2 = 0; k2 < idx.length; k2++) sc = Math.max(sc, q[idx[k2]] / wts[k2]);
        double inSet = 0;
        for (int i : idx) inSet += q[i];
        double best = Double.MAX_VALUE;
        for (double scale : new double[]{sc, sc * 1.3 + 2, sc * 2 + 8, (free + inSet) / sumW}) {
            double[] add = new double[q.length];
            double used = 0;
            for (int k2 = 0; k2 < idx.length; k2++) { add[idx[k2]] = Math.max(0, scale * wts[k2] - q[idx[k2]]); used += add[idx[k2]]; }
            if (used > free && used > 0) { double f = free / used; for (int i : idx) add[i] *= f; }
            double cur = eval(q, add, lab0), step = Math.max(1, free / 2);
            while (step >= 0.25 && cur > 0.6) {
                boolean improved = false;
                for (int a = 0; a < idx.length; a++) for (int d = -1; d <= 1; d += 2) {
                    double[] a2 = add.clone();
                    a2[idx[a]] = Math.max(0, a2[idx[a]] + d * step);
                    if (a2[idx[a]] == add[idx[a]] || sum(a2) > free + 1e-9) continue;
                    double v = eval(q, a2, lab0);
                    if (v < cur - 1e-6) { add = a2; cur = v; improved = true; }
                }
                if (!improved) {   // stuck: try moving two resins at once (valleys run diagonally)
                    for (int a = 0; a < idx.length; a++) for (int b = a + 1; b < idx.length; b++)
                        for (int[] dd : new int[][]{{1, 1}, {1, -1}, {-1, 1}}) {
                            double[] a2 = add.clone();
                            a2[idx[a]] = Math.max(0, a2[idx[a]] + dd[0] * step);
                            a2[idx[b]] = Math.max(0, a2[idx[b]] + dd[1] * step);
                            if (sum(a2) > free + 1e-9) continue;
                            double v = eval(q, a2, lab0);
                            if (v < cur - 1e-6) { add = a2; cur = v; improved = true; }
                        }
                }
                if (!improved) step /= 2;
            }
            best = Math.min(best, cur);
            if (best <= MATCH) break;
        }
        return best;
    }

    private static double sum(double[] a) {
        double t = 0;
        for (double v : a) t += v;
        return t;
    }

    private double eval(double[] q, double[] add, double[] lab0) {
        double[] m = new double[q.length];
        for (int i = 0; i < q.length; i++) m[i] = q[i] + add[i];
        return de2000(lab(m), lab0);
    }
}

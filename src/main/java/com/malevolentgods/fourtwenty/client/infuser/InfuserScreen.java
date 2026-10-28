package com.malevolentgods.fourtwenty.client.infuser;

import com.malevolentgods.fourtwenty.domain.Tint;
import com.malevolentgods.fourtwenty.infuser.Infuser;
import com.malevolentgods.fourtwenty.infuser.InfuserState;
import com.malevolentgods.fourtwenty.infuser.Infusion;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * The Inspiration Infuser: drop beads of resin into a pool of liquid weed magic until it matches a colour you're
 * inspired by. The same game as the design prototype (tools/infuser/prototype_template.html): the colour you chase
 * rings the pool and touches its edge exactly, breathing in at low inspiration and solid at full, when its edge also
 * glows green while the pool can still become a proper recipe and red when it can't. The map shows where your mind
 * went (washed in green; inspired colours show through truer with each hit) and is where you pick what to chase.
 * A catch seals the pool until it's drained. Nothing is timed and nothing can fail; mixing is free.
 */
public class InfuserScreen extends Screen {
    private static final String[] LABELS = {"Moonmilk", "Void Tar", "Sour Sunbeam", "Honey Oil", "Creamsicle", "Dragon Breath", "Berry Bliss",
            "Grape Goo", "Ocean Couch", "Bottomless Blue", "Emerald Ooze", "Mud Butter"};
    private static final String[] SIZE_NAMES = {"Glob", "Drop", "Drip", "Speck"};
    private static final int[] SIZE_SPECKS = {128, 4, 2, 1};
    private static final String KANJI = "〇一二三四五六七八九";
    private static final int BG = 0xF0141019, PANEL = 0xFF1D1830, LINE = 0xFF4A4170, FG = 0xFFEFE9F7, MUTED = 0xFFC2B9DD,
            GLOW = 0xFFB8F06A, AMBER = 0xFFFFC76B, GOOD = 0x6BE38A, BAD = 0xFF6F7D;
    private static final double PERIOD = 4800;

    private final Tint tint = Tint.get();
    private final int n = tint.resins().size();
    private int[] pool;
    private final Deque<int[]> history = new ArrayDeque<>();
    private int size = 1;
    private double[] shown = rgbParts(tint.emptyRgb());
    private long pulseStart = System.currentTimeMillis(), matchAt = -1, edgeUntil = 0, swellAt = -1;
    private boolean edgeGood;
    private final List<double[]> ripples = new ArrayList<>();   // {start ms, x offset, rgb, size}
    private final List<double[]> beads = new ArrayList<>();
    private String message = "";
    /** Which tab: mixing (the game) or infusing (seed + terpene into a strain seed). */
    private boolean infuseTab;

    // layout
    private int x0, y0, x1, y1, colL, colC, colR;
    private double pcx, pcy, pr, ring;
    private int mapX, mapY, mapW, mapH;
    private final List<int[]> hits = new ArrayList<>();    // clickable areas: {x0, y0, x1, y1, kind, index}

    // the colour map
    private static final int MW = 360, MH = 150, GREY = 30;
    private NativeImage trueMap, greenMap;
    private DynamicTexture mapTexture;
    private ResourceLocation mapId;
    private String mapKey = "";

    public InfuserScreen() {
        super(Component.translatable("block.fourtwenty.inspiration_infuser"));
        pool = state().pool.clone();
        if (pool.length != n) pool = new int[n];
        if (state().sealed != null) matchAt = System.currentTimeMillis() - 5000;
        shown = rgbParts(poolRgb());
    }

    private static InfuserState state() { return InfuserClient.state; }

    void synced() { /* the server's word on inspiration and catches; the local pool stays as the player left it */ }

    @Override public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------ state helpers

    private int total() { return Arrays.stream(pool).sum(); }

    private Tint.Target target() { return state().chasing == null ? null : tint.target(state().chasing); }

    private int level() { return state().chasing == null ? 0 : state().level(state().chasing); }

    private boolean sealed() { return state().sealed != null; }

    private boolean unlocked(int i) {
        String r = tint.resins().get(i);
        Tint.Target t = target();
        return tint.starter().contains(r) || (t != null && r.equals(t.extra()));
    }

    private int poolRgb() {
        if (sealed()) return tint.target(state().sealed).rgb();
        return total() == 0 ? tint.emptyRgb() : tint.rgb(Tint.specks(pool));
    }

    private double distance() {
        Tint.Target t = sealed() ? tint.target(state().sealed) : target();
        return t == null || total() == 0 ? -1 : tint.distance(Tint.specks(pool), t);
    }

    private static double[] rgbParts(int rgb) { return new double[]{(rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255}; }

    private String shownName(Tint.Target t) {
        boolean known = state().caught.containsKey(t.id()) || t.id().equals(InfuserState.FIRST);
        return known ? t.name() : "an unnamed vision";
    }

    private void save() {
        var s = state();
        s.pool = pool.clone();
        PacketDistributor.sendToServer(new Infuser.Pool(Arrays.stream(pool).boxed().toList(), s.chasing == null ? "" : s.chasing, s.sealed == null ? "" : s.sealed));
    }

    private void play(SoundEvent sound, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, 0.5f));
    }

    private void push() {
        history.push(pool.clone());
        while (history.size() > 3) history.removeLast();
    }

    // ------------------------------------------------------------------ actions

    private void drop(int i) {
        if (sealed()) { message = "This pool is finished. Drain it to mix again."; return; }
        if (!unlocked(i)) return;
        int add = Math.min(SIZE_SPECKS[size], tint.capSpecks() - total());
        if (add <= 0) { message = "The pool is full: 12 globs of resin."; return; }
        push();
        pool[i] += add;
        beads.add(new double[]{System.currentTimeMillis(), (Math.random() - 0.5) * 0.5, tint.swatch(tint.resins().get(i))[0], 3 + Math.log(add) / Math.log(2) * 1.6});
        play(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 1.6f - (float) Math.log(add) * 0.08f);
        afterChange();
    }

    private void swell() {
        if (sealed() || total() == 0 || total() * 2 > tint.capSpecks()) return;
        push();
        for (int i = 0; i < n; i++) pool[i] *= 2;
        swellAt = System.currentTimeMillis();
        play(SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 0.8f);
        afterChange();
    }

    private void takeBack() {
        if (sealed() || history.isEmpty()) return;
        pool = history.pop();
        message = "";
        save();
    }

    private void drain() {
        state().sealed = null;
        history.clear();
        pool = new int[n];
        matchAt = -1;
        message = "";
        play(SoundEvents.BUCKET_EMPTY, 1.2f);
        save();
    }

    private void afterChange() {
        message = "";
        Tint.Target t = target();
        if (t != null && level() >= InfuserState.MAX) {
            edgeGood = tint.canFinish(pool, t);
            edgeUntil = System.currentTimeMillis() + 900;
        }
        if (t != null && level() > 0 && total() > 0) {
            double d = tint.distance(Tint.specks(pool), t);
            if (d <= Tint.MATCH) {
                // caught: seal locally now; the server checks the same pool and records it
                state().sealed = t.id();
                state().caught.merge(t.id(), d <= Tint.SPOT_ON ? 2 : 1, Math::max);
                t.twins().forEach(id -> state().caught.merge(id, d <= Tint.SPOT_ON ? 2 : 1, Math::max));
                matchAt = System.currentTimeMillis();
                history.clear();
                play(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f);
                play(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5f);
                PacketDistributor.sendToServer(new Infuser.Catch(t.id(), Arrays.stream(pool).boxed().toList()));
            }
        }
        save();
    }

    private void choose(String id) {
        if (state().level(id) <= 0) return;
        state().chasing = id;
        pulseStart = System.currentTimeMillis();
        edgeUntil = 0;
        play(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4f);
        save();
    }

    @Override public void onClose() {
        save();
        super.onClose();
    }

    @Override public void removed() {
        if (mapTexture != null) mapTexture.close();
        if (trueMap != null) trueMap.close();
        if (greenMap != null) greenMap.close();
        super.removed();
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        for (int[] h : hits) {
            if (mx < h[0] || mx >= h[2] || my < h[1] || my >= h[3]) continue;
            switch (h[4]) {
                case 0 -> drop(h[5]);
                case 1 -> { size = h[5]; play(SoundEvents.UI_BUTTON_CLICK.value(), 1.6f); }
                case 2 -> swell();
                case 3 -> takeBack();
                case 4 -> drain();
                case 5 -> pickOnMap(mx, my);
                case 6 -> { infuseTab = h[5] == 1; play(SoundEvents.UI_BUTTON_CLICK.value(), 1.4f); }
                case 7 -> {
                    PacketDistributor.sendToServer(new Infusion.Infuse(h[5]));
                    play(SoundEvents.BREWING_STAND_BREW, 1.3f);
                }
                default -> {}
            }
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void pickOnMap(double mx, double my) {
        double best = Double.MAX_VALUE;
        String pick = null;
        for (Map.Entry<String, Integer> e : state().inspiration.entrySet()) {
            Tint.Target t = tint.target(e.getKey());
            if (t == null || e.getValue() <= 0) continue;
            double[] p = mapPos(t.lab());
            double x = mapX + p[0] * mapW / MW, y = mapY + p[1] * mapH / MH;
            double d = Math.hypot(mx - x, my - y), r = glowR(e.getValue()) * mapW / MW + 4;
            if (d <= r && d < best) { best = d; pick = e.getKey(); }
        }
        if (pick != null) choose(pick);
    }

    // ------------------------------------------------------------------ layout

    private void layout() {
        int w = Math.min(width - 16, 640), h = Math.min(height - 30, 340);
        x0 = (width - w) / 2; y0 = (height - h) / 2 + 9; x1 = x0 + w; y1 = y0 + h;      // room above for the tabs
        colL = (int) (w * 0.29); colR = (int) (w * 0.33); colC = w - colL - colR;
        double cx = x0 + colL + colC / 2.0;
        pr = Math.min(colC * 0.30, (h - 70) * 0.30);
        ring = pr * 0.28;
        pcx = cx; pcy = y0 + 14 + pr + ring + 6;
        mapX = x1 - colR + 6; mapY = y0 + 18; mapW = colR - 12; mapH = (int) Math.round(mapW * (double) MH / MW);
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        layout();
        hits.clear();
        long now = System.currentTimeMillis();
        g.fill(0, 0, width, height, 0x99000000);
        Draw.round(g, x0, y0, x1, y1, 10, 0xF01A1528);
        Draw.round(g, x0 + 1, y0 + 1, x1 - 1, y1 - 1, 9, BG);
        int tw = 64, tx = (x0 + x1) / 2 - tw - 2;
        button(g, tx, y0 - 17, tw, 14, "Mix", !infuseTab, true, mouseX, mouseY, 6, 0);
        button(g, tx + tw + 4, y0 - 17, tw, 14, "Infuse", infuseTab, true, mouseX, mouseY, 6, 1);
        if (infuseTab) {
            drawInfuse(g, mouseX, mouseY);
            return;
        }
        drawLeft(g, mouseX, mouseY);
        drawPool(g, now);
        drawCentreText(g, mouseX, mouseY);
        drawRight(g, now, mouseX, mouseY);
    }

    /**
     * The Infuse tab: every strain and what breeds it, a Landrace seed and its colour's terpene(s); Infuse when you
     * carry them. The server checks again and swaps them for the strain's seed.
     */
    private void drawInfuse(GuiGraphics g, int mx, int my) {
        var player = Minecraft.getInstance().player;
        int x = x0 + 12, y = y0 + 12, w = x1 - x0 - 24;
        label(g, "Infuse a Landrace seed with a colour's terpene to breed a strain", x, y);
        List<Infusion.Recipe> recipes = Infusion.RECIPES;
        int cols = 3, rows = (recipes.size() + cols - 1) / cols, gap = 5;
        int cw = (w - gap * (cols - 1)) / cols, ch = Math.max(26, Math.min(40, (y1 - y - 26 - gap * (rows - 1)) / rows));
        ItemStack hover = ItemStack.EMPTY;
        for (int i = 0; i < recipes.size(); i++) {
            Infusion.Recipe r = recipes.get(i);
            int cx = x + (i % cols) * (cw + gap), cy = y + 16 + (i / cols) * (ch + gap);
            int have = player == null ? 0 : Infusion.available(player, r);
            Draw.round(g, cx, cy, cx + cw, cy + ch, 6, have > 0 ? LINE : 0xFF2A2440);
            Draw.round(g, cx + 1, cy + 1, cx + cw - 1, cy + ch - 1, 5, 0xFF161220);
            double dr = Math.min(11, (ch - 8) / 2.0), dx = cx + 5 + dr, dy = cy + ch / 2.0;
            Draw.glow(g, dx, dy, dr * 0.6, dr + 3, r.strain().color(), 0.35);
            Draw.disc(g, dx, dy, dr, Draw.argb(r.strain().color(), 1));
            ItemStack seed = ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get()), r.strain());
            g.renderItem(seed, (int) dx - 8, (int) dy - 8);
            if (Math.hypot(mx - dx, my - dy) <= dr) hover = seed;
            int tx = (int) (dx + dr + 6), bw = 44, room = cx + cw - bw - 8 - tx;
            small(g, Component.translatable(r.strain().translationKey()).getString(), tx, cy + 5, room, FG);
            // the ingredients, drawn at 3/4 size: a Landrace seed + each terpene
            List<ItemStack> parts = new ArrayList<>();
            parts.add(new ItemStack(ModItems.WEED_SEEDS.get()));
            r.items().forEach(it -> parts.add(new ItemStack(it)));
            int ix = tx, iy = cy + ch - 15;
            for (int k = 0; k < parts.size(); k++) {
                if (k > 0) { small(g, "+", ix, iy + 3, 8, MUTED); ix += 7; }
                g.pose().pushPose();
                g.pose().translate(ix, iy, 0);
                g.pose().scale(0.75f, 0.75f, 1);
                g.renderItem(parts.get(k), 0, 0);
                g.pose().popPose();
                if (mx >= ix && mx < ix + 12 && my >= iy && my < iy + 12) hover = parts.get(k);
                ix += 14;
            }
            button(g, cx + cw - bw - 5, cy + (ch - 14) / 2, bw, 14, have > 0 ? "Infuse" : "Need", false, have > 0, mx, my, 7, i);
        }
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) { }

    /** Text at 85% size, trimmed with an ellipsis to fit {@code room} pixels. */
    private void small(GuiGraphics g, String s, int x, int y, int room, int colour) {
        float k = 0.85f;
        int max = (int) (room / k);
        if (font.width(s) > max) s = font.plainSubstrByWidth(s, max - font.width("…")) + "…";
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(k, k, 1);
        g.drawString(font, s, 0, 0, colour, false);
        g.pose().popPose();
    }

    private void label(GuiGraphics g, String s, int x, int y) {
        g.drawString(font, s.toUpperCase(), x, y, MUTED, false);
    }

    private void button(GuiGraphics g, int x, int y, int w, int h, String s, boolean on, boolean enabled, int mx, int my, int kind, int index) {
        boolean hover = enabled && mx >= x && mx < x + w && my >= y && my < y + h;
        int fill = on ? AMBER : hover ? 0xFF2A2342 : 0xFF15111F;
        Draw.round(g, x, y, x + w, y + h, h / 2.0, enabled ? (on ? fill : LINE) : 0xFF231E33);
        Draw.round(g, x + 1, y + 1, x + w - 1, y + h - 1, h / 2.0 - 1, enabled ? fill : 0xFF15111F);
        g.drawCenteredString(font, s, x + w / 2, y + (h - 8) / 2, !enabled ? 0xFF5A5470 : on ? 0xFF2A1A04 : FG);
        if (enabled) hits.add(new int[]{x, y, x + w, y + h, kind, index});
    }

    private void drawLeft(GuiGraphics g, int mx, int my) {
        int x = x0 + 12, y = y0 + 12, w = colL - 18;
        label(g, "Bead size", x, y);
        int bw = (w - 9) / 4;
        for (int i = 0; i < 4; i++) button(g, x + i * (bw + 3), y + 11, bw, 14, SIZE_NAMES[i], size == i, true, mx, my, 1, i);
        label(g, "Resins · click to drop", x, y + 32);
        int cw = (w - 4) / 2, chh = Math.max(20, Math.min(26, (y1 - (y + 44) - 8) / 6));
        for (int i = 0; i < n; i++) {
            int jx = x + (i % 2) * (cw + 4), jy = y + 44 + (i / 2) * (chh + 3);
            boolean en = unlocked(i) && !sealed();
            boolean hover = en && mx >= jx && mx < jx + cw && my >= jy && my < jy + chh;
            Draw.round(g, jx, jy, jx + cw, jy + chh, 6, hover ? AMBER : LINE);
            Draw.round(g, jx + 1, jy + 1, jx + cw - 1, jy + chh - 1, 5, en ? 0xFF161220 : 0xFF120F1A);
            int[] sw = tint.swatch(tint.resins().get(i));
            double br = (chh - 8) / 2.0;
            double bx = jx + 4 + br, by = jy + chh / 2.0;
            Draw.disc(g, bx, by, br, Draw.argb(sw[0], en ? 1 : 0.35));
            Draw.disc(g, bx - br * 0.25, by - br * 0.25, br * 0.6, Draw.argb(sw[1], en ? 0.55 : 0.2));
            Draw.disc(g, bx - br * 0.35, by - br * 0.4, br * 0.2, Draw.argb(0xFFFFFF, en ? 0.5 : 0.15));
            // names in a slightly smaller type, cut to the jar so they never run into the next column
            int tx = (int) (bx + br + 4), room = jx + cw - 4 - tx;
            String family = tint.starter().contains(tint.resins().get(i)) ? "Landrace" : familyOf(tint.resins().get(i));
            small(g, LABELS[i], tx, (int) (by - 8), room, en ? FG : 0xFF6E6788);
            small(g, family, tx, (int) (by + 1), room, en ? 0xFFB9B0D4 : 0xFF544D6B);
            if (en) hits.add(new int[]{jx, jy, jx + cw, jy + chh, 0, i});
        }
    }

    private static String familyOf(String resin) {
        return switch (resin) {
            case "gold" -> "Warm family";
            case "oxide_brown" -> "Neutrals family";
            case "phthalo_green" -> "Green family";
            case "violet" -> "Cool family";
            default -> "";
        };
    }

    private double breath(long now, int l) {
        if (l <= 0) return 0;
        if (l >= InfuserState.MAX) return 1;
        double duty = l / 5.0, ph = ((now - pulseStart) % (long) PERIOD) / PERIOD, fade = 0.14;
        if (ph > duty + fade) return 0;
        double v = Math.min(1, Math.min(ph / fade, (duty + fade - ph) / fade));
        return v * v * (3 - 2 * v);
    }

    private void drawPool(GuiGraphics g, long now) {
        int aim = poolRgb();
        double[] a = rgbParts(aim);
        for (int k = 0; k < 3; k++) shown[k] += (a[k] - shown[k]) * 0.12;
        if (sealed()) shown = a;
        int shownRgb = ((int) Math.round(shown[0]) << 16) | ((int) Math.round(shown[1]) << 8) | (int) Math.round(shown[2]);
        // weed-magic haze
        Draw.glow(g, pcx, pcy, pr, pr + ring * 3.2, 0xB8F06A, 0.10);
        // the inspired colour: a disc under the pool, so the two meet at one exact edge
        Tint.Target t = sealed() ? tint.target(state().sealed) : target();
        double alpha = sealed() ? 1 : t == null ? 0 : breath(now, level());
        if (alpha > 0.01) Draw.disc(g, pcx, pcy, pr + ring, Draw.argb(t.rgb(), alpha));
        if (!sealed() && t != null && level() >= InfuserState.MAX && now < edgeUntil) {
            double ea = Math.min(1, (edgeUntil - now) / 400.0);
            int c = edgeGood ? GOOD : BAD;
            Draw.glow(g, pcx, pcy, pr + ring + 1, pr + ring + ring * 0.9, c, 0.55 * ea);
            Draw.ring(g, pcx, pcy, pr + ring + 0.5, pr + ring + 2.5, Draw.argb(c, ea));
        }
        // the pool: one flat colour, nothing drawn over its edge
        Draw.disc(g, pcx, pcy, pr, Draw.argb(shownRgb, 1));
        // ripples and beads
        ripples.removeIf(rp -> now - rp[0] > 2400);
        for (double[] rp : ripples) {
            double k = (now - rp[0]) / 2400.0, px = pcx + rp[1] * pr;
            double r0 = pr * 0.1 + pr * 0.6 * k;
            if (Math.hypot(px - pcx, 0) + r0 < pr - 2)
                Draw.arc(g, px, pcy, r0, r0 + rp[3] * 0.9 * (1 - k * 0.7), k * 7, k * 7 + 1.4 + rp[3] * 0.12, tt -> Draw.argb((int) rp[2], (1 - k) * 0.8));
            double r1 = pr * 0.05 + pr * 0.85 * k;
            if (Math.abs(px - pcx) + r1 < pr - 1) Draw.ring(g, px, pcy, r1, r1 + 0.8, Draw.argb(0xFFFFFF, (1 - k) * 0.3));
        }
        beads.removeIf(b -> {
            double k = (now - b[0]) / 320.0;
            if (k >= 1) { ripples.add(new double[]{now, b[1], b[2], b[3]}); return true; }
            return false;
        });
        for (double[] b : beads) {
            double k = (now - b[0]) / 320.0, px = pcx + b[1] * pr, top = pcy - pr - ring - 10;
            Draw.disc(g, px, top + (pcy - top) * k * k, b[3] * 0.7, Draw.argb((int) b[2], 1));
        }
        double sk = (now - swellAt) / 700.0;
        if (swellAt > 0 && sk < 1) Draw.ring(g, pcx, pcy, pr * (0.2 + 0.75 * sk), pr * (0.2 + 0.75 * sk) + 1.2, Draw.argb(0xFFFFFF, (1 - sk) * 0.5));
        // caught: a swirling rainbow round the pool and its ring
        if (sealed()) {
            double k = Math.min(1, (now - matchAt) / 2200.0), flourish = 1 - k;
            double r0 = pr + ring + 0.6, band = ring * (0.45 + 0.4 * flourish), spin = now / 900.0;
            Draw.arc(g, pcx, pcy, r0, r0 + band, spin, spin + 2 * Math.PI, tt -> Draw.argb(Draw.hsv(tt, 0.62, 1), 1));
            Draw.glow(g, pcx, pcy, r0 + band, r0 + band + ring * (0.6 + flourish), 0xFFFFFF, 0.25 + 0.3 * flourish);
            for (int s = 0; s < 18; s++) {
                double ang = spin * (s % 2 == 1 ? 1.6 : -1.1) + s / 18.0 * 2 * Math.PI;
                double rr = r0 + band / 2 + Math.sin(now / 300.0 + s) * band * 0.7 + flourish * ring * 1.5 * ((s * 37) % 10) / 10.0;
                double sz = (0.8 + 0.6 * ((s * 13) % 3)) * (0.6 + 0.4 * Math.sin(now / 200.0 + s * 2));
                Draw.disc(g, pcx + Math.cos(ang) * rr, pcy + Math.sin(ang) * rr, Math.max(0.4, sz), Draw.argb(Draw.hsv(s / 18.0 + now / 4000.0, 0.4, 1), 1));
            }
        }
    }

    private static String kanji(double d) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", Math.min(d, 99.99));
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) out.append(c == '.' ? '・' : KANJI.charAt(c - '0'));
        return out.toString();
    }

    private void drawCentreText(GuiGraphics g, int mx, int my) {
        int cx = (int) pcx;
        int y = (int) (pcy + pr + ring + 10);
        g.drawCenteredString(font, "Δ", cx, y, MUTED);
        double d = distance();
        g.pose().pushPose();
        g.pose().translate(cx, y + 11, 0);
        g.pose().scale(2, 2, 1);
        g.drawCenteredString(font, d < 0 ? "—" : kanji(d), 0, 0, FG);
        g.pose().popPose();
        String status = message;
        int statusColour = MUTED;
        if (sealed()) {
            Tint.Target t = tint.target(state().sealed);
            int grade = state().caught.getOrDefault(t.id(), 1);
            List<String> twins = t.twins().stream().map(id -> tint.target(id).name()).toList();
            status = (grade == 2 ? "Spot On ★  " : "") + "You caught " + t.name() + (twins.isEmpty() ? "" : " and its twin" + (twins.size() > 1 ? "s " : " ") + String.join(" and ", twins)) + "!";
            statusColour = 0xFF6BE38A;
        } else if (target() == null) status = "Smoke to get inspired, then click a glow on the map.";
        if (!status.isEmpty()) {
            int sw = colC - 16;
            var lines = font.split(Component.literal(status), sw);
            for (int i = 0; i < lines.size(); i++) g.drawCenteredString(font, lines.get(i), cx, y + 32 + i * 10, statusColour);
        }
        int by = y1 - 22, bw = Math.min(64, (colC - 24) / 3);
        int bx = cx - (bw * 3 + 8) / 2;
        button(g, bx, by, bw, 14, "Swell ×2", false, !sealed() && total() > 0 && total() * 2 <= tint.capSpecks(), mx, my, 2, 0);
        button(g, bx + bw + 4, by, bw, 14, "Take back (" + history.size() + ")", false, !sealed() && !history.isEmpty(), mx, my, 3, 0);
        button(g, bx + 2 * (bw + 4), by, bw, 14, "Drain", false, true, mx, my, 4, 0);
    }

    // ------------------------------------------------------------------ the right column: map, chase, recipe, collection

    private static double glowR(int level) { return 13 + level * 4.5; }

    /** Map coordinates (0..MW, 0..MH) of a colour: hue across, lightness down, greys at the right. */
    private double[] mapPos(double[] lab) {
        double c = Math.hypot(lab[1], lab[2]), y = (96 - Math.max(8, Math.min(96, lab[0]))) / 88 * (MH - 1);
        if (c < 10) return new double[]{MW - GREY / 2.0, y};
        double hd = Math.toDegrees(Math.atan2(lab[2], lab[1]));
        if (hd < 0) hd += 360;
        return new double[]{hd / 360 * (MW - GREY), y};
    }

    private static int toAbgr(int rgb) { return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF); }

    private void buildMaps() {
        double[] white = tint.whitePoint();
        trueMap = new NativeImage(MW, MH, false);
        greenMap = new NativeImage(MW, MH, false);
        for (int y = 0; y < MH; y++) {
            double l = 96 - y / (double) (MH - 1) * 88;
            int gr = Tint.rgbOf(Tint.labToXyz(new double[]{l * 0.9 + 4, -26, 22}, white));
            for (int x = 0; x < MW; x++) {
                int rgb;
                if (x >= MW - GREY) rgb = Tint.rgbOf(Tint.labToXyz(new double[]{l, 0, 0}, white));
                else {
                    double hr = x / (double) (MW - GREY) * 2 * Math.PI;
                    rgb = 0;
                    for (int c = 90; c > 0; c -= 3) {
                        double[] xyz = Tint.labToXyz(new double[]{l, c * Math.cos(hr), c * Math.sin(hr)}, white);
                        if (inGamut(xyz)) { rgb = Tint.rgbOf(xyz); break; }
                    }
                }
                trueMap.setPixelRGBA(x, y, toAbgr(rgb));
                int mix = 0;
                for (int k = 16; k >= 0; k -= 8) mix |= ((int) Math.round(((gr >> k) & 255) * 0.86 + ((rgb >> k) & 255) * 0.14)) << k;
                greenMap.setPixelRGBA(x, y, toAbgr(mix));
            }
        }
    }

    private static boolean inGamut(double[] xyz) {
        double x = xyz[0], y = xyz[1], z = xyz[2];
        double[] lin = {3.2404542 * x - 1.5371385 * y - 0.4985314 * z, -0.9692660 * x + 1.8760108 * y + 0.0415560 * z, 0.0556434 * x - 0.2040259 * y + 1.0572252 * z};
        for (double v : lin) if (v < -0.001 || v > 1.001) return false;
        return true;
    }

    /** Re-reveal the map when inspiration changes: each inspired colour shows through the green, truer with each level. */
    private void refreshMap() {
        String key = state().inspiration.toString();
        if (mapTexture != null && key.equals(mapKey)) return;
        mapKey = key;
        if (trueMap == null) buildMaps();
        if (mapTexture == null) {
            mapTexture = new DynamicTexture(MW, MH, false);
            mapId = Minecraft.getInstance().getTextureManager().register("fourtwenty_infuser_map", mapTexture);
        }
        NativeImage img = mapTexture.getPixels();
        double[] reveal = new double[MW * MH];
        for (var e : state().inspiration.entrySet()) {
            Tint.Target t = tint.target(e.getKey());
            if (t == null || e.getValue() <= 0) continue;
            double[] p = mapPos(t.lab());
            double r = glowR(e.getValue()), strength = e.getValue() / 5.0;
            for (int y = (int) Math.max(0, p[1] - r); y < Math.min(MH, p[1] + r); y++)
                for (int x = (int) Math.max(0, p[0] - r); x < Math.min(MW, p[0] + r); x++) {
                    double d = Math.hypot(x - p[0], y - p[1]) / r;
                    if (d >= 1) continue;
                    double v = strength * (d < 0.55 ? 1 - d * 0.55 : 0.7 * (1 - d) / 0.45);
                    int k = y * MW + x;
                    reveal[k] = Math.max(reveal[k], v);
                }
        }
        for (int y = 0; y < MH; y++) for (int x = 0; x < MW; x++) {
            int a = trueMap.getPixelRGBA(x, y), b = greenMap.getPixelRGBA(x, y);
            double v = reveal[y * MW + x];
            int out = 0xFF000000;
            for (int k = 0; k <= 16; k += 8) out |= ((int) Math.round(((b >> k) & 255) * (1 - v) + ((a >> k) & 255) * v)) << k;
            img.setPixelRGBA(x, y, out);
        }
        mapTexture.upload();
    }

    private void drawRight(GuiGraphics g, long now, int mx, int my) {
        int x = x1 - colR + 6, w = colR - 12;
        label(g, "Your mind's map · click a glow", x, y0 + 8);
        refreshMap();
        Draw.round(g, mapX - 1, mapY - 1, mapX + mapW + 1, mapY + mapH + 1, 4, LINE);
        g.blit(mapId, mapX, mapY, mapW, mapH, 0, 0, MW, MH, MW, MH);
        g.fill(mapX + mapW - GREY * mapW / MW - 1, mapY, mapX + mapW - GREY * mapW / MW, mapY + mapH, 0xE015111F);
        hits.add(new int[]{mapX, mapY, mapX + mapW, mapY + mapH, 5, 0});
        double sx = (double) mapW / MW, sy = (double) mapH / MH;
        // fully inspired colours: their exact spot, a swatch with crosshair ticks
        for (var e : state().inspiration.entrySet()) {
            Tint.Target t = tint.target(e.getKey());
            if (t == null || e.getValue() < InfuserState.MAX) continue;
            double[] p = mapPos(t.lab());
            double px = mapX + p[0] * sx, py = mapY + p[1] * sy;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
                g.fill((int) (px + d[0] * 5 - (d[0] == 0 ? 0.5 : 0)), (int) (py + d[1] * 5), (int) (px + d[0] * 8 + (d[0] == 0 ? 1 : 0)), (int) (py + d[1] * 8 + (d[1] == 0 ? 1 : 0)), 0xEEFFFFFF);
            Draw.disc(g, px, py, 4.5, 0xFF15111F);
            Draw.disc(g, px, py, 3.6, 0xFFFFFFFF);
            Draw.disc(g, px, py, 2.6, Draw.argb(t.rgb(), 1));
        }
        for (String id : state().caught.keySet()) {
            Tint.Target t = tint.target(id);
            if (t == null || state().level(id) <= 0 || state().level(id) >= InfuserState.MAX) continue;
            double[] p = mapPos(t.lab());
            Draw.disc(g, mapX + p[0] * sx, mapY + p[1] * sy, 1.6, 0xFFFFFFFF);
        }
        Tint.Target t = target();
        if (t != null) {
            double[] p = mapPos(t.lab());
            double r = glowR(level()) * sx, spin = now / 900.0;
            for (int k = 0; k < 12; k++) {
                double a0 = spin + k * Math.PI / 6;
                Draw.arc(g, mapX + p[0] * sx, mapY + p[1] * sy, r - 0.6, r + 0.6, a0, a0 + Math.PI / 10, tt -> 0xE6FFFFFF);
            }
        }
        if (total() > 0) {
            double[] p = mapPos(tint.lab(Tint.specks(pool)));
            double px = mapX + p[0] * sx, py = mapY + p[1] * sy;
            Draw.ring(g, px, py, 5, 7, 0xFF15111F);
            Draw.ring(g, px, py, 3.5, 5, 0xFFFFFFFF);
        }
        // what you're chasing, and how inspired you are
        int y = mapY + mapH + 6;
        g.drawString(font, t == null ? "Nothing yet. Take a hit." : "Chasing " + shownName(t), x, y, FG, false);
        for (int i = 0; i < 5; i++) {
            int lx = x + w - 5 * 9 + i * 9;
            Draw.round(g, lx, y + 1, lx + 7, y + 5, 2, i < level() ? GLOW : LINE);
        }
        // the recipe card
        y += 14;
        label(g, "Recipe", x, y);
        // four count columns, evenly spaced at the right; headers at a smaller size so they fit their column
        int colW = Math.min(30, (w - 70) / 4);
        int[] cols = new int[4];
        for (int c = 0; c < 4; c++) cols[c] = x + w - (4 - c) * colW + colW - 12;
        for (int c = 0; c < 4; c++) {
            String h = SIZE_NAMES[c];
            g.pose().pushPose();
            g.pose().translate(cols[c] + 12 - font.width(h) * 0.75f, y + 1, 0);
            g.pose().scale(0.75f, 0.75f, 1);
            g.drawString(font, h, 0, 0, MUTED, false);
            g.pose().popPose();
        }
        y += 11;
        int rows = 0;
        for (int i = 0; i < n; i++) {
            if (pool[i] == 0) continue;
            int[] parts = {pool[i] / 128, pool[i] % 128 / 4, pool[i] % 4 / 2, pool[i] % 2};
            small(g, LABELS[i], x, y, cols[0] - x - 4, FG);
            for (int c = 0; c < 4; c++) {
                String v = parts[c] == 0 ? "·" : Integer.toString(parts[c]);
                g.drawString(font, v, cols[c] - font.width(v) + 12, y, FG, false);
            }
            y += 10;
            rows++;
        }
        if (rows == 0) { g.drawString(font, "The pool is clear.", x, y, MUTED, false); y += 10; }
        double globs = total() / 128.0;
        String fill = String.format(java.util.Locale.ROOT, "%s of 12 globs", globs == Math.floor(globs) ? Integer.toString((int) globs) : String.format(java.util.Locale.ROOT, "%.2f", globs));
        g.drawString(font, "Pool", x, y + 2, MUTED, false);
        g.drawString(font, fill, x + w - font.width(fill), y + 2, MUTED, false);
        Draw.round(g, x, y + 12, x + w, y + 16, 2, LINE);
        if (total() > 0) Draw.round(g, x, y + 12, x + w * total() / (double) tint.capSpecks(), y + 16, 2, GLOW);
        // the collection: every colour, caught ones filled in
        y += 24;
        label(g, "Discovered · " + state().caught.size() + "/80", x, y);
        y += 11;
        int per = 16;
        double cell = (w - 2.0) / per;
        List<Tint.Target> ts = tint.targets();
        for (int i = 0; i < ts.size(); i++) {
            double cx = x + (i % per) * cell + cell / 2, cy = y + (i / per) * cell + cell / 2;
            if (cy + cell / 2 > y1 - 4) break;
            Tint.Target c = ts.get(i);
            Integer got = state().caught.get(c.id());
            if (got != null) Draw.disc(g, cx, cy, cell * 0.42, Draw.argb(c.rgb(), 1));
            else Draw.ring(g, cx, cy, cell * 0.36, cell * 0.42, state().level(c.id()) > 0 ? 0xFF6A5E8F : LINE);
            if (got != null && got == 2) Draw.disc(g, cx + cell * 0.3, cy - cell * 0.3, cell * 0.12, 0xFFFFFFFF);
        }
    }
}

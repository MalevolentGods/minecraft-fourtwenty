package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.PaveRules;
import com.malevolentgods.fourtwenty.domain.PaveRules.Shape;
import com.malevolentgods.fourtwenty.paving.PaveNetwork;
import com.malevolentgods.fourtwenty.paving.PavePenItem;
import com.malevolentgods.fourtwenty.paving.Paving;
import com.malevolentgods.fourtwenty.registry.ModDataComponents.PenState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;

/**
 * The Pave Pen's settings: its charge, the road it lays (with what each road takes and how much of that you carry), and on a Pro the
 * shape. Everything is drawn here, in the Tech screen's colours; clicking a card tells the server.
 */
public class PavePenScreen extends Screen {
    private static final int W = 248, H = 190, PAD = 10, CARD_GAP = 6, LAY_H = 46, SHAPE_H = 30;
    private static final int PANEL = 0xF023262B, CARD = 0xFF1B1E22, CARD_HOVER = 0xFF262A2F, LINE = 0xFF3A3D42, FRAME = 0xFF14161A;
    private static final int TEXT = 0xFFE3E6DF, DIM = 0xFF8A8F87, WARN = 0xFFE8B04A, GOOD = 0xFF7BFF5A, GREEN = 0xFF60C448, EMERALD = 0xFF2FD07A;

    private final InteractionHand hand;
    private int left, top;
    /** What was last clicked, shown until the server's copy of the pen catches up. */
    private int pendingTarget = -1, pendingShape = -1;

    public PavePenScreen(InteractionHand hand) {
        super(Component.translatable("gui.fourtwenty.pave_pen.title"));
        this.hand = hand;
    }

    private ItemStack pen() { return minecraft == null || minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(hand); }

    @Override protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override public void tick() {
        if (!(pen().getItem() instanceof PavePenItem)) onClose();
    }

    private int accent(boolean pro) { return pro ? EMERALD : GREEN; }

    private int target(PenState s) { return pendingTarget > 0 ? pendingTarget : s.target(); }

    private int shape(PenState s, boolean pro) { return !pro ? 0 : pendingShape >= 0 ? pendingShape : s.shape(); }

    private int cardW() { return (W - PAD * 2 - CARD_GAP * 2) / 3; }

    private int cardX(int i) { return left + PAD + i * (cardW() + CARD_GAP); }

    private int layY() { return top + 86; }

    private int shapeY() { return top + 148; }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        ItemStack pen = pen();
        if (!(pen.getItem() instanceof PavePenItem item)) return;
        PenState s = PavePenItem.state(pen);
        if (pendingTarget == s.target()) pendingTarget = -1;
        if (pendingShape == s.shape()) pendingShape = -1;
        boolean pro = item.pro();
        int edge = accent(pro);
        var player = minecraft.player;

        // Panel, with a coloured edge and a glow down the left side.
        g.fill(left - 1, top - 1, left + W + 1, top + H + 1, edge);
        g.fill(left, top, left + W, top + H, PANEL);
        g.fill(left, top, left + 2, top + H, edge);

        // Title.
        g.renderItem(pen, left + PAD, top + 7);
        g.drawString(font, pen.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.BOLD), left + PAD + 22, top + 8, TEXT, false);
        g.drawString(font, Component.translatable(pro ? "gui.fourtwenty.pave_pen.subtitle_pro" : "gui.fourtwenty.pave_pen.subtitle"), left + PAD + 22, top + 18, DIM, false);
        if (pro) {
            Component badge = Component.translatable("gui.fourtwenty.pave_pen.pro");
            int bw = font.width(badge) + 8, bx = left + W - PAD - bw;
            g.fill(bx, top + 8, bx + bw, top + 20, EMERALD);
            g.drawString(font, badge, bx + 4, top + 10, 0xFF0E2A1A, false);
        }
        g.fill(left + PAD, top + 30, left + W - PAD, top + 31, LINE);

        // Charge.
        int capacity = Math.max(1, Paving.numbers().capacity());
        label(g, "gui.fourtwenty.pave_pen.charge", top + 36);
        Component amount = Component.translatable("gui.fourtwenty.pave_pen.bi", String.format("%,d", s.charge()), String.format("%,d", capacity));
        g.drawString(font, amount, left + W - PAD - font.width(amount), top + 36, TEXT, false);
        int barX = left + PAD, barY = top + 47, barW = W - PAD * 2, barH = 9;
        g.fill(barX, barY, barX + barW, barY + barH, FRAME);
        int filled = (int) ((long) Math.min(s.charge(), capacity) * (barW - 2) / capacity);
        if (filled > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + filled, barY + barH - 1, edge);
            g.fill(barX + 1, barY + 1, barX + 1 + filled, barY + 3, 0x40FFFFFF);
        }
        long carried = Paving.fuelCarried(player);
        int dirt = PaveRules.jobs(s.charge() + carried, PaveRules.cost(PaveRules.Action.LAY, 0.5, Paving.numbers()));
        Component fuel = StrainTint.high() ? Component.translatable("gui.fourtwenty.pave_pen.free")
                : carried > 0 ? Component.translatable("gui.fourtwenty.pave_pen.fuel", String.format("%,d", carried), String.format("%,d", dirt))
                : s.charge() > 0 ? Component.translatable("gui.fourtwenty.pave_pen.last", String.format("%,d", dirt))
                : Component.translatable("gui.fourtwenty.pave_pen.no_fuel");
        g.drawString(font, fuel, left + PAD, top + 61, StrainTint.high() ? GOOD : carried > 0 || s.charge() > 0 ? DIM : WARN, false);

        // Lay: the three roads.
        label(g, "gui.fourtwenty.pave_pen.lay", layY() - 12);
        int stone = player.getInventory().countItem(Items.SMOOTH_STONE), gold = player.getInventory().countItem(Items.GOLD_INGOT);
        int chosen = target(s);
        for (int i = 0; i < PaveRules.TIERS; i++) {
            int x = cardX(i), y = layY(), w = cardW(), tier = i + 1;
            boolean hover = inside(mouseX, mouseY, x, y, w, LAY_H), selected = tier == chosen;
            card(g, x, y, w, LAY_H, hover, selected, edge);
            ItemStack road = new ItemStack(Paving.road(tier, Shape.of(shape(s, pro))));
            g.renderItem(road, x + (w - 16) / 2, y + 4);
            small(g, road.getHoverName(), x + w / 2, y + 23, selected ? TEXT : DIM);
            Component need;
            int colour;
            if (tier == 1) {
                need = Component.translatable("gui.fourtwenty.pave_pen.weed_only");
                colour = GOOD;
            } else {
                boolean haveStone = stone > 0 || s.stone() > 0, haveGold = tier < 3 || gold > 0 || s.gold() > 0;
                need = Component.translatable(tier == 2 ? "gui.fourtwenty.pave_pen.needs_stone" : "gui.fourtwenty.pave_pen.needs_gold",
                        tier == 2 ? stone : gold);
                colour = haveStone && haveGold ? GOOD : WARN;
            }
            small(g, need, x + w / 2, y + 34, colour);
        }

        // Shape: a Pro's slabs and stairs.
        label(g, "gui.fourtwenty.pave_pen.shape", shapeY() - 12);
        String[] shapes = {"gui.fourtwenty.pave_pen.block", "gui.fourtwenty.pave_pen.slab", "gui.fourtwenty.pave_pen.stairs"};
        for (int i = 0; i < shapes.length; i++) {
            int x = cardX(i), y = shapeY(), w = cardW();
            boolean hover = pro && inside(mouseX, mouseY, x, y, w, SHAPE_H), selected = i == shape(s, pro);
            card(g, x, y, w, SHAPE_H, hover, selected, edge);
            g.renderItem(new ItemStack(Paving.road(chosen, Shape.of(i))), x + 6, y + 7);
            g.drawString(font, Component.translatable(shapes[i]), x + 28, y + 11, selected ? TEXT : DIM, false);
        }
        if (!pro) {
            g.fill(cardX(0), shapeY(), cardX(2) + cardW(), shapeY() + SHAPE_H, 0xC814161A);
            Component lock = Component.translatable("gui.fourtwenty.pave_pen.locked");
            g.drawString(font, lock, left + (W - font.width(lock)) / 2, shapeY() + 11, WARN, false);
        }

        // How to use it.
        Component hint = Component.translatable("gui.fourtwenty.pave_pen.hint", PavePenClient.KEY.getTranslatedKeyMessage());
        small(g, hint, left + W / 2, top + H - 9, DIM);
    }

    private void label(GuiGraphics g, String key, int y) {
        g.drawString(font, Component.translatable(key).withStyle(net.minecraft.ChatFormatting.BOLD), left + PAD, y, DIM, false);
    }

    private void card(GuiGraphics g, int x, int y, int w, int h, boolean hover, boolean selected, int edge) {
        g.fill(x, y, x + w, y + h, selected ? edge : LINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? CARD_HOVER : CARD);
        if (selected) g.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, edge);
    }

    /** Text at three-quarter size, centred on {@code cx}. */
    private void small(GuiGraphics g, Component text, int cx, int y, int colour) {
        g.pose().pushPose();
        g.pose().translate(cx, y, 0);
        g.pose().scale(0.75f, 0.75f, 1);
        g.drawString(font, text, -font.width(text) / 2, 0, colour, false);
        g.pose().popPose();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack pen = pen();
        if (button == 0 && pen.getItem() instanceof PavePenItem item) {
            PenState s = PavePenItem.state(pen);
            for (int i = 0; i < 3; i++) {
                if (inside(mouseX, mouseY, cardX(i), layY(), cardW(), LAY_H)) return choose(i + 1, shape(s, item.pro()));
                if (item.pro() && inside(mouseX, mouseY, cardX(i), shapeY(), cardW(), SHAPE_H)) return choose(target(s), i);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean choose(int target, int shape) {
        pendingTarget = target;
        pendingShape = shape;
        PaveNetwork.send(target, shape);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (PavePenClient.KEY.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

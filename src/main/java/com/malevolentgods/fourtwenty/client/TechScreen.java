package com.malevolentgods.fourtwenty.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

/**
 * The look of everything on Boundless Inspiration: a 1024x1024 backplate per tier, an BI gauge that bubbles, and an overload that turns the
 * whole screen green and smoky. The backplate is only decoration: every slot, frame and gauge is drawn here on top of it, so any art can go
 * in without lining anything up. The atlas layout is shared with tools/generate_tech_ui.py.
 */
public abstract class TechScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final String[] PANELS = {"puff", "toke", "rip"};
    protected static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/tech_atlas.png");
    private static final int GAUGE_W = 14, GAUGE_H = 54, FILL_U = 16, FILL_FRAMES = 4, ARROW_V = 56, ARROW_W = 24, ARROW_H = 16, ARROW_FILL = 8, ARROW_FRAMES = 4, SLOT_V = 76,
            SMOKE_V = 96, SMOKE_FRAMES = 4, PUFFS = 9;
    private static final long FRAME_MS = 140;
    private static final int ART = 1024, TITLE_RULE = 15, POPUP_WIDTH = 170, GHOST = 0xB014161A;
    private static final int[] EDGES = {0xFF60C448, 0xFF3CC4B4, 0xFFA864E8};
    protected static final int TEXT = 0xFFE3E6DF, WARN = 0xFFE8B04A, GREEN = 0xFF7BFF5A;

    protected TechScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract int tier();

    protected abstract boolean overloaded();

    protected static int frame(int frames) {
        return (int) (System.currentTimeMillis() / FRAME_MS % frames);
    }

    private int tierIndex() { return Math.max(0, Math.min(tier(), 2)); }

    protected ResourceLocation panel() {
        return ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/tech_" + PANELS[tierIndex()] + ".png");
    }

    /** What an empty slot shows and says; null for slots with nothing to explain, like the player's inventory. */
    public record SlotHint(ItemStack ghost, Component tip) {}

    protected SlotHint hint(int slot) { return null; }

    /** Popups for anything that isn't a slot: call {@link #hover} for each. */
    protected void tips(GuiGraphics g, int mouseX, int mouseY) {}

    /** The key of this screen's tutorial, gui.fourtwenty.help.KEY. */
    protected abstract String helpKey();

    @Override protected void init() {
        super.init();
        addRenderableWidget(HelpScreen.button(leftPos + imageWidth - 15, topPos + 3, title, Component.translatable("gui.fourtwenty.help." + helpKey())));
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        if (overloaded()) overload(g);
        renderTooltip(g, mouseX, mouseY);
        if (hoveredSlot == null) tips(g, mouseX, mouseY);
        else if (!hoveredSlot.hasItem() && hint(hoveredSlot.index) != null) popup(g, mouseX, mouseY, hint(hoveredSlot.index).tip());
    }

    private void popup(GuiGraphics g, int mouseX, int mouseY, Component text) {
        g.renderTooltip(font, font.split(text, POPUP_WIDTH), mouseX, mouseY);
    }

    /** A popup over a rectangle given relative to the panel. */
    protected void hover(GuiGraphics g, int mouseX, int mouseY, int x, int y, int w, int h, Component text) {
        int mx = mouseX - leftPos, my = mouseY - topPos;
        if (mx >= x && mx < x + w && my >= y && my < y + h) popup(g, mouseX, mouseY, text);
    }

    /** The popup for an BI gauge at this spot. */
    protected void gaugeTip(GuiGraphics g, int mouseX, int mouseY, int x, int y, int stored, int capacity, Component what) {
        hover(g, mouseX, mouseY, x, y, GAUGE_W, GAUGE_H, Component.translatable("gui.fourtwenty.gauge", String.format("%,d", stored), String.format("%,d", capacity), what));
    }

    protected void arrowTip(GuiGraphics g, int mouseX, int mouseY, int x, int y, Component text) {
        hover(g, mouseX, mouseY, x, y, ARROW_W, ARROW_H, text);
    }

    @Override protected void renderBg(@Nonnull GuiGraphics g, float partial, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // The art's top left, scaled to the screen's shape: a short screen shows less of it, never a squashed version.
        g.blit(panel(), leftPos, topPos, imageWidth, imageHeight, 0, 0, ART, ART * imageHeight / imageWidth, ART, ART);
        int edge = EDGES[tierIndex()];
        g.renderOutline(leftPos, topPos, imageWidth, imageHeight, edge);
        g.fill(leftPos + 3, topPos + TITLE_RULE, leftPos + imageWidth - 3, topPos + TITLE_RULE + 1, edge);
        for (Slot slot : menu.slots) {
            g.blit(ATLAS, leftPos + slot.x - 1, topPos + slot.y - 1, 0, SLOT_V, 18, 18);
            SlotHint hint = hint(slot.index);
            if (hint == null || slot.hasItem()) continue;
            int x = leftPos + slot.x, y = topPos + slot.y;
            g.renderFakeItem(hint.ghost(), x, y);
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            g.fill(x, y, x + 16, y + 16, GHOST);
            g.pose().popPose();
        }
        details(g);
    }

    protected void details(GuiGraphics g) {}

    /** The BI gauge, bubbling up as it fills. */
    protected void gauge(GuiGraphics g, int x, int y, int stored, int capacity, int mouseX, int mouseY) {
        g.blit(ATLAS, x, y, 0, 0, GAUGE_W, GAUGE_H);
        int height = capacity <= 0 ? 0 : (int) Math.round((GAUGE_H - 4) * Math.min(1.0, (double) stored / capacity));
        if (height > 0) {
            int u = FILL_U + frame(FILL_FRAMES) * (GAUGE_W - 2);
            g.blit(ATLAS, x + 2, y + GAUGE_H - 2 - height, u, GAUGE_H - 2 - height, GAUGE_W - 4, height);
        }
    }

    /** The work arrow: {@code row} 0 is blades, 1 is a drying wave. */
    protected void arrow(GuiGraphics g, int x, int y, int row, int progress, int total) {
        g.blit(ATLAS, x, y, 0, ARROW_V, ARROW_W, ARROW_H);
        int width = total <= 0 ? 0 : (ARROW_W - 4) * Math.min(progress, total) / total;
        if (width > 0) g.blit(ATLAS, x + 2, y + (ARROW_H - ARROW_FILL) / 2, ARROW_W * (1 + frame(ARROW_FRAMES)), ARROW_V + row * ARROW_FILL, width, ARROW_FILL);
    }

    /** Overloaded: the screen goes green, smoke rolls up it, and the word pulses. */
    private void overload(GuiGraphics g) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(0, 0, 300);
        long now = System.currentTimeMillis();
        int pulse = (int) (70 + 30 * Math.sin(now / 200.0));
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, pulse << 24 | 0x2FD040);
        RenderSystem.enableBlend();
        for (int i = 0; i < PUFFS; i++) {
            double t = (now / 3600.0 + i * 0.37) % 1;
            int x = leftPos + (i * 53 + 11) % (imageWidth - 32) + (int) (8 * Math.sin(now / 500.0 + i));
            int y = topPos + imageHeight - 32 - (int) (t * (imageHeight - 24));
            g.setColor(0.7f, 1, 0.7f, (float) Math.sin(t * Math.PI) * 0.8f);
            g.blit(ATLAS, x, y, 32 * ((i + frame(SMOKE_FRAMES)) % SMOKE_FRAMES), SMOKE_V, 32, 32);
        }
        g.setColor(1, 1, 1, 1);
        Component word = Component.translatable("gui.fourtwenty.machine.overloaded");
        g.drawCenteredString(font, word, leftPos + imageWidth / 2, topPos + imageHeight / 2 - 4, GREEN);
        pose.popPose();
    }
}

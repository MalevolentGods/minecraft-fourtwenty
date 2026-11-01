package com.malevolentgods.fourtwenty.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nonnull;

/**
 * The look of everything on Boundless Inspiration: a panel in the tier's colours, an Inspo gauge that bubbles, and an overload that turns the
 * whole screen green and smoky. Art comes from tools/generate_tech_ui.py; the atlas layout is shared with it.
 */
public abstract class TechScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private static final String[] PANELS = {"puff", "toke", "rip"};
    protected static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/tech_atlas.png");
    private static final int GAUGE_W = 14, GAUGE_H = 54, FILL_U = 16, FILL_FRAMES = 4, ARROW_V = 56, ARROW_W = 24, ARROW_H = 16, ARROW_FRAMES = 4, SLOT_V = 76,
            SMOKE_V = 96, SMOKE_FRAMES = 4, PUFFS = 9;
    private static final long FRAME_MS = 140;
    protected static final int TEXT = 0xFFE3E6DF, WARN = 0xFFE8B04A, GREEN = 0xFF7BFF5A;

    protected TechScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    protected abstract int tier();

    protected abstract boolean overloaded();

    protected static int frame(int frames) {
        return (int) (System.currentTimeMillis() / FRAME_MS % frames);
    }

    protected ResourceLocation panel() {
        return ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/gui/tech_" + PANELS[Math.max(0, Math.min(tier(), 2))] + ".png");
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        renderBackground(g, mouseX, mouseY, partial);
        super.render(g, mouseX, mouseY, partial);
        if (overloaded()) overload(g);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override protected void renderBg(@Nonnull GuiGraphics g, float partial, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(panel(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            if (slot.y < topSlotsEnd()) g.blit(ATLAS, leftPos + slot.x - 1, topPos + slot.y - 1, 0, SLOT_V, 18, 18);
        }
        details(g);
    }

    /** Slots above this row get a frame drawn; the player's inventory is in the panel art. */
    protected int topSlotsEnd() { return 80; }

    protected void details(GuiGraphics g) {}

    /** The Inspo gauge, bubbling up as it fills. */
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
        int width = total <= 0 ? 0 : ARROW_W * Math.min(progress, total) / total;
        if (width > 0) g.blit(ATLAS, x, y, ARROW_W * (1 + frame(ARROW_FRAMES)), ARROW_V + row * ARROW_H / 2, width, ARROW_H);
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

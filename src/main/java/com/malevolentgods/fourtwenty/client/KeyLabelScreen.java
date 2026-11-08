package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.electronics.KeyLabels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;

/**
 * A mounted key's label (KeyLabels): up to four characters for its face, the colour they glow in (or plain ink), and the number a
 * Dankoder reads it as. A preview shows the face as it will look.
 */
public class KeyLabelScreen extends Screen {
    private static final int SWATCH = 12, GAP = 3;
    private final BlockPos pos;
    private final String start;
    private int glow, number;
    private EditBox box;

    private KeyLabelScreen(BlockPos pos, String label, int glow, int number) {
        super(Component.translatable("gui.fourtwenty.key_label"));
        this.pos = pos;
        this.start = label;
        this.glow = glow;
        this.number = number;
    }

    public static void open(BlockPos pos, String label, int glow, int number) {
        Minecraft.getInstance().setScreen(new KeyLabelScreen(pos, label, glow, number));
    }

    private static Component numberText(int n) {
        return n < 0 ? Component.translatable("gui.fourtwenty.key_label.number_auto")
                : Component.translatable("gui.fourtwenty.key_label.number", Integer.toHexString(n).toUpperCase(java.util.Locale.ROOT));
    }

    @Override protected void init() {
        int x = width / 2 - 60, y = height / 2 - 20;
        box = new EditBox(font, x, y, 120, 20, Component.translatable("gui.fourtwenty.key_label.text"));
        box.setMaxLength(KeyLabels.MAX);
        box.setValue(start);
        addRenderableWidget(box);
        setInitialFocus(box);
        // its number for a Dankoder: its place on the board, then 0 to F; click to step on, sneak-click (shift) to step back
        addRenderableWidget(Button.builder(numberText(number), b -> {
            number = Math.floorMod(number + 1 + (hasShiftDown() ? -1 : 1), 17) - 1;
            b.setMessage(numberText(number));
        }).bounds(x, y + 58, 120, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done()).bounds(x, y + 82, 120, 20).build());
    }

    /** The swatches' row: no glow, then the sixteen dyes. */
    private int swatchX(int i) { return width / 2 - (17 * (SWATCH + GAP) - GAP) / 2 + i * (SWATCH + GAP); }

    private int swatchY() { return height / 2 + 8; }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        for (int i = 0; i < 17; i++) {
            int sx = swatchX(i), sy = swatchY();
            if (mx >= sx && mx < sx + SWATCH && my >= sy && my < sy + SWATCH) {
                glow = i - 1;
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void done() {
        PacketDistributor.sendToServer(new KeyLabels.SetLabel(pos, KeyLabels.clean(box.getValue()), glow, number));
        onClose();
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == 257 || key == 335) {
            done();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(font, title, width / 2, height / 2 - 58, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("gui.fourtwenty.key_label.text"), width / 2, height / 2 - 32, 0xA0A0A0);
        // the preview: the label on a dark key face, as it will read
        int px = width / 2 + 70, py = height / 2 - 20;
        g.fill(px, py, px + 40, py + 20, 0xFF15171A);
        String label = KeyLabels.clean(box.getValue());
        int colour = glow < 0 ? 0xD8D8D8 : DyeColor.byId(glow).getFireworkColor();
        g.drawCenteredString(font, label, px + 20, py + 6, 0xFF000000 | colour);
        // the swatches: no glow (a slashed grey), then each dye; the chosen one framed
        for (int i = 0; i < 17; i++) {
            int sx = swatchX(i), sy = swatchY();
            if (i - 1 == glow) g.fill(sx - 2, sy - 2, sx + SWATCH + 2, sy + SWATCH + 2, 0xFFFFFFFF);
            if (i == 0) {
                g.fill(sx, sy, sx + SWATCH, sy + SWATCH, 0xFF3A3A40);
                for (int k = 0; k < SWATCH; k++) g.fill(sx + k, sy + SWATCH - 1 - k, sx + k + 1, sy + SWATCH - k, 0xFFB04040);
            } else {
                g.fill(sx, sy, sx + SWATCH, sy + SWATCH, 0xFF000000 | DyeColor.byId(i - 1).getFireworkColor());
            }
        }
        g.drawCenteredString(font, Component.translatable(glow < 0 ? "gui.fourtwenty.key_label.no_glow" : "color.minecraft." + DyeColor.byId(glow).getName()),
                width / 2, swatchY() + SWATCH + 5, 0xA0A0A0);
    }

    @Override public boolean isPauseScreen() { return false; }
}

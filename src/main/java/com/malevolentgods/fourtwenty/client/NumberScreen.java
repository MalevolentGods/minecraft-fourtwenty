package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.LitBitIdRules;
import com.malevolentgods.fourtwenty.lights.LightsNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nonnull;

/** A small screen with one number: a Lit Bit's ID, or the Sticky Finger's next ID. */
public abstract class NumberScreen extends Screen {
    private final Component prompt;
    private final int start;
    private EditBox box;

    protected NumberScreen(Component title, Component prompt, int start) {
        super(title);
        this.prompt = prompt;
        this.start = start;
    }

    protected abstract void submit(int value);

    @Override protected void init() {
        int x = width / 2 - 60, y = height / 2 - 10;
        box = new EditBox(font, x, y, 120, 20, prompt);
        box.setValue(Integer.toString(start));
        box.setFilter(s -> s.isEmpty() || s.matches("\\d{1,5}"));
        addRenderableWidget(box);
        setInitialFocus(box);
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> done()).bounds(x, y + 26, 120, 20).build());
    }

    private void done() {
        try {
            submit(LitBitIdRules.clamp(Integer.parseInt(box.getValue())));
        } catch (NumberFormatException ignored) {
            // nothing typed: keep what it was
        }
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
        g.drawCenteredString(font, title, width / 2, height / 2 - 44, 0xFFFFFF);
        g.drawCenteredString(font, prompt, width / 2, height / 2 - 28, 0xA0A0A0);
    }

    @Override public boolean isPauseScreen() { return false; }

    public static final class LitBit extends NumberScreen {
        private final BlockPos pos;

        LitBit(BlockPos pos, int id) {
            super(Component.translatable("block.fourtwenty.lit_bit"), Component.translatable("gui.fourtwenty.lit_bit.id"), id);
            this.pos = pos;
        }

        @Override protected void submit(int value) { PacketDistributor.sendToServer(new LightsNetwork.SetLampId(pos, value)); }
    }

    public static final class Finger extends NumberScreen {
        private final InteractionHand hand;

        Finger(InteractionHand hand, int next) {
            super(Component.translatable("item.fourtwenty.sticky_finger"), Component.translatable("gui.fourtwenty.sticky_finger.next"), next);
            this.hand = hand;
        }

        @Override protected void submit(int value) { PacketDistributor.sendToServer(new LightsNetwork.SetFingerId(hand == InteractionHand.OFF_HAND, value)); }
    }
}

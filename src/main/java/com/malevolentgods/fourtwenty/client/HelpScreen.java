package com.malevolentgods.fourtwenty.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import javax.annotation.Nonnull;
import java.util.List;

/** A machine's tutorial, opened over its screen by the ? button. Closing it goes back to the machine, which stays open underneath. */
public class HelpScreen extends Screen {
    private static final int WIDTH = 240, PADDING = 10, LINE = 10, EDGE = 0xFF60C448, PANEL = 0xF023262B, TEXT = 0xFFE3E6DF, TITLE = 0xFF7BFF5A, BUTTON_H = 16;

    private final Component body;
    private List<FormattedCharSequence> lines = List.of();

    public HelpScreen(Component title, Component body) {
        super(title);
        this.body = body;
    }

    private int panelHeight() {
        return PADDING * 3 + LINE * 2 + lines.size() * LINE + BUTTON_H;
    }

    @Override protected void init() {
        lines = font.split(body, WIDTH - PADDING * 2);
        int top = (height - panelHeight()) / 2;
        addRenderableWidget(new TechButton((width - 60) / 2, top + panelHeight() - PADDING - BUTTON_H, 60, BUTTON_H, Component.translatable("gui.done"), b -> onClose()));
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
        int left = (width - WIDTH) / 2, top = (height - panelHeight()) / 2;
        g.fill(0, 0, width, height, 0x80000000);
        g.pose().pushPose();
        g.fill(left, top, left + WIDTH, top + panelHeight(), PANEL);
        g.renderOutline(left, top, WIDTH, panelHeight(), EDGE);
        g.drawCenteredString(font, title, width / 2, top + PADDING, TITLE);
        int y = top + PADDING + LINE * 2;
        for (FormattedCharSequence line : lines) {
            g.drawString(font, line, left + PADDING, y, TEXT, false);
            y += LINE;
        }
        g.pose().popPose();
        super.render(g, mouseX, mouseY, partial);
    }

    @Override public void renderBackground(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {}

    @Override public void onClose() {
        Minecraft.getInstance().popGuiLayer();
    }

    /** The ? in a screen's corner. */
    public static TechButton button(int x, int y, Component title, Component body) {
        return new TechButton(x, y, 11, 11, Component.literal("?"), b -> Minecraft.getInstance().pushGuiLayer(new HelpScreen(title, body)));
    }

    /** A button in the tech style. */
    public static class TechButton extends Button {
        public TechButton(int x, int y, int width, int height, Component message, OnPress press) {
            super(x, y, width, height, message, press, DEFAULT_NARRATION);
        }

        @Override protected void renderWidget(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partial) {
            Font font = Minecraft.getInstance().font;
            g.fill(getX(), getY(), getX() + width, getY() + height, isHovered() ? TITLE : EDGE);
            g.fill(getX() + 1, getY() + 1, getX() + width - 1, getY() + height - 1, isHovered() ? 0xFF2C3A2C : 0xFF23262B);
            g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2 + 1, TEXT);
        }

        @Override public void playDownSound(@Nonnull SoundManager sounds) {
            sounds.play(SimpleSoundInstance.forUI(com.malevolentgods.fourtwenty.registry.ModPower.SWITCH.get(), 1));
        }
    }
}

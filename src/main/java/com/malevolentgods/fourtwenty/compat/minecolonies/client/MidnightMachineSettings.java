package com.malevolentgods.fourtwenty.compat.minecolonies.client;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Each player's own Midnight Machine volume and mute (the user, 2026-11-14), in the pause menu right under
 * ModularCraft's button (another Malevolent Gods mod, always in the pack, whose button sits at the top left). Kept in
 * fourtwenty-midnight-machine-client.toml, applied to her music live, on top of the Jukebox/Note Blocks slider.
 */
public final class MidnightMachineSettings {
    private MidnightMachineSettings() {}

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue VOLUME;
    private static final ModConfigSpec.BooleanValue MUTED;

    static {
        var b = new ModConfigSpec.Builder();
        b.push("midnightMachine");
        VOLUME = b.comment("The Midnight Machine's music, 0 to 1, on top of the Jukebox/Note Blocks slider.").defineInRange("volume", 1.0, 0, 1);
        MUTED = b.comment("Mute the Midnight Machine for you (the boombox and dancing stay).").define("muted", false);
        b.pop();
        SPEC = b.build();
    }

    /** ModularCraft's pause-menu button: x 6, y 6, 132 by 20. Ours go just under it. */
    private static final int X = 6, Y = 30, WIDTH = 132, HEIGHT = 20, GAP = 2;

    /** The gain to apply to her music: 0 when muted. */
    public static double gain() {
        try {
            return MUTED.get() ? 0 : VOLUME.get();
        } catch (IllegalStateException notLoaded) {
            return 1;
        }
    }

    static void install() {
        NeoForge.EVENT_BUS.addListener(MidnightMachineSettings::pauseMenu);
    }

    private static void pauseMenu(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof PauseScreen pause) || !pause.showsPauseMenu()) return;
        if (!SPEC.isLoaded()) return;
        event.addListener(new VolumeSlider(X, Y, WIDTH, HEIGHT));
        event.addListener(muteButton());
    }

    private static Button muteButton() {
        var button = Button.builder(muteLabel(), b -> {
            MUTED.set(!MUTED.get());
            MUTED.save();
            b.setMessage(muteLabel());
            b.setTooltip(muteTooltip());
        }).bounds(X + WIDTH + GAP, Y, HEIGHT, HEIGHT).build();
        button.setTooltip(muteTooltip());
        return button;
    }

    private static Component muteLabel() {
        return MUTED.get() ? Component.literal("✕").withStyle(ChatFormatting.RED) : Component.literal("♪");
    }

    private static Tooltip muteTooltip() {
        return Tooltip.create(Component.translatable(MUTED.get() ? "fourtwenty.midnight_machine.unmute" : "fourtwenty.midnight_machine.mute"));
    }

    private static final class VolumeSlider extends AbstractSliderButton {
        VolumeSlider(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty(), VOLUME.get());
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("fourtwenty.midnight_machine.volume", Math.round(value * 100)));
        }

        @Override
        protected void applyValue() {
            VOLUME.set(value);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            super.onRelease(mouseX, mouseY);
            VOLUME.save();
        }
    }
}

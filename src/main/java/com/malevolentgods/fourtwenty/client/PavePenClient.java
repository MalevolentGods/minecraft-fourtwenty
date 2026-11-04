package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.paving.PavePenItem;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

/** The Pave Pen's key (V unless rebound) opens its settings while a pen is in either hand. */
public final class PavePenClient {
    private PavePenClient() {}

    public static final KeyMapping KEY = new KeyMapping("key.fourtwenty.pave_pen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.fourtwenty");

    public static void registerKeys(RegisterKeyMappingsEvent event) { event.register(KEY); }

    public static void install() {
        PavePenItem.clientOpen = PavePenClient::open;
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            Minecraft mc = Minecraft.getInstance();
            while (KEY.consumeClick()) {
                InteractionHand hand = penHand();
                if (mc.screen == null && hand != null) open(hand);
            }
        });
    }

    static @Nullable InteractionHand penHand() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        for (InteractionHand hand : InteractionHand.values()) if (player.getItemInHand(hand).getItem() instanceof PavePenItem) return hand;
        return null;
    }

    public static void open(InteractionHand hand) { Minecraft.getInstance().setScreen(new PavePenScreen(hand)); }
}

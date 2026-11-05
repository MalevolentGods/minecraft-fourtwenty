package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import com.malevolentgods.fourtwenty.gear.Gear;
import com.malevolentgods.fourtwenty.gear.GearNetwork;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * The gear on the client: the gear key (terpene slots), the Helm's zoom key, and the Hotbox Helm's haze HUD: BI per worn piece, the
 * terpenes in them, and how high the wearer is.
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearClient {
    private GearClient() {}

    public static final KeyMapping SLOTS = new KeyMapping("key.fourtwenty.gear", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.fourtwenty");
    public static final KeyMapping ZOOM = new KeyMapping("key.fourtwenty.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.categories.fourtwenty");
    private static final int PANEL = 0xA014161A, GOOD = 0xFF7BFF5A, TEXT = 0xFFE3E6DF, DIM = 0xFF8A8F87;
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent event) {
        event.register(SLOTS);
        event.register(ZOOM);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> {
            while (SLOTS.consumeClick()) if (Minecraft.getInstance().screen == null) GearNetwork.openSlots();
        });
        NeoForge.EVENT_BUS.addListener(GearClient::zoom);
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ResourceLocation.fromNamespaceAndPath("fourtwenty", "haze_hud"), GearClient::hud);
    }

    private static ItemStack helm() {
        var player = Minecraft.getInstance().player;
        return player == null ? ItemStack.EMPTY : Gear.worn(player, EquipmentSlot.HEAD, Piece.HOTBOX_HELM);
    }

    /** The Helm's own check, as far as the client can tell: high and some charge. The server still decides what it pays for. */
    private static boolean helmOn() { return !helm().isEmpty() && StrainTint.high() && Gear.charge(helm()) > 0; }

    private static void zoom(ComputeFovModifierEvent event) {
        if (ZOOM.isDown() && helmOn() && Gear.has(helm(), "zoom")) event.setNewFovModifier(event.getNewFovModifier() * 0.3f);
    }

    private static void hud(GuiGraphics g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !helmOn()) return;
        var font = mc.font;
        int x = 6, y = 6, w = 132;
        long left = StrainTint.ticksLeft() / 20;
        var lines = new java.util.ArrayList<Component>();
        lines.add(Component.translatable("hud.fourtwenty.high", StrainTint.smoked().displayName(), String.format("%d:%02d", left / 60, left % 60)));
        for (EquipmentSlot slot : WORN) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (Gear.piece(stack).isEmpty()) continue;
            int cap = Math.max(1, Gear.capacity(stack));
            lines.add(Component.translatable("hud.fourtwenty.piece", stack.getHoverName(), Gear.charge(stack) * 100L / cap, Gear.state(stack).terps().size()));
        }
        int h = 6 + lines.size() * 11;
        g.fill(x - 3, y - 3, x + w, y + h - 3, PANEL);
        for (int i = 0; i < lines.size(); i++) g.drawString(font, lines.get(i), x, y + i * 11, i == 0 ? GOOD : TEXT, false);
        g.drawString(font, Component.translatable("hud.fourtwenty.haze"), x, y + h - 2, DIM, false);
    }
}

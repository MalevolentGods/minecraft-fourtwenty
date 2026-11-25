package com.malevolentgods.fourtwenty.client.handbook;

import com.malevolentgods.fourtwenty.handbook.SashSlot;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The sash slot in the survival inventory (SashSlot): drawn just above the off-hand slot, beside the player's figure, in the
 * vanilla slot look. It isn't one of the menu's slots, so no other mod's slot numbers move: a click sends {@link SashSlot.ClickPayload}
 * and the server swaps the cursor's item with the slot. Only drawn when the server has the channel (a server without Four Twenty
 * has no sash slot).
 */
@EventBusSubscriber(modid = "fourtwenty", value = Dist.CLIENT)
public final class SashSlotClient {
    private SashSlotClient() {}

    /** The slot's frame, relative to the inventory's top left: 18x18, directly above the off-hand slot (76, 61). */
    private static final int X = 76, Y = 43, SIZE = 18;
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("fourtwenty", "textures/item/stoner_badge_sash.png");

    private static boolean available() {
        var c = Minecraft.getInstance().getConnection();
        return c != null && c.hasChannel(SashSlot.ClickPayload.TYPE);
    }

    private static boolean wearing() {
        var p = Minecraft.getInstance().player;
        return p != null && SashSlot.CLIENT_WORN.contains(p.getId());
    }

    private static boolean over(InventoryScreen s, double mx, double my) {
        int x = s.getGuiLeft() + X, y = s.getGuiTop() + Y;
        return mx >= x && mx < x + SIZE && my >= y && my < y + SIZE;
    }

    /** Drawn with the background, under the cursor's item. */
    @SubscribeEvent
    public static void background(ContainerScreenEvent.Render.Background event) {
        if (!(event.getContainerScreen() instanceof InventoryScreen s) || !available()) return;
        GuiGraphics g = event.getGuiGraphics();
        int x = s.getGuiLeft() + X, y = s.getGuiTop() + Y;
        g.fill(x, y, x + SIZE - 1, y + SIZE - 1, 0xFF373737);               // vanilla slot: dark top and left,
        g.fill(x + 1, y + 1, x + SIZE, y + SIZE, 0xFFFFFFFF);               // light bottom and right,
        g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0xFF8B8B8B);       // grey well
        if (wearing()) {
            g.renderItem(new ItemStack(ModItems.STONER_BADGE_SASH.get()), x + 1, y + 1);
        } else {                                                              // a faint sash shows what goes here
            RenderSystem.enableBlend();
            g.setColor(1f, 1f, 1f, 0.3f);
            g.blit(ICON, x + 1, y + 1, 16, 16, 0, 0, 256, 256, 256, 256);
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
        }
        if (over(s, event.getMouseX(), event.getMouseY())) g.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, 0x80FFFFFF);
    }

    /** The tooltip, on top of everything, when nothing is on the cursor. */
    @SubscribeEvent
    public static void tooltip(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen s) || !available() || !over(s, event.getMouseX(), event.getMouseY())) return;
        if (!s.getMenu().getCarried().isEmpty()) return;
        Component line = wearing() ? Component.translatable("item.fourtwenty.stoner_badge_sash") : Component.translatable("gui.fourtwenty.sash_slot");
        event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, line, event.getMouseX(), event.getMouseY());
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof InventoryScreen s) || !available() || !over(s, event.getMouseX(), event.getMouseY())) return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(new SashSlot.ClickPayload());
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SashSlot.CLIENT_WORN.clear();
    }
}

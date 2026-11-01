package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.item.StrainItems;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.util.SmokeEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Every effect from smoking, in one badge showing what was smoked: on the HUD beside any other effect icons, and in the inventory, where hovering lists them all.
 * The effects themselves carry no icon, and mixin/EffectListMixin keeps them out of the inventory's own list.
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SmokeBadge {
    private SmokeBadge() {}

    private static final ResourceLocation HUD_FRAME = ResourceLocation.withDefaultNamespace("hud/effect_background");
    private static final ResourceLocation INVENTORY_FRAME = ResourceLocation.withDefaultNamespace("container/inventory/effect_background_small");
    private static final int HUD_SIZE = 24, HUD_STEP = 25, INVENTORY_SIZE = 32, INVENTORY_STEP = 33, INVENTORY_ROOM = 132;

    private static List<MobEffectInstance> smoke(Player player) {
        return player.getActiveEffects().stream().filter(SmokeEffects::isSmoke).sorted().toList();
    }

    /** The effects that aren't from smoking: what the inventory's own list shows. */
    public static Collection<MobEffectInstance> others(LocalPlayer player) {
        return player.getActiveEffects().stream().filter(e -> !SmokeEffects.isSmoke(e)).toList();
    }

    private static ItemStack smoked() {
        return ModDataComponents.withStrain(new ItemStack(ModItems.WEED_BUD.get()), StrainTint.smoked());
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(SmokeBadge::inventory);
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EFFECTS, ResourceLocation.fromNamespaceAndPath("fourtwenty", "smoke_badge"), SmokeBadge::hud);
    }

    private static void hud(GuiGraphics g, DeltaTracker delta) {
        var player = Minecraft.getInstance().player;
        if (player == null || smoke(player).isEmpty()) return;
        long beside = player.getActiveEffects().stream().filter(MobEffectInstance::showIcon).filter(e -> e.getEffect().value().isBeneficial()).count();
        int x = g.guiWidth() - HUD_STEP * (int) (beside + 1), y = 1;
        g.blitSprite(HUD_FRAME, x, y, HUD_SIZE, HUD_SIZE);
        g.renderItem(smoked(), x + 4, y + 4);
    }

    private static void inventory(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof EffectRenderingInventoryScreen<?> screen)) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var effects = smoke(player);
        int x = screen.getGuiLeft() + screen.getXSize() + 2;
        if (effects.isEmpty() || x + INVENTORY_SIZE > screen.width) return;
        int others = others(player).size();
        int y = screen.getGuiTop() + others * (others > 5 ? INVENTORY_ROOM / (others - 1) : INVENTORY_STEP);
        GuiGraphics g = event.getGuiGraphics();
        g.blitSprite(INVENTORY_FRAME, x, y, INVENTORY_SIZE, INVENTORY_SIZE);
        g.renderItem(smoked(), x + 8, y + 8);
        int mouseX = event.getMouseX(), mouseY = event.getMouseY();
        if (mouseX >= x && mouseX < x + INVENTORY_SIZE && mouseY >= y && mouseY < y + INVENTORY_SIZE) {
            g.renderComponentTooltip(Minecraft.getInstance().font, lines(player, effects), mouseX, mouseY);
        }
    }

    private static List<Component> lines(Player player, List<MobEffectInstance> effects) {
        Strain strain = StrainTint.smoked();
        float rate = player.level().tickRateManager().tickrate();
        List<Component> lines = new ArrayList<>();
        lines.add(StrainItems.strainName(strain));
        for (MobEffectInstance effect : effects) {
            var name = Component.translatable(effect.getDescriptionId());
            if (effect.getAmplifier() > 0) name.append(" ").append(Component.translatable("potion.potency." + effect.getAmplifier()));
            lines.add(name.append(" ").append(MobEffectUtil.formatDuration(effect, 1, rate))
                    .withStyle(effect.getEffect().value().isBeneficial() ? ChatFormatting.GRAY : ChatFormatting.RED));
        }
        return lines;
    }
}

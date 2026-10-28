package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * While a strain's high lasts, a faint wash of its colour sits at the edges of the screen: strongest just after the hit,
 * fading over the high, and gently breathing. Unobtrusive by design (well under a fifth opaque at the very edge).
 */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class StrainTint {
    private StrainTint() {}

    private static Strain strain = Strain.LANDRACE;
    private static long start, end;

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        // Bud and joint icons per strain (models/item/<item>.json overrides on this property: ordinal / 16).
        event.enqueueWork(() -> {
            var id = ResourceLocation.fromNamespaceAndPath("fourtwenty", "strain");
            for (var item : java.util.List.of(com.malevolentgods.fourtwenty.registry.ModItems.WEED_BUD.get(), com.malevolentgods.fourtwenty.registry.ModItems.WEED_JOINT.get()))
                net.minecraft.client.renderer.item.ItemProperties.register(item, id,
                        (stack, level, entity, seed) -> com.malevolentgods.fourtwenty.registry.ModDataComponents.strain(stack).ordinal() / 16f);
        });
        StrainEffects.clientTint = t -> {
            var level = Minecraft.getInstance().level;
            if (level == null) return;
            strain = t.strain();
            start = level.getGameTime();
            end = start + t.ticks();
        };
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, ResourceLocation.fromNamespaceAndPath("fourtwenty", "strain_tint"), StrainTint::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || strain == Strain.LANDRACE || mc.options.hideGui) return;
        long now = mc.level.getGameTime();
        if (now >= end) return;
        float t = (now + delta.getGameTimeDeltaPartialTick(false) - start) / Math.max(1f, end - start);
        float rise = Math.min(1f, t * 40f), fall = 1f - t * t;
        float breathe = 0.85f + 0.15f * (float) Math.sin((now + delta.getGameTimeDeltaPartialTick(false)) / 30.0);
        float peak = 0.17f * rise * fall * breathe;
        if (peak <= 0.005f) return;
        int w = g.guiWidth(), h = g.guiHeight(), rgb = strain.color() & 0xFFFFFF;
        int band = Math.max(12, Math.min(w, h) / 6), steps = 16;
        for (int i = 0; i < steps; i++) {
            float k = 1f - (float) i / steps;
            int a = Math.round(255 * peak * k * k);
            if (a <= 0) continue;
            int c = (a << 24) | rgb, o = i * band / steps, o2 = (i + 1) * band / steps;
            g.fill(0, o, w, o2, c);                 // top
            g.fill(0, h - o2, w, h - o, c);         // bottom
            g.fill(o, o2, o2, h - o2, c);           // left (between the top and bottom bands)
            g.fill(w - o2, o2, w - o, h - o2, c);   // right
        }
    }
}

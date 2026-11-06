package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.lights.LightsNetwork;
import com.malevolentgods.fourtwenty.lights.LitBitBlockEntity;
import com.malevolentgods.fourtwenty.lights.ModLights;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The client half of Lit Bits and the Weedweeno: colours from the server, screens, and the lamp renderer. Only ever called on a client. */
@EventBusSubscriber(modid = "fourtwenty", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LightsClient {
    private LightsClient() {}

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModLights.LIT_BIT_ENTITY.get(), context -> new LitBitRenderer());
    }

    public static void colours(LightsNetwork.LampColours m) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (int i = 0; i < m.at().length; i++) {
            if (level.getBlockEntity(BlockPos.of(m.at()[i])) instanceof LitBitBlockEntity lamp) lamp.showOnClient(m.rgb()[i]);
        }
    }

    public static void state(LightsNetwork.State m) {
        var mc = Minecraft.getInstance();
        if (mc.screen instanceof WeedweenoScreen screen && screen.pos().equals(m.pos())) screen.update(m);
        else if (m.open()) mc.setScreen(new WeedweenoScreen(m));
    }

    public static void openLitBit(BlockPos pos, int id) { Minecraft.getInstance().setScreen(new NumberScreen.LitBit(pos, id)); }

    public static void openFinger(InteractionHand hand, int next) { Minecraft.getInstance().setScreen(new NumberScreen.Finger(hand, next)); }
}

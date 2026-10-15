package com.malevolentgods.fourtwenty.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in standalone startup check: optional client setup must not resolve colony API classes. */
@EventBusSubscriber(modid="fourtwenty_tests", value=Dist.CLIENT)
public final class StandaloneClientProbe {
    private static boolean ran;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(ran||!Boolean.getBoolean("fourtwenty.standaloneClientProbe")||mc.getOverlay()!=null||!(mc.screen instanceof TitleScreen))return;
        ran=true;
        if(ModList.get().isLoaded("minecolonies"))throw new AssertionError("Standalone profile unexpectedly contains MineColonies");
        com.mojang.logging.LogUtils.getLogger().info("FOURTWENTY_STANDALONE_CLIENT_PROBE_PASSED");
        mc.stop();
    }
}

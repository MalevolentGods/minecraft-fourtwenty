package com.malevolentgods.fourtwenty.colonytest;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

/** GameTestServer omits profile services that MineColonies requires when restoring permissions. */
@EventBusSubscriber(modid = "fourtwenty_colonytests")
public final class RestartTestServices {
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void beforeColonyLoad(LevelEvent.Load event) throws ReflectiveOperationException {
        if (System.getProperty("fourtwenty.dispensaryLifecycleStage", "").isEmpty()
                || !(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !(level.getServer() instanceof net.minecraft.gametest.framework.GameTestServer server)
                || server.getProfileCache() != null) return;
        var cache = new net.minecraft.server.players.GameProfileCache(null,
                server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("fixture-usercache.json").toFile());
        // UUID lookup only, seeded with the fixture identity; no authentication or network service.
        cache.add(net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level).getGameProfile());
        var services = new net.minecraft.server.Services(null, com.mojang.authlib.yggdrasil.ServicesKeySet.EMPTY, null, cache);
        var field = net.minecraft.server.MinecraftServer.class.getDeclaredField("services");
        field.setAccessible(true); field.set(server, services);
        com.mojang.logging.LogUtils.getLogger().info("FOURTWENTY_RESTART_TEST_PROFILE_CACHE: test-only UUID cache installed");
    }
}

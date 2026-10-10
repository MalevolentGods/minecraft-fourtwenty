package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.util.DelayedEffectManager;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("fourtwenty_playertests")
@PrefixGameTestTemplate(false)
public final class EffectGameTests {
    /** Real server players with local connections; these are not network/client acceptance tests. */
    public static ServerPlayer player(GameTestHelper h) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "effect-fixture"), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(0);
        return player;
    }
    public static void remove(ServerPlayer player) {
        player.getServer().getPlayerList().remove(player);
        player.discard();
    }
    private static void ticks(GameTestHelper h, int count) {
        for (int i = 0; i < count; i++) DelayedEffectManager.onServerTick(new ServerTickEvent.Post(() -> true, h.getLevel().getServer()));
    }

    @GameTest(template = "empty")
    public static void foodOnsetRejectsDuplicatesAndUsesServerTicks(GameTestHelper h) {
        var cookiePlayer = player(h);
        var browniePlayer = player(h);
        try {
            var cookies = new ItemStack(ModItems.WEED_COOKIE.get(), 3);
            cookiePlayer.setItemInHand(InteractionHand.MAIN_HAND, cookies);
            ModItems.WEED_COOKIE.get().finishUsingItem(cookies, h.getLevel(), cookiePlayer);
            h.assertTrue(cookies.getCount() == 2, "First cookie consumed");
            int food = cookiePlayer.getFoodData().getFoodLevel();
            ModItems.WEED_COOKIE.get().finishUsingItem(cookies, h.getLevel(), cookiePlayer);
            h.assertTrue(cookies.getCount() == 2 && cookiePlayer.getFoodData().getFoodLevel() == food, "Duplicate dose consumes neither item nor nutrition");
            var brownies = new ItemStack(ModItems.WEED_BROWNIE.get(), 2);
            ModItems.WEED_BROWNIE.get().finishUsingItem(brownies, h.getLevel(), browniePlayer);
            h.assertTrue(brownies.getCount() == 1, "Independent player's brownie consumed");
            long time = h.getLevel().getDayTime();
            h.getLevel().setDayTime(time + 100000);
            ticks(h, 199);
            h.assertTrue(!cookiePlayer.hasEffect(MobEffects.NIGHT_VISION), "Daylight change does not accelerate onset");
            ticks(h, 1);
            h.assertTrue(cookiePlayer.hasEffect(MobEffects.NIGHT_VISION) && !browniePlayer.hasEffect(MobEffects.NIGHT_VISION), "Cookie starts at tick 200 only");
            ticks(h, 100);
            h.assertTrue(browniePlayer.hasEffect(MobEffects.JUMP) && !DelayedEffectManager.hasPendingEffect(browniePlayer), "Brownie starts at tick 300 once");
            h.getLevel().setDayTime(time);
            h.succeed();
        } finally { remove(cookiePlayer); remove(browniePlayer); }
    }

    @GameTest(template = "empty")
    public static void logoutDeathAndServerStopCancelPendingEffects(GameTestHelper h) {
        var player = player(h);
        try {
            DelayedEffectManager.scheduleEffect(player, 2, 100, 0, false);
            DelayedEffectManager.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
            ticks(h, 3);
            h.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Logout cancels pending onset");
            DelayedEffectManager.scheduleEffect(player, 2, 100, 0, false);
            player.setHealth(0);
            ticks(h, 1);
            player.setHealth(20);
            ticks(h, 2);
            h.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Finalized death cancels onset before revival/respawn");
            DelayedEffectManager.scheduleEffect(player, 2, 100, 0, false);
            DelayedEffectManager.onClone(new PlayerEvent.Clone(player, player, true));
            h.assertTrue(!DelayedEffectManager.hasPendingEffect(player), "Death clone clears pending state");
            DelayedEffectManager.scheduleEffect(player, 2, 100, 0, false);
            DelayedEffectManager.onStopping(new ServerStoppingEvent(h.getLevel().getServer()));
            ticks(h, 3);
            h.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Shutdown clears server queue");
            h.succeed();
        } finally { remove(player); }
    }

    @GameTest(template = "empty")
    public static void dimensionTravelPreservesOnsetAndVanillaEffectRules(GameTestHelper h) {
        var player = player(h);
        try {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 4));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 2));
            DelayedEffectManager.scheduleEffect(player, 2, 400, 0, false);
            var nether = h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
            player.teleportTo(nether, 0, 100, 0, 0, 0);
            h.assertTrue(DelayedEffectManager.hasPendingEffect(player), "Dimension transfer preserves pending dose");
            ticks(h, 2);
            h.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "Effect starts in destination dimension");
            h.assertTrue(player.getEffect(MobEffects.REGENERATION).getAmplifier() == 4 && player.hasEffect(MobEffects.DAMAGE_RESISTANCE),
                "Stronger and unrelated vanilla effects survive onset");
            Items.MILK_BUCKET.finishUsingItem(new ItemStack(Items.MILK_BUCKET), player.level(), player);
            h.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Vanilla milk removes active effects");
            ticks(h, 5);
            h.assertTrue(!player.hasEffect(MobEffects.NIGHT_VISION), "Consumed pending dose does not reappear after milk");
            h.succeed();
        } finally { remove(player); }
    }
}

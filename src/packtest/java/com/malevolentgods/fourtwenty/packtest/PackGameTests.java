package com.malevolentgods.fourtwenty.packtest;

import com.malevolentgods.fourtwenty.gametest.EffectGameTests;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.util.DelayedEffectManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import team.creative.playerrevive.server.PlayerReviveServer;

@GameTestHolder("fourtwenty_packtests")
@PrefixGameTestTemplate(false)
public final class PackGameTests {
    @GameTest(template = "empty", batch = "harvest_replant_setting")
    public static void fakePlayerHarvestRespectsDisabledReplanting(GameTestHelper h) throws Exception {
        var commonField = actuallyharvest.config.ConfigHandler.class.getDeclaredField("COMMON");
        commonField.setAccessible(true);
        var replantField = actuallyharvest.config.ConfigHandler.Common.class.getDeclaredField("replantCrops");
        replantField.setAccessible(true);
        var setting = replantField.get(commonField.get(null));
        var get = setting.getClass().getMethod("get");
        var set = setting.getClass().getMethod("set", Object.class);
        boolean original = (boolean) get.invoke(setting);
        try {
            set.invoke(setting, false);
            var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "harvest-machine"));
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var pos = new BlockPos(2, 2, 2);
            var absolute = h.absolutePos(pos);
            player.setPos(Vec3.atCenterOf(absolute));
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            h.setBlock(pos.below(), Blocks.FARMLAND);
            h.setBlock(pos, ModBlocks.WEED_CROP.get().defaultBlockState().setValue(CropBlock.AGE, 7));
            player.gameMode.useItemOn(player, h.getLevel(), ItemStack.EMPTY, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false));
            h.assertTrue(h.getBlockState(pos).isAir(), "Disabled replanting leaves no crop after fake-player harvest: state=" + h.getBlockState(pos) + "; replant=" + actuallyharvest.config.ConfigHandler.Common.replantCrops() + "; fake=" + actuallyharvest.config.ConfigHandler.Common.allowFakePlayer() + "; empty=" + actuallyharvest.config.ConfigHandler.Common.allowEmptyHand());
            h.assertItemEntityPresent(ModItems.WEED_SEEDS.get(), pos, 3);
            h.assertItemEntityPresent(ModItems.WEED_BUD.get(), pos, 3);
            h.succeed();
        } finally { set.invoke(setting, original); }
    }

    @GameTest(template = "empty", batch = "harvest_pack")
    public static void actuallyHarvestOwnsClicksAndAreaReplanting(GameTestHelper h) {
        var player = EffectGameTests.player(h);
        try {
            var center = new BlockPos(3, 2, 3);
            var absolute = h.absolutePos(center);
            player.setPos(absolute.getX(), absolute.getY(), absolute.getZ());
            var mature = ModBlocks.WEED_CROP.get().defaultBlockState().setValue(CropBlock.AGE, 7);
            h.assertTrue(actuallyharvest.config.ConfigHandler.Common.getCrops().containsKey(mature), "Installed auto-discovery recognizes cannabis");
            h.assertTrue(actuallyharvest.config.ConfigHandler.Common.replantCrops(), "Reference configuration enables replanting");
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                h.setBlock(center.offset(x, -1, z), Blocks.FARMLAND);
                h.setBlock(center.offset(x, 0, z), mature);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_HOE));
            var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            int matureAfter = 0;
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                var state = h.getBlockState(center.offset(x, 0, z));
                h.assertTrue(state.is(ModBlocks.WEED_CROP.get()), "Area harvest replants each cannabis block");
                if (state.getValue(CropBlock.AGE) == 7) matureAfter++;
            }
            h.assertTrue(matureAfter == 0, "Hoe harvests the entire configured 3x3 area");
            var bounds = new net.minecraft.world.phys.AABB(absolute).inflate(4);
            int buds = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .filter(e -> e.getItem().is(ModItems.WEED_BUD.get())).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(buds >= 9 && buds <= 27, "Exactly one normal harvest per crop");
            int xp = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, bounds).stream().mapToInt(net.minecraft.world.entity.ExperienceOrb::getValue).sum();
            h.assertTrue(xp == 9, "Installed XP configuration awards one XP per harvested crop");
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            player.gameMode.useItemOn(player, h.getLevel(), ItemStack.EMPTY, InteractionHand.OFF_HAND, hit);
            int after = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, bounds).stream()
                .filter(e -> e.getItem().is(ModItems.WEED_BUD.get())).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertTrue(after == buds, "Second-hand click on replanted crop cannot award another harvest");
            h.succeed();
        } finally { EffectGameTests.remove(player); }
    }

    @GameTest(template = "empty", batch = "revive_pack")
    public static void canceledDeathStaysDownedDuringEdibleOnset(GameTestHelper h) {
        var player = EffectGameTests.player(h);
        boolean singleplayer = team.creative.playerrevive.PlayerRevive.CONFIG.bleedInSingleplayer;
        try {
            // GameTestServer reports itself unpublished. Enable the native death handler only
            // for this fixture, restoring the reference setting even if the assertion fails.
            team.creative.playerrevive.PlayerRevive.CONFIG.bleedInSingleplayer = true;
            DelayedEffectManager.scheduleEffect(player, 2, 400, 0, false);
            // Use the installed mod's real cancelable death handler, not a synthetic isAlive flag.
            var death = new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(player, player.damageSources().generic());
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(death);
            h.assertTrue(death.isCanceled() && PlayerReviveServer.isBleeding(player) && player.isAlive(), "PlayerRevive cancels death into a living downed state");
            for (int i = 0; i < 2; i++) DelayedEffectManager.onServerTick(new ServerTickEvent.Post(() -> true, h.getLevel().getServer()));
            for (int i = 0; i < 60; i++) {
                player.tick();
                h.assertTrue(PlayerReviveServer.isBleeding(player), "Regeneration must not end PlayerRevive's downed state");
            }
            PlayerReviveServer.revive(player); // Fixture performs the native revival; production code never calls this API.
            h.assertTrue(!PlayerReviveServer.isBleeding(player) && player.isAlive(), "Native revival remains available");
            DelayedEffectManager.scheduleEffect(player, 2, 400, 0, false);
            PlayerReviveServer.kill(player);
            DelayedEffectManager.onServerTick(new ServerTickEvent.Post(() -> true, h.getLevel().getServer()));
            h.assertTrue(!DelayedEffectManager.hasPendingEffect(player), "Final death clears pending onset");
            h.succeed();
        } finally {
            team.creative.playerrevive.PlayerRevive.CONFIG.bleedInSingleplayer = singleplayer;
            EffectGameTests.remove(player);
        }
    }
}

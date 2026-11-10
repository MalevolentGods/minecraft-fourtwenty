package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.GearRules;
import com.malevolentgods.fourtwenty.gear.BlazerItem;
import com.malevolentgods.fourtwenty.gear.DabCellItem;
import com.malevolentgods.fourtwenty.gear.FlightLease;
import com.malevolentgods.fourtwenty.gear.Gear;
import com.malevolentgods.fourtwenty.gear.GearPowers;
import com.malevolentgods.fourtwenty.gear.ModGear;
import com.malevolentgods.fourtwenty.gear.ReUpStationBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class GearGameTests {
    /** Stands in for a claim: while set, every block break and place is refused, as a protection mod would. */
    private static volatile boolean protectEverything;

    static {
        NeoForge.EVENT_BUS.addListener((BlockEvent.BreakEvent e) -> { if (protectEverything) e.setCanceled(true); });
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent e) -> { if (protectEverything) e.setCanceled(true); });
    }

    private static ItemStack charged(net.minecraft.world.item.Item item, int charge, String... terps) {
        ItemStack stack = new ItemStack(item);
        Gear.set(stack, Gear.state(stack).withCharge(charge).withTerps(List.of(terps)));
        return stack;
    }

    private static ServerPlayer wearer(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        StrainEffects.forget(p);
        return p;
    }

    @GameTest(template = "empty")
    public static void soberGearRunsOnItsChargeOrNotAtAll(GameTestHelper h) {
        var p = wearer(h);
        // sober and empty: nothing
        p.setItemSlot(EquipmentSlot.HEAD, charged(ModGear.HOTBOX_HELM.get(), 0));
        p.setItemSlot(EquipmentSlot.FEET, charged(ModGear.MOON_BOOTS.get(), 0));
        GearPowers.tickPlayer(p, true);
        h.assertFalse(p.hasEffect(MobEffects.NIGHT_VISION), "no night vision sober and empty");
        var fall = new LivingFallEvent(p, 20, 1);
        GearPowers.fall(fall);
        h.assertTrue(fall.getDamageMultiplier() == 1, "sober, empty boots don't soften a fall");
        // sober and charged: it runs on its charge, and pays
        p.setItemSlot(EquipmentSlot.HEAD, charged(ModGear.HOTBOX_HELM.get(), 10_000));
        p.setItemSlot(EquipmentSlot.FEET, charged(ModGear.MOON_BOOTS.get(), 10_000));
        GearPowers.tickPlayer(p, true);
        h.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION), "sober, night vision on its charge");
        var fall2 = new LivingFallEvent(p, 20, 1);
        GearPowers.fall(fall2);
        h.assertTrue(fall2.getDamageMultiplier() == 0, "sober, charged boots soften the fall");
        h.assertTrue(Gear.charge(p.getItemBySlot(EquipmentSlot.HEAD)) < 10_000 && Gear.charge(p.getItemBySlot(EquipmentSlot.FEET)) < 10_000, "and both paid");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void highAndChargedTheSuitWorks(GameTestHelper h) {
        var p = wearer(h);
        p.setItemSlot(EquipmentSlot.HEAD, charged(ModGear.HOTBOX_HELM.get(), 10_000));
        p.setItemSlot(EquipmentSlot.LEGS, charged(ModGear.COUCHLOCK_GREAVES.get(), 10_000));
        p.setItemSlot(EquipmentSlot.FEET, charged(ModGear.MOON_BOOTS.get(), 10_000));
        StrainEffects.markHigh(p, 2000);
        GearPowers.tickPlayer(p, true);
        h.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION), "night vision");
        h.assertTrue(Gear.charge(p.getItemBySlot(EquipmentSlot.HEAD)) == 10_000, "free while high, like the machines");
        var fall = new LivingFallEvent(p, 20, 1);
        GearPowers.fall(fall);
        h.assertTrue(fall.getDamageMultiplier() == 0, "the boots soak up the fall");
        var knock = new LivingKnockBackEvent(p, 1, 1, 0);
        GearPowers.knockback(knock);
        h.assertTrue(knock.isCanceled(), "the greaves stop knockback");
        h.assertTrue(Gear.charge(p.getItemBySlot(EquipmentSlot.FEET)) == 10_000 && Gear.charge(p.getItemBySlot(EquipmentSlot.LEGS)) == 10_000, "none of it paid");
        StrainEffects.forget(p);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void armourAlwaysProtects(GameTestHelper h) {
        // The protection is the item's own armour value, which nothing about charge or the high touches.
        ItemStack plate = charged(ModGear.PUFF_PLATE.get(), 0);
        double armour = plate.getAttributeModifiers().modifiers().stream()
                .filter(e -> e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)).mapToDouble(e -> e.modifier().amount()).sum();
        h.assertTrue(armour >= 8, "an empty Plate still protects: " + armour);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void thePuffPackFliesAndLandsSoftlyWhenTheHighEnds(GameTestHelper h) {
        var p = wearer(h);
        p.setItemSlot(EquipmentSlot.CHEST, charged(ModGear.PUFF_PLATE.get(), 100_000));
        StrainEffects.markHigh(p, 2000);
        GearPowers.tickPlayer(p, false);
        h.assertTrue(p.getAbilities().mayfly, "high, the Plate lends flight");
        p.getAbilities().flying = true;
        p.teleportTo(p.getX(), p.getY() + 30, p.getZ());
        p.setOnGround(false);
        GearPowers.tickPlayer(p, false);
        h.assertTrue(Gear.charge(p.getItemBySlot(EquipmentSlot.CHEST)) == 100_000, "flying is free while high");
        StrainEffects.forget(p);
        GearPowers.tickPlayer(p, false);
        h.assertTrue(p.getAbilities().mayfly, "sober, it flies on its charge");
        h.assertTrue(Gear.charge(p.getItemBySlot(EquipmentSlot.CHEST)) < 100_000, "and pays for it");
        Gear.set(p.getItemBySlot(EquipmentSlot.CHEST), Gear.state(p.getItemBySlot(EquipmentSlot.CHEST)).withCharge(0));
        GearPowers.tickPlayer(p, false);
        h.assertFalse(p.getAbilities().mayfly || p.getAbilities().flying, "sober and empty, the flight is taken back");
        h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), "and they float down");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anEmptyOrRemovedPlateLandsSoftlyToo(GameTestHelper h) {
        for (int path = 0; path < 2; path++) {
            var p = wearer(h);
            p.setItemSlot(EquipmentSlot.CHEST, charged(ModGear.PUFF_PLATE.get(), 100_000));
            StrainEffects.markHigh(p, 2000);
            GearPowers.tickPlayer(p, false);
            p.getAbilities().flying = true;
            p.teleportTo(p.getX(), p.getY() + 30, p.getZ());
            p.setOnGround(false);
            if (path == 0) {
                // sober and empty: nothing pays for it any more
                Gear.set(p.getItemBySlot(EquipmentSlot.CHEST), Gear.state(p.getItemBySlot(EquipmentSlot.CHEST)).withCharge(0));
                StrainEffects.forget(p);
            }
            else p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            GearPowers.tickPlayer(p, false);
            h.assertFalse(p.getAbilities().mayfly, (path == 0 ? "empty" : "removed") + ": flight taken back");
            h.assertTrue(p.hasEffect(MobEffects.SLOW_FALLING), (path == 0 ? "empty" : "removed") + ": floats down");
            StrainEffects.forget(p);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theLeaseNeverTakesAPlayersOwnFlight(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.CREATIVE);
        FlightLease.lend(p, GearPowers.PLATE_FLIGHT);
        FlightLease.release(p, GearPowers.PLATE_FLIGHT);
        h.assertTrue(p.getAbilities().mayfly, "creative flight stays");
        var q = wearer(h);
        FlightLease.lend(q, "golden_god");
        FlightLease.lend(q, GearPowers.PLATE_FLIGHT);
        FlightLease.release(q, GearPowers.PLATE_FLIGHT);
        h.assertTrue(q.getAbilities().mayfly, "Golden God still lends it");
        FlightLease.release(q, "golden_god");
        h.assertFalse(q.getAbilities().mayfly, "the last one out takes it back");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aReUpStationChargesAtItsTierRate(GameTestHelper h) {
        var at = new BlockPos(1, 2, 1);
        h.setBlock(at, ModGear.RE_UP_STATIONS.get(0).get());
        var station = (ReUpStationBlockEntity) h.getBlockEntity(at);
        ((BIBuffer) station.node()).set(station.node().capacity());
        station.hang(new ItemStack(ModGear.MOON_BOOTS.get()));
        long start = h.getLevel().getGameTime();
        h.runAfterDelay(20, () -> {
            ItemStack boots = station.hung();
            double want = (h.getLevel().getGameTime() - start) * GearRules.stationRate(BIRules.Tier.PUFF);
            h.assertTrue(Math.abs(Gear.charge(boots) - want) <= GearRules.stationRate(BIRules.Tier.PUFF) * 2 + 1, "charged " + Gear.charge(boots) + ", wanted about " + want);
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void aDabCellTopsUpWornGear(GameTestHelper h) {
        var p = wearer(h);
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModGear.MOON_BOOTS.get()));
        ItemStack cell = charged(ModGear.DAB_CELL.get(), 10_000);
        int moved = DabCellItem.topUp(p, cell);
        h.assertTrue(moved > 0 && Gear.charge(p.getItemBySlot(EquipmentSlot.FEET)) == moved && Gear.charge(cell) == 10_000 - moved, "moved " + moved);
        h.succeed();
    }

    private static ServerPlayer blazerAt(GameTestHelper h, ItemStack blazer) {
        var p = wearer(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, blazer);
        StrainEffects.markHigh(p, 2000);
        return p;
    }

    @GameTest(template = "empty")
    public static void boreTokeMinesAThreeByThreeFace(GameTestHelper h) {
        var centre = new BlockPos(2, 3, 2);
        for (int x = 1; x <= 3; x++) for (int y = 2; y <= 4; y++) h.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
        ItemStack blazer = charged(ModGear.BLAZER.get(), 100_000, "lim_pin_pin");
        var p = blazerAt(h, blazer);
        var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(centre)), Direction.NORTH, h.absolutePos(centre), false);
        h.assertTrue(BlazerItem.bore(p, blazer, hit), "it bored");
        for (int x = 1; x <= 3; x++) for (int y = 2; y <= 4; y++) h.assertBlockPresent(Blocks.AIR, new BlockPos(x, y, 2));
        h.assertTrue(Gear.charge(blazer) == 100_000, "free while high");
        StrainEffects.forget(p);
        for (int x = 1; x <= 3; x++) for (int y = 2; y <= 4; y++) h.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
        h.assertTrue(BlazerItem.bore(p, blazer, hit), "sober, it bores on its charge");
        h.assertTrue(Gear.charge(blazer) == 100_000 - 9 * Gear.cost(blazer, "bore_toke"), "nine blocks paid for");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theBlazerChangesNothingInProtectedLand(GameTestHelper h) {
        var centre = new BlockPos(2, 3, 2);
        h.setBlock(centre, Blocks.STONE);
        h.setBlock(new BlockPos(2, 2, 4), Blocks.OAK_LOG);
        ItemStack blazer = charged(ModGear.BLAZER.get(), 100_000, "lim_pin_pin", "lim_lav_lav", "mc_orange");
        var p = blazerAt(h, blazer);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 16));
        protectEverything = true;
        try {
            BlazerItem.bore(p, blazer, new BlockHitResult(Vec3.atCenterOf(h.absolutePos(centre)), Direction.NORTH, h.absolutePos(centre), false));
            BlazerItem.timber(p, blazer, h.absolutePos(new BlockPos(2, 2, 4)));
            BlazerItem.paint(p, blazer, new BlockHitResult(Vec3.atCenterOf(h.absolutePos(centre)), Direction.UP, h.absolutePos(centre), false), false);
        } finally {
            protectEverything = false;
        }
        h.assertBlockPresent(Blocks.STONE, centre);
        h.assertBlockPresent(Blocks.OAK_LOG, new BlockPos(2, 2, 4));
        h.assertBlockPresent(Blocks.AIR, centre.above());
        h.assertTrue(p.getOffhandItem().getCount() == 16, "no paint used");
        StrainEffects.forget(p);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void terpenesUnlockAbilitiesAndOnlyTheirFamilyFits(GameTestHelper h) {
        var p = wearer(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, charged(ModGear.MOON_BOOTS.get(), 1000));
        var menu = new com.malevolentgods.fourtwenty.gear.GearMenu(1, p.getInventory(), InteractionHand.MAIN_HAND);
        var slot = menu.getSlot(0);
        h.assertFalse(slot.mayPlace(com.malevolentgods.fourtwenty.gear.GearMenu.stackOf("lim_pin_pin")), "a red won't go in the boots");
        ItemStack blue = com.malevolentgods.fourtwenty.gear.GearMenu.stackOf("pin_lav_lav");
        h.assertTrue(slot.mayPlace(blue), "a blue will");
        slot.set(blue);
        h.assertTrue(Gear.has(p.getMainHandItem(), "water_walk"), "and unlocks water walking");
        slot.set(ItemStack.EMPTY);
        h.assertFalse(Gear.has(p.getMainHandItem(), "water_walk"), "taken out, it's gone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void timberTorchFellsTheWholeTree(GameTestHelper h) {
        for (int y = 2; y <= 6; y++) h.setBlock(new BlockPos(2, y, 2), Blocks.OAK_LOG);
        h.setBlock(new BlockPos(3, 6, 2), Blocks.OAK_LOG);
        ItemStack blazer = charged(ModGear.BLAZER.get(), 100_000, "lim_lav_lav");
        var p = blazerAt(h, blazer);
        h.assertTrue(BlazerItem.timber(p, blazer, h.absolutePos(new BlockPos(2, 2, 2))), "felled");
        for (int y = 2; y <= 6; y++) h.assertBlockPresent(Blocks.AIR, new BlockPos(2, y, 2));
        h.assertBlockPresent(Blocks.AIR, new BlockPos(3, 6, 2));
        StrainEffects.forget(p);
        h.succeed();
    }
}

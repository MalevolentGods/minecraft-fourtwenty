package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.Pollination;
import com.malevolentgods.fourtwenty.block.WeedCropBlock;
import com.malevolentgods.fourtwenty.block.WeedCropBlockEntity;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.entity.BuzzBot;
import com.malevolentgods.fourtwenty.infuser.Infuser;
import com.malevolentgods.fourtwenty.processing.PhenoBlockEntity;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModEntities;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.StashBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class PhenoGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);
    private static final String BATCH = "pheno";

    private static ItemStack seed(Strain strain, Traits traits) {
        return ModDataComponents.withTraits(ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get()), strain), traits);
    }

    private static PhenoBlockEntity chamber(GameTestHelper h, Block block) {
        h.setBlock(AT, block);
        return filled(h);
    }

    private static PhenoBlockEntity filled(GameTestHelper h) {
        var pheno = (PhenoBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(AT));
        ((BIBuffer) pheno.node()).set(1_000_000);
        return pheno;
    }

    /** The override is shared, and tests in a batch run at once: it goes back when they're all done. */
    @BeforeBatch(batch = BATCH)
    public static void quickCrosses(ServerLevel level) { ProcessingConfig.overrideCrossTicks(2); }

    @AfterBatch(batch = BATCH)
    public static void normalCrosses(ServerLevel level) { ProcessingConfig.overrideCrossTicks(null); }

    @GameTest(template = "empty", batch = BATCH)
    public static void parentsMakeTheirHybrid(GameTestHelper h) {
        var pheno = chamber(h, ModPower.PHENO_CHAMBER.get());
        pheno.setItem(0, seed(Strain.WAKE_AND_BAKE, new Traits(4, 4, 4)));
        pheno.setItem(1, seed(Strain.SUNSHINE_DAYDREAM, new Traits(4, 4, 4)));
        h.succeedWhen(() -> {
            ItemStack out = pheno.getItem(PhenoBlockEntity.PARENTS);
            h.assertTrue(ModDataComponents.strain(out) == Strain.ALARM_CLOCK_ASSASSIN, "made the hybrid, got " + out);
            Traits t = ModDataComponents.traits(out);
            h.assertTrue(Math.abs(t.potency() - 4) <= 1 && Math.abs(t.yield() - 4) <= 1 && Math.abs(t.length() - 4) <= 1, "traits are the average, give or take one: " + t);
            h.assertTrue(pheno.getItem(0).isEmpty() && pheno.getItem(1).isEmpty(), "the parents went in");
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = BATCH)
    public static void aWeakChamberRefusesAHighCross(GameTestHelper h) {
        var pheno = chamber(h, ModPower.PHENO_CHAMBER.get());
        pheno.setItem(0, seed(Strain.OLD_GROWTH, Traits.DEFAULT));
        pheno.setItem(1, seed(Strain.OLD_GROWTH, Traits.DEFAULT));
        h.runAfterDelay(10, () -> {
            h.assertTrue(pheno.getItem(PhenoBlockEntity.PARENTS).isEmpty() && pheno.progress() == 0, "a Pheno Chamber can't breed a Double Cross");
            h.setBlock(AT, ModPower.PHENO_LAB.get());
            var lab = filled(h);
            lab.setItem(0, seed(Strain.OLD_GROWTH, Traits.DEFAULT));
            lab.setItem(1, seed(Strain.OLD_GROWTH, Traits.DEFAULT));
            h.succeedWhen(() -> {
                h.assertTrue(ModDataComponents.strain(lab.getItem(PhenoBlockEntity.PARENTS)) == Strain.OLD_GROWTH, "a Pheno Lab can");
                h.succeed();
            });
        });
    }

    /** Double Crosses and up aren't for the Chamber, even a Cathedral. */
    @GameTest(template = "empty", batch = BATCH)
    public static void noChamberMakesWhatNeedsPollinating(GameTestHelper h) {
        var pheno = chamber(h, ModPower.PHENO_CATHEDRAL.get());
        pheno.setItem(0, seed(Strain.MOSS_BOSS, Traits.DEFAULT));
        pheno.setItem(1, seed(Strain.CANOPY_CREEPER, Traits.DEFAULT));
        h.runAfterDelay(10, () -> {
            h.assertTrue(pheno.getItem(PhenoBlockEntity.PARENTS).isEmpty() && !pheno.getItem(0).isEmpty(), "it won't start");
            h.succeed();
        });
    }

    private static void plant(GameTestHelper h, BlockPos at, Block plot, Strain strain) {
        h.setBlock(at.below(), plot);
        h.setBlock(at, ModBlocks.WEED_CROP.get().defaultBlockState().setValue(WeedCropBlock.STRAIN, strain).setValue(CropBlock.AGE, 7));
    }

    @GameTest(template = "empty", batch = "pollen")
    public static void partnersSideBySideSometimesCross(GameTestHelper h) {
        ProcessingConfig.overridePollenChance(1.0);
        var a = new BlockPos(1, 2, 1);
        var b = new BlockPos(2, 2, 1);
        plant(h, a, ModPower.HYDRO_BED.get(), Strain.MOSS_BOSS);
        plant(h, b, ModPower.HYDRO_BED.get(), Strain.CANOPY_CREEPER);
        var seed = Pollination.harvest(h.getBlockState(a), h.getLevel(), h.absolutePos(a), null);
        ProcessingConfig.overridePollenChance(0.0);
        var none = Pollination.harvest(h.getBlockState(a), h.getLevel(), h.absolutePos(a), null);
        ProcessingConfig.overridePollenChance(null);
        h.assertTrue(seed != null && ModDataComponents.strain(seed) == Strain.OLD_GROWTH, "they crossed: " + seed);
        h.assertTrue(none == null, "and it's only a chance");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void neighboursCantMakeUltrasAlone(GameTestHelper h) {
        ProcessingConfig.overridePollenChance(1.0);
        var a = new BlockPos(1, 2, 1);
        var b = new BlockPos(2, 2, 1);
        plant(h, a, ModPower.AERO_POD.get(), Strain.OLD_GROWTH);
        plant(h, b, ModPower.AERO_POD.get(), Strain.ORACLE_OF_SNACKS);
        var seed = Pollination.harvest(h.getBlockState(a), h.getLevel(), h.absolutePos(a), null);
        ProcessingConfig.overridePollenChance(null);
        h.assertTrue(seed == null, "an Ultra needs a Buzz Bot, got " + seed);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 400, batch = "bot_cross")
    public static void aBuzzBotMakesAnUltra(GameTestHelper h) {
        var a = new BlockPos(1, 2, 1);
        var b = new BlockPos(3, 2, 1);
        plant(h, a, ModPower.AERO_POD.get(), Strain.OLD_GROWTH);
        plant(h, b, ModPower.AERO_POD.get(), Strain.ORACLE_OF_SNACKS);
        var bot = h.spawn(ModEntities.BUZZ_BOT.get(), new BlockPos(2, 3, 2));
        bot.addBuds(1);
        h.succeedWhen(() -> {
            h.assertTrue(bot.cooldown() > 0, "it pollinated, and now it waits");
            BlockPos done = WeedCropBlockEntity.class.cast(h.getLevel().getBlockEntity(h.absolutePos(a))).pollen() != null ? a : b;
            var crop = (WeedCropBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(done));
            h.assertTrue(crop.pollen() != null && crop.pollen().strain() == Strain.THE_ELDER_BUD, "it left the Ultra's pollen");
            var seed = Pollination.harvest(h.getBlockState(done), h.getLevel(), h.absolutePos(done), crop);
            h.assertTrue(seed != null && ModDataComponents.strain(seed) == Strain.THE_ELDER_BUD, "the harvest gives the seed: " + seed);
            h.assertTrue(crop.pollen() == null, "once");
            bot.discard();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "bot_wander")
    public static void aBuzzBotStaysNearWeed(GameTestHelper h) {
        var plant = new BlockPos(3, 2, 3);
        plant(h, plant, ModPower.CLOSET_GROW.get(), Strain.LANDRACE);
        var bot = h.spawn(ModEntities.BUZZ_BOT.get(), new BlockPos(3, 4, 3));
        bot.addBuds(1);
        var start = bot.position();
        var at = h.absolutePos(plant).getCenter();
        boolean[] moved = {false};
        h.onEachTick(() -> {
            h.assertTrue(bot.position().distanceTo(at) <= BuzzBot.REACH + 3, "it strayed to " + bot.position());
            moved[0] |= bot.position().distanceTo(start) > 1;
        });
        h.runAfterDelay(250, () -> {
            h.assertTrue(moved[0], "it wandered");
            bot.discard();
            h.succeed();
        });
    }

    /** Stashes left by other tests stay in the world within a Bot's 16 blocks: none of them holds enough to wake this one. */
    @BeforeBatch(batch = "bot_sleep")
    public static void stashesTooLow(ServerLevel level) { ProcessingConfig.overrideBotCharge(Integer.MAX_VALUE); }

    @AfterBatch(batch = "bot_sleep")
    public static void stashesAsNormal(ServerLevel level) { ProcessingConfig.overrideBotCharge(null); }

    /** A Bot reaches 16 blocks, past the next test's structure, so each runs in a batch of its own. */
    @GameTest(template = "empty", timeoutTicks = 300, batch = "bot_sleep")
    public static void aBuzzBotOutOfPowerGoesHomeAndSleeps(GameTestHelper h) {
        var home = new BlockPos(1, 2, 1);
        var bot = h.spawn(ModEntities.BUZZ_BOT.get(), new BlockPos(4, 4, 4));
        bot.setHome(h.absolutePos(home));
        bot.setPower(2);
        h.succeedWhen(() -> {
            h.assertTrue(bot.dormant(), "it went to sleep");
            h.assertTrue(bot.blockPosition().equals(h.absolutePos(home)), "at home, not at " + bot.blockPosition());
            bot.addBuds(1);
            h.assertTrue(bot.power() == ProcessingConfig.botBudTicks(), "a bud is an hour");
            bot.discard();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300, batch = "bot_charge")
    public static void aBuzzBotRechargesAtAStash(GameTestHelper h) {
        var jar = new BlockPos(3, 2, 3);
        h.setBlock(jar, ModPower.STASH_JAR.get());
        var stash = (StashBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(jar));
        ((BIBuffer) stash.node()).set(10_000);
        var bot = h.spawn(ModEntities.BUZZ_BOT.get(), new BlockPos(1, 3, 1));
        bot.setPower(100);
        h.succeedWhen(() -> {
            h.assertTrue(stash.node().stored() == 10_000 - ProcessingConfig.botChargeBI(), "it took one bud's worth of BI, leaving " + stash.node().stored());
            h.assertTrue(bot.power() > ProcessingConfig.botBudTicks() / 2, "and has an hour's power");
            bot.discard();
        });
    }

    @GameTest(template = "empty")
    public static void takingAParentStartsOver(GameTestHelper h) {
        var pheno = chamber(h, ModPower.PHENO_CHAMBER.get());
        pheno.setItem(0, seed(Strain.BLUE_DREAM, Traits.DEFAULT));
        pheno.setItem(1, seed(Strain.BLUE_DREAM, Traits.DEFAULT));
        h.runAfterDelay(10, () -> {
            h.assertTrue(pheno.progress() > 0, "it started");
            pheno.removeItem(0, 1);
            h.assertTrue(pheno.progress() == 0, "and starts over");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void hybridsInspireTheirParentsColours(GameTestHelper h) {
        var colours = Infuser.colours(Strain.MOSS_BOSS);
        h.assertTrue(colours.containsAll(Infuser.colours(Strain.MOSSY_PHILOSOPHER)) && colours.containsAll(Infuser.colours(Strain.STICKY_ICKY)), "both parents' colours: " + colours);
        h.assertTrue(!Infuser.colours(Strain.GOLDEN_GOD).isEmpty(), "and so all the way up");
        h.succeed();
    }
}

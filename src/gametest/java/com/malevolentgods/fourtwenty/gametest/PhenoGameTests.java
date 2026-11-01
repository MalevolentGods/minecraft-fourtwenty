package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.infuser.Infuser;
import com.malevolentgods.fourtwenty.processing.PhenoBlockEntity;
import com.malevolentgods.fourtwenty.processing.ProcessingConfig;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
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
        pheno.setItem(0, seed(Strain.MOSS_BOSS, Traits.DEFAULT));
        pheno.setItem(1, seed(Strain.CANOPY_CREEPER, Traits.DEFAULT));
        h.runAfterDelay(10, () -> {
            h.assertTrue(pheno.getItem(PhenoBlockEntity.PARENTS).isEmpty() && pheno.progress() == 0, "a Pheno Chamber can't make a Double Cross");
            h.setBlock(AT, ModPower.PHENO_LAB.get());
            var lab = filled(h);
            lab.setItem(0, seed(Strain.MOSS_BOSS, Traits.DEFAULT));
            lab.setItem(1, seed(Strain.CANOPY_CREEPER, Traits.DEFAULT));
            h.succeedWhen(() -> {
                h.assertTrue(ModDataComponents.strain(lab.getItem(PhenoBlockEntity.PARENTS)) == Strain.OLD_GROWTH, "a Pheno Lab can");
                h.succeed();
            });
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

package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.item.WeedPipeItem;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.processing.ConstructionMenu;
import com.malevolentgods.fourtwenty.processing.RigBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class ExtractGameTests {
    private static final BlockPos AT = new BlockPos(1, 2, 1);

    private static ItemStack extract(int index) { return new ItemStack(ModPower.EXTRACTS.get(index).get()); }

    private static RigBlockEntity rig(GameTestHelper h) {
        h.setBlock(AT, ModPower.ROSIN_RIG.get());
        var rig = (RigBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(AT));
        ((BIBuffer) rig.node()).set(1_000_000);
        return rig;
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aRigTurnsBudIntoKiefAndKeepsTheBreed(GameTestHelper h) {
        var rig = rig(h);
        Traits traits = new Traits(4, 3, 4);
        rig.setItem(RigBlockEntity.INPUT, ModDataComponents.withTraits(ModDataComponents.withStrain(new ItemStack(ModItems.WEED_BUD.get()), Strain.BLUE_DREAM), traits));
        h.succeedWhen(() -> {
            ItemStack out = rig.getItem(RigBlockEntity.OUTPUTS);
            h.assertTrue(out.is(ModPower.EXTRACTS.get(0).get()), "made Kief, got " + out);
            h.assertTrue(ModDataComponents.strain(out) == Strain.BLUE_DREAM && ModDataComponents.traits(out).equals(traits), "keeping its strain and traits");
            h.assertTrue(rig.getItem(RigBlockEntity.INPUT).isEmpty(), "and the bud went in");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    public static void aBottleMakesOilAndHashTakesTwoKief(GameTestHelper h) {
        var rig = rig(h);
        rig.setItem(RigBlockEntity.INPUT, extract(2));
        rig.setItem(RigBlockEntity.BOTTLE, new ItemStack(Items.GLASS_BOTTLE));
        h.succeedWhen(() -> {
            h.assertTrue(rig.getItem(RigBlockEntity.OUTPUTS).is(ModPower.EXTRACTS.get(4).get()), "Resin and a bottle make Oil");
            h.assertTrue(rig.getItem(RigBlockEntity.BOTTLE).isEmpty(), "using the bottle");
        });
    }

    @GameTest(template = "empty")
    public static void oneKiefIsNotEnoughForHash(GameTestHelper h) {
        var rig = rig(h);
        rig.setItem(RigBlockEntity.INPUT, extract(0));
        h.runAfterDelay(30, () -> {
            h.assertTrue(rig.getItem(RigBlockEntity.INPUT).getCount() == 1 && rig.getItem(RigBlockEntity.OUTPUTS).isEmpty(), "it waits for a second");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void hashBurnsLongerInAHookah(GameTestHelper h) {
        h.setBlock(AT, ModBlocks.WEED_BONG.get());
        var hookah = (WeedBongBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(AT));
        hookah.setItem(WeedBongBlockEntity.WEED_SLOT, ModDataComponents.withStrain(extract(1), Strain.DUST_DEVIL));
        hookah.setItem(WeedBongBlockEntity.WATER_SLOT, new ItemStack(Items.WATER_BUCKET));
        hookah.setItem(WeedBongBlockEntity.FUEL_SLOT, new ItemStack(Items.CHARCOAL));
        hookah.lightBong(null);
        h.assertTrue(hookah.isLit() && hookah.burnDuration() == 9000, "Hash of a biome strain burns 7.5 minutes, got " + hookah.burnDuration());
        var saved = hookah.saveWithoutMetadata(h.getLevel().registryAccess());
        hookah.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(hookah.burnDuration() == 9000, "and keeps that through a save");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aPipeSmokesExtractsStronger(GameTestHelper h) {
        ItemStack pipe = new ItemStack(ModItems.WEED_PIPE.get());
        h.assertTrue(WeedPipeItem.fill(pipe, extract(1)) == 1, "Hash loads into a pipe");
        var load = pipe.get(ModDataComponents.PIPE_LOAD.get());
        h.assertTrue(load != null && load.traits().equals(new Traits(3, 2, 3)), "and smokes one up, got " + load);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void theConstructionJointDoublesBuildingBlocks(GameTestHelper h) {
        ItemStack road = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("fourtwenty", "paved_road")), 3);
        h.assertTrue(ConstructionMenu.doubled(road).getCount() == 6, "roads come in twos: " + ConstructionMenu.doubled(road));
        ItemStack hookah = new ItemStack(ModItems.WEED_BONG.get());
        h.assertTrue(ConstructionMenu.doubled(hookah).getCount() == 1, "machines don't");
        h.succeed();
    }
}

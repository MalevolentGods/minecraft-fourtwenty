package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import java.util.Collection;
import java.util.List;

@GameTestHolder("fourtwenty_tests")
public final class PersistenceGameTests {
    // Deliberately outside the random GameTest structure area; never used in a player world.
    private static final BlockPos CHECKPOINT = new BlockPos(1000, 100, 1000);

    @GameTestGenerator
    public static Collection<TestFunction> persistenceCheckpoint() {
        String stage = System.getProperty("fourtwenty.persistenceStage", "");
        if (stage.isEmpty()) return List.of();
        if (!stage.equals("write") && !stage.equals("read")) throw new IllegalArgumentException("persistenceStage must be write or read");
        return List.of(new TestFunction("persistence", "paid_burn_disk_" + stage, "fourtwenty_tests:empty", 100, 0, true,
            h -> checkpoint(h, stage)));
    }

    private static void checkpoint(GameTestHelper h, String stage) {
        var level = h.getLevel();
        level.getChunkAt(CHECKPOINT);
        if (stage.equals("write")) {
            h.assertTrue(!level.getBlockState(CHECKPOINT).is(ModBlocks.WEED_BONG.get()), "Write stage requires a fresh checkpoint");
            level.setBlockAndUpdate(CHECKPOINT, ModBlocks.WEED_BONG.get().defaultBlockState());
            var bong = (WeedBongBlockEntity) level.getBlockEntity(CHECKPOINT);
            bong.setItem(0, new ItemStack(ModItems.WEED_BUD.get(), 3));
            bong.setItem(1, new ItemStack(Items.WATER_BUCKET));
            bong.setItem(2, new ItemStack(Items.CHARCOAL, 5));
            for (int i = 0; i < 37; i++) WeedBongBlockEntity.tick(level, CHECKPOINT, bong.getBlockState(), bong);
            level.getChunkSource().save(true);
        } else {
            h.assertTrue(level.getBlockEntity(CHECKPOINT) instanceof WeedBongBlockEntity, "Saved bong exists after process restart");
            var bong = (WeedBongBlockEntity) level.getBlockEntity(CHECKPOINT);
            h.assertTrue(bong.isLit() && bong.burnTime() == 37 && bong.getItem(0).getCount() == 2 && bong.getItem(2).getCount() == 4,
                "Disk checkpoint preserves paid charge and pauses unloaded time");
            WeedBongBlockEntity.tick(level, CHECKPOINT, bong.getBlockState(), bong);
            h.assertTrue(bong.burnTime() == 38 && bong.getItem(0).getCount() == 2 && bong.getItem(2).getCount() == 4,
                "Loaded charge resumes without double payment");
        }
        h.succeed();
    }
}

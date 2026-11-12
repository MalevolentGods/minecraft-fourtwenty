package com.malevolentgods.fourtwenty.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class RackOptionalGameTests {
    @GameTest(template="empty")
    public static void vanillaLaddersStillClimbWithoutMinecolonies(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 1, 2));
        h.getLevel().setBlockAndUpdate(pos, Blocks.LADDER.defaultBlockState());
        var player = FakePlayerFactory.getMinecraft(h.getLevel());
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        var state = h.getLevel().getBlockState(pos);
        h.assertTrue(state.is(BlockTags.CLIMBABLE), "The vanilla climbable tag loads without MineColonies");
        h.assertTrue(state.isLadder(h.getLevel(), pos, player), "Vanilla ladders still work without MineColonies");
        h.assertTrue(player.onClimbable(), "A player can still climb a vanilla ladder without MineColonies");
        h.succeed();
    }
}

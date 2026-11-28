package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.tiedye.ModTieDye;
import com.malevolentgods.fourtwenty.tiedye.TieDye;
import com.malevolentgods.fourtwenty.tiedye.TieDyeBlockEntity;
import com.malevolentgods.fourtwenty.tiedye.TieDyeBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Tie-dye's core (specs/018-tie-dye-shack FR-013, SC-003): a tie-dyed block keeps its pattern and colourway through place, mine and place. */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class TieDyeGameTests {
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void tieDyedHempKeepsItsLook(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var look = new TieDye(3, "terrapin");
        ItemStack stack = ModTieDye.stack(ModTieDye.HEMP_ITEM.get(), look, 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos floor = new BlockPos(2, 1, 2);
        h.setBlock(floor, Blocks.STONE);
        var abs = h.absolutePos(floor);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false)));
        var at = abs.above();
        var state = h.getLevel().getBlockState(at);
        h.assertTrue(state.is(ModTieDye.HEMP.get()) && state.getValue(TieDyeBlocks.PATTERN) == 3, "placed in its pattern: " + state);
        h.assertTrue(h.getLevel().getBlockEntity(at) instanceof TieDyeBlockEntity be && "terrapin".equals(be.colourway()), "with its colourway");
        var drops = Block.getDrops(state, h.getLevel(), at, h.getLevel().getBlockEntity(at));
        h.assertTrue(drops.size() == 1 && look.equals(drops.getFirst().get(ModDataComponents.TIE_DYE.get())), "mined, it drops its look: " + drops);
        var saved = h.getLevel().getBlockEntity(at).saveWithFullMetadata(h.getLevel().registryAccess());
        h.assertTrue("terrapin".equals(saved.getString("colourway")), "and saves it");
        h.succeed();
    }
}

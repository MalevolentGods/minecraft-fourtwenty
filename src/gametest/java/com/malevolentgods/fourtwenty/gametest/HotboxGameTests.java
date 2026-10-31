package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.HotboxBlock;
import com.malevolentgods.fourtwenty.domain.HotboxRules;
import com.malevolentgods.fourtwenty.blockentity.HotboxBlockEntity;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class HotboxGameTests {
    /** A game test forces its own chunks, so look at the Hotbox's tickets: re-adding one fails if it's held, removing one fails if it's gone. */
    private static boolean held(ServerLevel level, BlockPos pos) {
        var c = new ChunkPos(pos);
        return !HotboxBlockEntity.TICKETS.forceChunk(level, pos, c.x, c.z, true, true)
                && !HotboxBlockEntity.TICKETS.forceChunk(level, pos, c.x + 1, c.z + 1, true, true)
                && !HotboxBlockEntity.TICKETS.forceChunk(level, pos, c.x - 1, c.z - 1, true, true);
    }

    private static boolean released(ServerLevel level, BlockPos pos) {
        var c = new ChunkPos(pos);
        return !HotboxBlockEntity.TICKETS.forceChunk(level, pos, c.x, c.z, false, true)
                && !HotboxBlockEntity.TICKETS.forceChunk(level, pos, c.x + 1, c.z + 1, false, true);
    }

    /** A player arrives, it holds; they walk off, it counts down and keeps holding; the server empties, it lets go; breaking it clears the tickets. */
    @GameTest(template = "empty", batch = "hotbox", timeoutTicks = 400)
    public static void holdsForAPlayerAndLetsGoWhenTheServerEmpties(GameTestHelper h) {
        var level = h.getLevel();
        var rel = new BlockPos(1, 1, 1);
        var pos = h.absolutePos(rel);
        var player = h.makeMockServerPlayerInLevel();
        player.moveTo(pos.getX(), pos.getY() + 1, pos.getZ());
        h.setBlock(rel, ModBlocks.HOTBOX.get());
        h.runAfterDelay(50, () -> {
            h.assertTrue(level.getBlockState(pos).getValue(HotboxBlock.LIT), "Lit with a player in the area");
            h.assertTrue(held(level, pos), "Its 3x3 chunks are held");
            var box = (HotboxBlockEntity) level.getBlockEntity(pos);
            h.assertTrue(box.ticksLeft() == HotboxRules.HOLD_TICKS, "Full while a player is near");
            player.moveTo(pos.getX() + 500, pos.getY() + 1, pos.getZ() + 500);
        });
        h.runAfterDelay(120, () -> {
            var box = (HotboxBlockEntity) level.getBlockEntity(pos);
            h.assertTrue(box.ticksLeft() < HotboxRules.HOLD_TICKS && box.ticksLeft() > 0, "Counting down with the player away");
            h.assertTrue(level.getBlockState(pos).getValue(HotboxBlock.LIT), "Still holding");
            h.assertTrue(held(level, pos), "Still held");
            level.getServer().getPlayerList().remove(player);
        });
        h.runAfterDelay(190, () -> {
            h.assertTrue(!level.getBlockState(pos).getValue(HotboxBlock.LIT), "Lets go with nobody online");
            h.assertTrue(released(level, pos), "Tickets gone with nobody online");
            h.setBlock(rel, Blocks.AIR);
            h.succeed();
        });
    }

    @GameTest(template = "empty", batch = "hotbox_break", timeoutTicks = 200)
    public static void breakingItReleasesItsChunks(GameTestHelper h) {
        var level = h.getLevel();
        var rel = new BlockPos(1, 1, 1);
        var pos = h.absolutePos(rel);
        var player = h.makeMockServerPlayerInLevel();
        player.moveTo(pos.getX(), pos.getY() + 1, pos.getZ());
        h.setBlock(rel, ModBlocks.HOTBOX.get());
        h.runAfterDelay(50, () -> {
            h.assertTrue(held(level, pos), "Held before it breaks");
            h.setBlock(rel, Blocks.AIR);
            h.assertTrue(released(level, pos), "Released the moment it breaks");
            level.getServer().getPlayerList().remove(player);
            h.succeed();
        });
    }
}

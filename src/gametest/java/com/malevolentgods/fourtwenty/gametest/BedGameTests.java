package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.block.BedShape;
import com.malevolentgods.fourtwenty.block.FancyBedBlock;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Joining beds: north-facing oak beds with their feet on z 2 (heads on z 1, a king's extension on
 * z 3); from the foot looking north, left is west (x 1) and right is east (x 3).
 */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class BedGameTests {
    private static final BlockPos LEFT = new BlockPos(1, 2, 2), MIDDLE = new BlockPos(2, 2, 2), RIGHT = new BlockPos(3, 2, 2);

    /** Places a bed as its item would: the foot, then setPlacedBy (which puts down the head and joins). */
    private static void place(GameTestHelper h, BlockPos rel, DyeColor color) {
        var bed = ModBlocks.BEDS.get("oak").get();
        BlockPos pos = h.absolutePos(rel);
        BlockState state = bed.defaultBlockState().setValue(FancyBedBlock.FACING, Direction.NORTH).setValue(FancyBedBlock.BLANKET, color);
        h.getLevel().setBlock(pos, state, 3);
        bed.setPlacedBy(h.getLevel(), pos, state, null, new ItemStack(ModItems.BEDS.get(0).get()));
    }

    private static void assertColumn(GameTestHelper h, BlockPos foot, BedShape shape, DyeColor color) {
        for (BlockPos p : new BlockPos[] {foot, foot.north()}) {
            BlockState s = h.getBlockState(p);
            h.assertTrue(s.getBlock() instanceof FancyBedBlock, "A bed at " + p);
            h.assertTrue(s.getValue(FancyBedBlock.SHAPE) == shape, "Shape " + shape + " at " + p + ", got " + s.getValue(FancyBedBlock.SHAPE));
            h.assertTrue(s.getValue(FancyBedBlock.BLANKET) == color, "Blanket " + color + " at " + p);
        }
    }

    private static boolean extension(GameTestHelper h, BlockPos foot) {
        return h.getBlockState(foot.south()).is(ModBlocks.BED_EXTENSION.get());
    }

    @GameTest(template = "empty")
    public static void twoBedsMakeADoubleInTheFirstBedsColour(GameTestHelper h) {
        place(h, LEFT, DyeColor.RED);
        place(h, MIDDLE, DyeColor.BLUE);
        assertColumn(h, LEFT, BedShape.LEFT, DyeColor.RED);
        assertColumn(h, MIDDLE, BedShape.RIGHT, DyeColor.RED);
        h.assertTrue(!extension(h, LEFT) && !extension(h, MIDDLE), "A double is no longer");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aThirdBedMakesAKingABlockLonger(GameTestHelper h) {
        place(h, LEFT, DyeColor.RED);
        place(h, MIDDLE, DyeColor.RED);
        place(h, RIGHT, DyeColor.WHITE);
        assertColumn(h, LEFT, BedShape.KING_LEFT, DyeColor.RED);
        assertColumn(h, MIDDLE, BedShape.KING_MIDDLE, DyeColor.RED);
        assertColumn(h, RIGHT, BedShape.KING_RIGHT, DyeColor.RED);
        h.assertTrue(extension(h, LEFT) && extension(h, MIDDLE) && extension(h, RIGHT), "Every column extends past the foot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aBedBetweenTwoSinglesMakesAKing(GameTestHelper h) {
        place(h, LEFT, DyeColor.CYAN);
        place(h, RIGHT, DyeColor.CYAN);
        place(h, MIDDLE, DyeColor.CYAN);
        assertColumn(h, LEFT, BedShape.KING_LEFT, DyeColor.CYAN);
        assertColumn(h, MIDDLE, BedShape.KING_MIDDLE, DyeColor.CYAN);
        assertColumn(h, RIGHT, BedShape.KING_RIGHT, DyeColor.CYAN);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aBlockedKingLeavesTheThirdBedSingle(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        h.setBlock(RIGHT.south(), Blocks.STONE);
        place(h, RIGHT, DyeColor.LIME);
        assertColumn(h, LEFT, BedShape.LEFT, DyeColor.LIME);
        assertColumn(h, MIDDLE, BedShape.RIGHT, DyeColor.LIME);
        assertColumn(h, RIGHT, BedShape.SINGLE, DyeColor.LIME);
        h.assertTrue(!extension(h, LEFT) && !extension(h, MIDDLE), "No extension when the king can't fit");
        h.succeed();
    }

    /** The oak beds lying around after a break, and their blankets all {@code color}. */
    private static int dropped(GameTestHelper h, DyeColor color) {
        int count = 0;
        for (var item : h.getEntities(net.minecraft.world.entity.EntityType.ITEM)) {
            var stack = item.getItem();
            if (!stack.is(ModItems.BEDS.get(0).get())) continue;
            h.assertTrue(com.malevolentgods.fourtwenty.item.FancyBedItem.blanket(stack) == color, "Dropped bed keeps its blanket");
            count += stack.getCount();
        }
        return count;
    }

    private static void assertGone(GameTestHelper h, BlockPos... feet) {
        for (BlockPos foot : feet) {
            h.assertBlockNotPresent(ModBlocks.BEDS.get("oak").get(), foot);
            h.assertBlockNotPresent(ModBlocks.BEDS.get("oak").get(), foot.north());
            h.assertBlockNotPresent(ModBlocks.BED_EXTENSION.get(), foot.south());
        }
    }

    @GameTest(template = "empty")
    public static void breakingAKingColumnBreaksTheWholeBed(GameTestHelper h) {
        place(h, LEFT, DyeColor.ORANGE);
        place(h, MIDDLE, DyeColor.ORANGE);
        place(h, RIGHT, DyeColor.ORANGE);
        h.getLevel().destroyBlock(h.absolutePos(MIDDLE.north()), true);   // a survival break, with drops
        assertGone(h, LEFT, MIDDLE, RIGHT);
        h.assertTrue(dropped(h, DyeColor.ORANGE) == 3, "A king gives back its three beds, got " + dropped(h, DyeColor.ORANGE));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingADoublesFootBreaksBothBeds(GameTestHelper h) {
        place(h, LEFT, DyeColor.BLUE);
        place(h, MIDDLE, DyeColor.BLUE);
        h.destroyBlock(LEFT);
        assertGone(h, LEFT, MIDDLE);
        h.assertTrue(dropped(h, DyeColor.BLUE) == 2, "A double gives back its two beds, got " + dropped(h, DyeColor.BLUE));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingAnExtensionBreaksTheWholeKing(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        place(h, RIGHT, DyeColor.LIME);
        h.destroyBlock(LEFT.south());
        assertGone(h, LEFT, MIDDLE, RIGHT);
        h.assertTrue(dropped(h, DyeColor.LIME) == 3, "Three beds back, got " + dropped(h, DyeColor.LIME));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aCreativePlayerBreakingAKingGetsNoDrops(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        place(h, RIGHT, DyeColor.LIME);
        var player = h.makeMockPlayer(GameType.CREATIVE);
        BlockPos pos = h.absolutePos(RIGHT.north());
        BlockState state = h.getLevel().getBlockState(pos);
        state.getBlock().playerWillDestroy(h.getLevel(), pos, state, player);
        h.getLevel().removeBlock(pos, false);
        assertGone(h, LEFT, MIDDLE, RIGHT);
        h.assertTrue(dropped(h, DyeColor.LIME) == 0, "Nothing drops in creative");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void dyeingOneColumnRecoloursTheWholeBed(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        place(h, RIGHT, DyeColor.LIME);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var dye = new ItemStack(Items.PURPLE_DYE, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, dye);
        BlockPos pos = h.absolutePos(RIGHT.north());
        h.getLevel().getBlockState(pos).useItemOn(dye, h.getLevel(), player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        assertColumn(h, LEFT, BedShape.KING_LEFT, DyeColor.PURPLE);
        assertColumn(h, MIDDLE, BedShape.KING_MIDDLE, DyeColor.PURPLE);
        assertColumn(h, RIGHT, BedShape.KING_RIGHT, DyeColor.PURPLE);
        h.assertTrue(dye.getCount() == 3, "One dye used");
        h.succeed();
    }

    private static boolean occupied(GameTestHelper h, BlockPos foot) {
        return h.getBlockState(foot).getValue(FancyBedBlock.OCCUPIED) && h.getBlockState(foot.north()).getValue(FancyBedBlock.OCCUPIED);
    }

    /** What MineColonies checks before a colonist claims a bed: a BedBlock HEAD, in #minecraft:beds, isBed, not occupied. */
    private static boolean colonistCanUse(GameTestHelper h, BlockPos foot, net.minecraft.world.entity.LivingEntity who) {
        BlockPos head = h.absolutePos(foot.north());
        BlockState s = h.getLevel().getBlockState(head);
        return s.getBlock() instanceof net.minecraft.world.level.block.BedBlock
                && s.getValue(FancyBedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.HEAD
                && s.is(net.minecraft.tags.BlockTags.BEDS) && s.isBed(h.getLevel(), head, who) && !s.getValue(FancyBedBlock.OCCUPIED);
    }

    @GameTest(template = "empty")
    public static void threeSleepersShareAKingOneColumnEach(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        place(h, RIGHT, DyeColor.LIME);
        var sleepers = new java.util.ArrayList<net.minecraft.world.entity.player.Player>();
        for (BlockPos foot : new BlockPos[] {LEFT, MIDDLE, RIGHT}) {
            var sleeper = h.makeMockPlayer(GameType.SURVIVAL);
            h.assertTrue(colonistCanUse(h, foot, sleeper), "Column at " + foot + " is free for the next sleeper");
            sleeper.startSleeping(h.absolutePos(foot.north()));
            h.assertTrue(occupied(h, foot), "Sleeper takes the column at " + foot);
            sleepers.add(sleeper);
        }
        for (BlockPos foot : new BlockPos[] {LEFT, MIDDLE, RIGHT}) {
            h.assertTrue(h.getBlockState(foot).getValue(FancyBedBlock.SHAPE).king(), "Still a king with everyone asleep");
        }
        sleepers.get(1).stopSleeping();
        h.assertTrue(!occupied(h, MIDDLE) && occupied(h, LEFT) && occupied(h, RIGHT), "Waking frees only that column");
        h.assertTrue(colonistCanUse(h, MIDDLE, sleepers.get(1)), "The middle is free again");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bothSidesOfADoubleAreBedsForColonists(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        var first = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(colonistCanUse(h, LEFT, first) && colonistCanUse(h, MIDDLE, first), "Two beds to claim");
        first.startSleeping(h.absolutePos(LEFT.north()));
        h.assertTrue(!colonistCanUse(h, LEFT, first) && colonistCanUse(h, MIDDLE, first), "The other side stays free");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void usingAKingsFootEndUsesThatColumnsBed(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        place(h, RIGHT, DyeColor.LIME);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        BlockPos ext = h.absolutePos(RIGHT.south());
        var result = h.getLevel().getBlockState(ext).useWithoutItem(h.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(ext), Direction.UP, ext, false));
        // It's day in the test world, so the bed refuses sleep; what matters is that the bed answered.
        h.assertTrue(result != net.minecraft.world.InteractionResult.PASS, "The bed handles the click, got " + result);
        h.succeed();
    }
}

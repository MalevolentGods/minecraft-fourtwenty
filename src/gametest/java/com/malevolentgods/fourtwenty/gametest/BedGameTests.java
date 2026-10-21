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

    @GameTest(template = "empty")
    public static void breakingAKingColumnSplitsItIntoSingles(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        place(h, RIGHT, DyeColor.LIME);
        h.destroyBlock(MIDDLE.north());
        h.assertBlockNotPresent(ModBlocks.BEDS.get("oak").get(), MIDDLE);
        assertColumn(h, LEFT, BedShape.SINGLE, DyeColor.LIME);
        assertColumn(h, RIGHT, BedShape.SINGLE, DyeColor.LIME);
        h.assertTrue(!extension(h, LEFT) && !extension(h, MIDDLE) && !extension(h, RIGHT), "The extensions go with the king");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void breakingAnExtensionSplitsTheKingButKeepsTheBeds(GameTestHelper h) {
        place(h, LEFT, DyeColor.LIME);
        place(h, MIDDLE, DyeColor.LIME);
        place(h, RIGHT, DyeColor.LIME);
        h.destroyBlock(LEFT.south());
        for (BlockPos foot : new BlockPos[] {LEFT, MIDDLE, RIGHT}) assertColumn(h, foot, BedShape.SINGLE, DyeColor.LIME);
        h.assertTrue(!extension(h, MIDDLE) && !extension(h, RIGHT), "The other extensions go too");
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
}

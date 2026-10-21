package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.domain.Terpenes.Base;
import com.malevolentgods.fourtwenty.domain.Terpenes.Code;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Collections;
import java.util.List;

import static com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity.*;

@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class TerpinatorGameTests {
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private static TerpinatorBlockEntity place(GameTestHelper h) {
        h.setBlock(POS, ModTerpenes.TERPINATOR.get());
        var t = (TerpinatorBlockEntity) h.getBlockEntity(POS);
        for (int stage = 0; stage < 3; stage++) t.toggle(stage);   // every stage on
        return t;
    }

    private static void run(GameTestHelper h, TerpinatorBlockEntity t, int ticks) {
        for (int i = 0; i < ticks; i++) TerpinatorBlockEntity.tick(h.getLevel(), t.getBlockPos(), h.getLevel().getBlockState(t.getBlockPos()), t);
    }

    private static ItemStack terp(Code code, int count) { return new ItemStack(ModTerpenes.terpene(code), count); }

    @GameTest(template = "empty")
    public static void budsPressIntoResinThatRefinesIntoTheDialledBase(GameTestHelper h) {
        var t = place(h);
        t.toggle(STAGE_REFINE);
        t.setMethod(Base.LIM.ordinal());
        t.toggle(STAGE_REFINE);
        t.setItem(BUDS, new ItemStack(ModItems.WEED_BUD.get(), 4));
        run(h, t, PRESS_TIME - 1);
        h.assertTrue(t.getItem(RESIN).isEmpty(), "Pressing takes " + PRESS_TIME + " ticks");
        run(h, t, 1);
        h.assertTrue(t.getItem(RESIN).getCount() == 1 && t.getItem(BUDS).getCount() == 2, "Two buds make one resin");
        run(h, t, PRESS_TIME + 2 * REFINE_TIME);
        h.assertTrue(t.getItem(BUDS).isEmpty() && t.getItem(RESIN).isEmpty(), "Everything is used");
        h.assertTrue(t.getItem(BASE_OUT).is(ModTerpenes.terpene(Code.of(Base.LIM))) && t.getItem(BASE_OUT).getCount() == 4,
                "Each resin refines into two Limonade, got " + t.getItem(BASE_OUT));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void twoBasesBlendAnAccordAndABaseFinishesIt(GameTestHelper h) {
        var t = place(h);
        t.setItem(PORT1, terp(Code.of(Base.MYR), 1));
        t.setItem(PORT2, terp(Code.of(Base.PIN), 1));
        run(h, t, BLEND_TIME);
        h.assertTrue(t.getItem(BLEND_OUT).is(ModTerpenes.terpene(Code.of(Base.MYR, Base.PIN))), "Myr then Pin make the Spacebar Static accord");
        h.assertTrue(t.getItem(PORT1).isEmpty() && t.getItem(PORT2).isEmpty(), "Both bases used");
        t.setItem(PORT1, t.removeItemNoUpdate(BLEND_OUT));
        t.setItem(PORT3, terp(Code.of(Base.PIN), 1));
        run(h, t, BLEND_TIME);
        h.assertTrue(t.getItem(BLEND_OUT).is(ModTerpenes.terpene(Code.of(Base.MYR, Base.PIN, Base.PIN))), "The accord + Pin finish Touch of Grey");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void threeBasesMakeNothing(GameTestHelper h) {
        var t = place(h);
        t.setItem(PORT1, terp(Code.of(Base.MYR), 1));
        t.setItem(PORT2, terp(Code.of(Base.PIN), 1));
        t.setItem(PORT3, terp(Code.of(Base.LIM), 1));
        run(h, t, BLEND_TIME * 3);
        h.assertTrue(t.getItem(BLEND_OUT).isEmpty() && t.getItem(PORT1).getCount() == 1, "The accord comes first; nothing is used");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void changingThePortsRestartsTheBlend(GameTestHelper h) {
        var t = place(h);
        t.setItem(PORT1, terp(Code.of(Base.LAV), 2));
        t.setItem(PORT2, terp(Code.of(Base.LIM), 1));
        run(h, t, BLEND_TIME - 5);
        t.setItem(PORT2, terp(Code.of(Base.PIN), 1));
        run(h, t, 10);
        h.assertTrue(t.getItem(BLEND_OUT).isEmpty(), "A new blend starts from zero");
        run(h, t, BLEND_TIME);
        h.assertTrue(t.getItem(BLEND_OUT).is(ModTerpenes.terpene(Code.of(Base.LAV, Base.PIN))), "Blue Dream, from the new ports");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void portsOnlyTakeWhatTheyCanBlend(GameTestHelper h) {
        Code base = Code.of(Base.MYR), accord = Code.of(Base.MYR, Base.LIM), fin = Code.of(Base.MYR, Base.LIM, Base.LIM);
        h.assertTrue(accepts(PORT1, terp(base, 1)) && accepts(PORT1, terp(accord, 1)) && !accepts(PORT1, terp(fin, 1)), "Port 1: a base or an accord");
        h.assertTrue(accepts(PORT3, terp(base, 1)) && !accepts(PORT3, terp(accord, 1)), "Port 3: a base");
        h.assertTrue(!accepts(BLEND_OUT, terp(base, 1)) && accepts(BUDS, new ItemStack(ModItems.WEED_BUD.get())), "Outputs take nothing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void hempRecipes(GameTestHelper h) {
        var level = h.getLevel();
        var bud = new ItemStack(ModItems.WEED_BUD.get());
        var natural = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(2, 2, List.of(bud, bud, bud, bud)), level);
        h.assertTrue(natural.isPresent(), "4 buds make hemp");
        var made = natural.get().value().assemble(CraftingInput.of(2, 2, List.of(bud, bud, bud, bud)), level.registryAccess());
        h.assertTrue(made.is(ModTerpenes.NATURAL_HEMP.items().get(0).get()) && made.getCount() == 2, "... 2 Natural Hemp, got " + made);
        Code grey = Code.of(Base.MYR, Base.PIN, Base.PIN);
        var hemp = new ItemStack(ModTerpenes.HEMP.get(Terpenes.Code.of(Base.LAV, Base.PIN, Base.LIM).id()).items().get(0).get());
        List<ItemStack> grid = new java.util.ArrayList<>(Collections.nCopies(9, hemp));
        grid.set(4, terp(grey, 1));
        var input = CraftingInput.of(3, 3, grid);
        var dye = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
        h.assertTrue(dye.isPresent(), "A terpene recolours 8 hemp of any colour");
        var dyed = dye.get().value().assemble(input, level.registryAccess());
        h.assertTrue(dyed.is(ModTerpenes.HEMP.get(grey.id()).items().get(0).get()) && dyed.getCount() == 8, "... 8 Touch of Grey Hemp, got " + dyed);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aStoppedStageWaitsAndKeepsItsProgress(GameTestHelper h) {
        var t = place(h);
        t.setItem(BUDS, new ItemStack(ModItems.WEED_BUD.get(), 2));
        run(h, t, PRESS_TIME / 2);
        t.toggle(STAGE_PRESS);
        run(h, t, PRESS_TIME * 2);
        h.assertTrue(t.getItem(RESIN).isEmpty() && t.getItem(BUDS).getCount() == 2, "Nothing happens while stopped");
        t.toggle(STAGE_PRESS);
        run(h, t, PRESS_TIME / 2);
        h.assertTrue(t.getItem(RESIN).getCount() == 1, "It picks up where it stopped");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void refiningOnlyStartsWithAnEmptyOutputAndLocksTheDial(GameTestHelper h) {
        var t = place(h);
        t.toggle(STAGE_REFINE);
        t.setItem(BASE_OUT, terp(Code.of(Base.PIN), 3));
        h.assertTrue(!t.toggle(STAGE_REFINE) && !t.isRunning(STAGE_REFINE), "A full output refuses to start");
        t.setItem(BASE_OUT, ItemStack.EMPTY);
        h.assertTrue(t.toggle(STAGE_REFINE) && t.isRunning(STAGE_REFINE), "An empty one starts");
        int before = t.data().get(DATA_METHOD);
        t.setMethod((before + 1) % 4);
        h.assertTrue(t.data().get(DATA_METHOD) == before, "The dial is locked while refining");
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, int w, int hgt, List<ItemStack> grid) {
        var input = CraftingInput.of(w, hgt, grid);
        return h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(r -> r.value().assemble(input, h.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
    }

    @GameTest(template = "empty")
    public static void rugsConcreteAndLampsCraftInEveryColour(GameTestHelper h) {
        Code teal = Code.of(Base.PIN, Base.LAV, Base.PIN);   // Hippie Hot Tub
        var hemp = new ItemStack(ModTerpenes.HEMP.get(teal.id()).items().get(0).get());
        var rugs = craft(h, 2, 1, List.of(hemp, hemp));
        h.assertTrue(rugs.is(ModTerpenes.RUGS.get(teal.id()).get().asItem()) && rugs.getCount() == 3, "2 hemp make 3 rugs, got " + rugs);
        var natural = new ItemStack(ModTerpenes.NATURAL_HEMP.items().get(0).get());
        h.assertTrue(craft(h, 2, 1, List.of(natural, natural)).is(ModTerpenes.RUGS.get("natural").get().asItem()), "Natural Hemp rugs");
        Code grey = Code.of(Base.MYR, Base.PIN, Base.PIN);
        List<ItemStack> grid = new java.util.ArrayList<>(Collections.nCopies(9, new ItemStack(rugs.getItem())));
        grid.set(4, terp(grey, 1));
        var recoloured = craft(h, 3, 3, grid);
        h.assertTrue(recoloured.is(ModTerpenes.RUGS.get(grey.id()).get().asItem()) && recoloured.getCount() == 8, "A terpene recolours 8 rugs, got " + recoloured);
        var sand = new ItemStack(net.minecraft.world.item.Items.SAND);
        var gravel = new ItemStack(net.minecraft.world.item.Items.GRAVEL);
        var bud = new ItemStack(ModItems.WEED_BUD.get());
        var powder = craft(h, 3, 3, List.of(sand, sand, sand, gravel, gravel, gravel, bud, bud, terp(teal, 1)));
        h.assertTrue(powder.is(ModTerpenes.CONCRETE_POWDER.get(teal.id()).get().asItem()) && powder.getCount() == 4,
                "3 sand, 3 gravel, 2 buds and a terpene make 4 powder, got " + powder);
        grid = new java.util.ArrayList<>(Collections.nCopies(9, new ItemStack(net.minecraft.world.item.Items.GLASS)));
        grid.set(4, terp(teal, 1));
        var glass = craft(h, 3, 3, grid);
        h.assertTrue(glass.is(ModTerpenes.GLASS.get(teal.id()).get().asItem()) && glass.getCount() == 8, "8 glass + a terpene, got " + glass);
        var panes = craft(h, 3, 2, Collections.nCopies(6, new ItemStack(glass.getItem())));
        h.assertTrue(panes.is(ModTerpenes.GLASS_PANES.get(teal.id()).get().asItem()) && panes.getCount() == 16, "6 glass make 16 panes, got " + panes);
        var plain = new ItemStack(net.minecraft.world.item.Items.GLASS);
        var lamps = craft(h, 3, 2, List.of(plain, plain, plain, new ItemStack(ModItems.WEED_BUD.get()), terp(teal, 1), ItemStack.EMPTY));
        h.assertTrue(lamps.is(ModTerpenes.LAMPS.get(teal.id()).get().asItem()) && lamps.getCount() == 4, "3 glass, a bud and a terpene make 4 lamps, got " + lamps);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aRugLetsTheFloorShowAndNeedsSupport(GameTestHelper h) {
        var rug = ModTerpenes.RUGS.get("natural").get().defaultBlockState();
        h.assertTrue(!rug.canOcclude(), "The floor's top face still draws round the inset rug");
        h.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);
        h.assertTrue(rug.canSurvive(h.getLevel(), h.absolutePos(new BlockPos(1, 3, 1))), "A rug sits on a block");
        h.assertTrue(!rug.canSurvive(h.getLevel(), h.absolutePos(new BlockPos(3, 3, 3))), "...and not on air");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void powderSetsIntoItsConcreteInWater(GameTestHelper h) {
        String code = Code.of(Base.LAV, Base.PIN, Base.PIN).id();
        h.setBlock(new BlockPos(2, 1, 2), Blocks.STONE);
        h.setBlock(new BlockPos(2, 2, 2), ModTerpenes.CONCRETE_POWDER.get(code).get());
        h.setBlock(new BlockPos(3, 2, 2), Blocks.WATER);
        h.succeedWhen(() -> h.assertBlockPresent(ModTerpenes.CONCRETE.get(code).get(), new BlockPos(2, 2, 2)));
    }

    /** Places a rug as a player would (state for placement, then onPlace). */
    private static void rug(GameTestHelper h, BlockPos rel, net.minecraft.world.level.block.Block block) {
        BlockPos pos = h.absolutePos(rel);
        var rugBlock = (com.malevolentgods.fourtwenty.block.RugBlock) block;
        h.getLevel().setBlock(pos, rugBlock.connected(h.getLevel(), pos, block.defaultBlockState()), 3);
    }

    @GameTest(template = "empty")
    public static void rugsJoinTheSameRugOnEverySideAndDiagonal(GameTestHelper h) {
        var teal = ModTerpenes.RUGS.get(Code.of(Base.PIN, Base.LAV, Base.PIN).id()).get();
        var grey = ModTerpenes.RUGS.get(Code.of(Base.MYR, Base.PIN, Base.PIN).id()).get();
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        BlockPos centre = new BlockPos(2, 2, 2);
        rug(h, centre, teal);
        rug(h, centre.east(), teal);                 // a side neighbour
        rug(h, centre.north().west(), teal);         // a diagonal one (Minecraft doesn't tell the centre)
        rug(h, centre.south(), grey);                // another colour: a separate rug
        var state = h.getBlockState(centre);
        h.assertTrue(state.getValue(com.malevolentgods.fourtwenty.block.RugBlock.EAST), "Joined to the rug beside it");
        h.assertTrue(state.getValue(com.malevolentgods.fourtwenty.block.RugBlock.NORTH_WEST), "Told about the diagonal rug");
        h.assertTrue(!state.getValue(com.malevolentgods.fourtwenty.block.RugBlock.SOUTH), "Another colour stays separate");
        h.assertTrue(h.getBlockState(centre.east()).getValue(com.malevolentgods.fourtwenty.block.RugBlock.WEST), "...both ways");
        h.destroyBlock(centre.north().west());
        h.assertTrue(!h.getBlockState(centre).getValue(com.malevolentgods.fourtwenty.block.RugBlock.NORTH_WEST), "Told when the diagonal goes");
        h.succeed();
    }
}

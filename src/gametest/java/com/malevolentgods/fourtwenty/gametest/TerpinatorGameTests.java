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
        return (TerpinatorBlockEntity) h.getBlockEntity(POS);
    }

    private static void run(GameTestHelper h, TerpinatorBlockEntity t, int ticks) {
        for (int i = 0; i < ticks; i++) TerpinatorBlockEntity.tick(h.getLevel(), t.getBlockPos(), h.getLevel().getBlockState(t.getBlockPos()), t);
    }

    private static ItemStack terp(Code code, int count) { return new ItemStack(ModTerpenes.terpene(code), count); }

    @GameTest(template = "empty")
    public static void budsPressIntoResinThatRefinesIntoTheDialledBase(GameTestHelper h) {
        var t = place(h);
        t.setMethod(Base.LIM.ordinal());
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
}

package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.domain.Terpenes.Base;
import com.malevolentgods.fourtwenty.domain.Terpenes.Code;
import com.malevolentgods.fourtwenty.infuser.Infuser;
import com.malevolentgods.fourtwenty.infuser.InfuserState;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.malevolentgods.fourtwenty.terpene.TerpinatorBlockEntity;
import com.malevolentgods.fourtwenty.terpene.TerpinatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
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

    private static TerpinatorBlockEntity place(GameTestHelper h, int product) {
        h.setBlock(POS, ModTerpenes.TERPINATOR.get());
        var t = (TerpinatorBlockEntity) h.getBlockEntity(POS);
        t.setProduct(product);
        t.toggle();
        return t;
    }

    private static void run(GameTestHelper h, TerpinatorBlockEntity t, int ticks) {
        for (int i = 0; i < ticks; i++) {
            t.node().receive(t.node().capacity());
            TerpinatorBlockEntity.tick(h.getLevel(), t.getBlockPos(), h.getLevel().getBlockState(t.getBlockPos()), t);
        }
    }

    private static ItemStack terp(Code code, int count) { return new ItemStack(ModTerpenes.terpene(code), count); }

    private static ItemStack buds(Strain strain, int count) {
        return ModDataComponents.withStrain(new ItemStack(ModItems.WEED_BUD.get(), count), strain);
    }

    private static final int WHITE = PRODUCTS - 16 + DyeColor.WHITE.getId();

    @GameTest(template = "empty")
    public static void withNoPowerItPausesAndKeepsItsProgress(GameTestHelper h) {
        var t = place(h, WHITE);
        t.setItem(BUDS, buds(Strain.LANDRACE, 2));
        run(h, t, 10);
        t.node().extract(t.node().capacity());
        for (int i = 0; i < 20; i++) TerpinatorBlockEntity.tick(h.getLevel(), t.getBlockPos(), h.getLevel().getBlockState(t.getBlockPos()), t);
        h.assertTrue(t.data().get(DATA_PROGRESS) >= 10 && t.data().get(DATA_PROGRESS) < 20, "held its progress, at " + t.data().get(DATA_PROGRESS));
        h.assertTrue(t.data().get(DATA_PAUSED) == 1, "says it's paused");
        h.assertTrue(t.getItem(BUDS).getCount() == 2, "kept its buds");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anyWeedMakesAMinecraftColour(GameTestHelper h) {
        var t = place(h, WHITE);
        t.setItem(BUDS, buds(Strain.LANDRACE, 4));
        run(h, t, TIME - 1);
        h.assertTrue(t.getItem(OUT).isEmpty(), "A vial takes " + TIME + " ticks");
        run(h, t, 1);
        h.assertTrue(t.getItem(OUT).is(ModTerpenes.MINECRAFT_TERPENES.get("mc_white").get()) && t.getItem(BUDS).getCount() == 2,
                "Two buds make one Bone Meal Haze, got " + t.getItem(OUT));
        t.setItem(BUDS, buds(Strain.BLUE_DREAM, 2));
        run(h, t, TIME);
        h.assertTrue(t.getItem(OUT).getCount() == 2 && t.getItem(BUDS).isEmpty(), "A strain's buds work too");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aHempColourTakesItsFamilysWeed(GameTestHelper h) {
        Code teal = Code.of(Base.PIN, Base.LAV, Base.PIN);   // Hippie Hot Tub: the Green family
        var t = place(h, teal.index());
        t.setItem(BUDS, buds(Strain.LANDRACE, 2));
        run(h, t, TIME * 2);
        h.assertTrue(t.getItem(OUT).isEmpty() && t.getItem(BUDS).getCount() == 2, "Landrace can't make a hemp colour");
        h.assertTrue("wrong_family".equals(status(true, teal.index(), t.getItem(BUDS), t.getItem(OUT))), "... and says so");
        t.setItem(BUDS, buds(Strain.STICKY_ICKY, 2));
        run(h, t, TIME);
        h.assertTrue(t.getItem(OUT).is(ModTerpenes.terpene(teal)), "Any Green strain makes it, got " + t.getItem(OUT));
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void aStoppedTerpinatorWaitsAndKeepsItsProgress(GameTestHelper h) {
        var t = place(h, WHITE);
        t.setItem(BUDS, buds(Strain.LANDRACE, 2));
        run(h, t, TIME / 2);
        t.toggle();
        run(h, t, TIME * 2);
        h.assertTrue(t.getItem(OUT).isEmpty() && t.getItem(BUDS).getCount() == 2, "Nothing happens while stopped");
        t.toggle();
        run(h, t, TIME / 2);
        h.assertTrue(t.getItem(OUT).getCount() == 1, "It picks up where it stopped");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pickingAnotherColourRestartsTheVial(GameTestHelper h) {
        var t = place(h, WHITE);
        t.setItem(BUDS, buds(Strain.LANDRACE, 2));
        run(h, t, TIME - 5);
        t.setProduct(WHITE + 1);
        run(h, t, 10);
        h.assertTrue(t.getItem(OUT).isEmpty(), "A new colour starts from zero");
        run(h, t, TIME);
        h.assertTrue(t.getItem(OUT).is(ModTerpenes.MINECRAFT_TERPENES.get("mc_orange").get()), "Blaze Rod Glow, the new pick");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyCaughtColoursCanBePicked(GameTestHelper h) {
        h.setBlock(POS, ModTerpenes.TERPINATOR.get());
        var t = (TerpinatorBlockEntity) h.getBlockEntity(POS);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var menu = new TerpinatorMenu(0, player.getInventory(), t, t.data());
        h.assertTrue(!menu.clickMenuButton(player, TerpinatorMenu.BUTTON_PICK + WHITE) && t.product() == NONE, "White isn't caught yet");
        InfuserState s = Infuser.get(player);
        s.caught.put("white", 1);
        player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).put("fourtwenty_infuser", s.save());
        h.assertTrue(menu.clickMenuButton(player, TerpinatorMenu.BUTTON_PICK + WHITE) && t.product() == WHITE, "Caught, it can be made");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void anOldTerpinatorKeepsItsBudsAndPopsOutTheRest(GameTestHelper h) {
        h.setBlock(POS, ModTerpenes.TERPINATOR.get());
        var t = (TerpinatorBlockEntity) h.getBlockEntity(POS);
        var old = net.minecraft.core.NonNullList.withSize(7, ItemStack.EMPTY);
        old.set(0, new ItemStack(ModItems.WEED_BUD.get(), 5));
        old.set(1, new ItemStack(ModTerpenes.RESIN.get(), 3));
        old.set(6, terp(Code.of(Base.MYR, Base.PIN), 1));
        var tag = new CompoundTag();
        ContainerHelper.saveAllItems(tag, old, h.getLevel().registryAccess());
        tag.putInt("method", 2);
        t.loadWithComponents(tag, h.getLevel().registryAccess());
        h.assertTrue(t.getItem(BUDS).getCount() == 5 && t.getItem(OUT).isEmpty() && t.contents().size() == SIZE + 2, "Buds kept, two stacks waiting");
        run(h, t, 1);
        h.assertTrue(t.contents().size() == SIZE, "The leftovers are out");
        h.assertItemEntityPresent(ModTerpenes.RESIN.get(), POS, 2);
        h.assertItemEntityPresent(ModTerpenes.terpene(Code.of(Base.MYR, Base.PIN)), POS, 2);
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

    /** Domum Ornamentum takes hemp, concrete and glass as materials: the tags load (no missing entries) with every colour in them. */
    @GameTest(template = "empty")
    public static void hempConcreteAndGlassAreDomumMaterials(GameTestHelper h) {
        var tag = (java.util.function.Function<String, net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block>>) id ->
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.parse(id));
        var domumDefault = tag.apply("domum_ornamentum:default");
        var domumConcrete = tag.apply("domum_ornamentum:concrete");
        var glass = tag.apply("c:glass_blocks");
        h.assertTrue(ModTerpenes.NATURAL_HEMP.block().get().defaultBlockState().is(domumDefault), "natural hemp isn't a Domum material");
        ModTerpenes.HEMP.forEach((code, hemp) -> h.assertTrue(hemp.block().get().defaultBlockState().is(domumDefault), "hemp " + code + " isn't a Domum material"));
        ModTerpenes.CONCRETE.forEach((code, b) -> h.assertTrue(b.get().defaultBlockState().is(domumConcrete), "concrete " + code + " isn't a Domum material"));
        ModTerpenes.GLASS.forEach((code, b) -> h.assertTrue(b.get().defaultBlockState().is(glass), "glass " + code + " isn't c:glass_blocks"));
        h.succeed();
    }
}

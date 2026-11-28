package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBoardModule;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBuilding;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeNetwork;
import com.malevolentgods.fourtwenty.domain.college.StudyBoard;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeRegistry;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeResearch;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeSavedData;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeStudies;
import com.malevolentgods.fourtwenty.compat.minecolonies.college.WeedologistJob;
import com.malevolentgods.fourtwenty.domain.college.ColonyResearch;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.HiringMode;
import com.minecolonies.core.entity.citizen.EntityCitizen;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Weed College in a real colony (specs/017-weed-college; tasks T019): a study runs from the queue to done and teaches the colony its
 * colours; the University's research tree never sees it (SC-002); the research record survives a save (SC-005); a hired Weedologist asks
 * the warehouse for the study's supplies (FR-012).
 */
@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class CollegeGameTests {
    private static final String NEUTRALS = "fourtwenty:chem_neutrals";

    private static CollegeBuilding college(GameTestHelper h, IColony colony, BlockPos pos) {
        h.getLevel().setBlockAndUpdate(pos, CollegeRegistry.HUT.get().defaultBlockState());
        var building = (CollegeBuilding) colony.getServerBuildingManager().addNewBuilding(
                (TileEntityColonyBuilding) h.getLevel().getBlockEntity(pos), h.getLevel());
        building.setBuildingLevel(1);
        building.onUpgradeComplete(null, 1);
        return building;
    }

    @GameTest(template = "empty", batch = "college")
    public static void aStudyRunsToDoneAndTeachesTheColony(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College study", "Sensi Heights");
        try {
            college(h, colony, h.absolutePos(new BlockPos(1, 1, 1)));
            h.assertTrue(CollegeStudies.board().size() == 63, "all 63 studies loaded, got " + CollegeStudies.board().size());
            h.assertTrue(CollegeResearch.queue(colony, "fourtwenty:chem_lean_extraction") != null, "a tier-4 study is refused at level 1");
            h.assertTrue(CollegeResearch.queue(colony, NEUTRALS) == null, "a tier-1 colour study queues");
            ColonyResearch r = CollegeResearch.record(colony);
            CollegeResearch.startHead(colony);
            h.assertTrue(r.started() && !r.missing().isEmpty(), "starting fixes the study's supplies");
            r.deliver(0, 1);
            h.assertTrue(CollegeResearch.remove(colony, NEUTRALS) != null, "a study with supplies in it can't be removed");
            for (int line = 0; line < r.cost().size(); line++) r.deliver(line, r.cost().get(line).count());
            h.assertTrue(r.missing().isEmpty(), "everything delivered");
            int units = CollegeStudies.board().get(NEUTRALS).units();
            for (int i = 0; i < units; i++) r.work(false);
            var inProgress = colony.getResearchManager().getResearchTree().getResearchInProgress();
            h.assertTrue(inProgress.isEmpty(), "MineColonies' own research tree never holds a College study: " + inProgress);
            h.assertTrue(CollegeResearch.finishHead(colony) != null, "the study is written up");
            h.assertTrue(r.isDone(NEUTRALS) && r.head() == null, "done, and the queue moved on");
            var colours = CollegeResearch.knownColours(colony);
            h.assertTrue(colours.contains("mc_white") && colours.contains("mc_brown") && colours.size() == 5, "the colony knows its five colours: " + colours);
            h.assertTrue(CollegeResearch.knowsColour(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), "mc_gray"), "knowsColour at the colony");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    @GameTest(template = "empty", batch = "college")
    public static void theRecordSurvivesASave(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College save", "Sensi Heights");
        try {
            college(h, colony, h.absolutePos(new BlockPos(1, 1, 1)));
            CollegeResearch.queue(colony, NEUTRALS);
            CollegeResearch.queue(colony, "fourtwenty:chem_warms");
            CollegeResearch.startHead(colony);
            ColonyResearch r = CollegeResearch.record(colony);
            r.deliver(0, 3);
            r.work(false);
            r.work(true);
            r.learnColours(java.util.List.of("mc_red"));
            var provider = h.getLevel().registryAccess();
            var saved = CollegeSavedData.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(), provider);
            ColonyResearch back = CollegeSavedData.read(saved, provider).colony(colony.getID());
            h.assertTrue(back.queue().equals(r.queue()), "queue kept: " + back.queue());
            h.assertTrue(back.progress(NEUTRALS) == 1 && back.badBatches() == 1, "progress and bad batches kept");
            h.assertTrue(back.cost() != null && back.cost().equals(r.cost()) && back.delivered().equals(r.delivered()), "the fixed cost and deliveries kept");
            h.assertTrue(back.membersColours().contains("mc_red"), "members' colours kept");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    @GameTest(template = "empty", batch = "college")
    public static void theBoardChecksWhoAndWhat(GameTestHelper h) {
        var owner = FakePlayerFactory.getMinecraft(h.getLevel());
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), owner, "College board", "Sensi Heights");
        try {
            var pos = h.absolutePos(new BlockPos(1, 1, 1));
            var building = college(h, colony, pos);
            var stranger = FakePlayerFactory.get(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("college-stranger".getBytes()), "[CollegeStranger]"));
            int id = colony.getID();
            h.assertTrue("college.fourtwenty.refused.permission".equals(CollegeNetwork.act(stranger, id, pos, CollegeNetwork.Action.QUEUE, NEUTRALS)),
                    "a player without MANAGE_HUTS can't queue");
            h.assertTrue(CollegeResearch.record(colony).queue().isEmpty(), "and nothing was queued");
            h.assertTrue("college.fourtwenty.refused.no_college".equals(CollegeNetwork.act(owner, id, pos.above(4), CollegeNetwork.Action.QUEUE, NEUTRALS)),
                    "there must be a College at the position");
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.QUEUE, NEUTRALS) == null, "the owner queues a tier-1 study");
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.QUEUE, NEUTRALS) != null, "not twice");
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.QUEUE, "fourtwenty:chem_accord_myr_lim") != null,
                    "a tier-2 study is refused at level 1, even with its parent queued");
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.QUEUE, "fourtwenty:chem_warms") == null, "a second queues");
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.QUEUE, "fourtwenty:chem_purples") == null, "a third queues");
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.REMOVE, "fourtwenty:chem_purples") == null, "the third comes off");
            h.assertTrue("college.fourtwenty.refused.permission".equals(CollegeNetwork.act(stranger, id, pos, CollegeNetwork.Action.REMOVE, "fourtwenty:chem_warms")),
                    "a stranger can't remove either");
            CollegeResearch.startHead(colony);
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.REMOVE, NEUTRALS) == null,
                    "a head whose cost is fixed but has nothing in it yet can come off (a mistake can be taken back)");
            h.assertTrue(!CollegeResearch.record(colony).started(), "its fixed cost goes with it");
            String warms = "fourtwenty:chem_warms";
            CollegeResearch.startHead(colony);
            CollegeResearch.record(colony).deliver(0, 1);
            h.assertTrue("college.fourtwenty.refused.started".equals(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.REMOVE, warms)),
                    "a head with supplies in it can't come off");
            h.assertTrue(CollegeNetwork.act(owner, id, pos, CollegeNetwork.Action.QUEUE, NEUTRALS) == null, "the first queues again, behind it");

            CollegeBoardModule.Board board = building.getModule(CollegeRegistry.BOARD).snapshot();
            h.assertTrue(board.entries().size() == 63, "the board carries all 63 studies");
            h.assertTrue(board.queue().equals(java.util.List.of(warms, NEUTRALS)), "the queue, head first: " + board.queue());
            h.assertTrue(board.entry(warms).state() == StudyBoard.State.IN_PROGRESS && board.headStarted() && board.headUnderWay(),
                    "the head is under way");
            h.assertTrue(board.entry(NEUTRALS).state() == StudyBoard.State.QUEUED, "the second is queued");
            h.assertTrue(board.entry("fourtwenty:chem_purples").state() == StudyBoard.State.AVAILABLE, "the removed one is available again");
            var accord = board.entry("fourtwenty:chem_accord_myr_lim");
            h.assertTrue(accord.state() == StudyBoard.State.LOCKED && accord.lock() == StudyBoard.Lock.LEVEL && "2".equals(accord.lockArg()),
                    "an accord says it needs level 2: " + accord);
            h.assertTrue(!board.entry(warms).cost().isEmpty() && board.entry(warms).cost().equals(CollegeResearch.record(colony).cost()),
                    "the head shows its fixed cost");
            var pattern = board.entry("fourtwenty:tex_pattern_1");
            h.assertTrue(pattern.state() == StudyBoard.State.LOCKED && pattern.lock() == StudyBoard.Lock.FEATURE && "tie_dye".equals(pattern.lockArg()),
                    "Textiles waits for the Tie Dye Shack: " + pattern);
            var uniform = board.entry("fourtwenty:arm_weedscout_uniform");
            h.assertTrue(uniform.lock() == StudyBoard.Lock.FEATURE && "weedscout_uniform".equals(uniform.lockArg()), "and the uniform for itself: " + uniform);
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            board.write(buffer);
            h.assertTrue(CollegeBoardModule.Board.read(buffer).equals(board), "the board view survives the trip to the client");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    /** The research R6 spike (T028), kept as a test: an Armory study unlocks a Blacksmith crafter recipe, and only then. */
    @GameTest(template = "empty", batch = "college")
    public static void anArmoryStudyTeachesTheBlacksmith(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College armory", "Sensi Heights");
        try {
            college(h, colony, h.absolutePos(new BlockPos(1, 1, 1)));
            var smithPos = h.absolutePos(new BlockPos(4, 1, 1));
            h.getLevel().setBlockAndUpdate(smithPos, com.minecolonies.api.blocks.ModBlocks.blockHutBlacksmith.defaultBlockState());
            var smith = colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding) h.getLevel().getBlockEntity(smithPos), h.getLevel());
            smith.setBuildingLevel(3);
            var dabCell = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("fourtwenty:dab_cell"));
            var recipe = com.minecolonies.core.colony.crafting.CustomRecipeManager.getInstance().getRecipes("blacksmith_crafting").stream()
                    .filter(r -> r.getPrimaryOutput().is(dabCell)).findFirst().orElse(null);
            h.assertTrue(recipe != null, "the Dab Cell's Blacksmith recipe is loaded");
            h.assertTrue(!recipe.isValidForBuilding(smith), "a colony without Dab Cell can't make it");
            var study = CollegeStudies.board().get("fourtwenty:arm_dab_cell");
            CollegeResearch.finish(colony, study.id());
            h.assertTrue(com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeEffects.applied(colony, study), "the effect is applied");
            h.assertTrue(recipe.isValidForBuilding(smith), "the recipe is valid for the colony's Blacksmith once Dab Cell is done");
            boolean learned = false;
            for (var module : smith.getModulesByType(com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule.class)) {
                module.checkForWorkerSpecificRecipes();
                for (var token : module.getRecipes()) {
                    var storage = IColonyManager.getInstance().getRecipeManager().getRecipe(token);
                    if (storage != null && storage.getPrimaryOutput().is(dabCell)) learned = true;
                }
            }
            h.assertTrue(learned, "the Blacksmith's crafting module takes the recipe up");
            // undoing a University research rebuilds MineColonies' effects without ours; the colony clock puts them back
            colony.getResearchManager().getResearchEffects().removeAllEffects();
            h.assertTrue(!recipe.isValidForBuilding(smith), "wiped");
            com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeEffects.keepApplied(colony);
            h.assertTrue(recipe.isValidForBuilding(smith), "and restored");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    /** FR-022: a finished Botany strain study's seeds arrive in the warehouse, of the new strain, with no traits. */
    @GameTest(template = "empty", batch = "college")
    public static void aBotanyStudyStocksTheWarehouse(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College seeds", "Sensi Heights");
        try {
            college(h, colony, h.absolutePos(new BlockPos(1, 1, 1)));
            var housePos = h.absolutePos(new BlockPos(5, 1, 1));
            h.getLevel().setBlockAndUpdate(housePos, com.minecolonies.api.blocks.ModBlocks.blockHutWareHouse.defaultBlockState());
            var warehouse = colony.getServerBuildingManager().addNewBuilding((TileEntityColonyBuilding) h.getLevel().getBlockEntity(housePos), h.getLevel());
            warehouse.setBuildingLevel(1);
            CollegeResearch.finish(colony, "fourtwenty:bot_dust_devil");
            int seeds = 0;
            var handler = warehouse.getItemHandlerCap();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                var stack = handler.getStackInSlot(slot);
                if (stack.is(com.malevolentgods.fourtwenty.registry.ModItems.WEED_SEEDS.get())
                        && com.malevolentgods.fourtwenty.registry.ModDataComponents.strain(stack) == com.malevolentgods.fourtwenty.domain.Strain.DUST_DEVIL
                        && !stack.has(com.malevolentgods.fourtwenty.registry.ModDataComponents.TRAITS.get())) {
                    seeds += stack.getCount();
                }
            }
            h.assertTrue(seeds == 4, "four plain Dust Devil seeds in the warehouse, got " + seeds);
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    /** FR-022, research R8: Master Grower gives an extra bud on every harvest by a colony's worker, and nothing to a harvest by anyone else. */
    @GameTest(template = "empty", batch = "college")
    public static void aYieldStudyFeedsTheFarms(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College yields", "Sensi Heights");
        EntityCitizen farmer = null;
        try {
            var crop = h.absolutePos(new BlockPos(3, 2, 3));
            var ripe = com.malevolentgods.fourtwenty.registry.ModBlocks.WEED_CROP.get();
            farmer = DispensaryProductionGameTests.actor(h, colony, crop.offset(1, 0, 0));
            var roughBud = com.malevolentgods.fourtwenty.registry.ModItems.ROUGH_BUD.get();
            h.getLevel().setBlockAndUpdate(crop.below(), net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState());
            h.getLevel().setBlockAndUpdate(crop, ripe.getStateForAge(ripe.getMaxAge()));
            int before = count(com.minecolonies.api.util.BlockPosUtil.getBlockDrops(h.getLevel(), crop, 0, net.minecraft.world.item.ItemStack.EMPTY, farmer), roughBud);
            h.assertTrue(before == 1, "one bud without the study, got " + before);
            CollegeResearch.finish(colony, "fourtwenty:bot_master_grower");
            int after = count(com.minecolonies.api.util.BlockPosUtil.getBlockDrops(h.getLevel(), crop, 0, net.minecraft.world.item.ItemStack.EMPTY, farmer), roughBud);
            h.assertTrue(after == 2, "two buds with Master Grower, got " + after);
            int byHand = count(net.minecraft.world.level.block.Block.getDrops(h.getLevel().getBlockState(crop), h.getLevel(), crop,
                    h.getLevel().getBlockEntity(crop), FakePlayerFactory.getMinecraft(h.getLevel()), net.minecraft.world.item.ItemStack.EMPTY), roughBud);
            h.assertTrue(byHand == 1, "a player harvesting in the same colony gets the usual one, got " + byHand);
            h.succeed();
        } finally {
            if (farmer != null) farmer.discard();
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    /**
     * Research R7: an Engineering study speeds a trimmer inside the colony and not one outside it; a Chemistry study halves a colony
     * Terpinator's BI and leaves its speed alone; Deep Stash makes a colony Stash bigger.
     */
    @GameTest(template = "empty", batch = "college")
    public static void colonyStudiesTuneTheColonysMachines(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College machines", "Sensi Heights");
        try {
            var level = h.getLevel();
            var inside = h.absolutePos(new BlockPos(3, 2, 3));
            var outside = inside.offset(800, 0, 0);
            level.setBlockAndUpdate(inside, com.malevolentgods.fourtwenty.registry.ModPower.SNIP_STATION.get().defaultBlockState());
            var snip = (com.malevolentgods.fourtwenty.processing.MachineBlockEntity) level.getBlockEntity(inside);
            int normal = snip.workTicksForTest();
            var trim = com.malevolentgods.fourtwenty.domain.college.ColonyEffects.Effect.TRIM_CURE_SPEED;
            var terp = com.malevolentgods.fourtwenty.domain.college.ColonyEffects.Effect.TERPINATOR_BI;
            var stash = com.malevolentgods.fourtwenty.domain.college.ColonyEffects.Effect.STASH_CAPACITY;
            h.assertTrue(com.malevolentgods.fourtwenty.power.ColonyFactor.of(level, inside, trim) == 1.0, "no study, no change");

            CollegeResearch.finish(colony, "fourtwenty:eng_bench_habits");
            CollegeResearch.finish(colony, "fourtwenty:chem_lean_extraction");
            CollegeResearch.finish(colony, "fourtwenty:eng_deep_stash");
            int faster = snip.workTicksForTest();
            h.assertTrue(faster == com.malevolentgods.fourtwenty.domain.college.ColonyEffects.scaledTicks(normal, 1.25) && faster < normal,
                    "Bench Habits: the colony's Snip Station works a quarter faster, " + normal + " -> " + faster);
            h.assertTrue(com.malevolentgods.fourtwenty.power.ColonyFactor.of(level, outside, trim) == 1.0, "one outside the colony doesn't");
            h.assertTrue(com.malevolentgods.fourtwenty.power.ColonyFactor.cost(level, inside, terp, 105) == 52.5,
                    "Lean Extraction: a colony Terpinator draws half its BI");
            h.assertTrue(com.malevolentgods.fourtwenty.power.ColonyFactor.length(level, inside, stash, 1000) == 1500, "Deep Stash: half as much again");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    /** FR-020, research R9: a colour an officer has caught makes its study cheaper; a stranger's catches don't count. */
    @GameTest(template = "empty", batch = "college")
    public static void caughtColoursMakeAStudyCheaper(GameTestHelper h) {
        var owner = FakePlayerFactory.getMinecraft(h.getLevel());
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)), owner, "College colours", "Sensi Heights");
        try {
            var building = college(h, colony, h.absolutePos(new BlockPos(1, 1, 1)));
            String warms = "fourtwenty:chem_warms";
            var buds = com.malevolentgods.fourtwenty.registry.ModItems.WEED_BUD.getId().toString();
            java.util.function.IntSupplier cost = () -> building.getModule(CollegeRegistry.BOARD).snapshot().entry(warms).cost().stream()
                    .filter(s -> s.item().equals(buds)).mapToInt(com.malevolentgods.fourtwenty.domain.college.Study.Supply::count).sum();
            h.assertTrue(cost.getAsInt() == 24, "eight buds for each of three colours, got " + cost.getAsInt());
            var stranger = FakePlayerFactory.get(h.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("college-stranger".getBytes()), "[CollegeStranger]"));
            CollegeResearch.memberColours(stranger, java.util.Set.of("mc_red"));
            h.assertTrue(cost.getAsInt() == 24, "a stranger's red doesn't count");
            CollegeResearch.memberColours(owner, java.util.Set.of("mc_red"));
            h.assertTrue(cost.getAsInt() == 20, "the owner's red halves red's share: 4 + 8 + 8, got " + cost.getAsInt());
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    /** T039, FR-015/FR-016: Weedologists 1, 1, 2, 2, 2 by level; a study's tier needs a College of that level; tier 6 needs its branch's top. */
    @GameTest(template = "empty", batch = "college")
    public static void aCollegeGrowsWithItsLevel(GameTestHelper h) {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College levels", "Sensi Heights");
        try {
            var building = college(h, colony, h.absolutePos(new BlockPos(1, 1, 1)));
            int[] workers = {1, 1, 2, 2, 2};
            for (int level = 1; level <= 5; level++) {
                building.setBuildingLevel(level);
                int max = building.getModule(CollegeRegistry.WORK).getModuleMax();
                h.assertTrue(max == workers[level - 1], "level " + level + " hires " + workers[level - 1] + ", got " + max);
            }
            var board = CollegeStudies.board();
            ColonyResearch r = CollegeResearch.record(colony);
            var botany = board.all().stream().filter(s -> s.branch() == com.malevolentgods.fourtwenty.domain.college.Study.Branch.BOTANY).toList();
            for (var s : botany) if (s.tier() <= 4) r.finish(s.id(), 1);
            var fives = board.branchTop(com.malevolentgods.fourtwenty.domain.college.Study.Branch.BOTANY);
            var six = botany.stream().filter(s -> s.tier() == 6).findFirst().orElseThrow();
            var fiveId = fives.getFirst().id();
            building.setBuildingLevel(4);
            h.assertTrue(CollegeResearch.queue(colony, fiveId) != null, "a tier-5 study needs a level-5 College");
            var lockAt4 = board.standings(r, 4, f -> true).get(six.id());
            h.assertTrue(lockAt4.lock() != StudyBoard.Lock.NONE, "tier 6 is locked at level 4: " + lockAt4);
            building.setBuildingLevel(5);
            h.assertTrue(CollegeResearch.queue(colony, fiveId) == null, "and queues at level 5");
            CollegeResearch.remove(colony, fiveId);
            // tier 6 needs every tier-5 study in its branch done (or queued ahead of it)
            for (var f : fives) if (six.parents().contains(f.id())) r.finish(f.id(), 2);
            var others = fives.stream().filter(f -> !r.isDone(f.id())).toList();
            if (!others.isEmpty()) {
                var lock = board.standings(r, 5, f -> true).get(six.id());
                h.assertTrue(lock.lock() == StudyBoard.Lock.BRANCH, "tier 6 waits for the rest of its branch's top: " + lock);
                for (var f : others) r.finish(f.id(), 3);
            }
            h.assertTrue(board.standings(r, 5, f -> true).get(six.id()).state() == StudyBoard.State.AVAILABLE, "then it's open");
            h.assertTrue(CollegeResearch.queue(colony, six.id()) == null, "and queues");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }

    private static int count(java.util.List<net.minecraft.world.item.ItemStack> drops, net.minecraft.world.item.Item item) {
        int n = 0;
        for (var s : drops) if (s.is(item)) n += s.getCount();
        return n;
    }

    @GameTest(template = "empty", batch = "college", timeoutTicks = 200)
    public static void aWeedologistAsksForTheSupplies(GameTestHelper h) throws Exception {
        var colony = IColonyManager.getInstance().createColony(h.getLevel(), h.absolutePos(new BlockPos(1, 1, 1)),
                FakePlayerFactory.getMinecraft(h.getLevel()), "College supplies", "Sensi Heights");
        EntityCitizen actor = null;
        try {
            var pos = h.absolutePos(new BlockPos(1, 1, 1));
            var building = college(h, colony, pos);
            building.getModule(CollegeRegistry.WORK).setHiringMode(HiringMode.MANUAL);
            actor = DispensaryProductionGameTests.actor(h, colony, pos.offset(1, 0, 0));
            h.assertTrue(building.getModule(CollegeRegistry.WORK).assignCitizen(actor.getCitizenData()), "Weedologist hired");
            CollegeResearch.queue(colony, NEUTRALS);
            var ai = ((WeedologistJob) actor.getCitizenData().getJob()).getWorkerAI();
            var decide = ai.getClass().getDeclaredMethod("decide");
            decide.setAccessible(true);
            decide.invoke(ai);
            h.assertTrue(CollegeResearch.record(colony).started(), "the head study started");
            h.assertTrue(!building.getOpenRequestsByRequestableType().isEmpty(), "the Weedologist asked the colony for supplies");
            h.assertTrue(building.activity() == CollegeBuilding.Activity.WAITING, "waiting for supplies, got " + building.activity());
            h.succeed();
        } finally {
            if (actor != null) actor.discard();
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
    }
}

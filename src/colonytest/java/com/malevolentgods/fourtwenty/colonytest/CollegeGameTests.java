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
            var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), h.getLevel().registryAccess());
            board.write(buffer);
            h.assertTrue(CollegeBoardModule.Board.read(buffer).equals(board), "the board view survives the trip to the client");
            h.succeed();
        } finally {
            IColonyManager.getInstance().deleteColonyByWorld(colony.getID(), false, h.getLevel());
        }
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

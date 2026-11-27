package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.college.CollegeBuilding;
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
            h.assertTrue(CollegeResearch.remove(colony, NEUTRALS) != null, "a started study can't be removed");
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

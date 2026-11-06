package com.malevolentgods.fourtwenty.gametest;

import com.malevolentgods.fourtwenty.domain.LedRules;
import com.malevolentgods.fourtwenty.domain.Programs;
import com.malevolentgods.fourtwenty.lights.BitChitItem;
import com.malevolentgods.fourtwenty.lights.LitBitBlockEntity;
import com.malevolentgods.fourtwenty.lights.ModLights;
import com.malevolentgods.fourtwenty.lights.StickyFingerItem;
import com.malevolentgods.fourtwenty.lights.WeedweenoBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.registry.ModBlocks;
import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/** Lit Bits, the Weedweeno, the Sticky Finger and Bit Chits in the world (specs/008-lit-bits). */
@GameTestHolder("fourtwenty_tests")
@PrefixGameTestTemplate(false)
public final class LightsGameTests {
    private LightsGameTests() {}

    private static LitBitBlockEntity lamp(GameTestHelper h, BlockPos pos) { return (LitBitBlockEntity) h.getBlockEntity(pos); }

    private static BlockPos lamp(Ladder l, int x, int id, int rgb) {
        BlockPos pos = l.put(x, ModLights.LIT_BIT.get());
        var lamp = lamp(l.h, pos);
        lamp.setId(id);
        lamp.setColour(rgb);
        return pos;
    }

    private static WeedweenoBlockEntity weedweeno(GameTestHelper h, BlockPos pos, boolean charged) {
        h.setBlock(pos, Ladder.facingWest(ModLights.WEEDWEENO.get()));
        var w = (WeedweenoBlockEntity) h.getBlockEntity(pos);
        if (charged) ((BIBuffer) w.node()).set(100);
        return w;
    }

    private static void run(WeedweenoBlockEntity w, String code) {
        w.create();
        w.save(w.selected(), "Test", code);
        w.run(w.selected());
    }

    /** On a Hookah's 4.2 Kb a Lit Bit lights its colour at near full brightness. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aLitBitShowsItsColourOnAHookah(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 5);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos red = lamp(l, 3, 30001, 0xFF0000), mixed = lamp(l, 5, 30002, 0x00FF80);
        h.runAfterDelay(80, () -> {
            int a = lamp(h, red).shown(), b = lamp(h, mixed).shown();
            h.assertTrue(LedRules.red(a) > 230 && LedRules.green(a) == 0 && LedRules.blue(a) == 0, "red, got " + Integer.toHexString(a));
            h.assertTrue(LedRules.green(b) > 230 && LedRules.blue(b) > 110 && LedRules.red(b) == 0, "green-cyan, got " + Integer.toHexString(b));
            h.assertTrue(h.getBlockState(red).getValue(com.malevolentgods.fourtwenty.lights.LitBitBlock.LEVEL) > 0, "and it lights the world");
            h.succeed();
        });
    }

    /** No loop, no light: a Lit Bit set to a colour but on no circuit stays dark. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void anUnpoweredLitBitIsDark(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModLights.LIT_BIT.get());
        lamp(h, pos).setColour(0xFFFFFF);
        h.runAfterDelay(30, () -> {
            h.assertTrue(lamp(h, pos).shown() == 0, "dark, got " + Integer.toHexString(lamp(h, pos).shown()));
            h.succeed();
        });
    }

    /** A Lit Bit whose − side meets an undyed Wick has no return: it stays dark while the one beside it lights. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aLitBitWithNoBlackWickStaysDark(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 5);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        BlockPos right = lamp(l, 3, 30011, 0xFFFFFF);
        h.setBlock(l.minus(5), ModPower.HEMP_WICK.get());
        h.setBlock(l.minus(4), ModPower.HEMP_WICK.get());
        BlockPos wrong = lamp(l, 5, 30012, 0xFFFFFF);
        h.runAfterDelay(80, () -> {
            h.assertTrue(lamp(h, right).shown() != 0, "the wired one lights");
            h.assertTrue(lamp(h, wrong).shown() == 0, "no return, dark: got " + Integer.toHexString(lamp(h, wrong).shown()));
            h.succeed();
        });
    }

    /** 42 Kb across a 4.2 Kb lamp overloads it: dark, in smoke, and nothing breaks or explodes. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aLitBitOverloadsOnTooHighAVoltage(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.ROLLED_WICK.get(), 1, 5);
        l.hookah(1, ModPower.DOUBLE_HOSE.get());
        BlockPos pos = lamp(l, 3, 30021, 0xFFFFFF);
        l.put(5, ModPower.BUD_BARBER.get());
        h.runAfterDelay(100, () -> {
            h.assertBlockPresent(ModLights.LIT_BIT.get(), pos);
            h.assertTrue(lamp(h, pos).shown() == 0, "overloaded and dark, got " + Integer.toHexString(lamp(h, pos).shown()));
            h.succeed();
        });
    }

    /** A Weedweeno on the loop runs a show: each lamp takes the colour set for its ID, and Stop turns them off. */
    @GameTest(template = "long", timeoutTicks = 300)
    public static void aWeedweenoSetsLampsByIdAndStopTurnsThemOff(GameTestHelper h) {
        var l = new Ladder(h, 2, 1).rails(ModPower.HEMP_WICK.get(), 1, 7);
        l.hookah(1, ModBlocks.WEED_BONG.get());
        var w = weedweeno(h, l.at(3), false);
        BlockPos one = lamp(l, 5, 30031, 0), two = lamp(l, 7, 30032, 0);
        run(w, "set(30031, 0, 255, 0)\nset(30032, 0, 0, 255)\nwhile true do frame() end\n");
        h.runAfterDelay(100, () -> {
            h.assertTrue(w.state() == WeedweenoBlockEntity.State.RUNNING, "running, got " + w.state() + " " + w.message());
            int a = lamp(h, one).shown(), b = lamp(h, two).shown();
            h.assertTrue(LedRules.green(a) > 230 && LedRules.red(a) == 0, "lamp 1 green, got " + Integer.toHexString(a));
            h.assertTrue(LedRules.blue(b) > 230 && LedRules.red(b) == 0, "lamp 2 blue, got " + Integer.toHexString(b));
            w.stop();
            h.assertTrue(lamp(h, one).colour() == 0 && lamp(h, two).colour() == 0, "Stop turns its lamps off");
            h.succeed();
        });
    }

    /** A syntax error stops the show, with the line it's on. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aBrokenProgramReportsItsLine(GameTestHelper h) {
        var w = weedweeno(h, new BlockPos(2, 2, 2), true);
        run(w, "set(1, 255, 0, 0)\nwhile true do\n  frame(\n");
        h.assertTrue(w.state() == WeedweenoBlockEntity.State.ERROR, "error, got " + w.state());
        h.assertTrue(w.line() >= 2, "its line, got " + w.line() + ": " + w.message());
        h.succeed();
    }

    /** A loop that never yields runs out of its time budget and stops as too slow; the server goes on. */
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void aShowThatNeverYieldsIsStopped(GameTestHelper h) {
        var w = weedweeno(h, new BlockPos(2, 2, 2), true);
        run(w, "local n = 0\nwhile true do n = n + 1 end\n");
        h.runAfterDelay(20, () -> {
            h.assertTrue(w.state() == WeedweenoBlockEntity.State.SLOW, "too slow, got " + w.state() + " " + w.message());
            h.succeed();
        });
    }

    /** The sandbox: no files, no OS, no loading code. */
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void aShowCannotReachTheOutside(GameTestHelper h) {
        var w = weedweeno(h, new BlockPos(2, 2, 2), true);
        run(w, "assert(io == nil and os == nil and require == nil and load == nil and dofile == nil and debug == nil)\nprint('sealed')\n");
        h.runAfterDelay(10, () -> {
            h.assertTrue(w.state() == WeedweenoBlockEntity.State.DONE, "ran to the end, got " + w.state() + " " + w.message());
            h.assertTrue(w.console().contains("sealed"), "and printed, got " + w.console());
            h.succeed();
        });
    }

    /** A Lit Bit placed where one was broken gets its old ID back. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void aLitBitPlacedWhereOneBrokeGetsItsId(GameTestHelper h) {
        BlockPos pos = new BlockPos(2, 2, 2);
        h.setBlock(pos, ModLights.LIT_BIT.get());
        lamp(h, pos).setId(4242);
        h.setBlock(pos, Blocks.AIR);
        h.setBlock(pos, ModLights.LIT_BIT.get());
        var abs = h.absolutePos(pos);
        var state = h.getBlockState(pos);
        state.getBlock().setPlacedBy(h.getLevel(), abs, state, null, new ItemStack(ModLights.LIT_BIT_ITEM.get()));
        h.assertTrue(lamp(h, pos).id() == 4242, "its old ID, got " + lamp(h, pos).id());
        h.assertTrue(lamp(h, pos).fromMemory(), "remembered");
        h.succeed();
    }

    /** The Sticky Finger places Lit Bits from the off hand, numbering them from its next ID. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void theStickyFingerNumbersLampsAsItPlacesThem(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack glove = new ItemStack(ModLights.STICKY_FINGER.get());
        StickyFingerItem.setNext(glove, 300);
        player.setItemInHand(InteractionHand.MAIN_HAND, glove);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModLights.LIT_BIT_ITEM.get(), 3));
        List<BlockPos> placed = new ArrayList<>();
        for (int x = 1; x <= 3; x += 2) {
            BlockPos floor = new BlockPos(x, 1, 3);
            h.setBlock(floor, Blocks.STONE);
            var abs = h.absolutePos(floor);
            var hit = new BlockHitResult(Vec3.atCenterOf(abs).add(0, 0.5, 0), Direction.UP, abs, false);
            glove.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            placed.add(floor.above());
        }
        h.assertTrue(lamp(h, placed.get(0)).id() == 300 && lamp(h, placed.get(1)).id() == 301,
                "numbered 300 and 301, got " + lamp(h, placed.get(0)).id() + " and " + lamp(h, placed.get(1)).id());
        h.assertTrue(StickyFingerItem.next(glove) == 302, "next is 302, got " + StickyFingerItem.next(glove));
        h.assertTrue(player.getOffhandItem().getCount() == 1, "two lamps used, " + player.getOffhandItem().getCount() + " left");
        h.succeed();
    }

    /** A program goes onto a Bit Chit and into another Weedweeno under a name of its own; a broken Weedweeno keeps its programs. */
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void programsTravelOnBitChitsAndInTheBlock(GameTestHelper h) {
        var a = weedweeno(h, new BlockPos(1, 2, 1), false);
        a.save(0, "Kept", "frame()\n");
        ItemStack chit = BitChitItem.written(a.programs().get(0));
        h.assertTrue(BitChitItem.program(chit).name().equals("Kept") && !BitChitItem.blank(chit), "the chit holds it");
        var b = weedweeno(h, new BlockPos(3, 2, 3), false);
        b.add(BitChitItem.program(chit));
        b.add(BitChitItem.program(chit));
        long kept = b.programs().stream().map(Programs.Program::name).filter(n -> n.startsWith("Kept")).distinct().count();
        h.assertTrue(kept == 2, "loaded twice, under two names: " + b.programs().stream().map(Programs.Program::name).toList());
        ItemStack drop = new ItemStack(ModLights.WEEDWEENO_ITEM.get());
        drop.applyComponents(a.collectComponents());
        var c = weedweeno(h, new BlockPos(1, 2, 3), false);
        c.applyComponentsFromItemStack(drop);
        h.assertTrue(c.programs().get(0).name().equals("Kept"), "the dropped block keeps its programs, got " + c.programs().get(0).name());
        h.succeed();
    }

    /** 1,024 lamps set every frame: all take their colour within a few ticks, and the show stays inside its time budget. */
    @GameTest(template = "long", timeoutTicks = 200)
    public static void aThousandLampsKeepUp(GameTestHelper h) {
        List<BlockPos> lamps = new ArrayList<>();
        int id = 40001;
        for (int x = 1; x <= 103 && id <= 41024; x++) {
            for (int y = 1; y <= 2 && id <= 41024; y++) {
                for (int z = 1; z <= 5 && id <= 41024; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (x == 52 && y == 2 && z == 3) continue;
                    h.setBlock(pos, ModLights.LIT_BIT.get());
                    lamp(h, pos).setId(id++);
                    lamps.add(pos);
                }
            }
        }
        var w = weedweeno(h, new BlockPos(52, 2, 3), true);
        run(w, "local t = 0\nwhile true do\n  for _, id in ipairs(ids()) do if id >= 40001 and id <= 41024 then set(id, 255, t % 256, 0) end end\n  t = t + 1\n  frame()\nend\n");
        h.runAfterDelay(20, () -> {
            h.assertTrue(w.state() == WeedweenoBlockEntity.State.RUNNING, "still running, got " + w.state() + " " + w.message());
            int lit = 0;
            for (BlockPos pos : lamps) if (LedRules.red(lamp(h, pos).colour()) == 255) lit++;
            h.assertTrue(lit == lamps.size(), lit + " of " + lamps.size() + " lamps set");
            w.stop();
            h.succeed();
        });
    }
}

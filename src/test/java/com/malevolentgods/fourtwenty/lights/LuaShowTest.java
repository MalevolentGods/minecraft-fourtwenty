package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.LedRules;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuaShowTest {
    private static final long BUDGET = 2_000_000;

    /** Five lamps, IDs 1-5, and a console. */
    private static final class Fake implements LuaShow.Lamps {
        final Map<Integer, Integer> colours = new TreeMap<>();
        final List<String> console = new ArrayList<>();

        @Override public void set(int id, int rgb) { if (id >= 1 && id <= 5) colours.put(id, rgb); }

        @Override public List<Integer> ids() { return List.of(1, 2, 3, 4, 5); }

        @Override public void print(String line) { console.add(line); }
    }

    @Test
    void itSetsLampsByIdAndWaitsForFrames() {
        var lamps = new Fake();
        var show = LuaShow.start("set(1, 255, 0, 0) frame() set(2, 0, 0, 255) frame() off(1)", lamps);
        assertEquals(LuaShow.Status.RUNNING, show.tick(BUDGET));
        assertEquals(Map.of(1, LedRules.pack(255, 0, 0)), lamps.colours, "one frame at a time");
        show.tick(BUDGET);
        assertEquals(LedRules.pack(0, 0, 255), lamps.colours.get(2));
        assertEquals(LuaShow.Status.DONE, show.tick(BUDGET));
        assertEquals(0, lamps.colours.get(1));
    }

    @Test
    void theLibrary() {
        var lamps = new Fake();
        var show = LuaShow.start("""
                assert(count() == 5)
                local list = ids()
                assert(#list == 5 and list[1] == 1)
                local r, g, b = bits.rgb(120, 1, 1)
                assert(r == 0 and g == 255 and b == 0)
                all(1, 2, 3)
                hsv(3, 240, 1, 1)
                print("hello", 42)
                """, lamps);
        assertEquals(LuaShow.Status.DONE, show.tick(BUDGET), show.message());
        assertEquals(LedRules.pack(1, 2, 3), lamps.colours.get(1));
        assertEquals(LedRules.pack(0, 0, 255), lamps.colours.get(3));
        assertEquals(List.of("hello\t42"), lamps.console);
    }

    @Test
    void sleepWaitsWholeTicks() {
        var lamps = new Fake();
        var show = LuaShow.start("sleep(0.25) set(1, 9, 9, 9)", lamps);
        for (int i = 0; i < 5; i++) show.tick(BUDGET);
        assertTrue(lamps.colours.isEmpty(), "five ticks of sleep");
        show.tick(BUDGET);
        assertEquals(LedRules.pack(9, 9, 9), lamps.colours.get(1));
    }

    @Test
    void theSandboxHasNoWayOut() {
        var show = LuaShow.start("frame()", new Fake());
        for (String name : List.of("io", "os", "debug", "dofile", "loadfile", "require", "package", "collectgarbage")) {
            assertFalse(show.has(name), name + " must not exist");
        }
    }

    @Test
    void aRunawayLoopIsStoppedWithinItsBudget() {
        var show = LuaShow.start("while true do end", new Fake());
        long start = System.nanoTime();
        assertEquals(LuaShow.Status.SLOW, show.tick(BUDGET));
        assertTrue(System.nanoTime() - start < 200_000_000L, "stopped quickly");
        assertTrue(show.message().contains("too slow"));
    }

    @Test
    void aRunawayPatternIsStoppedToo() {
        var show = LuaShow.start("local s = string.rep('a', 100000) while true do s:find('(a*)*b') end", new Fake());
        assertEquals(LuaShow.Status.SLOW, show.tick(BUDGET));
    }

    @Test
    void errorsSayTheirLine() {
        var syntax = LuaShow.start("frame()\nframe()\nset(1, 2, 3\n", new Fake());
        assertEquals(LuaShow.Status.ERROR, syntax.status());
        assertEquals(3, syntax.line(), syntax.message());
        var runtime = LuaShow.start("frame()\nlocal x = nil\nx.y = 1\n", new Fake());
        runtime.tick(BUDGET);
        assertEquals(LuaShow.Status.ERROR, runtime.tick(BUDGET));
        assertEquals(3, runtime.line(), runtime.message());
    }

    @Test
    void hugeStringsAreRefused() {
        var show = LuaShow.start("local s = string.rep('x', 10000000)", new Fake());
        assertEquals(LuaShow.Status.ERROR, show.tick(BUDGET));
        assertTrue(show.message().contains("string too long"), show.message());
    }

    @Test
    void theChasePresetRuns() {
        var lamps = new Fake();
        var show = LuaShow.start("""
                local list = ids()
                local i = 0
                while true do
                  for n, id in ipairs(list) do
                    if n == i % #list + 1 then set(id, 255, 120, 0) else off(id) end
                  end
                  i = i + 1
                  sleep(0.1)
                end
                """, lamps);
        for (int t = 0; t < 40; t++) assertEquals(LuaShow.Status.RUNNING, show.tick(BUDGET), show.message());
        long lit = lamps.colours.values().stream().filter(c -> c != 0).count();
        assertEquals(1, lit, "one lamp lit at a time");
    }

    @Test
    void nothingTurnsTextIntoCode() {
        var show = LuaShow.start("assert(load == nil and loadstring == nil and dofile == nil and require == nil and string.dump == nil)", new Fake());
        assertEquals(LuaShow.Status.DONE, show.tick(BUDGET), show.message());
    }

    @Test
    void oneSlowFrameIsLetOffButASlowShowIsStopped() {
        var once = LuaShow.start("""
                local first = true
                while true do
                  if first then local x = 0 for i = 1, 3000000 do x = x + i end first = false end
                  frame()
                end
                """, new Fake());
        assertEquals(LuaShow.Status.RUNNING, once.tick(1_000, 5_000_000_000L), "a slow first frame carries on");
        for (int t = 0; t < 60; t++) assertEquals(LuaShow.Status.RUNNING, once.tick(BUDGET, 5_000_000_000L), once.message());
        var always = LuaShow.start("while true do local x = 0 for i = 1, 3000000 do x = x + i end frame() end", new Fake());
        LuaShow.Status status = LuaShow.Status.RUNNING;
        for (int t = 0; t < LuaShow.MAX_OVERRUNS + 5 && status == LuaShow.Status.RUNNING; t++) status = always.tick(1_000, 5_000_000_000L);
        assertEquals(LuaShow.Status.SLOW, status, "every frame over budget: stopped");
    }
}

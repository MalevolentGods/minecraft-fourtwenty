package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.LedRules;
import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaState;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.LuaValue;
import org.squiddev.cobalt.ValueFactory;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.compiler.CompileException;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.RegisteredFunction;
import org.squiddev.cobalt.interrupt.InterruptAction;
import org.squiddev.cobalt.lib.CoreLibraries;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One running light show: a Lua program in Cobalt (the sandboxed Lua CC: Tweaked runs), with only the standard libraries (no io, os, debug
 * or files) and the light-show library (specs/008-lit-bits/contracts/lua-api.md). The program runs as a coroutine: each tick it is resumed
 * until it calls {@code frame()} or {@code sleep()}. A watchdog thread interrupts it when its time for the tick is up, and it is stopped
 * as too slow. No game types, so it can be tested on its own.
 */
public final class LuaShow {
    /** What the show does to the world, given by its Weedweeno. */
    public interface Lamps {
        /** Set every lamp with this ID in range to a colour, 0xRRGGBB. */
        void set(int id, int rgb);

        /** The IDs in range, sorted. */
        List<Integer> ids();

        void print(String line);
    }

    public enum Status { RUNNING, DONE, ERROR, SLOW }

    private static final ScheduledExecutorService WATCHDOG = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FourTwenty Weedweeno watchdog");
        t.setDaemon(true);
        return t;
    });
    private static final Pattern LINE = Pattern.compile("show:(\\d+):");
    private static final Pattern OPENED = Pattern.compile("to close .* at line (\\d+)");
    private static final int MAX_STRING = 1 << 20;
    private static final String PRELUDE = """
            local rep = string.rep
            string.rep = function(s, n, sep)
              local len = #tostring(s) * (tonumber(n) or 0) + #tostring(sep or "") * math.max((tonumber(n) or 0) - 1, 0)
              if len > %d then error("string too long", 2) end
              return rep(s, n, sep)
            end
            local yield = coroutine.yield
            function frame() yield() end
            function sleep(seconds)
              local n = math.max(1, math.floor((tonumber(seconds) or 0) * 20 + 0.5))
              for _ = 1, n do yield() end
            end
            bits = { set = set, hsv = hsv, off = off, all = all, ids = ids, count = count, frame = frame, sleep = sleep,
                     time = time, tick = tick, rgb = rgb, print = print }
            """.formatted(MAX_STRING);

    private final Lamps lamps;
    private final LuaState state;
    private LuaThread thread;
    private volatile long deadline = Long.MAX_VALUE;
    private volatile boolean slow;
    private long ticks;
    private Status status = Status.RUNNING;
    private String message = "";
    private int line;

    private LuaShow(Lamps lamps) throws LuaError {
        this.lamps = lamps;
        this.state = LuaState.builder().interruptHandler(() -> {
            if (System.nanoTime() > deadline) {
                slow = true;
                throw new LuaError("too slow: call frame() in your loop");
            }
            return InterruptAction.CONTINUE;
        }).build();
        CoreLibraries.standardGlobals(state);
        LuaTable g = state.globals();
        RegisteredFunction.bind(g, new RegisteredFunction[]{
                RegisteredFunction.ofV("set", (s, a) -> {
                    lamps.set(a.arg(1).checkInteger(), LedRules.pack(a.arg(2).checkInteger(), a.arg(3).checkInteger(), a.arg(4).checkInteger()));
                    return Constants.NONE;
                }),
                RegisteredFunction.ofV("hsv", (s, a) -> {
                    lamps.set(a.arg(1).checkInteger(), LedRules.hsv(a.arg(2).checkDouble(), a.arg(3).optDouble(1), a.arg(4).optDouble(1)));
                    return Constants.NONE;
                }),
                RegisteredFunction.of("off", (s, id) -> {
                    lamps.set(id.checkInteger(), 0);
                    return Constants.NIL;
                }),
                RegisteredFunction.of("all", (s, r, gr, b) -> {
                    int rgb = LedRules.pack(r.checkInteger(), gr.checkInteger(), b.checkInteger());
                    for (int id : lamps.ids()) lamps.set(id, rgb);
                    return Constants.NIL;
                }),
                RegisteredFunction.of("ids", s -> {
                    List<Integer> ids = lamps.ids();
                    LuaTable t = new LuaTable(ids.size(), 0);
                    for (int i = 0; i < ids.size(); i++) t.rawset(i + 1, ValueFactory.valueOf(ids.get(i)));
                    return t;
                }),
                RegisteredFunction.of("count", s -> ValueFactory.valueOf(lamps.ids().size())),
                RegisteredFunction.of("time", s -> ValueFactory.valueOf(ticks / 20.0)),
                RegisteredFunction.of("tick", s -> ValueFactory.valueOf((double) ticks)),
                RegisteredFunction.ofV("rgb", (s, a) -> {
                    int rgb = LedRules.hsv(a.arg(1).checkDouble(), a.arg(2).optDouble(1), a.arg(3).optDouble(1));
                    return ValueFactory.varargsOf(ValueFactory.valueOf(LedRules.red(rgb)), ValueFactory.valueOf(LedRules.green(rgb)), ValueFactory.valueOf(LedRules.blue(rgb)));
                }),
                RegisteredFunction.ofV("print", (s, a) -> {
                    StringBuilder out = new StringBuilder();
                    for (int i = 1; i <= a.count(); i++) {
                        if (i > 1) out.append('\t');
                        out.append(a.arg(i).toString());
                    }
                    lamps.print(out.length() > 200 ? out.substring(0, 200) : out.toString());
                    return Constants.NONE;
                }),
        });
    }

    /** A new show for {@code code}, ready to run; it fails at once with the line of a syntax error. */
    public static LuaShow start(String code, Lamps lamps) {
        LuaShow show;
        try {
            show = new LuaShow(lamps);
            LuaThread.runMain(show.state, LoadState.load(show.state, stream(PRELUDE), "=prelude", show.state.globals()));
        } catch (LuaError | CompileException e) {
            throw new IllegalStateException("the Weedweeno library failed to load", e);
        }
        try {
            show.thread = new LuaThread(show.state, LoadState.load(show.state, stream(code), "=show", show.state.globals()));
        } catch (CompileException | LuaError e) {
            show.fail(Status.ERROR, e.getMessage());
        }
        return show;
    }

    private static ByteArrayInputStream stream(String code) { return new ByteArrayInputStream(code.getBytes(StandardCharsets.UTF_8)); }

    private void fail(Status why, String text) {
        status = why;
        message = text == null ? "error" : text.lines().findFirst().orElse("error");
        // an unclosed bracket is noticed where the file ends, but Lua says where it opened: point at that
        Matcher opened = OPENED.matcher(message), m = LINE.matcher(message);
        line = opened.find() ? Integer.parseInt(opened.group(1)) : m.find() ? Integer.parseInt(m.group(1)) : 0;
        thread = null;
    }

    /** Runs one frame: resumes the program until it waits for the next one, with {@code budgetNanos} to do it. */
    public Status tick(long budgetNanos) {
        if (status != Status.RUNNING || thread == null) return status;
        slow = false;
        deadline = System.nanoTime() + budgetNanos;
        var alarm = WATCHDOG.schedule(state::interrupt, Math.max(1, budgetNanos), TimeUnit.NANOSECONDS);
        try {
            LuaThread.run(thread, Constants.NONE);
            if (thread.getStatus() == LuaThread.Status.DEAD) {
                status = Status.DONE;
                thread = null;
            }
        } catch (LuaError e) {
            fail(slow ? Status.SLOW : Status.ERROR, slow ? "too slow: call frame() in your loop" : e.getMessage());
        } catch (RuntimeException | StackOverflowError e) {
            fail(Status.ERROR, "the show crashed: " + e.getClass().getSimpleName());
        } finally {
            alarm.cancel(false);
            deadline = Long.MAX_VALUE;
            ticks++;
        }
        return status;
    }

    public Status status() { return status; }

    /** The error or reason it stopped, and its line (0 if none). */
    public String message() { return message; }

    public int line() { return line; }

    public long ticks() { return ticks; }

    /** For tests: whether a global name exists in the show. */
    boolean has(String global) {
        LuaValue v = state.globals().rawget(global);
        return v != Constants.NIL;
    }

    static Varargs none() { return Constants.NONE; }
}

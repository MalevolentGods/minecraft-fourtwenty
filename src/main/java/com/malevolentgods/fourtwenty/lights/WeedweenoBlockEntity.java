package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.BIRules;
import com.malevolentgods.fourtwenty.domain.Programs;
import com.malevolentgods.fourtwenty.power.BIBlockEntity;
import com.malevolentgods.fourtwenty.power.BIBuffer;
import com.malevolentgods.fourtwenty.power.BINode;
import com.malevolentgods.fourtwenty.power.MachinePower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * A Weedweeno: its saved programs, the one running, and what became of it. Each tick it runs one frame of its show, with its time budget,
 * then sends the colours the show set to the Lit Bits within its range by ID. It is a small Puff load, and runs free near a high player
 * like every machine.
 */
public class WeedweenoBlockEntity extends BIBlockEntity {
    public static final List<String> PRESETS = List.of("rainbow", "chase", "pulse", "strobe");
    private static final int CONSOLE = 20;
    /** Ws a tick it draws while a show runs: 5 W. */
    private static final double COST = 0.25;
    private static long spent;

    public enum State { STOPPED, RUNNING, DONE, ERROR, SLOW }

    private final List<Programs.Program> programs = new ArrayList<>(presets());
    private int selected, running = -1;
    private State state = State.STOPPED;
    private String message = "";
    private int line;
    private final Deque<String> console = new ArrayDeque<>();
    private final BIBuffer buffer = new BIBuffer(BIRules.Tier.PUFF, 100, true, false, 5, this::setChanged);
    private final MachinePower.Meter meter = new MachinePower.Meter();
    private final Set<UUID> watchers = new HashSet<>();
    private final Map<Integer, Integer> pending = new LinkedHashMap<>();
    private final Set<Integer> touched = new HashSet<>();
    private LuaShow show;
    private List<Integer> idsThisTick;

    public WeedweenoBlockEntity(BlockPos pos, BlockState state) {
        super(ModLights.WEEDWEENO_ENTITY.get(), pos, state);
    }

    public BINode node() { return buffer; }

    public List<Programs.Program> programs() { return programs; }

    public int selected() { return selected; }

    public int running() { return running; }

    public State state() { return state; }

    public String message() { return message; }

    public int line() { return line; }

    public List<String> console() { return List.copyOf(console); }

    /** The four presets, from the mod's own resources. */
    public static List<Programs.Program> presets() {
        List<Programs.Program> out = new ArrayList<>();
        for (String name : PRESETS) {
            try (InputStream in = WeedweenoBlockEntity.class.getResourceAsStream("/data/fourtwenty/weedweeno/presets/" + name + ".lua")) {
                if (in != null) out.add(new Programs.Program(Character.toUpperCase(name.charAt(0)) + name.substring(1), new String(in.readAllBytes(), StandardCharsets.UTF_8)));
            } catch (IOException ignored) {
                // a missing preset is simply not offered
            }
        }
        return out;
    }

    public static void startTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) { spent = 0; }

    // ---- editing (from the screen; WeedweenoNetwork checks who may) --------------------------------------------------------------

    public void select(int index) {
        if (index >= 0 && index < programs.size()) selected = index;
        changed();
    }

    public boolean save(int index, String name, String code) {
        if (!Programs.fits(code) || index < 0 || index >= programs.size()) return false;
        programs.set(index, new Programs.Program(unique(name, index), code));
        changed();
        return true;
    }

    public void create() {
        programs.add(new Programs.Program(unique("Show", -1), "-- set(id, r, g, b), then frame() every loop\nwhile true do\n  frame()\nend\n"));
        selected = programs.size() - 1;
        changed();
    }

    public void duplicate(int index) {
        if (index < 0 || index >= programs.size()) return;
        programs.add(new Programs.Program(unique(programs.get(index).name(), -1), programs.get(index).code()));
        selected = programs.size() - 1;
        changed();
    }

    public void delete(int index) {
        if (index < 0 || index >= programs.size()) return;
        if (index == running) stop();
        programs.remove(index);
        if (running > index) running--;
        selected = Math.max(0, Math.min(selected, programs.size() - 1));
        if (programs.isEmpty()) create();
        changed();
    }

    public void restorePresets() {
        var names = programs.stream().map(Programs.Program::name).toList();
        for (var p : presets()) if (!names.contains(p.name())) programs.add(p);
        changed();
    }

    /** A program from elsewhere (a Bit Chit), added under a name of its own. */
    public void add(Programs.Program program) {
        programs.add(new Programs.Program(unique(program.name(), -1), program.code()));
        selected = programs.size() - 1;
        changed();
    }

    private String unique(String name, int except) {
        List<String> taken = new ArrayList<>();
        for (int i = 0; i < programs.size(); i++) if (i != except) taken.add(programs.get(i).name());
        return Programs.unique(name, taken);
    }

    public void run(int index) {
        if (index < 0 || index >= programs.size()) return;
        stop();
        running = index;
        selected = index;
        console.clear();
        begin();
    }

    private void begin() {
        show = LuaShow.start(programs.get(running).code(), lamps());
        state = show.status() == LuaShow.Status.RUNNING ? State.RUNNING : State.ERROR;
        message = show.message();
        line = show.line();
        if (state != State.RUNNING) {
            show = null;
            running = -1;
        }
        changed();
    }

    /** Stops the show and turns off every lamp it set. */
    public void stop() {
        if (level instanceof ServerLevel server) {
            var ids = LitBitIds.of(server);
            for (int id : touched) for (BlockPos pos : ids.lampsNear(worldPosition, LightsConfig.range(), id)) lamp(pos, 0);
        }
        touched.clear();
        pending.clear();
        show = null;
        running = -1;
        state = State.STOPPED;
        message = "";
        line = 0;
        changed();
    }

    private LuaShow.Lamps lamps() {
        return new LuaShow.Lamps() {
            @Override public void set(int id, int rgb) {
                pending.put(id, rgb);
                touched.add(id);
            }

            @Override public List<Integer> ids() {
                if (idsThisTick == null) {
                    idsThisTick = level instanceof ServerLevel server ? new ArrayList<>(LitBitIds.of(server).idsNear(worldPosition, LightsConfig.range())) : List.of();
                }
                return idsThisTick;
            }

            @Override public void print(String text) {
                console.addLast(text);
                while (console.size() > CONSOLE) console.removeFirst();
                changed();
            }
        };
    }

    // ---- running ----------------------------------------------------------------------------------------------------------------

    @Override public void serverTick() {
        if (running >= 0 && show == null && running < programs.size() && state == State.RUNNING) begin();
        apply();
        if (show == null) return;
        if (MachinePower.steps(level, worldPosition, buffer, COST, meter) <= 0) return;
        // a show starts its frame only with a full budget left this tick; a frame may run over, up to the whole tick's allowance
        if (LightsConfig.totalNanos() - spent < LightsConfig.budgetNanos()) return;
        idsThisTick = null;
        long start = System.nanoTime();
        LuaShow.Status status = show.tick(LightsConfig.budgetNanos(), LightsConfig.totalNanos());
        spent += System.nanoTime() - start;
        if (status != LuaShow.Status.RUNNING) {
            state = switch (status) {
                case DONE -> State.DONE;
                case SLOW -> State.SLOW;
                default -> State.ERROR;
            };
            message = show.message();
            line = show.line();
            show = null;
            running = -1;
            changed();
        }
        apply();
    }

    /** Sends what the show set to the lamps in range, at most so many a tick; the rest wait. */
    private void apply() {
        if (pending.isEmpty() || !(level instanceof ServerLevel server)) return;
        var ids = LitBitIds.of(server);
        int budget = LightsConfig.lampsPerTick();
        var it = pending.entrySet().iterator();
        while (it.hasNext() && budget > 0) {
            var e = it.next();
            for (BlockPos pos : ids.lampsNear(worldPosition, LightsConfig.range(), e.getKey())) {
                lamp(pos, e.getValue());
                budget--;
            }
            it.remove();
        }
    }

    private void lamp(BlockPos pos, int rgb) {
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof LitBitBlockEntity lamp) lamp.setColour(rgb);
    }

    // ---- the screen's view ------------------------------------------------------------------------------------------------------

    public void watch(ServerPlayer player) { watchers.add(player.getUUID()); }

    public void unwatch(ServerPlayer player) { watchers.remove(player.getUUID()); }

    private void changed() {
        setChanged();
        if (!(level instanceof ServerLevel server)) return;
        watchers.removeIf(id -> {
            ServerPlayer p = server.getServer().getPlayerList().getPlayer(id);
            if (p == null || p.level() != level || p.distanceToSqr(worldPosition.getCenter()) > 64) return true;
            LightsNetwork.sendState(p, this, false);
            return false;
        });
    }

    // ---- saving -----------------------------------------------------------------------------------------------------------------

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (var p : programs) {
            CompoundTag c = new CompoundTag();
            c.putString("name", p.name());
            c.putString("code", p.code());
            list.add(c);
        }
        tag.put("programs", list);
        tag.putInt("selected", selected);
        tag.putInt("running", state == State.RUNNING ? running : -1);
        tag.putLong("ws", buffer.stored());
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("programs")) {
            programs.clear();
            for (Tag t : tag.getList("programs", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                programs.add(new Programs.Program(c.getString("name"), c.getString("code")));
            }
            if (programs.isEmpty()) programs.addAll(presets());
        }
        selected = Math.max(0, Math.min(tag.getInt("selected"), programs.size() - 1));
        running = tag.contains("running") ? tag.getInt("running") : -1;
        // a show that was running starts again from the top
        state = running >= 0 && running < programs.size() ? State.RUNNING : State.STOPPED;
        if (state != State.RUNNING) running = -1;
        buffer.set(tag.getLong("ws"));
    }

    @Override protected void collectImplicitComponents(@Nonnull DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModLights.PROGRAMS.get(), List.copyOf(programs));
    }

    @Override protected void applyImplicitComponents(@Nonnull net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput input) {
        super.applyImplicitComponents(input);
        var saved = input.get(ModLights.PROGRAMS.get());
        if (saved != null && !saved.isEmpty()) {
            programs.clear();
            programs.addAll(saved);
        }
    }

    @Override public void removeComponentsFromTag(@Nonnull CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("programs");
    }
}

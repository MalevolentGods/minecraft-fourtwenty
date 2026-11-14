package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * Per-Builder bookkeeping for the crawl (FR-001, FR-002): step-asides waiting to be acted on, the last placement, and
 * the diagnostics log lines (on with {@code logBuilderPathing}). The watchdog reads the placement times too.
 */
public final class BuilderDiagnostics {
    private BuilderDiagnostics() {}

    private static final Map<Entity, Boolean> STEPPED_ASIDE = new WeakHashMap<>();
    private static final Map<Entity, Long> LAST_PLACED = new WeakHashMap<>();
    private static final Map<Entity, Long> PLACEMENTS = new WeakHashMap<>();

    public static synchronized void steppedAside(Entity worker, BlockPos block) {
        STEPPED_ASIDE.put(worker, Boolean.TRUE);
        BuilderLog.diag("stepaside citizen={} block={}", worker.getName().getString(), block.toShortString());
    }

    /** Whether she stepped aside since this was last asked (and clears it). */
    public static synchronized boolean takeStepAside(Entity worker) {
        return STEPPED_ASIDE.remove(worker) != null;
    }

    public static synchronized void placed(Entity worker, @Nullable BlockPos block) {
        long now = worker.level().getGameTime();
        Long last = LAST_PLACED.put(worker, now);
        PLACEMENTS.merge(worker, 1L, Long::sum);
        BuilderLog.diag("placed citizen={} block={} sinceLast={}", worker.getName().getString(),
                block == null ? "?" : block.toShortString(), last == null ? "-" : String.valueOf(now - last));
    }

    /** Blocks she has placed since the server started (the watchdog samples it). */
    public static synchronized long placements(Entity worker) {
        return PLACEMENTS.getOrDefault(worker, 0L);
    }

    public static void workFromChanged(Entity worker, @Nullable BlockPos old, @Nullable BlockPos now, String why) {
        if (Objects.equals(old, now)) return;
        BuilderLog.diag("workfrom citizen={} old={} new={} why={}", worker.getName().getString(),
                old == null ? "none" : old.toShortString(), now == null ? "none" : now.toShortString(), why);
    }

    public static void routeSearch(Entity worker, @Nullable BlockPos target) {
        BuilderLog.diag("path citizen={} job=closeTo target={} from={}", worker.getName().getString(),
                target == null ? "?" : target.toShortString(), worker.blockPosition().toShortString());
    }
}

package com.malevolentgods.fourtwenty.compat.minecolonies.builder;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** The builder log lines of specs/013-lifted-builders/contracts/config.md, under the logger FourTwenty/Builders. */
public final class BuilderLog {
    private BuilderLog() {}

    public static final Logger LOG = LogUtils.getLogger();
    private static final String P = "[FourTwenty/Builders] ";

    public static void rescue(String step, int colony, String citizen, Object building, Object at, Object next) {
        LOG.info(P + "rescue {} colony={} citizen={} building={} at={} next={}", step, colony, citizen, building, at, next);
    }

    public static void lifted(int colony, String citizen, Object strain, double minutes) {
        LOG.info(P + "lifted colony={} citizen={} strain={} minutes={}", colony, citizen, strain, String.format(java.util.Locale.ROOT, "%.1f", minutes));
    }

    public static void landed(int colony, String citizen, String reason) {
        LOG.info(P + "landed colony={} citizen={} reason={}", colony, citizen, reason);
    }

    public static void flightStall(String citizen, Object at, Object next) {
        LOG.info(P + "flightstall citizen={} at={} next={}", citizen, at, next);
    }

    public static void diag(String line, Object... args) {
        if (BuilderConfig.logBuilderPathing()) LOG.info(P + line, args);
    }

    private static final java.util.Set<String> MISSING = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** A MineColonies hook that didn't apply: warn once per name, so an update switches the part off instead of crashing. */
    public static void missing(String name, Throwable why) {
        if (MISSING.add(name)) LOG.warn(P + "MineColonies hook {} unavailable ({}); that part of the Builder changes is off", name, why.toString());
    }
}

package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

/** Where the Dispensary's blueprints live, and how a hut built from the old FourTwenty pack finds its way to them. */
public final class DispensaryPackRules {
    public static final String PACK = "Sensi Heights", PATH = "craftsmanship/luxury/dispensary";
    private static final String OLD_PACK = "FourTwenty", OLD_PATH = "dispensary/dispensary";

    public static boolean isOldPack(String pack) { return OLD_PACK.equals(pack); }

    /** The same level's blueprint in Sensi Heights: dispensary/dispensary5.blueprint to craftsmanship/luxury/dispensary5.blueprint. */
    public static String newPath(String path) { return path.replace(OLD_PATH, PATH); }

    private DispensaryPackRules() {}
}

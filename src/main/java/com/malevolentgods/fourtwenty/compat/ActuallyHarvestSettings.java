package com.malevolentgods.fourtwenty.compat;

import java.lang.reflect.Method;

/** Only called from the optional Actually Harvest target after that mod has initialized. */
public final class ActuallyHarvestSettings {
    private ActuallyHarvestSettings() {}

    public static boolean replants() {
        try { return (boolean) Access.REPLANTS.invoke(null); }
        catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Actually Harvest replant configuration API changed", error);
        }
    }

    private static final class Access {
        static final Method REPLANTS;
        static {
            try {
                REPLANTS = Class.forName("actuallyharvest.config.ConfigHandler$Common").getMethod("replantCrops");
            } catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
        }
    }
}

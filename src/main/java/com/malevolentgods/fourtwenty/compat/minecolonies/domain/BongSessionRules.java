package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

/**
 * Timing for the "bong session" happiness bonus: colonists near a burning bong stay happier for
 * {@code durationTicks} after they were last nearby. Pure so it can be unit tested.
 */
public final class BongSessionRules {
    /** Re-save an active bonus at most this often while a citizen stays near a burning bong. */
    public static final int EXTEND_EVERY_TICKS = 200;

    public static boolean active(long expires, long now) {
        return now < expires;
    }

    /** True when a fresh exposure should be written: the bonus is off, or has run down by a full extension step. */
    public static boolean shouldRefresh(long expires, long now, int durationTicks) {
        return !active(expires, now) || now + durationTicks - expires >= EXTEND_EVERY_TICKS;
    }

    private BongSessionRules() {}
}

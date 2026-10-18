package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.domain.BongVariety;

/**
 * A colonist's one-day specialty bong buff: +{@code bonus} levels to the variety's two skills until
 * {@code until} (dispensary simulation time).
 */
public record ColonistBuff(BongVariety variety, long until, int bonus) {
    public ColonistBuff {
        if (variety == null || !variety.specialty() || until < 0 || bonus < 1 || bonus > 99) {
            throw new IllegalArgumentException("Invalid colonist buff");
        }
    }
    public boolean active(long now) { return now < until; }
}

package com.malevolentgods.fourtwenty.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightFalloffTest {
    @Test
    void theFilmFadesSmoothlyToNothing() {
        assertEquals(1f, LightFalloff.film(0, 9));
        assertEquals(0f, LightFalloff.film(9, 9));
        assertEquals(0f, LightFalloff.film(20, 9));
        assertTrue(LightFalloff.film(3, 9) > LightFalloff.film(6, 9));
    }
}

package com.malevolentgods.fourtwenty.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.malevolentgods.fourtwenty.domain.MachinePowerRule.State;
import org.junit.jupiter.api.Test;

class MachinePowerRuleTest {
    @Test
    void aHighPlayerRunsItFreeWhateverIsStored() {
        assertEquals(State.RUN_FREE, MachinePowerRule.state(true, 0, 16));
        assertEquals(State.RUN_FREE, MachinePowerRule.state(true, 1000, 16));
    }

    @Test
    void soberItPaysInBI() {
        assertEquals(State.RUN_PAID, MachinePowerRule.state(false, 16, 16));
    }

    @Test
    void withNeitherItPauses() {
        assertEquals(State.PAUSE, MachinePowerRule.state(false, 15, 16));
    }

    @Test
    void freeRunningIsTheSameSpeedByDefault() {
        for (int roll = 0; roll < 100; roll++) assertEquals(1, MachinePowerRule.freeSteps(100, roll));
    }

    @Test
    void aSlowerOrFasterFreeSpeedAveragesOut() {
        int steps = 0;
        for (int roll = 0; roll < 100; roll++) steps += MachinePowerRule.freeSteps(75, roll);
        assertEquals(75, steps);
        steps = 0;
        for (int roll = 0; roll < 100; roll++) steps += MachinePowerRule.freeSteps(125, roll);
        assertEquals(125, steps);
    }
}

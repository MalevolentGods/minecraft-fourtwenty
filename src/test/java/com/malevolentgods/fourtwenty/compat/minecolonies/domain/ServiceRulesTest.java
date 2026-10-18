package com.malevolentgods.fourtwenty.compat.minecolonies.domain;

import com.malevolentgods.fourtwenty.compat.minecolonies.domain.ServiceRules.Plan;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static com.malevolentgods.fourtwenty.compat.minecolonies.domain.MenuItem.*;
import static org.junit.jupiter.api.Assertions.*;

class ServiceRulesTest {
    private static final Map<MenuItem, Integer> FULL = Map.of(JOINT, 10, COOKIE, 10, PIPE, 3, BROWNIE, 10);
    private static final Map<MenuItem, Integer> JOINTS_ONLY = Map.of(JOINT, 10);

    @Test void lowHomesConsumeAtTheShop() {
        assertEquals(new Plan(JOINT, null, 0, false), ServiceRules.plan(1, Preference.SMOKE, FULL));
        assertEquals(new Plan(BROWNIE, null, 0, false), ServiceRules.plan(2, Preference.EDIBLE, FULL));
        assertEquals(new Plan(COOKIE, null, 0, false), ServiceRules.plan(2, Preference.EDIBLE, Map.of(COOKIE, 1, JOINT, 5)));
    }
    @Test void midHomesCarryASupplyAway() {
        assertEquals(new Plan(null, PIPE, 1, false), ServiceRules.plan(3, Preference.SMOKE, FULL));
        assertEquals(new Plan(null, BROWNIE, 4, false), ServiceRules.plan(4, Preference.EDIBLE, FULL));
        assertEquals(new Plan(null, COOKIE, 2, false), ServiceRules.plan(3, Preference.EDIBLE, Map.of(COOKIE, 2)));
    }
    @Test void noTakeAwayMeansAJointAtTheShopAndAComplaint() {
        assertEquals(new Plan(JOINT, null, 0, true), ServiceRules.plan(3, Preference.SMOKE, JOINTS_ONLY));
        assertEquals(new Plan(JOINT, null, 0, true), ServiceRules.plan(5, Preference.SMOKE, JOINTS_ONLY));
        assertEquals(Plan.NOTHING, ServiceRules.plan(4, Preference.EDIBLE, JOINTS_ONLY));
    }
    @Test void topHomesHaveASessionAndTakeAPipeHome() {
        assertEquals(new Plan(JOINT, PIPE, 1, false), ServiceRules.plan(5, Preference.SMOKE, FULL));
        // The same item for both: the session's brownie is not double-counted for the take-away.
        assertEquals(new Plan(BROWNIE, BROWNIE, 4, false), ServiceRules.plan(5, Preference.EDIBLE, Map.of(BROWNIE, 5)));
        assertEquals(new Plan(BROWNIE, BROWNIE, 3, false), ServiceRules.plan(5, Preference.EDIBLE, Map.of(BROWNIE, 4)));
        assertEquals(new Plan(null, PIPE, 1, false), ServiceRules.plan(5, Preference.SMOKE, Map.of(PIPE, 1)));
    }
    @Test void bothTakesTheHighestLevelItemInStock() {
        assertEquals(new Plan(null, BROWNIE, 4, false), ServiceRules.plan(3, Preference.BOTH, FULL));
        assertEquals(new Plan(null, PIPE, 1, false), ServiceRules.plan(3, Preference.BOTH, Map.of(PIPE, 1, COOKIE, 9, JOINT, 9)));
        assertEquals(new Plan(COOKIE, null, 0, false), ServiceRules.plan(1, Preference.BOTH, Map.of(COOKIE, 1, JOINT, 9)));
        assertEquals(new Plan(JOINT, null, 0, false), ServiceRules.plan(2, Preference.BOTH, JOINTS_ONLY));
    }
    @Test void nothingForTheUninterestedTheHomelessOrAnEmptyShop() {
        assertEquals(Plan.NOTHING, ServiceRules.plan(5, Preference.NONE, FULL));
        assertEquals(Plan.NOTHING, ServiceRules.plan(0, Preference.SMOKE, FULL));
        assertEquals(Plan.NOTHING, ServiceRules.plan(3, Preference.SMOKE, Map.of()));
    }
}

package com.malevolentgods.fourtwenty.compat.minecolonies.placement;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class BuilderCostSubstitutionsTest {
    @Test void everyFroglightCostsASeaLantern() {
        for (String froglight : new String[] {"minecraft:verdant_froglight", "minecraft:ochre_froglight", "minecraft:pearlescent_froglight"}) {
            assertEquals(Optional.of("minecraft:sea_lantern"), BuilderCostSubstitutions.costFor(froglight), froglight);
        }
    }
    @Test void theArchitectsCutterCostsOneCobblestone() {
        assertEquals(Optional.of("minecraft:cobblestone"), BuilderCostSubstitutions.costFor("domum_ornamentum:architectscutter"));
    }
    @Test void ordinaryBlocksCostThemselves() {
        assertEquals(Optional.empty(), BuilderCostSubstitutions.costFor("minecraft:sea_lantern"));
        assertEquals(Optional.empty(), BuilderCostSubstitutions.costFor("minecraft:glowstone"));
        assertEquals(Optional.empty(), BuilderCostSubstitutions.costFor("fourtwenty:dispensary"));
    }
}

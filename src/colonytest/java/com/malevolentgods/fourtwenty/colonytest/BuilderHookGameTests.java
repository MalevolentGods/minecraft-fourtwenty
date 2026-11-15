package com.malevolentgods.fourtwenty.colonytest;

import com.malevolentgods.fourtwenty.compat.minecolonies.mixin.BuilderAIAccessor;
import com.malevolentgods.fourtwenty.compat.minecolonies.mixin.BuilderPathAccessor;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Builder hooks (spec 013) are optional injections that switch themselves off if MineColonies changes. This proves
 * each one actually landed in the MineColonies version we run, so a silent miss fails here instead of in a colony.
 */
@GameTestHolder("fourtwenty_colonytests")
@PrefixGameTestTemplate(false)
public final class BuilderHookGameTests {
    private static boolean injected(Class<?> target, String handler) {
        return Arrays.stream(target.getDeclaredMethods()).anyMatch(m -> m.getName().contains(handler));
    }

    @GameTest(template = "empty")
    public static void everyBuilderHookLandsInMineColonies(GameTestHelper h) {
        List<String> missing = new ArrayList<>();
        var builderAI = com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder.class;
        var structureAI = com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure.class;
        if (!BuilderAIAccessor.class.isAssignableFrom(structureAI)) missing.add("BuilderAIAccessor");
        if (!BuilderPathAccessor.class.isAssignableFrom(builderAI)) missing.add("BuilderPathAccessor");
        String[][] hooks = {
                {"com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder", "fourtwenty$beforeWalk"},
                {"com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder", "fourtwenty$afterWalk"},
                {"com.minecolonies.core.entity.ai.workers.util.BuildingStructureHandler", "fourtwenty$noteStepAside"},
                {"com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure", "fourtwenty$placed"},
                {"com.minecolonies.api.colony.buildings.registry.BuildingEntry", "fourtwenty$flyWhenLifted"},
                {"com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure", "fourtwenty$liftedSpeed"},
                {"com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure", "fourtwenty$emptyFirst"},
                {"com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure", "fourtwenty$mineFromHere"},
                {"com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate", "fourtwenty$fly"},
                {"com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate", "fourtwenty$done"},
                {"com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate", "fourtwenty$noWalking"},
                {"com.minecolonies.core.entity.pathfinding.navigation.MinecoloniesAdvancedPathNavigate", "fourtwenty$stop"},
        };
        for (String[] hook : hooks) {
            try {
                if (!injected(Class.forName(hook[0]), hook[1])) missing.add(hook[0].substring(hook[0].lastIndexOf('.') + 1) + "." + hook[1]);
            } catch (ClassNotFoundException e) {
                missing.add(hook[0]);
            }
        }
        h.assertTrue(missing.isEmpty(), "Builder hooks that didn't land: " + missing);
        h.succeed();
    }
}

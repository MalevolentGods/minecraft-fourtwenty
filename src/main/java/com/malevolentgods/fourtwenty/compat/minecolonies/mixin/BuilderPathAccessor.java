package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.minecolonies.core.entity.ai.workers.builder.EntityAIStructureBuilder;
import com.minecolonies.core.entity.pathfinding.pathresults.PathResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Builder AI's pending route search, for {@code BuilderAccess} only. */
@Mixin(value = EntityAIStructureBuilder.class, remap = false)
public interface BuilderPathAccessor {
    @Accessor("gotoPath") PathResult<?> fourtwenty$gotoPath();
    @Accessor("gotoPath") void fourtwenty$gotoPath(PathResult<?> path);
}

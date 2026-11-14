package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.minecolonies.core.entity.ai.workers.AbstractEntityAIStructure;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The structure AI's standing spot and progress fields, for {@code BuilderAccess} only. */
@Mixin(value = AbstractEntityAIStructure.class, remap = false)
public interface BuilderAIAccessor {
    @Accessor("workFrom") BlockPos fourtwenty$workFrom();
    @Accessor("workFrom") void fourtwenty$workFrom(BlockPos pos);
    @Accessor("prevBlockPosition") void fourtwenty$prevBlockPosition(BlockPos pos);
    /** The world position of the block she's working on next (the work order's location when none). */
    @Invoker("getPosToWorkAt") BlockPos fourtwenty$posToWorkAt();
}

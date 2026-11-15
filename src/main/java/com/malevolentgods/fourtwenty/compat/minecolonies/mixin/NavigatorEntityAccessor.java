package com.malevolentgods.fourtwenty.compat.minecolonies.mixin;

import com.minecolonies.core.entity.pathfinding.navigation.AbstractAdvancedPathNavigate;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The citizen a MineColonies navigator moves: for the Lifted takeover (NavigatorTakeoverMixin). */
@Mixin(value = AbstractAdvancedPathNavigate.class, remap = false)
public interface NavigatorEntityAccessor {
    @Accessor("ourEntity")
    Mob fourtwenty$entity();
}

package com.malevolentgods.fourtwenty.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.lang.reflect.Method;

/** Consult the optional colony's native rules without dispatching a second harvest event. */
public final class ColonyHarvestPermission {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean reportedFailure;

    private ColonyHarvestPermission() {}

    public static boolean allows(Player player, Level level, BlockPos pos, InteractionHand hand) {
        if (!ModList.get().isLoaded("minecolonies")) return true;
        try {
            Object manager = Bridge.INSTANCE.invoke(null);
            Object colony = Bridge.COLONY_AT.invoke(manager, level, pos);
            if (colony == null) return true;
            var event = new PlayerInteractEvent.RightClickBlock(player, hand == null ? InteractionHand.MAIN_HAND : hand,
                pos, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            Object handler = Bridge.EVENT_HANDLER.invoke(colony);
            Bridge.RIGHT_CLICK.invoke(handler, event);
            return !event.isCanceled() && event.getUseBlock() != net.neoforged.neoforge.common.util.TriState.FALSE;
        } catch (ReflectiveOperationException | LinkageError error) {
            if (!reportedFailure) {
                LOGGER.error("Cannot verify MineColonies crop permission; denying FourTwenty automated right-click harvesting", error);
                reportedFailure = true;
            }
            return false;
        }
    }

    /** Loaded only after the mod-presence check. No optional classes enter core signatures. */
    private static final class Bridge {
        static final Method INSTANCE;
        static final Method COLONY_AT;
        static final Method EVENT_HANDLER;
        static final Method RIGHT_CLICK;
        static {
            try {
                var manager = Class.forName("com.minecolonies.api.colony.IColonyManager");
                INSTANCE = manager.getMethod("getInstance");
                COLONY_AT = manager.getMethod("getColonyByPosFromWorld", Level.class, BlockPos.class);
                EVENT_HANDLER = Class.forName("com.minecolonies.core.colony.Colony").getMethod("getEventHandler");
                RIGHT_CLICK = Class.forName("com.minecolonies.core.colony.permissions.ColonyPermissionEventHandler")
                    .getMethod("onRightClickBlock", PlayerInteractEvent.RightClickBlock.class);
            } catch (ReflectiveOperationException error) { throw new ExceptionInInitializerError(error); }
        }
    }
}

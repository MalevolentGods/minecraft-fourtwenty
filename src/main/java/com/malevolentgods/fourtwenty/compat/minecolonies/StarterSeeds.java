package com.malevolentgods.fourtwenty.compat.minecolonies;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * A new player's first weed (specs/012-progression-balance): Landrace seeds once, on their first login with MineColonies, in hand as
 * they place their supply camp. Remembered in the player's persistent data, so it survives death and restarts.
 */
public final class StarterSeeds {
    public static final String FLAG = "fourtwenty.starterSeeds";

    private StarterSeeds() {}

    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) give(player, DispensaryConfig.starterSeeds());
    }

    /** Gives {@code count} Landrace seeds unless this player has had theirs; true if it gave them now. */
    public static boolean give(Player player, int count) {
        CompoundTag kept = keptData(player);
        if (kept.getBoolean(FLAG)) return false;
        kept.putBoolean(FLAG, true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        if (count <= 0) return false;
        ItemStack seeds = ModDataComponents.withStrain(new ItemStack(ModItems.WEED_SEEDS.get(), count), Strain.LANDRACE);
        if (!player.getInventory().add(seeds)) player.drop(seeds, false);
        return true;
    }

    /** The part of a player's persistent data that is copied across death. */
    private static CompoundTag keptData(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(Player.PERSISTED_NBT_TAG) ? data.getCompound(Player.PERSISTED_NBT_TAG) : new CompoundTag();
    }
}

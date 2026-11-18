package com.malevolentgods.fourtwenty.handbook;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * A Handbook of Weedcraft, Scouting, and Weed-craft (specs/016-weedcraft-handbook FR-001): using it opens the book.
 * The book is read entirely on the player's side; the client installs {@link #opener}.
 */
public class HandbookItem extends Item {
    /** Opens the book screen; set by the client at startup, nothing on a server. */
    public static volatile Runnable opener = () -> {};

    public HandbookItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        if (level.isClientSide()) opener.run();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}

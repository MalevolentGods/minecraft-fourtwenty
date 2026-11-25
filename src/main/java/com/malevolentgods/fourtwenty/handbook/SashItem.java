package com.malevolentgods.fourtwenty.handbook;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;

/**
 * The Stoner Badge Sash (specs/016-weedcraft-handbook FR-035 to FR-040): a cloth sash worn across the chest that shows the wearer's
 * earned badges as patches. Purely cosmetic: no armour, no effects, and no data of its own, so it always shows the badges of whoever
 * wears it.
 *
 * <p>An accessory, never armour (FR-038): it has its own slot ({@link SashSlot}), filled by right-clicking the sash or through the
 * sash slot beside the player's figure in the inventory, so a chestplate stays on. With Curios installed it can also go in Curios'
 * body slot.
 */
public class SashItem extends Item {
    public SashItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canEquip(@Nonnull ItemStack stack, @Nonnull EquipmentSlot slot, @Nonnull LivingEntity entity) {
        return false;
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide) {
            return SashSlot.CLIENT_WORN.contains(player.getId()) ? InteractionResultHolder.pass(held) : InteractionResultHolder.success(held);
        }
        return player instanceof ServerPlayer sp && SashSlot.wear(sp, held) ? InteractionResultHolder.consume(held) : InteractionResultHolder.pass(held);
    }
}

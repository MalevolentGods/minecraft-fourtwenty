package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.util.JointSmoke;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/** A joint of one strain: smoking it gives that strain's effects, in its colour. */
public class WeedJointItem extends Item {
    public WeedJointItem(Properties properties) {
        super(properties);
    }

    @Override
    public @Nonnull ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        Strain strain = ModDataComponents.strain(stack);
        if (level instanceof ServerLevel server && entity instanceof Player player) StrainEffects.apply(server, player, strain, ModDataComponents.traits(stack));
        JointSmoke.puff(entity, 5, strain);
        stack.shrink(1);
        return stack;
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        JointSmoke.puff(player, 2, ModDataComponents.strain(itemstack));
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public void onUseTick(@Nonnull Level level, @Nonnull LivingEntity entity, @Nonnull ItemStack stack, int remainingUseDuration) {
        JointSmoke.tick(entity, getUseDuration(stack, entity) - remainingUseDuration, ModDataComponents.strain(stack));
    }

    @Override
    public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) {
        return 32;
    }

    @Override
    public @Nonnull Component getName(@Nonnull ItemStack stack) { return StrainItems.name(stack, super.getName(stack), getDescriptionId()); }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        StrainItems.tooltip(stack, lines);
    }
}

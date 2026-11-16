package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.domain.Extracts;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.domain.VapeRules;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModDataComponents.PipeLoad;
import com.malevolentgods.fourtwenty.util.JointSmoke;
import com.malevolentgods.fourtwenty.util.StrainEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The Vape Pen (user, 2026-11-15): loaded with Oil like a pipe with buds (right-click Oil onto it in the inventory, or
 * it onto Oil), one strain at a time. One Oil is {@link VapeRules#HITS_PER_OIL} hits, each a full high with the Oil's
 * extract boost, so it lasts far longer than a pipe. No power. Its load is kept in the pipe's load component, counting
 * hits instead of buds.
 */
public class VapePenItem extends Item {
    public VapePenItem(Properties properties) {
        super(properties);
    }

    public static PipeLoad load(ItemStack pen) {
        PipeLoad l = pen.get(ModDataComponents.PIPE_LOAD.get());
        return l == null ? new PipeLoad(Strain.LANDRACE, 0, Traits.DEFAULT) : l;
    }

    private static boolean oil(ItemStack stack) {
        return !stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(VapeRules.OIL);
    }

    /** Load Oil from {@code oil} into the pen; returns how many Oils went in. */
    public static int fill(ItemStack pen, ItemStack oil) {
        if (!oil(oil)) return 0;
        PipeLoad l = load(pen);
        Strain s = ModDataComponents.strain(oil);
        Traits t = Extracts.boosted(VapeRules.OIL, ModDataComponents.traits(oil));
        if (l.buds() > 0 && (l.strain() != s || !l.traits().equals(t))) return 0;
        int n = VapeRules.oilsThatFit(l.buds(), oil.getCount());
        if (n <= 0) return 0;
        pen.set(ModDataComponents.PIPE_LOAD.get(), new PipeLoad(s, l.buds() + n * VapeRules.HITS_PER_OIL, t));
        oil.shrink(n);
        return n;
    }

    // The pen on the cursor, right-clicked onto a slot of Oil.
    @Override
    public boolean overrideStackedOnOther(@Nonnull ItemStack pen, @Nonnull Slot slot, @Nonnull ClickAction action, @Nonnull Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack oil = slot.getItem();
        if (!oil(oil)) return false;
        ItemStack taken = oil.copy();
        if (fill(pen, taken) > 0) { slot.set(taken); loadSound(player); }
        return true;
    }

    // Oil on the cursor, right-clicked onto the pen.
    @Override
    public boolean overrideOtherStackedOnMe(@Nonnull ItemStack pen, @Nonnull ItemStack other, @Nonnull Slot slot, @Nonnull ClickAction action,
                                            @Nonnull Player player, @Nonnull SlotAccess access) {
        if (action != ClickAction.SECONDARY || !oil(other)) return false;
        if (fill(pen, other) > 0) loadSound(player);
        return true;
    }

    private static void loadSound(Player player) {
        player.playSound(SoundEvents.BOTTLE_FILL, 0.6f, 1.3f + player.level().getRandom().nextFloat() * 0.2f);
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack pen = player.getItemInHand(hand);
        if (load(pen).buds() <= 0 && !player.getAbilities().instabuild) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("message.fourtwenty.vape_pen_empty"), true);
            return InteractionResultHolder.fail(pen);
        }
        JointSmoke.puff(player, 4, load(pen).strain());
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(pen);
    }

    @Override
    public void onUseTick(@Nonnull Level level, @Nonnull LivingEntity entity, @Nonnull ItemStack stack, int remainingUseDuration) {
        JointSmoke.tick(entity, getUseDuration(stack, entity) - remainingUseDuration, load(stack).strain());
    }

    @Override
    public @Nonnull ItemStack finishUsingItem(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity) {
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            PipeLoad l = load(stack);
            if (l.buds() > 0 || player.getAbilities().instabuild) {
                StrainEffects.apply(server, player, l.strain(), l.traits());
                if (!player.getAbilities().instabuild) {
                    if (l.buds() <= 1) stack.remove(ModDataComponents.PIPE_LOAD.get());
                    else stack.set(ModDataComponents.PIPE_LOAD.get(), new PipeLoad(l.strain(), l.buds() - 1, l.traits()));
                }
                player.getCooldowns().addCooldown(this, 60);
                level.playSound(null, player, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.12f, 1.8f);
            }
        }
        return stack;
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
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return stack.has(ModDataComponents.PIPE_LOAD.get());
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        return Mth.clamp(Math.round(13f * load(stack).buds() / VapeRules.CAPACITY), 1, 13);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        return load(stack).strain().color();
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        PipeLoad l = load(stack);
        if (l.buds() > 0) lines.add(Component.translatable("tooltip.fourtwenty.vape_pen_load", l.buds(), VapeRules.CAPACITY, StrainItems.strainName(l.strain())));
        else lines.add(Component.translatable("tooltip.fourtwenty.vape_pen_empty"));
    }
}

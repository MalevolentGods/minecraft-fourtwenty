package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.domain.Extracts;
import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.Traits;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModDataComponents.PipeLoad;
import com.malevolentgods.fourtwenty.registry.ModItems;
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
 * A pipe you load with buds like a bundle (right-click buds onto it in the inventory, or it onto buds): one strain at a
 * time, up to {@link PipeLoad#CAPACITY} buds, one bud per hit. Colonists don't load pipes: theirs still wear out by
 * durability (the Dispensary's take-away pipe), so the durability stays on the item.
 */
public class WeedPipeItem extends Item {
    public WeedPipeItem(Properties properties) {
        super(properties);
    }

    public static PipeLoad load(ItemStack pipe) {
        PipeLoad l = pipe.get(ModDataComponents.PIPE_LOAD.get());
        return l == null ? new PipeLoad(Strain.LANDRACE, 0, Traits.DEFAULT) : l;
    }

    private static boolean smokable(ItemStack stack) {
        return stack.is(ModItems.WEED_BUD.get()) || Extracts.byId(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()).isPresent();
    }

    /** Load buds or extracts from {@code buds} into the pipe; returns how many went in. */
    public static int fill(ItemStack pipe, ItemStack buds) {
        if (!smokable(buds)) return 0;
        PipeLoad l = load(pipe);
        Strain s = ModDataComponents.strain(buds);
        Traits t = Extracts.boosted(BuiltInRegistries.ITEM.getKey(buds.getItem()).toString(), ModDataComponents.traits(buds));
        if (l.buds() > 0 && (l.strain() != s || !l.traits().equals(t))) return 0;
        int n = Math.min(buds.getCount(), PipeLoad.CAPACITY - l.buds());
        if (n <= 0) return 0;
        pipe.set(ModDataComponents.PIPE_LOAD.get(), new PipeLoad(s, l.buds() + n, t));
        buds.shrink(n);
        return n;
    }

    // The pipe on the cursor, right-clicked onto a slot of buds.
    @Override
    public boolean overrideStackedOnOther(@Nonnull ItemStack pipe, @Nonnull Slot slot, @Nonnull ClickAction action, @Nonnull Player player) {
        if (action != ClickAction.SECONDARY) return false;
        ItemStack buds = slot.getItem();
        if (!smokable(buds)) return false;
        ItemStack taken = buds.copy();
        int n = fill(pipe, taken);
        if (n > 0) { slot.set(taken); loadSound(player); }
        return true;
    }

    // Buds on the cursor, right-clicked onto the pipe.
    @Override
    public boolean overrideOtherStackedOnMe(@Nonnull ItemStack pipe, @Nonnull ItemStack other, @Nonnull Slot slot, @Nonnull ClickAction action,
                                            @Nonnull Player player, @Nonnull SlotAccess access) {
        if (action != ClickAction.SECONDARY || !smokable(other)) return false;
        if (fill(pipe, other) > 0) loadSound(player);
        return true;
    }

    private static void loadSound(Player player) {
        player.playSound(SoundEvents.BUNDLE_INSERT, 0.8f, 0.9f + player.level().getRandom().nextFloat() * 0.2f);
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack pipe = player.getItemInHand(hand);
        if (load(pipe).buds() <= 0 && !player.getAbilities().instabuild) {
            if (!level.isClientSide()) player.displayClientMessage(Component.translatable("message.fourtwenty.pipe_empty"), true);
            return InteractionResultHolder.fail(pipe);
        }
        JointSmoke.puff(player, 4, load(pipe).strain());
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(pipe);
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
                level.playSound(null, player, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.15f, 1.6f);
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

    // The bar shows the load (a colonist's worn pipe still shows its wear).
    @Override
    public boolean isBarVisible(@Nonnull ItemStack stack) {
        return stack.has(ModDataComponents.PIPE_LOAD.get()) || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(@Nonnull ItemStack stack) {
        if (!stack.has(ModDataComponents.PIPE_LOAD.get())) return super.getBarWidth(stack);
        return Mth.clamp(Math.round(13f * load(stack).buds() / PipeLoad.CAPACITY), 1, 13);
    }

    @Override
    public int getBarColor(@Nonnull ItemStack stack) {
        return stack.has(ModDataComponents.PIPE_LOAD.get()) ? load(stack).strain().color() : super.getBarColor(stack);
    }

    @Override
    public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        PipeLoad l = load(stack);
        if (l.buds() > 0) lines.add(Component.translatable("tooltip.fourtwenty.pipe_load", l.buds(), PipeLoad.CAPACITY, StrainItems.strainName(l.strain())));
        else lines.add(Component.translatable("tooltip.fourtwenty.pipe_empty"));
    }

    @Override
    public boolean isValidRepairItem(@Nonnull ItemStack toRepair, @Nonnull ItemStack repair) {
        return repair.getItem() == ModItems.WEED_BUD.get();
    }
}

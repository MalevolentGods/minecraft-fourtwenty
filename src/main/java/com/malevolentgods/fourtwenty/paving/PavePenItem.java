package com.malevolentgods.fourtwenty.paving;

import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModDataComponents.PenState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The Pave Pen and Pave Pen Pro. Hold right-click on a block to pave it; sneak and right-click, or press the pen's key, for its settings.
 * The Pro also lays and reshapes road slabs and stairs.
 */
public class PavePenItem extends Item {
    /** Opens the settings screen for the hand holding the pen. Set by the client; does nothing on a server. */
    public static java.util.function.Consumer<InteractionHand> clientOpen = hand -> {};
    public static final int BAR = 0x60C448, PRO_BAR = 0x2FD07A;

    private final boolean pro;

    public PavePenItem(boolean pro, Properties properties) {
        super(properties.stacksTo(1));
        this.pro = pro;
    }

    public boolean pro() { return pro; }

    public static boolean isPro(ItemStack stack) { return stack.getItem() instanceof PavePenItem pen && pen.pro; }

    public static PenState state(ItemStack stack) {
        PenState s = stack.get(ModDataComponents.PEN.get());
        return s == null ? PenState.DEFAULT : s;
    }

    public static void set(ItemStack stack, PenState state) { stack.set(ModDataComponents.PEN.get(), state); }

    @Override public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (context.getLevel().isClientSide) clientOpen.accept(context.getHand());
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        }
        player.startUsingItem(context.getHand());
        return InteractionResult.CONSUME;
    }

    @Override public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) clientOpen.accept(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public int getUseDuration(@Nonnull ItemStack stack, @Nonnull LivingEntity entity) { return 72_000; }

    @Override public @Nonnull UseAnim getUseAnimation(@Nonnull ItemStack stack) { return UseAnim.BRUSH; }

    @Override public void onUseTick(@Nonnull Level level, @Nonnull LivingEntity entity, @Nonnull ItemStack stack, int remaining) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) Paving.tick(player, stack);
    }

    @Override public void releaseUsing(@Nonnull ItemStack stack, @Nonnull Level level, @Nonnull LivingEntity entity, int timeLeft) {
        if (!level.isClientSide && entity instanceof Player player) Paving.stop(player);
    }

    @Override public boolean isBarVisible(@Nonnull ItemStack stack) { return true; }

    @Override public int getBarWidth(@Nonnull ItemStack stack) {
        return Math.round(13f * Math.min(1f, state(stack).charge() / (float) Paving.numbers().capacity()));
    }

    @Override public int getBarColor(@Nonnull ItemStack stack) { return pro ? PRO_BAR : BAR; }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        PenState s = state(stack);
        lines.add(Component.translatable("tooltip.fourtwenty.pave_pen.charge", String.format("%,d", s.charge()), String.format("%,d", Paving.numbers().capacity()))
                .withStyle(ChatFormatting.GREEN));
        lines.add(Component.translatable("tooltip.fourtwenty.pave_pen.lays", Paving.road(s.target(), pro ? com.malevolentgods.fourtwenty.domain.PaveRules.Shape.of(s.shape())
                : com.malevolentgods.fourtwenty.domain.PaveRules.Shape.FULL).getName()).withStyle(ChatFormatting.DARK_GREEN));
    }
}

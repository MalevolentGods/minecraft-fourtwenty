package com.malevolentgods.fourtwenty.lights;

import com.malevolentgods.fourtwenty.domain.LitBitIdRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * The Sticky Finger: places Lit Bits from your inventory, each with the next ID, counting up (100, 101, 102...). A spot where a lamp was
 * broken gives its old ID back instead. Sneak-use it in the air to set the next ID.
 */
public class StickyFingerItem extends Item {
    public StickyFingerItem(Properties properties) {
        super(properties);
    }

    public static int next(ItemStack glove) { return LitBitIdRules.clamp(glove.getOrDefault(ModLights.NEXT_ID.get(), 1)); }

    public static void setNext(ItemStack glove, int id) { glove.set(ModLights.NEXT_ID.get(), LitBitIdRules.clamp(id)); }

    @Override public @Nonnull InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack glove = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) return InteractionResultHolder.pass(glove);
        if (level.isClientSide) com.malevolentgods.fourtwenty.client.LightsClient.openFinger(hand, next(glove));
        return InteractionResultHolder.sidedSuccess(glove, level.isClientSide);
    }

    @Override public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack lamps = findLamps(player);
        if (lamps.isEmpty()) {
            if (!context.getLevel().isClientSide) player.displayClientMessage(Component.translatable("message.fourtwenty.sticky_finger.empty"), true);
            return InteractionResult.FAIL;
        }
        // place one Lit Bit as if it were in hand; keep the stack's count right in creative
        ItemStack one = player.getAbilities().instabuild ? lamps.copyWithCount(1) : lamps;
        var place = new BlockPlaceContext(context.getLevel(), player, context.getHand(), one,
                new net.minecraft.world.phys.BlockHitResult(context.getClickLocation(), context.getClickedFace(), context.getClickedPos(), context.isInside()));
        BlockItem item = (BlockItem) lamps.getItem();
        InteractionResult result = item.place(place);
        if (!result.consumesAction() || context.getLevel().isClientSide) return result;
        ItemStack glove = context.getItemInHand();
        if (context.getLevel().getBlockEntity(place.getClickedPos()) instanceof LitBitBlockEntity lamp && !lamp.fromMemory()) {
            int id = next(glove);
            lamp.setId(id);
            setNext(glove, LitBitIdRules.after(id));
            player.displayClientMessage(Component.translatable("message.fourtwenty.lit_bit.placed", id), true);
        }
        return result;
    }

    private static ItemStack findLamps(Player player) {
        ItemStack off = player.getOffhandItem();
        if (off.is(ModLights.LIT_BIT_ITEM.get())) return off;
        for (ItemStack stack : player.getInventory().items) if (stack.is(ModLights.LIT_BIT_ITEM.get())) return stack;
        return player.getAbilities().instabuild ? new ItemStack(ModLights.LIT_BIT_ITEM.get()) : ItemStack.EMPTY;
    }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> lines, @Nonnull TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.fourtwenty.sticky_finger.next", next(stack)).withStyle(ChatFormatting.AQUA));
    }
}

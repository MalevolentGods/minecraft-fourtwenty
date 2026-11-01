package com.malevolentgods.fourtwenty.item;

import com.malevolentgods.fourtwenty.entity.BuzzBot;
import com.malevolentgods.fourtwenty.registry.ModEntities;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nonnull;

/** Sets a Buzz Bot free. */
public class BuzzBotItem extends Item {
    public BuzzBotItem(Properties properties) { super(properties); }

    @Override public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        Level level = context.getLevel();
        if (!level.isClientSide) {
            BuzzBot bot = ModEntities.BUZZ_BOT.get().create(level);
            if (bot == null) return InteractionResult.FAIL;
            Vec3 at = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace()));
            bot.moveTo(at.x, at.y + 0.5, at.z, 0, 0);
            level.addFreshEntity(bot);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

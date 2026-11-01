package com.malevolentgods.fourtwenty.registry;

import com.malevolentgods.fourtwenty.block.SeatEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** FourTwenty's entities: the seat a player rides when they sit on a couch. */
public final class ModEntities {
    private ModEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "fourtwenty");

    public static final DeferredHolder<EntityType<?>, EntityType<com.malevolentgods.fourtwenty.entity.BuzzBot>> BUZZ_BOT = ENTITY_TYPES.register("buzz_bot",
            () -> EntityType.Builder.of(com.malevolentgods.fourtwenty.entity.BuzzBot::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.45f, 0.4f).clientTrackingRange(8).build("buzz_bot"));

    public static final DeferredHolder<EntityType<?>, EntityType<SeatEntity>> SEAT = ENTITY_TYPES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.001f, 0.001f)
                    .noSummon().clientTrackingRange(10).build("seat"));
}

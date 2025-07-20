package com.malevolentgods.fourtwenty.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import com.malevolentgods.fourtwenty.blockentity.WeedBongBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "fourtwenty");

    @SuppressWarnings("null")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeedBongBlockEntity>> WEED_BONG = 
        BLOCK_ENTITIES.register("weed_bong", () -> 
            BlockEntityType.Builder.of(WeedBongBlockEntity::new, ModBlocks.WEED_BONG.get())
                .build(null));
}

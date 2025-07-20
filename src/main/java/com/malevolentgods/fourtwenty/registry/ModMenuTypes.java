package com.malevolentgods.fourtwenty.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import com.malevolentgods.fourtwenty.menu.WeedBongMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
        DeferredRegister.create(Registries.MENU, "fourtwenty");

    public static final DeferredHolder<MenuType<?>, MenuType<WeedBongMenu>> WEED_BONG = 
        MENU_TYPES.register("weed_bong", () -> 
            IMenuTypeExtension.create((containerId, inventory, data) -> 
                new WeedBongMenu(containerId, inventory, data)));
}

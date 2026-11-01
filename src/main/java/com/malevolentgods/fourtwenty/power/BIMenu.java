package com.malevolentgods.fourtwenty.power;

import com.malevolentgods.fourtwenty.registry.ModPower;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** The gauge screen of a Stash or Hit Box: no slots. Button 0 switches a Hit Box's tier. */
public class BIMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE = 0;
    private final BlockEntity entity;
    private final ContainerData data;

    public BIMenu(int id, @Nullable BlockEntity entity, ContainerData data) {
        super(ModPower.BI_MENU.get(), id);
        checkContainerDataCount(data, BIBlockEntity.DATA_COUNT);
        this.entity = entity;
        this.data = data;
        addDataSlots(data);
    }

    public BIMenu(int id) {
        this(id, null, new SimpleContainerData(BIBlockEntity.DATA_COUNT));
    }

    public int stored() { return data.get(BIBlockEntity.DATA_STORED); }

    public int capacity() { return data.get(BIBlockEntity.DATA_CAPACITY); }

    public boolean overloaded() { return data.get(BIBlockEntity.DATA_OVERLOADED) != 0; }

    public int tier() { return data.get(BIBlockEntity.DATA_TIER); }

    public boolean hitBox() { return data.get(BIBlockEntity.DATA_HIT_BOX) != 0; }

    @Override public boolean clickMenuButton(@Nonnull Player player, int id) {
        if (id == BUTTON_TOGGLE && entity instanceof HitBoxBlockEntity box) {
            box.toggle();
            return true;
        }
        return false;
    }

    @Override public boolean stillValid(@Nonnull Player player) {
        return entity == null || (!entity.isRemoved() && player.distanceToSqr(entity.getBlockPos().getCenter()) <= 64);
    }

    @Override public @Nonnull ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }
}

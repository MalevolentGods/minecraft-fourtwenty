package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.domain.Strain;
import com.malevolentgods.fourtwenty.domain.TerpenePalette;
import com.malevolentgods.fourtwenty.domain.Terpenes;
import com.malevolentgods.fourtwenty.registry.ModDataComponents;
import com.malevolentgods.fourtwenty.registry.ModItems;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

/**
 * The Terpinator: mass production. Pick a terpene whose colour you've caught in the Inspiration Infuser, feed it
 * weed, press Start: every {@link #BUDS_PER} buds make one vial. The 16 Minecraft colours take any weed; a hemp
 * colour takes weed of its family's strains (its accord's family: Neutrals, Warm, Green or Cool). What's being
 * made is the machine's, so anyone can keep it running; only picking a terpene needs the colour caught.
 */
public class TerpinatorBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int BUDS = 0, OUT = 1, SIZE = 2;
    /** Ticks per vial, and buds per vial. */
    public static final int TIME = 100, BUDS_PER = 2;
    public static final int DATA_PROGRESS = 0, DATA_PRODUCT = 1, DATA_RUNNING = 2, DATA_COUNT = 3;
    /** Products: 0..63 the hemp colours in codex order, 64..79 the Minecraft colours in DyeColor order; -1 none. */
    public static final int PRODUCTS = 80, NONE = -1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress, product = NONE;
    private boolean running;
    /** A pre-production Terpinator's resin, bases and accords: popped out on the first tick. */
    private final NonNullList<ItemStack> legacy = NonNullList.create();

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_PRODUCT -> product;
                case DATA_RUNNING -> running ? 1 : 0;
                default -> 0;
            };
        }

        @Override public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_PRODUCT -> product = value;
                case DATA_RUNNING -> running = value != 0;
                default -> {}
            }
        }

        @Override public int getCount() { return DATA_COUNT; }
    };

    public TerpinatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModTerpenes.TERPINATOR_ENTITY.get(), pos, state);
    }

    public ContainerData data() { return data; }

    // ---- products ---------------------------------------------------------------------------------

    public static boolean valid(int product) { return product >= 0 && product < PRODUCTS; }

    /** The product's terpene code: "myr_pin_lim" or "mc_white". */
    public static String code(int product) {
        return product < 64 ? Terpenes.finals().get(product).id() : "mc_" + DyeColor.byId(product - 64).getSerializedName();
    }

    /** The Infuser colour that unlocks it: the hemp code, or the dye's name ("white"). */
    public static String target(int product) {
        return product < 64 ? code(product) : DyeColor.byId(product - 64).getSerializedName();
    }

    public static Item item(int product) {
        String code = code(product);
        return product < 64 ? ModTerpenes.TERPENES.get(code).get() : ModTerpenes.MINECRAFT_TERPENES.get(code).get();
    }

    public static int color(int product) {
        return product < 64 ? TerpenePalette.color(Terpenes.finals().get(product)) : TerpenePalette.minecraft(DyeColor.byId(product - 64));
    }

    /** The strain family whose weed makes it (Landrace for the Minecraft colours: any weed will do). */
    public static Strain.Family family(int product) {
        return product < 64 ? Strain.byId(Terpenes.finals().get(product).accord().id()).family() : Strain.Family.LANDRACE;
    }

    /** Whether these buds feed this product. */
    public static boolean feeds(ItemStack buds, int product) {
        if (!buds.is(ModItems.WEED_BUD.get()) || !valid(product)) return false;
        Strain.Family need = family(product);
        return need == Strain.Family.LANDRACE || ModDataComponents.strain(buds).family() == need;
    }

    // ---- running ----------------------------------------------------------------------------------

    private boolean fits(Item item) {
        ItemStack s = items.get(OUT);
        return s.isEmpty() || (s.is(item) && s.getCount() < s.getMaxStackSize());
    }

    /** Why it isn't making anything right now (a status key), or null if it is. */
    public static String status(boolean running, int product, ItemStack buds, ItemStack out) {
        if (!valid(product)) return "pick";
        if (!running) return "stopped";
        if (buds.getCount() < BUDS_PER) return buds.isEmpty() ? "needs_buds" : "needs_more";
        if (!feeds(buds, product)) return "wrong_family";
        if (!out.isEmpty() && (!out.is(item(product)) || out.getCount() >= out.getMaxStackSize())) return "full";
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TerpinatorBlockEntity t) {
        if (!t.legacy.isEmpty()) {
            t.legacy.forEach(s -> Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, s));
            t.legacy.clear();
            t.setChanged();
        }
        boolean working = status(t.running, t.product, t.items.get(BUDS), t.items.get(OUT)) == null;
        if (working) {
            if (++t.progress >= TIME) {
                t.progress = 0;
                t.items.get(BUDS).shrink(BUDS_PER);
                ItemStack out = t.items.get(OUT);
                if (out.isEmpty()) t.items.set(OUT, new ItemStack(item(t.product)));
                else out.grow(1);
                t.setChanged();
            }
        } else if (t.running && t.progress != 0) {
            t.progress = 0;                    // ran out of buds (a stopped machine keeps its progress)
        }
        if (state.getValue(TerpinatorBlock.LIT) != working) level.setBlock(pos, state.setValue(TerpinatorBlock.LIT, working), 3);
    }

    public boolean isRunning() { return running; }

    public int product() { return product; }

    public void toggle() {
        running = !running;
        setChanged();
    }

    /** Picks what to make (the caller checks the player has caught it); restarts the vial in progress. */
    public void setProduct(int value) {
        if (!valid(value) || value == product) return;
        product = value;
        progress = 0;
        setChanged();
    }

    // Container.

    @Override public int getContainerSize() { return SIZE; }

    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }

    @Override public @Nonnull ItemStack getItem(int slot) { return items.get(slot); }

    @Override public @Nonnull ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }

    @Override public @Nonnull ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }

    @Override public void setItem(int slot, @Nonnull ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override public boolean canPlaceItem(int slot, @Nonnull ItemStack stack) { return slot == BUDS && stack.is(ModItems.WEED_BUD.get()); }

    @Override public boolean stillValid(@Nonnull Player player) { return Container.stillValidBlockEntity(this, player); }

    @Override public void clearContent() { items.clear(); }

    /** Everything to drop when broken, leftovers from an old Terpinator included. */
    public NonNullList<ItemStack> contents() {
        NonNullList<ItemStack> out = NonNullList.create();
        out.addAll(items);
        out.addAll(legacy);
        return out;
    }

    // Menu.

    @Override public @Nonnull Component getDisplayName() { return Component.translatable("container.fourtwenty.terpinator"); }

    @Override public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new TerpinatorMenu(id, inventory, this, data);
    }

    // Saving.

    @Override protected void saveAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("progress", progress);
        tag.putInt("product", product);
        tag.putBoolean("running", running);
        if (!legacy.isEmpty()) {
            CompoundTag old = new CompoundTag();
            ContainerHelper.saveAllItems(old, legacy, registries);
            tag.put("legacy", old);
        }
    }

    @Override protected void loadAdditional(@Nonnull CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        legacy.clear();
        if (tag.contains("method")) {
            // Before production: seven slots (buds, resin, bases, three ports, blend). Keep the buds, pop the rest.
            NonNullList<ItemStack> old = NonNullList.withSize(7, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, old, registries);
            items.set(BUDS, old.get(0));
            old.subList(1, 7).stream().filter(s -> !s.isEmpty()).forEach(legacy::add);
            return;
        }
        ContainerHelper.loadAllItems(tag, items, registries);
        progress = tag.getInt("progress");
        product = tag.contains("product") ? tag.getInt("product") : NONE;
        if (!valid(product)) product = NONE;
        running = tag.getBoolean("running");
        if (tag.contains("legacy")) {
            NonNullList<ItemStack> old = NonNullList.withSize(6, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("legacy"), old, registries);
            old.stream().filter(s -> !s.isEmpty()).forEach(legacy::add);
        }
    }
}

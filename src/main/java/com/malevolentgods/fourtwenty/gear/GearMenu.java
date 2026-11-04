package com.malevolentgods.fourtwenty.gear;

import com.malevolentgods.fourtwenty.domain.GearAbilities;
import com.malevolentgods.fourtwenty.domain.GearRules.Piece;
import com.malevolentgods.fourtwenty.registry.ModTerpenes;
import com.malevolentgods.fourtwenty.terpene.MinecraftTerpeneItem;
import com.malevolentgods.fourtwenty.terpene.TerpeneItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Optional;

/** A piece of gear's terpene slots: only its own hue family goes in; what's in them is saved on the piece as colour codes. */
public class GearMenu extends AbstractContainerMenu {
    public static final int SLOT_X = 44, SLOT_Y = 36, INV_X = 8, INV_Y = 84;
    private final InteractionHand hand;
    private final Player player;
    private final Piece piece;
    private final SimpleContainer terps;

    public GearMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModGear.GEAR_MENU.get(), id);
        this.hand = hand;
        this.player = inventory.player;
        ItemStack gear = player.getItemInHand(hand);
        this.piece = Gear.piece(gear).orElse(Piece.DAB_CELL);
        this.terps = new SimpleContainer(Math.max(1, piece.slots)) {
            @Override public int getMaxStackSize() { return 1; }
        };
        var codes = Gear.state(gear).terps();
        for (int i = 0; i < codes.size() && i < terps.getContainerSize(); i++) terps.setItem(i, stackOf(codes.get(i)));
        terps.addListener(c -> save());
        for (int i = 0; i < piece.slots; i++) {
            addSlot(new Slot(terps, i, SLOT_X + i * 22, SLOT_Y) {
                @Override public boolean mayPlace(@Nonnull ItemStack stack) {
                    return colourOf(stack).map(c -> GearAbilities.fits(piece, c)).orElse(false);
                }
            });
        }
        int locked = hand == InteractionHand.MAIN_HAND ? inventory.selected : -1;
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
        for (int col = 0; col < 9; col++) {
            final int index = col;
            addSlot(new Slot(inventory, col, INV_X + col * 18, INV_Y + 58) {
                @Override public boolean mayPickup(@Nonnull Player p) { return index != locked; }   // the piece being edited stays put
            });
        }
    }

    public Piece piece() { return piece; }

    public ItemStack gear() { return player.getItemInHand(hand); }

    private void save() {
        if (player.level().isClientSide) return;
        var codes = new ArrayList<String>();
        for (int i = 0; i < terps.getContainerSize(); i++) colourOf(terps.getItem(i)).ifPresent(codes::add);
        ItemStack gear = gear();
        if (Gear.piece(gear).isPresent()) Gear.set(gear, Gear.state(gear).withTerps(codes));
    }

    /** A terpene's colour code, if it's a final or Minecraft terpene. */
    public static Optional<String> colourOf(ItemStack stack) {
        if (stack.getItem() instanceof TerpeneItem t && t.code().isFinal()) return Optional.of(t.code().id());
        if (stack.getItem() instanceof MinecraftTerpeneItem m) return Optional.of(m.code());
        return Optional.empty();
    }

    public static ItemStack stackOf(String code) {
        var holder = code.startsWith("mc_") ? ModTerpenes.MINECRAFT_TERPENES.get(code) : ModTerpenes.TERPENES.get(code);
        return holder == null ? ItemStack.EMPTY : new ItemStack(holder.get());
    }

    @Override public boolean stillValid(@Nonnull Player p) { return Gear.piece(gear()).filter(x -> x == piece).isPresent(); }

    @Override public void removed(@Nonnull Player p) {
        super.removed(p);
        save();
    }

    @Override public @Nonnull ItemStack quickMoveStack(@Nonnull Player p, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), copy = stack.copy();
        int terpSlots = piece.slots;
        if (index < terpSlots) {
            if (!moveItemStackTo(stack, terpSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, terpSlots, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Opens the slots for the piece in {@code hand}, if it has any. */
    public static void open(ServerPlayer player, InteractionHand hand) {
        ItemStack gear = player.getItemInHand(hand);
        var piece = Gear.piece(gear);
        if (piece.isEmpty() || piece.get().slots == 0) return;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new GearMenu(id, inv, hand), gear.getHoverName()),
                buf -> buf.writeVarInt(hand.ordinal()));
    }

    public static MenuType<GearMenu> type() {
        return net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) -> new GearMenu(id, inv, InteractionHand.values()[buf.readVarInt()]));
    }

    public static Component title(ItemStack gear) { return gear.getHoverName(); }
}

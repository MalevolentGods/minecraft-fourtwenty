package com.malevolentgods.fourtwenty.client;

import com.malevolentgods.fourtwenty.domain.GearAbilities;
import com.malevolentgods.fourtwenty.gear.Gear;
import com.malevolentgods.fourtwenty.gear.GearMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nonnull;

/** A piece of gear's terpene slots, its charge, and what each terpene unlocks; drawn in the Tech screen's colours. */
public class GearScreen extends AbstractContainerScreen<GearMenu> {
    private static final int PANEL = 0xF023262B, EDGE = 0xFF60C448, SLOT = 0xFF14161A, SLOT_EDGE = 0xFF3A3D42, TEXT = 0xFFE3E6DF, DIM = 0xFF8A8F87, GOOD = 0xFF7BFF5A;

    public GearScreen(GearMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
        inventoryLabelY = GearMenu.INV_Y - 11;
    }

    @Override protected void renderBg(@Nonnull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, EDGE);
        g.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        for (Slot slot : menu.slots) {
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, SLOT_EDGE);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, SLOT);
        }
        // charge bar
        int cap = Math.max(1, Gear.capacity(menu.gear())), bar = 160;
        int filled = (int) ((long) Math.min(Gear.charge(menu.gear()), cap) * bar / cap);
        g.fill(x + 8, y + 18, x + 8 + bar, y + 24, SLOT);
        if (filled > 0) g.fill(x + 8, y + 18, x + 8 + filled, y + 24, GOOD);
    }

    @Override protected void renderLabels(@Nonnull GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 8, 6, TEXT, false);
        Component charge = Component.translatable("gui.fourtwenty.gear.charge", String.format("%,d", Gear.charge(menu.gear())), String.format("%,d", Gear.capacity(menu.gear())));
        g.drawString(font, charge, imageWidth - 8 - font.width(charge), 6, DIM, false);
        int y = GearMenu.SLOT_Y + 22;
        var codes = Gear.state(menu.gear()).terps();
        Component line = codes.isEmpty() ? Component.translatable("gui.fourtwenty.gear.family." + menu.piece().family.name().toLowerCase(java.util.Locale.ROOT))
                : Component.literal(String.join(", ", codes.stream().map(c -> GearAbilities.of(menu.piece(), c)
                        .map(a -> Component.translatable("gear.fourtwenty.ability." + a.id()).getString())
                        .orElse(Component.translatable("gui.fourtwenty.gear.no_ability").getString())).toList()));
        g.drawString(font, line, 8, y, codes.isEmpty() ? DIM : GOOD, false);
        g.drawString(font, playerInventoryTitle, 8, inventoryLabelY, DIM, false);
    }

    @Override public void render(@Nonnull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}

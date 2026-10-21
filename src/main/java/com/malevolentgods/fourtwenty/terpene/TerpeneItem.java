package com.malevolentgods.fourtwenty.terpene;

import com.malevolentgods.fourtwenty.domain.Terpenes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * A terpene: a base, an accord or a final terpene. Until the player has discovered it, it shows
 * as an undiscovered terpene of its tier (its colour, from the texture, is always visible); once
 * discovered, its name, flavour line and code.
 */
public class TerpeneItem extends Item {
    private final Terpenes.Code code;

    public TerpeneItem(Terpenes.Code code, Properties properties) {
        super(properties);
        this.code = code;
    }

    public Terpenes.Code code() { return code; }

    /** "base", "accord" or "final". */
    public static String tierName(Terpenes.Code code) {
        return switch (code.tier()) {
            case 1 -> "base";
            case 2 -> "accord";
            default -> "final";
        };
    }

    @Override public @Nonnull Component getName(@Nonnull ItemStack stack) {
        if (!TerpeneKnowledge.knows(code)) return Component.translatable("item.fourtwenty.terpene.unknown." + tierName(code));
        return super.getName(stack);
    }

    /** The code as coloured abbreviations: "Myr · Pin · Lim". */
    public static MutableComponent codeLine(Terpenes.Code code) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < code.letters().size(); i++) {
            Terpenes.Base b = code.letters().get(i);
            if (i > 0) out.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY));
            out.append(Component.translatable("terpene.fourtwenty." + b.code() + ".abbr")
                    .withStyle(s -> s.withColor(TextColor.fromRgb(com.malevolentgods.fourtwenty.domain.TerpenePalette.color(Terpenes.Code.of(b))))));
        }
        return out;
    }

    @Override public void appendHoverText(@Nonnull ItemStack stack, @Nonnull TooltipContext context, @Nonnull List<Component> tooltip,
                                          @Nonnull TooltipFlag flag) {
        tooltip.add(codeLine(code));
        if (!TerpeneKnowledge.knows(code)) {
            tooltip.add(Component.translatable("item.fourtwenty.terpene.unknown.hint").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (code.isBase()) {
            tooltip.add(Component.translatable("item.fourtwenty.terpene.roles",
                    Component.translatable("terpene.fourtwenty." + code.id() + ".family"),
                    Component.translatable("terpene.fourtwenty." + code.id() + ".hue"),
                    Component.translatable("terpene.fourtwenty." + code.id() + ".tone")).withStyle(ChatFormatting.GRAY));
        } else if (code.isAccord()) {
            tooltip.add(Component.translatable("item.fourtwenty.terpene.accord",
                    Component.translatable("terpene.fourtwenty." + code.id() + ".hue")).withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.fourtwenty.terpene.final").withStyle(ChatFormatting.GRAY));
        }
        tooltip.addAll(flavour(code.id(), 40));
    }

    /**
     * A flavour line as tooltip lines: " / " marks a line break (verse), and long lines wrap at
     * about {@code width} characters. Empty when there's no line.
     */
    public static List<Component> flavour(String key, int width) {
        String id = "terpene.fourtwenty." + key + ".line";
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        if (!language.has(id)) return List.of();
        List<Component> out = new java.util.ArrayList<>();
        for (String verse : language.getOrDefault(id).split(" / ")) {
            StringBuilder line = new StringBuilder();
            for (String word : verse.split(" ")) {
                if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                    out.add(Component.literal(line.toString()).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_AQUA));
                    line.setLength(0);
                }
                line.append(line.isEmpty() ? "" : " ").append(word);
            }
            if (!line.isEmpty()) out.add(Component.literal(line.toString()).withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_AQUA));
        }
        return out;
    }
}

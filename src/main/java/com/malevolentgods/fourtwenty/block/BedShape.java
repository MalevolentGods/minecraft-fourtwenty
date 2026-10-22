package com.malevolentgods.fourtwenty.block;

import net.minecraft.util.StringRepresentable;

import javax.annotation.Nonnull;

/**
 * Where a bed column sits in a joined bed, seen from the foot looking towards the head: a single
 * bed, either side of a double, or one of a king's three columns (a king is also a block longer).
 */
public enum BedShape implements StringRepresentable {
    SINGLE("single"), LEFT("left"), RIGHT("right"), KING_LEFT("king_left"), KING_MIDDLE("king_middle"), KING_RIGHT("king_right");

    private final String name;

    BedShape(String name) { this.name = name; }

    @Override public @Nonnull String getSerializedName() { return name; }

    public boolean king() { return this == KING_LEFT || this == KING_MIDDLE || this == KING_RIGHT; }

    /** Open towards the bed on its left (the bed continues that way). */
    public boolean openLeft() { return this == RIGHT || this == KING_MIDDLE || this == KING_RIGHT; }

    /** Open towards the bed on its right. */
    public boolean openRight() { return this == LEFT || this == KING_LEFT || this == KING_MIDDLE; }
}

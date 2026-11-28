package com.malevolentgods.fourtwenty.tiedye;

import com.malevolentgods.fourtwenty.domain.tiedye.TieDyeRules;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A tie-dye look (specs/018-tie-dye-shack, contracts/tie-dye-data.md): a pattern (0-7) in a colourway. The `fourtwenty:tie_dye` item
 * component; placed blocks keep the pattern in their state and the colourway in their block entity.
 */
public record TieDye(int pattern, String colourway) {
    public static final Codec<TieDye> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, TieDyeRules.PATTERNS.size() - 1).fieldOf("pattern").forGetter(TieDye::pattern),
            Codec.STRING.fieldOf("colourway").forGetter(TieDye::colourway)).apply(i, TieDye::new));
    public static final StreamCodec<ByteBuf, TieDye> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, TieDye::pattern, ByteBufCodecs.STRING_UTF8, TieDye::colourway, TieDye::new);

    public TieDye {
        pattern = Math.max(0, Math.min(TieDyeRules.PATTERNS.size() - 1, pattern));
    }
}

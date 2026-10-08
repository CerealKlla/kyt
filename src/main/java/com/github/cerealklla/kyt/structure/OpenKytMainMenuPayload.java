package com.github.cerealklla.kyt.structure;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.kyt.KytMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-to-client: opens {@code structure.client.KytMainMenuScreen} on right-clicking a {@link KytConfigurationEntity}. */
public record OpenKytMainMenuPayload(int entityId) implements CustomPacketPayload {

    public static final Type<OpenKytMainMenuPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KytMod.MODID, "open_kyt_main_menu"));

    public static final StreamCodec<ByteBuf, OpenKytMainMenuPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenKytMainMenuPayload::entityId,
            OpenKytMainMenuPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

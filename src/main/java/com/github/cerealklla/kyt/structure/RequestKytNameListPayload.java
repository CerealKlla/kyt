package com.github.cerealklla.kyt.structure;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.kyt.KytMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-to-server: "Edit Kyt" on {@code client.KytMainMenuScreen} -- server replies with {@link OpenKytPickerPayload}. */
public record RequestKytNameListPayload(int entityId) implements CustomPacketPayload {

    public static final Type<RequestKytNameListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KytMod.MODID, "request_kyt_name_list"));

    public static final StreamCodec<ByteBuf, RequestKytNameListPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestKytNameListPayload::entityId,
            RequestKytNameListPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

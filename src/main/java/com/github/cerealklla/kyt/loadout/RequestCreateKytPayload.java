package com.github.cerealklla.kyt.loadout;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.kyt.KytMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-to-server: "Create new Kyt" with a chosen name -- server replies with {@link OpenKytEditorPayload} (nameEditable = true). */
public record RequestCreateKytPayload(int entityId, String name) implements CustomPacketPayload {

    public static final Type<RequestCreateKytPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KytMod.MODID, "request_create_kyt"));

    public static final StreamCodec<ByteBuf, RequestCreateKytPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestCreateKytPayload::entityId,
            ByteBufCodecs.STRING_UTF8, RequestCreateKytPayload::name,
            RequestCreateKytPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.github.cerealklla.kyt.loadout;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.kyt.KytMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-to-server: "Edit Kyt" after picking a saved name -- server replies with {@link OpenKytEditorPayload} (nameEditable = false). */
public record RequestEditKytPayload(int entityId, String name) implements CustomPacketPayload {

    public static final Type<RequestEditKytPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KytMod.MODID, "request_edit_kyt"));

    public static final StreamCodec<ByteBuf, RequestEditKytPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RequestEditKytPayload::entityId,
            ByteBufCodecs.STRING_UTF8, RequestEditKytPayload::name,
            RequestEditKytPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

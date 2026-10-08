package com.github.cerealklla.kyt.structure;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.kyt.KytMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-to-client: reply to {@link RequestKytNameListPayload} -- opens {@code client.KytPickerScreen} with every saved Kyt name. */
public record OpenKytPickerPayload(int entityId, List<String> names) implements CustomPacketPayload {

    public static final Type<OpenKytPickerPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KytMod.MODID, "open_kyt_picker"));

    public static final StreamCodec<ByteBuf, OpenKytPickerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenKytPickerPayload::entityId,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), OpenKytPickerPayload::names,
            OpenKytPickerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

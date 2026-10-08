package com.github.cerealklla.kyt.loadout;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.kyt.KytMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: {@code client.ItemPaletteScreen} picked an item -- left-click wants a full
 * stack, right-click wants one. The client never materializes the item itself (that would desync
 * from the server's own authoritative {@code AbstractContainerMenu#carried} the moment a slot click
 * followed it); the server resolves the item, calls {@code LoadoutMenu#setCarried}, and vanilla's
 * own container-sync machinery (see {@code AbstractContainerMenu#broadcastChanges}) pushes the
 * change back to the client from there.
 */
public record RequestPaletteItemPayload(Identifier itemId, int count) implements CustomPacketPayload {

    public static final Type<RequestPaletteItemPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KytMod.MODID, "request_palette_item"));

    public static final StreamCodec<ByteBuf, RequestPaletteItemPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, RequestPaletteItemPayload::itemId,
            ByteBufCodecs.VAR_INT, RequestPaletteItemPayload::count,
            RequestPaletteItemPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

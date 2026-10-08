package com.github.cerealklla.kyt.loadout;

import io.netty.buffer.ByteBuf;

import com.github.cerealklla.kyt.KytMod;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client-to-server: "Save" on {@code client.LoadoutEditorScreen}. No item data travels in this
 * payload -- the server already has the live {@link LoadoutMenu} this player currently has open
 * (vanilla's own container-sync machinery kept its backing {@code Container} up to date the whole
 * time), so the handler just reads that menu's own slots directly via {@link
 * LoadoutMenu#toRecord(String)}. The server re-validates/sanitizes {@code name} itself, never
 * trusting the client's value outright.
 */
public record SaveKytPayload(String name) implements CustomPacketPayload {

    public static final Type<SaveKytPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(KytMod.MODID, "save_kyt"));

    public static final StreamCodec<ByteBuf, SaveKytPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SaveKytPayload::name,
            SaveKytPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

package com.zmine.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

@SuppressWarnings("null")
public record SyncThirstPayload(int thirstLevel) implements CustomPacketPayload {
    public static final Type<SyncThirstPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("zmine", "sync_thirst"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncThirstPayload> CODEC = CustomPacketPayload.codec(
        SyncThirstPayload::write,
        SyncThirstPayload::new
    );

    public SyncThirstPayload(RegistryFriendlyByteBuf buf) {
        this(buf.readInt());
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(this.thirstLevel);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

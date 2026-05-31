package dev.hub.corylib.impl.sync;

import dev.architectury.networking.NetworkManager;
import dev.hub.corylib.Corylib;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class CoryNetworking {
    public static final CustomPacketPayload.Type<DataSyncMessage> DATA_SYNC =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(Corylib.MOD_ID, "data_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DataSyncMessage> DATA_SYNC_CODEC =
            StreamCodec.ofMember(DataSyncMessage::write, DataSyncMessage::new);

    private static boolean registered;

    private CoryNetworking() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }
        registered = true;
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, DATA_SYNC, DATA_SYNC_CODEC,
                (message, context) -> context.queue(message::handle));
    }
}

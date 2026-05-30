package dev.hub.corylib.impl.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class DataSyncMessage implements CustomPacketPayload {
    private final List<Update> updates;

    public DataSyncMessage(List<Update> updates) {
        this.updates = List.copyOf(updates);
    }

    public DataSyncMessage(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Update> decoded = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            decoded.add(new Update(buf.readUtf(), JsonParser.parseString(buf.readUtf())));
        }
        this.updates = List.copyOf(decoded);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return CoryNetworking.DATA_SYNC;
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(updates.size());
        for (Update update : updates) {
            buf.writeUtf(update.entryId());
            buf.writeUtf(update.value().toString());
        }
    }

    public void handle() {
        for (Update update : updates) {
            ClientSyncStorage.INSTANCE.put(update.entryId(), update.value());
        }
    }

    public void sendTo(ServerPlayer player) {
        NetworkManager.sendToPlayer(player, this);
    }

    public void sendToLevel(ServerLevel level) {
        NetworkManager.sendToPlayers(level.players(), this);
    }

    public void sendToAll(MinecraftServer server) {
        NetworkManager.sendToPlayers(server.getPlayerList().getPlayers(), this);
    }

    public record Update(String entryId, JsonElement value) {
    }
}

package dev.hub.corylib.impl.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public final class DataSyncMessage extends BaseS2CMessage {
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
    public MessageType getType() {
        return CoryNetworking.DATA_SYNC;
    }

    @Override
    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(updates.size());
        for (Update update : updates) {
            buf.writeUtf(update.entryId());
            buf.writeUtf(update.value().toString());
        }
    }

    @Override
    public void handle(NetworkManager.PacketContext context) {
        for (Update update : updates) {
            ClientSyncStorage.INSTANCE.put(update.entryId(), update.value());
        }
    }

    public record Update(String entryId, JsonElement value) {
    }
}

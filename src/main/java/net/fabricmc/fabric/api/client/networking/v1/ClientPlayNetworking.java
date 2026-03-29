package net.fabricmc.fabric.api.client.networking.v1;

import newvmcomputers.networking.PacketList;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public final class ClientPlayNetworking {
    private ClientPlayNetworking() {
    }

    public static void send(ResourceLocation id, FriendlyByteBuf payload) {
        PacketList.sendToServer(id, payload);
    }

    public static void registerGlobalReceiver(ResourceLocation id, PlayChannelHandler handler) {
        PacketList.registerClientReceiver(id, buf -> handler.receive(Minecraft.getInstance(), null, buf, null));
    }

    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(Minecraft client, Object handler, FriendlyByteBuf buf, Object responseSender);
    }
}

package mcvmcomputers.networking;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Drop-in replacement for net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.
 *
 * Fabric handler signature:
 *   (client, handler, buf, responseSender) -> ...
 */
public final class ClientPlayNetworking {
    private ClientPlayNetworking() {}

    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(MinecraftClient client,
                     ClientPlayNetworkHandler handler,
                     PacketByteBuf buf,
                     PacketSender responseSender);
    }

    /** Marker interface; the mod never invokes it. */
    public interface PacketSender {
    }

    private static final PacketSender NO_OP = new PacketSender() {};

    public static void registerGlobalReceiver(Identifier id, PlayChannelHandler handler) {
        Net.registerClientHandler(id, (client, buf) ->
            handler.receive(client,
                client.getNetworkHandler(),
                buf, NO_OP));
    }

    /** Send a packet from client to server. */
    public static void send(Identifier id, PacketByteBuf buf) {
        Net.CHANNEL.sendToServer(new MvcMessage(id, buf));
    }
}

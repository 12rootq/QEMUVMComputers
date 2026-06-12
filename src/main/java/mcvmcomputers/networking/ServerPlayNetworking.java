package mcvmcomputers.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * Drop-in replacement for net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.
 *
 * The Fabric handler signature is:
 *   (server, player, handler, buf, responseSender) -> ...
 * We preserve this so the existing lambdas in MainMod/ServerMixin compile unchanged —
 * they just need their import updated.
 */
public final class ServerPlayNetworking {
    private ServerPlayNetworking() {}

    @FunctionalInterface
    public interface PlayChannelHandler {
        void receive(MinecraftServer server,
                     ServerPlayerEntity player,
                     ServerPlayNetworkHandler handler,
                     PacketByteBuf buf,
                     PacketSender responseSender);
    }

    /**
     * Matches the Fabric no-op PacketSender used in the lambdas. The mod never
     * calls into it (responses are sent via the static send methods), so it is a
     * simple marker interface.
     */
    public interface PacketSender {
    }

    private static final PacketSender NO_OP_SENDER = new PacketSender() {};

    public static void registerGlobalReceiver(Identifier id, PlayChannelHandler handler) {
        Net.registerServerHandler(id, (server, player, buf) ->
            handler.receive(server, player, player.networkHandler, buf, NO_OP_SENDER));
    }

    /** Send a packet to a specific player (server → client). */
    public static void send(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        Net.sendToPlayer(player, id, buf);
    }
}

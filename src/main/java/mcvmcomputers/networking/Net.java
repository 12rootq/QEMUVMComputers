package mcvmcomputers.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Central Forge SimpleChannel that replaces all Fabric networking calls.
 *
 * Each Fabric packet keeps its own Identifier; all packets travel through one
 * wrapper message type (MvcMessage) carrying that Identifier + raw bytes.
 */
public final class Net {
    public static final String CHANNEL_VERSION = "1";
    public static SimpleChannel CHANNEL;

    private static final Map<Identifier, ServerHandler> SERVER_HANDLERS = new HashMap<>();
    private static final Map<Identifier, ClientHandler> CLIENT_HANDLERS = new HashMap<>();

    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new Identifier("mcvmcomputers", "main"),
            () -> CHANNEL_VERSION,
            CHANNEL_VERSION::equals,
            CHANNEL_VERSION::equals
        );

        CHANNEL.registerMessage(0, MvcMessage.class,
            MvcMessage::encode,
            MvcMessage::decode,
            Net::handleMessage
        );
    }

    private static void handleMessage(MvcMessage msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);
        if (ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
            ServerHandler h = SERVER_HANDLERS.get(msg.id);
            if (h != null) {
                ServerPlayerEntity sender = ctx.getSender();
                if (sender == null) return;
                MinecraftServer server = sender.getServer();
                PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(msg.payload));
                ctx.enqueueWork(() -> h.handle(server, sender, buf));
            }
        } else {
            ClientHandler h = CLIENT_HANDLERS.get(msg.id);
            if (h != null) {
                PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(msg.payload));
                net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
                ctx.enqueueWork(() -> h.handle(client, buf));
            }
        }
    }

    static void registerServerHandler(Identifier id, ServerHandler h) {
        SERVER_HANDLERS.put(id, h);
    }

    static void registerClientHandler(Identifier id, ClientHandler h) {
        CLIENT_HANDLERS.put(id, h);
    }

    @FunctionalInterface
    public interface ServerHandler {
        void handle(MinecraftServer server, ServerPlayerEntity player, PacketByteBuf buf);
    }

    @FunctionalInterface
    public interface ClientHandler {
        void handle(net.minecraft.client.MinecraftClient client, PacketByteBuf buf);
    }

    /** Send a packet from the server to a specific player. */
    public static void sendToPlayer(ServerPlayerEntity player, Identifier id, PacketByteBuf buf) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new MvcMessage(id, buf));
    }
}

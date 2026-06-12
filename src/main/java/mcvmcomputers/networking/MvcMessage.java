package mcvmcomputers.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** Single wire-message type carrying an Identifier + raw payload bytes. */
public final class MvcMessage {
    public final Identifier id;
    public final byte[] payload;

    public MvcMessage(Identifier id, PacketByteBuf buf) {
        this.id = id;
        // Copy all readable bytes from the buffer.
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        this.payload = bytes;
    }

    private MvcMessage(Identifier id, byte[] payload) {
        this.id = id;
        this.payload = payload;
    }

    public static void encode(MvcMessage msg, PacketByteBuf out) {
        out.writeIdentifier(msg.id);
        out.writeByteArray(msg.payload);
    }

    public static MvcMessage decode(PacketByteBuf in) {
        Identifier id = in.readIdentifier();
        byte[] payload = in.readByteArray();
        return new MvcMessage(id, payload);
    }
}

package mcvmcomputers.networking;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;

/**
 * Drop-in replacement for net.fabricmc.fabric.api.networking.v1.PacketByteBufs.
 */
public final class PacketByteBufs {
    private PacketByteBufs() {}

    public static PacketByteBuf create() {
        return new PacketByteBuf(Unpooled.buffer());
    }
}

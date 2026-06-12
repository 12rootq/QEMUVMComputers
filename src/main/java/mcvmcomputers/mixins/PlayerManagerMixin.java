package mcvmcomputers.mixins;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.buffer.Unpooled;
import mcvmcomputers.MainMod;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.networking.PacketList;
import net.minecraft.network.FriendlyByteBuf;



/**
 * Server-side cleanup when a player disconnects (mixin into {@link PlayerList}).
 *
 * <p>When a player leaves the server, removes their order and PC entity from
 * {@link MainMod#orders}/{@link MainMod#computers} and sends the
 * {@code s2c_stop_screen} packet to tracking clients so they stop rendering that
 * PC's screen. Prevents leaks and "stuck" screens after a player leaves.</p>
 */
@Mixin(PlayerList.class)
public class PlayerManagerMixin {
    @Inject(at = @At("HEAD"), method = "remove")
    public void remove(ServerPlayer player, CallbackInfo ci) {
        MainMod.orders.remove(player.getUUID());
        EntityPC pc = MainMod.computers.remove(player.getUUID());
        if (pc != null) {
            // Tell tracking clients to stop rendering this PC's screen.
            FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
            b.writeUUID(player.getUUID());
            PacketList.sendToTracking(pc, "s2c_stop_screen", b);
        }
    }
}

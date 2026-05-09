package mcvmcomputers.mixins;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import mcvmcomputers.MainMod;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.networking.PacketList;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {
	@Inject(at = @At("HEAD"), method = "remove")
	public void remove(ServerPlayerEntity player, CallbackInfo ci) {
		MainMod.orders.remove(player.getUuid());
		EntityPC pc = MainMod.computers.remove(player.getUuid());;
		if(pc != null){
			Collection<ServerPlayerEntity> watchingPlayers = PlayerLookup.tracking(pc);
			PacketByteBuf b = PacketByteBufs.create();
			b.writeUuid(player.getUuid());
			watchingPlayers.forEach((p) -> {
				ServerPlayNetworking.send(p, PacketList.S2C_STOP_SCREEN, b);
			});
		}
	}
}

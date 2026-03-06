package mcvmcomputers.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcvmcomputers.entities.EntityCRTScreen;
import mcvmcomputers.entities.EntityDeliveryChest;
import mcvmcomputers.entities.EntityFlatScreen;
import mcvmcomputers.entities.EntityWallTV;
import mcvmcomputers.entities.EntityItemPreview;
import mcvmcomputers.entities.EntityKeyboard;
import mcvmcomputers.entities.EntityList;
import mcvmcomputers.entities.EntityMouse;
import mcvmcomputers.entities.EntityPC;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkMixin {
	@Shadow
	private ClientWorld world;

	@Shadow
	private MinecraftClient client;

	@Inject(at = @At("TAIL"), method = "onEntitySpawn")
	public void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
		// В 1.20.1 метод называется getEntityType()
		EntityType<?> entityType = packet.getEntityType();
		double d = packet.getX();
		double e = packet.getY();
		double f = packet.getZ();

		// Используем сразу тип Entity, чтобы потом постоянно не писать (Entity)
		Entity entity15 = null;

		if (entityType == EntityList.ITEM_PREVIEW) {
			entity15 = new EntityItemPreview(this.world, d, e, f);
		} else if (entityType == EntityList.KEYBOARD) {
			entity15 = new EntityKeyboard(this.world, d, e, f);
		} else if (entityType == EntityList.MOUSE) {
			entity15 = new EntityMouse(this.world, d, e, f);
		} else if (entityType == EntityList.CRT_SCREEN) {
			entity15 = new EntityCRTScreen(this.world, d, e, f);
		} else if (entityType == EntityList.FLATSCREEN) {
			entity15 = new EntityFlatScreen(this.world, d, e, f);
		} else if (entityType == EntityList.PC) {
			entity15 = new EntityPC(this.world, d, e, f);
		} else if (entityType == EntityList.DELIVERY_CHEST) {
			entity15 = new EntityDeliveryChest(this.world, d, e, f);
		} else if (entityType == EntityList.WALLTV) {
			entity15 = new EntityWallTV(this.world, d, e, f);
		}

		if (entity15 != null) {
			int i = packet.getId();
			entity15.updateTrackedPosition(d, e, f);

			// Поля pitch и yaw теперь приватные, используем сеттеры
			entity15.setPitch((float)(packet.getPitch() * 360) / 256.0F);
			entity15.setYaw((float)(packet.getYaw() * 360) / 256.0F);

			// setEntityId переименовали в setId
			entity15.setId(i);
			entity15.setUuid(packet.getUuid());
			this.world.addEntity(i, entity15);
		}
	}

	// Метод onEntityPosition должен был иметь @Inject.
	// Вместо переписывания всей логики перемещения сущностей из 1.16.5,
	// мы просто отменяем выполнение оригинального метода, если это EntityDeliveryChest!
	// Это гораздо безопаснее и в 10 раз короче:
	@Inject(at = @At("HEAD"), method = "onEntityPosition", cancellable = true)
	public void onEntityPosition(EntityPositionS2CPacket packet, CallbackInfo ci) {
		if (this.world != null) {
			Entity entity = this.world.getEntityById(packet.getId());
			// Если пакет пришел для нашего сундука, мы просто отменяем этот пакет
			if (entity instanceof EntityDeliveryChest) {
				ci.cancel();
			}
		}
	}
}
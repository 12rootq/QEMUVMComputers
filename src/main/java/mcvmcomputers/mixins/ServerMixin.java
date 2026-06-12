package mcvmcomputers.mixins;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import mcvmcomputers.MainMod;
import mcvmcomputers.entities.EntityDeliveryChest;
import mcvmcomputers.item.OrderableItem;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.TabletOrder;
import mcvmcomputers.utils.TabletOrder.OrderStatus;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Server-side lifecycle logic for tablet orders (mixin into MinecraftServer).
 *
 * <p>Each server tick advances every order's state machine, spawns the delivery/payment
 * chest at the right moment, and sends order status sync to the player. On shutdown it
 * clears all orders and PCs.</p>
 */
@Mixin(MinecraftServer.class)
public class ServerMixin {
private static final Logger LOGGER = LogManager.getLogger();
/** Duration of a single tick in seconds (20 TPS). */
private static final float TICK_TIME = 0.05f;

@Shadow
private PlayerManager playerManager;

@Inject(at = @At("HEAD"), method = "shutdown")
protected void shutdown(CallbackInfo ci) {
LOGGER.info("Stopping VM Computers");
MainMod.computers.clear();
MainMod.orders.clear();
}

@Inject(at = @At("HEAD"), method = "tick")
protected void tick(CallbackInfo ci) {
java.util.List<java.util.UUID> toRemove = new java.util.ArrayList<>();
for (TabletOrder order : MainMod.orders.values()) {
if (order.currentStatus == OrderStatus.ORDER_CHEST_ARRIVAL_SOON) {
order.tickCount += TICK_TIME;
if (order.tickCount > 5) {
order.currentStatus = OrderStatus.ORDER_CHEST_ARRIVED;
order.tickCount = 0;
}
} else if (order.currentStatus == OrderStatus.PAYMENT_CHEST_ARRIVAL_SOON) {
order.tickCount += TICK_TIME;
if (order.tickCount > 5) {
order.currentStatus = OrderStatus.PAYMENT_CHEST_ARRIVED;
order.tickCount = 0;
}
} else if (order.currentStatus == OrderStatus.ORDER_CHEST_RECEIVED) {
order.tickCount += TICK_TIME;
if (order.tickCount > 0.25) {
toRemove.add(UUID.fromString(order.orderUUID));
}
} else if (order.currentStatus == OrderStatus.ORDER_CHEST_ARRIVED) {
if (!order.entitySpawned) {
PlayerEntity p = playerManager.getPlayer(UUID.fromString(order.orderUUID));
if (p == null) { continue; }
World w = p.getWorld();
w.spawnEntity(new EntityDeliveryChest(w, new Vec3d(p.getX(), p.getY(), p.getZ()), p.getUuid()));
order.entitySpawned = true;
}
} else if (order.currentStatus == OrderStatus.PAYMENT_CHEST_ARRIVED) {
if (!order.entitySpawned) {
PlayerEntity p = playerManager.getPlayer(UUID.fromString(order.orderUUID));
if (p == null) { continue; }
World w = p.getWorld();
EntityDeliveryChest chest = new EntityDeliveryChest(w, new Vec3d(p.getX(), p.getY(), p.getZ()), p.getUuid());
// Label the payment chest with the price (e.g. "3 Iron Ingot").
if (order.price > 0) {
chest.setCustomName(Text.literal(order.price + " ").append(Text.translatable("item.minecraft.iron_ingot")));
chest.setCustomNameVisible(true);
}
w.spawnEntity(chest);
order.entitySpawned = true;
}
}

PacketByteBuf pb = PacketByteBufs.create();
// Skip null items so a partially-filled order can't NPE during encode.
int validItemCount = 0;
for (OrderableItem oi : order.items) {
if (oi != null) validItemCount++;
}
pb.writeInt(validItemCount);
for (OrderableItem oi : order.items) {
if (oi == null) continue;
pb.writeString(net.minecraft.registry.Registries.ITEM.getId(oi).toString());
}
pb.writeInt(order.price);
pb.writeInt(order.currentStatus.ordinal());
// Only send if the target player is online.
ServerPlayerEntity sp = playerManager.getPlayer(UUID.fromString(order.orderUUID));
if (sp != null) {
ServerPlayNetworking.send(sp, new PacketList.RawBytesPayload(PacketList.S2C_SYNC_ORDER, pb));
}
}
for (UUID u : toRemove) {
MainMod.orders.remove(u);
}
}
}
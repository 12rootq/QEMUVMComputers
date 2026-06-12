package mcvmcomputers.mixins;
import net.minecraft.server.players.PlayerList;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.buffer.Unpooled;
import mcvmcomputers.MainMod;
import mcvmcomputers.entities.EntityDeliveryChest;
import mcvmcomputers.item.OrderableItem;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.TabletOrder;
import mcvmcomputers.utils.TabletOrder.OrderStatus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Server-side lifecycle logic for tablet orders (mixin into {@link MinecraftServer}).
 *
 * <p>Every server tick ({@code tickServer}) advances the state machine of each order
 * in {@link MainMod#orders}: counts down chest arrival delays (payment/delivery),
 * spawns {@link EntityDeliveryChest} at the right moment, and sends order status
 * synchronization ({@code s2c_sync_order}) to the player. On server stop
 * ({@code stopServer}) it clears all orders and PCs.</p>
 */
@Mixin(MinecraftServer.class)
public class ServerMixin {
    private static final Logger LOGGER = LogManager.getLogger();
    /** Duration of a single tick in seconds (20 TPS). */
    private static final float TICK_TIME = 0.05f;

    @Shadow
    private PlayerList playerList;

    @Inject(at = @At("HEAD"), method = "stopServer")
    protected void stopServer(CallbackInfo ci) {
        LOGGER.info("Stopping VM Computers");
        MainMod.computers.clear();
        MainMod.orders.clear();
    }

    @Inject(at = @At("HEAD"), method = "tickServer")
    protected void tickServer(CallbackInfo ci) {
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
                    Player p = playerList.getPlayer(UUID.fromString(order.orderUUID));
                    if (p == null) continue;
                    Level w = p.level();
                    w.addFreshEntity(new EntityDeliveryChest(w, new Vec3(p.getX(), p.getY(), p.getZ()), p.getUUID()));
                    order.entitySpawned = true;
                }
            } else if (order.currentStatus == OrderStatus.PAYMENT_CHEST_ARRIVED) {
                if (!order.entitySpawned) {
                    Player p = playerList.getPlayer(UUID.fromString(order.orderUUID));
                    if (p == null) continue;
                    Level w = p.level();
                    EntityDeliveryChest chest = new EntityDeliveryChest(w, new Vec3(p.getX(), p.getY(), p.getZ()), p.getUUID());
                    if (order.price > 0) {
                        chest.setCustomName(net.minecraft.network.chat.Component.literal(order.price + " ").append(net.minecraft.network.chat.Component.translatable("item.minecraft.iron_ingot")));
                        chest.setCustomNameVisible(true);
                    }
                    w.addFreshEntity(chest);
                    order.entitySpawned = true;
                }
            }

            FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
            pb.writeUtf(order.orderUUID);
            int validItemCount = 0;
            for (OrderableItem oi : order.items) {
                if (oi != null) validItemCount++;
            }
            pb.writeInt(validItemCount);
            for (OrderableItem oi : order.items) {
                if (oi == null) continue;
                pb.writeUtf(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(new ItemStack(oi).getItem()).toString());
            }
            pb.writeInt(order.price);
            pb.writeInt(order.currentStatus.ordinal());
            Player p = playerList.getPlayer(UUID.fromString(order.orderUUID));
            if (p != null && p instanceof net.minecraft.server.level.ServerPlayer sp) {
                PacketList.sendToPlayer(sp, "s2c_sync_order", pb);
            }
        }
        for (UUID u : toRemove) {
            MainMod.orders.remove(u);
        }
    }
}

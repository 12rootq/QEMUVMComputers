package newvmcomputers;

import static newvmcomputers.networking.PacketList.C2S_ADD_CPU;
import static newvmcomputers.networking.PacketList.C2S_ADD_GPU;
import static newvmcomputers.networking.PacketList.C2S_ADD_HARD_DRIVE;
import static newvmcomputers.networking.PacketList.C2S_ADD_ISO;
import static newvmcomputers.networking.PacketList.C2S_ADD_MOBO;
import static newvmcomputers.networking.PacketList.C2S_ADD_RAM;
import static newvmcomputers.networking.PacketList.C2S_CHANGE_HDD;
import static newvmcomputers.networking.PacketList.C2S_ORDER;
import static newvmcomputers.networking.PacketList.C2S_REMOVE_CPU;
import static newvmcomputers.networking.PacketList.C2S_REMOVE_GPU;
import static newvmcomputers.networking.PacketList.C2S_REMOVE_HARD_DRIVE;
import static newvmcomputers.networking.PacketList.C2S_REMOVE_ISO;
import static newvmcomputers.networking.PacketList.C2S_REMOVE_MOBO;
import static newvmcomputers.networking.PacketList.C2S_REMOVE_RAM;
import static newvmcomputers.networking.PacketList.C2S_SCREEN;
import static newvmcomputers.networking.PacketList.C2S_TURN_OFF_PC;
import static newvmcomputers.networking.PacketList.C2S_TURN_ON_PC;
import static newvmcomputers.networking.PacketList.S2C_SCREEN;
import static newvmcomputers.networking.PacketList.S2C_STOP_SCREEN;
import static newvmcomputers.networking.PacketList.S2C_SYNC_ORDER;
import static newvmcomputers.networking.PacketList.removeCpu;
import static newvmcomputers.networking.PacketList.removeGpu;
import static newvmcomputers.networking.PacketList.removeHdd;
import static newvmcomputers.networking.PacketList.removeRam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import newvmcomputers.entities.EntityDeliveryChest;
import newvmcomputers.entities.EntityList;
import newvmcomputers.entities.EntityPC;
import newvmcomputers.item.ItemHarddrive;
import newvmcomputers.item.ItemList;
import newvmcomputers.item.OrderableItem;
import newvmcomputers.networking.PacketList;
import newvmcomputers.sound.SoundList;
import newvmcomputers.utils.TabletOrder;
import newvmcomputers.utils.TabletOrder.OrderStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MainMod.MODID)
public class MainMod {
    public static final String MODID = "newvmcomputers";

    public static Map<UUID, TabletOrder> orders = new ConcurrentHashMap<>();
    public static Map<UUID, EntityPC> computers = new ConcurrentHashMap<>();
    public static Runnable hardDriveClick = () -> {};
    public static Runnable deliveryChestSound = () -> {};
    public static Runnable focus = () -> {};
    public static Runnable pcOpenGui = () -> {};

    public MainMod() {
        ItemList.init(FMLJavaModLoadingContext.get().getModEventBus());
        EntityList.init(FMLJavaModLoadingContext.get().getModEventBus());
        SoundList.init(FMLJavaModLoadingContext.get().getModEventBus());
        PacketList.init();
        registerServerPackets();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> new newvmcomputers.client.ClientMod().onInitializeClient());

        MinecraftForge.EVENT_BUS.addListener(MainMod::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(MainMod::onServerStopping);
        MinecraftForge.EVENT_BUS.addListener(MainMod::onPlayerLoggedOut);
    }

    public static void registerServerPackets() {
        PacketList.registerServerReceiver(C2S_ORDER, (player, buf) -> {
            int arraySize = buf.readInt();
            OrderableItem[] items = new OrderableItem[arraySize];
            int price = 0;
            for (int i = 0; i < arraySize; i++) {
                items[i] = (OrderableItem) buf.readItem().getItem();
                price += items[i].getPrice();
            }

            TabletOrder order = new TabletOrder();
            order.items = new ArrayList<>();
            order.items.addAll(Arrays.asList(items));
            order.price = price;
            order.orderUUID = player.getStringUUID();
            orders.put(player.getUUID(), order);
        });

        PacketList.registerServerReceiver(C2S_SCREEN, (player, buf) -> {
            byte[] screen = buf.readByteArray();
            int compressedDataSize = buf.readInt();
            int dataSize = buf.readInt();

            EntityPC pc = computers.get(player.getUUID());
            if (pc != null) {
                FriendlyByteBuf response = PacketList.buffer();
                response.writeByteArray(screen);
                response.writeInt(compressedDataSize);
                response.writeInt(dataSize);
                response.writeUUID(player.getUUID());
                PacketList.sendToTracking(pc, S2C_SCREEN, response);
            }
        });

        PacketList.registerServerReceiver(C2S_TURN_ON_PC, (player, buf) -> {
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                computers.put(player.getUUID(), pc);
            }
        });

        PacketList.registerServerReceiver(C2S_TURN_OFF_PC, (player, buf) -> {
            EntityPC pc = computers.remove(player.getUUID());
            if (pc != null) {
                FriendlyByteBuf response = PacketList.buffer();
                response.writeUUID(player.getUUID());
                PacketList.sendToTracking(pc, S2C_STOP_SCREEN, response);
            }
        });

        PacketList.registerServerReceiver(C2S_CHANGE_HDD, (player, buf) -> {
            String newHddName = buf.readUtf(32767);
            for (ItemStack stack : player.getHandSlots()) {
                if (!stack.isEmpty() && stack.getItem() instanceof ItemHarddrive) {
                    stack.getOrCreateTag().putString("vhdfile", newHddName);
                    break;
                }
            }
        });

        PacketList.registerServerReceiver(C2S_ADD_MOBO, (player, buf) -> {
            boolean x64 = buf.readBoolean();
            int entityId = buf.readInt();
            Item lookingFor = x64 ? ItemList.ITEM_MOTHERBOARD64.get() : ItemList.ITEM_MOTHERBOARD.get();

            if (player.getInventory().contains(new ItemStack(lookingFor))) {
                Entity entity = player.level().getEntity(entityId);
                if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID()) && !pc.getMotherboardInstalled()) {
                    removeStack(player.getInventory(), new ItemStack(lookingFor));
                    pc.setMotherboardInstalled(true);
                    pc.set64Bit(x64);
                }
            } else {
                player.sendSystemMessage(Component.translatable("newvmcomputers.motherboard_not_present").withStyle(ChatFormatting.RED));
            }
        });

        PacketList.registerServerReceiver(C2S_ADD_GPU, (player, buf) -> {
            Item lookingFor = ItemList.ITEM_GPU.get();
            int entityId = buf.readInt();

            if (player.getInventory().contains(new ItemStack(lookingFor))) {
                Entity entity = player.level().getEntity(entityId);
                if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID()) && !pc.getGpuInstalled()) {
                    removeStack(player.getInventory(), new ItemStack(lookingFor));
                    pc.setGpuInstalled(true);
                }
            } else {
                player.sendSystemMessage(Component.translatable("newvmcomputers.gpu_not_present").withStyle(ChatFormatting.RED));
            }
        });

        PacketList.registerServerReceiver(C2S_ADD_CPU, (player, buf) -> {
            int dividedBy = buf.readInt();
            int entityId = buf.readInt();
            Item lookingFor = switch (dividedBy) {
                case 2 -> ItemList.ITEM_CPU2.get();
                case 4 -> ItemList.ITEM_CPU4.get();
                case 6 -> ItemList.ITEM_CPU6.get();
                default -> null;
            };

            if (lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
                Entity entity = player.level().getEntity(entityId);
                if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID()) && pc.getCpuDividedBy() == 0) {
                    removeStack(player.getInventory(), new ItemStack(lookingFor));
                    pc.setCpuDividedBy(dividedBy);
                }
            } else {
                player.sendSystemMessage(Component.translatable("newvmcomputers.cpu_not_present").withStyle(ChatFormatting.RED));
            }
        });

        PacketList.registerServerReceiver(C2S_ADD_RAM, (player, buf) -> {
            int mb = buf.readInt();
            int entityId = buf.readInt();
            Item lookingFor = switch (mb) {
                case 64 -> ItemList.ITEM_RAM64M.get();
                case 128 -> ItemList.ITEM_RAM128M.get();
                case 256 -> ItemList.ITEM_RAM256M.get();
                case 512 -> ItemList.ITEM_RAM512M.get();
                case 1024 -> ItemList.ITEM_RAM1G.get();
                case 2048 -> ItemList.ITEM_RAM2G.get();
                case 4096 -> ItemList.ITEM_RAM4G.get();
                default -> null;
            };

            if (lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
                Entity entity = player.level().getEntity(entityId);
                if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                    if (pc.getGigsOfRamInSlot0() == 0) {
                        removeStack(player.getInventory(), new ItemStack(lookingFor));
                        pc.setGigsOfRamInSlot0(mb);
                    } else if (pc.getGigsOfRamInSlot1() == 0) {
                        removeStack(player.getInventory(), new ItemStack(lookingFor));
                        pc.setGigsOfRamInSlot1(mb);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("newvmcomputers.cpu_not_present").withStyle(ChatFormatting.RED));
            }
        });

        PacketList.registerServerReceiver(C2S_ADD_HARD_DRIVE, (player, buf) -> {
            String vhdName = buf.readUtf(32767);
            int entityId = buf.readInt();
            ItemStack lookingFor = ItemHarddrive.createHardDrive(vhdName);

            if (player.getInventory().contains(lookingFor)) {
                Entity entity = player.level().getEntity(entityId);
                if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID()) && pc.getHardDriveFileName().isEmpty()) {
                    removeStack(player.getInventory(), lookingFor);
                    pc.setHardDriveFileName(vhdName);
                }
            } else {
                player.sendSystemMessage(Component.translatable("newvmcomputers.cpu_not_present").withStyle(ChatFormatting.RED));
            }
        });

        PacketList.registerServerReceiver(C2S_REMOVE_MOBO, (player, buf) -> {
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID()) && pc.getMotherboardInstalled()) {
                pc.setMotherboardInstalled(false);
                pc.level().addFreshEntity(new ItemEntity(pc.level(), pc.getX(), pc.getY(), pc.getZ(),
                        new ItemStack(pc.get64Bit() ? ItemList.ITEM_MOTHERBOARD64.get() : ItemList.ITEM_MOTHERBOARD.get())));
                removeCpu(pc);
                removeGpu(pc);
                removeHdd(pc, player.getStringUUID());
                removeRam(pc, 0);
                removeRam(pc, 1);
            }
        });

        PacketList.registerServerReceiver(C2S_REMOVE_GPU, (player, buf) -> {
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                removeGpu(pc);
            }
        });

        PacketList.registerServerReceiver(C2S_REMOVE_HARD_DRIVE, (player, buf) -> {
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                removeHdd(pc, player.getStringUUID());
            }
        });

        PacketList.registerServerReceiver(C2S_REMOVE_CPU, (player, buf) -> {
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                removeCpu(pc);
            }
        });

        PacketList.registerServerReceiver(C2S_REMOVE_RAM, (player, buf) -> {
            int slot = buf.readInt();
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                removeRam(pc, slot);
            }
        });

        PacketList.registerServerReceiver(C2S_ADD_ISO, (player, buf) -> {
            String isoName = buf.readUtf(32767);
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID()) && pc.getIsoFileName().isEmpty()) {
                pc.setIsoFileName(isoName);
            }
        });

        PacketList.registerServerReceiver(C2S_REMOVE_ISO, (player, buf) -> {
            Entity entity = player.level().getEntity(buf.readInt());
            if (entity instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID()) && !pc.getIsoFileName().isEmpty()) {
                pc.setIsoFileName("");
            }
        });
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Iterator<Map.Entry<UUID, TabletOrder>> iterator = orders.entrySet().iterator();
        while (iterator.hasNext()) {
            TabletOrder order = iterator.next().getValue();

            if (order.currentStatus == OrderStatus.ORDER_CHEST_ARRIVAL_SOON) {
                order.tickCount += 0.05f;
                if (order.tickCount > 5f) {
                    order.currentStatus = OrderStatus.ORDER_CHEST_ARRIVED;
                    order.tickCount = 0f;
                }
            } else if (order.currentStatus == OrderStatus.PAYMENT_CHEST_ARRIVAL_SOON) {
                order.tickCount += 0.05f;
                if (order.tickCount > 5f) {
                    order.currentStatus = OrderStatus.PAYMENT_CHEST_ARRIVED;
                    order.tickCount = 0f;
                }
            } else if (order.currentStatus == OrderStatus.ORDER_CHEST_RECEIVED) {
                order.tickCount += 0.05f;
                if (order.tickCount > 0.25f) {
                    iterator.remove();
                    continue;
                }
            } else if ((order.currentStatus == OrderStatus.ORDER_CHEST_ARRIVED
                    || order.currentStatus == OrderStatus.PAYMENT_CHEST_ARRIVED)
                    && !order.entitySpawned) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(UUID.fromString(order.orderUUID));
                if (player == null) {
                    iterator.remove();
                    continue;
                }
                player.level().addFreshEntity(new EntityDeliveryChest(player.level(),
                        new Vec3(player.getX(), player.getY(), player.getZ()), player.getUUID()));
                order.entitySpawned = true;
            }

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(UUID.fromString(order.orderUUID));
            if (player != null) {
                FriendlyByteBuf buffer = PacketList.buffer();
                buffer.writeInt(order.items.size());
                for (OrderableItem orderableItem : order.items) {
                    buffer.writeItem(new ItemStack(orderableItem));
                }
                buffer.writeInt(order.price);
                buffer.writeInt(order.currentStatus.ordinal());
                PacketList.sendToPlayer(player, S2C_SYNC_ORDER, buffer);
            }
        }
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        computers.clear();
        orders.clear();
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        orders.remove(player.getUUID());
        EntityPC pc = computers.remove(player.getUUID());
        if (pc != null) {
            FriendlyByteBuf buffer = PacketList.buffer();
            buffer.writeUUID(player.getUUID());
            PacketList.sendToTracking(pc, S2C_STOP_SCREEN, buffer);
        }
    }

    private static void removeStack(Inventory inventory, ItemStack target) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, target)) {
                stack.shrink(1);
                return;
            }
        }
        throw new IllegalStateException("Inventory does not contain required stack");
    }
}


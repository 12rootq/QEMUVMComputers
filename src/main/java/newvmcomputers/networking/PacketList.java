package newvmcomputers.networking;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import newvmcomputers.MainMod;
import newvmcomputers.entities.EntityPC;
import newvmcomputers.item.ItemHarddrive;
import newvmcomputers.item.ItemList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class PacketList {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MainMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static final Map<ResourceLocation, BiConsumer<ServerPlayer, FriendlyByteBuf>> SERVER_HANDLERS = new HashMap<>();
    private static final Map<ResourceLocation, Consumer<FriendlyByteBuf>> CLIENT_HANDLERS = new HashMap<>();

    private static int nextId = 0;

    public static final ResourceLocation C2S_ORDER = new ResourceLocation(MainMod.MODID, "c2s_order");
    public static final ResourceLocation C2S_SCREEN = new ResourceLocation(MainMod.MODID, "c2s_screen");
    public static final ResourceLocation C2S_CHANGE_HDD = new ResourceLocation(MainMod.MODID, "c2s_change_hdd");
    public static final ResourceLocation C2S_TURN_ON_PC = new ResourceLocation(MainMod.MODID, "c2s_turn_on_pc");
    public static final ResourceLocation C2S_TURN_OFF_PC = new ResourceLocation(MainMod.MODID, "c2s_turn_off_pc");
    public static final ResourceLocation C2S_ADD_MOBO = new ResourceLocation(MainMod.MODID, "c2s_add_mobo");
    public static final ResourceLocation C2S_ADD_RAM = new ResourceLocation(MainMod.MODID, "c2s_add_ram");
    public static final ResourceLocation C2S_ADD_CPU = new ResourceLocation(MainMod.MODID, "c2s_add_cpu");
    public static final ResourceLocation C2S_ADD_GPU = new ResourceLocation(MainMod.MODID, "c2s_add_gpu");
    public static final ResourceLocation C2S_ADD_HARD_DRIVE = new ResourceLocation(MainMod.MODID, "c2s_add_hard_drive");
    public static final ResourceLocation C2S_ADD_ISO = new ResourceLocation(MainMod.MODID, "c2s_add_iso");
    public static final ResourceLocation C2S_REMOVE_MOBO = new ResourceLocation(MainMod.MODID, "c2s_remove_mobo");
    public static final ResourceLocation C2S_REMOVE_RAM = new ResourceLocation(MainMod.MODID, "c2s_remove_ram");
    public static final ResourceLocation C2S_REMOVE_CPU = new ResourceLocation(MainMod.MODID, "c2s_remove_cpu");
    public static final ResourceLocation C2S_REMOVE_GPU = new ResourceLocation(MainMod.MODID, "c2s_remove_gpu");
    public static final ResourceLocation C2S_REMOVE_HARD_DRIVE = new ResourceLocation(MainMod.MODID, "c2s_remove_hard_drive");
    public static final ResourceLocation C2S_REMOVE_ISO = new ResourceLocation(MainMod.MODID, "c2s_remove_iso");

    public static final ResourceLocation S2C_SCREEN = new ResourceLocation(MainMod.MODID, "s2c_screen");
    public static final ResourceLocation S2C_STOP_SCREEN = new ResourceLocation(MainMod.MODID, "s2c_stop_screen");
    public static final ResourceLocation S2C_SYNC_ORDER = new ResourceLocation(MainMod.MODID, "s2c_sync_order");

    private PacketList() {
    }

    public static void init() {
        CHANNEL.registerMessage(nextId++, C2SPayload.class, C2SPayload::encode, C2SPayload::decode, C2SPayload::handle);
        CHANNEL.registerMessage(nextId++, S2CPayload.class, S2CPayload::encode, S2CPayload::decode, S2CPayload::handle);
    }

    public static FriendlyByteBuf buffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static void registerServerReceiver(ResourceLocation id, BiConsumer<ServerPlayer, FriendlyByteBuf> handler) {
        SERVER_HANDLERS.put(id, handler);
    }

    public static void registerClientReceiver(ResourceLocation id, Consumer<FriendlyByteBuf> handler) {
        CLIENT_HANDLERS.put(id, handler);
    }

    public static void sendToServer(ResourceLocation id, FriendlyByteBuf payload) {
        CHANNEL.sendToServer(new C2SPayload(id, extractBytes(payload)));
    }

    public static void sendToPlayer(ServerPlayer player, ResourceLocation id, FriendlyByteBuf payload) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPayload(id, extractBytes(payload)));
    }

    public static void sendToPlayers(Collection<ServerPlayer> players, ResourceLocation id, FriendlyByteBuf payload) {
        byte[] data = extractBytes(payload);
        for (ServerPlayer player : players) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new S2CPayload(id, data));
        }
    }

    public static void sendToTracking(Entity entity, ResourceLocation id, FriendlyByteBuf payload) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new S2CPayload(id, extractBytes(payload)));
    }

    private static byte[] extractBytes(FriendlyByteBuf payload) {
        return ByteBufUtil.getBytes(payload, payload.readerIndex(), payload.readableBytes(), false);
    }

    private static FriendlyByteBuf wrap(byte[] data) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }

    public static void removeGpu(EntityPC pc) {
        if (pc.getGpuInstalled()) {
            pc.setGpuInstalled(false);
            pc.level().addFreshEntity(new ItemEntity(
                    pc.level(),
                    pc.getX(),
                    pc.getY(),
                    pc.getZ(),
                    new ItemStack(ItemList.ITEM_GPU.get())
            ));
        }
    }

    public static void removeHdd(EntityPC pc, String uuid) {
        if (!pc.getHardDriveFileName().isEmpty()) {
            pc.level().addFreshEntity(new ItemEntity(
                    pc.level(),
                    pc.getX(),
                    pc.getY(),
                    pc.getZ(),
                    ItemHarddrive.createHardDrive(pc.getHardDriveFileName())
            ));
            pc.setHardDriveFileName("");
        }
    }

    public static void removeCpu(EntityPC pc) {
        if (pc.getCpuDividedBy() <= 0) {
            return;
        }

        Item cpuItem;
        switch (pc.getCpuDividedBy()) {
            case 2 -> cpuItem = ItemList.ITEM_CPU2.get();
            case 4 -> cpuItem = ItemList.ITEM_CPU4.get();
            case 6 -> cpuItem = ItemList.ITEM_CPU6.get();
            default -> {
                return;
            }
        }

        pc.setCpuDividedBy(0);
        pc.level().addFreshEntity(new ItemEntity(
                pc.level(),
                pc.getX(),
                pc.getY(),
                pc.getZ(),
                new ItemStack(cpuItem)
        ));
    }

    public static void removeRam(EntityPC pc, int slot) {
        Item ramStickItem = null;

        if (slot == 0) {
            if (pc.getGigsOfRamInSlot0() == 1024) {
                ramStickItem = ItemList.ITEM_RAM1G.get();
            } else if (pc.getGigsOfRamInSlot0() == 2048) {
                ramStickItem = ItemList.ITEM_RAM2G.get();
            } else if (pc.getGigsOfRamInSlot0() == 4096) {
                ramStickItem = ItemList.ITEM_RAM4G.get();
            } else if (pc.getGigsOfRamInSlot0() == 256) {
                ramStickItem = ItemList.ITEM_RAM256M.get();
            } else if (pc.getGigsOfRamInSlot0() == 512) {
                ramStickItem = ItemList.ITEM_RAM512M.get();
            } else if (pc.getGigsOfRamInSlot0() == 128) {
                ramStickItem = ItemList.ITEM_RAM128M.get();
            } else if (pc.getGigsOfRamInSlot0() == 64) {
                ramStickItem = ItemList.ITEM_RAM64M.get();
            } else {
                return;
            }

            pc.setGigsOfRamInSlot0(0);
        } else {
            if (pc.getGigsOfRamInSlot1() == 1024) {
                ramStickItem = ItemList.ITEM_RAM1G.get();
            } else if (pc.getGigsOfRamInSlot1() == 2048) {
                ramStickItem = ItemList.ITEM_RAM2G.get();
            } else if (pc.getGigsOfRamInSlot1() == 4096) {
                ramStickItem = ItemList.ITEM_RAM4G.get();
            } else if (pc.getGigsOfRamInSlot1() == 256) {
                ramStickItem = ItemList.ITEM_RAM256M.get();
            } else if (pc.getGigsOfRamInSlot1() == 512) {
                ramStickItem = ItemList.ITEM_RAM512M.get();
            } else if (pc.getGigsOfRamInSlot1() == 128) {
                ramStickItem = ItemList.ITEM_RAM128M.get();
            } else if (pc.getGigsOfRamInSlot1() == 64) {
                ramStickItem = ItemList.ITEM_RAM64M.get();
            } else {
                return;
            }

            pc.setGigsOfRamInSlot1(0);
        }

        pc.level().addFreshEntity(new ItemEntity(
                pc.level(),
                pc.getX(),
                pc.getY(),
                pc.getZ(),
                new ItemStack(ramStickItem)
        ));
    }

    private record C2SPayload(ResourceLocation id, byte[] payload) {
        private static void encode(C2SPayload message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.id);
            buffer.writeVarInt(message.payload.length);
            buffer.writeBytes(message.payload);
        }

        private static C2SPayload decode(FriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            int length = buffer.readVarInt();
            byte[] payload = new byte[length];
            buffer.readBytes(payload);
            return new C2SPayload(id, payload);
        }

        private static void handle(C2SPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            ServerPlayer player = context.getSender();
            context.enqueueWork(() -> {
                BiConsumer<ServerPlayer, FriendlyByteBuf> handler = SERVER_HANDLERS.get(message.id);
                if (player != null && handler != null) {
                    handler.accept(player, wrap(message.payload));
                }
            });
            context.setPacketHandled(true);
        }
    }

    private record S2CPayload(ResourceLocation id, byte[] payload) {
        private static void encode(S2CPayload message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.id);
            buffer.writeVarInt(message.payload.length);
            buffer.writeBytes(message.payload);
        }

        private static S2CPayload decode(FriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            int length = buffer.readVarInt();
            byte[] payload = new byte[length];
            buffer.readBytes(payload);
            return new S2CPayload(id, payload);
        }

        private static void handle(S2CPayload message, Supplier<NetworkEvent.Context> contextSupplier) {
            NetworkEvent.Context context = contextSupplier.get();
            context.enqueueWork(() -> {
                Consumer<FriendlyByteBuf> handler = CLIENT_HANDLERS.get(message.id);
                if (handler != null) {
                    handler.accept(wrap(message.payload));
                }
            });
            context.setPacketHandled(true);
        }
    }
}

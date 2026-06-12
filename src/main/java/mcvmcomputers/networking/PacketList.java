package mcvmcomputers.networking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.screens.Screen;


import com.mojang.blaze3d.platform.NativeImage;
import io.netty.buffer.Unpooled;
import mcvmcomputers.MainMod;
import mcvmcomputers.client.ClientMod;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.item.ItemHarddrive;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.item.OrderableItem;
import mcvmcomputers.utils.TabletOrder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import static mcvmcomputers.client.ClientMod.*;

/**
 * Mod network layer: definition, registration and handling of client<->server packets.
 *
 * <p>Uses the NeoForge payload system. All messages go through two types -
 * {@link C2SPayload} (client->server, type {@code c2s_main}) and its server
 * counterpart - where the {@code channel} field (the {@code C2S_*}/{@code S2C_*}
 * strings) selects the specific command and {@code data} carries the payload.</p>
 *
 * <p>Client->server ({@code C2S_*}): placing an order, sending the VM screen frame,
 * turning the PC on/off, adding/removing components (board, RAM, CPU, GPU, drive,
 * ISO). Server->client ({@code S2C_*}): the screen frame to render on other players'
 * PCs, stopping a screen, and order status synchronization.</p>
 *
 * <p>{@link #registerPackets(PayloadRegistrar)} is called from
 * {@link mcvmcomputers.MainMod}. The helper methods {@code sendToServer},
 * {@code sendToPlayer}, {@code sendToTracking} simplify sending.</p>
 */
/**
 * Central registry of network packet identifiers exchanged between client and
 * server (ordering, VM screen streaming, power on/off and PC part install/remove),
 * plus helper methods that drop installed parts back into the world.
 */
public class PacketList {
    private static final ResourceLocation C2S_TYPE = ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "c2s_main");
    private static final ResourceLocation S2C_TYPE = ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "s2c_main");

    private static final String C2S_ORDER = "c2s_order";
    private static final String C2S_SCREEN = "c2s_screen";
    private static final String C2S_CHANGE_HDD = "c2s_change_hdd";
    private static final String C2S_TURN_ON_PC = "c2s_turn_on_pc";
    private static final String C2S_TURN_OFF_PC = "c2s_turn_off_pc";
    private static final String C2S_ADD_MOBO = "c2s_add_mobo";
    private static final String C2S_ADD_RAM = "c2s_add_ram";
    private static final String C2S_ADD_CPU = "c2s_add_cpu";
    private static final String C2S_ADD_GPU = "c2s_add_gpu";
    private static final String C2S_ADD_HARD_DRIVE = "c2s_add_hard_drive";
    private static final String C2S_ADD_ISO = "c2s_add_iso";
    private static final String C2S_REMOVE_MOBO = "c2s_remove_mobo";
    private static final String C2S_REMOVE_RAM = "c2s_remove_ram";
    private static final String C2S_REMOVE_CPU = "c2s_remove_cpu";
    private static final String C2S_REMOVE_GPU = "c2s_remove_gpu";
    private static final String C2S_REMOVE_HARD_DRIVE = "c2s_remove_hard_drive";
    private static final String C2S_REMOVE_ISO = "c2s_remove_iso";

    private static final String S2C_SCREEN = "s2c_screen";
    private static final String S2C_STOP_SCREEN = "s2c_stop_screen";
    private static final String S2C_SYNC_ORDER = "s2c_sync_order";

    public record C2SPayload(String channel, FriendlyByteBuf data) implements CustomPacketPayload {
        public static final Type<C2SPayload> TYPE = new Type<>(C2S_TYPE);
        public static final StreamCodec<RegistryFriendlyByteBuf, C2SPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public C2SPayload decode(RegistryFriendlyByteBuf buf) {
                String channel = buf.readUtf();
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new C2SPayload(channel, new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
            }
            @Override public void encode(RegistryFriendlyByteBuf buf, C2SPayload value) {
                buf.writeUtf(value.channel);
                byte[] bytes = new byte[value.data.readableBytes()];
                value.data.getBytes(value.data.readerIndex(), bytes);
                buf.writeBytes(bytes);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record S2CPayload(String channel, FriendlyByteBuf data) implements CustomPacketPayload {
        public static final Type<S2CPayload> TYPE = new Type<>(S2C_TYPE);
        public static final StreamCodec<RegistryFriendlyByteBuf, S2CPayload> STREAM_CODEC = new StreamCodec<>() {
            @Override public S2CPayload decode(RegistryFriendlyByteBuf buf) {
                String channel = buf.readUtf();
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new S2CPayload(channel, new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes)));
            }
            @Override public void encode(RegistryFriendlyByteBuf buf, S2CPayload value) {
                buf.writeUtf(value.channel);
                byte[] bytes = new byte[value.data.readableBytes()];
                value.data.getBytes(value.data.readerIndex(), bytes);
                buf.writeBytes(bytes);
            }
        };
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPackets(PayloadRegistrar registrar) {
        registrar.playToServer(C2SPayload.TYPE, C2SPayload.STREAM_CODEC, PacketList::handleC2S);
        registrar.playToClient(S2CPayload.TYPE, S2CPayload.STREAM_CODEC, PacketList::handleS2C);
    }

    public static void sendToServer(String channel, FriendlyByteBuf data) {
        PacketDistributor.sendToServer(new C2SPayload(channel, data));
    }

    public static void sendToPlayer(ServerPlayer player, String channel, FriendlyByteBuf data) {
        PacketDistributor.sendToPlayer(player, new S2CPayload(channel, data));
    }

    public static void sendToTracking(Entity entity, String channel, FriendlyByteBuf data) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, new S2CPayload(channel, data));
    }

    private static void handleC2S(C2SPayload payload, IPayloadContext ctx) {
        switch (payload.channel) {
            case C2S_ORDER -> handleC2SOrder(payload.data, ctx);
            case C2S_SCREEN -> handleC2SScreen(payload.data, ctx);
            case C2S_CHANGE_HDD -> handleC2SChangeHdd(payload.data, ctx);
            case C2S_TURN_ON_PC -> handleC2STurnOnPc(payload.data, ctx);
            case C2S_TURN_OFF_PC -> handleC2STurnOffPc(payload.data, ctx);
            case C2S_ADD_MOBO -> handleC2SAddMobo(payload.data, ctx);
            case C2S_ADD_RAM -> handleC2SAddRam(payload.data, ctx);
            case C2S_ADD_CPU -> handleC2SAddCpu(payload.data, ctx);
            case C2S_ADD_GPU -> handleC2SAddGpu(payload.data, ctx);
            case C2S_ADD_HARD_DRIVE -> handleC2SAddHardDrive(payload.data, ctx);
            case C2S_ADD_ISO -> handleC2SAddIso(payload.data, ctx);
            case C2S_REMOVE_MOBO -> handleC2SRemoveMobo(payload.data, ctx);
            case C2S_REMOVE_RAM -> handleC2SRemoveRam(payload.data, ctx);
            case C2S_REMOVE_CPU -> handleC2SRemoveCpu(payload.data, ctx);
            case C2S_REMOVE_GPU -> handleC2SRemoveGpu(payload.data, ctx);
            case C2S_REMOVE_HARD_DRIVE -> handleC2SRemoveHdd(payload.data, ctx);
            case C2S_REMOVE_ISO -> handleC2SRemoveIso(payload.data, ctx);
        }
    }

    private static void handleS2C(S2CPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            switch (payload.channel) {
                case S2C_SCREEN -> handleS2CScreen(payload.data);
                case S2C_STOP_SCREEN -> handleS2CStopScreen(payload.data);
                case S2C_SYNC_ORDER -> handleS2CSyncOrder(payload.data);
            }
        });
    }

    // --- C2S handlers ---

    private static void handleC2SOrder(FriendlyByteBuf buf, IPayloadContext ctx) {
        int arraySize = buf.readInt();
        List<OrderableItem> itemList = new ArrayList<>();
        int price = 0;
        for (int i = 0; i < arraySize; i++) {
            String itemId = buf.readUtf();
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(itemId);
            if (rl != null) {
                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                if (item instanceof OrderableItem oi) {
                    itemList.add(oi);
                    price += oi.getPrice();
                }
            }
        }
        final int pr = price;
        final List<OrderableItem> finalItems = new ArrayList<>(itemList);
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            TabletOrder to = new TabletOrder();
            to.items = finalItems;
            to.price = pr;
            to.orderUUID = player.getStringUUID();
            MainMod.orders.put(player.getUUID(), to);
        });
    }

    private static void handleC2SScreen(FriendlyByteBuf buf, IPayloadContext ctx) {
        byte[] screen = buf.readByteArray();
        int compressedDataSize = buf.readInt();
        int dataSize = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            if (MainMod.computers.containsKey(player.getUUID())) {
                FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
                b.writeByteArray(screen);
                b.writeInt(compressedDataSize);
                b.writeInt(dataSize);
                b.writeUUID(player.getUUID());
                sendToTracking(MainMod.computers.get(player.getUUID()), S2C_SCREEN, b);
            }
        });
    }

    private static void handleC2STurnOnPc(FriendlyByteBuf buf, IPayloadContext ctx) {
        int pcEntityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(pcEntityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                MainMod.computers.put(player.getUUID(), pc);
            }
        });
    }

    private static void handleC2STurnOffPc(FriendlyByteBuf buf, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            if (MainMod.computers.containsKey(player.getUUID())) {
                FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
                b.writeUUID(player.getUUID());
                sendToTracking(MainMod.computers.get(player.getUUID()), S2C_STOP_SCREEN, b);
                MainMod.computers.remove(player.getUUID());
            }
        });
    }

    private static void handleC2SChangeHdd(FriendlyByteBuf buf, IPayloadContext ctx) {
        String newHddName = buf.readUtf();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            for (ItemStack is : player.getHandSlots()) {
                if (is.getItem() instanceof ItemHarddrive) {
                    CompoundTag ct = new CompoundTag();
                    ct.putString("vhdfile", newHddName);
                    is.set(DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(ct));
                    break;
                }
            }
        });
    }

    private static void handleC2SAddMobo(FriendlyByteBuf buf, IPayloadContext ctx) {
        boolean x64 = buf.readBoolean();
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Item lookingFor = x64 ? ItemList.ITEM_MOTHERBOARD64 : ItemList.ITEM_MOTHERBOARD;
            if (player.getInventory().contains(new ItemStack(lookingFor))) {
                Entity e = player.level().getEntity(entityId);
                if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                    if (!pc.getMotherboardInstalled()) {
                        removeStack(player.getInventory(), new ItemStack(lookingFor));
                        pc.setMotherboardInstalled(true);
                        pc.set64Bit(x64);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("mcvmcomputers.motherboard_not_present").withStyle(ChatFormatting.RED));
            }
        });
    }

    private static void handleC2SAddGpu(FriendlyByteBuf buf, IPayloadContext ctx) {
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            if (player.getInventory().contains(new ItemStack(ItemList.ITEM_GPU))) {
                Entity e = player.level().getEntity(entityId);
                if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                    if (!pc.getGpuInstalled()) {
                        removeStack(player.getInventory(), new ItemStack(ItemList.ITEM_GPU));
                        pc.setGpuInstalled(true);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("mcvmcomputers.gpu_not_present").withStyle(ChatFormatting.RED));
            }
        });
    }

    private static void handleC2SAddCpu(FriendlyByteBuf buf, IPayloadContext ctx) {
        int dividedBy = buf.readInt();
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Item lookingFor = switch (dividedBy) { case 2 -> ItemList.ITEM_CPU2; case 4 -> ItemList.ITEM_CPU4; case 6 -> ItemList.ITEM_CPU6; default -> null; };
            if (lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
                Entity e = player.level().getEntity(entityId);
                if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                    if (pc.getCpuDividedBy() == 0) {
                        removeStack(player.getInventory(), new ItemStack(lookingFor));
                        pc.setCpuDividedBy(dividedBy);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("mcvmcomputers.ram_not_present").withStyle(ChatFormatting.RED));
            }
        });
    }

    private static void handleC2SAddRam(FriendlyByteBuf buf, IPayloadContext ctx) {
        int mb = buf.readInt();
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Item lookingFor = getRamItem(mb);
            if (lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
                Entity e = player.level().getEntity(entityId);
                if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                    if (pc.getGigsOfRamInSlot0() == 0) {
                        removeStack(player.getInventory(), new ItemStack(lookingFor));
                        pc.setGigsOfRamInSlot0(mb);
                    } else if (pc.getGigsOfRamInSlot1() == 0) {
                        removeStack(player.getInventory(), new ItemStack(lookingFor));
                        pc.setGigsOfRamInSlot1(mb);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("mcvmcomputers.hdd_not_present").withStyle(ChatFormatting.RED));
            }
        });
    }

    private static void handleC2SAddHardDrive(FriendlyByteBuf buf, IPayloadContext ctx) {
        String vhdname = buf.readUtf();
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            ItemStack lookingFor = ItemHarddrive.createHardDrive(vhdname);
            if (player.getInventory().contains(lookingFor)) {
                Entity e = player.level().getEntity(entityId);
                if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                    if (pc.getHardDriveFileName().isEmpty()) {
                        removeStack(player.getInventory(), lookingFor);
                        pc.setHardDriveFileName(vhdname);
                    }
                }
            } else {
                player.sendSystemMessage(Component.translatable("mcvmcomputers.hdd_not_present").withStyle(ChatFormatting.RED));
            }
        });
    }

    private static void handleC2SRemoveMobo(FriendlyByteBuf buf, IPayloadContext ctx) {
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                if (pc.getMotherboardInstalled()) {
                    pc.setMotherboardInstalled(false);
                    Item moboItem = pc.get64Bit() ? ItemList.ITEM_MOTHERBOARD64 : ItemList.ITEM_MOTHERBOARD;
                    pc.level().addFreshEntity(new ItemEntity(pc.level(), pc.getX(), pc.getY(), pc.getZ(), new ItemStack(moboItem)));
                    removeCpu(pc); removeGpu(pc); removeHdd(pc, player.getStringUUID());
                    removeRam(pc, 0); removeRam(pc, 1);
                }
            }
        });
    }

    private static void handleC2SRemoveGpu(FriendlyByteBuf buf, IPayloadContext ctx) {
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) removeGpu(pc);
        });
    }

    private static void handleC2SRemoveHdd(FriendlyByteBuf buf, IPayloadContext ctx) {
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) removeHdd(pc, player.getStringUUID());
        });
    }

    private static void handleC2SRemoveCpu(FriendlyByteBuf buf, IPayloadContext ctx) {
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) removeCpu(pc);
        });
    }

    private static void handleC2SRemoveRam(FriendlyByteBuf buf, IPayloadContext ctx) {
        int slot = buf.readInt();
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) removeRam(pc, slot);
        });
    }

    private static void handleC2SAddIso(FriendlyByteBuf buf, IPayloadContext ctx) {
        String isoName = buf.readUtf();
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                if (pc.getIsoFileName().isEmpty()) pc.setIsoFileName(isoName);
            }
        });
    }

    private static void handleC2SRemoveIso(FriendlyByteBuf buf, IPayloadContext ctx) {
        int entityId = buf.readInt();
        ServerPlayer player = (ServerPlayer) ctx.player();
        player.server.execute(() -> {
            Entity e = player.level().getEntity(entityId);
            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getStringUUID())) {
                if (!pc.getIsoFileName().isEmpty()) pc.setIsoFileName("");
            }
        });
    }

    // --- S2C handlers ---

    private static void handleS2CScreen(FriendlyByteBuf buf) {
        byte[] screen = buf.readByteArray();
        int compressedDataSize = buf.readInt();
        int dataSize = buf.readInt();
        UUID pcOwner = buf.readUUID();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !pcOwner.equals(mc.player.getUUID())) {
            if (ClientMod.vmScreenTextures.containsKey(pcOwner)) {
                mc.getTextureManager().release(ClientMod.vmScreenTextures.get(pcOwner));
                vmScreenTextures.remove(pcOwner);
            }
            if (ClientMod.vmScreenTextureNI.containsKey(pcOwner)) {
                ClientMod.vmScreenTextureNI.get(pcOwner).close();
                vmScreenTextureNI.remove(pcOwner);
            }
            if (ClientMod.vmScreenTextureNIBT.containsKey(pcOwner)) {
                ClientMod.vmScreenTextureNIBT.get(pcOwner).close();
                vmScreenTextureNIBT.remove(pcOwner);
            }
            try {
                Inflater inf = new Inflater();
                inf.setInput(screen, 0, compressedDataSize);
                byte[] actualScreen = new byte[dataSize + 1];
                int size = inf.inflate(actualScreen);
                inf.end();
                NativeImage ni = NativeImage.read(new ByteArrayInputStream(actualScreen, 0, size));
                DynamicTexture nibt = new DynamicTexture(ni);
                ClientMod.vmScreenTextures.put(pcOwner, mc.getTextureManager().register("pc_screen_mp", nibt));
                ClientMod.vmScreenTextureNI.put(pcOwner, ni);
                ClientMod.vmScreenTextureNIBT.put(pcOwner, nibt);
            } catch (IOException | DataFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleS2CStopScreen(FriendlyByteBuf buf) {
        UUID pcOwner = buf.readUUID();
        Minecraft mc = Minecraft.getInstance();
        if (ClientMod.vmScreenTextures.containsKey(pcOwner)) {
            mc.getTextureManager().release(ClientMod.vmScreenTextures.get(pcOwner));
            vmScreenTextures.remove(pcOwner);
        }
        if (ClientMod.vmScreenTextureNI.containsKey(pcOwner)) {
            ClientMod.vmScreenTextureNI.get(pcOwner).close();
            vmScreenTextureNI.remove(pcOwner);
        }
        if (ClientMod.vmScreenTextureNIBT.containsKey(pcOwner)) {
            ClientMod.vmScreenTextureNIBT.get(pcOwner).close();
            vmScreenTextureNIBT.remove(pcOwner);
        }
    }

    private static void handleS2CSyncOrder(FriendlyByteBuf buf) {
        String orderUUID = buf.readUtf();
        int arraySize = buf.readInt();
        List<OrderableItem> itemList = new ArrayList<>();
        for (int i = 0; i < arraySize; i++) {
            String itemId = buf.readUtf();
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(itemId);
            if (rl != null) {
                Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(rl);
                if (item instanceof OrderableItem oi) itemList.add(oi);
            }
        }
        int price = buf.readInt();
        TabletOrder.OrderStatus status = TabletOrder.OrderStatus.values()[buf.readInt()];

        if (ClientMod.myOrder == null) ClientMod.myOrder = new TabletOrder();
        ClientMod.myOrder.orderUUID = orderUUID;
        ClientMod.myOrder.price = price;
        ClientMod.myOrder.items = new ArrayList<>(itemList);
        ClientMod.myOrder.currentStatus = status;
    }

    // --- Helpers ---

    private static void removeStack(Inventory inv, ItemStack is) {
        for (var list : List.of(inv.items, inv.armor, inv.offhand)) {
            for (var itemStack : list) {
                if (!itemStack.isEmpty() && itemStack.is(is.getItem())
                    && Objects.equals(getNbtSafe(itemStack), getNbtSafe(is))) {
                    itemStack.shrink(1);
                    return;
                }
            }
        }
        for (var list : List.of(inv.items, inv.armor, inv.offhand)) {
            for (var itemStack : list) {
                if (!itemStack.isEmpty() && itemStack.is(is.getItem())) {
                    itemStack.shrink(1);
                    return;
                }
            }
        }
    }

    private static CompoundTag getNbtSafe(ItemStack stack) {
        var cd = stack.get(DataComponents.CUSTOM_DATA);
        return cd != null ? cd.copyTag() : null;
    }

    public static void removeGpu(EntityPC pc) {
        if (pc.getGpuInstalled()) {
            pc.setGpuInstalled(false);
            pc.level().addFreshEntity(new ItemEntity(pc.level(), pc.getX(), pc.getY(), pc.getZ(), new ItemStack(ItemList.ITEM_GPU)));
        }
    }

    public static void removeHdd(EntityPC pc, String uuid) {
        if (!pc.getHardDriveFileName().isEmpty()) {
            pc.level().addFreshEntity(new ItemEntity(pc.level(), pc.getX(), pc.getY(), pc.getZ(),
                ItemHarddrive.createHardDrive(pc.getHardDriveFileName())));
            pc.setHardDriveFileName("");
        }
    }

    public static void removeCpu(EntityPC pc) {
        if (pc.getCpuDividedBy() > 0) {
            Item cpuItem = switch (pc.getCpuDividedBy()) {
                case 2 -> ItemList.ITEM_CPU2; case 4 -> ItemList.ITEM_CPU4; case 6 -> ItemList.ITEM_CPU6; default -> null;
            };
            if (cpuItem != null) {
                pc.setCpuDividedBy(0);
                pc.level().addFreshEntity(new ItemEntity(pc.level(), pc.getX(), pc.getY(), pc.getZ(), new ItemStack(cpuItem)));
            }
        }
    }

    public static void removeRam(EntityPC pc, int slot) {
        Item ramStickItem;
        if (slot == 0) {
            ramStickItem = getRamItem(pc.getGigsOfRamInSlot0());
            if (ramStickItem != null) pc.setGigsOfRamInSlot0(0);
        } else {
            ramStickItem = getRamItem(pc.getGigsOfRamInSlot1());
            if (ramStickItem != null) pc.setGigsOfRamInSlot1(0);
        }
        if (ramStickItem != null) {
            pc.level().addFreshEntity(new ItemEntity(pc.level(), pc.getX(), pc.getY(), pc.getZ(), new ItemStack(ramStickItem)));
        }
    }

    private static Item getRamItem(int mb) {
        return switch (mb) {
            case 64 -> ItemList.ITEM_RAM64M; case 128 -> ItemList.ITEM_RAM128M; case 256 -> ItemList.ITEM_RAM256M;
            case 512 -> ItemList.ITEM_RAM512M; case 1024 -> ItemList.ITEM_RAM1G; case 2048 -> ItemList.ITEM_RAM2G;
            case 4096 -> ItemList.ITEM_RAM4G; default -> null;
        };
    }
}

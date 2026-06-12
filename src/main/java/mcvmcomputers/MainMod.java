package mcvmcomputers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.UnmodifiableIterator;

import mcvmcomputers.entities.EntityList;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.item.ItemHarddrive;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.item.OrderableItem;
import mcvmcomputers.networking.Net;
import mcvmcomputers.networking.PacketByteBufs;
import mcvmcomputers.networking.PlayerLookup;
import mcvmcomputers.networking.ServerPlayNetworking;
import mcvmcomputers.sound.SoundList;
import mcvmcomputers.utils.TabletOrder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.registry.RegistryKeys;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

import static mcvmcomputers.networking.PacketList.*;

/**
 * Forge mod entry point. Registers items, entities, sounds and creative tabs via
 * RegisterEvent on the mod event bus, initialises the networking channel and
 * server-side packet handlers, and holds the global state shared between sides
 * (active orders and computers).
 */
@Mod("mcvmcomputers")
public class MainMod {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
    public static Map<UUID, TabletOrder> orders;
    public static Map<UUID, EntityPC> computers;

    public static Runnable hardDriveClick = () -> {};
    public static Runnable deliveryChestSound = () -> {};
    public static Runnable focus = () -> {};
    public static Runnable pcOpenGui = () -> {};

    public MainMod() {
        orders    = new HashMap<>();
        computers = new HashMap<>();

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Vanilla registries are frozen outside of RegisterEvent on Forge, so all
        // registration happens inside this listener via the RegisterHelper.
        modBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(RegistryKeys.ITEM)) {
                event.register(RegistryKeys.ITEM, ItemList::registerItems);
            } else if (event.getRegistryKey().equals(RegistryKeys.ENTITY_TYPE)) {
                event.register(RegistryKeys.ENTITY_TYPE, EntityList::init);
            } else if (event.getRegistryKey().equals(RegistryKeys.SOUND_EVENT)) {
                event.register(RegistryKeys.SOUND_EVENT, SoundList::init);
            } else if (event.getRegistryKey().equals(RegistryKeys.ITEM_GROUP)) {
                event.register(RegistryKeys.ITEM_GROUP, ItemList::registerGroups);
            }
        });

        // Networking + server packet handlers are safe to set up immediately.
        Net.init();
        registerServerPackets();

        // Client-only setup (entity renderers etc.) lives in ClientMod, wired here
        // so the dedicated server never touches client classes.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            mcvmcomputers.client.ClientMod.init(modBus);
        }
    }

    public static void registerServerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_ORDER,
                (server, player, handler, attachedData, responseSender) -> {
                    int arraySize = attachedData.readInt();
                    OrderableItem[] items = new OrderableItem[arraySize];
                    int price = 0;
                    for (int i = 0; i < arraySize; i++) {
                        Item readItem = attachedData.readItemStack().getItem();
                        if (readItem instanceof OrderableItem) {
                            items[i] = (OrderableItem) readItem;
                        } else {
                            LOGGER.warn("C2S_ORDER: received non-OrderableItem, ignoring");
                            items[i] = null;
                        }
                        price += items[i].getPrice();
                    }
                    final int pr = price;
                    server.execute(() -> {
                        TabletOrder to = new TabletOrder();
                        to.items = new ArrayList<>();
                        to.items.addAll(Arrays.asList(items));
                        to.price = pr;
                        to.orderUUID = player.getUuid().toString();
                        MainMod.orders.put(player.getUuid(), to);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_SCREEN,
                (server, player, handler, attachedData, responseSender) -> {
                    byte[] screen = attachedData.readByteArray();
                    int compressedDataSize = attachedData.readInt();
                    int dataSize = attachedData.readInt();
                    server.execute(() -> {
                        if (MainMod.computers.containsKey(player.getUuid())) {
                            PacketByteBuf b = PacketByteBufs.create();
                            b.writeByteArray(screen);
                            b.writeInt(compressedDataSize);
                            b.writeInt(dataSize);
                            b.writeUuid(player.getUuid());
                            for (ServerPlayerEntity watcher : PlayerLookup
                                    .tracking(MainMod.computers.get(player.getUuid()))) {
                                ServerPlayNetworking.send(watcher, S2C_SCREEN, b);
                            }
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_TURN_ON_PC,
                (server, player, handler, attachedData, responseSender) -> {
                    int pcEntityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(pcEntityId);
                        if (e instanceof EntityPC pc) {
                            if (pc.getOwner().equals(player.getUuid().toString())) {
                                MainMod.computers.put(player.getUuid(), pc);
                            }
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_TURN_OFF_PC,
                (server, player, handler, attachedData, responseSender) -> {
                    server.execute(() -> {
                        if (MainMod.computers.containsKey(player.getUuid())) {
                            PacketByteBuf b = PacketByteBufs.create();
                            b.writeUuid(player.getUuid());
                            for (ServerPlayerEntity watcher : PlayerLookup
                                    .tracking(MainMod.computers.get(player.getUuid()))) {
                                ServerPlayNetworking.send(watcher, S2C_STOP_SCREEN, b);
                            }
                            MainMod.computers.remove(player.getUuid());
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_CHANGE_HDD,
                (server, player, handler, attachedData, responseSender) -> {
                    String newHddName = attachedData.readString(32767);
                    server.execute(() -> {
                        for (ItemStack is : player.getHandItems()) {
                            if (is != null && is.getItem() instanceof ItemHarddrive) {
                                NbtCompound ct = is.getOrCreateNbt();
                                ct.putString("vhdfile", newHddName);
                                break;
                            }
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_MOBO,
                (server, player, handler, attachedData, responseSender) -> {
                    boolean x64 = attachedData.readBoolean();
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Item lookingFor = x64 ? ItemList.ITEM_MOTHERBOARD64 : ItemList.ITEM_MOTHERBOARD;
                        if (player.getInventory().contains(new ItemStack(lookingFor))) {
                            Entity e = player.getWorld().getEntityById(entityId);
                            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                                if (!pc.getMotherboardInstalled()) {
                                    removeStck(player.getInventory(), new ItemStack(lookingFor));
                                    pc.setMotherboardInstalled(true);
                                    pc.set64Bit(x64);
                                }
                            }
                        } else {
                            player.sendMessage(Text.translatable("mcvmcomputers.motherboard_not_present").formatted(Formatting.RED), false);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_GPU,
                (server, player, handler, attachedData, responseSender) -> {
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        if (player.getInventory().contains(new ItemStack(ItemList.ITEM_GPU))) {
                            Entity e = player.getWorld().getEntityById(entityId);
                            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                                if (!pc.getGpuInstalled()) {
                                    removeStck(player.getInventory(), new ItemStack(ItemList.ITEM_GPU));
                                    pc.setGpuInstalled(true);
                                }
                            }
                        } else {
                            player.sendMessage(Text.translatable("mcvmcomputers.gpu_not_present").formatted(Formatting.RED), false);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_CPU,
                (server, player, handler, attachedData, responseSender) -> {
                    int dividedBy = attachedData.readInt();
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Item lookingFor = switch (dividedBy) {
                            case 2 -> ItemList.ITEM_CPU2;
                            case 4 -> ItemList.ITEM_CPU4;
                            case 6 -> ItemList.ITEM_CPU6;
                            default -> null;
                        };
                        if (lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
                            Entity e = player.getWorld().getEntityById(entityId);
                            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                                if (pc.getCpuDividedBy() == 0) {
                                    removeStck(player.getInventory(), new ItemStack(lookingFor));
                                    pc.setCpuDividedBy(dividedBy);
                                }
                            }
                        } else {
                            player.sendMessage(Text.translatable("mcvmcomputers.ram_not_present").formatted(Formatting.RED), false);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_RAM,
                (server, player, handler, attachedData, responseSender) -> {
                    int mb = attachedData.readInt();
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Item lookingFor = switch (mb) {
                            case 64   -> ItemList.ITEM_RAM64M;
                            case 128  -> ItemList.ITEM_RAM128M;
                            case 256  -> ItemList.ITEM_RAM256M;
                            case 512  -> ItemList.ITEM_RAM512M;
                            case 1024 -> ItemList.ITEM_RAM1G;
                            case 2048 -> ItemList.ITEM_RAM2G;
                            case 4096 -> ItemList.ITEM_RAM4G;
                            default   -> null;
                        };
                        if (lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
                            Entity e = player.getWorld().getEntityById(entityId);
                            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                                if (pc.getGigsOfRamInSlot0() == 0) {
                                    removeStck(player.getInventory(), new ItemStack(lookingFor));
                                    pc.setGigsOfRamInSlot0(mb);
                                } else if (pc.getGigsOfRamInSlot1() == 0) {
                                    removeStck(player.getInventory(), new ItemStack(lookingFor));
                                    pc.setGigsOfRamInSlot1(mb);
                                }
                            }
                        } else {
                            player.sendMessage(Text.translatable("mcvmcomputers.hdd_not_present").formatted(Formatting.RED), false);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_HARD_DRIVE,
                (server, player, handler, attachedData, responseSender) -> {
                    String vhdname = attachedData.readString(32767);
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        ItemStack lookingFor = ItemHarddrive.createHardDrive(vhdname);
                        if (player.getInventory().contains(lookingFor)) {
                            Entity e = player.getWorld().getEntityById(entityId);
                            if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                                if (pc.getHardDriveFileName().isEmpty()) {
                                    removeStck(player.getInventory(), lookingFor);
                                    pc.setHardDriveFileName(vhdname);
                                }
                            }
                        } else {
                            player.sendMessage(Text.translatable("mcvmcomputers.hdd_not_present").formatted(Formatting.RED), false);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_MOBO,
                (server, player, handler, attachedData, responseSender) -> {
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(entityId);
                        if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                            if (pc.getMotherboardInstalled()) {
                                pc.setMotherboardInstalled(false);
                                Item drop = pc.get64Bit() ? ItemList.ITEM_MOTHERBOARD64 : ItemList.ITEM_MOTHERBOARD;
                                pc.getWorld().spawnEntity(new ItemEntity(pc.getWorld(), pc.getX(), pc.getY(), pc.getZ(), new ItemStack(drop)));
                                removeCpu(pc); removeGpu(pc);
                                removeHdd(pc, player.getUuid().toString());
                                removeRam(pc, 0); removeRam(pc, 1);
                            }
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_GPU,
                (server, player, handler, attachedData, responseSender) -> {
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(entityId);
                        if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString()))
                            removeGpu(pc);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_HARD_DRIVE,
                (server, player, handler, attachedData, responseSender) -> {
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(entityId);
                        if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString()))
                            removeHdd(pc, player.getUuid().toString());
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_CPU,
                (server, player, handler, attachedData, responseSender) -> {
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(entityId);
                        if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString()))
                            removeCpu(pc);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_RAM,
                (server, player, handler, attachedData, responseSender) -> {
                    int slot = attachedData.readInt();
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(entityId);
                        if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString()))
                            removeRam(pc, slot);
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_ISO,
                (server, player, handler, attachedData, responseSender) -> {
                    String isoName = attachedData.readString(32767);
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(entityId);
                        if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                            if (pc.getIsoFileName().isEmpty()) pc.setIsoFileName(isoName);
                        }
                    });
                });

        ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_ISO,
                (server, player, handler, attachedData, responseSender) -> {
                    int entityId = attachedData.readInt();
                    server.execute(() -> {
                        Entity e = player.getWorld().getEntityById(entityId);
                        if (e instanceof EntityPC pc && pc.getOwner().equals(player.getUuid().toString())) {
                            if (!pc.getIsoFileName().isEmpty()) pc.setIsoFileName("");
                        }
                    });
                });
    }

    private static void removeStck(PlayerInventory inv, ItemStack is) {
        UnmodifiableIterator<DefaultedList<ItemStack>> var2 = ImmutableList.of(inv.main, inv.armor, inv.offHand).iterator();
        if (is.getNbt() != null) {
            while (var2.hasNext()) {
                for (ItemStack stack : var2.next()) {
                    if (!stack.isEmpty() && stack.isOf(is.getItem()) && Objects.equals(stack.getNbt(), is.getNbt())) {
                        stack.decrement(1); return;
                    }
                }
            }
            var2 = ImmutableList.of(inv.main, inv.armor, inv.offHand).iterator();
        }
        while (var2.hasNext()) {
            for (ItemStack stack : var2.next()) {
                if (!stack.isEmpty() && stack.isOf(is.getItem())) {
                    stack.decrement(1); return;
                }
            }
        }
        LOGGER.error("removeStck: item not found in inventory (desync?)");
    }
}

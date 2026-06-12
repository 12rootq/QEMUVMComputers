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
import mcvmcomputers.sound.SoundList;
import mcvmcomputers.utils.TabletOrder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;

import mcvmcomputers.networking.PacketList;
import static mcvmcomputers.networking.PacketList.*;

/**
 * Common (client + server) mod entry point. Initialises the item, entity and
 * sound registries, registers the server-side networking packet handlers and
 * holds the global state shared between sides (active orders and computers).
 */
public class MainMod implements ModInitializer {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
	public static Map<UUID, TabletOrder> orders;
	public static Map<UUID, EntityPC> computers;

	public static Runnable hardDriveClick = new Runnable() {
		@Override
		public void run() {
		}
	};
	public static Runnable deliveryChestSound = new Runnable() {
		@Override
		public void run() {
		}
	};
	public static Runnable focus = new Runnable() {
		@Override
		public void run() {
		}
	};
	public static Runnable pcOpenGui = new Runnable() {
		@Override
		public void run() {
		}
	};

	public void onInitialize() {
		orders = new HashMap<UUID, TabletOrder>();
		computers = new HashMap<UUID, EntityPC>();
		ItemList.init();
		EntityList.init();
		SoundList.init();
		registerServerPackets();
	}

	public static void registerServerPackets() {
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ORDER), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_SCREEN), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_CHANGE_HDD), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_TURN_ON_PC), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_TURN_OFF_PC), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_MOBO), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_RAM), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_CPU), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_GPU), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_HARD_DRIVE), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_ISO), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_MOBO), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_RAM), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_CPU), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_GPU), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_HARD_DRIVE), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_ISO), RawBytesPayload.CODEC);

		PayloadTypeRegistry.playS2C().register(new net.minecraft.network.packet.CustomPayload.Id<>(S2C_SCREEN), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(new net.minecraft.network.packet.CustomPayload.Id<>(S2C_STOP_SCREEN), RawBytesPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(new net.minecraft.network.packet.CustomPayload.Id<>(S2C_SYNC_ORDER), RawBytesPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ORDER),
				(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

					int arraySize = attachedData.readInt();
java.util.List<OrderableItem> itemList = new java.util.ArrayList<>();
int price = 0;
for (int i = 0; i < arraySize; i++) {
	Item readItem = net.minecraft.registry.Registries.ITEM.get(net.minecraft.util.Identifier.of(attachedData.readString()));
	if (readItem instanceof OrderableItem oi) {
		itemList.add(oi);
		price += oi.getPrice();
	} else {
		LOGGER.warn("C2S_ORDER: received non-OrderableItem, ignoring");
	}
}

final int pr = price;
final java.util.List<OrderableItem> finalItems = new java.util.ArrayList<>(itemList);

context.server().execute(() -> {
	TabletOrder to = new TabletOrder();
	to.items = finalItems;
	to.price = pr;
	to.orderUUID = context.player().getUuid().toString();
	MainMod.orders.put(context.player().getUuid(), to);
});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_SCREEN),
				(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

					byte[] screen = attachedData.readByteArray();
					int compressedDataSize = attachedData.readInt();
					int dataSize = attachedData.readInt();

					context.server().execute(() -> {
						if (MainMod.computers.containsKey(context.player().getUuid())) {
							PacketByteBuf b = PacketByteBufs.create();
							b.writeByteArray(screen);
							b.writeInt(compressedDataSize);
							b.writeInt(dataSize);
							b.writeUuid(context.player().getUuid());
							for (ServerPlayerEntity watcher : PlayerLookup
									.tracking(MainMod.computers.get(context.player().getUuid()))) {
								ServerPlayNetworking.send(watcher, new PacketList.RawBytesPayload(S2C_SCREEN, b));
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_TURN_ON_PC),
				(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

					int pcEntityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(pcEntityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									MainMod.computers.put(context.player().getUuid(), (EntityPC) e);
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_TURN_OFF_PC),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();
				context.server().execute(() -> {
					if (MainMod.computers.containsKey(context.player().getUuid())) {
							PacketByteBuf b = PacketByteBufs.create();
							b.writeUuid(context.player().getUuid());
							for (ServerPlayerEntity watcher : PlayerLookup
									.tracking(MainMod.computers.get(context.player().getUuid()))) {
								ServerPlayNetworking.send(watcher, new PacketList.RawBytesPayload(S2C_STOP_SCREEN, b));
							}
							MainMod.computers.remove(context.player().getUuid());
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_CHANGE_HDD),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				String newHddName = attachedData.readString(32767);

					context.server().execute(() -> {
						for (ItemStack is : context.player().getHandItems()) {
							if (is != null) {
								if (is.getItem() instanceof ItemHarddrive) {
									NbtCompound ct = new NbtCompound();
									ct.putString("vhdfile", newHddName);
									is.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(ct));
									break;
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_MOBO),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				boolean x64 = attachedData.readBoolean();
					int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Item lookingFor = null;
						if (x64) {
							lookingFor = ItemList.ITEM_MOTHERBOARD64;
						} else {
							lookingFor = ItemList.ITEM_MOTHERBOARD;
						}
						if (context.player().getInventory().contains(new ItemStack(lookingFor))) {
							Entity e = context.player().getWorld().getEntityById(entityId);
							if (e != null) {
								if (e instanceof EntityPC) {
									EntityPC pc = (EntityPC) e;
									if (pc.getOwner().equals(context.player().getUuid().toString())) {
										if (!pc.getMotherboardInstalled()) {
											removeStck(context.player().getInventory(), new ItemStack(lookingFor));
											pc.setMotherboardInstalled(true);
											pc.set64Bit(x64);
										}
									}
								}
							}
						} else {
							context.player().sendMessage(Text.translatable("mcvmcomputers.motherboard_not_present")
									.formatted(Formatting.RED));
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_GPU),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Item lookingFor = ItemList.ITEM_GPU;
						if (context.player().getInventory().contains(new ItemStack(lookingFor))) {
							Entity e = context.player().getWorld().getEntityById(entityId);
							if (e != null) {
								if (e instanceof EntityPC) {
									EntityPC pc = (EntityPC) e;
									if (pc.getOwner().equals(context.player().getUuid().toString())) {
										if (!pc.getGpuInstalled()) {
											removeStck(context.player().getInventory(), new ItemStack(lookingFor));
											pc.setGpuInstalled(true);
										}
									}
								}
							}
						} else {
							context.player().sendMessage(
									Text.translatable("mcvmcomputers.gpu_not_present").formatted(Formatting.RED));
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_CPU),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int dividedBy = attachedData.readInt();
					int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Item lookingFor = null;
						if (dividedBy == 2) {
							lookingFor = ItemList.ITEM_CPU2;
						} else if (dividedBy == 4) {
							lookingFor = ItemList.ITEM_CPU4;
						} else if (dividedBy == 6) {
							lookingFor = ItemList.ITEM_CPU6;
						}
						if (lookingFor != null && context.player().getInventory().contains(new ItemStack(lookingFor))) {
							Entity e = context.player().getWorld().getEntityById(entityId);
							if (e != null) {
								if (e instanceof EntityPC) {
									EntityPC pc = (EntityPC) e;
									if (pc.getOwner().equals(context.player().getUuid().toString())) {
										if (pc.getCpuDividedBy() == 0) {
											removeStck(context.player().getInventory(), new ItemStack(lookingFor));
											pc.setCpuDividedBy(dividedBy);
										}
									}
								}
							}
						} else {
							context.player().sendMessage(
									Text.translatable("mcvmcomputers.ram_not_present").formatted(Formatting.RED));
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_RAM),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int mb = attachedData.readInt();
					int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Item lookingFor = null;
						if (mb == 64) {
							lookingFor = ItemList.ITEM_RAM64M;
						} else if (mb == 128) {
							lookingFor = ItemList.ITEM_RAM128M;
						} else if (mb == 256) {
							lookingFor = ItemList.ITEM_RAM256M;
						} else if (mb == 512) {
							lookingFor = ItemList.ITEM_RAM512M;
						} else if (mb == 1024) {
							lookingFor = ItemList.ITEM_RAM1G;
						} else if (mb == 2048) {
							lookingFor = ItemList.ITEM_RAM2G;
						} else if (mb == 4096) {
							lookingFor = ItemList.ITEM_RAM4G;
						}
						if (lookingFor != null && context.player().getInventory().contains(new ItemStack(lookingFor))) {
							Entity e = context.player().getWorld().getEntityById(entityId);
							if (e != null) {
								if (e instanceof EntityPC) {
									EntityPC pc = (EntityPC) e;
									if (pc.getOwner().equals(context.player().getUuid().toString())) {
										if (pc.getGigsOfRamInSlot0() == 0) {
											removeStck(context.player().getInventory(), new ItemStack(lookingFor));
											pc.setGigsOfRamInSlot0(mb);
										} else if (pc.getGigsOfRamInSlot1() == 0) {
											removeStck(context.player().getInventory(), new ItemStack(lookingFor));
											pc.setGigsOfRamInSlot1(mb);
										}
									}
								}
							}
						} else {
							context.player().sendMessage(
									Text.translatable("mcvmcomputers.hdd_not_present").formatted(Formatting.RED));
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_HARD_DRIVE),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				String vhdname = attachedData.readString(32767);
					int entityId = attachedData.readInt();

					context.server().execute(() -> {
						ItemStack lookingFor = ItemHarddrive.createHardDrive(vhdname);
						if (context.player().getInventory().contains(lookingFor)) {
							Entity e = context.player().getWorld().getEntityById(entityId);
							if (e != null) {
								if (e instanceof EntityPC) {
									EntityPC pc = (EntityPC) e;
									if (pc.getOwner().equals(context.player().getUuid().toString())) {
										if (pc.getHardDriveFileName().isEmpty()) {
											removeStck(context.player().getInventory(), lookingFor);
											pc.setHardDriveFileName(vhdname);
										}
									}
								}
							}
						} else {
							context.player().sendMessage(
									Text.translatable("mcvmcomputers.hdd_not_present").formatted(Formatting.RED));
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_MOBO),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(entityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									if (pc.getMotherboardInstalled()) {
										pc.setMotherboardInstalled(false);
										if (pc.get64Bit()) {
											pc.getWorld().spawnEntity(new ItemEntity(pc.getWorld(), pc.getX(), pc.getY(),
													pc.getZ(), new ItemStack(ItemList.ITEM_MOTHERBOARD64)));
										} else {
											pc.getWorld().spawnEntity(new ItemEntity(pc.getWorld(), pc.getX(), pc.getY(),
													pc.getZ(), new ItemStack(ItemList.ITEM_MOTHERBOARD)));
										}
										removeCpu(pc);
										removeGpu(pc);
										removeHdd(pc, context.player().getUuid().toString());
										removeRam(pc, 0);
										removeRam(pc, 1);
									}
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_GPU),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(entityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									removeGpu(pc);
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_HARD_DRIVE),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(entityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									removeHdd(pc, context.player().getUuid().toString());
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_CPU),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(entityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									removeCpu(pc);
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_RAM),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int slot = attachedData.readInt();
					int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(entityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									removeRam(pc, slot);
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_ADD_ISO),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				String isoName = attachedData.readString(32767);
					int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(entityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									if (pc.getIsoFileName().isEmpty()) {
										pc.setIsoFileName(isoName);
									}
								}
							}
						}
					});
				});

		ServerPlayNetworking.registerGlobalReceiver(new net.minecraft.network.packet.CustomPayload.Id<>(C2S_REMOVE_ISO),
			(PacketList.RawBytesPayload payload, ServerPlayNetworking.Context context) -> {
					PacketByteBuf attachedData = payload.data();

				int entityId = attachedData.readInt();

					context.server().execute(() -> {
						Entity e = context.player().getWorld().getEntityById(entityId);
						if (e != null) {
							if (e instanceof EntityPC) {
								EntityPC pc = (EntityPC) e;
								if (pc.getOwner().equals(context.player().getUuid().toString())) {
									if (!pc.getIsoFileName().isEmpty()) {
										pc.setIsoFileName("");
									}
								}
							}
						}
					});
				});
	}

	private static NbtCompound getNbtSafe(ItemStack stack) {
		NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
		return comp != null ? comp.copyNbt() : null;
	}

	private static void removeStck(PlayerInventory inv, ItemStack is) {
		UnmodifiableIterator<DefaultedList<ItemStack>> var2 = ImmutableList.of(inv.main, inv.armor, inv.offHand)
				.iterator();


		if (getNbtSafe(is) != null) {
			while (var2.hasNext()) {
				List<ItemStack> list = (List<ItemStack>) var2.next();
				Iterator<ItemStack> var4 = list.iterator();

				while (var4.hasNext()) {
					ItemStack itemStack = (ItemStack) var4.next();
					if (!itemStack.isEmpty() && itemStack.isOf(is.getItem()) && Objects.equals(getNbtSafe(itemStack), getNbtSafe(is))) {
						itemStack.decrement(1);
						return;
					}
				}
			}

			var2 = ImmutableList.of(inv.main, inv.armor, inv.offHand).iterator();
		}

		while (var2.hasNext()) {
			List<ItemStack> list = (List<ItemStack>) var2.next();
			Iterator<ItemStack> var4 = list.iterator();

			while (var4.hasNext()) {
				ItemStack itemStack = (ItemStack) var4.next();
				if (!itemStack.isEmpty() && itemStack.isOf(is.getItem())) {
					itemStack.decrement(1);
					return;
				}
			}
		}
		LOGGER.error("removeStck: item not found in inventory (desync?)");
		return;
	}
}

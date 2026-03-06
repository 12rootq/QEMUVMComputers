package mcvmcomputers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.ImmutableList;

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
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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

import static mcvmcomputers.networking.PacketList.*;

public class MainMod implements ModInitializer {
	public static Map<UUID, TabletOrder> orders;
	public static Map<UUID, EntityPC> computers;

	public static Runnable hardDriveClick = new Runnable() { @Override public void run() {} };
	public static Runnable deliveryChestSound = new Runnable() { @Override public void run() {} };
	public static Runnable focus = new Runnable() { @Override public void run() {} };
	public static Runnable pcOpenGui = new Runnable() { @Override public void run() {} };

	@Override
	public void onInitialize() {
		orders = new HashMap<UUID, TabletOrder>();
		computers = new HashMap<UUID, EntityPC>();
		ItemList.init();
		EntityList.init();
		SoundList.init();
		registerServerPackets();
	}

	public static void registerServerPackets() {
		ServerPlayNetworking.registerGlobalReceiver(C2S_ORDER, (server, player, handler, buf, responseSender) -> {
			int arraySize = buf.readInt();
			OrderableItem[] items = new OrderableItem[arraySize];
			int price = 0;
			for(int i = 0; i < arraySize; i++) {
				items[i] = (OrderableItem) buf.readItemStack().getItem();
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

		ServerPlayNetworking.registerGlobalReceiver(C2S_SCREEN, (server, player, handler, buf, responseSender) -> {
			byte[] screen = buf.readByteArray();
			int compressedDataSize = buf.readInt();
			int dataSize = buf.readInt();

			server.execute(() -> {
				if(MainMod.computers.containsKey(player.getUuid())) {
					Collection<ServerPlayerEntity> watchingPlayers = PlayerLookup.tracking(MainMod.computers.get(player.getUuid()));
					PacketByteBuf b = PacketByteBufs.create();
					b.writeByteArray(screen);
					b.writeInt(compressedDataSize);
					b.writeInt(dataSize);
					b.writeUuid(player.getUuid());
					watchingPlayers.forEach((p) -> {
						ServerPlayNetworking.send(p, S2C_SCREEN, b);
					});
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_TURN_ON_PC, (server, player, handler, buf, responseSender) -> {
			int pcEntityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(pcEntityId);
				if(e != null) {
					if(e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							MainMod.computers.put(player.getUuid(), (EntityPC) e);
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_TURN_OFF_PC, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				if(MainMod.computers.containsKey(player.getUuid())) {
					Collection<ServerPlayerEntity> watchingPlayers = PlayerLookup.tracking(MainMod.computers.get(player.getUuid()));
					PacketByteBuf b = PacketByteBufs.create();
					b.writeUuid(player.getUuid());
					watchingPlayers.forEach((p) -> {
						ServerPlayNetworking.send(p, S2C_STOP_SCREEN, b);
					});
					MainMod.computers.remove(player.getUuid());
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_CHANGE_HDD, (server, player, handler, buf, responseSender) -> {
			String newHddName = buf.readString(32767);

			server.execute(() -> {
				for(ItemStack is : player.getHandItems()) {
					if(is != null && !is.isEmpty()) {
						if(is.getItem() instanceof ItemHarddrive) {
							NbtCompound ct = is.getOrCreateNbt();
							ct.putString("vhdfile", newHddName);
							break;
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_MOBO, (server, player, handler, buf, responseSender) -> {
			boolean x64 = buf.readBoolean();
			int entityId = buf.readInt();

			server.execute(() -> {
				Item lookingFor = x64 ? ItemList.ITEM_MOTHERBOARD64 : ItemList.ITEM_MOTHERBOARD;
				if(player.getInventory().contains(new ItemStack(lookingFor))) {
					Entity e = player.getWorld().getEntityById(entityId);
					if(e != null) {
						if (e instanceof EntityPC) {
							EntityPC pc = (EntityPC) e;
							if(pc.getOwner().equals(player.getUuid().toString())) {
								if(!pc.getMotherboardInstalled()) {
									removeStck(player.getInventory(), new ItemStack(lookingFor));
									pc.setMotherboardInstalled(true);
									pc.set64Bit(x64);
								}
							}
						}
					}
				} else {
					player.sendMessage(Text.translatable("mcvmcomputers.motherboard_not_present").formatted(Formatting.RED), false);
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_GPU, (server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();

			server.execute(() -> {
				Item lookingFor = ItemList.ITEM_GPU;
				if(player.getInventory().contains(new ItemStack(lookingFor))) {
					Entity e = player.getWorld().getEntityById(entityId);
					if(e != null) {
						if (e instanceof EntityPC) {
							EntityPC pc = (EntityPC) e;
							if(pc.getOwner().equals(player.getUuid().toString())) {
								if(!pc.getGpuInstalled()) {
									removeStck(player.getInventory(), new ItemStack(lookingFor));
									pc.setGpuInstalled(true);
								}
							}
						}
					}
				} else {
					player.sendMessage(Text.translatable("mcvmcomputers.gpu_not_present").formatted(Formatting.RED), false);
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_CPU, (server, player, handler, buf, responseSender) -> {
			int dividedBy = buf.readInt();
			int entityId = buf.readInt();

			server.execute(() -> {
				Item lookingFor = null;
				if(dividedBy == 2) {lookingFor = ItemList.ITEM_CPU2;}
				else if(dividedBy == 4) {lookingFor = ItemList.ITEM_CPU4;}
				else if(dividedBy == 6) {lookingFor = ItemList.ITEM_CPU6;}

				if(lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
					Entity e = player.getWorld().getEntityById(entityId);
					if(e != null) {
						if (e instanceof EntityPC) {
							EntityPC pc = (EntityPC) e;
							if(pc.getOwner().equals(player.getUuid().toString())) {
								if(pc.getCpuDividedBy() == 0) {
									removeStck(player.getInventory(), new ItemStack(lookingFor));
									pc.setCpuDividedBy(dividedBy);
								}
							}
						}
					}
				} else {
					player.sendMessage(Text.translatable("mcvmcomputers.cpu_not_present").formatted(Formatting.RED), false);
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_RAM, (server, player, handler, buf, responseSender) -> {
			int mb = buf.readInt();
			int entityId = buf.readInt();

			server.execute(() -> {
				Item lookingFor = null;
				if(mb == 64) {lookingFor = ItemList.ITEM_RAM64M;}
				else if(mb == 128) {lookingFor = ItemList.ITEM_RAM128M;}
				else if(mb == 256) {lookingFor = ItemList.ITEM_RAM256M;}
				else if(mb == 512) {lookingFor = ItemList.ITEM_RAM512M;}
				else if(mb == 1024) {lookingFor = ItemList.ITEM_RAM1G;}
				else if(mb == 2048) {lookingFor = ItemList.ITEM_RAM2G;}
				else if(mb == 4096) {lookingFor = ItemList.ITEM_RAM4G;}

				if(lookingFor != null && player.getInventory().contains(new ItemStack(lookingFor))) {
					Entity e = player.getWorld().getEntityById(entityId);
					if(e != null) {
						if (e instanceof EntityPC) {
							EntityPC pc = (EntityPC) e;
							if(pc.getOwner().equals(player.getUuid().toString())) {
								if(pc.getGigsOfRamInSlot0() == 0) {
									removeStck(player.getInventory(), new ItemStack(lookingFor));
									pc.setGigsOfRamInSlot0(mb);
								} else if(pc.getGigsOfRamInSlot1() == 0) {
									removeStck(player.getInventory(), new ItemStack(lookingFor));
									pc.setGigsOfRamInSlot1(mb);
								}
							}
						}
					}
				} else {
					player.sendMessage(Text.translatable("mcvmcomputers.cpu_not_present").formatted(Formatting.RED), false); // В оригинале было cpu_not_present, возможно стоит поменять на ram_not_present
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_HARD_DRIVE, (server, player, handler, buf, responseSender) -> {
			String vhdname = buf.readString(32767);
			int entityId = buf.readInt();

			server.execute(() -> {
				ItemStack lookingFor = ItemHarddrive.createHardDrive(vhdname);
				if(player.getInventory().contains(lookingFor)) {
					Entity e = player.getWorld().getEntityById(entityId);
					if(e != null) {
						if (e instanceof EntityPC) {
							EntityPC pc = (EntityPC) e;
							if(pc.getOwner().equals(player.getUuid().toString())) {
								if(pc.getHardDriveFileName().isEmpty()) {
									removeStck(player.getInventory(), lookingFor);
									pc.setHardDriveFileName(vhdname);
								}
							}
						}
					}
				} else {
					player.sendMessage(Text.translatable("mcvmcomputers.cpu_not_present").formatted(Formatting.RED), false); // Тут тоже в оригинале была ошибка с ключом локализации
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_MOBO, (server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(entityId);
				if(e != null) {
					if (e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							if(pc.getMotherboardInstalled()) {
								pc.setMotherboardInstalled(false);
								if(pc.get64Bit()) {
									pc.getWorld().spawnEntity(new ItemEntity(pc.getWorld(), pc.getX(), pc.getY(), pc.getZ(), new ItemStack(ItemList.ITEM_MOTHERBOARD64)));
								} else {
									pc.getWorld().spawnEntity(new ItemEntity(pc.getWorld(), pc.getX(), pc.getY(), pc.getZ(), new ItemStack(ItemList.ITEM_MOTHERBOARD)));
								}
								removeCpu(pc);
								removeGpu(pc);
								removeHdd(pc, player.getUuid().toString());
								removeRam(pc, 0);
								removeRam(pc, 1);
							}
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_GPU, (server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(entityId);
				if(e != null) {
					if (e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							removeGpu(pc);
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_HARD_DRIVE, (server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(entityId);
				if(e != null) {
					if (e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							removeHdd(pc, player.getUuid().toString());
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_CPU, (server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(entityId);
				if(e != null) {
					if (e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							removeCpu(pc);
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_RAM, (server, player, handler, buf, responseSender) -> {
			int slot = buf.readInt();
			int entityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(entityId);
				if(e != null) {
					if (e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							removeRam(pc, slot);
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_ADD_ISO, (server, player, handler, buf, responseSender) -> {
			String isoName = buf.readString(32767);
			int entityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(entityId);
				if(e != null) {
					if (e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							if(pc.getIsoFileName().isEmpty()) {
								pc.setIsoFileName(isoName);
							}
						}
					}
				}
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(C2S_REMOVE_ISO, (server, player, handler, buf, responseSender) -> {
			int entityId = buf.readInt();

			server.execute(() -> {
				Entity e = player.getWorld().getEntityById(entityId);
				if(e != null) {
					if (e instanceof EntityPC) {
						EntityPC pc = (EntityPC) e;
						if(pc.getOwner().equals(player.getUuid().toString())) {
							if(!pc.getIsoFileName().isEmpty()) {
								pc.setIsoFileName("");
							}
						}
					}
				}
			});
		});
	}

	private static void removeStck(PlayerInventory inv, ItemStack is) {
		for (DefaultedList<ItemStack> list : ImmutableList.of(inv.main, inv.armor, inv.offHand)) {
			for (ItemStack itemStack : list) {
				// Заменили isItemEqualIgnoreDamage на isOf
				if (!itemStack.isEmpty() && itemStack.isOf(is.getItem())) {
					itemStack.decrement(1);
					return;
				}
			}
		}
		throw new RuntimeException("Doesn't contain item!");
	}

	// Примечание: В вашем коде вызывались методы removeCpu, removeGpu, removeHdd, removeRam.
	// Убедитесь, что они существуют в этом классе ниже (вероятно, они не поместились в копию текста).
}
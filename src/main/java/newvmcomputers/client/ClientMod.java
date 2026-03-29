package newvmcomputers.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.google.gson.Gson; // <-- Р вЂќР С•Р В±Р В°Р Р†Р В»Р ВµР Р… Р С‘Р СР С—Р С•РЎР‚РЎвЂљ Р Т‘Р В»РЎРЏ РЎвЂЎРЎвЂљР ВµР Р…Р С‘РЎРЏ JSON

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import newvmcomputers.client.entities.model.DeliveryChestModel;
import newvmcomputers.client.entities.model.OrderingTabletModel;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerLocationRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.network.chat.Component;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.IProgress;
import org.virtualbox_6_1.ISession;
import org.virtualbox_6_1.IVirtualBox;
import org.virtualbox_6_1.LockType;
import org.virtualbox_6_1.MachineState;
import org.virtualbox_6_1.VirtualBoxManager;

import io.netty.buffer.Unpooled;
import newvmcomputers.MainMod;
import newvmcomputers.client.entities.render.CRTScreenRender;
import newvmcomputers.client.entities.render.DeliveryChestRender;
import newvmcomputers.client.entities.render.FlatScreenRender;
import newvmcomputers.client.entities.render.WallTVRender;
import newvmcomputers.client.entities.render.ItemPreviewRender;
import newvmcomputers.client.entities.render.KeyboardRender;
import newvmcomputers.client.entities.render.MouseRender;
import newvmcomputers.client.entities.render.PCRender;
import newvmcomputers.client.gui.GuiCreateHarddrive;
import newvmcomputers.client.gui.GuiFocus;
import newvmcomputers.client.gui.GuiPCEditing;
import newvmcomputers.client.gui.setup.GuiSetup;
import newvmcomputers.client.tablet.TabletOS;
import newvmcomputers.client.utils.VMRunnable;
import newvmcomputers.client.utils.VMSettings; // <-- Р вЂќР С•Р В±Р В°Р Р†Р В»Р ВµР Р… Р С‘Р СР С—Р С•РЎР‚РЎвЂљ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р ВµР С”
import newvmcomputers.entities.EntityDeliveryChest;
import newvmcomputers.entities.EntityItemPreview;
import newvmcomputers.entities.EntityList;
import newvmcomputers.entities.EntityPC;
import newvmcomputers.item.OrderableItem;
import newvmcomputers.networking.PacketList;
import newvmcomputers.utils.TabletOrder;
import newvmcomputers.utils.TabletOrder.OrderStatus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

public class ClientMod implements ClientModInitializer {

	public static final ModelLayerLocation DELIVERY_CHEST_LAYER = new ModelLayerLocation(new ResourceLocation("newvmcomputers", "delivery_chest"), "main");
	public static final ModelLayerLocation ORDERING_TABLET_LAYER = new ModelLayerLocation(new ResourceLocation("newvmcomputers", "ordering_tablet"), "main");

	public static final OutputStream discardAllBytes = new OutputStream() { @Override public void write(int b) throws IOException {} };
	public static Map<UUID, ResourceLocation> vmScreenTextures;
	public static Map<UUID, NativeImage> vmScreenTextureNI;
	public static Map<UUID, DynamicTexture> vmScreenTextureNIBT;
	public static EntityItemPreview thePreviewEntity;
	public static volatile boolean vmTurnedOn;
	public static volatile boolean vmTurningOff;
	public static volatile boolean vmTurningOn;
	public static volatile ISession vmSession;

	public static boolean useVmware3D = true;

	public static int maxRam = 8192;
	public static int videoMem = 256;

	public static VirtualBoxManager vbManager;
	public static IVirtualBox vb;

	public static Process vboxWebSrv;
	public static volatile Thread vmUpdateThread;
	public static volatile byte[] vmTextureBytes;
	public static volatile int vmTextureBytesSize;
	public static boolean failedSend;
	public static boolean useVmware = false; // Р СџР С• РЎС“Р СР С•Р В»РЎвЂЎР В°Р Р…Р С‘РЎР‹ false
	public static String vmwareDirectory = "";
	public static String virtualBoxDirectory = "";
	public static double mouseLastX = 0;
	public static double mouseLastY = 0;
	public static double mouseCurX = 0;
	public static double mouseCurY = 0;
	public static int mouseDeltaScroll;
	public static boolean leftMouseButton;
	public static boolean middleMouseButton;
	public static boolean rightMouseButton;
	public static final List<Integer> vmKeyboardScancodes = Collections.synchronizedList(new ArrayList<>());
	public static boolean releaseKeys = false;
	public static File vhdDirectory;
	public static File isoDirectory;
	public static int latestVHDNum = 0;
	public static TabletOS tabletOS;
	public static TabletOrder myOrder;
	public static volatile int vmEntityID = -1;

	public static Thread tabletThread;

	public static float deltaTime;
	public static long lastDeltaTimeTime;
	private static boolean setupScreenCheckPending = true;

	public static int glfwUnfocusKey1;
	public static int glfwUnfocusKey2;
	public static int glfwUnfocusKey3;
	public static int glfwUnfocusKey4;

	private static boolean hasSavedSetup() {
		File setupFile = new File(Minecraft.getInstance().gameDirectory, "vm_computers/setup.json");
		if (!setupFile.exists()) {
			return false;
		}

		try (FileReader fr = new FileReader(setupFile)) {
			VMSettings set = new Gson().fromJson(fr, VMSettings.class);
			return set != null
					&& set.vmComputersDirectory != null
					&& !set.vmComputersDirectory.isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	private static void startVmUpdateThreadIfNeeded() {
		Thread updateThread = vmUpdateThread;
		if (!vmTurnedOn || vmTurningOff || (updateThread != null && updateThread.isAlive())) {
			return;
		}

		Thread newThread = new Thread(new VMRunnable(), useVmware ? "VMware Screen Update" : "VirtualBox Screen Update");
		newThread.setDaemon(true);
		vmUpdateThread = newThread;
		newThread.start();
		System.out.println("VMComputers: Started VM screen update thread.");
	}

	private static void tickVmRuntime() {
		startVmUpdateThreadIfNeeded();
		if (vmTextureBytes != null) {
			generatePCScreen();
		}
	}

	public static synchronized void closeVirtualBoxSession(boolean powerDown) {
		if (useVmware) {
			return;
		}

		ISession activeSession = vmSession;
		vmSession = null;

		if (activeSession != null) {
			try {
				if (powerDown) {
					IProgress progress = activeSession.getConsole().powerDown();
					if (progress != null) {
						progress.waitForCompletion(-1);
					}
				}
			} catch (Exception e) {
				System.err.println("VMComputers: Failed to power down active VirtualBox session: " + e.getMessage());
			}

			try {
				activeSession.unlockMachine();
			} catch (Exception ignored) {
			}
		}

		if (!powerDown || vb == null || vbManager == null) {
			return;
		}

		try {
			IMachine machine = vb.findMachine("VmComputersVm");
			if (machine != null && machine.getState() != MachineState.PoweredOff) {
				ISession tempSession = vbManager.getSessionObject();
				machine.lockMachine(tempSession, LockType.Shared);
				try {
					IProgress progress = tempSession.getConsole().powerDown();
					if (progress != null) {
						progress.waitForCompletion(-1);
					}
				} finally {
					try {
						tempSession.unlockMachine();
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception e) {
			System.err.println("VMComputers: Failed to close VirtualBox machine cleanly: " + e.getMessage());
		}
	}

	private static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		tickVmRuntime();

		if (!setupScreenCheckPending) {
			return;
		}

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null || minecraft.player != null || minecraft.screen == null) {
			return;
		}

		if (minecraft.screen instanceof GuiSetup) {
			return;
		}

		setupScreenCheckPending = false;
		if (hasSavedSetup()) {
			System.out.println("VMComputers: Opening setup screen with saved configuration.");
		} else {
			System.out.println("VMComputers: Opening initial setup screen.");
		}
		minecraft.setScreen(new GuiSetup());
	}

	public static void forceStopVM() {
		System.out.println("[VMComputers] Shutting down virtual machine runtime.");
		try {
			if (useVmware) {
				// Р Р€Р В±Р С‘Р Р†Р В°Р ВµР С РЎРѓР В°Р С Р Т‘Р Р†Р С‘Р В¶Р С•Р С” VMware
				Runtime.getRuntime().exec("taskkill /F /IM vmware-vmx.exe /T");
				// Р Р€Р В±Р С‘Р Р†Р В°Р ВµР С РЎвЂћР С•Р Р…Р С•Р Р†РЎвЂ№Р в„– Р С—Р В»Р ВµР ВµРЎР‚
				Runtime.getRuntime().exec("taskkill /F /IM vmware-kvm.exe /T");
				Runtime.getRuntime().exec("taskkill /F /IM vmplayer.exe /T");
			} else {
				// Р вЂќР В»РЎРЏ VirtualBox
				closeVirtualBoxSession(true);
				Runtime.getRuntime().exec("taskkill /F /IM VirtualBoxVM.exe /T");
				Runtime.getRuntime().exec("taskkill /F /IM VBoxHeadless.exe /T");
			}
		} catch (Exception e) {
			System.err.println("Could not stop virtual machine runtime:");
			e.printStackTrace();
		} finally {
			vmTurnedOn = false;
			vmTurningOn = false;
			vmTurningOff = false;
			vmEntityID = -1;
			vmTextureBytes = null;
			vmTextureBytesSize = 0;
			vmUpdateThread = null;
			leftMouseButton = false;
			middleMouseButton = false;
			rightMouseButton = false;
			mouseDeltaScroll = 0;
			releaseKeys = false;
			synchronized (vmKeyboardScancodes) {
				vmKeyboardScancodes.clear();
			}
		}
	}
	static {
		if(SystemUtils.IS_OS_MAC) {
			glfwUnfocusKey1 = GLFW.GLFW_KEY_LEFT_ALT;
			glfwUnfocusKey2 = GLFW.GLFW_KEY_RIGHT_ALT;
			glfwUnfocusKey3 = GLFW.GLFW_KEY_BACKSPACE;
			glfwUnfocusKey4 = -1;
		}else {
			glfwUnfocusKey1 = GLFW.GLFW_KEY_LEFT_CONTROL;
			glfwUnfocusKey2 = GLFW.GLFW_KEY_RIGHT_CONTROL;
			glfwUnfocusKey3 = GLFW.GLFW_KEY_BACKSPACE;
			glfwUnfocusKey4 = -1;
		}
	}

	public static EntityDeliveryChest currentDeliveryChest;
	public static EntityPC currentPC;

	public static String getKeyName(int key) {
		if (key < 0) {
			return "None";
		}

		switch(key) {
			case org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE: return "Space";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE: return "Escape";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL: return "L Control";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL: return "R Control";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT: return "L Alt";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_ALT: return "R Alt";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT: return "L Shift";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT: return "R Shift";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_TAB: return "Tab";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER: return "Enter";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE: return "Backspace";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_DELETE: return "Delete";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_INSERT: return "Insert";
			case org.lwjgl.glfw.GLFW.GLFW_KEY_CAPS_LOCK: return "Caps Lock";
		}

		String name = GLFW.glfwGetKeyName(key, 0);
		if (name == null || name.isEmpty() || name.contains("key.keyboard.unknown")) {
			return "Key " + key;
		}
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	public static void getVHDNum() throws NumberFormatException, IOException {
		File f = new File(vhdDirectory.getParentFile(), "vhdnum");
		if(f.exists()) {
			latestVHDNum = Integer.parseInt(Files.readAllLines(f.toPath()).get(0));
		}
	}

	public static void increaseVHDNum() throws IOException {
		latestVHDNum++;
		File f = new File(vhdDirectory.getParentFile(), "vhdnum");
		if(f.exists()) {
			f.delete();
		}
		f.createNewFile();
		FileWriter fw = new FileWriter(f);
		fw.append(""+latestVHDNum);
		fw.flush();
		fw.close();
	}

	public static void generatePCScreen() {
		Minecraft mcc = Minecraft.getInstance();
		if(mcc.player == null) {
			return;
		}
		if(vmTextureBytes != null) {
			if(vmScreenTextures.containsKey(mcc.player.getUUID())) {
				Minecraft.getInstance().getTextureManager().release(vmScreenTextures.get(mcc.player.getUUID()));
				vmScreenTextures.remove(mcc.player.getUUID());
			}

			Deflater def = new Deflater();
			def.setInput(vmTextureBytes);
			def.finish();
			byte[] deflated = new byte[vmTextureBytesSize];
			int sz = def.deflate(deflated);
			def.end();

			if(sz > 32766) {
				if(!failedSend){
					mcc.player.displayClientMessage(Component.translatable("newvmcomputers.screen_too_big_mp").withStyle(ChatFormatting.RED), false);
					failedSend = true;
				}
			} else {
				if(failedSend) {
					mcc.player.displayClientMessage(Component.translatable("newvmcomputers.screen_ok_mp").withStyle(ChatFormatting.GREEN), false);
					failedSend = false;
				}

				FriendlyByteBuf p = new FriendlyByteBuf(Unpooled.buffer());
				p.writeByteArray(Arrays.copyOfRange(deflated, 0, sz));
				p.writeInt(sz);
				p.writeInt(vmTextureBytesSize);

				ClientPlayNetworking.send(PacketList.C2S_SCREEN, p);
			}

			NativeImage ni = null;
			try {
				ni = NativeImage.read(new ByteArrayInputStream(vmTextureBytes));
			} catch (IOException e) {
			}
			if(ni != null) {
				if(vmScreenTextureNI.containsKey(mcc.player.getUUID())) {
					vmScreenTextureNI.get(mcc.player.getUUID()).close();
					vmScreenTextureNI.remove(mcc.player.getUUID());
				}
				if(vmScreenTextureNIBT.containsKey(mcc.player.getUUID())) {
					vmScreenTextureNIBT.get(mcc.player.getUUID()).close();
					vmScreenTextureNIBT.remove(mcc.player.getUUID());
				}
				vmScreenTextureNI.put(mcc.player.getUUID(), ni);
				DynamicTexture nibt = new DynamicTexture(ni);
				vmScreenTextureNIBT.put(mcc.player.getUUID(), nibt);
				vmScreenTextures.put(mcc.player.getUUID(), Minecraft.getInstance().getTextureManager().register("vm_texture", nibt));
			}
			vmTextureBytes = null;
		}
	}

	public static void registerClientPackets() {
		ClientPlayNetworking.registerGlobalReceiver(PacketList.S2C_SCREEN, (client, handler, buf, responseSender) -> {
			byte[] screen = buf.readByteArray();
			int compressedDataSize = buf.readInt();
			int dataSize = buf.readInt();
			UUID pcOwner = buf.readUUID();

			client.execute(() -> {
				Minecraft mcc = Minecraft.getInstance();
				if(mcc.player == null) return;

				if(!pcOwner.equals(mcc.player.getUUID())) {
					if(ClientMod.vmScreenTextures.containsKey(pcOwner)) {
						mcc.getTextureManager().release(ClientMod.vmScreenTextures.get(pcOwner));
						vmScreenTextures.remove(pcOwner);
					}
					if(ClientMod.vmScreenTextureNI.containsKey(pcOwner)) {
						ClientMod.vmScreenTextureNI.get(pcOwner).close();
						vmScreenTextureNI.remove(pcOwner);
					}
					if(ClientMod.vmScreenTextureNIBT.containsKey(pcOwner)) {
						ClientMod.vmScreenTextureNIBT.get(pcOwner).close();
						vmScreenTextureNIBT.remove(pcOwner);
					}
					try {
						Inflater inf = new Inflater();
						inf.setInput(screen, 0, compressedDataSize);
						byte[] actualScreen = new byte[dataSize+1];
						int size = inf.inflate(actualScreen);
						inf.end();
						NativeImage ni = NativeImage.read(new ByteArrayInputStream(actualScreen, 0, size));
						DynamicTexture nibt = new DynamicTexture(ni);
						ClientMod.vmScreenTextures.put(pcOwner, mcc.getTextureManager().register("pc_screen_mp", nibt));
						ClientMod.vmScreenTextureNI.put(pcOwner, ni);
						ClientMod.vmScreenTextureNIBT.put(pcOwner, nibt);
					} catch (IOException | DataFormatException e) {
						e.printStackTrace();
					}
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(PacketList.S2C_STOP_SCREEN, (client, handler, buf, responseSender) -> {
			UUID pcOwner = buf.readUUID();

			client.execute(() -> {
				Minecraft mcc = Minecraft.getInstance();
				if(mcc.player == null) return;

				if(ClientMod.vmScreenTextures.containsKey(pcOwner)) {
					mcc.getTextureManager().release(ClientMod.vmScreenTextures.get(pcOwner));
					vmScreenTextures.remove(pcOwner);
				}
				if(ClientMod.vmScreenTextureNI.containsKey(pcOwner)) {
					ClientMod.vmScreenTextureNI.get(pcOwner).close();
					vmScreenTextureNI.remove(pcOwner);
				}
				if(ClientMod.vmScreenTextureNIBT.containsKey(pcOwner)) {
					ClientMod.vmScreenTextureNIBT.get(pcOwner).close();
					vmScreenTextureNIBT.remove(pcOwner);
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(PacketList.S2C_SYNC_ORDER, (client, handler, buf, responseSender) -> {
			int arraySize = buf.readInt();
			OrderableItem[] arr = new OrderableItem[arraySize];
			for(int i = 0; i < arraySize; i++) {
				arr[i] = (OrderableItem) buf.readItem().getItem();
			}
			int price = buf.readInt();
			OrderStatus status = OrderStatus.values()[buf.readInt()];

			client.execute(() -> {
				if(client.player == null) return;

				if(ClientMod.myOrder == null) {
					ClientMod.myOrder = new TabletOrder();
				}
				ClientMod.myOrder.price = price;
				ClientMod.myOrder.items = Arrays.asList(arr);
				ClientMod.myOrder.orderUUID = client.player.getUUID().toString();
				ClientMod.myOrder.currentStatus = status;
			});
		});
	}

	@Override
	public void onInitializeClient() {
		setupScreenCheckPending = true;
		File setupFile = new File(Minecraft.getInstance().gameDirectory, "vm_computers/setup.json");
		if (setupFile.exists()) {
			try (FileReader fr = new FileReader(setupFile)) {
				VMSettings set = new Gson().fromJson(fr, VMSettings.class);
				if (set != null) {
					ClientMod.useVmware = set.useVmware;
					ClientMod.vmwareDirectory = set.vmwareDirectory == null ? "" : set.vmwareDirectory;
					ClientMod.virtualBoxDirectory = set.vboxDirectory == null ? "" : set.vboxDirectory;
					ClientMod.maxRam = set.maxRam;
					ClientMod.videoMem = set.videoMem;
					ClientMod.glfwUnfocusKey1 = set.unfocusKey1;
					ClientMod.glfwUnfocusKey2 = set.unfocusKey2;
					ClientMod.glfwUnfocusKey3 = set.unfocusKey3;
					ClientMod.glfwUnfocusKey4 = set.unfocusKey4;

					if (set.vmComputersDirectory != null && !set.vmComputersDirectory.isEmpty()) {
						ClientMod.isoDirectory = new File(set.vmComputersDirectory, "isos");
						ClientMod.vhdDirectory = new File(set.vmComputersDirectory, "vhds");

						try {
							ClientMod.getVHDNum();
						} catch (Exception e) {
							System.err.println("VMComputers: Failed to read vhdnum: " + e.getMessage());
						}
					}
					System.out.println("VMComputers: Loaded settings. useVmware=" + ClientMod.useVmware);
				}
			} catch (Exception e) {
				System.err.println("VMComputers: Failed to load setup.json on startup: " + e.getMessage());
			}
		} else {
			System.out.println("VMComputers: setup.json not found, using default paths.");
			File defaultDir = new File(Minecraft.getInstance().gameDirectory, "vm_computers");
			ClientMod.isoDirectory = new File(defaultDir, "isos");
			ClientMod.vhdDirectory = new File(defaultDir, "vhds");
		}
		// ==============================================================
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			if (ClientMod.vmTurnedOn || ClientMod.vmUpdateThread != null) {
				System.out.println("[VMComputers] The player has left the world, turning it off...");
				ClientMod.vmTurnedOn = false;
				ClientMod.vmTurningOff = true;
				forceStopVM();
			}
		});

		MainMod.pcOpenGui = () -> Minecraft.getInstance().setScreen(new GuiPCEditing(currentPC));

		MainMod.hardDriveClick = () -> Minecraft.getInstance().setScreen(new GuiCreateHarddrive());

		MainMod.focus = () -> Minecraft.getInstance().setScreen(new GuiFocus());

		MainMod.deliveryChestSound = () -> {
			if(currentDeliveryChest != null && currentDeliveryChest.rocketSound != null) {
				if(Minecraft.getInstance().getSoundManager().isActive(currentDeliveryChest.rocketSound)) {
					Minecraft.getInstance().getSoundManager().stop(currentDeliveryChest.rocketSound);
				}
			}
		};

		registerClientPackets();
		MinecraftForge.EVENT_BUS.addListener(ClientMod::onClientTick);

		vmScreenTextures = new HashMap<>();
		vmScreenTextureNI = new HashMap<>();
		vmScreenTextureNIBT = new HashMap<>();

		ModelLayerLocationRegistry.registerModelLayer(DELIVERY_CHEST_LAYER, DeliveryChestModel::getLayerDefinition);
		ModelLayerLocationRegistry.registerModelLayer(ORDERING_TABLET_LAYER, OrderingTabletModel::getLayerDefinition);

		EntityRendererRegistry.register(EntityList.ITEM_PREVIEW, ItemPreviewRender::new);
		EntityRendererRegistry.register(EntityList.KEYBOARD, KeyboardRender::new);
		EntityRendererRegistry.register(EntityList.MOUSE, MouseRender::new);
		EntityRendererRegistry.register(EntityList.CRT_SCREEN, CRTScreenRender::new);
		EntityRendererRegistry.register(EntityList.FLATSCREEN, FlatScreenRender::new);
		EntityRendererRegistry.register(EntityList.WALLTV, WallTVRender::new);
		EntityRendererRegistry.register(EntityList.PC, PCRender::new);
		EntityRendererRegistry.register(EntityList.DELIVERY_CHEST, DeliveryChestRender::new);
	}
}


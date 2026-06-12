package mcvmcomputers.client;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

import mcvmcomputers.client.utils.VBoxManage;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import mcvmcomputers.MainMod;
import mcvmcomputers.client.entities.render.CRTScreenRender;
import mcvmcomputers.client.entities.render.DeliveryChestRender;
import mcvmcomputers.client.entities.render.FlatScreenRender;
import mcvmcomputers.client.entities.render.WallTVRender;
import mcvmcomputers.client.entities.render.ItemPreviewRender;
import mcvmcomputers.client.entities.render.KeyboardRender;
import mcvmcomputers.client.entities.render.MouseRender;
import mcvmcomputers.client.entities.render.PCRender;
import mcvmcomputers.client.gui.GuiCreateHarddrive;
import mcvmcomputers.client.gui.GuiFocus;
import mcvmcomputers.client.gui.GuiPCEditing;
import mcvmcomputers.client.tablet.TabletOS;
import mcvmcomputers.entities.EntityCRTScreen;
import mcvmcomputers.entities.EntityDeliveryChest;
import mcvmcomputers.entities.EntityFlatScreen;
import mcvmcomputers.entities.EntityItemPreview;
import mcvmcomputers.entities.EntityKeyboard;
import mcvmcomputers.entities.EntityList;
import mcvmcomputers.entities.EntityMouse;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.entities.EntityWallTV;
import mcvmcomputers.item.OrderableItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.TabletOrder;
import mcvmcomputers.utils.TabletOrder.OrderStatus;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

/**
 * Client mod entry point. Registers entity renderers and client-side packet
 * handlers, and stores all client-only VM state: the running VirtualBox handle,
 * the live screen textures streamed from the guest, mouse/keyboard input buffers
 * and the unfocus key bindings.
 */
public class ClientMod implements ClientModInitializer{
	public static final OutputStream discardAllBytes = new OutputStream() { @Override public void write(int b) throws IOException {} };
	public static Map<UUID, Identifier> vmScreenTextures;
	public static Map<UUID, NativeImage> vmScreenTextureNI;
	public static Map<UUID, NativeImageBackedTexture> vmScreenTextureNIBT;
	public static EntityItemPreview thePreviewEntity;
	public static final Object VM_TURNING_ON_LOCK = new Object();
	public static volatile boolean vmTurnedOn;
	public static volatile boolean vmTurningOff;
	public static volatile boolean vmTurningOn;
	public static int maxRam = 8192;
	public static int videoMem = 256;
	public static VBoxManage vbox;

	public static String vboxDirectory = "";
	public static volatile Thread vmUpdateThread;
	public static final Object VM_TEXTURE_LOCK = new Object();
	public static volatile byte[] vmTextureBytes;
	public static volatile int vmTextureBytesSize;
	public static boolean failedSend;

	public static double mouseLastX = 0;
	public static double mouseLastY = 0;
	public static double mouseCurX = 0;
	public static double mouseCurY = 0;
	public static int mouseDeltaScroll;
	// Mouse button bits: left=0x01, right=0x02, middle=0x04 (VBox buttonState convention).
	// mouseButtonMask  = real-time held state.
	// mouseButtonPressedLatch = bits pressed since the VM loop last consumed them, so a
	// fast click (press+release within one ~66ms VM tick) is never lost.
	public static volatile int mouseButtonMask;
	public static volatile int mouseButtonPressedLatch;
	public static List<Integer> vmKeyboardScancodes = new ArrayList<>();
	public static boolean releaseKeys = false;
	public static File vhdDirectory;
	public static File isoDirectory;
	public static int latestVHDNum = 0;
	public static TabletOS tabletOS;
	public static TabletOrder myOrder;
	public static int vmEntityID = -1;

	public static Thread tabletThread;

	public static float deltaTime;
	public static long lastDeltaTimeTime;

	public static int glfwUnfocusKey1;
	public static int glfwUnfocusKey2;
	public static int glfwUnfocusKey3;
	public static int glfwUnfocusKey4;

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
		}else {
			return glfwKey(key);
		}
	}

	private static String glfwKey(int key) {
		switch(key) {
		case GLFW.GLFW_KEY_LEFT_CONTROL:
			return "L Control";
		case GLFW.GLFW_KEY_RIGHT_CONTROL:
			return "R Control";
		case GLFW.GLFW_KEY_RIGHT_ALT:
			return "R Alt";
		case GLFW.GLFW_KEY_LEFT_ALT:
			return "L Alt";
		case GLFW.GLFW_KEY_LEFT_SHIFT:
			return "L Shift";
		case GLFW.GLFW_KEY_RIGHT_SHIFT:
			return "R Shift";
		case GLFW.GLFW_KEY_ENTER:
			return "Enter";
		case GLFW.GLFW_KEY_BACKSPACE:
			return "Backspace";
		case GLFW.GLFW_KEY_CAPS_LOCK:
			return "Caps Lock";
		case GLFW.GLFW_KEY_TAB:
			return "Tab";
		default:
			return GLFW.glfwGetKeyName(key, 0);
		}
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
		try (FileWriter fw = new FileWriter(f)) {
			fw.append(""+latestVHDNum);
			fw.flush();
		}
	}

	public static void generatePCScreen() {
		MinecraftClient mcc = MinecraftClient.getInstance();
		if(mcc.player == null) {
			return;
		}
		byte[] localTextureBytes;
		int localTextureBytesSize;
		synchronized (VM_TEXTURE_LOCK) {
			localTextureBytes = vmTextureBytes;
			localTextureBytesSize = vmTextureBytesSize;
			vmTextureBytes = null;
		}
		if(localTextureBytes != null) {
			if(vmScreenTextures.containsKey(mcc.player.getUuid())) {
				MinecraftClient.getInstance().getTextureManager().destroyTexture(vmScreenTextures.get(mcc.player.getUuid()));
				vmScreenTextures.remove(mcc.player.getUuid());
			}

			Deflater def = new Deflater();
			def.setInput(localTextureBytes);
			def.finish();
			byte[] deflated = new byte[localTextureBytesSize];
			int sz = def.deflate(deflated);
			def.end();

			if(sz > 32766) {
				if(!failedSend){
					mcc.player.sendMessage(Text.translatable("mcvmcomputers.screen_too_big_mp").formatted(Formatting.RED), false);
					failedSend = true;
				}
			}else {
				if(failedSend) {
					mcc.player.sendMessage(Text.translatable("mcvmcomputers.screen_ok_mp").formatted(Formatting.GREEN), false);
					failedSend = false;
				}

				PacketByteBuf p = PacketByteBufs.create();
				p.writeByteArray(Arrays.copyOfRange(deflated, 0, sz));
				p.writeInt(sz);
				p.writeInt(localTextureBytesSize);
				ClientPlayNetworking.send(PacketList.C2S_SCREEN, p);
			}

			NativeImage ni = null;
			try {
				ni = NativeImage.read(new ByteArrayInputStream(localTextureBytes));
			} catch (IOException e) {
			}
			if(ni != null) {
				if(vmScreenTextureNI.containsKey(mcc.player.getUuid())) {
					vmScreenTextureNI.get(mcc.player.getUuid()).close();
					vmScreenTextureNI.remove(mcc.player.getUuid());
				}
				if(vmScreenTextureNIBT.containsKey(mcc.player.getUuid())) {
					vmScreenTextureNIBT.get(mcc.player.getUuid()).close();
					vmScreenTextureNIBT.remove(mcc.player.getUuid());
				}
				vmScreenTextureNI.put(mcc.player.getUuid(), ni);
				NativeImageBackedTexture nibt = new NativeImageBackedTexture(ni);
				vmScreenTextureNIBT.put(mcc.player.getUuid(), nibt);
				vmScreenTextures.put(mcc.player.getUuid(), MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("vm_texture", nibt));
			}
			vmTextureBytes = null;
		}
	}

	public static void registerClientPackets() {
		ClientPlayNetworking.registerGlobalReceiver(PacketList.S2C_SCREEN, (client, handler, attachedData, responseSender) -> {
			byte[] screen = attachedData.readByteArray();
			int compressedDataSize = attachedData.readInt();
			int dataSize = attachedData.readInt();
			UUID pcOwner = attachedData.readUuid();

			client.execute(() -> {
				MinecraftClient mcc = MinecraftClient.getInstance();
				if(!pcOwner.equals(mcc.player.getUuid())) {
					if(ClientMod.vmScreenTextures.containsKey(pcOwner)) {
						mcc.getTextureManager().destroyTexture(ClientMod.vmScreenTextures.get(pcOwner));
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
						NativeImageBackedTexture nibt = new NativeImageBackedTexture(ni);
						ClientMod.vmScreenTextures.put(pcOwner, mcc.getTextureManager().registerDynamicTexture("pc_screen_mp", nibt));
						ClientMod.vmScreenTextureNI.put(pcOwner, ni);
						ClientMod.vmScreenTextureNIBT.put(pcOwner, nibt);
					} catch (IOException | DataFormatException e) {
						e.printStackTrace();
					}
				}
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(PacketList.S2C_STOP_SCREEN, (client, handler, attachedData, responseSender) -> {
			UUID pcOwner = attachedData.readUuid();

			client.execute(() -> {
				MinecraftClient mcc = MinecraftClient.getInstance();
				if(ClientMod.vmScreenTextures.containsKey(pcOwner)) {
					mcc.getTextureManager().destroyTexture(ClientMod.vmScreenTextures.get(pcOwner));
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

		ClientPlayNetworking.registerGlobalReceiver(PacketList.S2C_SYNC_ORDER, (client, handler, attachedData, responseSender) -> {
			int arraySize = attachedData.readInt();
			OrderableItem[] arr = new OrderableItem[arraySize];
			for(int i = 0;i<arraySize;i++) {
				Item readItem = attachedData.readItemStack().getItem();
				if (readItem instanceof OrderableItem) {
					arr[i] = (OrderableItem) readItem;
				} else {
					arr[i] = null;
				}
			}
			int price = attachedData.readInt();
			OrderStatus status = OrderStatus.values()[attachedData.readInt()];

			client.execute(() -> {
				if(ClientMod.myOrder == null) {
					ClientMod.myOrder = new TabletOrder();
				}
				ClientMod.myOrder.price = price;
				ClientMod.myOrder.items = Arrays.asList(arr);
				ClientMod.myOrder.orderUUID = client.player.getUuid().toString();
				ClientMod.myOrder.currentStatus = status;
			});
		});
	}

	@Override
	public void onInitializeClient() {
		MainMod.pcOpenGui = new Runnable() {
			@Override
			public void run() {
				MinecraftClient.getInstance().setScreen(new GuiPCEditing(currentPC));
			}
		};
		MainMod.hardDriveClick = new Runnable() {
			@Override
			public void run() {
				MinecraftClient.getInstance().setScreen(new GuiCreateHarddrive());
			}
		};
		MainMod.focus = new Runnable() {
			@Override
			public void run() {
				MinecraftClient.getInstance().setScreen(new GuiFocus());
			}
		};
		MainMod.deliveryChestSound = new Runnable() {
			@Override
			public void run() {
				if(MinecraftClient.getInstance().getSoundManager().isPlaying(currentDeliveryChest.rocketSound)) {
					MinecraftClient.getInstance().getSoundManager().stop(currentDeliveryChest.rocketSound);
				}
			}
		};

		registerClientPackets();

		vmScreenTextures = new HashMap<UUID, Identifier>();
		vmScreenTextureNI = new HashMap<UUID, NativeImage>();
		vmScreenTextureNIBT = new HashMap<UUID, NativeImageBackedTexture>();

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

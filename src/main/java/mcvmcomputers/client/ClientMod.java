package mcvmcomputers.client;
import net.minecraft.world.entity.player.Player;


import mcvmcomputers.MainMod;
import mcvmcomputers.client.entities.render.*;
import mcvmcomputers.client.gui.*;
import mcvmcomputers.client.tablet.TabletOS;
import mcvmcomputers.client.utils.VBoxManage;
import mcvmcomputers.entities.*;
import mcvmcomputers.item.ItemOrderingTablet;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.TabletOrder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.Deflater;

/**
 * Central holder of client-side mod state and renderer registration.
 *
 * <p>Subscribed to the mod bus ({@link EventBusSubscriber}); in
 * {@link #registerRenderers} it registers the renderers for all entities and wires
 * up the client implementations of the {@link MainMod} callbacks (opening GUIs,
 * sounds). It also holds all shared client-side VM state: the on/off flags
 * ({@code vmTurnedOn}/{@code vmTurningOn}/{@code vmTurningOff}), the
 * {@link VBoxManage} instance, the keyboard scancode queue
 * ({@link #vmKeyboardScancodes}), the mouse state, the VM screen textures and the
 * unfocus key bindings.</p>
 *
 * <p>Many fields are {@code volatile}/synchronized because they are accessed by both
 * the render thread and the background {@link VMRunnable}.</p>
 */
@EventBusSubscriber(modid = "mcvmcomputers", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
/**
 * Client-side helper. Registers entity renderers and client packet handlers via
 * NeoForge events, and stores all client-only VM state: the running VirtualBox
 * handle, the live screen textures streamed from the guest, mouse/keyboard input
 * buffers and the unfocus key bindings.
 */
public class ClientMod {
    public static final OutputStream discardAllBytes = new OutputStream() {
        @Override public void write(int b) throws IOException {}
    };
    public static Map<UUID, ResourceLocation> vmScreenTextures;
    public static Map<UUID, com.mojang.blaze3d.platform.NativeImage> vmScreenTextureNI;
    public static Map<UUID, DynamicTexture> vmScreenTextureNIBT;
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

    public static volatile double mouseLastX = 0;
    public static volatile double mouseLastY = 0;
    public static volatile double mouseCurX = 0;
    public static volatile double mouseCurY = 0;
    public static int mouseDeltaScroll;
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
        if (SystemUtils.IS_OS_MAC) {
            glfwUnfocusKey1 = GLFW.GLFW_KEY_LEFT_ALT;
            glfwUnfocusKey2 = GLFW.GLFW_KEY_RIGHT_ALT;
            glfwUnfocusKey3 = GLFW.GLFW_KEY_BACKSPACE;
            glfwUnfocusKey4 = -1;
        } else {
            glfwUnfocusKey1 = GLFW.GLFW_KEY_LEFT_CONTROL;
            glfwUnfocusKey2 = GLFW.GLFW_KEY_RIGHT_CONTROL;
            glfwUnfocusKey3 = GLFW.GLFW_KEY_BACKSPACE;
            glfwUnfocusKey4 = -1;
        }
    }

    public static EntityDeliveryChest currentDeliveryChest;
    public static EntityPC currentPC;

    public static void playPlaceSound(Level level, Player user, SoundEvent sound, double reach) {
        Vec3 soundPos = thePreviewEntity != null
            ? thePreviewEntity.position()
            : user.pick(reach, 0f, false).getLocation();
        level.playSound(null, soundPos.x, soundPos.y, soundPos.z, sound, SoundSource.BLOCKS, 1f, 1f);
    }

    public static String getKeyName(int key) {
        if (key < 0) return "None";
        return glfwKey(key);
    }

    private static String glfwKey(int key) {
        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L Control";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R Control";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R Alt";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L Alt";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L Shift";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R Shift";
            case GLFW.GLFW_KEY_ENTER -> "Enter";
            case GLFW.GLFW_KEY_BACKSPACE -> "Backspace";
            case GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock";
            case GLFW.GLFW_KEY_TAB -> "Tab";
            default -> GLFW.glfwGetKeyName(key, 0);
        };
    }

    public static void getVHDNum() throws NumberFormatException, IOException {
        File f = new File(vhdDirectory.getParentFile(), "vhdnum");
        if (f.exists()) {
            latestVHDNum = Integer.parseInt(Files.readAllLines(f.toPath()).get(0));
        }
    }

    public static void increaseVHDNum() throws IOException {
        latestVHDNum++;
        File f = new File(vhdDirectory.getParentFile(), "vhdnum");
        if (f.exists()) f.delete();
        f.createNewFile();
        try (FileWriter fw = new FileWriter(f)) {
            fw.append("" + latestVHDNum);
            fw.flush();
        }
    }

    public static void generatePCScreen() {
        Minecraft mcc = Minecraft.getInstance();
        if (mcc.player == null) return;
        byte[] localTextureBytes;
        int localTextureBytesSize;
        synchronized (VM_TEXTURE_LOCK) {
            localTextureBytes = vmTextureBytes;
            localTextureBytesSize = vmTextureBytesSize;
            vmTextureBytes = null;
        }
        if (localTextureBytes != null) {
            if (vmScreenTextures.containsKey(mcc.player.getUUID())) {
                mcc.getTextureManager().release(vmScreenTextures.get(mcc.player.getUUID()));
                vmScreenTextures.remove(mcc.player.getUUID());
            }

            Deflater def = new Deflater();
            def.setInput(localTextureBytes);
            def.finish();
            byte[] deflated = new byte[localTextureBytesSize];
            int sz = def.deflate(deflated);
            def.end();

            if (sz > 32766) {
                if (!failedSend) {
                    mcc.player.displayClientMessage(
                        Component.translatable("mcvmcomputers.screen_too_big_mp").withStyle(ChatFormatting.RED), false);
                    failedSend = true;
                }
            } else {
                if (failedSend) {
                    mcc.player.displayClientMessage(
                        Component.translatable("mcvmcomputers.screen_ok_mp").withStyle(ChatFormatting.GREEN), false);
                    failedSend = false;
                }

                FriendlyByteBuf p = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                p.writeByteArray(Arrays.copyOfRange(deflated, 0, sz));
                p.writeInt(sz);
                p.writeInt(localTextureBytesSize);
                PacketList.sendToServer("c2s_screen", p);
            }

            com.mojang.blaze3d.platform.NativeImage ni = null;
            try {
                ni = com.mojang.blaze3d.platform.NativeImage.read(new ByteArrayInputStream(localTextureBytes));
            } catch (IOException e) {}
            if (ni != null) {
                if (vmScreenTextureNI.containsKey(mcc.player.getUUID())) {
                    vmScreenTextureNI.get(mcc.player.getUUID()).close();
                    vmScreenTextureNI.remove(mcc.player.getUUID());
                }
                if (vmScreenTextureNIBT.containsKey(mcc.player.getUUID())) {
                    vmScreenTextureNIBT.get(mcc.player.getUUID()).close();
                    vmScreenTextureNIBT.remove(mcc.player.getUUID());
                }
                vmScreenTextureNI.put(mcc.player.getUUID(), ni);
                DynamicTexture nibt = new DynamicTexture(ni);
                vmScreenTextureNIBT.put(mcc.player.getUUID(), nibt);
                vmScreenTextures.put(mcc.player.getUUID(),
                    mcc.getTextureManager().register("vm_texture", nibt));
            }
            vmTextureBytes = null;
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        MainMod.pcOpenGui = () -> Minecraft.getInstance().setScreen(new GuiPCEditing(currentPC));
        MainMod.hardDriveClick = () -> Minecraft.getInstance().setScreen(new GuiCreateHarddrive());
        MainMod.focus = () -> Minecraft.getInstance().setScreen(new GuiFocus());
        MainMod.deliveryChestSound = () -> {
            if (currentDeliveryChest.rocketSound != null && Minecraft.getInstance().getSoundManager().isActive(currentDeliveryChest.rocketSound)) {
                Minecraft.getInstance().getSoundManager().stop(currentDeliveryChest.rocketSound);
            }
        };

        vmScreenTextures = new HashMap<>();
        vmScreenTextureNI = new HashMap<>();
        vmScreenTextureNIBT = new HashMap<>();

        event.registerEntityRenderer(EntityList.ITEM_PREVIEW, ItemPreviewRender::new);
        event.registerEntityRenderer(EntityList.KEYBOARD, KeyboardRender::new);
        event.registerEntityRenderer(EntityList.MOUSE, MouseRender::new);
        event.registerEntityRenderer(EntityList.CRT_SCREEN, CRTScreenRender::new);
        event.registerEntityRenderer(EntityList.FLATSCREEN, FlatScreenRender::new);
        event.registerEntityRenderer(EntityList.WALLTV, WallTVRender::new);
        event.registerEntityRenderer(EntityList.PC, PCRender::new);
        event.registerEntityRenderer(EntityList.DELIVERY_CHEST, DeliveryChestRender::new);
    }
}

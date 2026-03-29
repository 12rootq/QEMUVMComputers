package newvmcomputers.client.gui;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.SystemUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.virtualbox_6_1.AccessMode;
import org.virtualbox_6_1.DeviceType;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.IMedium;
import org.virtualbox_6_1.IProgress;
import org.virtualbox_6_1.ISession;
import org.virtualbox_6_1.LockType;
import org.virtualbox_6_1.MachineState;
import org.virtualbox_6_1.StorageBus;
import org.virtualbox_6_1.VBoxException;

import com.mojang.blaze3d.systems.RenderSystem;

import io.netty.buffer.Unpooled;
import newvmcomputers.client.ClientMod;
import newvmcomputers.entities.EntityPC;
import newvmcomputers.item.ItemHarddrive;
import newvmcomputers.item.ItemList;
import newvmcomputers.networking.PacketList;
import newvmcomputers.utils.MVCUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import com.mojang.math.Axis;

public class GuiPCEditing extends Screen {
	private float introScale;
	private float panelX;
	private final EntityPC pc_case;
	private boolean openCase;
	private final Minecraft minecraft;

	private final Language lang = Language.getInstance();

	private static final ItemStack CASE_NO_PANEL = new ItemStack(ItemList.PC_CASE_NO_PANEL.get());
	private static final ItemStack CASE_ONLY_PANEL = new ItemStack(ItemList.PC_CASE_ONLY_PANEL.get());
	private static final ItemStack CASE_ONLY_GLASS_PANEL = new ItemStack(ItemList.PC_CASE_GLASS_PANEL.get());
	private static final ItemStack MOBO = new ItemStack(ItemList.ITEM_MOTHERBOARD.get());
	private static final ItemStack CPU = new ItemStack(ItemList.ITEM_CPU2.get());
	private static final ItemStack GPU = new ItemStack(ItemList.ITEM_GPU.get());
	private static final ItemStack RAM = new ItemStack(ItemList.ITEM_RAM1G.get());
	private static final ItemStack HARD_DRIVE = new ItemStack(ItemList.ITEM_HARDDRIVE.get());
	private final Object vmTurningON = new Object();

	public GuiPCEditing(EntityPC pc_case) {
		super(Component.translatable("text.pc_editor.title"));
		this.pc_case = pc_case;
		this.minecraft = Minecraft.getInstance();
	}

	private String readProcessOutput(Process process) throws IOException {
		return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
	}

	private boolean waitForLocalPort(String host, int port, long timeoutMs) {
		long deadline = System.currentTimeMillis() + timeoutMs;
		while (System.currentTimeMillis() < deadline) {
			try (Socket socket = new Socket()) {
				socket.connect(new InetSocketAddress(host, port), 500);
				return true;
			} catch (IOException ignored) {
				try {
					Thread.sleep(250L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
			}
		}
		return false;
	}

	private File resolveVmwareDiskFile() throws Exception {
		if (pc_case.getHardDriveFileName().isEmpty()) {
			return null;
		}

		File sourceDisk = new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName());
		String sourceName = sourceDisk.getName().toLowerCase(Locale.ROOT);
		if (!sourceName.endsWith(".vdi")) {
			return sourceDisk;
		}

		String baseName = sourceDisk.getName().substring(0, sourceDisk.getName().length() - 4);
		File convertedDisk = new File(sourceDisk.getParentFile(), baseName + "-vmware.vmdk");
		if (convertedDisk.exists() && convertedDisk.lastModified() >= sourceDisk.lastModified()) {
			return convertedDisk;
		}

		if (minecraft.player != null) {
			minecraft.player.displayClientMessage(Component.literal("Converting VDI disk for VMware...").withStyle(ChatFormatting.YELLOW), false);
		}

		String vboxManageExecutable = SystemUtils.IS_OS_WINDOWS ? "VBoxManage.exe" : "VBoxManage";
		String vboxManagePath = ClientMod.virtualBoxDirectory == null || ClientMod.virtualBoxDirectory.isBlank()
			? vboxManageExecutable
			: ClientMod.virtualBoxDirectory + File.separator + vboxManageExecutable;
		ProcessBuilder conversionBuilder = new ProcessBuilder(
			vboxManagePath,
			"clonemedium",
			"disk",
			sourceDisk.getAbsolutePath(),
			convertedDisk.getAbsolutePath(),
			"--format",
			"VMDK"
		);
		conversionBuilder.redirectErrorStream(true);
		Process conversionProcess = conversionBuilder.start();
		boolean finished = conversionProcess.waitFor(60, TimeUnit.SECONDS);
		if (!finished) {
			conversionProcess.destroyForcibly();
			throw new IOException("VBoxManage timed out while converting the VMware disk.");
		}

		String output = readProcessOutput(conversionProcess);
		if (conversionProcess.exitValue() != 0) {
			throw new IOException("VBoxManage exited with code " + conversionProcess.exitValue() + (output.isEmpty() ? "" : ": " + output));
		}
		if (!convertedDisk.exists()) {
			throw new IOException("Converted VMware disk was not created.");
		}

		return convertedDisk;
	}

	public void renderBackgroundAndMobo(GuiGraphics context) {
		context.fillGradient(0, 0, this.width, this.height, new Color(0f,0f,0f,Math.max(0.5f*introScale,0)).getRGB(), new Color(0f,0f,0f,0.5f*introScale).getRGB());

		PoseStack ms = context.pose();
		ms.pushPose();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		ms.translate((this.width / 2f), (this.height / 2f)-40, 100.0F);
		ms.mulPose(Axis.YN.rotationDegrees(90f*introScale));
		ms.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(6f * introScale), 0f, 0.70710678f, 0.70710678f));
		ms.scale(1.0F, -1.0F, 1.0F);
		ms.scale(introScale, introScale, introScale);
		ms.scale(230.0F, 230.0F, 230.0F);

		renderItem(context, CASE_NO_PANEL);

		if(pc_case.getMotherboardInstalled()) {
			ms.pushPose();
			ms.mulPose(Axis.ZN.rotationDegrees(90f));
			ms.mulPose(Axis.YN.rotationDegrees(90f));
			ms.translate(0.04f, 0.2f, -0.16f);
			ms.scale(0.55f, 0.55f, 0.55f);
			renderItem(context, MOBO);
			ms.popPose();
		}
		if(pc_case.getCpuDividedBy() > 0) {
			ms.pushPose();
			ms.mulPose(Axis.ZN.rotationDegrees(90f));
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.translate(0.23f, 0.48f, 0.13f);
			renderItem(context, CPU);
			ms.popPose();
		}
		if(pc_case.getGpuInstalled()) {
			ms.pushPose();
			ms.mulPose(Axis.YN.rotationDegrees(90f));
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.mulPose(Axis.XP.rotationDegrees(-90f));
			ms.translate(0.33f, 0.5f, -0.565f);
			renderItem(context, GPU);
			ms.popPose();
		}
		if(pc_case.getGigsOfRamInSlot0() > 0) {
			ms.pushPose();
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.mulPose(Axis.ZN.rotationDegrees(90f));
			ms.translate(0.07f, 0.5f, -0.203f);
			renderItem(context, RAM);
			ms.popPose();
		}
		if(pc_case.getGigsOfRamInSlot1() > 0) {
			ms.pushPose();
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.mulPose(Axis.ZN.rotationDegrees(90f));
			ms.translate(0.07f, 0.5f, -0.33f);
			renderItem(context, RAM);
			ms.popPose();
		}
		if(!pc_case.getHardDriveFileName().isEmpty()) {
			ms.pushPose();
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.translate(0.1f, -0.3f, -0.5f);
			renderItem(context, HARD_DRIVE);
			ms.popPose();
		}
		ms.pushPose();
		ms.translate(0, -panelX, 0);
		if(pc_case.getGlassSidepanel()) {
			renderItem(context, CASE_ONLY_GLASS_PANEL);
		}else{
			renderItem(context, CASE_ONLY_PANEL);
		}
		ms.popPose();
		ms.popPose();
	}

	private void renderItem(GuiGraphics context, ItemStack stack) {
		BakedModel model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
		MultiBufferSource.BufferSource immediate = minecraft.renderBuffers().bufferSource();
		boolean isNotSideLit = !model.usesBlockLight();
		if (isNotSideLit) {
			Lighting.setupForFlatItems();
		}

		this.minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, context.pose(), immediate, 15728640, OverlayTexture.NO_OVERLAY, model);
		immediate.endBatch();

		if (isNotSideLit) {
			Lighting.setupFor3DItems();
		}
	}

	private void addMotherboard(boolean sixtyFour) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeBoolean(sixtyFour);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_MOBO, b);
	}

	private void removeMotherboard() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_MOBO, b);
	}

	private void addCPU(int dividedBy) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(dividedBy);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_CPU, b);
	}

	private void addGPU() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_GPU, b);
	}

	private void addHardDrive(String fileName) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeUtf(fileName);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_HARD_DRIVE, b);
	}

	private void removeHardDrive() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_HARD_DRIVE, b);
	}

	private void addRamStick(int megs) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(megs);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_RAM, b);
	}

	private void removeRamStick(int slot) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(slot);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_RAM, b);
	}

	private void removeCPU() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_CPU, b);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		if(introScale > 0.92f && openCase)
			panelX = MVCUtils.lerp(panelX, 8, delta/20f);
		if(!openCase) {
			panelX = MVCUtils.lerp(panelX, 0, delta/1.5f);
		}
		introScale = MVCUtils.lerp(introScale, 1f, delta/4f);
		this.renderBackgroundAndMobo(context);
		this.clearWidgets();

		PoseStack ms = context.pose();

		if(introScale > 0.99f) {
			if(openCase) {
				if((ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
					openCase = false;
				}
				if(SystemUtils.IS_OS_MAC) {
					context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.put_panel_back_mac"), 4, 14, -1, false);
					if(minecraft.getWindow() != null && GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}else {
					context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.put_panel_back"), 4, 14, -1, false);
					if(minecraft.getWindow() != null && GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}
				if(pc_case.getMotherboardInstalled()) {
					this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeMotherboard())
							.bounds(this.width/2-70, this.height / 2 - 70, 10, 10).build());

					ms.pushPose();
					ms.translate(0, 0, 200);
					if(pc_case.get64Bit()) {
						context.drawString(this.font, lang.getOrDefault("newvmcomputers.64bit"), this.width/2 - 66, this.height/2 - 56, -1, false);
					}else {
						context.drawString(this.font, lang.getOrDefault("newvmcomputers.32bit"), this.width/2 - 66, this.height/2 - 56, -1, false);
					}
					ms.popPose();

					if(pc_case.getCpuDividedBy() == 0) {
						ms.pushPose();
						ms.translate(0, 0, 200);
						context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.add_cpu"), this.width/2 - 120, this.height/2 - 40, -1, false);
						ms.popPose();

						int addCpuWidth = font.width(lang.getOrDefault("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6"));
						Button div2 = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "2")), (btn) -> this.addCPU(2))
								.bounds(this.width/2 - (addCpuWidth+59), this.height / 2 - 31, addCpuWidth+4, 12).build();
						Button div4 = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "4")), (btn) -> this.addCPU(4))
								.bounds(this.width/2 - (addCpuWidth+59), this.height / 2 - 18, addCpuWidth+4, 12).build();
						Button div6 = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6")), (btn) -> this.addCPU(6))
								.bounds(this.width/2 - (addCpuWidth+59), this.height / 2 - 5, addCpuWidth+4, 12).build();

						if(minecraft.player != null) {
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU2.get()))) div2.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU4.get()))) div4.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU6.get()))) div6.active = false;
						}

						this.addRenderableWidget(div2);
						this.addRenderableWidget(div4);
						this.addRenderableWidget(div6);
					}else {
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeCPU())
								.bounds(this.width/2-43, this.height / 2 - 16, 10, 10).build());
						ms.pushPose();
						ms.translate(0, 0, 200);
						context.drawString(this.font, "1/" + pc_case.getCpuDividedBy(), this.width/2-25, this.height/2+2, -1, false);
						ms.popPose();
					}
					if(!pc_case.getGpuInstalled()) {
						int addGpuWidth = this.font.width(lang.getOrDefault("newvmcomputers.pc_editing.add_gpu"));
						Button bw = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_gpu")), (btn) -> this.addGPU())
								.bounds(this.width/2 - 64, this.height / 2 + 33, addGpuWidth+4, 12).build();
						if(minecraft.player != null && !minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_GPU.get())))
							bw.active = false;
						this.addRenderableWidget(bw);
					}
					if(pc_case.getHardDriveFileName().isEmpty()) {
						int lastYOffset = 0;
						int xOffCount = 0;
						int lastXOffset = 0;
						int count = 0;
						ms.pushPose();
						ms.translate(0, 0, 200);
						context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.add_vhds"), this.width/2 + 20, this.height/2 + 30, -1, false);
						ms.popPose();

						if (minecraft.player != null) {
							for(ItemStack is : minecraft.player.getInventory().items) {
								if(is.getItem() instanceof ItemHarddrive) {
									if(is.hasTag() && is.getTag().contains("vhdfile")) {
										String file = is.getTag().getString("vhdfile");
										if(new File(ClientMod.vhdDirectory, file).exists()) {
											int w = Math.max(50, this.font.width(file)+4);
											this.addRenderableWidget(Button.builder(Component.literal(file), (btn) -> this.addHardDrive(file))
													.bounds(this.width/2 + 20 + lastXOffset, this.height / 2 + 40 + lastYOffset, w, 12).build());
											lastXOffset += w+1;
											xOffCount += 1;
											if(xOffCount >= 3) {
												xOffCount = 0;
												lastXOffset = 0;
												lastYOffset += 13;
											}
											count++;
										}
									}
								}
							}
						}
						if(count == 0) {
							ms.pushPose();
							ms.translate(0, 0, 200);
							context.drawString(this.font, (char) (0xfeff00a7) + "7" + lang.getOrDefault("newvmcomputers.pc_editing.no_valid_vhd"), this.width/2 + 20, this.height/2 + 40, -1, false);
							ms.popPose();
						}
					}else {
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeHardDrive())
								.bounds(this.width/2+30, this.height / 2 + 55, 10, 10).build());
						ms.pushPose();
						ms.translate(0, 0, 200);
						context.drawString(this.font, pc_case.getHardDriveFileName(), this.width/2+45, this.height/2+65, -1, false);
						ms.popPose();
					}
					if(pc_case.getGigsOfRamInSlot0() == 0 || pc_case.getGigsOfRamInSlot1() == 0) {
						ms.pushPose();
						ms.translate(0, 0, 200);
						context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.add_ram"), this.width/2 + 50, this.height/2 - 60, -1, false);
						ms.popPose();

						int addMBRamWidth = font.width(lang.getOrDefault("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512"))+4;
						Button sixfourM = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "64")), (btn) -> this.addRamStick(64))
								.bounds(this.width/2 + 50, this.height / 2 - 64, addMBRamWidth, 12).build();
						Button oneM = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "128")), (btn) -> this.addRamStick(128))
								.bounds(this.width/2 + 50, this.height / 2 - 51, addMBRamWidth, 12).build();
						Button twoM = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "256")), (btn) -> this.addRamStick(256))
								.bounds(this.width/2 + 50, this.height / 2 - 38, addMBRamWidth, 12).build();
						Button fiveM = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512")), (btn) -> this.addRamStick(512))
								.bounds(this.width/2 + 50, this.height / 2 - 25, addMBRamWidth, 12).build();
						Button oneG = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_ram_btn").replace("%s", "1")), (btn) -> this.addRamStick(1024))
								.bounds(this.width/2 + 50, this.height / 2 - 12, addMBRamWidth, 12).build();
						Button twoG = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_ram_btn").replace("%s", "2")), (btn) -> this.addRamStick(2048))
								.bounds(this.width/2 + 50, this.height / 2 + 1, addMBRamWidth, 12).build();
						Button fourG = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_ram_btn").replace("%s", "4")), (btn) -> this.addRamStick(4096))
								.bounds(this.width/2 + 50, this.height / 2 + 14, addMBRamWidth, 12).build();

						if(minecraft.player != null) {
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM64M.get()))) sixfourM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM128M.get()))) oneM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM256M.get()))) twoM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM512M.get()))) fiveM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM1G.get()))) oneG.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM2G.get()))) twoG.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM4G.get()))) fourG.active = false;
						}
						this.addRenderableWidget(sixfourM);
						this.addRenderableWidget(oneM);
						this.addRenderableWidget(twoM);
						this.addRenderableWidget(fiveM);
						this.addRenderableWidget(oneG);
						this.addRenderableWidget(twoG);
						this.addRenderableWidget(fourG);
					}
					if(pc_case.getGigsOfRamInSlot0() > 0) {
						ms.pushPose();
						ms.translate(0, 0, 200);
						if(pc_case.getGigsOfRamInSlot0() < 1000 && pc_case.getGigsOfRamInSlot0() >= 100) {
							context.drawString(this.font, pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+4, this.height/2+2, -1, false);
						}else if(pc_case.getGigsOfRamInSlot0() < 100) {
							context.drawString(this.font, pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+10, this.height/2+2, -1, false);
						}else {
							context.drawString(this.font, (pc_case.getGigsOfRamInSlot0()/1024) + " GB", this.width / 2 + 16, this.height / 2 + 2, -1, false);
						}
						ms.popPose();
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeRamStick(0))
								.bounds(this.width/2+21, this.height / 2 - 70, 10, 10).build());
					}
					if(pc_case.getGigsOfRamInSlot1() > 0) {
						ms.pushPose();
						ms.translate(0, 0, 200);
						if(pc_case.getGigsOfRamInSlot1() < 1000) {
							context.drawString(this.font, pc_case.getGigsOfRamInSlot1() + " MB", this.width/2+42, this.height/2+2, -1, false);
						}else {
							context.drawString(this.font, (pc_case.getGigsOfRamInSlot1()/1024) + " GB", this.width / 2+42, this.height / 2+2, -1, false);
						}
						ms.popPose();
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeRamStick(1))
								.bounds(this.width/2 + 37, this.height / 2 - 70, 10, 10).build());
					}
				}else {
					int thirtyTwoWidth = font.width(lang.getOrDefault("newvmcomputers.pc_editing.add_32bit_mobo"))+4;
					Button thirtytwo = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_32bit_mobo")), (btn) -> this.addMotherboard(false))
							.bounds(this.width/2 - (thirtyTwoWidth/2), this.height / 2 - 23, thirtyTwoWidth, 14).build();
					int sixtyfourw = font.width(lang.getOrDefault("newvmcomputers.pc_editing.add_64bit_mobo"))+4;
					Button sixtyfour = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.add_64bit_mobo")), (btn) -> this.addMotherboard(true))
							.bounds(this.width/2 - (sixtyfourw/2), this.height / 2 - 7, sixtyfourw, 14).build();

					if(minecraft.player != null) {
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD.get()))) thirtytwo.active = false;
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD64.get()))) sixtyfour.active = false;
					}

					this.addRenderableWidget(thirtytwo);
					this.addRenderableWidget(sixtyfour);
				}
			}else {
				boolean turnedOn = (ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) || (ClientMod.vmTurnedOn && ClientMod.vmEntityID == pc_case.getId());
				if(turnedOn) {
					int buttonW = font.width(lang.getOrDefault("newvmcomputers.pc_editing.turn_off"))+4;
					this.addRenderableWidget(Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.turn_off")), this::turnOffPC)
							.bounds((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}else {
					int buttonW = font.width(lang.getOrDefault("newvmcomputers.pc_editing.turn_on"))+4;
					this.addRenderableWidget(Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.turn_on")), this::turnOnPC)
							.bounds((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}
				if (ClientMod.useVmware) {
					String text3D = ClientMod.useVmware3D ? "3D Acceleration: ON" : "3D Acceleration: OFF";
					int btn3DW = font.width(text3D) + 8;
					this.addRenderableWidget(Button.builder(Component.literal(text3D), (btn) -> {
						ClientMod.useVmware3D = !ClientMod.useVmware3D;
					}).bounds((this.width/2 + 103) - btn3DW, this.height / 2 - 95, btn3DW, 12).build());
				}
				if(!ClientMod.useVmware && ClientMod.vmSession != null) {
					boolean ejected = false;
					try {
						ejected = ClientMod.vmSession.getMachine().getMediumAttachment("IDE Controller", 1, 0).getIsEjected();
					}catch(VBoxException e) { /* Ignored */ }

					if(ejected && !pc_case.getIsoFileName().isEmpty()) {
						this.removeISO();
					}
				}

				if(pc_case.getIsoFileName().isEmpty()) {
					ms.pushPose();
					ms.translate(0, 0, 200);
					context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.select_iso"), this.width/2 - 75, this.height/2 - 75, -1, false);
					ms.popPose();

					int offX = 0;
					int offY = 0;
					File[] isoFiles = ClientMod.isoDirectory.listFiles();
					if (isoFiles != null) {
						for(File f : isoFiles) {
							if(f.getName().endsWith(".iso") || f.getName().endsWith(".ISO")) {
								if((this.width/2 - 75 + offX) + this.font.width(f.getName())+10 > this.width/2 + 105) {
									offX = 0;
									offY += 14;
								}
								this.addRenderableWidget(Button.builder(Component.literal(f.getName()), (btn) -> insertISO(f.getName()))
										.bounds(this.width/2 - 75 + offX, this.height / 2 - 62 + offY, this.font.width(f.getName())+8, 12).build());
								offX += this.font.width(f.getName())+10;
							}
						}
					}
				}else {
					ms.pushPose();
					ms.translate(0, 0, 200);
					context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.inserted_iso"), this.width/2 - 75, this.height/2 - 75, -1, false);
					context.drawString(this.font, (char) (0xfeff00a7) + "7" + pc_case.getIsoFileName(), this.width/2 - 75, this.height/2 - 65, -1, false);
					ms.popPose();
					int ejectW = font.width(lang.getOrDefault("newvmcomputers.pc_editing.eject"));
					this.addRenderableWidget(Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.eject")), (btn) -> removeISO())
							.bounds(this.width/2 - 75, this.height / 2 - 50, ejectW+4, 12).build());
				}
				int openCaseW = font.width(lang.getOrDefault("newvmcomputers.pc_editing.open_case"));
				Button bw = Button.builder(Component.literal(lang.getOrDefault("newvmcomputers.pc_editing.open_case")), (btn) -> openCase = true)
						.bounds(this.width/2 - 82, this.height / 2 + 65, openCaseW+4, 12).build();
				bw.active = !turnedOn;
				this.addRenderableWidget(bw);
			}
		}

		ms.pushPose();
		ms.translate(0, 0, 200);
		super.render(context, mouseX, mouseY, delta);
		context.drawString(this.font, lang.getOrDefault("newvmcomputers.pc_editing.close"), 4, 4, -1, false);
		ms.popPose();
	}

	private void removeISO() {
		if(!ClientMod.useVmware && (ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
			try {
				ClientMod.vmSession.getMachine().unmountMedium("IDE Controller", 1, 0, true);
			}catch(VBoxException ex) { /* Ignored */ }
		}
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_ISO, b);
	}

	private void insertISO(String name) {
		if(!ClientMod.useVmware) {
			if(ClientMod.vmTurnedOn && ClientMod.vmEntityID == pc_case.getId()) {
				IMedium m = ClientMod.vb.openMedium(new File(ClientMod.isoDirectory, name).getPath(), DeviceType.DVD, AccessMode.ReadOnly, true);
				ClientMod.vmSession.getMachine().mountMedium("IDE Controller", 1, 0, m, true);
			}
			if(ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) {
				if (minecraft.player != null)
					minecraft.player.displayClientMessage(Component.translatable("newvmcomputers.waitingforvmtostart").withStyle(ChatFormatting.YELLOW), false);

				synchronized (vmTurningON) {
					try {
						vmTurningON.wait();
					} catch (InterruptedException e) { /* Ignored */ }
					IMedium m = ClientMod.vb.openMedium(new File(ClientMod.isoDirectory, name).getPath(), DeviceType.DVD, AccessMode.ReadOnly, true);
					ClientMod.vmSession.getMachine().mountMedium("IDE Controller", 1, 0, m, true);
				}
			}
		} else {
			if (minecraft.player != null && (ClientMod.vmTurnedOn || ClientMod.vmTurningOn)) {
				minecraft.player.displayClientMessage(Component.literal("Note: On-the-fly ISO disk replacement is not yet supported in VMware. Restart your PC.").withStyle(ChatFormatting.YELLOW), false);
			}
		}

		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeUtf(name);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_ISO, b);
	}

	public void turnOffPC(Button widget) {
		ClientMod.vmTurningOff = true;
		ClientMod.vmTurnedOn = false;
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		ClientPlayNetworking.send(PacketList.C2S_TURN_OFF_PC, b);

		new Thread(() -> {
			while(ClientMod.vmTurningOn) { /* wait */ }

			if (ClientMod.useVmware) {
				try {
					File vmxFile = new File(ClientMod.vhdDirectory.getParentFile(), "vmware_vm.vmx");
					String vmrunPath = ClientMod.vmwareDirectory + File.separator + (SystemUtils.IS_OS_WINDOWS ? "vmrun.exe" : "vmrun");
					ProcessBuilder pb = new ProcessBuilder(vmrunPath, "-T", "ws", "stop", vmxFile.getAbsolutePath(), "hard");
					Process p = pb.start();
					p.waitFor();

					ClientMod.vmTurnedOn = false;
					ClientMod.vmTurningOff = false;
					ClientMod.vmEntityID = -1;
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				try {
					ClientMod.closeVirtualBoxSession(true);
				} finally {
					ClientMod.vmSession = null;
					ClientMod.vmTurnedOn = false;
					ClientMod.vmTurningOff = false;
					ClientMod.vmEntityID = -1;
				}
			}
		}, "Turn off PC").start();
	}

	public void turnOnPC(Button widget) {
		if(pc_case.getCpuDividedBy() > 0 && pc_case.getGpuInstalled() && pc_case.getMotherboardInstalled() && (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()) >= 1) {
			if(!pc_case.getHardDriveFileName().isEmpty()) {
				if(!new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
					if(minecraft.player != null)
						minecraft.player.displayClientMessage(Component.translatable("newvmcomputers.hdd_doesnt_exist").withStyle(ChatFormatting.RED), false);
					return;
				}
			}
			if(!pc_case.getIsoFileName().isEmpty()) {
				if(!new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
					if(minecraft.player != null)
						minecraft.player.displayClientMessage(Component.translatable("newvmcomputers.iso_doesnt_exist").withStyle(ChatFormatting.RED), false);
					return;
				}
			}

			if(ClientMod.vmTurningOn || ClientMod.vmTurnedOn) {
				return;
			}
			ClientMod.vmTurningOn = true;
			ClientMod.vmEntityID = pc_case.getId();
			FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
			b.writeInt(pc_case.getId());
			ClientPlayNetworking.send(PacketList.C2S_TURN_ON_PC, b);

			new Thread(() -> {
				if (ClientMod.useVmware) {
					try {
						if (minecraft.player != null) {
							minecraft.player.displayClientMessage(Component.literal("Creating a .vmx file and running VMware...").withStyle(ChatFormatting.GOLD), false);
						}

						// 1. Р В Р’В Р вЂ™Р’В Р В Р’В Р В РІР‚в„–Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В·Р В Р’В Р вЂ™Р’В Р В РЎС›Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’В Р В Р Р‹Р вЂ™Р’В Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’ВµР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р В Р вЂ№Р В Р’В Р РЋРІР‚СљР В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІвЂћСћР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р Р†РІР‚С›РІР‚вЂњР В Р’В Р вЂ™Р’В Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚Сљ Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р вЂ™Р’В Р В Р вЂ Р Р†Р вЂљРЎвЂєР Р†Р вЂљРІР‚СљР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В» Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎСљР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРЎС›Р В Р’В Р вЂ™Р’В Р В Р’В Р Р†Р вЂљР’В¦Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р РЋРІР‚С”Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљРІР‚СљР В Р’В Р В Р вЂ№Р В Р Р‹Р Р†Р вЂљРЎС™Р В Р’В Р В Р вЂ№Р В Р’В Р Р†Р вЂљРЎв„ўР В Р’В Р вЂ™Р’В Р В РІР‚в„ўР вЂ™Р’В°Р В Р’В Р В Р вЂ№Р В Р вЂ Р В РІР‚С™Р вЂ™Р’В Р В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’ВР В Р’В Р вЂ™Р’В Р В Р Р‹Р Р†Р вЂљР’В .vmx
						File vmxFile = new File(ClientMod.vhdDirectory.getParentFile(), "vmware_vm.vmx");
						File vmwareDisk = resolveVmwareDiskFile();
						try (java.io.FileWriter fw = new java.io.FileWriter(vmxFile)) {
							fw.write(".encoding = \"UTF-8\"\n");
							fw.write("config.version = \"8\"\n");
							fw.write("virtualHW.version = \"16\"\n");

							fw.write("guestOS = \"" + (pc_case.get64Bit() ? "other-64" : "other") + "\"\n");

							int cpus = Math.max(1, Runtime.getRuntime().availableProcessors() / pc_case.getCpuDividedBy());
							fw.write("numvcpus = \"" + cpus + "\"\n");

							long ramMB = (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()) * 1024L;
							fw.write("memsize = \"" + Math.min((long)ClientMod.maxRam, ramMB) + "\"\n");

							if (ClientMod.useVmware3D) {
								fw.write("mks.enable3d = \"TRUE\"\n");
							} else {
								fw.write("mks.enable3d = \"FALSE\"\n");
							}
							fw.write("svga.vramSize = \"" + ((long)ClientMod.videoMem * 1024 * 1024) + "\"\n");
							fw.write("usb.present = \"TRUE\"\n");
							fw.write("usb.generic.allowHID = \"TRUE\"\n");
							fw.write("mouse.vusb.enable = \"TRUE\"\n");
							if(vmwareDisk != null) {
								fw.write("ide0:0.present = \"TRUE\"\n");
								fw.write("ide0:0.fileName = \"" + vmwareDisk.getAbsolutePath().replace("\\", "\\\\") + "\"\n");
							}
							if(!pc_case.getIsoFileName().isEmpty()) {
								File iso = new File(ClientMod.isoDirectory, pc_case.getIsoFileName());

								fw.write("ide1:0.present = \"TRUE\"\n");


								fw.write("ide1:0.fileName = \"" + iso.getAbsolutePath().replace("\\", "/") + "\"\n");

								fw.write("ide1:0.deviceType = \"cdrom-image\"\n");
								fw.write("ide1:0.startConnected = \"TRUE\"\n");
								fw.write("bios.bootOrder = \"cdrom,hdd\"\n");
								fw.write("firmware = \"efi\"\n");
							}
							fw.write("ethernet0.present = \"TRUE\"\n");
							fw.write("ethernet0.connectionType = \"nat\"\n");
							fw.write("ethernet0.virtualDev = \"e1000\"\n");
							fw.write("RemoteDisplay.vnc.enabled = \"TRUE\"\n");
							fw.write("RemoteDisplay.vnc.port = \"5900\"\n");
						}
						String vmrunPath = ClientMod.vmwareDirectory + File.separator + (SystemUtils.IS_OS_WINDOWS ? "vmrun.exe" : "vmrun");
						ProcessBuilder pb = new ProcessBuilder(vmrunPath, "-T", "ws", "start", vmxFile.getAbsolutePath(), "nogui");
						pb.redirectErrorStream(true);
						Process p = pb.start();
						boolean finished = p.waitFor(30, TimeUnit.SECONDS);
						if (!finished) {
							p.destroyForcibly();
							throw new IOException("vmrun timed out while starting VMware.");
						}
						String output = readProcessOutput(p);
						if (p.exitValue() != 0) {
							throw new IOException("vmrun exited with code " + p.exitValue() + (output.isEmpty() ? "" : ": " + output));
						}
						if (!waitForLocalPort("127.0.0.1", 5900, 15000L)) {
							throw new IOException("VMware started but VNC port 5900 did not open.");
						}

						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = true;

					} catch (Exception ex) {
						if(minecraft.player != null) {
							minecraft.player.displayClientMessage(Component.translatable("newvmcomputers.failed_to_start", ex.getMessage()).withStyle(ChatFormatting.RED), false);
						}
						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = false;
						FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
						ClientPlayNetworking.send(PacketList.C2S_TURN_OFF_PC, buf);
					}

				}

				 else {
					ArrayList<ISession> usedSessions = new ArrayList<>();
					try {
						ClientMod.closeVirtualBoxSession(false);
						IMachine found = null;
						try {
							found = ClientMod.vb.findMachine("VmComputersVm");
						} catch(VBoxException e) { /* Ignored */ }

						if(found != null) {
							if(found.getState() == MachineState.Running) {
								ISession sess = ClientMod.vbManager.getSessionObject();
								found.lockMachine(sess, LockType.Shared);
								IProgress ip = sess.getConsole().powerDown();
								ip.waitForCompletion(-1);
								sess.unlockMachine();
							}
							ISession sess = ClientMod.vbManager.getSessionObject();
							found.lockMachine(sess, LockType.Write);
							usedSessions.add(sess);
							IMachine edit = sess.getMachine();
							String OSType = edit.getOSTypeId();
							if(pc_case.get64Bit()) {
								if(!OSType.endsWith("_64"))
									OSType += "_64";
							} else {
								OSType = OSType.replace("_64","");
							}
							edit.setOSTypeId(OSType);
							edit.setMemorySize((long) Math.min(ClientMod.maxRam, (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1())));
							edit.setCPUCount(Math.max(1, ClientMod.vb.getHost().getProcessorCount() / pc_case.getCpuDividedBy()));
							edit.getGraphicsAdapter().setAccelerate2DVideoEnabled(true);
							edit.getGraphicsAdapter().setAccelerate3DEnabled(true);
							edit.getGraphicsAdapter().setVRAMSize((long)ClientMod.videoMem);
							try {
								edit.removeStorageController("IDE Controller");
							} catch (VBoxException ex) { /* Ignored */ }
							edit.addStorageController("IDE Controller", StorageBus.IDE);

							if(!pc_case.getHardDriveFileName().isEmpty() && new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
								IMedium medium = ClientMod.vb.openMedium(new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).getPath(), DeviceType.HardDisk, AccessMode.ReadWrite, true);
								edit.attachDevice("IDE Controller", 0, 0, DeviceType.HardDisk, medium);
							}

							if(!pc_case.getIsoFileName().isEmpty()) {
								if(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
									IMedium cd = ClientMod.vb.openMedium(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).getPath(), DeviceType.DVD, AccessMode.ReadOnly, true);
									try {
										edit.attachDevice("IDE Controller", 1, 0, DeviceType.DVD, cd);
									} catch(VBoxException ex) { /* Ignored */ }
								} else if(pc_case.getIsoFileName().equals("Additions")) {
									IMedium cd = ClientMod.vb.openMedium(new File(ClientMod.vb.getSystemProperties().getDefaultAdditionsISO()).getPath(), DeviceType.DVD, AccessMode.ReadOnly, true);
									try {
										edit.attachDevice("IDE Controller", 1, 0, DeviceType.DVD, cd);
									} catch(VBoxException ex) { /* Ignored */ }
								}
							}

							if(pc_case.getIsoFileName().isEmpty()) {
								edit.attachDevice("IDE Controller",1,0,DeviceType.DVD,null);
							}
							edit.saveSettings();
							sess.unlockMachine();
							usedSessions.remove(sess);
						} else {
							String OSType = "Other";
							if(pc_case.get64Bit()) {
								OSType += "_64";
							}
							IMachine machine = ClientMod.vb.createMachine("", "VmComputersVm", null, OSType, "");
							ClientMod.vb.registerMachine(machine);
							ISession sess = ClientMod.vbManager.getSessionObject();
							machine.lockMachine(sess, LockType.Write);
							usedSessions.add(sess);
							IMachine edit = sess.getMachine();
							edit.setMemorySize((long) Math.min(ClientMod.maxRam, (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1())));
							edit.setCPUCount(Math.max(1, ClientMod.vb.getHost().getProcessorCount() / pc_case.getCpuDividedBy()));
							edit.getGraphicsAdapter().setAccelerate2DVideoEnabled(true);
							edit.getGraphicsAdapter().setAccelerate3DEnabled(true);
							edit.getGraphicsAdapter().setVRAMSize((long)ClientMod.videoMem);
							edit.addStorageController("IDE Controller", StorageBus.IDE);

							if(!pc_case.getHardDriveFileName().isEmpty() && new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
								IMedium medium = ClientMod.vb.openMedium(new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).getPath(), DeviceType.HardDisk, AccessMode.ReadWrite, true);
								edit.attachDevice("IDE Controller", 0, 0, DeviceType.HardDisk, medium);
							}

							if(!pc_case.getIsoFileName().isEmpty()) {
								if(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
									IMedium cd = ClientMod.vb.openMedium(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).getPath(), DeviceType.DVD, AccessMode.ReadOnly, true);
									try {
										edit.attachDevice("IDE Controller", 1, 0, DeviceType.DVD, cd);
									} catch(VBoxException ex) { /* Ignored */ }
								} else if(pc_case.getIsoFileName().equals("Additions")) {
									IMedium cd = ClientMod.vb.openMedium(new File(ClientMod.vb.getSystemProperties().getDefaultAdditionsISO()).getPath(), DeviceType.DVD, AccessMode.ReadOnly, true);
									try {
										edit.attachDevice("IDE Controller", 1, 0, DeviceType.DVD, cd);
									} catch(VBoxException ex) { /* Ignored */ }
								}
							}

							if(pc_case.getIsoFileName().isEmpty()) {
								edit.attachDevice("IDE Controller",1,0,DeviceType.DVD,null);
							}
							edit.saveSettings();
							sess.unlockMachine();
							usedSessions.remove(sess);
						}

						IMachine machine = ClientMod.vb.findMachine("VmComputersVm");
						ClientMod.vmSession = ClientMod.vbManager.getSessionObject();
						IProgress pr = machine.launchVMProcess(ClientMod.vmSession, "headless", Collections.emptyList());
						pr.waitForCompletion(-1);
						ClientMod.vmTurningOn = false;
						ClientMod.vmTurningOff = false;
						ClientMod.vmTurnedOn = true;
						synchronized (vmTurningON) {
							vmTurningON.notifyAll();
						}
					} catch(Exception ex) {
						for(ISession is : usedSessions) {
							try {
								is.unlockMachine();
							} catch(Exception exx) { /* Ignored */ }
						}
						if(minecraft.player != null) {
							minecraft.player.displayClientMessage(Component.translatable("newvmcomputers.failed_to_start", ex.getMessage()).withStyle(ChatFormatting.RED), false);
							minecraft.player.displayClientMessage(Component.translatable("newvmcomputers.contact_me").withStyle(ChatFormatting.RED), false);
						}
						ClientMod.closeVirtualBoxSession(false);
						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = false;
						ClientMod.vmTurningOff = false;
						ClientMod.vmEntityID = -1;

						FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
						ClientPlayNetworking.send(PacketList.C2S_TURN_OFF_PC, buf);
					}
				}
			}, "Turn on PC").start();
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}




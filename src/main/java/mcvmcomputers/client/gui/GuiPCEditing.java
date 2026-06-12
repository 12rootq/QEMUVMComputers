package mcvmcomputers.client.gui;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.screens.Screen;


import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;


import mcvmcomputers.client.ClientMod;
import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.item.ItemHarddrive;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.MVCUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;

import com.mojang.math.Axis;
import io.netty.buffer.Unpooled;

import net.minecraft.core.component.DataComponents;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;

import org.joml.Quaternionf;

public class GuiPCEditing extends net.minecraft.client.gui.screens.Screen{
	@Override
	public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
	}

	private float introScale;
	private float panelX;
	private EntityPC pc_case;
	private boolean openCase;
	private Minecraft minecraft;
	private long lastEjectCheckTime = 0;

	private final Language lang = Language.getInstance();

	private static final ItemStack CASE_NO_PANEL = new ItemStack(ItemList.PC_CASE_NO_PANEL);
	private static final ItemStack CASE_ONLY_PANEL = new ItemStack(ItemList.PC_CASE_ONLY_PANEL);
	private static final ItemStack CASE_ONLY_GLASS_PANEL = new ItemStack(ItemList.PC_CASE_GLASS_PANEL);
	private static final ItemStack MOBO = new ItemStack(ItemList.ITEM_MOTHERBOARD);
	private static final ItemStack CPU = new ItemStack(ItemList.ITEM_CPU2);
	private static final ItemStack GPU = new ItemStack(ItemList.ITEM_GPU);
	private static final ItemStack RAM = new ItemStack(ItemList.ITEM_RAM1G);
	private static final ItemStack HARD_DRIVE = new ItemStack(ItemList.ITEM_HARDDRIVE);


	public GuiPCEditing(EntityPC pc_case) {
		super(Component.translatable("text.pc_editor.title"));
		this.pc_case = pc_case;
		minecraft = Minecraft.getInstance();
	}

	public void renderBackgroundAndMobo(GuiGraphics context) {
		context.fillGradient(0, 0, this.width, this.height, new Color(0f,0f,0f,Math.max(0.5f*introScale,0)).getRGB(), new Color(0f,0f,0f,0.5f*introScale).getRGB());
		context.pose().pushPose();
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		context.pose().translate((this.width / 2), (this.height / 2)-40, 100.0F);
		context.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f*introScale));
		context.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(-6f*introScale*0.1f));
		context.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(6f*introScale*0.1f));
		context.pose().scale(1.0F, -1.0F, 1.0F);
		context.pose().scale(introScale, introScale, introScale);
		context.pose().scale(230.0F, 230.0F, 230.0F);

		RenderSystem.enableDepthTest();
		renderItem(CASE_NO_PANEL, context);


		RenderSystem.disableDepthTest();
		context.pose().pushPose();
		context.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90f));
		context.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90f));
		context.pose().scale(0.55f, 0.55f, 0.55f);
		context.pose().translate(0.06f, 0.28f, -0.29f); if(pc_case.getMotherboardInstalled()) {
			renderItem(MOBO, context);
		}
		if(pc_case.getGpuInstalled()) {
			context.pose().pushPose();
			context.pose().translate(0.24, 0.07f, -0.28f);
			renderItem(GPU, context);
			context.pose().popPose();
		}
		if(pc_case.getCpuDividedBy() > 0) {
			context.pose().pushPose();
			context.pose().translate(0.06, 0.12f, 0.06f);
			renderItem(CPU, context);
			context.pose().popPose();
		}
		if(pc_case.getGigsOfRamInSlot0() > 0) {
			context.pose().pushPose();
			context.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f));
			context.pose().translate(-0.22f, 0.1f, -0.285f);
			renderItem(RAM, context);
			context.pose().popPose();
		}
		if(pc_case.getGigsOfRamInSlot1() > 0) {
			context.pose().pushPose();
			context.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f));
			context.pose().translate(-0.22f, 0.1f, -0.404f);
			renderItem(RAM, context);
			context.pose().popPose();
		}
		if(!pc_case.getHardDriveFileName().isEmpty()) {
			context.pose().pushPose();
			context.pose().mulPose(com.mojang.math.Axis.XP.rotationDegrees(90f));
			context.pose().mulPose(com.mojang.math.Axis.YP.rotationDegrees(90f));
			context.pose().translate(-0.2f, 0, -0.6f);
			renderItem(HARD_DRIVE, context);
			context.pose().popPose();
		}
		context.pose().popPose();

		RenderSystem.enableDepthTest();
		context.pose().pushPose();
		context.pose().translate(0, -panelX, 0); if(pc_case.getGlassSidepanel()) {
			renderItem(CASE_ONLY_GLASS_PANEL, context);
		}else{
			renderItem(CASE_ONLY_PANEL, context);
		}
		context.pose().popPose();
		context.pose().popPose();
	}

	private void renderItem(ItemStack stack, GuiGraphics context) {
		BakedModel mdll = minecraft.getItemRenderer().getModel(stack, null, null, 0);
		MultiBufferSource.BufferSource immediatee = Minecraft.getInstance().renderBuffers().bufferSource();
		this.minecraft.getItemRenderer().render(stack, ItemDisplayContext.NONE, false, context.pose(), immediatee, 15728640, OverlayTexture.NO_OVERLAY, mdll);
		immediatee.endBatch();
	}


	private void addMotherboard(boolean sixtyFour) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeBoolean(sixtyFour);
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_add_mobo", b);
	}

	private void removeMotherboard() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_remove_mobo", b);
	}

	private void addCPU(Item cpuItem, int dividedBy) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(dividedBy);
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_add_cpu", b);
	}

	private void addGPU() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_add_gpu", b);
	}

	private void addHardDrive(String fileName) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeUtf(fileName);
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_add_hard_drive", b);
	}

	private void removeHardDrive() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_remove_hard_drive", b);
	}

	private void addRamStick(Item ramItem, int megs) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(megs);
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_add_ram", b);
	}

	private void removeRamStick(int slot) {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(slot);
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_remove_ram", b);
	}
	private void removeCPU() {
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_remove_cpu", b);
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
		if(introScale > 0.99f) {
			if(openCase) {
				if((ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
					openCase = false;
				}
				if(SystemUtils.IS_OS_MAC) {
					context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.put_panel_back_mac"), 4, 14, -1);
					if(GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}else {
					context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.put_panel_back"), 4, 14, -1);
					if(GLFW.glfwGetKey(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}
				if(pc_case.getMotherboardInstalled()) {
					this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeMotherboard()).bounds(this.width/2-70, this.height / 2 - 70, 10, 10).build());
					RenderSystem.disableDepthTest();
					context.pose().pushPose();
					context.pose().translate(0, 0, 200); if(pc_case.get64Bit()) {
						context.drawString(this.font, lang.getOrDefault("mcvmcomputers.64bit"), this.width/2 - 66, this.height/2 - 56, -1);
					}else {
						context.drawString(this.font, lang.getOrDefault("mcvmcomputers.32bit"), this.width/2 - 66, this.height/2 - 56, -1);
					}
					context.pose().popPose();
					RenderSystem.enableDepthTest(); if(pc_case.getCpuDividedBy() == 0) {
						RenderSystem.disableDepthTest();
						context.pose().pushPose();
						context.pose().translate(0, 0, 200);
						context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.add_cpu"), this.width/2 - 120, this.height/2 - 40, -1);
						context.pose().popPose();
						RenderSystem.enableDepthTest();
						int addCpuWidth = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6"));
						Button div2 = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "2")), (btn) -> this.addCPU(ItemList.ITEM_CPU2, 2)).bounds(this.width/2 - (addCpuWidth+59), this.height / 2 - 31, addCpuWidth+4, 12).build();
						Button div4 = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "4")), (btn) -> this.addCPU(ItemList.ITEM_CPU4, 4)).bounds(this.width/2 - (addCpuWidth+59), this.height / 2 - 18, addCpuWidth+4, 12).build();
						Button div6 = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6")), (btn) -> this.addCPU(ItemList.ITEM_CPU6, 6)).bounds(this.width/2 - (addCpuWidth+59), this.height / 2 - 5, addCpuWidth+4, 12).build();
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU2))) {
							div2.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU4))) {
							div4.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU6))) {
							div6.active = false;
						}
						this.addRenderableWidget(div2);
						this.addRenderableWidget(div4);
						this.addRenderableWidget(div6);
					}else {
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeCPU()).bounds(this.width/2-43, this.height / 2 - 16, 10, 10).build());
						RenderSystem.disableDepthTest();
						context.pose().pushPose();
						context.pose().translate(0, 0, 200);
						context.drawString(this.font, "1/" + pc_case.getCpuDividedBy(), this.width/2-25, this.height/2+2, -1);
						context.pose().popPose();
						RenderSystem.enableDepthTest();
					}
					if(!pc_case.getGpuInstalled()) {
						int addGpuWidth = this.font.width(lang.getOrDefault("mcvmcomputers.pc_editing.add_gpu"));
						Button bw = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_gpu")), (btn) -> this.addGPU()).bounds(this.width/2 - 64, this.height / 2 + 33, addGpuWidth+4, 12).build();
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_GPU)))
							bw.active = false;
						this.addRenderableWidget(bw);
					}
					if(pc_case.getHardDriveFileName().isEmpty()) {
						int lastYOffset = 0;
						int xOffCount = 0;
						int lastXOffset = 0;
						int count = 0;
						RenderSystem.disableDepthTest();
						context.pose().pushPose();
						context.pose().translate(0, 0, 200);
						context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.add_vhds"), this.width/2 + 20, this.height/2 + 30, -1);
						context.pose().popPose();
						RenderSystem.enableDepthTest();
						for(ItemStack is : minecraft.player.getInventory().items) {
							if(is.getItem() instanceof ItemHarddrive) {
								CustomData nbtComp = is.get(DataComponents.CUSTOM_DATA);
								if(nbtComp != null){
									CompoundTag nbt = nbtComp.copyTag();
									if(nbt.contains("vhdfile")) {
		    							String file = nbt.getString("vhdfile");
		    							if(new File(ClientMod.vhdDirectory, file).exists()) {
		    								int w = Math.max(50, this.font.width(file)+4);
		    								this.addRenderableWidget(Button.builder(Component.literal(file), (btn) -> this.addHardDrive(file)).bounds(this.width/2 + 20 + lastXOffset, this.height / 2 + 40 + lastYOffset, Math.max(50, this.font.width(file)+4), 12).build());
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
							RenderSystem.disableDepthTest();
							context.pose().pushPose();
							context.pose().translate(0, 0, 200);
							context.drawString(this.font, (char) (0xfeff00a7) + "7" + lang.getOrDefault("mcvmcomputers.pc_editing.no_valid_vhd"), this.width/2 + 20, this.height/2 + 40, -1);
							context.pose().popPose();
							RenderSystem.enableDepthTest();
						}
					}else {
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeHardDrive()).bounds(this.width/2+30, this.height / 2 + 55, 10, 10).build());
						RenderSystem.disableDepthTest();
						context.pose().pushPose();
						context.pose().translate(0, 0, 200);
						context.drawString(this.font, pc_case.getHardDriveFileName(), this.width/2+45, this.height/2+65, -1);
						context.pose().popPose();
						RenderSystem.enableDepthTest();
					}
					if(pc_case.getGigsOfRamInSlot0() == 0 || pc_case.getGigsOfRamInSlot1() == 0) {
						RenderSystem.disableDepthTest();
						context.pose().pushPose();
						context.pose().translate(0, 0, 200);
						context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.add_ram"), this.width/2 + 50, this.height/2 - 75, -1);
						context.pose().popPose();
						RenderSystem.enableDepthTest();
						int addMBRamWidth = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512"))+4;
						Button sixfourM = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "64")), (btn) -> this.addRamStick(ItemList.ITEM_RAM64M, 64)).bounds(this.width/2 + 50, this.height / 2 - 64, addMBRamWidth, 12).build();
						Button oneM = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "128")), (btn) -> this.addRamStick(ItemList.ITEM_RAM128M, 128)).bounds(this.width/2 + 50, this.height / 2 - 51, addMBRamWidth, 12).build();
						Button twoM = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "256")), (btn) -> this.addRamStick(ItemList.ITEM_RAM256M, 256)).bounds(this.width/2 + 50, this.height / 2 - 38, addMBRamWidth, 12).build();
						Button fiveM = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512")), (btn) -> this.addRamStick(ItemList.ITEM_RAM512M, 512)).bounds(this.width/2 + 50, this.height / 2 - 25, addMBRamWidth, 12).build();
						Button oneG = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_ram_btn").replace("%s", "1")), (btn) -> this.addRamStick(ItemList.ITEM_RAM1G, 1024)).bounds(this.width/2 + 50, this.height / 2 - 12, addMBRamWidth, 12).build();
						Button twoG = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_ram_btn").replace("%s", "2")), (btn) -> this.addRamStick(ItemList.ITEM_RAM2G, 2048)).bounds(this.width/2 + 50, this.height / 2 + 1, addMBRamWidth, 12).build();
						Button fourG = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_ram_btn").replace("%s", "4")), (btn) -> this.addRamStick(ItemList.ITEM_RAM4G, 4096)).bounds(this.width/2 + 50, this.height / 2 + 14, addMBRamWidth, 12).build();
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM64M))) {
							sixfourM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM128M))) {
							oneM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM256M))) {
							twoM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM512M))) {
							fiveM.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM1G))) {
							oneG.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM2G))) {
							twoG.active = false;
						}
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM4G))) {
							fourG.active = false;
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
						RenderSystem.disableDepthTest();
						context.pose().pushPose();
						context.pose().translate(0, 0, 200); if(pc_case.getGigsOfRamInSlot0() < 1000 && pc_case.getGigsOfRamInSlot0() >= 100) {
							context.drawString(this.font, (int) pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+4, this.height/2+2, -1);
						}else if(pc_case.getGigsOfRamInSlot0() < 100) {
							context.drawString(this.font, (int) pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+10, this.height/2+2, -1);
						}else {
							context.drawString(this.font, (int) pc_case.getGigsOfRamInSlot0()/1024 + " GB", this.width / 2 + 16, this.height / 2 + 2, -1);
						}
						context.pose().popPose();
						RenderSystem.enableDepthTest();
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeRamStick(0)).bounds(this.width/2+21, this.height / 2 - 70, 10, 10).build());
					}
					if(pc_case.getGigsOfRamInSlot1() > 0) {
						RenderSystem.disableDepthTest();
						context.pose().pushPose();
						context.pose().translate(0, 0, 200); if(pc_case.getGigsOfRamInSlot1() < 1000) {
							context.drawString(this.font, (int) pc_case.getGigsOfRamInSlot1() + " MB", this.width/2+42, this.height/2+2, -1);
						}else {
							context.drawString(this.font, (int) pc_case.getGigsOfRamInSlot1()/1024 + " GB", this.width / 2+42, this.height / 2+2, -1);
						}
						context.pose().popPose();
						RenderSystem.enableDepthTest();
						this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> this.removeRamStick(1)).bounds(this.width/2 + 37, this.height / 2 - 70, 10, 10).build());
					}
				}else {
					int thirtytwow = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.add_32bit_mobo"))+4;
					Button thirtytwo = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_32bit_mobo")), (btn) -> this.addMotherboard(false)).bounds(this.width/2 - (thirtytwow/2), this.height / 2 - 23, thirtytwow, 14).build();
					int sixtyfourw = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.add_64bit_mobo"))+4;
					Button sixtyfour = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.add_64bit_mobo")), (btn) -> this.addMotherboard(true)).bounds(this.width/2 - (sixtyfourw/2), this.height / 2 - 7, sixtyfourw, 14).build();

					if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD))) {
						thirtytwo.active = false;
					}
					if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD64))) {
						sixtyfour.active = false;
					}

					this.addRenderableWidget(thirtytwo);
					this.addRenderableWidget(sixtyfour);
				}
			}else {
				boolean turnedOn = (ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) || (ClientMod.vmTurnedOn && ClientMod.vmEntityID == pc_case.getId());
				if(turnedOn) {
					int buttonW = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.turn_off"))+4;
					this.addRenderableWidget(Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.turn_off")), (btn) -> this.turnOffPC(btn)).bounds((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}else {
					int buttonW = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.turn_on"))+4;
					this.addRenderableWidget(Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.turn_on")), (btn) -> this.turnOnPC(btn)).bounds((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}

				if(ClientMod.vmTurnedOn && !pc_case.getIsoFileName().isEmpty()) {
					try {
						long now = System.currentTimeMillis();
						if (now - lastEjectCheckTime > 2000) {
							lastEjectCheckTime = now;
							if(ClientMod.vbox.isMediumEjected("VmComputersVm", "IDE Controller", 1, 0)) {
								this.removeISO();
							}
						}
					}catch(Exception e) {}
				}

				if(pc_case.getIsoFileName().isEmpty()) {
					RenderSystem.disableDepthTest();
					context.pose().pushPose();
					context.pose().translate(0, 0, 200);
					context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.select_iso"), this.width/2 - 75, this.height/2 - 75, -1);
					context.pose().popPose();
					RenderSystem.enableDepthTest();
					int offX = 0;
					int offY = 0;
					for(File f : ClientMod.isoDirectory.listFiles()) {
						if(f.getName().endsWith(".iso") || f.getName().endsWith(".ISO")) {
							if((this.width/2 - 75 + offX) + this.font.width(f.getName())+10 > this.width/2 + 105) {
								offX = 0;
								offY += 14;
							}
							this.addRenderableWidget(Button.builder(Component.literal(f.getName()), (btn) -> insertISO(f.getName())).bounds(this.width/2 - 75 + offX, this.height / 2 - 62 + offY, this.font.width(f.getName())+8, 12).build());
							offX += this.font.width(f.getName())+10;
						}
					}
				}else {
					RenderSystem.disableDepthTest();
					context.pose().pushPose();
					context.pose().translate(0, 0, 200);
					context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.inserted_iso"), this.width/2 - 75, this.height/2 - 75, -1);
					context.drawString(this.font, (char) (0xfeff00a7) + "7" + pc_case.getIsoFileName(), this.width/2 - 75, this.height/2 - 65, -1);
					context.pose().popPose();
					RenderSystem.enableDepthTest();
					int ejectW = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.eject"));
					this.addRenderableWidget(Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.eject")), (btn) -> removeISO()).bounds(this.width/2 - 75, this.height / 2 - 50, ejectW+4, 12).build());
				}
				int openCaseW = font.width(lang.getOrDefault("mcvmcomputers.pc_editing.open_case"));
				Button bw = Button.builder(Component.literal(lang.getOrDefault("mcvmcomputers.pc_editing.open_case")), (btn) -> openCase = true).bounds(this.width/2 - 82, this.height / 2 + 65, openCaseW+4, 12).build();
				bw.active = !turnedOn;
				this.addRenderableWidget(bw);
			}
		}
		RenderSystem.disableDepthTest();
		context.pose().translate(0, 0, 200);
		super.render(context, mouseX, mouseY, delta);
		context.drawString(this.font, lang.getOrDefault("mcvmcomputers.pc_editing.close"), 4, 4, -1);
		RenderSystem.enableDepthTest();
	}

	private void removeISO() {
		if((ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
			try {
				ClientMod.vbox.unmountMedium("VmComputersVm", "IDE Controller", 1, 0);
			}catch(Exception ex) {}
		}
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_remove_iso", b);
	}

	private void insertISO(String name) {
		String isoPath = new File(ClientMod.isoDirectory, name).getPath();
		if(ClientMod.vmTurnedOn && ClientMod.vmEntityID == pc_case.getId()) {
			ClientMod.vbox.mountMedium("VmComputersVm", "IDE Controller", 1, 0, isoPath);
		}
		if(ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) {
			minecraft.player.sendSystemMessage(Component.translatable("mcvmcomputers.waitingforvmtostart").withStyle(ChatFormatting.YELLOW));
			synchronized (ClientMod.VM_TURNING_ON_LOCK) {
				try {
					while(ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) {
						ClientMod.VM_TURNING_ON_LOCK.wait(5000);
					}
				} catch (InterruptedException e) {}
				ClientMod.vbox.mountMedium("VmComputersVm", "IDE Controller", 1, 0, isoPath);
			}
		}
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		b.writeUtf(name);
		b.writeInt(this.pc_case.getId());
		PacketList.sendToServer("c2s_add_iso", b);
	}

	public void turnOffPC(Button wdgt) {
		ClientMod.vmTurningOff = true;
		ClientMod.vmTurnedOn = false;
		FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
		PacketList.sendToServer("c2s_turn_off_pc", b);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while(ClientMod.vmTurningOn) {
						Thread.sleep(50);
					}
				} catch (InterruptedException e) {

				}
				ClientMod.vbox.powerOffVm("VmComputersVm");

				ClientMod.vmTurnedOn = false;
				ClientMod.vmTurningOff = false;
				ClientMod.vmEntityID = -1;
			}
		}, "Turn off PC").start();
	}

	public void turnOnPC(Button wdgt) {
		if(pc_case.getCpuDividedBy() > 0 && pc_case.getGpuInstalled() && pc_case.getMotherboardInstalled() && (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()) >= 1) {
			if(!pc_case.getHardDriveFileName().isEmpty()) {
				if(!new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
					minecraft.player.sendSystemMessage(Component.translatable("mcvmcomputers.hdd_doesnt_exist").withStyle(ChatFormatting.RED));
					return;
				}
			}
			if(!pc_case.getIsoFileName().isEmpty()) {
				if(!pc_case.getIsoFileName().equals("Additions") && !new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
					minecraft.player.sendSystemMessage(Component.translatable("mcvmcomputers.iso_doesnt_exist").withStyle(ChatFormatting.RED));
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
			PacketList.sendToServer("c2s_turn_on_pc", b);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						boolean vmExists = ClientMod.vbox.vmExists("VmComputersVm");

						if(vmExists) {
							String state = ClientMod.vbox.getVmState("VmComputersVm");

							if("running".equals(state) || "firstonline".equals(state) || "starting".equals(state)) {
								ClientMod.vbox.powerOffVm("VmComputersVm");
							}
							if("saved".equals(state)) {
								ClientMod.vbox.discardSavedState("VmComputersVm");
							}


							String OSType = "Other"; if(pc_case.get64Bit()) {
								OSType += "_64";
							}
							long ramMB = Math.min(ClientMod.maxRam, (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()));
							int cpuCount = Math.max(1, ClientMod.vbox.getHostProcessorCount() / pc_case.getCpuDividedBy());

							ClientMod.vbox.modifyVmWith3dAccelFallback("VmComputersVm",
									"--ostype", OSType,
									"--memory", String.valueOf(ramMB),
									"--cpus", String.valueOf(cpuCount),
									"--vram", String.valueOf(ClientMod.videoMem),
									"--accelerate3d", "on");

							ClientMod.vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");


							ClientMod.vbox.removeStorageController("VmComputersVm", "IDE Controller");
							ClientMod.vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

							if(!pc_case.getHardDriveFileName().isEmpty()) {
								if(new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
									String hddPath = new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", hddPath);
								}
							}
							if(!pc_case.getIsoFileName().isEmpty()) {
								if(pc_case.getIsoFileName().equals("Additions")) {
									String additionsPath = ClientMod.vbox.getDefaultAdditionsISO();
									if(additionsPath != null) {
										ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", additionsPath);
									}
								}else if(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
									String isoPath = new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", isoPath);
								}
							}
							if(pc_case.getIsoFileName().isEmpty()) {
								ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
							}
						}else {

							String OSType = "Other"; if(pc_case.get64Bit()) {
								OSType += "_64";
							}
							ClientMod.vbox.createVm("VmComputersVm", OSType);

							long ramMB = Math.min(ClientMod.maxRam, (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()));
							int cpuCount = Math.max(1, ClientMod.vbox.getHostProcessorCount() / pc_case.getCpuDividedBy());

							ClientMod.vbox.modifyVmWith3dAccelFallback("VmComputersVm",
									"--memory", String.valueOf(ramMB),
									"--cpus", String.valueOf(cpuCount),
									"--vram", String.valueOf(ClientMod.videoMem),
									"--accelerate3d", "on");
							ClientMod.vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");

							ClientMod.vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

							if(!pc_case.getHardDriveFileName().isEmpty()) {
								if(new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
									String hddPath = new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", hddPath);
								}
							}
							if(!pc_case.getIsoFileName().isEmpty()) {
								if(pc_case.getIsoFileName().equals("Additions")) {
									String additionsPath = ClientMod.vbox.getDefaultAdditionsISO();
									if(additionsPath != null) {
										ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", additionsPath);
									}
								}else if(new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
									String isoPath = new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).getPath();
									ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", isoPath);
								}
							}
							if(pc_case.getIsoFileName().isEmpty()) {
								ClientMod.vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
							}
						}


						ClientMod.vbox.startVm("VmComputersVm");

						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = true;
						synchronized (ClientMod.VM_TURNING_ON_LOCK) {
							ClientMod.VM_TURNING_ON_LOCK.notifyAll();
						}
					}catch(Exception ex) {
						ex.printStackTrace();
						minecraft.player.sendSystemMessage(Component.translatable("mcvmcomputers.failed_to_start", ex.getMessage()).withStyle(ChatFormatting.RED));
						minecraft.player.sendSystemMessage(Component.translatable("mcvmcomputers.contact_me").withStyle(ChatFormatting.RED));
						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = false;

						FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
						PacketList.sendToServer("c2s_turn_off_pc", b);
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

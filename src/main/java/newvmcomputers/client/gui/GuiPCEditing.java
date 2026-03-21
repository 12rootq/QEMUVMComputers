package newvmcomputers.client.gui;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;
import net.minecraft.util.math.RotationAxis;

public class GuiPCEditing extends Screen {
	private float introScale;
	private float panelX;
	private final EntityPC pc_case;
	private boolean openCase;
	private final MinecraftClient minecraft;

	private final Language lang = Language.getInstance();

	private static final ItemStack CASE_NO_PANEL = new ItemStack(ItemList.PC_CASE_NO_PANEL);
	private static final ItemStack CASE_ONLY_PANEL = new ItemStack(ItemList.PC_CASE_ONLY_PANEL);
	private static final ItemStack CASE_ONLY_GLASS_PANEL = new ItemStack(ItemList.PC_CASE_GLASS_PANEL);
	private static final ItemStack MOBO = new ItemStack(ItemList.ITEM_MOTHERBOARD);
	private static final ItemStack CPU = new ItemStack(ItemList.ITEM_CPU2);
	private static final ItemStack GPU = new ItemStack(ItemList.ITEM_GPU);
	private static final ItemStack RAM = new ItemStack(ItemList.ITEM_RAM1G);
	private static final ItemStack HARD_DRIVE = new ItemStack(ItemList.ITEM_HARDDRIVE);
	private final Object vmTurningON = new Object();

	public GuiPCEditing(EntityPC pc_case) {
		super(Text.translatable("text.pc_editor.title"));
		this.pc_case = pc_case;
		this.minecraft = MinecraftClient.getInstance();
	}

	public void renderBackgroundAndMobo(DrawContext context) {
		context.fillGradient(0, 0, this.width, this.height, new Color(0f,0f,0f,Math.max(0.5f*introScale,0)).getRGB(), new Color(0f,0f,0f,0.5f*introScale).getRGB());

		MatrixStack ms = context.getMatrices();
		ms.push();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		ms.translate((this.width / 2f), (this.height / 2f)-40, 100.0F);
		ms.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90f*introScale));
		ms.multiply(new Quaternionf().rotationAxis((float) Math.toRadians(6f * introScale), 0f, 0.70710678f, 0.70710678f));
		ms.scale(1.0F, -1.0F, 1.0F);
		ms.scale(introScale, introScale, introScale);
		ms.scale(230.0F, 230.0F, 230.0F);

		renderItem(context, CASE_NO_PANEL);

		if(pc_case.getMotherboardInstalled()) {
			ms.push();
			ms.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(90f));
			ms.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90f));
			ms.translate(0.04f, 0.2f, -0.16f);
			ms.scale(0.55f, 0.55f, 0.55f);
			renderItem(context, MOBO);
			ms.pop();
		}
		if(pc_case.getCpuDividedBy() > 0) {
			ms.push();
			ms.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(90f));
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.translate(0.23f, 0.48f, 0.13f);
			renderItem(context, CPU);
			ms.pop();
		}
		if(pc_case.getGpuInstalled()) {
			ms.push();
			ms.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(90f));
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90f));
			ms.translate(0.33f, 0.5f, -0.565f);
			renderItem(context, GPU);
			ms.pop();
		}
		if(pc_case.getGigsOfRamInSlot0() > 0) {
			ms.push();
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(90f));
			ms.translate(0.07f, 0.5f, -0.203f);
			renderItem(context, RAM);
			ms.pop();
		}
		if(pc_case.getGigsOfRamInSlot1() > 0) {
			ms.push();
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(90f));
			ms.translate(0.07f, 0.5f, -0.33f);
			renderItem(context, RAM);
			ms.pop();
		}
		if(!pc_case.getHardDriveFileName().isEmpty()) {
			ms.push();
			ms.scale(0.55f, 0.55f, 0.55f);
			ms.translate(0.1f, -0.3f, -0.5f);
			renderItem(context, HARD_DRIVE);
			ms.pop();
		}
		ms.push();
		ms.translate(0, -panelX, 0);
		if(pc_case.getGlassSidepanel()) {
			renderItem(context, CASE_ONLY_GLASS_PANEL);
		}else{
			renderItem(context, CASE_ONLY_PANEL);
		}
		ms.pop();
		ms.pop();
	}

	private void renderItem(DrawContext context, ItemStack stack) {
		BakedModel model = minecraft.getItemRenderer().getModel(stack, null, null, 0);
		VertexConsumerProvider.Immediate immediate = minecraft.getBufferBuilders().getEntityVertexConsumers();
		boolean isNotSideLit = !model.isSideLit();
		if (isNotSideLit) {
			DiffuseLighting.disableGuiDepthLighting();
		}

		this.minecraft.getItemRenderer().renderItem(stack, ModelTransformationMode.NONE, false, context.getMatrices(), immediate, 15728640, OverlayTexture.DEFAULT_UV, model);
		immediate.draw();

		if (isNotSideLit) {
			DiffuseLighting.enableGuiDepthLighting();
		}
	}

	private void addMotherboard(boolean sixtyFour) {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeBoolean(sixtyFour);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_MOBO, b);
	}

	private void removeMotherboard() {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_MOBO, b);
	}

	private void addCPU(Item cpuItem, int dividedBy) {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeInt(dividedBy);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_CPU, b);
	}

	private void addGPU() {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_GPU, b);
	}

	private void addHardDrive(String fileName) {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeString(fileName);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_HARD_DRIVE, b);
	}

	private void removeHardDrive() {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_HARD_DRIVE, b);
	}

	private void addRamStick(Item ramItem, int megs) {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeInt(megs);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_RAM, b);
	}

	private void removeRamStick(int slot) {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeInt(slot);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_RAM, b);
	}

	private void removeCPU() {
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_REMOVE_CPU, b);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if(introScale > 0.92f && openCase)
			panelX = MVCUtils.lerp(panelX, 8, delta/20f);
		if(!openCase) {
			panelX = MVCUtils.lerp(panelX, 0, delta/1.5f);
		}
		introScale = MVCUtils.lerp(introScale, 1f, delta/4f);
		this.renderBackgroundAndMobo(context);
		this.clearChildren();

		MatrixStack ms = context.getMatrices();

		if(introScale > 0.99f) {
			if(openCase) {
				if((ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
					openCase = false;
				}
				if(SystemUtils.IS_OS_MAC) {
					context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.put_panel_back_mac"), 4, 14, -1, false);
					if(minecraft.getWindow() != null && GLFW.glfwGetKey(minecraft.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_ALT) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}else {
					context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.put_panel_back"), 4, 14, -1, false);
					if(minecraft.getWindow() != null && GLFW.glfwGetKey(minecraft.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
						openCase = false;
						panelX = -panelX;
					}
				}
				if(pc_case.getMotherboardInstalled()) {
					this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeMotherboard())
							.dimensions(this.width/2-70, this.height / 2 - 70, 10, 10).build());

					ms.push();
					ms.translate(0, 0, 200);
					if(pc_case.get64Bit()) {
						context.drawText(this.textRenderer, lang.get("newvmcomputers.64bit"), this.width/2 - 66, this.height/2 - 56, -1, false);
					}else {
						context.drawText(this.textRenderer, lang.get("newvmcomputers.32bit"), this.width/2 - 66, this.height/2 - 56, -1, false);
					}
					ms.pop();

					if(pc_case.getCpuDividedBy() == 0) {
						ms.push();
						ms.translate(0, 0, 200);
						context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.add_cpu"), this.width/2 - 120, this.height/2 - 40, -1, false);
						ms.pop();

						int addCpuWidth = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6"));
						ButtonWidget div2 = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "2")), (btn) -> this.addCPU(ItemList.ITEM_CPU2, 2))
								.dimensions(this.width/2 - (addCpuWidth+59), this.height / 2 - 31, addCpuWidth+4, 12).build();
						ButtonWidget div4 = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "4")), (btn) -> this.addCPU(ItemList.ITEM_CPU4, 4))
								.dimensions(this.width/2 - (addCpuWidth+59), this.height / 2 - 18, addCpuWidth+4, 12).build();
						ButtonWidget div6 = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_cpu_btn").replace("%s", "6")), (btn) -> this.addCPU(ItemList.ITEM_CPU6, 6))
								.dimensions(this.width/2 - (addCpuWidth+59), this.height / 2 - 5, addCpuWidth+4, 12).build();

						if(minecraft.player != null) {
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU2))) div2.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU4))) div4.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_CPU6))) div6.active = false;
						}

						this.addDrawableChild(div2);
						this.addDrawableChild(div4);
						this.addDrawableChild(div6);
					}else {
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeCPU())
								.dimensions(this.width/2-43, this.height / 2 - 16, 10, 10).build());
						ms.push();
						ms.translate(0, 0, 200);
						context.drawText(this.textRenderer, "1/" + pc_case.getCpuDividedBy(), this.width/2-25, this.height/2+2, -1, false);
						ms.pop();
					}
					if(!pc_case.getGpuInstalled()) {
						int addGpuWidth = this.textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.add_gpu"));
						ButtonWidget bw = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_gpu")), (btn) -> this.addGPU())
								.dimensions(this.width/2 - 64, this.height / 2 + 33, addGpuWidth+4, 12).build();
						if(minecraft.player != null && !minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_GPU)))
							bw.active = false;
						this.addDrawableChild(bw);
					}
					if(pc_case.getHardDriveFileName().isEmpty()) {
						int lastYOffset = 0;
						int xOffCount = 0;
						int lastXOffset = 0;
						int count = 0;
						ms.push();
						ms.translate(0, 0, 200);
						context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.add_vhds"), this.width/2 + 20, this.height/2 + 30, -1, false);
						ms.pop();

						if (minecraft.player != null) {
							for(ItemStack is : minecraft.player.getInventory().main) {
								if(is.getItem() instanceof ItemHarddrive) {
									if(is.hasNbt() && is.getNbt().contains("vhdfile")) {
										String file = is.getNbt().getString("vhdfile");
										if(new File(ClientMod.vhdDirectory, file).exists()) {
											int w = Math.max(50, this.textRenderer.getWidth(file)+4);
											this.addDrawableChild(ButtonWidget.builder(Text.literal(file), (btn) -> this.addHardDrive(file))
													.dimensions(this.width/2 + 20 + lastXOffset, this.height / 2 + 40 + lastYOffset, w, 12).build());
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
							ms.push();
							ms.translate(0, 0, 200);
							context.drawText(this.textRenderer, (char) (0xfeff00a7) + "7" + lang.get("newvmcomputers.pc_editing.no_valid_vhd"), this.width/2 + 20, this.height/2 + 40, -1, false);
							ms.pop();
						}
					}else {
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeHardDrive())
								.dimensions(this.width/2+30, this.height / 2 + 55, 10, 10).build());
						ms.push();
						ms.translate(0, 0, 200);
						context.drawText(this.textRenderer, pc_case.getHardDriveFileName(), this.width/2+45, this.height/2+65, -1, false);
						ms.pop();
					}
					if(pc_case.getGigsOfRamInSlot0() == 0 || pc_case.getGigsOfRamInSlot1() == 0) {
						ms.push();
						ms.translate(0, 0, 200);
						context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.add_ram"), this.width/2 + 50, this.height/2 - 60, -1, false);
						ms.pop();

						int addMBRamWidth = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512"))+4;
						ButtonWidget sixfourM = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "64")), (btn) -> this.addRamStick(ItemList.ITEM_RAM64M, 64))
								.dimensions(this.width/2 + 50, this.height / 2 - 64, addMBRamWidth, 12).build();
						ButtonWidget oneM = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "128")), (btn) -> this.addRamStick(ItemList.ITEM_RAM128M, 128))
								.dimensions(this.width/2 + 50, this.height / 2 - 51, addMBRamWidth, 12).build();
						ButtonWidget twoM = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "256")), (btn) -> this.addRamStick(ItemList.ITEM_RAM256M, 256))
								.dimensions(this.width/2 + 50, this.height / 2 - 38, addMBRamWidth, 12).build();
						ButtonWidget fiveM = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_mbram_btn").replace("%s", "512")), (btn) -> this.addRamStick(ItemList.ITEM_RAM512M, 512))
								.dimensions(this.width/2 + 50, this.height / 2 - 25, addMBRamWidth, 12).build();
						ButtonWidget oneG = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_ram_btn").replace("%s", "1")), (btn) -> this.addRamStick(ItemList.ITEM_RAM1G, 1024))
								.dimensions(this.width/2 + 50, this.height / 2 - 12, addMBRamWidth, 12).build();
						ButtonWidget twoG = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_ram_btn").replace("%s", "2")), (btn) -> this.addRamStick(ItemList.ITEM_RAM2G, 2048))
								.dimensions(this.width/2 + 50, this.height / 2 + 1, addMBRamWidth, 12).build();
						ButtonWidget fourG = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_ram_btn").replace("%s", "4")), (btn) -> this.addRamStick(ItemList.ITEM_RAM4G, 4096))
								.dimensions(this.width/2 + 50, this.height / 2 + 14, addMBRamWidth, 12).build();

						if(minecraft.player != null) {
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM64M))) sixfourM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM128M))) oneM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM256M))) twoM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM512M))) fiveM.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM1G))) oneG.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM2G))) twoG.active = false;
							if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_RAM4G))) fourG.active = false;
						}
						this.addDrawableChild(sixfourM);
						this.addDrawableChild(oneM);
						this.addDrawableChild(twoM);
						this.addDrawableChild(fiveM);
						this.addDrawableChild(oneG);
						this.addDrawableChild(twoG);
						this.addDrawableChild(fourG);
					}
					if(pc_case.getGigsOfRamInSlot0() > 0) {
						ms.push();
						ms.translate(0, 0, 200);
						if(pc_case.getGigsOfRamInSlot0() < 1000 && pc_case.getGigsOfRamInSlot0() >= 100) {
							context.drawText(this.textRenderer, pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+4, this.height/2+2, -1, false);
						}else if(pc_case.getGigsOfRamInSlot0() < 100) {
							context.drawText(this.textRenderer, pc_case.getGigsOfRamInSlot0() + " MB", this.width/2+10, this.height/2+2, -1, false);
						}else {
							context.drawText(this.textRenderer, (pc_case.getGigsOfRamInSlot0()/1024) + " GB", this.width / 2 + 16, this.height / 2 + 2, -1, false);
						}
						ms.pop();
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeRamStick(0))
								.dimensions(this.width/2+21, this.height / 2 - 70, 10, 10).build());
					}
					if(pc_case.getGigsOfRamInSlot1() > 0) {
						ms.push();
						ms.translate(0, 0, 200);
						if(pc_case.getGigsOfRamInSlot1() < 1000) {
							context.drawText(this.textRenderer, pc_case.getGigsOfRamInSlot1() + " MB", this.width/2+42, this.height/2+2, -1, false);
						}else {
							context.drawText(this.textRenderer, (pc_case.getGigsOfRamInSlot1()/1024) + " GB", this.width / 2+42, this.height / 2+2, -1, false);
						}
						ms.pop();
						this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (btn) -> this.removeRamStick(1))
								.dimensions(this.width/2 + 37, this.height / 2 - 70, 10, 10).build());
					}
				}else {
					int thirtyTwoWidth = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.add_32bit_mobo"))+4;
					ButtonWidget thirtytwo = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_32bit_mobo")), (btn) -> this.addMotherboard(false))
							.dimensions(this.width/2 - (thirtyTwoWidth/2), this.height / 2 - 23, thirtyTwoWidth, 14).build();
					int sixtyfourw = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.add_64bit_mobo"))+4;
					ButtonWidget sixtyfour = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.add_64bit_mobo")), (btn) -> this.addMotherboard(true))
							.dimensions(this.width/2 - (sixtyfourw/2), this.height / 2 - 7, sixtyfourw, 14).build();

					if(minecraft.player != null) {
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD))) thirtytwo.active = false;
						if(!minecraft.player.getInventory().contains(new ItemStack(ItemList.ITEM_MOTHERBOARD64))) sixtyfour.active = false;
					}

					this.addDrawableChild(thirtytwo);
					this.addDrawableChild(sixtyfour);
				}
			}else {
				boolean turnedOn = (ClientMod.vmTurningOn && ClientMod.vmEntityID == pc_case.getId()) || (ClientMod.vmTurnedOn && ClientMod.vmEntityID == pc_case.getId());
				if(turnedOn) {
					int buttonW = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.turn_off"))+4;
					this.addDrawableChild(ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.turn_off")), this::turnOffPC)
							.dimensions((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}else {
					int buttonW = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.turn_on"))+4;
					this.addDrawableChild(ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.turn_on")), this::turnOnPC)
							.dimensions((this.width/2 + 103) - buttonW, this.height / 2 - 80, buttonW, 12).build());
				}
				if (ClientMod.useVmware) {
					String text3D = ClientMod.useVmware3D ? "3D Acceleration: ON" : "3D Acceleration: OFF";
					int btn3DW = textRenderer.getWidth(text3D) + 8;
					this.addDrawableChild(ButtonWidget.builder(Text.literal(text3D), (btn) -> {
						ClientMod.useVmware3D = !ClientMod.useVmware3D;
					}).dimensions((this.width/2 + 103) - btn3DW, this.height / 2 - 95, btn3DW, 12).build());
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
					ms.push();
					ms.translate(0, 0, 200);
					context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.select_iso"), this.width/2 - 75, this.height/2 - 75, -1, false);
					ms.pop();

					int offX = 0;
					int offY = 0;
					File[] isoFiles = ClientMod.isoDirectory.listFiles();
					if (isoFiles != null) {
						for(File f : isoFiles) {
							if(f.getName().endsWith(".iso") || f.getName().endsWith(".ISO")) {
								if((this.width/2 - 75 + offX) + this.textRenderer.getWidth(f.getName())+10 > this.width/2 + 105) {
									offX = 0;
									offY += 14;
								}
								this.addDrawableChild(ButtonWidget.builder(Text.literal(f.getName()), (btn) -> insertISO(f.getName()))
										.dimensions(this.width/2 - 75 + offX, this.height / 2 - 62 + offY, this.textRenderer.getWidth(f.getName())+8, 12).build());
								offX += this.textRenderer.getWidth(f.getName())+10;
							}
						}
					}
				}else {
					ms.push();
					ms.translate(0, 0, 200);
					context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.inserted_iso"), this.width/2 - 75, this.height/2 - 75, -1, false);
					context.drawText(this.textRenderer, (char) (0xfeff00a7) + "7" + pc_case.getIsoFileName(), this.width/2 - 75, this.height/2 - 65, -1, false);
					ms.pop();
					int ejectW = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.eject"));
					this.addDrawableChild(ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.eject")), (btn) -> removeISO())
							.dimensions(this.width/2 - 75, this.height / 2 - 50, ejectW+4, 12).build());
				}
				int openCaseW = textRenderer.getWidth(lang.get("newvmcomputers.pc_editing.open_case"));
				ButtonWidget bw = ButtonWidget.builder(Text.literal(lang.get("newvmcomputers.pc_editing.open_case")), (btn) -> openCase = true)
						.dimensions(this.width/2 - 82, this.height / 2 + 65, openCaseW+4, 12).build();
				bw.active = !turnedOn;
				this.addDrawableChild(bw);
			}
		}

		ms.push();
		ms.translate(0, 0, 200);
		super.render(context, mouseX, mouseY, delta);
		context.drawText(this.textRenderer, lang.get("newvmcomputers.pc_editing.close"), 4, 4, -1, false);
		ms.pop();
	}

	private void removeISO() {
		if(!ClientMod.useVmware && (ClientMod.vmTurningOn || ClientMod.vmTurnedOn) && ClientMod.vmEntityID == pc_case.getId()) {
			try {
				ClientMod.vmSession.getMachine().unmountMedium("IDE Controller", 1, 0, true);
			}catch(VBoxException ex) { /* Ignored */ }
		}
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
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
					minecraft.player.sendMessage(Text.translatable("newvmcomputers.waitingforvmtostart").formatted(Formatting.YELLOW), false);

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
				minecraft.player.sendMessage(Text.literal("Note: On-the-fly ISO disk replacement is not yet supported in VMware. Restart your PC.").formatted(Formatting.YELLOW), false);
			}
		}

		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
		b.writeString(name);
		b.writeInt(this.pc_case.getId());
		ClientPlayNetworking.send(PacketList.C2S_ADD_ISO, b);
	}

	public void turnOffPC(ButtonWidget widget) {
		ClientMod.vmTurningOff = true;
		ClientMod.vmTurnedOn = false;
		PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
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
				IProgress ip = ClientMod.vmSession.getConsole().powerDown();
				ip.waitForCompletion(-1);

				try {
					ClientMod.vmSession.unlockMachine();
				} catch (VBoxException e) { /* Ignored */ }

				ClientMod.vmSession = null;
				ClientMod.vmTurnedOn = false;
				ClientMod.vmTurningOff = false;
				ClientMod.vmEntityID = -1;
			}
		}, "Turn off PC").start();
	}

	public void turnOnPC(ButtonWidget widget) {
		if(pc_case.getCpuDividedBy() > 0 && pc_case.getGpuInstalled() && pc_case.getMotherboardInstalled() && (pc_case.getGigsOfRamInSlot0() + pc_case.getGigsOfRamInSlot1()) >= 1) {
			if(!pc_case.getHardDriveFileName().isEmpty()) {
				if(!new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName()).exists()) {
					if(minecraft.player != null)
						minecraft.player.sendMessage(Text.translatable("newvmcomputers.hdd_doesnt_exist").formatted(Formatting.RED), false);
					return;
				}
			}
			if(!pc_case.getIsoFileName().isEmpty()) {
				if(!new File(ClientMod.isoDirectory, pc_case.getIsoFileName()).exists()) {
					if(minecraft.player != null)
						minecraft.player.sendMessage(Text.translatable("newvmcomputers.iso_doesnt_exist").formatted(Formatting.RED), false);
					return;
				}
			}

			if(ClientMod.vmTurningOn || ClientMod.vmTurnedOn) {
				return;
			}
			ClientMod.vmTurningOn = true;
			ClientMod.vmEntityID = pc_case.getId();
			PacketByteBuf b = new PacketByteBuf(Unpooled.buffer());
			b.writeInt(pc_case.getId());
			ClientPlayNetworking.send(PacketList.C2S_TURN_ON_PC, b);

			new Thread(() -> {
				if (ClientMod.useVmware) {
					try {
						if (minecraft.player != null) {
							minecraft.player.sendMessage(Text.literal("Creating a .vmx file and running VMware...").formatted(Formatting.GOLD), false);
						}

						// 1. Создаем текстовый файл конфигурации .vmx
						File vmxFile = new File(ClientMod.vhdDirectory.getParentFile(), "vmware_vm.vmx");
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
							if(!pc_case.getHardDriveFileName().isEmpty()) {
								File hdd = new File(ClientMod.vhdDirectory, pc_case.getHardDriveFileName());
								fw.write("ide0:0.present = \"TRUE\"\n");
								fw.write("ide0:0.fileName = \"" + hdd.getAbsolutePath().replace("\\", "\\\\") + "\"\n");
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
						Process p = pb.start();
						p.waitFor();

						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = true;

					} catch (Exception ex) {
						if(minecraft.player != null) {
							minecraft.player.sendMessage(Text.translatable("newvmcomputers.failed_to_start", ex.getMessage()).formatted(Formatting.RED), false);
						}
						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = false;
						PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
						ClientPlayNetworking.send(PacketList.C2S_TURN_OFF_PC, buf);
					}

				}

				 else {
					ArrayList<ISession> usedSessions = new ArrayList<>();
					try {
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
							edit.setCPUCount(Math.min(1, ClientMod.vb.getHost().getProcessorCount() / pc_case.getCpuDividedBy()));
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
						ClientMod.vmTurnedOn = true;
						synchronized (vmTurningON) {
							vmTurningON.notify();
						}
					} catch(Exception ex) {
						for(ISession is : usedSessions) {
							try {
								is.unlockMachine();
							} catch(Exception exx) { /* Ignored */ }
						}
						if(minecraft.player != null) {
							minecraft.player.sendMessage(Text.translatable("newvmcomputers.failed_to_start", ex.getMessage()).formatted(Formatting.RED), false);
							minecraft.player.sendMessage(Text.translatable("newvmcomputers.contact_me").formatted(Formatting.RED), false);
						}
						ClientMod.vmTurningOn = false;
						ClientMod.vmTurnedOn = false;

						PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
						ClientPlayNetworking.send(PacketList.C2S_TURN_OFF_PC, buf);
					}
				}
			}, "Turn on PC").start();
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
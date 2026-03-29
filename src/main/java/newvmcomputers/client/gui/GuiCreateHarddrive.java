package newvmcomputers.client.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.virtualbox_6_1.AccessMode;
import org.virtualbox_6_1.DeviceType;
import org.virtualbox_6_1.IMedium;
import org.virtualbox_6_1.IProgress;
import org.virtualbox_6_1.MediumVariant;

import io.netty.buffer.Unpooled;
import newvmcomputers.client.ClientMod;
import newvmcomputers.networking.PacketList;
import newvmcomputers.utils.MVCUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;

public class GuiCreateHarddrive extends Screen {
	private EditBox hddSize;
	private String status;
	private State currentState = State.MENU;
	private final Language lang = Language.getInstance();
	private Ext extension = Ext.vdi;
	private static final char COLOR_CHAR = (char) (0xfeff00a7);
	private Button AA;
	private Button BB;
	private final Minecraft minecraft = Minecraft.getInstance();

	public enum State{
		MENU,
		CREATE_NEW,
		SELECT_OLD
	}

	public enum Ext{
		vdi,
		vmdk
	}

	public GuiCreateHarddrive() {
		super(Component.translatable("Create Harddrive"));
	}

	public String translation(String in) {
		return lang.getOrDefault(in).replace("%c", ""+MVCUtils.COLOR_CHAR);
	}

	@Override
	public void init() {
		if(currentState == State.CREATE_NEW) {
			String s = "";
			if(hddSize != null) {
				s = hddSize.getValue();
			}
			hddSize = new EditBox(this.font, this.width/2-150, this.height/2-10, 300, 20, Component.empty());
			hddSize.setValue(s);
			hddSize.setResponder(this::hddSizeUpdate);
			this.addRenderableWidget(hddSize);
			this.hddSizeUpdate(hddSize.getValue());

			AA = this.addRenderableWidget(Button.builder(Component.literal("vdi"), (btn) -> extset(Ext.vdi))
					.bounds(this.width/2-150, this.height/2+25, 50, 20).build());

			BB = this.addRenderableWidget(Button.builder(Component.literal("vmdk"), (btn) -> extset(Ext.vmdk))
					.bounds(this.width/2-96, this.height/2+25, 50, 20).build());
			if (ClientMod.useVmware) {
				extset(Ext.vmdk);
			} else {
				extset(Ext.vdi);
			}

			int newvhdWidth = font.width(translation("newvmcomputers.vhd_setup.newvhd"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("newvmcomputers.vhd_setup.newvhd")), (btn) -> createNew())
					.bounds(this.width/2-(newvhdWidth/2), this.height/2+50, newvhdWidth, 20).build());

			int menuWidth = font.width(translation("newvmcomputers.vhd_setup.menu"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("newvmcomputers.vhd_setup.menu")), (btn) -> switchState(State.MENU))
					.bounds(this.width - (menuWidth+10), this.height - 30, menuWidth, 20).build());
		}else if(currentState == State.MENU) {
			int newvhdWidth = font.width(translation("newvmcomputers.vhd_setup.newvhd"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("newvmcomputers.vhd_setup.newvhd")), (btn) -> switchState(State.CREATE_NEW))
					.bounds(this.width/2 - (newvhdWidth/2), this.height/2 - 12, newvhdWidth, 20).build());

			int oldvhdWidth = font.width(translation("newvmcomputers.vhd_setup.oldvhd"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("newvmcomputers.vhd_setup.oldvhd")), (btn) -> switchState(State.SELECT_OLD))
					.bounds(this.width/2 - (oldvhdWidth/2), this.height/2 + 12, oldvhdWidth, 20).build());
		}else {
			int lastY = 60;
			ArrayList<File> files = new ArrayList<>();

			File[] dirFiles = ClientMod.vhdDirectory.listFiles();
			if (dirFiles != null) {
				for(File f : dirFiles) {
					if(f.getName().endsWith(".vdi") || f.getName().endsWith(".vmdk")) {
						files.add(f);
					}
				}
			}

			files.sort(Comparator.comparing(File::getName));

			for(File f : files) {
				this.addRenderableWidget(Button.builder(Component.literal((f.getName() + " | " + ((float)f.length()/1024f/1024f) + " " + translation("newvmcomputers.vhd_setup.mb_used"))), this::selectOld)
						.bounds(this.width/2 - 90, lastY, 180, 14).build());

				this.addRenderableWidget(Button.builder(Component.literal("x"), (btn) -> removevhd(f.getName()))
						.bounds(this.width/2 + 92, lastY, 14, 14).build());
				lastY += 16;
			}

			int menuWidth = font.width(translation("newvmcomputers.vhd_setup.menu"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("newvmcomputers.vhd_setup.menu")), (btn) -> switchState(State.MENU))
					.bounds(this.width - (menuWidth+10), this.height - 30, menuWidth, 20).build());
		}
	}

	private void extset(Ext ext){
		if (ClientMod.useVmware) {
			extension = Ext.vmdk;
			if(AA != null) AA.active = false;
			if(BB != null) BB.active = false;
			return;
		}

		extension=ext;
		if (extension==Ext.vdi){
			if(AA != null) AA.active=false;
			if(BB != null) BB.active=true;
		}else if (extension==Ext.vmdk){
			if(BB != null) BB.active=false;
			if(AA != null) AA.active=true;
		}
	}

	private void switchState(State newState) {
		this.clearWidgets();
		currentState = newState;
		this.init();
	}

	private void selectOld(Button wdgt) {
		String fileName = wdgt.getMessage().getString().split(Pattern.quote(" | "))[0];
		FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
		pb.writeUtf(fileName);
		ClientPlayNetworking.send(PacketList.C2S_CHANGE_HDD, pb);
		minecraft.setScreen(null);
	}

	private void createNew() {
		if(status != null && !status.startsWith(COLOR_CHAR + "c")) {
			long sizeMB = Long.parseLong(hddSize.getValue());
			long sizeBytes = sizeMB * 1024L * 1024L;
			int i = ClientMod.latestVHDNum;
			File vhd = new File(ClientMod.vhdDirectory, "vhd" + i + "."+extension);

			if (ClientMod.useVmware) {
				try {
					String vdiskManager = ClientMod.vmwareDirectory + File.separator + (SystemUtils.IS_OS_WINDOWS ? "vmware-vdiskmanager.exe" : "vmware-vdiskmanager");

					if (minecraft.player != null) {
						minecraft.player.displayClientMessage(Component.literal("Creating VMDK disk... Please wait.").withStyle(ChatFormatting.YELLOW), false);
					}
					ProcessBuilder pb = new ProcessBuilder(
							vdiskManager,
							"-c",
							"-t", "0",
							"-s", sizeMB + "MB",
							"-a", "ide",
							vhd.getAbsolutePath()
					);
					Process p = pb.start();
					p.waitFor();

					if (!vhd.exists()) {
						System.err.println("VMware-vdiskmanager failed to create file.");
						if (minecraft.player != null) {
							minecraft.player.displayClientMessage(Component.literal("Error: vdiskmanager could not create the disk. It may not be in the VMware folder.").withStyle(ChatFormatting.RED), false);
						}
						return;
					}
				} catch (Exception e) {
					System.err.println("Failed to execute vmware-vdiskmanager: " + e.getMessage());
					if (minecraft.player != null) {
						minecraft.player.displayClientMessage(Component.literal("vdiskmanager execution error: " + e.getMessage()).withStyle(ChatFormatting.RED), false);
					}
					return;
				}
			} else {
				IMedium hdd = null;

				if(extension == Ext.vdi){
					hdd = ClientMod.vb.createMedium("vdi", vhd.getPath(), AccessMode.ReadWrite, DeviceType.HardDisk);
				}else if(extension == Ext.vmdk){
					hdd = ClientMod.vb.createMedium("vmdk", vhd.getPath(), AccessMode.ReadWrite, DeviceType.HardDisk);
				}

				if (hdd != null) {
					IProgress pr = hdd.createBaseStorage(sizeBytes, Collections.singletonList(MediumVariant.Standard));
					pr.waitForCompletion(-1);
				} else {
					System.err.println("Failed to create virtual hard drive medium.");
					return;
				}
			}

			try {
				ClientMod.increaseVHDNum();
			} catch (IOException e) {
				System.err.println("Error increasing VHD Num: " + e.getMessage());
			}

			FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
			pb.writeUtf(vhd.getName());
			ClientPlayNetworking.send(PacketList.C2S_CHANGE_HDD, pb);
			minecraft.setScreen(null);
		}
	}

	private void removevhd(String name) {
		try {
			Files.deleteIfExists(new File(ClientMod.vhdDirectory, name).toPath());
		} catch (IOException e) {
			System.err.println("Failed to delete VHD file: " + e.getMessage());
		}
		minecraft.setScreen(null);
	}

	private void hddSizeUpdate(String in) {
		if(NumberUtils.isDigits(in)) {
			long i;
			try {
				i = Long.parseLong(in);
			} catch(NumberFormatException e) {
				status = translation("newvmcomputers.input_parser_error");
				return;
			}

			if(i > 0) {
				if(i*1024*1024 < 0) {
					status = translation("newvmcomputers.input_too_much").replace("%s", ""+Long.MAX_VALUE/1024L/1024L);
				} else if(i*1024*1024 >= ClientMod.vhdDirectory.getFreeSpace()) {
					status = translation("newvmcomputers.vhd_setup.space");
				} else {
					status = translation("newvmcomputers.vhd_setup.validspace");
				}
			} else {
				status = translation("newvmcomputers.input_too_little").replace("%s", "1");
			}
		} else {
			status = translation("newvmcomputers.input_nan");
		}
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		if(currentState == State.CREATE_NEW) {
			context.drawString(this.font, status, this.width/2-150, this.height/2+13, -1, false);
			context.drawString(this.font, translation("newvmcomputers.vhd_setup.vhdsize"), this.width/2-150, this.height/2-20, -1, false);
			if (this.hddSize != null) {
				this.hddSize.render(context, mouseX, mouseY, delta);
			}
		} else if(currentState == State.MENU) {
			String s = translation("newvmcomputers.vhd_setup.setupnewvhd");
			context.drawString(this.font, s, this.width/2 - this.font.width(s)/2, this.height/2 - 30, -1, false);
		}
		super.render(context, mouseX, mouseY, delta);
	}
}


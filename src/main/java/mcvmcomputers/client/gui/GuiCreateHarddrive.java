package mcvmcomputers.client.gui;
import net.minecraft.client.gui.screens.Screen;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.lang3.math.NumberUtils;

import io.netty.buffer.Unpooled;
import mcvmcomputers.client.ClientMod;
import mcvmcomputers.networking.PacketList;
import mcvmcomputers.utils.MVCUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;



import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

public class GuiCreateHarddrive extends net.minecraft.client.gui.screens.Screen{
	@Override
	public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
	}
	private EditBox hddSize;
	private String status;
	private State currentState = State.MENU;
	private final Language lang = Language.getInstance();
	private Ext extension = Ext.vdi;
	private static final char COLOR_CHAR = (char) (0xfeff00a7);
	private Button AA;
	private Button BB;
	private Minecraft minecraft = Minecraft.getInstance();

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
			hddSize = new EditBox(this.font, this.width/2-150, this.height/2-10, 300, 20, Component.literal(""));
			hddSize.setValue(s);
			hddSize.setResponder((st) -> hddSizeUpdate(st));
			this.addWidget(hddSize);
			this.hddSizeUpdate(hddSize.getValue());
			AA = this.addRenderableWidget(Button.builder(Component.literal("vdi"), (wdgt) -> extset(Ext.vdi)).bounds(this.width/2-150, this.height/2+25, 50, 20).build());
			AA.active = false;
			BB = this.addRenderableWidget(Button.builder(Component.literal("vmdk"), (wdgt) -> extset(Ext.vmdk)).bounds(this.width/2-96, this.height/2+25, 50, 20).build());
			int newvhdWidth = font.width(translation("mcvmcomputers.vhd_setup.newvhd"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("mcvmcomputers.vhd_setup.newvhd")), (wdgt) -> createNew(wdgt)).bounds(this.width/2-(newvhdWidth/2), this.height/2+50, newvhdWidth, 20).build());
			int menuWidth = font.width(translation("mcvmcomputers.vhd_setup.menu"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("mcvmcomputers.vhd_setup.menu")), (wdgt) -> switchState(State.MENU)).bounds(this.width - (menuWidth+10), this.height - 30, menuWidth, 20).build());
		}else if(currentState == State.MENU) {
			int newvhdWidth = font.width(translation("mcvmcomputers.vhd_setup.newvhd"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("mcvmcomputers.vhd_setup.newvhd")), (wdgt) -> switchState(State.CREATE_NEW)).bounds(this.width/2 - (newvhdWidth/2), this.height/2 - 12, newvhdWidth, 20).build());
			int oldvhdWidth = font.width(translation("mcvmcomputers.vhd_setup.oldvhd"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("mcvmcomputers.vhd_setup.oldvhd")), (wdgt) -> switchState(State.SELECT_OLD)).bounds(this.width/2 - (oldvhdWidth/2), this.height/2 + 12, oldvhdWidth, 20).build());
		}else {
			int lastY = 60;
			ArrayList<File> files = new ArrayList<>();
			for(File f : ClientMod.vhdDirectory.listFiles()) {
				if(f.getName().endsWith(".vdi") || f.getName().endsWith(".vmdk")) {
					files.add(f);
				}
			}
			files.sort(new Comparator<File>() {
				@Override
				public int compare(File o1, File o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for(File f : files) {
				this.addRenderableWidget(Button.builder(Component.literal((f.getName() + " | " + ((float)f.length()/1024f/1024f) + " " + translation("mcvmcomputers.vhd_setup.mb_used"))), (wdgt) -> selectOld(wdgt)).bounds(this.width/2 - 90, lastY, 180, 14).build());
				this.addRenderableWidget(Button.builder(Component.literal("x"), (wdgt) -> removevhd(f.getName())).bounds(this.width/2 + 92, lastY, 14, 14).build());
				lastY += 16;
			}

			int menuWidth = font.width(translation("mcvmcomputers.vhd_setup.menu"))+40;
			this.addRenderableWidget(Button.builder(Component.literal(translation("mcvmcomputers.vhd_setup.menu")), (wdgt) -> switchState(State.MENU)).bounds(this.width - (menuWidth+10), this.height - 30, menuWidth, 20).build());
		}
	}

	private void extset(Ext ext){
		extension=ext;
		if (extension==Ext.vdi){
			AA.active=false;
			BB.active=true;
		}else if (extension==Ext.vmdk){
			BB.active=false;
			AA.active=true;
		}
		return;
	}

	private void switchState(State newState) {
		this.clearWidgets();
		currentState = newState;
		this.init();
	}

	private void selectOld(Button wdgt) {
		String buttonText = wdgt.getMessage().getString();
		int sepIdx = buttonText.indexOf(" | ");
		String fileName = sepIdx >= 0 ? buttonText.substring(0, sepIdx) : buttonText;
		FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
		pb.writeUtf(fileName);
		PacketList.sendToServer("c2s_change_hdd", pb);
		minecraft.setScreen(null);
	}

	private void createNew(Button wdgt) {
		if(!status.startsWith(COLOR_CHAR + "c")) {
			long sizeMB = Long.parseLong(hddSize.getValue());
			// Skip any numbers whose file already exists (e.g. leftover from a failed
			// previous attempt that VirtualBox already registered in its media library).
			int i = ClientMod.latestVHDNum;
			File vhd = new File(ClientMod.vhdDirectory, "vhd" + i + "." + extension);
			while (vhd.exists()) {
				i++;
				vhd = new File(ClientMod.vhdDirectory, "vhd" + i + "." + extension);
			}
			// Sync the in-memory counter so the next creation starts from the right number.
			ClientMod.latestVHDNum = i;
			try {
				ClientMod.vbox.createHardDisk(vhd.getPath(), sizeMB, extension.name());
			} catch (Exception e) {
				e.printStackTrace();
				status = COLOR_CHAR + "c" + translation("mcvmcomputers.failed_to_start").replace("%s", e.getMessage());
				return;
			}

			try {
				ClientMod.increaseVHDNum();
			} catch (IOException e) {
				e.printStackTrace();
			}

			FriendlyByteBuf pb = new FriendlyByteBuf(Unpooled.buffer());
			pb.writeUtf(vhd.getName());
PacketList.sendToServer("c2s_change_hdd", pb);
			minecraft.setScreen(null);
		}
	}

	private void removevhd(String name) {
		new File(ClientMod.vhdDirectory, name).delete();
		minecraft.setScreen(null);
	}

	private void hddSizeUpdate(String in) {
		if(NumberUtils.isDigits(in)) {
			long i = 0;
			try {
				i = Long.parseLong(in);
			}catch(NumberFormatException e) {
				status = translation("mcvmcomputers.input_parser_error");
				return;
			}
			if(i > 0) {
				if(i*1024*1024 < 0) {
					status = translation("mcvmcomputers.input_too_much").replace("%s", ""+Long.MAX_VALUE/1024L/1024L);
					return;
				}

				if(i*1024*1024 >= ClientMod.vhdDirectory.getFreeSpace()) {
					status = translation("mcvmcomputers.vhd_setup.space");
					return;
				}else {
					status = translation("mcvmcomputers.vhd_setup.validspace");
					return;
				}
			}else {
				status = translation("mcvmcomputers.input_too_little").replace("%s", "1");
				return;
			}
		}else {
			status = translation("mcvmcomputers.input_nan");
			return;
		}
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		if(currentState == State.CREATE_NEW) {
			context.drawString(this.font, status, this.width/2-150, this.height/2+13, -1);
			context.drawString(this.font, translation("mcvmcomputers.vhd_setup.vhdsize"), this.width/2-150, this.height/2-20, -1);
			this.hddSize.render(context, mouseX, mouseY, delta);
		}else if(currentState == State.MENU) {
			String s = translation("mcvmcomputers.vhd_setup.setupnewvhd");
			context.drawString(this.font, s, this.width/2 - this.font.width(s)/2, this.height/2 - 30, -1);
		}
		super.render(context, mouseX, mouseY, delta);
	}

}

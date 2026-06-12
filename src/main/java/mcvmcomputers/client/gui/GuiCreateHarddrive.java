package mcvmcomputers.client.gui;

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
import mcvmcomputers.networking.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

public class GuiCreateHarddrive extends Screen{
	private TextFieldWidget hddSize;
	private String status;
	private State currentState = State.MENU;
	private final Language lang = Language.getInstance();
	private Ext extension = Ext.vdi;
	private static final char COLOR_CHAR = (char) (0xfeff00a7);
	private ButtonWidget AA;
	private ButtonWidget BB;
	private MinecraftClient minecraft = MinecraftClient.getInstance();

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
		super(Text.translatable("Create Harddrive"));
	}

	public String translation(String in) {
		return lang.get(in).replace("%c", ""+MVCUtils.COLOR_CHAR);
	}

	@Override
	public void init() {
		if(currentState == State.CREATE_NEW) {
			String s = "";
			if(hddSize != null) {
				s = hddSize.getText();
			}
			hddSize = new TextFieldWidget(this.textRenderer, this.width/2-150, this.height/2-10, 300, 20, Text.literal(""));
			hddSize.setText(s);
			hddSize.setChangedListener((st) -> hddSizeUpdate(st));
			this.addSelectableChild(hddSize);
			this.hddSizeUpdate(hddSize.getText());
			AA = this.addDrawableChild(ButtonWidget.builder(Text.literal("vdi"), (wdgt) -> extset(Ext.vdi)).dimensions(this.width/2-150, this.height/2+25, 50, 20).build());
			AA.active = false;
			BB = this.addDrawableChild(ButtonWidget.builder(Text.literal("vmdk"), (wdgt) -> extset(Ext.vmdk)).dimensions(this.width/2-96, this.height/2+25, 50, 20).build());
			int newvhdWidth = textRenderer.getWidth(translation("mcvmcomputers.vhd_setup.newvhd"))+40;
			this.addDrawableChild(ButtonWidget.builder(Text.literal(translation("mcvmcomputers.vhd_setup.newvhd")), (wdgt) -> createNew(wdgt)).dimensions(this.width/2-(newvhdWidth/2), this.height/2+50, newvhdWidth, 20).build());
			int menuWidth = textRenderer.getWidth(translation("mcvmcomputers.vhd_setup.menu"))+40;
			this.addDrawableChild(ButtonWidget.builder(Text.literal(translation("mcvmcomputers.vhd_setup.menu")), (wdgt) -> switchState(State.MENU)).dimensions(this.width - (menuWidth+10), this.height - 30, menuWidth, 20).build());
		}else if(currentState == State.MENU) {
			int newvhdWidth = textRenderer.getWidth(translation("mcvmcomputers.vhd_setup.newvhd"))+40;
			this.addDrawableChild(ButtonWidget.builder(Text.literal(translation("mcvmcomputers.vhd_setup.newvhd")), (wdgt) -> switchState(State.CREATE_NEW)).dimensions(this.width/2 - (newvhdWidth/2), this.height/2 - 12, newvhdWidth, 20).build());
			int oldvhdWidth = textRenderer.getWidth(translation("mcvmcomputers.vhd_setup.oldvhd"))+40;
			this.addDrawableChild(ButtonWidget.builder(Text.literal(translation("mcvmcomputers.vhd_setup.oldvhd")), (wdgt) -> switchState(State.SELECT_OLD)).dimensions(this.width/2 - (oldvhdWidth/2), this.height/2 + 12, oldvhdWidth, 20).build());
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
				this.addDrawableChild(ButtonWidget.builder(Text.literal((f.getName() + " | " + ((float)f.length()/1024f/1024f) + " " + translation("mcvmcomputers.vhd_setup.mb_used"))), (wdgt) -> selectOld(wdgt)).dimensions(this.width/2 - 90, lastY, 180, 14).build());
				this.addDrawableChild(ButtonWidget.builder(Text.literal("x"), (wdgt) -> removevhd(f.getName())).dimensions(this.width/2 + 92, lastY, 14, 14).build());
				lastY += 16;
			}

			int menuWidth = textRenderer.getWidth(translation("mcvmcomputers.vhd_setup.menu"))+40;
			this.addDrawableChild(ButtonWidget.builder(Text.literal(translation("mcvmcomputers.vhd_setup.menu")), (wdgt) -> switchState(State.MENU)).dimensions(this.width - (menuWidth+10), this.height - 30, menuWidth, 20).build());
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
		this.clearChildren();
		currentState = newState;
		this.init();
	}

	private void selectOld(ButtonWidget wdgt) {
		String buttonText = wdgt.getMessage().getString();
		int sepIdx = buttonText.indexOf(" | ");
		String fileName = sepIdx >= 0 ? buttonText.substring(0, sepIdx) : buttonText;
		PacketByteBuf pb = new PacketByteBuf(Unpooled.buffer());
		pb.writeString(fileName);
		ClientPlayNetworking.send(PacketList.C2S_CHANGE_HDD, pb);
		minecraft.setScreen(null);
	}

	private void createNew(ButtonWidget wdgt) {
		if(!status.startsWith(COLOR_CHAR + "c")) {
			long sizeMB = Long.parseLong(hddSize.getText());
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

			PacketByteBuf pb = new PacketByteBuf(Unpooled.buffer());
			pb.writeString(vhd.getName());
			ClientPlayNetworking.send(PacketList.C2S_CHANGE_HDD, pb);
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
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		if(currentState == State.CREATE_NEW) {
			context.drawTextWithShadow(this.textRenderer, status, this.width/2-150, this.height/2+13, -1);
			context.drawTextWithShadow(this.textRenderer, translation("mcvmcomputers.vhd_setup.vhdsize"), this.width/2-150, this.height/2-20, -1);
			this.hddSize.render(context, mouseX, mouseY, delta);
		}else if(currentState == State.MENU) {
			String s = translation("mcvmcomputers.vhd_setup.setupnewvhd");
			context.drawTextWithShadow(this.textRenderer, s, this.width/2 - this.textRenderer.getWidth(s)/2, this.height/2 - 30, -1);
		}
		super.render(context, mouseX, mouseY, delta);
	}

}

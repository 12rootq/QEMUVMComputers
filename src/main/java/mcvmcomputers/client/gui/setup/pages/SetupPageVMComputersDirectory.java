package mcvmcomputers.client.gui.setup.pages;

import java.io.File;
import java.io.IOException;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;


import net.minecraft.network.chat.Component;

public class SetupPageVMComputersDirectory extends SetupPage{
	private EditBox vmComputersDirectory;
	private Button next;
	private String vboxStatus;

	public SetupPageVMComputersDirectory(GuiSetup setupGui, Font textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.vmcomputersdir"), setupGui.width/2-160, setupGui.height/2-20, -1);
		context.drawString(this.textRender, vboxStatus, setupGui.width/2-160, setupGui.height/2+13, -1);
		context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange0"), setupGui.width/2-160, 60, -1);
		context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange1"), setupGui.width/2-160, 70, -1);
		this.vmComputersDirectory.render(context, mouseX, mouseY, delta);
	}

	private void next(Button bw) {
		if(checkDirectory(vmComputersDirectory.getValue())) {
			File parent = new File(vmComputersDirectory.getValue());
			ClientMod.isoDirectory = new File(parent, "isos");
			ClientMod.vhdDirectory = new File(parent, "vhds");
			if(!ClientMod.isoDirectory.exists()) {
				ClientMod.isoDirectory.mkdir();
			}
			if(!ClientMod.vhdDirectory.exists()) {
				ClientMod.vhdDirectory.mkdir();
			}
			try {
				ClientMod.getVHDNum();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.setupGui.nextPage();
		}
	}

	private boolean checkDirectory(String s) {
		if(s.isEmpty()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_empty");
			next.active = false;
			return false;
		}
		File vboxDir = new File(s);
		if(!vboxDir.exists()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notfound");
			next.active = false;
			return false;
		}
		if(vboxDir.isFile()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notdir");
			next.active = false;
			return false;
		}
		vboxStatus = setupGui.translation("mcvmcomputers.input_dir_yes");
		next.active = true;
		return true;
	}

	@Override
	public void init() {
		int nextButtonW = textRender.width(setupGui.translation("mcvmcomputers.setup.nextButton"))+40;
		next = Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), (bw) -> this.next(bw)).bounds(setupGui.width/2 - (nextButtonW/2), setupGui.height - 40, nextButtonW, 20).build();
		String dirText = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
		if(vmComputersDirectory != null) {
			dirText = vmComputersDirectory.getValue();
		}
		this.checkDirectory(dirText);
		vmComputersDirectory = new EditBox(this.textRender, setupGui.width/2 - 160, setupGui.height/2 - 10, 320, 20, Component.literal(""));
		vmComputersDirectory.setMaxLength(35565);
		vmComputersDirectory.setValue(dirText);
		vmComputersDirectory.setResponder((s) -> checkDirectory(s));
		setupGui.addElement(vmComputersDirectory);
		setupGui.addButton(next);
	}

}

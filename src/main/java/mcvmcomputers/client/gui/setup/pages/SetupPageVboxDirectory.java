package mcvmcomputers.client.gui.setup.pages;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;

import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;


import net.minecraft.network.chat.Component;

public class SetupPageVboxDirectory extends SetupPage{
	private EditBox vboxDirectory;
	private Button next;
	private String vboxStatus;

	public SetupPageVboxDirectory(GuiSetup setupGui, Font textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.vbox_dir"), setupGui.width/2-160, setupGui.height/2-20, -1);
		context.drawString(this.textRender, vboxStatus, setupGui.width/2-160, setupGui.height/2+13, -1);
		context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange0"), setupGui.width/2-160, 60, -1);
		context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange1"), setupGui.width/2-160, 70, -1);
		this.vboxDirectory.render(context, mouseX, mouseY, delta);
	}

	private void next(Button bw) {
		if(checkDirectory(vboxDirectory.getValue())) {
			this.setupGui.virtualBoxDirectory = vboxDirectory.getValue();
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
			vboxStatus = setupGui.translation("mcvmcomputers.input_empty");
			next.active = false;
			return false;
		}else if(vboxDir.isFile()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notfound");
			next.active = false;
			return false;
		}else {

			if(SystemUtils.IS_OS_WINDOWS) {
				if(!new File(vboxDir, "VBoxManage.exe").exists()) {
					vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notvbox");
					next.active = false;
					return false;
				}
			}else if(SystemUtils.IS_OS_MAC) {
				if(!new File(vboxDir, "VBoxManage").exists()) {
					vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notvbox");
					next.active = false;
					return false;
				}
			}

		}
		vboxStatus = setupGui.translation("mcvmcomputers.input_dir_yesvbox");
		next.active = true;
		return true;
	}

	@Override
	public void init() {
		int nextButtonW = textRender.width(setupGui.translation("mcvmcomputers.setup.nextButton"))+40;
		next = Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), (bw) -> this.next(bw)).bounds(setupGui.width/2 - (nextButtonW/2), setupGui.height - 40, nextButtonW, 20).build();
		String dirText = this.setupGui.virtualBoxDirectory;
		if(vboxDirectory != null) {
			dirText = vboxDirectory.getValue();
		}
		this.checkDirectory(dirText);
		vboxDirectory = new EditBox(this.textRender, setupGui.width/2 - 160, setupGui.height/2 - 10, 320, 20, Component.literal(""));
		vboxDirectory.setMaxLength(35565);
		vboxDirectory.setValue(dirText);
		vboxDirectory.setResponder((s) -> checkDirectory(s));
		setupGui.addElement(vboxDirectory);
		setupGui.addButton(next);
	}

}

package mcvmcomputers.client.gui.setup.pages;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;

import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SetupPageVboxDirectory extends SetupPage {
	private TextFieldWidget vboxDirectory;
	private ButtonWidget next;
	private String vboxStatus;

	public SetupPageVboxDirectory(GuiSetup setupGui, TextRenderer textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.drawText(this.textRender, setupGui.translation("mcvmcomputers.setup.vbox_dir"), setupGui.width/2-160, setupGui.height/2-20, -1, false);
		context.drawText(this.textRender, vboxStatus, setupGui.width/2-160, setupGui.height/2+13, -1, false);
		context.drawText(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange0"), setupGui.width/2-160, 60, -1, false);
		context.drawText(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange1"), setupGui.width/2-160, 70, -1, false);
		this.vboxDirectory.render(context, mouseX, mouseY, delta);
	}

	private void next(ButtonWidget bw) {
		if(checkDirectory(vboxDirectory.getText())) {
			this.setupGui.virtualBoxDirectory = vboxDirectory.getText();
			this.setupGui.nextPage();
		}
	}

	private boolean checkDirectory(String s) {
		if(s.isEmpty()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_empty");
			if (next != null) next.active = false;
			return false;
		}
		File vboxDir = new File(s);
		if(!vboxDir.exists()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_empty");
			if (next != null) next.active = false;
			return false;
		} else if(vboxDir.isFile()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notfound");
			if (next != null) next.active = false;
			return false;
		} else {
			if(SystemUtils.IS_OS_WINDOWS) {
				if(!new File(vboxDir, "vboxmanage.exe").exists() || !new File(vboxDir, "vboxwebsrv.exe").exists()) {
					vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notvbox");
					if (next != null) next.active = false;
					return false;
				}
			} else if(SystemUtils.IS_OS_MAC) {
				if(!new File(vboxDir, "VBoxManage").exists() || !new File(vboxDir, "vboxwebsrv").exists()) {
					vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notvbox");
					if (next != null) next.active = false;
					return false;
				}
			}
		}
		vboxStatus = setupGui.translation("mcvmcomputers.input_dir_yesvbox");
		if (next != null) next.active = true;
		return true;
	}

	@Override
	public void init() {
		int nextButtonW = textRender.getWidth(setupGui.translation("mcvmcomputers.setup.nextButton"))+40;
		next = ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), this::next)
				.dimensions(setupGui.width/2 - (nextButtonW/2), setupGui.height - 40, nextButtonW, 20)
				.build();

		String dirText = this.setupGui.virtualBoxDirectory;
		if(vboxDirectory != null) {
			dirText = vboxDirectory.getText();
		}
		this.checkDirectory(dirText);

		vboxDirectory = new TextFieldWidget(this.textRender, setupGui.width/2 - 160, setupGui.height/2 - 10, 320, 20, Text.empty());
		vboxDirectory.setMaxLength(35565);
		vboxDirectory.setText(dirText);
		vboxDirectory.setChangedListener(this::checkDirectory);

		setupGui.addElement(vboxDirectory);
		setupGui.addButton(next);
	}
}
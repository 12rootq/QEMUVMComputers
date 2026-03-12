package newvmcomputers.client.gui.setup.pages;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;

import newvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SetupPageVboxDirectory extends SetupPage {
	private TextFieldWidget vboxDirectory;
	private TextFieldWidget vmwareDirectory;
	private CheckboxWidget useVmware;
	private ButtonWidget next;
	private String vboxStatus;
	private String vmwareStatus;

	public SetupPageVboxDirectory(GuiSetup setupGui, TextRenderer textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		boolean vmware = useVmware != null && useVmware.isChecked();

		if (vboxDirectory != null) vboxDirectory.setVisible(!vmware);
		if (vmwareDirectory != null) vmwareDirectory.setVisible(vmware);

		if(vmware) {
			checkVmwareDirectory(vmwareDirectory.getText());
			context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.vmware_dir"), setupGui.width/2-160, setupGui.height/2-20, -1, false);
			context.drawText(this.textRender, vmwareStatus, setupGui.width/2-160, setupGui.height/2+13, -1, false);
			this.vmwareDirectory.render(context, mouseX, mouseY, delta);
		}else {
			checkDirectory(vboxDirectory.getText());
			context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.vbox_dir"), setupGui.width/2-160, setupGui.height/2-20, -1, false);
			context.drawText(this.textRender, vboxStatus, setupGui.width/2-160, setupGui.height/2+13, -1, false);
			context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.dontchange0"), setupGui.width/2-160, 60, -1, false);
			context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.dontchange1"), setupGui.width/2-160, 70, -1, false);
			this.vboxDirectory.render(context, mouseX, mouseY, delta);
		}
	}

	private void next(ButtonWidget bw) {
		if (useVmware != null && useVmware.isChecked()) {
			if(checkVmwareDirectory(vmwareDirectory.getText())) {
				this.setupGui.useVmware = true;
				this.setupGui.vmwareDirectory = vmwareDirectory.getText();
				this.setupGui.nextPage();
			}
		} else {
			if(checkDirectory(vboxDirectory.getText())) {
				this.setupGui.useVmware = false;
				this.setupGui.virtualBoxDirectory = vboxDirectory.getText();
				this.setupGui.nextPage();
			}
		}
	}

	private boolean checkDirectory(String s) {
		if(s.isEmpty()) {
			vboxStatus = setupGui.translation("newvmcomputers.input_empty");
			if (next != null) next.active = false;
			return false;
		}
		File vboxDir = new File(s);
		if(!vboxDir.exists()) {
			vboxStatus = setupGui.translation("newvmcomputers.input_empty");
			if (next != null) next.active = false;
			return false;
		} else if(vboxDir.isFile()) {
			vboxStatus = setupGui.translation("newvmcomputers.input_dir_notfound");
			if (next != null) next.active = false;
			return false;
		} else {
			if(SystemUtils.IS_OS_WINDOWS) {
				if(!new File(vboxDir, "vboxmanage.exe").exists() || !new File(vboxDir, "vboxwebsrv.exe").exists()) {
					vboxStatus = setupGui.translation("newvmcomputers.input_dir_notvbox");
					if (next != null) next.active = false;
					return false;
				}
			} else if(SystemUtils.IS_OS_MAC) {
				if(!new File(vboxDir, "VBoxManage").exists() || !new File(vboxDir, "vboxwebsrv").exists()) {
					vboxStatus = setupGui.translation("newvmcomputers.input_dir_notvbox");
					if (next != null) next.active = false;
					return false;
				}
			}
		}
		vboxStatus = setupGui.translation("newvmcomputers.input_dir_yesvbox");
		if (next != null) next.active = true;
		return true;
	}

	private boolean checkVmwareDirectory(String s) {
		if(s.isEmpty()) {
			vmwareStatus = setupGui.translation("newvmcomputers.input_empty");
			if (next != null) next.active = false;
			return false;
		}
		File dir = new File(s);
		if(!dir.exists()) {
			vmwareStatus = setupGui.translation("newvmcomputers.input_dir_notfound");
			if (next != null) next.active = false;
			return false;
		}
		if(dir.isFile()) {
			vmwareStatus = setupGui.translation("newvmcomputers.input_dir_notdir");
			if (next != null) next.active = false;
			return false;
		}

		if(SystemUtils.IS_OS_WINDOWS) {
			if(!new File(dir, "vmrun.exe").exists()) {
				vmwareStatus = setupGui.translation("newvmcomputers.input_dir_notvmware");
				if (next != null) next.active = false;
				return false;
			}
		} else if(SystemUtils.IS_OS_MAC) {
			if(!new File(dir, "vmrun").exists()) {
				vmwareStatus = setupGui.translation("newvmcomputers.input_dir_notvmware");
				if (next != null) next.active = false;
				return false;
			}
		}
		vmwareStatus = setupGui.translation("newvmcomputers.input_dir_yesvmware");
		if (next != null) next.active = true;
		return true;
	}

	@Override
	public void init() {
		int nextButtonW = textRender.getWidth(setupGui.translation("newvmcomputers.setup.nextButton"))+40;
		next = ButtonWidget.builder(Text.literal(setupGui.translation("newvmcomputers.setup.nextButton")), this::next)
				.dimensions(setupGui.width/2 - (nextButtonW/2), setupGui.height - 40, nextButtonW, 20)
				.build();

		useVmware = new CheckboxWidget(
				setupGui.width/2 - 160, setupGui.height/2 - 55, 200, 20,
				Text.literal(setupGui.translation("newvmcomputers.setup.use_vmware")),
				setupGui.useVmware
		);

		String vboxText = this.setupGui.virtualBoxDirectory;
		if(vboxDirectory != null) {
			vboxText = vboxDirectory.getText();
		}

		String vmwareText = this.setupGui.vmwareDirectory;
		if(vmwareDirectory != null) {
			vmwareText = vmwareDirectory.getText();
		}

		vboxDirectory = new TextFieldWidget(this.textRender, setupGui.width/2 - 160, setupGui.height/2 - 10, 320, 20, Text.empty());
		vboxDirectory.setMaxLength(35565);
		vboxDirectory.setText(vboxText);
		vboxDirectory.setChangedListener(this::checkDirectory);

		vmwareDirectory = new TextFieldWidget(this.textRender, setupGui.width/2 - 160, setupGui.height/2 - 10, 320, 20, Text.empty());
		vmwareDirectory.setMaxLength(35565);
		vmwareDirectory.setText(vmwareText);
		vmwareDirectory.setChangedListener(this::checkVmwareDirectory);

		if(setupGui.useVmware) {
			checkVmwareDirectory(vmwareText);
		} else {
			checkDirectory(vboxText);
		}

		setupGui.addElement(useVmware);
		setupGui.addElement(vboxDirectory);
		setupGui.addElement(vmwareDirectory);
		setupGui.addButton(next);
	}
}
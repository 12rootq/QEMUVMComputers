package mcvmcomputers.client.gui.setup.pages;

import java.io.File;
import java.io.IOException;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SetupPageVMComputersDirectory extends SetupPage {
	private TextFieldWidget vmComputersDirectory;
	private ButtonWidget next;
	private String vboxStatus;

	public SetupPageVMComputersDirectory(GuiSetup setupGui, TextRenderer textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		context.drawText(this.textRender, setupGui.translation("mcvmcomputers.setup.vmcomputersdir"), setupGui.width/2-160, setupGui.height/2-20, -1, false);
		context.drawText(this.textRender, vboxStatus, setupGui.width/2-160, setupGui.height/2+13, -1, false);
		context.drawText(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange0"), setupGui.width/2-160, 60, -1, false);
		context.drawText(this.textRender, setupGui.translation("mcvmcomputers.setup.dontchange1"), setupGui.width/2-160, 70, -1, false);
		this.vmComputersDirectory.render(context, mouseX, mouseY, delta);
	}

	private void next(ButtonWidget bw) {
		if(checkDirectory(vmComputersDirectory.getText())) {
			File parent = new File(vmComputersDirectory.getText());
			ClientMod.isoDirectory = new File(parent, "isos");
			ClientMod.vhdDirectory = new File(parent, "vhds");

			if(!ClientMod.isoDirectory.exists()) {
				boolean created = ClientMod.isoDirectory.mkdirs();
				if (!created) System.err.println("Failed to create isos directory.");
			}
			if(!ClientMod.vhdDirectory.exists()) {
				boolean created = ClientMod.vhdDirectory.mkdirs();
				if (!created) System.err.println("Failed to create vhds directory.");
			}

			try {
				ClientMod.getVHDNum();
			} catch (NumberFormatException | IOException e) {
				System.err.println("Error getting VHD num: " + e.getMessage());
			}
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
			vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notfound");
			if (next != null) next.active = false;
			return false;
		}
		if(vboxDir.isFile()) {
			vboxStatus = setupGui.translation("mcvmcomputers.input_dir_notdir");
			if (next != null) next.active = false;
			return false;
		}
		vboxStatus = setupGui.translation("mcvmcomputers.input_dir_yes");
		if (next != null) next.active = true;
		return true;
	}

	@Override
	public void init() {
		int nextButtonW = textRender.getWidth(setupGui.translation("mcvmcomputers.setup.nextButton"))+40;
		next = ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), this::next)
				.dimensions(setupGui.width/2 - (nextButtonW/2), setupGui.height - 40, nextButtonW, 20)
				.build();

		String dirText = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
		if(vmComputersDirectory != null) {
			dirText = vmComputersDirectory.getText();
		}
		this.checkDirectory(dirText);

		vmComputersDirectory = new TextFieldWidget(this.textRender, setupGui.width/2 - 160, setupGui.height/2 - 10, 320, 20, Text.empty());
		vmComputersDirectory.setMaxLength(35565);
		vmComputersDirectory.setText(dirText);
		vmComputersDirectory.setChangedListener(this::checkDirectory);

		setupGui.addElement(vmComputersDirectory);
		setupGui.addButton(next);
	}
}
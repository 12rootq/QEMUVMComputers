package mcvmcomputers.client.gui.setup.pages;

import java.io.File;

import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;

import net.minecraft.network.chat.Component;

public class SetupPageIntroMessage extends SetupPage{
	public SetupPageIntroMessage(GuiSetup setupGui, Font textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		if(!setupGui.loadedConfiguration) {
			String text = setupGui.translation("mcvmcomputers.setup.intro_message");

			int offY = -36;

			for(String s : text.split("\n")) {
				context.drawString(this.textRender, s, setupGui.width/2 - this.textRender.width(s)/2, setupGui.height/2 + offY, -1);
				offY+=10;
			}
		}
	}

	@Override
	public void init() {
		if(!setupGui.loadedConfiguration) {
			int buttonW = textRender.width(setupGui.translation("mcvmcomputers.setup.nextButton"))+20;
			setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), (bw) -> this.setupGui.nextPage()).bounds(setupGui.width/2 - (buttonW/2), setupGui.height - 40, buttonW, 20).build());
		}else {
			int useConfigW = textRender.width(setupGui.translation("mcvmcomputers.setup.useConfig"))+20;
			int redoSetupW = textRender.width(setupGui.translation("mcvmcomputers.setup.redoSetup"))+20;
			int w = useConfigW;
			if(redoSetupW > useConfigW)
				w = redoSetupW;
			setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.useConfig")), (bw) -> this.setupGui.lastPage()).bounds(setupGui.width/2 - (w/2), setupGui.height / 2 - 25, w, 20).build());
			setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.redoSetup")), (bw) -> this.delete()).bounds(setupGui.width/2 - (w/2), setupGui.height / 2 + 5, w, 20).build());
		}
	}
	public void delete() {
		setupGui.loadedConfiguration = false;
		File f = new File(minecraft.gameDirectory, "vm_computers/setup.json");
		if(f.exists()) {
			f.delete();
		}
		this.setupGui.nextPage();
	}
}

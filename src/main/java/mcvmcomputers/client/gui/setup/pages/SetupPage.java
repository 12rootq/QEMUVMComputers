package mcvmcomputers.client.gui.setup.pages;

import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
public abstract class SetupPage {
	protected final GuiSetup setupGui;
	protected final Font textRender;
	protected final Minecraft minecraft;

	public SetupPage(GuiSetup setupGui, Font textRender) {
		this.setupGui = setupGui;
		this.textRender = textRender;
		this.minecraft = Minecraft.getInstance();
	}

	public abstract void render(GuiGraphics context, int mouseX, int mouseY, float delta);
	public abstract void init();
}

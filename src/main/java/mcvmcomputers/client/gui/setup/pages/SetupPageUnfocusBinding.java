package mcvmcomputers.client.gui.setup.pages;


import net.minecraft.network.chat.Component;

import static mcvmcomputers.client.ClientMod.*;

import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;

public class SetupPageUnfocusBinding extends SetupPage{
	public static boolean changeBinding;
	public static int bindingToBeChangedNum;
	public static boolean bindingJustChanged;

	public SetupPageUnfocusBinding(GuiSetup setupGui, Font textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		if(bindingJustChanged) {
			setupGui.init();
			bindingJustChanged = false;
		}
		String s = setupGui.translation("mcvmcomputers.setup.unfocusCombo");
		context.drawString(this.textRender, s, setupGui.width/2-this.textRender.width(s)/2, setupGui.height/2-20, -1);
	}

	private void changeBinding(int num) {
		changeBinding = true;
		bindingToBeChangedNum = num;
		bindingJustChanged = false;
		setupGui.init();
	}

	private void clearBinding(int num) {
		switch(num) {
		case 1:
			glfwUnfocusKey1 = -1;
			break;
		case 2:
			glfwUnfocusKey2 = -1;
			break;
		case 3:
			glfwUnfocusKey3 = -1;
			break;
		case 4:
			glfwUnfocusKey4 = -1;
			break;
		}
		setupGui.init();
	}

	@Override
	public void init() {
		if(changeBinding) {
			setupGui.addButton(Button.builder(Component.literal((bindingToBeChangedNum == 1 ? "> " : "") + getKeyName(glfwUnfocusKey1) + (bindingToBeChangedNum == 1 ? " <" : "")), (bw) -> changeBinding(1)).bounds(setupGui.width/2-130, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(Button.builder(Component.literal((bindingToBeChangedNum == 2 ? "> " : "") + getKeyName(glfwUnfocusKey2) + (bindingToBeChangedNum == 2 ? " <" : "")), (bw) -> changeBinding(2)).bounds(setupGui.width/2-64, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(Button.builder(Component.literal((bindingToBeChangedNum == 3 ? "> " : "") + getKeyName(glfwUnfocusKey3) + (bindingToBeChangedNum == 3 ? " <" : "")), (bw) -> changeBinding(3)).bounds(setupGui.width/2+3, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(Button.builder(Component.literal((bindingToBeChangedNum == 4 ? "> " : "") + getKeyName(glfwUnfocusKey4) + (bindingToBeChangedNum == 4 ? " <" : "")), (bw) -> changeBinding(4)).bounds(setupGui.width/2+70, setupGui.height/2-10, 60, 20).build());
		}else {
			setupGui.addButton(Button.builder(Component.literal(getKeyName(glfwUnfocusKey1)), (bw) -> changeBinding(1)).bounds(setupGui.width/2-130, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(Button.builder(Component.literal(getKeyName(glfwUnfocusKey2)), (bw) -> changeBinding(2)).bounds(setupGui.width/2-64, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(Button.builder(Component.literal(getKeyName(glfwUnfocusKey3)), (bw) -> changeBinding(3)).bounds(setupGui.width/2+3, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(Button.builder(Component.literal(getKeyName(glfwUnfocusKey4)), (bw) -> changeBinding(4)).bounds(setupGui.width/2+70, setupGui.height/2-10, 60, 20).build());
		}

		setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(1)).bounds(setupGui.width/2-130, setupGui.height/2+12, 60, 12).build());
		setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(2)).bounds(setupGui.width/2-64, setupGui.height/2+12, 60, 12).build());
		setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(3)).bounds(setupGui.width/2+3, setupGui.height/2+12, 60, 12).build());
		setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(4)).bounds(setupGui.width/2+70, setupGui.height/2+12, 60, 12).build());

		int nextButtonW = textRender.width(setupGui.translation("mcvmcomputers.setup.nextButton"))+40;
		setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), (bw) -> this.setupGui.nextPage()).bounds(setupGui.width/2 - (nextButtonW/2), setupGui.height - 40, nextButtonW, 20).build());
	}

}

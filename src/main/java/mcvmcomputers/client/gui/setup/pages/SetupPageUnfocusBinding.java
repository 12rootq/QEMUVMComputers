package mcvmcomputers.client.gui.setup.pages;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static mcvmcomputers.client.ClientMod.*;

import mcvmcomputers.client.gui.setup.GuiSetup;

public class SetupPageUnfocusBinding extends SetupPage {
	public static boolean changeBinding;
	public static int bindingToBeChangedNum;
	public static boolean bindingJustChanged;

	public SetupPageUnfocusBinding(GuiSetup setupGui, TextRenderer textRender) {
		super(setupGui, textRender);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if(bindingJustChanged) {
			setupGui.init();
			bindingJustChanged = false;
		}
		String s = setupGui.translation("mcvmcomputers.setup.unfocusCombo");
		context.drawText(this.textRender, s, setupGui.width/2-this.textRender.getWidth(s)/2, setupGui.height/2-20, -1, false);
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
			setupGui.addButton(ButtonWidget.builder(Text.literal((bindingToBeChangedNum == 1 ? "> " : "") + getKeyName(glfwUnfocusKey1) + (bindingToBeChangedNum == 1 ? " <" : "")), (bw) -> changeBinding(1))
					.dimensions(setupGui.width/2-130, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(ButtonWidget.builder(Text.literal((bindingToBeChangedNum == 2 ? "> " : "") + getKeyName(glfwUnfocusKey2) + (bindingToBeChangedNum == 2 ? " <" : "")), (bw) -> changeBinding(2))
					.dimensions(setupGui.width/2-64, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(ButtonWidget.builder(Text.literal((bindingToBeChangedNum == 3 ? "> " : "") + getKeyName(glfwUnfocusKey3) + (bindingToBeChangedNum == 3 ? " <" : "")), (bw) -> changeBinding(3))
					.dimensions(setupGui.width/2+3, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(ButtonWidget.builder(Text.literal((bindingToBeChangedNum == 4 ? "> " : "") + getKeyName(glfwUnfocusKey4) + (bindingToBeChangedNum == 4 ? " <" : "")), (bw) -> changeBinding(4))
					.dimensions(setupGui.width/2+70, setupGui.height/2-10, 60, 20).build());
		}else {
			setupGui.addButton(ButtonWidget.builder(Text.literal(getKeyName(glfwUnfocusKey1)), (bw) -> changeBinding(1))
					.dimensions(setupGui.width/2-130, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(ButtonWidget.builder(Text.literal(getKeyName(glfwUnfocusKey2)), (bw) -> changeBinding(2))
					.dimensions(setupGui.width/2-64, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(ButtonWidget.builder(Text.literal(getKeyName(glfwUnfocusKey3)), (bw) -> changeBinding(3))
					.dimensions(setupGui.width/2+3, setupGui.height/2-10, 60, 20).build());
			setupGui.addButton(ButtonWidget.builder(Text.literal(getKeyName(glfwUnfocusKey4)), (bw) -> changeBinding(4))
					.dimensions(setupGui.width/2+70, setupGui.height/2-10, 60, 20).build());
		}

		setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(1))
				.dimensions(setupGui.width/2-130, setupGui.height/2+12, 60, 12).build());
		setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(2))
				.dimensions(setupGui.width/2-64, setupGui.height/2+12, 60, 12).build());
		setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(3))
				.dimensions(setupGui.width/2+3, setupGui.height/2+12, 60, 12).build());
		setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.clearButton")), (bw) -> clearBinding(4))
				.dimensions(setupGui.width/2+70, setupGui.height/2+12, 60, 12).build());

		int nextButtonW = textRender.getWidth(setupGui.translation("mcvmcomputers.setup.nextButton"))+40;
		setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), (bw) -> this.setupGui.nextPage())
				.dimensions(setupGui.width/2 - (nextButtonW/2), setupGui.height - 40, nextButtonW, 20).build());
	}
}
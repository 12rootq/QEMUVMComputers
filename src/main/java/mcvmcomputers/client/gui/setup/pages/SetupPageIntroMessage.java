package mcvmcomputers.client.gui.setup.pages;

import java.io.File;
import java.nio.file.Files;

import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext; // Новый импорт для 1.20.1
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text; // Новый импорт текста

public class SetupPageIntroMessage extends SetupPage {
	public SetupPageIntroMessage(GuiSetup setupGui, TextRenderer textRender) {
		super(setupGui, textRender);
	}

	// Внимание: MatrixStack ms заменен на DrawContext context
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if(!setupGui.loadedConfiguration) {
			String text = setupGui.translation("mcvmcomputers.setup.intro_message");

			int offY = -36;

			for(String s : text.split("\n")) {
				// Отрисовка текста теперь делается через context.drawText
				context.drawText(this.textRender, s, setupGui.width/2 - this.textRender.getWidth(s)/2, setupGui.height/2 + offY, -1, false);
				offY+=10;
			}
		}
	}

	@Override
	public void init() {
		if(!setupGui.loadedConfiguration) {
			int buttonW = textRender.getWidth(setupGui.translation("mcvmcomputers.setup.nextButton"))+20;

			// В 1.20.1 кнопки создаются через билдер
			setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.nextButton")), (bw) -> this.setupGui.nextPage())
					.dimensions(setupGui.width/2 - (buttonW/2), setupGui.height - 40, buttonW, 20)
					.build());
		}else {
			int useConfigW = textRender.getWidth(setupGui.translation("mcvmcomputers.setup.useConfig"))+20;
			int redoSetupW = textRender.getWidth(setupGui.translation("mcvmcomputers.setup.redoSetup"))+20;

			// Оптимизация через Math.max
			int w = Math.max(useConfigW, redoSetupW);

			setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.useConfig")), (bw) -> this.setupGui.lastPage())
					.dimensions(setupGui.width/2 - (w/2), setupGui.height / 2 - 25, w, 20)
					.build());

			setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.redoSetup")), (bw) -> this.delete())
					.dimensions(setupGui.width/2 - (w/2), setupGui.height / 2 + 5, w, 20)
					.build());
		}
	}

	public void delete() {
		setupGui.loadedConfiguration = false;
		File f = new File(minecraft.runDirectory, "vm_computers/setup.json");
		// Безопасное удаление файла (избавляет от предупреждений Java)
		try {
			Files.deleteIfExists(f.toPath());
		} catch (Exception ignored) {
		}
		this.setupGui.nextPage();
	}
}
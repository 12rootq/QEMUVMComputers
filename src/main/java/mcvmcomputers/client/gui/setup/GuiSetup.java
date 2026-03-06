package mcvmcomputers.client.gui.setup;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import com.google.gson.Gson;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.client.gui.setup.pages.SetupPage;
import mcvmcomputers.client.gui.setup.pages.SetupPageIntroMessage;
import mcvmcomputers.client.gui.setup.pages.SetupPageMaxValues;
import mcvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding;
import mcvmcomputers.client.gui.setup.pages.SetupPageVMComputersDirectory;
import mcvmcomputers.client.gui.setup.pages.SetupPageVboxDirectory;
import mcvmcomputers.client.utils.VMSettings;
import mcvmcomputers.utils.MVCUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

public class GuiSetup extends Screen {
	private List<SetupPage> setupPages;
	private int setupIndex;
	private SetupPage currentSetupPage;
	private boolean initialized = false;
	public boolean loadedConfiguration = false;
	public boolean startVb = false;
	public String virtualBoxDirectory = "";
	private Language language = Language.getInstance();
	private final MinecraftClient minecraft = MinecraftClient.getInstance();

	public GuiSetup() {
		super(Text.literal("Setup"));
	}

	// В 1.20.1 элементы добавляются через addDrawableChild.
	// Меняем тип Element на ClickableWidget (к которому относятся текстовые поля и т.д.)
	public void addElement(ClickableWidget e) {
		this.addDrawableChild(e);
	}

	public void clearElements() {
		this.clearChildren();
	}

	public void clearButtons() {
		this.clearChildren();
	}

	public void addButton(ButtonWidget bw) {
		this.addDrawableChild(bw);
	}

	public void nextPage() {
		if(setupIndex < setupPages.size()) {
			setupIndex++;
			currentSetupPage = setupPages.get(setupIndex);
			this.init();
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	public String translation(String in) {
		return language.get(in).replace("%c", ""+MVCUtils.COLOR_CHAR);
	}

	public void lastPage() {
		startVb = true;
		setupIndex = setupPages.size() - 1;
		currentSetupPage = setupPages.get(setupPages.size() - 1);
		this.init();
	}

	public void firstPage() {
		this.clearButtons();
		this.clearElements();
		setupIndex = 0;
		currentSetupPage = setupPages.get(0);
		this.init();
	}

	@Override
	public void init() {
		language = Language.getInstance();
		File setupFile = new File(minecraft.runDirectory, "vm_computers/setup.json");

		if(setupFile.exists()) {
			VMSettings set = null;
			try (FileReader fr = new FileReader(setupFile)) {
				set = new Gson().fromJson(fr, VMSettings.class);
			} catch (Exception e) {
				System.err.println("Failed to load setup.json: " + e.getMessage());
			}

			// Проверка на null для предотвращения NullPointerException
			if (set != null) {
				if(set.vboxDirectory != null) {
					virtualBoxDirectory = set.vboxDirectory;
				} else {
					virtualBoxDirectory = new VMSettings().vboxDirectory;
				}

				if(set.vmComputersDirectory != null) {
					ClientMod.isoDirectory = new File(set.vmComputersDirectory, "isos");
					ClientMod.vhdDirectory = new File(set.vmComputersDirectory, "vhds");
				}

				ClientMod.glfwUnfocusKey1 = set.unfocusKey1;
				ClientMod.glfwUnfocusKey2 = set.unfocusKey2;
				ClientMod.glfwUnfocusKey3 = set.unfocusKey3;
				ClientMod.glfwUnfocusKey4 = set.unfocusKey4;
				ClientMod.maxRam = set.maxRam;
				ClientMod.videoMem = set.videoMem;

				loadedConfiguration = set.vmComputersDirectory != null && !set.vmComputersDirectory.contains(" ");
			} else {
				virtualBoxDirectory = new VMSettings().vboxDirectory;
			}
		} else {
			virtualBoxDirectory = new VMSettings().vboxDirectory;
		}

		if(!initialized) {
			setupPages = new ArrayList<>();
			setupPages.add(new SetupPageIntroMessage(this, this.textRenderer));
			if(SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
				setupPages.add(new SetupPageVboxDirectory(this, this.textRenderer));
			}
			setupPages.add(new SetupPageVMComputersDirectory(this, this.textRenderer));
			setupPages.add(new SetupPageUnfocusBinding(this, this.textRenderer));
			setupPages.add(new SetupPageMaxValues(this, this.textRenderer));
			currentSetupPage = setupPages.get(0);
			initialized = true;
		}

		this.clearButtons();
		this.clearElements();
		currentSetupPage.init();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context); // Обновлено под 1.20.1
		String title = translation("mcvmcomputers.setup.title");
		context.drawText(this.textRenderer, title, this.width/2 - this.textRenderer.getWidth(title)/2, 20, -1, false);

		String s = translation("mcvmcomputers.setup.page").replaceFirst("%s", ""+(setupIndex+1)).replaceFirst("%s", ""+setupPages.size());
		context.drawText(this.textRenderer, s, this.width/2 - this.textRenderer.getWidth(s)/2, 30, -1, false);

		currentSetupPage.render(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
	}
}
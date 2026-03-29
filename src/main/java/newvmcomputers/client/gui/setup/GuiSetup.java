package newvmcomputers.client.gui.setup;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import com.google.gson.Gson;

import newvmcomputers.client.ClientMod;
import newvmcomputers.client.gui.setup.pages.SetupPage;
import newvmcomputers.client.gui.setup.pages.SetupPageIntroMessage;
import newvmcomputers.client.gui.setup.pages.SetupPageMaxValues;
import newvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding;
import newvmcomputers.client.gui.setup.pages.SetupPageVMComputersDirectory;
import newvmcomputers.client.gui.setup.pages.SetupPageVboxDirectory;
import newvmcomputers.client.utils.VMSettings;
import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

public class GuiSetup extends Screen {
	private List<SetupPage> setupPages;
	private int setupIndex;
	private SetupPage currentSetupPage;
	private boolean initialized = false;
	public boolean loadedConfiguration = false;
	public boolean startVb = false;
	public String virtualBoxDirectory = "";
	public boolean useVmware = false;
	public String vmwareDirectory = "";
	private Language language = Language.getInstance();
	private final Minecraft minecraft = Minecraft.getInstance();

	public GuiSetup() {
		super(Component.literal("Setup"));
	}

	public void addElement(AbstractWidget e) {
		this.addRenderableWidget(e);
	}

	public void clearElements() {
		this.clearWidgets();
	}

	public void clearButtons() {
		this.clearWidgets();
	}

	public void addButton(Button bw) {
		this.addRenderableWidget(bw);
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
		return language.getOrDefault(in).replace("%c", ""+MVCUtils.COLOR_CHAR);
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

		if(!initialized) {
			File setupFile = new File(minecraft.gameDirectory, "vm_computers/setup.json");

			if(setupFile.exists()) {
				VMSettings set = null;
				try (FileReader fr = new FileReader(setupFile)) {
					set = new Gson().fromJson(fr, VMSettings.class);
				} catch (Exception e) {
					System.err.println("Failed to load setup.json: " + e.getMessage());
				}

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

					useVmware = set.useVmware;
					vmwareDirectory = set.vmwareDirectory == null ? "" : set.vmwareDirectory;

					ClientMod.glfwUnfocusKey1 = set.unfocusKey1;
					ClientMod.glfwUnfocusKey2 = set.unfocusKey2;
					ClientMod.glfwUnfocusKey3 = set.unfocusKey3;
					ClientMod.glfwUnfocusKey4 = set.unfocusKey4;
					ClientMod.maxRam = set.maxRam;
					ClientMod.videoMem = set.videoMem;

					loadedConfiguration = set.vmComputersDirectory != null && !set.vmComputersDirectory.isEmpty();
				} else {
					virtualBoxDirectory = new VMSettings().vboxDirectory;
				}
			} else {
				virtualBoxDirectory = new VMSettings().vboxDirectory;
			}

			setupPages = new ArrayList<>();
			setupPages.add(new SetupPageIntroMessage(this, this.font));
			if(SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_MAC) {
				setupPages.add(new SetupPageVboxDirectory(this, this.font));
			}
			setupPages.add(new SetupPageVMComputersDirectory(this, this.font));
			setupPages.add(new SetupPageUnfocusBinding(this, this.font));
			setupPages.add(new SetupPageMaxValues(this, this.font));
			currentSetupPage = setupPages.get(0);
			initialized = true;
		}

		this.clearButtons();
		this.clearElements();
		if (currentSetupPage != null) {
			currentSetupPage.init();
		}
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		String title = translation("newvmcomputers.setup.title");
		context.drawString(this.font, title, this.width/2 - this.font.width(title)/2, 20, -1, false);

		String s = translation("newvmcomputers.setup.page").replaceFirst("%s", ""+(setupIndex+1)).replaceFirst("%s", ""+setupPages.size());
		context.drawString(this.font, s, this.width/2 - this.font.width(s)/2, 30, -1, false);

		currentSetupPage.render(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (this.currentSetupPage instanceof newvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding) {
			if (newvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding.changeBinding) {
				switch (newvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding.bindingToBeChangedNum) {
					case 1: ClientMod.glfwUnfocusKey1 = keyCode; break;
					case 2: ClientMod.glfwUnfocusKey2 = keyCode; break;
					case 3: ClientMod.glfwUnfocusKey3 = keyCode; break;
					case 4: ClientMod.glfwUnfocusKey4 = keyCode; break;
				}

				newvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding.changeBinding = false;
				newvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding.bindingJustChanged = true;

				return true;
			}
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}


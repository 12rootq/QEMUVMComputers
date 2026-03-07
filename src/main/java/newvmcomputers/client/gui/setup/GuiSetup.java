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

		
		if(!initialized) {
			File setupFile = new File(minecraft.runDirectory, "vm_computers/setup.json");

			if(setupFile.exists()) {
				VMSettings set = null;
				try (java.io.FileReader fr = new java.io.FileReader(setupFile)) {
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
		if (currentSetupPage != null) {
			currentSetupPage.init();
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context); 
		String title = translation("newvmcomputers.setup.title");
		context.drawText(this.textRenderer, title, this.width/2 - this.textRenderer.getWidth(title)/2, 20, -1, false);

		String s = translation("newvmcomputers.setup.page").replaceFirst("%s", ""+(setupIndex+1)).replaceFirst("%s", ""+setupPages.size());
		context.drawText(this.textRenderer, s, this.width/2 - this.textRenderer.getWidth(s)/2, 30, -1, false);

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
package mcvmcomputers.client.gui.setup;
import net.minecraft.client.gui.screens.Screen;


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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;

import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

public class GuiSetup extends net.minecraft.client.gui.screens.Screen{
	@Override
	public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
		context.fillGradient(0, 0, this.width, this.height, 0xff404040, 0xff404040);
	}

	private List<SetupPage> setupPages;
	private int setupIndex;
	private SetupPage currentSetupPage;
	private boolean initialized = false;
	public boolean loadedConfiguration = false;
	public boolean startVb = false;
	public String virtualBoxDirectory = "";
	private Language language = Language.getInstance();
	private Minecraft minecraft = Minecraft.getInstance();

	public GuiSetup() {
		super(Component.literal("Setup"));
	}

	public <T extends AbstractWidget> void addElement(T e) {
		this.addRenderableWidget(e);
	}

	public void clearElements() {
		for(GuiEventListener e : new java.util.ArrayList<>(this.children())) {
			this.removeWidget(e);
		}
	}

	public void clearButtons() {
		for(GuiEventListener e : new java.util.ArrayList<>(this.children())) {
			this.removeWidget(e);
		}
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
		for(GuiEventListener e : new java.util.ArrayList<>(this.children())) {
			this.removeWidget(e);
		}
		setupIndex = 0;
		currentSetupPage = setupPages.get(0);
		this.init();
	}

	@Override
	public void init() {
		language = Language.getInstance();
		if(new File(minecraft.gameDirectory, "vm_computers/setup.json").exists()) {
			FileReader fr;
			VMSettings set = null;
			try {
				fr = new FileReader(new File(minecraft.gameDirectory, "vm_computers/setup.json"));
				set = new Gson().fromJson(fr, VMSettings.class);
				fr.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			if(set.vboxDirectory != null) {
				virtualBoxDirectory = set.vboxDirectory;
			}else {
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
			loadedConfiguration = !set.vmComputersDirectory.contains(" ");
		}else{
			virtualBoxDirectory = new VMSettings().vboxDirectory;
		}
		if(!initialized) {
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
		for(GuiEventListener e : new java.util.ArrayList<>(this.children())) {
			this.removeWidget(e);
		}
		currentSetupPage.init();
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context, mouseX, mouseY, delta);
		String title = translation("mcvmcomputers.setup.title");
		context.drawString(this.font, title, this.width/2 - this.font.width(title)/2, 20, -1);
		String s = translation("mcvmcomputers.setup.page").replaceFirst("%s", ""+(setupIndex+1)).replaceFirst("%s", ""+setupPages.size());
		context.drawString(this.font, s, this.width/2 - this.font.width(s)/2, 30, -1);
		currentSetupPage.render(context, mouseX, mouseY, delta);
		super.render(context, mouseX, mouseY, delta);
	}
}

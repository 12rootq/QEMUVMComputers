package newvmcomputers.client.gui.setup.pages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.virtualbox_6_1.IVirtualBox;
import org.virtualbox_6_1.VirtualBoxManager;

import com.google.gson.Gson;

import newvmcomputers.client.ClientMod;
import newvmcomputers.client.gui.setup.GuiSetup;
import newvmcomputers.client.utils.VMSettings;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class SetupPageMaxValues extends SetupPage{
	private String statusMaxRam;
	private String statusVideoMemory;
	private TextFieldWidget maxRam;
	private TextFieldWidget videoMemory;
	private String status;
	private boolean onlyStatusMessage = false;

	public SetupPageMaxValues(GuiSetup setupGui, TextRenderer textRender) {
		super(setupGui, textRender);
	}

	private boolean checkMaxRam(String input) {
		if(input.isEmpty()) {
			statusMaxRam = setupGui.translation("newvmcomputers.input_empty");
			return false;
		}
		if(!StringUtils.isNumeric(input)) {
			statusMaxRam = setupGui.translation("newvmcomputers.input_nan");
			return false;
		}
		int rm = Integer.parseInt(input);
		if(rm < 16) {
			statusMaxRam = setupGui.translation("newvmcomputers.input_too_little").replace("%s", "16");
			return false;
		}
		statusMaxRam = setupGui.translation("newvmcomputers.input_valid");
		return true;
	}

	private boolean videoMemory(String input) {
		if(input.isEmpty()) {
			statusVideoMemory = setupGui.translation("newvmcomputers.input_empty");
			return false;
		}
		if(!StringUtils.isNumeric(input)) {
			statusVideoMemory = setupGui.translation("newvmcomputers.input_nan");
			return false;
		}
		int nm = Integer.parseInt(input);
		if(nm > 256) {
			statusVideoMemory = setupGui.translation("newvmcomputers.input_too_much").replace("%s", "256");
			return false;
		}
		statusVideoMemory = setupGui.translation("newvmcomputers.input_valid");
		return true;
	}

	private void confirmButton(ButtonWidget in) {
		// --- Начало новой логики для VMware ---
		if (setupGui.useVmware) {
			status = setupGui.translation("newvmcomputers.setup.startingStatus");
			onlyStatusMessage = true;

			// ---> ЖЕСТКО ЗАПИСЫВАЕМ В ПАМЯТЬ ИГРЫ ВЫБОР VMWARE <---
			ClientMod.useVmware = true;
			ClientMod.vmwareDirectory = setupGui.vmwareDirectory;
			ClientMod.maxRam = 8192; // VMware пока не требует этого в настройках, ставим дефолт
			ClientMod.videoMem = 256;

			// Сохраняем настройки перед выходом
			VMSettings set = new VMSettings();
			set.vboxDirectory = setupGui.virtualBoxDirectory;
			set.vmComputersDirectory = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
			set.unfocusKey1 = ClientMod.glfwUnfocusKey1;
			set.unfocusKey2 = ClientMod.glfwUnfocusKey2;
			set.unfocusKey3 = ClientMod.glfwUnfocusKey3;
			set.unfocusKey4 = ClientMod.glfwUnfocusKey4;
			set.maxRam = ClientMod.maxRam;
			set.videoMem = ClientMod.videoMem;

			// Самое важное для сохранения в JSON
			set.useVmware = true;
			set.vmwareDirectory = setupGui.vmwareDirectory;

			File f = new File(minecraft.runDirectory, "vm_computers/setup.json");
			try {
				Files.deleteIfExists(f.toPath());
				f.createNewFile();
				try (FileWriter fw = new FileWriter(f)) {
					fw.append(new Gson().toJson(set));
					fw.flush();
				}
				System.out.println("Setup saved successfully for VMware.");
			} catch (Exception e) {
				System.err.println("Failed to save setup.json: " + e.getMessage());
			}

			// Завершаем настройку
			minecraft.execute(() -> minecraft.setScreen(new TitleScreen(false)));
			return;
		}
		// --- Конец новой логики для VMware ---


		// ----- Старая логика для VirtualBox -----
		if(ClientMod.vboxWebSrv != null) {
			ClientMod.vboxWebSrv.destroy();
		}

		if(SystemUtils.IS_OS_WINDOWS) {
			ProcessBuilder vboxConfig = new ProcessBuilder(this.setupGui.virtualBoxDirectory + "\\vboxmanage.exe", "setproperty", "websrvauthlibrary", "null");
			try {
				vboxConfig.start();
			} catch (IOException e1) {
				System.err.println("Failed to start vboxConfig: " + e1.getMessage());
			}

			ProcessBuilder vboxWebSrv = new ProcessBuilder(this.setupGui.virtualBoxDirectory + "\\vboxwebsrv.exe", "--timeout", "0");
			try {
				ClientMod.vboxWebSrv = vboxWebSrv.start();
			} catch (IOException e1) {
				System.err.println("Failed to start vboxWebSrv: " + e1.getMessage());
			}
		}else if(SystemUtils.IS_OS_MAC){
			ProcessBuilder vboxConfig = new ProcessBuilder(this.setupGui.virtualBoxDirectory + "/VBoxManage", "setproperty", "websrvauthlibrary", "null");
			try {
				vboxConfig.start();
			} catch (IOException e1) {
				System.err.println("Failed to start vboxConfig: " + e1.getMessage());
			}

			ProcessBuilder vboxWebSrv = new ProcessBuilder(this.setupGui.virtualBoxDirectory + "/vboxwebsrv", "--timeout", "0");
			try {
				ClientMod.vboxWebSrv = vboxWebSrv.start();
			} catch (IOException e1) {
				System.err.println("Failed to start vboxWebSrv: " + e1.getMessage());
			}
		}else { // Linux
			ProcessBuilder vboxConfig = new ProcessBuilder("vboxmanage", "setproperty", "websrvauthlibrary", "null");
			try {
				vboxConfig.start();
			} catch (IOException e1) {
				System.err.println("Failed to start vboxConfig: " + e1.getMessage());
			}

			ProcessBuilder vboxWebSrv = new ProcessBuilder("vboxwebsrv", "--timeout", "0");
			try {
				ClientMod.vboxWebSrv = vboxWebSrv.start();
			} catch (IOException e1) {
				System.err.println("Failed to start vboxWebSrv: " + e1.getMessage());
			}
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (ClientMod.vboxWebSrv != null) {
				ClientMod.vboxWebSrv.destroy();
			}
		}));

		boolean[] bools = new boolean[] {checkMaxRam(maxRam.getText()), videoMemory(videoMemory.getText())};
		for(boolean b : bools) {
			if(!b) {
				return;
			}
		}

		this.setupGui.clearElements();
		this.setupGui.clearButtons();
		onlyStatusMessage = true;

		// Если пошли по пути VirtualBox, жестко записываем в память игры
		ClientMod.useVmware = false;
		ClientMod.maxRam = Integer.parseInt(maxRam.getText());
		ClientMod.videoMem = Integer.parseInt(videoMemory.getText());

		status = setupGui.translation("newvmcomputers.setup.startingStatus");
		new Thread(() -> {
			try {
				VirtualBoxManager vm = VirtualBoxManager.createInstance(null);
				vm.connect("http://localhost:18083", "should", "work");
				IVirtualBox vb = vm.getVBox();
				VMSettings set = new VMSettings();
				set.vboxDirectory = setupGui.virtualBoxDirectory;
				set.vmComputersDirectory = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
				set.unfocusKey1 = ClientMod.glfwUnfocusKey1;
				set.unfocusKey2 = ClientMod.glfwUnfocusKey2;
				set.unfocusKey3 = ClientMod.glfwUnfocusKey3;
				set.unfocusKey4 = ClientMod.glfwUnfocusKey4;
				set.maxRam = ClientMod.maxRam;
				set.videoMem = ClientMod.videoMem;

				// Сохраняем настройки VIrtualBox в JSON
				set.useVmware = false;
				set.vmwareDirectory = "";

				File f = new File(minecraft.runDirectory, "vm_computers/setup.json");

				Files.deleteIfExists(f.toPath());
				f.createNewFile();

				try (FileWriter fw = new FileWriter(f)) {
					fw.append(new Gson().toJson(set));
					fw.flush();
				}

				for(int i = 5;i>=0;i--) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						System.err.println("Sleep interrupted: " + e.getMessage());
					}
					status = setupGui.translation("newvmcomputers.setup.successStatus").replaceFirst("%s", vb.getVersion()).replaceFirst("%s", ""+i);
				}
				ClientMod.vbManager = vm;
				ClientMod.vb = vb;

				minecraft.execute(() -> minecraft.setScreen(new TitleScreen(false)));

			}catch(Exception ex) {
				System.err.println("Setup failed: " + ex.getMessage());
				for(int i = 5;i>=0;i--) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						System.err.println("Sleep interrupted: " + e.getMessage());
					}
					status = setupGui.translation("newvmcomputers.setup.failedStatus").replace("%s", ""+i);
				}
				onlyStatusMessage = false;

				minecraft.execute(() -> setupGui.firstPage());
			}
		}).start();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if(!onlyStatusMessage) {
			if (setupGui.useVmware) {
				// Если выбран VMware, скрываем настройки RAM
				context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.vmware_dir"), setupGui.width/2 - 160, setupGui.height/2 - 55, -1, false);
				context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.vmrun_help"), setupGui.width/2 - 160, setupGui.height/2 - 40, -1, false);
				context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.vmrun_note"), setupGui.width/2 - 160, setupGui.height/2 - 28, -1, false);
				context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.vmrun_note_detail"), setupGui.width/2 - 160, setupGui.height/2 - 16, -1, false);

				this.maxRam.visible = false;
				this.videoMemory.visible = false;
			} else {
				// VBox Настройки RAM
				context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.max_ram_input"), setupGui.width/2 - 160, setupGui.height/2-30, -1, false);
				context.drawText(this.textRender, setupGui.translation("newvmcomputers.setup.vram_input"), setupGui.width/2 + 10, setupGui.height/2-30, -1, false);
				String s = setupGui.translation("newvmcomputers.setup.ram_input_help");
				context.drawText(this.textRender, s, setupGui.width/2 - textRender.getWidth(s)/2, setupGui.height/2+30, -1, false);
				context.drawText(this.textRender, statusMaxRam, setupGui.width / 2 - 160, setupGui.height/2 + 3, -1, false);
				context.drawText(this.textRender, statusVideoMemory, setupGui.width / 2 + 10, setupGui.height/2 + 3, -1, false);

				this.maxRam.visible = true;
				this.videoMemory.visible = true;
			}

			// Рендерятся поля ввода, только если они видимы
			if (this.maxRam.visible) {
				this.maxRam.render(context, mouseX, mouseY, delta);
				this.videoMemory.render(context, mouseX, mouseY, delta);
			}
		}else {
			int yOff = -((this.textRender.fontHeight * status.split("\n").length)/2);
			for(String s : status.split("\n")) {
				context.drawText(this.textRender, s, setupGui.width/2 - this.textRender.getWidth(s)/2, (setupGui.height/2-this.textRender.fontHeight/2)+yOff, -1, false);
				yOff+=this.textRender.fontHeight+1;
			}
		}
	}

	@Override
	public void init() {
		String maxRamText = ""+ClientMod.maxRam;
		if(maxRam != null) {
			maxRamText = maxRam.getText();
		}
		String videoMemoryText = ""+ClientMod.videoMem;
		if(videoMemory != null) {
			videoMemoryText = videoMemory.getText();
		}

		maxRam = new TextFieldWidget(this.textRender, setupGui.width/2-160, setupGui.height/2-20, 150, 20, Text.empty());
		maxRam.setMaxLength(10);
		maxRam.setText(maxRamText);
		maxRam.setChangedListener(this::checkMaxRam);

		videoMemory = new TextFieldWidget(this.textRender, setupGui.width/2+10, setupGui.height/2-20, 150, 20, Text.empty());
		videoMemory.setMaxLength(3);
		videoMemory.setText(videoMemoryText);
		videoMemory.setChangedListener(this::videoMemory);

		checkMaxRam(maxRam.getText());
		videoMemory(videoMemory.getText());

		setupGui.addElement(maxRam);
		setupGui.addElement(videoMemory);

		int confirmW = textRender.getWidth(setupGui.translation("newvmcomputers.setup.confirmButton"))+40;
		setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("newvmcomputers.setup.confirmButton")), this::confirmButton)
				.dimensions(setupGui.width/2 - (confirmW/2), setupGui.height - 40, confirmW, 20)
				.build());

		if(setupGui.startVb) {
			confirmButton(null);
			setupGui.startVb = false;
		}
	}
}
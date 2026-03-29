package newvmcomputers.client.gui.setup.pages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.virtualbox_6_1.IVirtualBox;
import org.virtualbox_6_1.VirtualBoxManager;

import com.google.gson.Gson;

import newvmcomputers.client.ClientMod;
import newvmcomputers.client.gui.setup.GuiSetup;
import newvmcomputers.client.utils.VMSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class SetupPageMaxValues extends SetupPage{
	private static final String[] VBOX_ENDPOINTS = {"http://127.0.0.1:18083", "http://localhost:18083"};
	private static final int INITIAL_CONNECT_ATTEMPTS = 3;
	private static final int STARTUP_CONNECT_ATTEMPTS = 30;
	private static final long CONNECT_RETRY_DELAY_MS = 1000L;

	private String statusMaxRam;
	private String statusVideoMemory;
	private EditBox maxRam;
	private EditBox videoMemory;
	private String status;
	private boolean onlyStatusMessage = false;

	public SetupPageMaxValues(GuiSetup setupGui, Font textRender) {
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

	private void confirmButton(Button in) {
		if (setupGui.useVmware) {
			status = setupGui.translation("newvmcomputers.setup.startingStatus");
			onlyStatusMessage = true;
			ClientMod.useVmware = true;
			ClientMod.vmwareDirectory = setupGui.vmwareDirectory;
			ClientMod.maxRam = 8192;
			ClientMod.videoMem = 256;
			VMSettings set = new VMSettings();
			set.vboxDirectory = setupGui.virtualBoxDirectory;
			set.vmComputersDirectory = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
			set.unfocusKey1 = ClientMod.glfwUnfocusKey1;
			set.unfocusKey2 = ClientMod.glfwUnfocusKey2;
			set.unfocusKey3 = ClientMod.glfwUnfocusKey3;
			set.unfocusKey4 = ClientMod.glfwUnfocusKey4;
			set.maxRam = ClientMod.maxRam;
			set.videoMem = ClientMod.videoMem;
			set.useVmware = true;
			set.vmwareDirectory = setupGui.vmwareDirectory;

			try {
				writeSetupFile(set);
				System.out.println("Setup saved successfully for VMware.");
			} catch (Exception e) {
				System.err.println("Failed to save setup.json: " + e.getMessage());
			}
			minecraft.execute(() -> minecraft.setScreen(new TitleScreen(false)));
			return;
		}

		boolean[] bools = new boolean[] {checkMaxRam(maxRam.getValue()), videoMemory(videoMemory.getValue())};
		for(boolean b : bools) {
			if(!b) {
				return;
			}
		}

		this.setupGui.clearElements();
		this.setupGui.clearButtons();
		onlyStatusMessage = true;
		ClientMod.useVmware = false;
		ClientMod.maxRam = Integer.parseInt(maxRam.getValue());
		ClientMod.videoMem = Integer.parseInt(videoMemory.getValue());

		status = setupGui.translation("newvmcomputers.setup.startingStatus");
		new Thread(() -> {
			try {
				VirtualBoxManager vm = VirtualBoxManager.createInstance(null);
				File vboxWebSrvLog = new File(minecraft.gameDirectory, "vm_computers/vboxwebsrv.log");
				IVirtualBox vb = tryConnectToVirtualBox(vm, INITIAL_CONNECT_ATTEMPTS, 500L);
				if (vb == null) {
					prepareVirtualBoxWebService(vboxWebSrvLog);
					vb = connectToVirtualBox(vm, STARTUP_CONNECT_ATTEMPTS, CONNECT_RETRY_DELAY_MS, vboxWebSrvLog);
				}
				VMSettings set = new VMSettings();
				set.vboxDirectory = setupGui.virtualBoxDirectory;
				set.vmComputersDirectory = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
				set.unfocusKey1 = ClientMod.glfwUnfocusKey1;
				set.unfocusKey2 = ClientMod.glfwUnfocusKey2;
				set.unfocusKey3 = ClientMod.glfwUnfocusKey3;
				set.unfocusKey4 = ClientMod.glfwUnfocusKey4;
				set.maxRam = ClientMod.maxRam;
				set.videoMem = ClientMod.videoMem;
				set.useVmware = false;
				set.vmwareDirectory = "";

				writeSetupFile(set);

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
				ex.printStackTrace();
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

	private IVirtualBox tryConnectToVirtualBox(VirtualBoxManager vm, int attempts, long delayMs) {
		try {
			return connectToVirtualBox(vm, attempts, delayMs, null);
		} catch (Exception ignored) {
			return null;
		}
	}

	private IVirtualBox connectToVirtualBox(VirtualBoxManager vm, int attempts, long delayMs, File logFile) throws Exception {
		Exception lastError = null;
		for (int attempt = 0; attempt < attempts; attempt++) {
			for (String endpoint : VBOX_ENDPOINTS) {
				try {
					vm.connect(endpoint, "should", "work");
					System.out.println("VMComputers: Connected to VirtualBox via " + endpoint);
					return vm.getVBox();
				} catch (Exception ex) {
					lastError = ex;
					try {
						vm.disconnect();
					} catch (Exception ignored) {
					}
				}
			}

			if (attempt + 1 < attempts) {
				Thread.sleep(delayMs);
			}
		}

		if (lastError != null) {
			if (logFile != null) {
				System.err.println("VMComputers: vboxwebsrv log file: " + logFile.getAbsolutePath());
			}
			throw lastError;
		}
		throw new IllegalStateException("VirtualBox connection failed without an exception");
	}

	private void writeSetupFile(VMSettings settings) throws IOException {
		File setupFile = new File(minecraft.gameDirectory, "vm_computers/setup.json");
		ensureParentDirectoryExists(setupFile);
		Files.deleteIfExists(setupFile.toPath());

		try (FileWriter writer = new FileWriter(setupFile)) {
			writer.append(new Gson().toJson(settings));
			writer.flush();
		}
	}

	private void ensureParentDirectoryExists(File file) throws IOException {
		File parent = file.getParentFile();
		if (parent == null) {
			return;
		}
		if (parent.exists()) {
			if (!parent.isDirectory()) {
				throw new IOException("Path is not a directory: " + parent.getAbsolutePath());
			}
			return;
		}
		if (!parent.mkdirs() && !parent.isDirectory()) {
			throw new IOException("Failed to create directory: " + parent.getAbsolutePath());
		}
	}

	private void prepareVirtualBoxWebService(File logFile) throws Exception {
		stopTrackedVirtualBoxWebService();
		runVirtualBoxCommand(createVBoxManageProcessBuilder("setproperty", "websrvauthlibrary", "null"), "VBoxManage");
		startVirtualBoxWebService(logFile);
	}

	private void stopTrackedVirtualBoxWebService() {
		if (ClientMod.vboxWebSrv == null) {
			return;
		}

		try {
			if (ClientMod.vboxWebSrv.isAlive()) {
				ClientMod.vboxWebSrv.destroy();
				if (!ClientMod.vboxWebSrv.waitFor(5, TimeUnit.SECONDS) && ClientMod.vboxWebSrv.isAlive()) {
					ClientMod.vboxWebSrv.destroyForcibly();
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			ClientMod.vboxWebSrv = null;
		}
	}

	private void runVirtualBoxCommand(ProcessBuilder builder, String commandName) throws Exception {
		Process process = builder.start();
		if (!process.waitFor(10, TimeUnit.SECONDS)) {
			process.destroyForcibly();
			throw new IOException(commandName + " did not finish in time");
		}

		if (process.exitValue() != 0) {
			throw new IOException(commandName + " exited with code " + process.exitValue());
		}
	}

	private void startVirtualBoxWebService(File logFile) throws Exception {
		File parent = logFile.getParentFile();
		if (parent != null) {
			parent.mkdirs();
		}

		ProcessBuilder builder = createVBoxWebServiceProcessBuilder(logFile);
		ClientMod.vboxWebSrv = builder.start();
		System.out.println("VMComputers: Started vboxwebsrv, log file: " + logFile.getAbsolutePath());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (ClientMod.vboxWebSrv != null) {
				ClientMod.vboxWebSrv.destroy();
			}
		}));

		Thread.sleep(1000L);
		if (!ClientMod.vboxWebSrv.isAlive()) {
			throw new IOException("vboxwebsrv exited immediately. See " + logFile.getAbsolutePath());
		}
	}

	private ProcessBuilder createVBoxManageProcessBuilder(String... args) {
		if (SystemUtils.IS_OS_WINDOWS) {
			return createVirtualBoxProcessBuilder(new File(this.setupGui.virtualBoxDirectory, "VBoxManage.exe").getAbsolutePath(), args);
		}
		if (SystemUtils.IS_OS_MAC) {
			return createVirtualBoxProcessBuilder(new File(this.setupGui.virtualBoxDirectory, "VBoxManage").getAbsolutePath(), args);
		}
		return createVirtualBoxProcessBuilder("vboxmanage", args);
	}

	private ProcessBuilder createVBoxWebServiceProcessBuilder(File logFile) {
		String executable;
		if (SystemUtils.IS_OS_WINDOWS) {
			executable = new File(this.setupGui.virtualBoxDirectory, "vboxwebsrv.exe").getAbsolutePath();
		} else if (SystemUtils.IS_OS_MAC) {
			executable = new File(this.setupGui.virtualBoxDirectory, "vboxwebsrv").getAbsolutePath();
		} else {
			executable = "vboxwebsrv";
		}

		ProcessBuilder builder = createVirtualBoxProcessBuilder(executable, "--host", "127.0.0.1", "--port", "18083", "--timeout", "0", "--logfile", logFile.getAbsolutePath());
		builder.redirectErrorStream(true);
		builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
		return builder;
	}

	private ProcessBuilder createVirtualBoxProcessBuilder(String executable, String... args) {
		String[] command = new String[args.length + 1];
		command[0] = executable;
		System.arraycopy(args, 0, command, 1, args.length);

		ProcessBuilder builder = new ProcessBuilder(command);
		if (!SystemUtils.IS_OS_LINUX && !this.setupGui.virtualBoxDirectory.isEmpty()) {
			builder.directory(new File(this.setupGui.virtualBoxDirectory));
		}
		return builder;
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		if(!onlyStatusMessage) {
			if (setupGui.useVmware) {
				context.drawString(this.textRender, setupGui.translation("newvmcomputers.setup.vmware_dir"), setupGui.width/2 - 160, setupGui.height/2 - 55, -1, false);
				context.drawString(this.textRender, setupGui.translation("newvmcomputers.setup.vmrun_help"), setupGui.width/2 - 160, setupGui.height/2 - 40, -1, false);
				context.drawString(this.textRender, setupGui.translation("newvmcomputers.setup.vmrun_note"), setupGui.width/2 - 160, setupGui.height/2 - 28, -1, false);
				context.drawString(this.textRender, setupGui.translation("newvmcomputers.setup.vmrun_note_detail"), setupGui.width/2 - 160, setupGui.height/2 - 16, -1, false);

				this.maxRam.visible = false;
				this.videoMemory.visible = false;
			} else {
				context.drawString(this.textRender, setupGui.translation("newvmcomputers.setup.max_ram_input"), setupGui.width/2 - 160, setupGui.height/2-30, -1, false);
				context.drawString(this.textRender, setupGui.translation("newvmcomputers.setup.vram_input"), setupGui.width/2 + 10, setupGui.height/2-30, -1, false);
				String s = setupGui.translation("newvmcomputers.setup.ram_input_help");
				context.drawString(this.textRender, s, setupGui.width/2 - textRender.width(s)/2, setupGui.height/2+30, -1, false);
				context.drawString(this.textRender, statusMaxRam, setupGui.width / 2 - 160, setupGui.height/2 + 3, -1, false);
				context.drawString(this.textRender, statusVideoMemory, setupGui.width / 2 + 10, setupGui.height/2 + 3, -1, false);

				this.maxRam.visible = true;
				this.videoMemory.visible = true;
			}
			if (this.maxRam.visible) {
				this.maxRam.render(context, mouseX, mouseY, delta);
				this.videoMemory.render(context, mouseX, mouseY, delta);
			}
		}else {
			int yOff = -((this.textRender.lineHeight * status.split("\n").length)/2);
			for(String s : status.split("\n")) {
				context.drawString(this.textRender, s, setupGui.width/2 - this.textRender.width(s)/2, (setupGui.height/2-this.textRender.lineHeight/2)+yOff, -1, false);
				yOff+=this.textRender.lineHeight+1;
			}
		}
	}

	@Override
	public void init() {
		String maxRamText = ""+ClientMod.maxRam;
		if(maxRam != null) {
			maxRamText = maxRam.getValue();
		}
		String videoMemoryText = ""+ClientMod.videoMem;
		if(videoMemory != null) {
			videoMemoryText = videoMemory.getValue();
		}

		maxRam = new EditBox(this.textRender, setupGui.width/2-160, setupGui.height/2-20, 150, 20, Component.empty());
		maxRam.setMaxLength(10);
		maxRam.setValue(maxRamText);
		maxRam.setResponder(this::checkMaxRam);

		videoMemory = new EditBox(this.textRender, setupGui.width/2+10, setupGui.height/2-20, 150, 20, Component.empty());
		videoMemory.setMaxLength(3);
		videoMemory.setValue(videoMemoryText);
		videoMemory.setResponder(this::videoMemory);

		checkMaxRam(maxRam.getValue());
		videoMemory(videoMemory.getValue());

		setupGui.addElement(maxRam);
		setupGui.addElement(videoMemory);

		int confirmW = textRender.width(setupGui.translation("newvmcomputers.setup.confirmButton"))+40;
		setupGui.addButton(Button.builder(Component.literal(setupGui.translation("newvmcomputers.setup.confirmButton")), this::confirmButton)
				.bounds(setupGui.width/2 - (confirmW/2), setupGui.height - 40, confirmW, 20)
				.build());

		if(setupGui.startVb) {
			confirmButton(null);
			setupGui.startVb = false;
		}
	}
}


package mcvmcomputers.client.gui.setup.pages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.gson.Gson;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.client.gui.setup.GuiSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import mcvmcomputers.client.utils.VBoxManage;
import mcvmcomputers.client.utils.VMSettings;


import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class SetupPageMaxValues extends SetupPage{
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
			statusMaxRam = setupGui.translation("mcvmcomputers.input_empty");
			return false;
		}
		if(!StringUtils.isNumeric(input)) {
			statusMaxRam = setupGui.translation("mcvmcomputers.input_nan");
			return false;
		}
		int rm = Integer.parseInt(input);
		if(rm < 16) {
			statusMaxRam = setupGui.translation("mcvmcomputers.input_too_little").replace("%s", "16");
			return false;
		}
		statusMaxRam = setupGui.translation("mcvmcomputers.input_valid");
		return true;
	}

	private boolean videoMemory(String input) {
		if(input.isEmpty()) {
			statusVideoMemory = setupGui.translation("mcvmcomputers.input_empty");
			return false;
		}
		if(!StringUtils.isNumeric(input)) {
			statusVideoMemory = setupGui.translation("mcvmcomputers.input_nan");
			return false;
		}
		int nm = Integer.parseInt(input);
		if(nm > 256) {
			statusVideoMemory = setupGui.translation("mcvmcomputers.input_too_much").replace("%s", "256");
			return false;
		}
		statusVideoMemory = setupGui.translation("mcvmcomputers.input_valid");
		return true;
	}

	private void confirmButton(Button in) {
		boolean[] bools = new boolean[] {checkMaxRam(maxRam.getValue()), videoMemory(videoMemory.getValue())};
		for(boolean b : bools) {
			if(!b) {
				return;
			}
		}
		this.setupGui.clearElements();
		this.setupGui.clearButtons();
		onlyStatusMessage = true;
		ClientMod.maxRam = Integer.parseInt(maxRam.getValue());
		ClientMod.videoMem = Integer.parseInt(videoMemory.getValue());
		status = setupGui.translation("mcvmcomputers.setup.startingStatus");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					VBoxManage vboxMgr = new VBoxManage(setupGui.virtualBoxDirectory);
					String version = vboxMgr.testConnection();

					if(version == null) {
						throw new RuntimeException("VBoxManage not found or not working");
					}


					vboxMgr.setWebsrvAuthNull();

					VMSettings set = new VMSettings();
					set.vboxDirectory = setupGui.virtualBoxDirectory;
					set.vmComputersDirectory = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
					set.unfocusKey1 = ClientMod.glfwUnfocusKey1;
					set.unfocusKey2 = ClientMod.glfwUnfocusKey2;
					set.unfocusKey3 = ClientMod.glfwUnfocusKey3;
					set.unfocusKey4 = ClientMod.glfwUnfocusKey4;
					set.maxRam = ClientMod.maxRam;
					set.videoMem = ClientMod.videoMem;
					File f = new File(minecraft.gameDirectory, "vm_computers/setup.json");
					if(f.exists()) {
						f.delete();
					}
					f.createNewFile();
					FileWriter fw = new FileWriter(f);
					fw.append(new Gson().toJson(set));
					fw.flush();
					fw.close();

					for(int i = 5;i>=0;i--) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						status = setupGui.translation("mcvmcomputers.setup.successStatus").replaceFirst("%s", version).replaceFirst("%s", ""+i);
					}

					ClientMod.vbox = vboxMgr;
					minecraft.execute(() -> minecraft.setScreen(new TitleScreen()));
					return;
				}catch(Exception ex) {
					ex.printStackTrace();
					for(int i = 5;i>=0;i--) {
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						status = setupGui.translation("mcvmcomputers.setup.failedStatus").replace("%s", ""+i);
					}
					minecraft.execute(() -> {
						onlyStatusMessage = false;
						setupGui.firstPage();
					});
				}
			}
		}).start();
	}

	@Override
	public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
		if(!onlyStatusMessage) {
			context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.max_ram_input"), setupGui.width/2 - 160, setupGui.height/2-30, -1);
			context.drawString(this.textRender, setupGui.translation("mcvmcomputers.setup.vram_input"), setupGui.width/2 + 10, setupGui.height/2-30, -1);
			String s = setupGui.translation("mcvmcomputers.setup.ram_input_help");
			context.drawString(this.textRender, s, setupGui.width/2 - textRender.width(s)/2, setupGui.height/2+30, -1);
			context.drawString(this.textRender, statusMaxRam, setupGui.width / 2 - 160, setupGui.height/2 + 3, -1);
			context.drawString(this.textRender, statusVideoMemory, setupGui.width / 2 + 10, setupGui.height/2 + 3, -1);
			this.maxRam.render(context, mouseX, mouseY, delta);
			this.videoMemory.render(context, mouseX, mouseY, delta);
		}else {
			int yOff = -((this.textRender.lineHeight * status.split("\n").length)/2);
			for(String s : status.split("\n")) {
				context.drawString(this.textRender, s, setupGui.width/2 - this.textRender.width(s)/2, (setupGui.height/2-this.textRender.lineHeight/2)+yOff, -1);
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
		if(!onlyStatusMessage) {
			maxRam = new EditBox(this.textRender, setupGui.width/2-160, setupGui.height/2-20, 150, 20, Component.literal(""));
			maxRam.setValue(maxRamText);
			maxRam.setResponder((str) -> checkMaxRam(str));
			videoMemory = new EditBox(this.textRender, setupGui.width/2+10, setupGui.height/2-20, 150, 20, Component.literal(""));
			videoMemory.setValue(videoMemoryText);
			videoMemory.setResponder((str) -> videoMemory(str));
			checkMaxRam(maxRam.getValue());
			videoMemory(videoMemory.getValue());
			setupGui.addElement(maxRam);
			setupGui.addElement(videoMemory);
			int confirmW = textRender.width(setupGui.translation("mcvmcomputers.setup.confirmButton"))+40;
			setupGui.addButton(Button.builder(Component.literal(setupGui.translation("mcvmcomputers.setup.confirmButton")), (btn) -> confirmButton(btn)).bounds(setupGui.width/2 - (confirmW/2), setupGui.height - 40, confirmW, 20).build());

			if(setupGui.startVb) {
				confirmButton(null);
				setupGui.startVb = false;
			}
		}
	}

}

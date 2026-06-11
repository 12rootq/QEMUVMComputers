package mcvmcomputers.client.gui.setup.pages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import com.google.gson.Gson;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.client.gui.setup.GuiSetup;
import mcvmcomputers.client.utils.VBoxManage;
import mcvmcomputers.client.utils.VMSettings;
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

	private void confirmButton(ButtonWidget in) {
		boolean[] bools = new boolean[] {checkMaxRam(maxRam.getText()), videoMemory(videoMemory.getText())};
		for(boolean b : bools) {
			if(!b) {
				return;
			}
		}
		this.setupGui.clearElements();
		this.setupGui.clearButtons();
		onlyStatusMessage = true;
		ClientMod.maxRam = Integer.parseInt(maxRam.getText());
		ClientMod.videoMem = Integer.parseInt(videoMemory.getText());
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
					File f = new File(minecraft.runDirectory, "vm_computers/setup.json");
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
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		if(!onlyStatusMessage) {
			context.drawTextWithShadow(this.textRender, setupGui.translation("mcvmcomputers.setup.max_ram_input"), setupGui.width/2 - 160, setupGui.height/2-30, -1);
			context.drawTextWithShadow(this.textRender, setupGui.translation("mcvmcomputers.setup.vram_input"), setupGui.width/2 + 10, setupGui.height/2-30, -1);
			String s = setupGui.translation("mcvmcomputers.setup.ram_input_help");
			context.drawTextWithShadow(this.textRender, s, setupGui.width/2 - textRender.getWidth(s)/2, setupGui.height/2+30, -1);
			context.drawTextWithShadow(this.textRender, statusMaxRam, setupGui.width / 2 - 160, setupGui.height/2 + 3, -1);
			context.drawTextWithShadow(this.textRender, statusVideoMemory, setupGui.width / 2 + 10, setupGui.height/2 + 3, -1);
			this.maxRam.render(context, mouseX, mouseY, delta);
			this.videoMemory.render(context, mouseX, mouseY, delta);
		}else {
			int yOff = -((this.textRender.fontHeight * status.split("\n").length)/2);
			for(String s : status.split("\n")) {
				context.drawTextWithShadow(this.textRender, s, setupGui.width/2 - this.textRender.getWidth(s)/2, (setupGui.height/2-this.textRender.fontHeight/2)+yOff, -1);
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
		if(!onlyStatusMessage) {
			maxRam = new TextFieldWidget(this.textRender, setupGui.width/2-160, setupGui.height/2-20, 150, 20, Text.literal(""));
			maxRam.setText(maxRamText);
			maxRam.setChangedListener((str) -> checkMaxRam(str));
			videoMemory = new TextFieldWidget(this.textRender, setupGui.width/2+10, setupGui.height/2-20, 150, 20, Text.literal(""));
			videoMemory.setText(videoMemoryText);
			videoMemory.setChangedListener((str) -> videoMemory(str));
			checkMaxRam(maxRam.getText());
			videoMemory(videoMemory.getText());
			setupGui.addElement(maxRam);
			setupGui.addElement(videoMemory);
			int confirmW = textRender.getWidth(setupGui.translation("mcvmcomputers.setup.confirmButton"))+40;
			setupGui.addButton(ButtonWidget.builder(Text.literal(setupGui.translation("mcvmcomputers.setup.confirmButton")), (btn) -> confirmButton(btn)).dimensions(setupGui.width/2 - (confirmW/2), setupGui.height - 40, confirmW, 20).build());

			if(setupGui.startVb) {
				confirmButton(null);
				setupGui.startVb = false;
			}
		}
	}

}

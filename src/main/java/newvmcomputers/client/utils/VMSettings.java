package newvmcomputers.client.utils;

import java.io.File;

import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.glfw.GLFW;

import newvmcomputers.client.ClientMod;
import net.minecraft.client.MinecraftClient;

public class VMSettings {
	public String vboxDirectory;
	public String vmComputersDirectory;
	public boolean useVmware = false;
	public String vmwareDirectory;

	public int maxRam = 8192;
	public int videoMem = 256;
	public int unfocusKey1 = GLFW.GLFW_KEY_LEFT_CONTROL;
	public int unfocusKey2 = GLFW.GLFW_KEY_RIGHT_CONTROL;
	public int unfocusKey3 = GLFW.GLFW_KEY_BACKSPACE;
	public int unfocusKey4 = -1;

	public VMSettings() {
		if(SystemUtils.IS_OS_WINDOWS) {
			vboxDirectory = "C:\\Program Files\\Oracle\\VirtualBox";
			if (new File("C:\\Program Files (x86)\\VMware\\VMware Workstation\\vmrun.exe").exists()) {
				vmwareDirectory = "C:\\Program Files (x86)\\VMware\\VMware Workstation";
			} else {
				vmwareDirectory = "C:\\Program Files\\VMware\\VMware Workstation";
			}
		} else if(SystemUtils.IS_OS_MAC) {
			vboxDirectory = "/Applications/VirtualBox.app/Contents/MacOS";
			vmwareDirectory = "/Applications/VMware Fusion.app/Contents/Library";
			unfocusKey1 = GLFW.GLFW_KEY_LEFT_ALT;
			unfocusKey2 = GLFW.GLFW_KEY_RIGHT_ALT;
		} else if (SystemUtils.IS_OS_LINUX) {
			vmwareDirectory = "/usr/bin";
		}
		if(ClientMod.vhdDirectory != null) {
			vmComputersDirectory = ClientMod.vhdDirectory.getParentFile().getAbsolutePath();
		}else {
			MinecraftClient mc = MinecraftClient.getInstance();
			vmComputersDirectory = new File(mc.runDirectory, "vm_computers").getAbsolutePath();
		}
	}
}
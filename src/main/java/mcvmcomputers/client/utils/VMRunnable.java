package mcvmcomputers.client.utils;

import static mcvmcomputers.client.ClientMod.*;

import java.util.Arrays;
import java.util.List;

import mcvmcomputers.client.gui.GuiFocus;
import net.minecraft.client.MinecraftClient;

public class VMRunnable implements Runnable {
	@Override
	public void run() {
		MinecraftClient mcc = MinecraftClient.getInstance();
		while (true) {
			try {
				double deltaX = 0;
				double deltaY = 0;

				deltaX = mouseCurX - mouseLastX;
				deltaY = mouseCurY - mouseLastY;
				mouseLastX = mouseCurX;
				mouseLastY = mouseCurY;


				String state = vbox.getVmState("VmComputersVm");

				if ("saved".equals(state)) {
					vbox.discardSavedState("VmComputersVm");
					state = "poweroff";
				}

				if ("poweroff".equals(state)) {
					if (!vmTurningOff && vmTurnedOn) {
						try {
							vbox.startVm("VmComputersVm");
						} catch (Exception e) {

							vmUpdateThread = null;
							return;
						}
					} else {
						vmUpdateThread = null;
						return;
					}
				}


				if ("running".equals(state) || "firstonline".equals(state)) {

					if (mcc.currentScreen instanceof GuiFocus) {
						int val = 0x00;
						if (leftMouseButton) val += 0x01;
						if (middleMouseButton) val += 0x04;
						if (rightMouseButton) val += 0x02;
						vbox.putMouseEvent("VmComputersVm", (int) deltaX, (int) deltaY, mouseDeltaScroll, val);
						mouseDeltaScroll = 0;
					}


					if (releaseKeys) {

						List<Integer> releaseCodes = Arrays.asList(0x1d + 0x80, 0xe0, 0x1d + 0x80, 0x0e + 0x80);
						vbox.putScancodes("VmComputersVm", releaseCodes);
						vmKeyboardScancodes.clear();
						releaseKeys = false;
					} else if (!vmKeyboardScancodes.isEmpty()) {
						vbox.putScancodes("VmComputersVm", vmKeyboardScancodes);
						vmKeyboardScancodes.clear();
					}


					byte[] image = vbox.takeScreenshot("VmComputersVm");
					if (image != null && image.length > 0) {
						synchronized (VM_TEXTURE_LOCK) {
							vmTextureBytesSize = image.length;
							vmTextureBytes = image;
						}
					}
				}


				try {
					Thread.sleep(66);
				} catch (InterruptedException e) {
					vmUpdateThread = null;
					return;
				}

			} catch (Exception ex) {

				try {
					Thread.sleep(100);
				} catch (InterruptedException ie) {
					vmUpdateThread = null;
					return;
				}
			}
		}
	}
}

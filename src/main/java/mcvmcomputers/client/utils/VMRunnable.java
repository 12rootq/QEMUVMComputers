package mcvmcomputers.client.utils;

import static mcvmcomputers.client.ClientMod.*;

import java.util.Arrays;
import java.util.List;

import mcvmcomputers.client.gui.GuiFocus;
import net.minecraft.client.MinecraftClient;

/**
 * Background worker that drives the running VirtualBox VM at ~15 Hz: forwards
 * the buffered mouse and keyboard input, takes a screenshot of the current
 * framebuffer and stages it for upload as a Minecraft texture.
 */
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
						// Consume any presses that happened since the last tick so a fast
						// click (press+release within one tick) is never dropped.
						int latch = mouseButtonPressedLatch;
						mouseButtonPressedLatch = 0;
						int held = mouseButtonMask;

						// First event includes every button that is held OR was just pressed.
						int downState = held | latch;
						vbox.putMouseEvent("VmComputersVm", (int) deltaX, (int) deltaY, mouseDeltaScroll, downState);
						mouseDeltaScroll = 0;

						// If a button was pressed-and-released within this tick, the press is
						// in the latch but not in the held mask. Emit a matching release so the
						// guest registers a complete click.
						if ((latch & ~held) != 0) {
							vbox.putMouseEvent("VmComputersVm", 0, 0, 0, held);
						}
					}


					if (releaseKeys) {

						List<Integer> releaseCodes = Arrays.asList(0x1d + 0x80, 0xe0, 0x1d + 0x80, 0x0e + 0x80);
						vbox.putScancodes("VmComputersVm", releaseCodes);
						synchronized (vmKeyboardScancodes) {
							vmKeyboardScancodes.clear();
						}
						releaseKeys = false;
					} else {
						// Atomically snapshot and clear the buffer under the same lock the
						// keyboard callback uses. Sending happens outside the lock so the
						// render thread is never blocked, and any key typed after this point
						// stays queued for the next tick instead of being wiped by clear().
						List<Integer> toSend = null;
						synchronized (vmKeyboardScancodes) {
							if (!vmKeyboardScancodes.isEmpty()) {
								toSend = new java.util.ArrayList<>(vmKeyboardScancodes);
								vmKeyboardScancodes.clear();
							}
						}
						if (toSend != null) {
							vbox.putScancodes("VmComputersVm", toSend);
						}
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

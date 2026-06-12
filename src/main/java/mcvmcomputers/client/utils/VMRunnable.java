package mcvmcomputers.client.utils;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;


import static mcvmcomputers.client.ClientMod.*;

import java.util.Arrays;
import java.util.List;

import mcvmcomputers.client.gui.GuiFocus;
import net.minecraft.client.Minecraft;

/**
 * Background virtual machine update thread (one per running VM).
 *
 * <p>Started from {@code GameloopMixin} when the VM is on. In a loop (~15 times per
 * second) it does the following:</p>
 * <ul>
 *   <li>periodically polls the VM state via VBoxManage; if the VM is off but the game
 *       thinks it is on, it starts it ({@code startVm});</li>
 *   <li>while the focus screen {@link GuiFocus} is open, sends mouse movement/buttons/
 *       scroll and the accumulated keyboard scancodes to the guest OS;</li>
 *   <li>on the {@code releaseKeys} flag, sends release codes so "stuck" keys do not
 *       remain held after leaving focus;</li>
 *   <li>takes a screenshot of the VM screen and stores its bytes under
 *       {@code VM_TEXTURE_LOCK} for subsequent rendering on in-world screens.</li>
 * </ul>
 * <p>All VM calls are best-effort: exceptions are logged/swallowed so the thread does
 * not crash.</p>
 */
/**
 * Background worker that drives the running VirtualBox VM at ~15 Hz: forwards
 * the buffered mouse and keyboard input, takes a screenshot of the current
 * framebuffer and stages it for upload as a Minecraft texture.
 */
public class VMRunnable implements Runnable {
	/** The VM state is re-checked every this many loop iterations. */
	private static final int STATE_CHECK_INTERVAL = 30;

	@Override
	public void run() {
		Minecraft mcc = Minecraft.getInstance();
		int ticksSinceStateCheck = STATE_CHECK_INTERVAL;
		String cachedState = "poweroff";

		while (true) {
			try {
				double curX = mouseCurX;
				double curY = mouseCurY;
				double lastX = mouseLastX;
				double lastY = mouseLastY;

				double deltaX = curX - lastX;
				double deltaY = curY - lastY;
				mouseLastX = curX;
				mouseLastY = curY;


				ticksSinceStateCheck++;
				if (ticksSinceStateCheck >= STATE_CHECK_INTERVAL) {
					ticksSinceStateCheck = 0;
					cachedState = vbox.getVmState("VmComputersVm");
				}

				if ("saved".equals(cachedState)) {
					vbox.discardSavedState("VmComputersVm");
					cachedState = "poweroff";
				}

				if ("poweroff".equals(cachedState)) {
					if (!vmTurningOff && vmTurnedOn) {
						try {
							vbox.startVm("VmComputersVm");
							cachedState = "running";
						} catch (Exception e) {
							vmUpdateThread = null;
							return;
						}
					} else {
						vmUpdateThread = null;
						return;
					}
				}


				if ("running".equals(cachedState) || "firstonline".equals(cachedState)) {

					if (mcc.screen instanceof GuiFocus) {
						int latch = mouseButtonPressedLatch;
						mouseButtonPressedLatch = 0;
						int held = mouseButtonMask;

						int scroll = mouseDeltaScroll;
						mouseDeltaScroll = 0;

						int downState = held | latch;
						vbox.putMouseEvent("VmComputersVm", (int) deltaX, (int) deltaY, scroll, downState);

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

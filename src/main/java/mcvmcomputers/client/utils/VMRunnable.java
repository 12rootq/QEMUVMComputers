package mcvmcomputers.client.utils;

import static mcvmcomputers.client.ClientMod.*;

import java.util.Arrays;
import java.util.List;

import mcvmcomputers.client.gui.GuiFocus;
import net.minecraft.client.MinecraftClient;

/**
 * Background virtual machine update thread (one per running VM).
 *
 * <p>Started from GameloopMixin when the VM is on. In a loop (~15 times per second) it:
 * periodically polls the VM state via VBoxManage (only every STATE_CHECK_INTERVAL
 * iterations, because each poll spawns a VBoxManage process); starts the VM if the game
 * thinks it is on but it is powered off; while the focus screen is open sends mouse and
 * keyboard input to the guest OS; and grabs a screenshot for in-world screens.</p>
 */
/**
 * Background worker that drives the running VirtualBox VM at ~15 Hz: forwards
 * the buffered mouse and keyboard input, takes a screenshot of the current
 * framebuffer and stages it for upload as a Minecraft texture.
 */
public class VMRunnable implements Runnable {
// The VM state is re-checked every this many loop iterations. Polling every tick
// would spawn a VBoxManage process ~15 times per second, which is wasteful.
private static final int STATE_CHECK_INTERVAL = 30;

@Override
public void run() {
MinecraftClient mcc = MinecraftClient.getInstance();
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

if (mcc.currentScreen instanceof GuiFocus) {
// Consume any presses since last tick so a fast click is never dropped.
int latch = mouseButtonPressedLatch;
mouseButtonPressedLatch = 0;
int held = mouseButtonMask;

int scroll = mouseDeltaScroll;
mouseDeltaScroll = 0;

int downState = held | latch;
vbox.putMouseEvent("VmComputersVm", (int) deltaX, (int) deltaY, scroll, downState);

// If a button was pressed-and-released within this tick, emit the matching release.
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
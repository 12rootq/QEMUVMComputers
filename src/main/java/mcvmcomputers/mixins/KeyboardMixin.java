package mcvmcomputers.mixins;

import static mcvmcomputers.client.ClientMod.*;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.client.gui.GuiFocus;
import mcvmcomputers.client.gui.setup.pages.SetupPageUnfocusBinding;
import mcvmcomputers.client.utils.KeyConverter;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;

@Mixin(Keyboard.class)
public class KeyboardMixin {
	@Inject(at = @At("HEAD"), method = "onKey")
	public void onKey(long window, int key, int scancode, int i, int j, CallbackInfo ci) {
		MinecraftClient mcc = MinecraftClient.getInstance();
		if (window == mcc.getWindow().getHandle()) {
			if (ClientMod.vmTurnedOn && mcc.currentScreen instanceof GuiFocus) {
				// Forward press, repeat AND release. Without the release, the guest sees the
				// key as still held down and its auto-repeat spams the key (e.g. Enter).
				if (i == GLFW.GLFW_PRESS || i == GLFW.GLFW_REPEAT || i == GLFW.GLFW_RELEASE) {
					// This callback runs on the render thread while VMRunnable drains the
					// buffer on the VM thread. Synchronize so a press/release is never lost
					// to a concurrent clear() (dropped key) and so we never iterate the list
					// while it is being mutated (ConcurrentModificationException -> stuck key).
					synchronized (ClientMod.vmKeyboardScancodes) {
						ClientMod.vmKeyboardScancodes.addAll(KeyConverter.toVBKey(key, i));
					}
				}
			} else if (SetupPageUnfocusBinding.changeBinding) {
				if (i == GLFW.GLFW_PRESS) {
					if (getKeyName(key) != null) {
						switch (SetupPageUnfocusBinding.bindingToBeChangedNum) {
							case 1:
								glfwUnfocusKey1 = key;
								break;
							case 2:
								glfwUnfocusKey2 = key;
								break;
							case 3:
								glfwUnfocusKey3 = key;
								break;
							case 4:
								glfwUnfocusKey4 = key;
								break;
						}
						SetupPageUnfocusBinding.bindingJustChanged = true;
						SetupPageUnfocusBinding.changeBinding = false;
					}
				}
			}
		}
	}
}

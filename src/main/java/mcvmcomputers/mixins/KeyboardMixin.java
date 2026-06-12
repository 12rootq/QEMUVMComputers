package mcvmcomputers.mixins;
import net.minecraft.client.gui.screens.Screen;


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
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;

/**
 * Keyboard input capture (mixin into vanilla {@link KeyboardHandler}).
 *
 * <p>Responsible for forwarding key presses to the virtual machine: when the VM is
 * on and the focus screen {@link GuiFocus} is open, every press/repeat/release is
 * converted to VirtualBox scancodes ({@link KeyConverter}) and queued into
 * {@link ClientMod#vmKeyboardScancodes}, from which the VM update thread sends them
 * to the guest OS.</p>
 *
 * <p>In a second mode (when the unfocus-key rebinding is active in the setup
 * screen), the same hook records the pressed key as the new unfocus binding.</p>
 */
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(at = @At("HEAD"), method = "keyPress")
    public void keyPress(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        Minecraft mcc = Minecraft.getInstance();
        // Only react to events from the main game window.
        if (window == mcc.getWindow().getWindow()) {
            // Mode 1: VM is on and the focus screen is active - send keys to the guest OS.
            if (ClientMod.vmTurnedOn && mcc.screen instanceof GuiFocus) {
                if (action == GLFW.GLFW_PRESS || action == GLFW.GLFW_REPEAT || action == GLFW.GLFW_RELEASE) {
                    synchronized (ClientMod.vmKeyboardScancodes) {
                        ClientMod.vmKeyboardScancodes.addAll(KeyConverter.toVBKey(key, action));
                    }
                }
            } else if (SetupPageUnfocusBinding.changeBinding) {
                // Mode 2: the user is rebinding the unfocus key.
                if (action == GLFW.GLFW_PRESS) {
                    if (getKeyName(key) != null) {
                        switch (SetupPageUnfocusBinding.bindingToBeChangedNum) {
                            case 1 -> glfwUnfocusKey1 = key;
                            case 2 -> glfwUnfocusKey2 = key;
                            case 3 -> glfwUnfocusKey3 = key;
                            case 4 -> glfwUnfocusKey4 = key;
                        }
                        SetupPageUnfocusBinding.bindingJustChanged = true;
                        SetupPageUnfocusBinding.changeBinding = false;
                    }
                }
            }
        }
    }
}

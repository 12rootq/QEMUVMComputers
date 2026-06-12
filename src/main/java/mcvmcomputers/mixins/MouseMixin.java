package mcvmcomputers.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcvmcomputers.client.ClientMod;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
/**
 * Mixin into the client mouse handler. Captures scroll deltas and button
 * press/release events into the VM input buffers (with a press latch so a fast
 * click within a single VM tick is never lost).
 */
public class MouseMixin {
	@Inject(at = @At("TAIL"), method = "onMouseScroll")
	private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		ClientMod.mouseDeltaScroll = (int) vertical;
	}

	@Inject(at = @At("TAIL"), method = "onMouseButton")
	private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
		// GLFW button: 0=left, 1=right, 2=middle. action: 1=press, 0=release.
		// Map to VBox buttonState bits: left=0x01, right=0x02, middle=0x04.
		int bit;
		switch (button) {
			case 0: bit = 0x01; break; // left
			case 1: bit = 0x02; break; // right
			case 2: bit = 0x04; break; // middle
			default: return;
		}
		if (action == 1) {
			// Press: update held state and latch so the VM loop can't miss a fast click.
			ClientMod.mouseButtonMask |= bit;
			ClientMod.mouseButtonPressedLatch |= bit;
		} else if (action == 0) {
			// Release: clear only this button, leave the others held (chording).
			ClientMod.mouseButtonMask &= ~bit;
		}
	}
}

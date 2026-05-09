package mcvmcomputers.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcvmcomputers.client.ClientMod;
import net.minecraft.client.Mouse;

@Mixin(Mouse.class)
public class MouseMixin {
	@Inject(at = @At("TAIL"), method = "onMouseScroll")
	private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		ClientMod.mouseDeltaScroll = (int) vertical;
	}

	@Inject(at = @At("TAIL"), method = "onMouseButton")
	private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
		ClientMod.leftMouseButton = button == 0 && action == 1;
		ClientMod.middleMouseButton = button == 2 && action == 1;
		ClientMod.rightMouseButton = button == 1 && action == 1;
	}
}

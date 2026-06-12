package mcvmcomputers.mixins;
import net.minecraft.client.gui.components.Button;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcvmcomputers.client.ClientMod;
import net.minecraft.client.MouseHandler;

/**
 * Mouse input capture (mixin into vanilla {@link MouseHandler}).
 *
 * <p>Collects mouse state for the virtual machine: vertical scroll wheel and
 * button press/release. Values are stored in static {@link ClientMod} fields
 * ({@code mouseDeltaScroll}, {@code mouseButtonMask}, {@code mouseButtonPressedLatch}),
 * from which the VM update thread forwards them to the guest OS via
 * {@code putMouseEvent}. Cursor movement (dx/dy) is computed separately from the
 * position delta, not here.</p>
 */
@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(at = @At("TAIL"), method = "onScroll")
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        // Remember the scroll delta for the next frame sent to the VM.
        ClientMod.mouseDeltaScroll = (int) vertical;
    }

    @Inject(at = @At("TAIL"), method = "onPress")
    private void onPress(long window, int button, int action, int mods, CallbackInfo ci) {
        // VirtualBox button bit mask: 1=left, 2=right, 4=middle.
        int bit;
        switch (button) {
            case 0: bit = 0x01; break;
            case 1: bit = 0x02; break;
            case 2: bit = 0x04; break;
            default: return;
        }
        if (action == 1) {
            // Button pressed: set the bit in the current mask and in the latch
            // (the latch ensures a short click is not lost between frames).
            ClientMod.mouseButtonMask |= bit;
            ClientMod.mouseButtonPressedLatch |= bit;
        } else if (action == 0) {
            // Button released: clear the bit from the current mask.
            ClientMod.mouseButtonMask &= ~bit;
        }
    }
}

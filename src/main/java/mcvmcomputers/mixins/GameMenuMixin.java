package mcvmcomputers.mixins;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class GameMenuMixin extends Screen {

    private static final Identifier PACKAGE_ICON = new Identifier("mcvmcomputers", "textures/gui/package_button.png");

    private ButtonWidget customizationButton;

    protected GameMenuMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("RETURN"))
    private void addCustomizationButton(CallbackInfo ci) {
        // In 1.20.1 OptionsScreen the manually-placed buttons start at:
        // y = this.height / 6 - 12
        // Each row is 24 px apart. "Chat Settings..." is in row index 2
        // (Skin Customisation=0, Music&Sounds=0 | Video=1, Controls=1 | Language=2,
        // Chat=2).
        // Right-column buttons begin at centerX + 5 and are 150 px wide.
        // Our small square sits 4 px to the right of the right-column button's edge.

        int startY = this.height / 6 - 12;
        int chatRow = 4; // 0-based row of Chat Settings
        int btnY = startY + chatRow * 24 + 6;

        int btnX = this.width / 2 + 5 + 150 + 4; // right of right-column button

        customizationButton = ButtonWidget.builder(Text.empty(), button -> {
            // action placeholder
        }).dimensions(btnX, btnY, 20, 20).build();

        this.addDrawableChild(customizationButton);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void renderCustomizationButton(DrawContext context, int mouseX, int mouseY,
            float delta, CallbackInfo ci) {
        if (customizationButton == null)
            return;

        int bx = customizationButton.getX();
        int by = customizationButton.getY();

        // Draw 16x16 icon centred inside the 20x20 button
        context.drawTexture(PACKAGE_ICON, bx + 2, by + 2, 0, 0, 16, 16, 16, 16);

        // Tooltip on hover
        if (mouseX >= bx && mouseX <= bx + 20 && mouseY >= by && mouseY <= by + 20) {
            context.drawTooltip(this.textRenderer,
                    Text.literal("Customization"), mouseX, mouseY);
        }
    }
}

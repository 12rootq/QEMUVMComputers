package mcvmcomputers.mixins;

// Новые импорты для 1.20.1
import org.joml.Matrix4f;
import net.minecraft.util.math.RotationAxis;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.item.ItemOrderingTablet;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

@Mixin(HeldItemRenderer.class)
public class HeldItemMixin {
	@Shadow
	private void renderArm(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Arm arm) {}

	@Inject(at = @At("HEAD"), method = "renderFirstPersonItem")
	private void renderItemHead(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if(!item.isEmpty()) {
			if(item.getItem() instanceof ItemOrderingTablet) {
				matrices.push();
				matrices.translate(0, -equipProgress*2, 0);
				matrices.push();
				matrices.translate(0, 0.1, 0.38);

				// В 1.20.1 вместо new Quaternion используется RotationAxis
				matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));

				// Цвета в 1.20.1 теперь передаются как float (1.0F), а не int (0)
				ClientMod.tabletOS.orderingTabletModel.render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(new Identifier("mcvmcomputers", "textures/entity/tablet.png"))), light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F);

				if(ClientMod.tabletOS.textureIdentifier != null) {
					matrices.push();
					matrices.scale(0.19f, 0.19f, 0.19f);
					matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
					matrices.translate(-1.65, -1.65, 7.57);

					// В 1.20.1 метод getModel() переименован в getPositionMatrix()
					Matrix4f matrix4f = matrices.peek().getPositionMatrix();
					VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getText(ClientMod.tabletOS.textureIdentifier));

					vertexConsumer.vertex(matrix4f, 0.0F, 3.3F, -0.01F).color(255, 255, 255, 255).texture(0.0F, 1.0F).light(15728640).next();
					vertexConsumer.vertex(matrix4f, 3.3F, 3.3F, -0.01F).color(255, 255, 255, 255).texture(1.0F, 1.0F).light(15728640).next();
					vertexConsumer.vertex(matrix4f, 3.3F, 0.0F, -0.01F).color(255, 255, 255, 255).texture(1.0F, 0.0F).light(15728640).next();
					vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, -0.01F).color(255, 255, 255, 255).texture(0.0F, 0.0F).light(15728640).next();
					matrices.pop();
				}
				matrices.pop();
				matrices.push();
				boolean b = hand == Hand.MAIN_HAND;
				Arm arm = b ? Arm.RIGHT : Arm.LEFT;

				// В 1.20.1 вместо Vector3f.POSITIVE_Y используется RotationAxis
				matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
				matrices.translate(1.03, 0.20, 0);
				this.renderArm(matrices, vertexConsumers, light, arm);
				matrices.pop();
				matrices.pop();
			}
		}
		matrices.push();
	}

	@Inject(at = @At("TAIL"), method = "renderFirstPersonItem")
	private void renderItemTail(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		matrices.pop();
	}
}
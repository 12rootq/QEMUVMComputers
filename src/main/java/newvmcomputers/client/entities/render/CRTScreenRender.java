package newvmcomputers.client.entities.render;

import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import newvmcomputers.client.ClientMod;
import newvmcomputers.entities.EntityCRTScreen;
import newvmcomputers.item.ItemList;
import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

public class CRTScreenRender extends EntityRenderer<EntityCRTScreen> {

	public CRTScreenRender(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityCRTScreen entity) {
		return null; 
	}

	@Override
	public void render(EntityCRTScreen entity, float entityYaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource vertexConsumers, int light) {
		if (entity.getOwnerUUID().isEmpty()) {
			return;
		}

		matrices.pushPose();
		matrices.translate(0.0, 0.5, 0.0);

		Quaternionf lookRotation = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(lookRotation);

		ItemStack screenStack = new ItemStack(ItemList.ITEM_CRTSCREEN.get());
		Minecraft.getInstance().getItemRenderer().renderStatic(
				screenStack,
				ItemDisplayContext.NONE,
				light,
				OverlayTexture.NO_OVERLAY,
				matrices,
				vertexConsumers,
				entity.level(),
				entity.getId()
		);

		UUID ownerUUID = UUID.fromString(entity.getOwnerUUID());
		ResourceLocation screenTexture = ClientMod.vmScreenTextures.get(ownerUUID);
		if (screenTexture != null) {
			matrices.pushPose();

			matrices.scale(0.006f, 0.006f, 0.006f);
			matrices.mulPose(new Quaternionf().rotateX((float) Math.toRadians(22.5f)));
			matrices.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(180.0f)));
			matrices.translate(-63.1f, -27.7f, -20.0f);
			matrices.scale(0.736f, 0.597f, 1.0f);
			matrices.translate(22.0f, 1.6f, 7.6f);

			Matrix4f positionMatrix = matrices.last().pose();
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(screenTexture));

			drawQuad(vertexConsumer, positionMatrix, matrices, light);

			matrices.popPose();
		}

		matrices.popPose();
	}

	private void drawQuad(VertexConsumer vertexConsumer, Matrix4f matrix, PoseStack matrices, int light) {
		float z = -0.01f;
		vertex(vertexConsumer, matrix, matrices, 0.0f,   128.0f, z, 0.0f, 1.0f, light);
		vertex(vertexConsumer, matrix, matrices, 128.0f, 128.0f, z, 1.0f, 1.0f, light);
		vertex(vertexConsumer, matrix, matrices, 128.0f, 0.0f,   z, 1.0f, 0.0f, light);
		vertex(vertexConsumer, matrix, matrices, 0.0f,   0.0f,   z, 0.0f, 0.0f, light);
	}

	private void vertex(VertexConsumer vertexConsumer, Matrix4f matrix, PoseStack matrices,
						float x, float y, float z, float u, float v, int light) {
		vertexConsumer.vertex(matrix, x, y, z)
				.color(255, 255, 255, 255)
				.uv(u, v)
				.overlayCoords(OverlayTexture.NO_OVERLAY)
				.uv2(light)
				.normal(matrices.last().normal(), 0.0f, 0.0f, -1.0f)
				.endVertex();
	}
}




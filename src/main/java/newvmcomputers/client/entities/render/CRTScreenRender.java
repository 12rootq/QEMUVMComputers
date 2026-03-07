package newvmcomputers.client.entities.render;

import java.util.UUID;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import newvmcomputers.client.ClientMod;
import newvmcomputers.entities.EntityCRTScreen;
import newvmcomputers.item.ItemList;
import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class CRTScreenRender extends EntityRenderer<EntityCRTScreen> {

	public CRTScreenRender(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(EntityCRTScreen entity) {
		return null; 
	}

	@Override
	public void render(EntityCRTScreen entity, float entityYaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {
		if (entity.getOwnerUUID().isEmpty()) {
			return;
		}

		matrices.push();
		matrices.translate(0.0, 0.5, 0.0);

		Quaternionf lookRotation = MVCUtils.lookAt(entity.getPos(), entity.getLookAtPos());
		matrices.multiply(lookRotation);

		ItemStack screenStack = new ItemStack(ItemList.ITEM_CRTSCREEN);
		MinecraftClient.getInstance().getItemRenderer().renderItem(
				screenStack,
				ModelTransformationMode.NONE,
				light,
				OverlayTexture.DEFAULT_UV,
				matrices,
				vertexConsumers,
				entity.getWorld(),
				entity.getId()
		);

		UUID ownerUUID = UUID.fromString(entity.getOwnerUUID());
		Identifier screenTexture = ClientMod.vmScreenTextures.get(ownerUUID);
		if (screenTexture != null) {
			matrices.push();

			matrices.scale(0.006f, 0.006f, 0.006f);
			matrices.multiply(new Quaternionf().rotateX((float) Math.toRadians(22.5f)));
			matrices.multiply(new Quaternionf().rotateZ((float) Math.toRadians(180.0f)));
			matrices.translate(-63.1f, -27.7f, -20.0f);
			matrices.scale(0.736f, 0.597f, 1.0f);
			matrices.translate(22.0f, 1.6f, 7.6f);

			Matrix4f positionMatrix = matrices.peek().getPositionMatrix();
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(screenTexture));

			drawQuad(vertexConsumer, positionMatrix, matrices, light);

			matrices.pop();
		}

		matrices.pop();
	}

	private void drawQuad(VertexConsumer vertexConsumer, Matrix4f matrix, MatrixStack matrices, int light) {
		float z = -0.01f;
		vertex(vertexConsumer, matrix, matrices, 0.0f,   128.0f, z, 0.0f, 1.0f, light);
		vertex(vertexConsumer, matrix, matrices, 128.0f, 128.0f, z, 1.0f, 1.0f, light);
		vertex(vertexConsumer, matrix, matrices, 128.0f, 0.0f,   z, 1.0f, 0.0f, light);
		vertex(vertexConsumer, matrix, matrices, 0.0f,   0.0f,   z, 0.0f, 0.0f, light);
	}

	private void vertex(VertexConsumer vertexConsumer, Matrix4f matrix, MatrixStack matrices,
						float x, float y, float z, float u, float v, int light) {
		vertexConsumer.vertex(matrix, x, y, z)
				.color(255, 255, 255, 255)
				.texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV)
				.light(light)
				.normal(matrices.peek().getNormalMatrix(), 0.0f, 0.0f, -1.0f)
				.next();
	}
}
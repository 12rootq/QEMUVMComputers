package newvmcomputers.client.entities.render;

import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import newvmcomputers.client.ClientMod;
import newvmcomputers.entities.EntityWallTV;
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
import net.minecraft.util.math.RotationAxis;

public class WallTVRender extends EntityRenderer<EntityWallTV> {

	public WallTVRender(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public Identifier getTexture(EntityWallTV entity) {
		return null;
	}

	@Override
	public void render(EntityWallTV entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {

		if (entity.getOwnerUUID() == null || entity.getOwnerUUID().isEmpty()) {
			return;
		}

		matrices.push();
		matrices.translate(0, 0.5, 0);

		
		Quaternionf look = MVCUtils.lookAt(entity.getPos(), entity.getLookAtPos());
		matrices.multiply(look);

		matrices.push();
		matrices.translate(0, 0, -0.1);

		
		MinecraftClient.getInstance().getItemRenderer().renderItem(
				new ItemStack(ItemList.ITEM_WALLTV),
				ModelTransformationMode.NONE,
				light,
				OverlayTexture.DEFAULT_UV,
				matrices,
				vertexConsumers,
				entity.getWorld(),
				entity.getId()
		);
		matrices.pop();

		UUID ownerUuid = UUID.fromString(entity.getOwnerUUID());
		if (ClientMod.vmScreenTextures.containsKey(ownerUuid)) {
			matrices.push();
			matrices.scale(0.0198f, 0.014f, 0.006f);

			
			matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));

			matrices.translate(-63.1f, -45.7f, -24.4f);
			matrices.scale(0.736f, 0.597f, 1f);
			matrices.translate(22, 1.6f, 7.6f);

			
			Matrix4f matrix4f = matrices.peek().getPositionMatrix();
			Identifier screenTex = ClientMod.vmScreenTextures.get(ownerUuid);

			
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(screenTex));

			
			vertexConsumer.vertex(matrix4f, 0.0F, 128.0F, -0.01F).color(255, 255, 255, 255).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1).next();
			vertexConsumer.vertex(matrix4f, 128.0F, 128.0F, -0.01F).color(255, 255, 255, 255).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1).next();
			vertexConsumer.vertex(matrix4f, 128.0F, 0.0F, -0.01F).color(255, 255, 255, 255).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1).next();
			vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, -0.01F).color(255, 255, 255, 255).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0, 0, 1).next();

			matrices.pop();
		}
		matrices.pop();

		
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}
package newvmcomputers.client.entities.render;

import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import newvmcomputers.client.ClientMod;
import newvmcomputers.entities.EntityWallTV;
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
import com.mojang.math.Axis;

public class WallTVRender extends EntityRenderer<EntityWallTV> {

	public WallTVRender(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityWallTV entity) {
		return null;
	}

	@Override
	public void render(EntityWallTV entity, float yaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource vertexConsumers, int light) {

		if (entity.getOwnerUUID() == null || entity.getOwnerUUID().isEmpty()) {
			return;
		}

		matrices.pushPose();
		matrices.translate(0, 0.5, 0);

		
		Quaternionf look = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(look);

		matrices.pushPose();
		matrices.translate(0, 0, -0.1);

		
		Minecraft.getInstance().getItemRenderer().renderStatic(
				new ItemStack(ItemList.ITEM_WALLTV.get()),
				ItemDisplayContext.NONE,
				light,
				OverlayTexture.NO_OVERLAY,
				matrices,
				vertexConsumers,
				entity.level(),
				entity.getId()
		);
		matrices.popPose();

		UUID ownerUuid = UUID.fromString(entity.getOwnerUUID());
		if (ClientMod.vmScreenTextures.containsKey(ownerUuid)) {
			matrices.pushPose();
			matrices.scale(0.0198f, 0.014f, 0.006f);

			
			matrices.mulPose(Axis.ZP.rotationDegrees(180f));

			matrices.translate(-63.1f, -45.7f, -24.4f);
			matrices.scale(0.736f, 0.597f, 1f);
			matrices.translate(22, 1.6f, 7.6f);

			
			Matrix4f matrix4f = matrices.last().pose();
			ResourceLocation screenTex = ClientMod.vmScreenTextures.get(ownerUuid);

			
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(screenTex));

			
			vertexConsumer.vertex(matrix4f, 0.0F, 128.0F, -0.01F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
			vertexConsumer.vertex(matrix4f, 128.0F, 128.0F, -0.01F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
			vertexConsumer.vertex(matrix4f, 128.0F, 0.0F, -0.01F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();
			vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, -0.01F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(0, 0, 1).endVertex();

			matrices.popPose();
		}
		matrices.popPose();

		
		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}




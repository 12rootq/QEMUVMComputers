package newvmcomputers.client.entities.render;

import java.util.UUID;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import newvmcomputers.client.ClientMod;
import newvmcomputers.entities.EntityFlatScreen;
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

public class FlatScreenRender extends EntityRenderer<EntityFlatScreen> {

	public FlatScreenRender(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityFlatScreen entity) {
		return null;
	}

	@Override
	public void render(EntityFlatScreen entity, float yaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource vertexConsumers, int light) {
		if (entity.getOwnerUUID() == null || entity.getOwnerUUID().isEmpty()) {
			return;
		}

		matrices.pushPose();
		matrices.translate(0, 0.5, 0);

		Quaternionf look = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(look);

		Minecraft.getInstance().getItemRenderer().renderStatic(
				new ItemStack(ItemList.ITEM_FLATSCREEN.get()),
				ItemDisplayContext.NONE,
				light,
				OverlayTexture.NO_OVERLAY,
				matrices,
				vertexConsumers,
				entity.level(),
				entity.getId());

		UUID ownerUuid = UUID.fromString(entity.getOwnerUUID());
		if (ClientMod.vmScreenTextures.containsKey(ownerUuid)) {
			matrices.pushPose();
			matrices.scale(0.006f, 0.006f, 0.006f);

			matrices.mulPose(Axis.ZP.rotationDegrees(180f));

			matrices.translate(-59.4f, -18.3f, -13f);
			matrices.scale(0.8079f, 0.488f, 1f);
			matrices.translate(10, 5.4f, 7.76f);

			Matrix4f matrix4f = matrices.last().pose();
			ResourceLocation screenTex = ClientMod.vmScreenTextures.get(ownerUuid);

			
			VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(screenTex));

			
			
			vertexConsumer.vertex(matrix4f, 0.0F, 128.0F, -0.01F).color(255, 255, 255, 255).uv(0.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
			vertexConsumer.vertex(matrix4f, 128.0F, 128.0F, -0.01F).color(255, 255, 255, 255).uv(1.0F, 1.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
			vertexConsumer.vertex(matrix4f, 128.0F, 0.0F, -0.01F).color(255, 255, 255, 255).uv(1.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();
			vertexConsumer.vertex(matrix4f, 0.0F, 0.0F, -0.01F).color(255, 255, 255, 255).uv(0.0F, 0.0F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0, 0, 1).endVertex();

			matrices.popPose();
		}
		matrices.popPose();
	}
}




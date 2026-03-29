package newvmcomputers.client.entities.render;

import org.joml.Quaternionf;

import newvmcomputers.entities.EntityItemPreview;
import newvmcomputers.item.PlacableOrderableItem;
import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class ItemPreviewRender extends EntityRenderer<EntityItemPreview> {

	public ItemPreviewRender(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityItemPreview entity) {
		return null;
	}

	@Override
	public void render(EntityItemPreview entity, float yaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource vertexConsumers, int light) {

		Minecraft mcc = Minecraft.getInstance();

		
		if (mcc.player == null) {
			return;
		}

		matrices.pushPose();
		matrices.translate(0, 0.5, 0);

		Vec3 v = mcc.player.position();
		
		Quaternionf look = MVCUtils.lookAt(entity.position(), new Vec3(v.x, entity.getY(), v.z));
		matrices.mulPose(look);

		matrices.pushPose();

		
		if (entity.getPreviewedItemStack().getItem() instanceof PlacableOrderableItem placableItem) {
			if (placableItem.wallTV) {
				matrices.translate(0, 0, -0.1);
			}
		}

		
		
		Minecraft.getInstance().getItemRenderer().renderStatic(
				entity.getPreviewedItemStack(),
				ItemDisplayContext.NONE,
				15728880,
				OverlayTexture.NO_OVERLAY,
				matrices,
				vertexConsumers,
				entity.level(),
				entity.getId()
		);

		matrices.popPose();
		matrices.popPose();

		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}



package mcvmcomputers.client.entities.render;
import net.minecraft.world.entity.player.Player;


import mcvmcomputers.entities.EntityItemPreview;
import mcvmcomputers.item.PlacableOrderableItem;
import mcvmcomputers.utils.MVCUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;



import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;


public class ItemPreviewRender extends EntityRenderer<EntityItemPreview>{
	public ItemPreviewRender(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityItemPreview entity) {
		return null;
	}

	@Override
	public void render(EntityItemPreview entity, float yaw, float tickDelta, PoseStack matrices,
			MultiBufferSource VertexConsumers, int light) {
		Minecraft mcc = Minecraft.getInstance();

		matrices.pushPose();

		matrices.translate(0, 0.5, 0);
		Vec3 v = mcc.player.position();
		Quaternionf look = MVCUtils.lookAt(entity.position(), new Vec3(v.x, entity.getY(), v.z));
		matrices.mulPose(look);
		matrices.pushPose();
		if(entity.getPreviewedItemStack().getItem() instanceof PlacableOrderableItem) {
			if(((PlacableOrderableItem)entity.getPreviewedItemStack().getItem()).wallTV) {
				matrices.translate(0, 0, -0.1);
			}
		}
		Minecraft.getInstance().getItemRenderer().renderStatic(entity.getPreviewedItemStack(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
		matrices.popPose();
		matrices.popPose();
	}
}

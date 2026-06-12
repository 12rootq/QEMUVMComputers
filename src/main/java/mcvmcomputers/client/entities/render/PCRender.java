package mcvmcomputers.client.entities.render;

import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.utils.MVCUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;




import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

public class PCRender extends EntityRenderer<EntityPC>{
	public PCRender(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityPC entity) {
		return null;
	}

	@Override
	public void render(EntityPC entity, float yaw, float tickDelta, PoseStack matrices,
			MultiBufferSource VertexConsumers, int light) {
		matrices.pushPose();
		matrices.translate(0, 0.5, 0);
		Quaternionf look = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(look);
		Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.PC_CASE_NO_PANEL), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);

		matrices.pushPose();
		matrices.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(-90)));
		matrices.mulPose(new Quaternionf().rotationY((float)Math.toRadians(-90)));

		matrices.scale(0.55f, 0.55f, 0.55f);
		matrices.translate(0.06f, 0.28f, -0.29f);
		if(entity.getMotherboardInstalled()) {
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_MOTHERBOARD), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
		}
		if(entity.getGpuInstalled()) {
			matrices.pushPose();
			matrices.translate(0.24, 0.07f, -0.28f);
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_GPU), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
			matrices.popPose();
		}
		if(entity.getCpuDividedBy() > 0) {
			matrices.pushPose();
			matrices.translate(0.06, 0.12f, 0.06f);
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_CPU2), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
			matrices.popPose();
		}
		if(entity.getGigsOfRamInSlot0() > 0) {
			matrices.pushPose();
			matrices.mulPose(new Quaternionf().rotationY((float)Math.toRadians(90)));
			matrices.translate(-0.22f, 0.1f, -0.285f);
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_RAM1G), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
			matrices.popPose();
		}
		if(entity.getGigsOfRamInSlot1() > 0) {
			matrices.pushPose();
			matrices.mulPose(new Quaternionf().rotationY((float)Math.toRadians(90)));
			matrices.translate(-0.22f, 0.1f, -0.404f);
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_RAM1G), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
			matrices.popPose();
		}
		if(!entity.getHardDriveFileName().isEmpty()) {
			matrices.pushPose();
			matrices.mulPose(new Quaternionf().rotationX((float)Math.toRadians(90)));
			matrices.mulPose(new Quaternionf().rotationY((float)Math.toRadians(90)));
			matrices.translate(-0.2f, 0, -0.6);
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_HARDDRIVE), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
			matrices.popPose();
		}
		matrices.popPose();

		if(entity.getGlassSidepanel()) {
			matrices.pushPose();
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.PC_CASE_GLASS_PANEL), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
			matrices.popPose();
		}else {
			matrices.pushPose();
			Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.PC_CASE_ONLY_PANEL), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
			matrices.popPose();
		}
		matrices.popPose();
	}

}

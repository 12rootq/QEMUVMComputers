package newvmcomputers.client.entities.render;

import org.joml.Quaternionf;
import newvmcomputers.entities.EntityPC;
import newvmcomputers.item.ItemList;
import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;

public class PCRender extends EntityRenderer<EntityPC> {

	public PCRender(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityPC entity) {
		return null;
	}

	private void renderItem(ItemStack stack, PoseStack matrices, MultiBufferSource vertexConsumers, int light, EntityPC entity) {
		Minecraft.getInstance().getItemRenderer().renderStatic(
				stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
				matrices, vertexConsumers, entity.level(), entity.getId()
		);
	}

	@Override
	public void render(EntityPC entity, float yaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource vertexConsumers, int light) {
		matrices.pushPose();
		matrices.translate(0, 0.5, 0);

		Quaternionf look = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(look);

		renderItem(new ItemStack(ItemList.PC_CASE_NO_PANEL.get()), matrices, vertexConsumers, light, entity);

		matrices.pushPose();
		
		matrices.mulPose(Axis.ZP.rotationDegrees(-90f));
		matrices.mulPose(Axis.YP.rotationDegrees(-90f));
		matrices.scale(0.55f, 0.55f, 0.55f);
		matrices.translate(0.06f, 0.28f, -0.29f);

		if (entity.getMotherboardInstalled()) {
			renderItem(new ItemStack(ItemList.ITEM_MOTHERBOARD.get()), matrices, vertexConsumers, light, entity);
		}
		if (entity.getGpuInstalled()) {
			matrices.pushPose();
			matrices.translate(0.24, 0.07f, -0.28f);
			renderItem(new ItemStack(ItemList.ITEM_GPU.get()), matrices, vertexConsumers, light, entity);
			matrices.popPose();
		}
		if (entity.getCpuDividedBy() > 0) {
			matrices.pushPose();
			matrices.translate(0.06, 0.12f, 0.06f);
			renderItem(new ItemStack(ItemList.ITEM_CPU2.get()), matrices, vertexConsumers, light, entity);
			matrices.popPose();
		}
		if (entity.getGigsOfRamInSlot0() > 0) {
			matrices.pushPose();
			matrices.mulPose(Axis.YP.rotationDegrees(90f));
			matrices.translate(-0.22f, 0.1f, -0.285f);
			renderItem(new ItemStack(ItemList.ITEM_RAM1G.get()), matrices, vertexConsumers, light, entity);
			matrices.popPose();
		}
		if (entity.getGigsOfRamInSlot1() > 0) {
			matrices.pushPose();
			matrices.mulPose(Axis.YP.rotationDegrees(90f));
			matrices.translate(-0.22f, 0.1f, -0.404f);
			renderItem(new ItemStack(ItemList.ITEM_RAM1G.get()), matrices, vertexConsumers, light, entity);
			matrices.popPose();
		}
		if (!entity.getHardDriveFileName().isEmpty()) {
			matrices.pushPose();
			matrices.mulPose(Axis.XP.rotationDegrees(90f));
			matrices.mulPose(Axis.YP.rotationDegrees(90f));
			matrices.translate(-0.2f, 0, -0.6);
			renderItem(new ItemStack(ItemList.ITEM_HARDDRIVE.get()), matrices, vertexConsumers, light, entity);
			matrices.popPose();
		}
		matrices.popPose();

		if (entity.getGlassSidepanel()) {
			renderItem(new ItemStack(ItemList.PC_CASE_GLASS_PANEL.get()), matrices, vertexConsumers, light, entity);
		} else {
			renderItem(new ItemStack(ItemList.PC_CASE_ONLY_PANEL.get()), matrices, vertexConsumers, light, entity);
		}
		matrices.popPose();

		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}



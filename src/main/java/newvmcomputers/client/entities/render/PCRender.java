package newvmcomputers.client.entities.render;

import org.joml.Quaternionf;
import newvmcomputers.entities.EntityPC;
import newvmcomputers.item.ItemList;
import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class PCRender extends EntityRenderer<EntityPC> {

	public PCRender(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public Identifier getTexture(EntityPC entity) {
		return null;
	}

	private void renderItem(ItemStack stack, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityPC entity) {
		MinecraftClient.getInstance().getItemRenderer().renderItem(
				stack, ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV,
				matrices, vertexConsumers, entity.getWorld(), entity.getId()
		);
	}

	@Override
	public void render(EntityPC entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.translate(0, 0.5, 0);

		Quaternionf look = MVCUtils.lookAt(entity.getPos(), entity.getLookAtPos());
		matrices.multiply(look);

		renderItem(new ItemStack(ItemList.PC_CASE_NO_PANEL), matrices, vertexConsumers, light, entity);

		matrices.push();
		
		matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90f));
		matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90f));
		matrices.scale(0.55f, 0.55f, 0.55f);
		matrices.translate(0.06f, 0.28f, -0.29f);

		if (entity.getMotherboardInstalled()) {
			renderItem(new ItemStack(ItemList.ITEM_MOTHERBOARD), matrices, vertexConsumers, light, entity);
		}
		if (entity.getGpuInstalled()) {
			matrices.push();
			matrices.translate(0.24, 0.07f, -0.28f);
			renderItem(new ItemStack(ItemList.ITEM_GPU), matrices, vertexConsumers, light, entity);
			matrices.pop();
		}
		if (entity.getCpuDividedBy() > 0) {
			matrices.push();
			matrices.translate(0.06, 0.12f, 0.06f);
			renderItem(new ItemStack(ItemList.ITEM_CPU2), matrices, vertexConsumers, light, entity);
			matrices.pop();
		}
		if (entity.getGigsOfRamInSlot0() > 0) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
			matrices.translate(-0.22f, 0.1f, -0.285f);
			renderItem(new ItemStack(ItemList.ITEM_RAM1G), matrices, vertexConsumers, light, entity);
			matrices.pop();
		}
		if (entity.getGigsOfRamInSlot1() > 0) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
			matrices.translate(-0.22f, 0.1f, -0.404f);
			renderItem(new ItemStack(ItemList.ITEM_RAM1G), matrices, vertexConsumers, light, entity);
			matrices.pop();
		}
		if (!entity.getHardDriveFileName().isEmpty()) {
			matrices.push();
			matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
			matrices.translate(-0.2f, 0, -0.6);
			renderItem(new ItemStack(ItemList.ITEM_HARDDRIVE), matrices, vertexConsumers, light, entity);
			matrices.pop();
		}
		matrices.pop();

		if (entity.getGlassSidepanel()) {
			renderItem(new ItemStack(ItemList.PC_CASE_GLASS_PANEL), matrices, vertexConsumers, light, entity);
		} else {
			renderItem(new ItemStack(ItemList.PC_CASE_ONLY_PANEL), matrices, vertexConsumers, light, entity);
		}
		matrices.pop();

		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}
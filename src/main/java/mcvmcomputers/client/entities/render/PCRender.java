package mcvmcomputers.client.entities.render;

import mcvmcomputers.entities.EntityPC;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.utils.MVCUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;

public class PCRender extends EntityRenderer<EntityPC>{
	public PCRender(EntityRendererFactory.Context context) {
		super(context);
	}

	@Override
	public Identifier getTexture(EntityPC entity) {
		return null;
	}
	
	@Override
	public void render(EntityPC entity, float yaw, float tickDelta, MatrixStack matrices,
			VertexConsumerProvider vertexConsumers, int light) {
		matrices.push();
		matrices.translate(0, 0.5, 0);
		Quaternionf look = MVCUtils.lookAt(entity.getPos(), entity.getLookAtPos());
		matrices.multiply(look);
		MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.PC_CASE_NO_PANEL), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
		
		matrices.push();
		matrices.multiply(new Quaternionf().rotationZ((float)Math.toRadians(-90)));
		matrices.multiply(new Quaternionf().rotationY((float)Math.toRadians(-90)));
		
		matrices.scale(0.55f, 0.55f, 0.55f);
		matrices.translate(0.06f, 0.28f, -0.29f);
		if(entity.getMotherboardInstalled()) {
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.ITEM_MOTHERBOARD), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
		}
		if(entity.getGpuInstalled()) {
			matrices.push();
			matrices.translate(0.24, 0.07f, -0.28f);
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.ITEM_GPU), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}
		if(entity.getCpuDividedBy() > 0) {
			matrices.push();
			matrices.translate(0.06, 0.12f, 0.06f);
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.ITEM_CPU2), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}
		if(entity.getGigsOfRamInSlot0() > 0) {
			matrices.push();
			matrices.multiply(new Quaternionf().rotationY((float)Math.toRadians(90)));
			matrices.translate(-0.22f, 0.1f, -0.285f);
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.ITEM_RAM1G), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}
		if(entity.getGigsOfRamInSlot1() > 0) {
			matrices.push();
			matrices.multiply(new Quaternionf().rotationY((float)Math.toRadians(90)));
			matrices.translate(-0.22f, 0.1f, -0.404f);
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.ITEM_RAM1G), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}
		if(!entity.getHardDriveFileName().isEmpty()) {
			matrices.push();
			matrices.multiply(new Quaternionf().rotationX((float)Math.toRadians(90)));
			matrices.multiply(new Quaternionf().rotationY((float)Math.toRadians(90)));
			matrices.translate(-0.2f, 0, -0.6);
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.ITEM_HARDDRIVE), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}
		matrices.pop();
		
		if(entity.getGlassSidepanel()) {
			matrices.push();
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.PC_CASE_GLASS_PANEL), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}else {
			matrices.push();
			MinecraftClient.getInstance().getItemRenderer().renderItem(new ItemStack(ItemList.PC_CASE_ONLY_PANEL), ModelTransformationMode.NONE, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers, entity.getWorld(), 0);
			matrices.pop();
		}
		matrices.pop();
	}

}

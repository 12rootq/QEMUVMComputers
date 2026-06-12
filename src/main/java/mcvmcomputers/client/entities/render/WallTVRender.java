package mcvmcomputers.client.entities.render;

import java.util.UUID;


import mcvmcomputers.client.ClientMod;
import mcvmcomputers.entities.EntityWallTV;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.utils.MVCUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;




import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class WallTVRender extends EntityRenderer<EntityWallTV>{


	public WallTVRender(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityWallTV entity) {
		return null;
	}

	@Override
	public void render(EntityWallTV entity, float yaw, float tickDelta, PoseStack matrices,
			MultiBufferSource VertexConsumers, int light) {
		if(entity.getOwnerUUID().isEmpty()) {
			return;
		}

		matrices.pushPose();
		matrices.translate(0, 0.5, 0);
		Quaternionf look = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(look);
		matrices.pushPose();
		matrices.translate(0, 0, -0.1);
		Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_WALLTV), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
		matrices.popPose();
		if(ClientMod.vmScreenTextures.containsKey(UUID.fromString(entity.getOwnerUUID()))) {
			matrices.pushPose();
			matrices.scale(0.0198f, 0.014f, 0.006f);
			matrices.mulPose(new Quaternionf().rotationZ((float)Math.toRadians(180)));
			matrices.translate(-63.1f, -45.7f, -24.4f);
			matrices.scale(0.736f, 0.597f, 1f);
			matrices.translate(22, 1.6f, 7.6f);
			Matrix4f matrix4f = matrices.last().pose();
			VertexConsumer VertexConsumer = VertexConsumers.getBuffer(RenderType.text(ClientMod.vmScreenTextures.get(UUID.fromString(entity.getOwnerUUID()))));
			VertexConsumer.addVertex(matrix4f, 0.0F, 128.0F, -0.01F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(light);
	        VertexConsumer.addVertex(matrix4f, 128.0F, 128.0F, -0.01F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(light);
	        VertexConsumer.addVertex(matrix4f, 128.0F, 0.0F, -0.01F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(light);
	        VertexConsumer.addVertex(matrix4f, 0.0F, 0.0F, -0.01F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(light);
			matrices.popPose();
		}
		matrices.popPose();
	}

}

package newvmcomputers.client.entities.render;

import org.joml.Quaternionf;

import newvmcomputers.entities.EntityKeyboard;
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

public class KeyboardRender extends EntityRenderer<EntityKeyboard> {

	public KeyboardRender(EntityRendererProvider.Context ctx) {
		super(ctx);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityKeyboard entity) {
		return null;
	}

	@Override
	public void render(EntityKeyboard entity, float yaw, float tickDelta, PoseStack matrices,
					   MultiBufferSource vertexConsumers, int light) {

		Minecraft mcc = Minecraft.getInstance();

		
		if (mcc.player == null) {
			return;
		}

		matrices.pushPose();
		matrices.translate(0, 0.5, 0);

		
		Quaternionf look = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(look);

		
		
		Minecraft.getInstance().getItemRenderer().renderStatic(
				new ItemStack(ItemList.ITEM_KEYBOARD.get()),
				ItemDisplayContext.NONE,
				15728880, 
				OverlayTexture.NO_OVERLAY,
				matrices,
				vertexConsumers,
				entity.level(),
				entity.getId()
		);

		matrices.popPose();

		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}



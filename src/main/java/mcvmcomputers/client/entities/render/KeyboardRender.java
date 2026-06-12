package mcvmcomputers.client.entities.render;

import mcvmcomputers.entities.EntityKeyboard;
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

public class KeyboardRender extends EntityRenderer<EntityKeyboard>{
	public KeyboardRender(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public ResourceLocation getTextureLocation(EntityKeyboard entity) {
		return null;
	}

	@Override
	public void render(EntityKeyboard entity, float yaw, float tickDelta, PoseStack matrices,
			MultiBufferSource VertexConsumers, int light) {
		matrices.pushPose();
		matrices.translate(0, 0.5, 0);
		Quaternionf look = MVCUtils.lookAt(entity.position(), entity.getLookAtPos());
		matrices.mulPose(look);
		Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ItemList.ITEM_KEYBOARD), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, VertexConsumers, entity.level(), 0);
		matrices.popPose();
	}

}

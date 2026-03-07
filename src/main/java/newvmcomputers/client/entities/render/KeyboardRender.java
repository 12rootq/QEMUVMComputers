package newvmcomputers.client.entities.render;

import org.joml.Quaternionf;

import newvmcomputers.entities.EntityKeyboard;
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

public class KeyboardRender extends EntityRenderer<EntityKeyboard> {

	public KeyboardRender(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public Identifier getTexture(EntityKeyboard entity) {
		return null;
	}

	@Override
	public void render(EntityKeyboard entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {

		MinecraftClient mcc = MinecraftClient.getInstance();

		
		if (mcc.player == null) {
			return;
		}

		matrices.push();
		matrices.translate(0, 0.5, 0);

		
		Quaternionf look = MVCUtils.lookAt(entity.getPos(), entity.getLookAtPos());
		matrices.multiply(look);

		
		
		MinecraftClient.getInstance().getItemRenderer().renderItem(
				new ItemStack(ItemList.ITEM_KEYBOARD),
				ModelTransformationMode.NONE,
				15728880, 
				OverlayTexture.DEFAULT_UV,
				matrices,
				vertexConsumers,
				entity.getWorld(),
				entity.getId()
		);

		matrices.pop();

		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}
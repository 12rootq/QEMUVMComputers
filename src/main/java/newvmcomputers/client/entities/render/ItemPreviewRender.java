package newvmcomputers.client.entities.render;

import org.joml.Quaternionf;

import newvmcomputers.entities.EntityItemPreview;
import newvmcomputers.item.PlacableOrderableItem;
import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class ItemPreviewRender extends EntityRenderer<EntityItemPreview> {

	public ItemPreviewRender(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public Identifier getTexture(EntityItemPreview entity) {
		return null;
	}

	@Override
	public void render(EntityItemPreview entity, float yaw, float tickDelta, MatrixStack matrices,
					   VertexConsumerProvider vertexConsumers, int light) {

		MinecraftClient mcc = MinecraftClient.getInstance();

		
		if (mcc.player == null) {
			return;
		}

		matrices.push();
		matrices.translate(0, 0.5, 0);

		Vec3d v = mcc.player.getPos();
		
		Quaternionf look = MVCUtils.lookAt(entity.getPos(), new Vec3d(v.x, entity.getY(), v.z));
		matrices.multiply(look);

		matrices.push();

		
		if (entity.getPreviewedItemStack().getItem() instanceof PlacableOrderableItem placableItem) {
			if (placableItem.wallTV) {
				matrices.translate(0, 0, -0.1);
			}
		}

		
		
		MinecraftClient.getInstance().getItemRenderer().renderItem(
				entity.getPreviewedItemStack(),
				ModelTransformationMode.NONE,
				15728880,
				OverlayTexture.DEFAULT_UV,
				matrices,
				vertexConsumers,
				entity.getWorld(),
				entity.getId()
		);

		matrices.pop();
		matrices.pop();

		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}
}
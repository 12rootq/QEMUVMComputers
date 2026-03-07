package newvmcomputers.client.entities.model;

import newvmcomputers.utils.MVCUtils;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;

public class OrderingTabletModel extends EntityModel<Entity> {
	private final ModelPart tablet;
	private final ModelPart buttons;
	private final ModelPart up;
	private final ModelPart down;
	private final ModelPart left;
	private final ModelPart right;
	private final ModelPart enter;

	public OrderingTabletModel(ModelPart root) {
		this.tablet = root.getChild("tablet");
		this.buttons = this.tablet.getChild("buttons");
		this.up = this.buttons.getChild("up");
		this.down = this.buttons.getChild("down");
		this.left = this.buttons.getChild("left");
		this.right = this.buttons.getChild("right");
		this.enter = this.buttons.getChild("enter");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		
		ModelPartData tablet = root.addChild("tablet", ModelPartBuilder.create()
						.uv(24, 19).cuboid(-6.0F, -2.0F, -6.0F, 12.0F, 1.0F, 1.0F)
						.uv(24, 24).cuboid(-6.0F, -2.0F, 5.0F, 12.0F, 1.0F, 1.0F)
						.uv(12, 20).cuboid(5.0F, -2.0F, -5.0F, 1.0F, 1.0F, 10.0F)
						.uv(0, 19).cuboid(-6.0F, -2.0F, -5.0F, 1.0F, 1.0F, 10.0F)
						.uv(0, 0).cuboid(-6.0F, -1.0F, -6.0F, 12.0F, 1.0F, 12.0F),
				ModelTransform.pivot(0.0F, 24.0F, 0.0F)
		);

		
		ModelPartData buttons = tablet.addChild("buttons", ModelPartBuilder.create()
						.uv(0, 13).cuboid(-6.0F, -0.6979F, -4.3F, 12.0F, 1.0F, 5.0F),
				ModelTransform.of(0.0F, -2.0F, -6.0F, -0.7854F, 0.0F, 0.0F)
		);

		buttons.addChild("up", ModelPartBuilder.create()
						.uv(4, 6).cuboid(-0.5F, -0.4F, -0.5F, 1.0F, 1.0F, 1.0F),
				ModelTransform.pivot(-2.5F, -0.9F, -0.8243F)
		);

		buttons.addChild("down", ModelPartBuilder.create()
						.uv(0, 6).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
				ModelTransform.pivot(-2.5F, -0.8393F, -2.792F)
		);

		buttons.addChild("left", ModelPartBuilder.create()
						.uv(4, 4).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
				ModelTransform.pivot(-3.5F, -0.8393F, -1.8021F)
		);

		buttons.addChild("right", ModelPartBuilder.create()
						.uv(0, 4).cuboid(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
				ModelTransform.pivot(-1.5F, -0.8393F, -1.8021F)
		);

		buttons.addChild("enter", ModelPartBuilder.create()
						.uv(0, 0).cuboid(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F),
				ModelTransform.pivot(3.0F, -1.1222F, -1.8021F)
		);

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void render(MatrixStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		tablet.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setButtons(boolean upActive, boolean downActive, boolean leftActive, boolean rightActive, boolean enterActive, float deltaTime) {
		if(upActive) {
			this.up.pivotY = MVCUtils.lerp(this.up.pivotY, -0.5F, deltaTime);
		} else {
			this.up.pivotY = MVCUtils.lerp(this.up.pivotY, -0.9F, deltaTime);
		}

		if(downActive) {
			this.down.pivotY = MVCUtils.lerp(this.down.pivotY, -0.4393F, deltaTime);
		} else {
			this.down.pivotY = MVCUtils.lerp(this.down.pivotY, -0.8393F, deltaTime);
		}

		if(leftActive) {
			this.left.pivotY = MVCUtils.lerp(this.left.pivotY, -0.4393F, deltaTime);
		} else {
			this.left.pivotY = MVCUtils.lerp(this.left.pivotY, -0.8393F, deltaTime);
		}

		if(rightActive) {
			this.right.pivotY = MVCUtils.lerp(this.right.pivotY, -0.4393F, deltaTime);
		} else {
			this.right.pivotY = MVCUtils.lerp(this.right.pivotY, -0.8393F, deltaTime);
		}

		if(enterActive) {
			this.enter.pivotY = MVCUtils.lerp(this.enter.pivotY, -0.5F, deltaTime);
		} else {
			this.enter.pivotY = MVCUtils.lerp(this.enter.pivotY, -1.1222F, deltaTime);
		}
	}

	public void rotateButtons(float rotX, float deltaTime) {
		this.buttons.pitch = MVCUtils.lerp(this.buttons.pitch, rotX, deltaTime);
	}

	@Override
	public void setAngles(Entity entity, float limbAngle, float limbDistance, float customAngle, float headYaw, float headPitch) {
		
	}
}
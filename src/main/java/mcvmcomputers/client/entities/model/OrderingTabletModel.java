package mcvmcomputers.client.entities.model;

import mcvmcomputers.utils.MVCUtils;

import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.EntityModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.entity.Entity;

public class OrderingTabletModel extends EntityModel<Entity> {
	private final ModelPart tablet;
	private final ModelPart buttons;
	private final ModelPart up;
	private final ModelPart down;
	private final ModelPart left;
	private final ModelPart right;
	private final ModelPart enter;

	public OrderingTabletModel() {
		this(getLayerDefinition().bakeRoot());
	}

	public OrderingTabletModel(ModelPart root) {
		this.tablet = root.getChild("tablet");
		this.buttons = this.tablet.getChild("buttons");
		this.up = this.buttons.getChild("up");
		this.down = this.buttons.getChild("down");
		this.left = this.buttons.getChild("left");
		this.right = this.buttons.getChild("right");
		this.enter = this.buttons.getChild("enter");
	}

	public static LayerDefinition getLayerDefinition() {
		MeshDefinition meshDef = new MeshDefinition();
		PartDefinition root = meshDef.getRoot();


		root.addOrReplaceChild("tablet", CubeListBuilder.create()
			.texOffs(24, 19).addBox(-6.0F, -2.0F, -6.0F, 12.0F, 1.0F, 1.0F)
			.texOffs(24, 24).addBox(-6.0F, -2.0F, 5.0F, 12.0F, 1.0F, 1.0F)
			.texOffs(12, 20).addBox(5.0F, -2.0F, -5.0F, 1.0F, 1.0F, 10.0F)
			.texOffs(0, 19).addBox(-6.0F, -2.0F, -5.0F, 1.0F, 1.0F, 10.0F)
			.texOffs(0, 0).addBox(-6.0F, -1.0F, -6.0F, 12.0F, 1.0F, 12.0F),
			PartPose.offset(0.0F, 24.0F, 0.0F));


		PartDefinition buttonsData = root.getChild("tablet").addOrReplaceChild("buttons", CubeListBuilder.create()
			.texOffs(0, 13).addBox(-6.0F, -0.6979F, -4.3F, 12.0F, 1.0F, 5.0F),
			PartPose.offsetAndRotation(0.0F, -2.0F, -6.0F, -0.7854F, 0.0F, 0.0F));


		buttonsData.addOrReplaceChild("up", CubeListBuilder.create()
			.texOffs(4, 6).addBox(-0.5F, -0.4F, -0.5F, 1.0F, 1.0F, 1.0F),
			PartPose.offset(-2.5F, -0.9F, -0.8243F));


		buttonsData.addOrReplaceChild("down", CubeListBuilder.create()
			.texOffs(0, 6).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
			PartPose.offset(-2.5F, -0.8393F, -2.792F));


		buttonsData.addOrReplaceChild("left", CubeListBuilder.create()
			.texOffs(4, 4).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
			PartPose.offset(-3.5F, -0.8393F, -1.8021F));


		buttonsData.addOrReplaceChild("right", CubeListBuilder.create()
			.texOffs(0, 4).addBox(-0.5F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F),
			PartPose.offset(-1.5F, -0.8393F, -1.8021F));


		buttonsData.addOrReplaceChild("enter", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-1.0F, -0.5F, -1.5F, 2.0F, 1.0F, 3.0F),
			PartPose.offset(3.0F, -1.1222F, -1.8021F));

		return LayerDefinition.create(meshDef, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack PoseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color){
		tablet.render(PoseStack, buffer, packedLight, packedOverlay, -1);
	}

	public void setRotationAngle(ModelPart modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}

	public void setButtons(boolean up, boolean down, boolean left, boolean right, boolean enter, float deltaTime) {
		if(up) {
			this.up.y = MVCUtils.lerp(this.up.y, -0.5F, deltaTime);
		}else {
			this.up.y = MVCUtils.lerp(this.up.y, -0.9F, deltaTime);
		}

		if(down) {
			this.down.y = MVCUtils.lerp(this.down.y, -0.4393F, deltaTime);
		}else {
			this.down.y = MVCUtils.lerp(this.down.y, -0.8393F, deltaTime);
		}

		if(left) {
			this.left.y = MVCUtils.lerp(this.left.y, -0.4393F, deltaTime);
		}else {
			this.left.y = MVCUtils.lerp(this.left.y, -0.8393F, deltaTime);
		}

		if(right) {
			this.right.y = MVCUtils.lerp(this.right.y, -0.4393F, deltaTime);
		}else {
			this.right.y = MVCUtils.lerp(this.right.y, -0.8393F, deltaTime);
		}

		if(enter) {
			this.enter.y = MVCUtils.lerp(this.enter.y, -0.5F, deltaTime);
		}else {
			this.enter.y = MVCUtils.lerp(this.enter.y, -1.1222F, deltaTime);
		}
	}

	public void rotateButtons(float rotX, float deltaTime) {
		this.buttons.xRot = MVCUtils.lerp(this.buttons.xRot, rotX, deltaTime);
	}

	@Override
	public void setupAnim(Entity entity, float limbAngle, float limbDistance, float customAngle, float headYaw,
			float headPitch) {

	}
}

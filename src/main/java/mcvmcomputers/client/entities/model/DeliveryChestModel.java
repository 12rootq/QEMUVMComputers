package mcvmcomputers.client.entities.model;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

import net.minecraft.client.Minecraft;

import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.EntityModel;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;

public class DeliveryChestModel extends EntityModel<Entity> {
	public final ModelPart model;
	public final ModelPart opening;
	public final ModelPart upleg0;
	public final ModelPart uleg0;
	public final ModelPart upleg1;
	public final ModelPart uleg1;
	public final ModelPart upleg2;
	public final ModelPart uleg2;
	public final ModelPart upleg3;
	public final ModelPart uleg3;
	public final ModelPart engine;
	public final ModelPart fire;

	private final NativeImage baseTexture;
	private final Minecraft mcc;

	public static final Random TEX_RANDOM = new Random();

	private NativeImage ni;
	private DynamicTexture nibt;
	private ResourceLocation texId;

	public boolean fireYes = true;

	public DeliveryChestModel() throws IOException {
		this(getLayerDefinition().bakeRoot());
	}

	public DeliveryChestModel(ModelPart root) throws IOException {
		this.mcc = Minecraft.getInstance();
		this.baseTexture = NativeImage.read(mcc.getResourceManager().getResourceOrThrow(ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "textures/entity/delivery_chest.png")).open());

		this.model = root.getChild("model");
		this.opening = this.model.getChild("opening");
		this.upleg0 = this.model.getChild("upleg0");
		this.uleg0 = this.upleg0.getChild("uleg0");
		this.upleg1 = this.model.getChild("upleg1");
		this.uleg1 = this.upleg1.getChild("uleg1");
		this.upleg2 = this.model.getChild("upleg2");
		this.uleg2 = this.upleg2.getChild("uleg2");
		this.upleg3 = this.model.getChild("upleg3");
		this.uleg3 = this.upleg3.getChild("uleg3");
		this.engine = this.model.getChild("engine");
		this.fire = this.engine.getChild("fire");
	}

	public static LayerDefinition getLayerDefinition() {
		MeshDefinition meshDef = new MeshDefinition();
		PartDefinition root = meshDef.getRoot();


		PartDefinition MeshDefinition_part = root.addOrReplaceChild("model", CubeListBuilder.create()
			.texOffs(0, 0).addBox(-6.0F, -5.0F, -6.0F, 12.0F, 8.0F, 12.0F),
			PartPose.offset(0.0F, 7.0F, 0.0F));


		MeshDefinition_part.addOrReplaceChild("opening", CubeListBuilder.create()
			.texOffs(0, 20).addBox(-6.0F, -2.0F, -12.0F, 12.0F, 2.0F, 12.0F),
			PartPose.offsetAndRotation(0.0F, -5.0F, 6.0F, -1.1345F, 0.0F, 0.0F));


		PartDefinition upleg0Data = MeshDefinition_part.addOrReplaceChild("upleg0", CubeListBuilder.create()
			.texOffs(24, 34).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(-6.0F, 3.0F, 6.0F, 0.0F, 0.7854F, 0.0F));


		upleg0Data.addOrReplaceChild("uleg0", CubeListBuilder.create()
			.texOffs(0, 46).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.texOffs(0, 20).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			PartPose.offset(-0.9828F, 7.0F, -0.0071F));


		PartDefinition upleg1Data = MeshDefinition_part.addOrReplaceChild("upleg1", CubeListBuilder.create()
			.texOffs(0, 34).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(-6.0F, 3.0F, -6.0F, 0.0F, -0.7854F, 0.0F));


		upleg1Data.addOrReplaceChild("uleg1", CubeListBuilder.create()
			.texOffs(44, 44).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.texOffs(0, 8).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			PartPose.offset(-0.9828F, 7.0F, -0.0071F));


		PartDefinition upleg2Data = MeshDefinition_part.addOrReplaceChild("upleg2", CubeListBuilder.create()
			.texOffs(6, 24).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(6.0F, 3.0F, -6.0F, 0.0F, -2.3562F, 0.0F));


		upleg2Data.addOrReplaceChild("uleg2", CubeListBuilder.create()
			.texOffs(38, 43).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.texOffs(0, 4).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			PartPose.offset(-0.9828F, 7.0F, -0.0071F));


		PartDefinition upleg3Data = MeshDefinition_part.addOrReplaceChild("upleg3", CubeListBuilder.create()
			.texOffs(0, 24).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			PartPose.offsetAndRotation(6.0F, 3.0F, 6.0F, 0.0F, 2.3562F, 0.0F));


		upleg3Data.addOrReplaceChild("uleg3", CubeListBuilder.create()
			.texOffs(32, 43).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.texOffs(0, 0).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			PartPose.offset(-0.9828F, 7.0F, -0.0071F));


		PartDefinition engineData = MeshDefinition_part.addOrReplaceChild("engine", CubeListBuilder.create()
			.texOffs(0, 34).addBox(-4.0F, 8.0F, -4.0F, 8.0F, 4.0F, 8.0F)
			.texOffs(36, 0).addBox(-3.0F, 5.0F, -3.0F, 6.0F, 3.0F, 6.0F)
			.texOffs(36, 20).addBox(-2.0F, 3.0F, -2.0F, 4.0F, 2.0F, 4.0F)
			.texOffs(36, 26).addBox(-4.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
			.texOffs(30, 34).addBox(3.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
			.texOffs(36, 0).addBox(-4.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F)
			.texOffs(34, 34).addBox(3.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F),
			PartPose.offset(0.0F, 0.0F, 0.0F));


		engineData.addOrReplaceChild("fire", CubeListBuilder.create()
			.texOffs(32, 34).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 3.0F, 6.0F),
			PartPose.offset(0.0F, 13.0F, 0.0F));

		return LayerDefinition.create(meshDef, 64, 64);
	}

	private void generateTexture() {
		if(ni != null) {ni.close(); ni = null;}
		if(nibt != null) {nibt.close(); nibt = null;}
		if(texId != null) {mcc.getTextureManager().release(texId); texId = null;};

		ni = new NativeImage(64, 64, true);
		ni.copyFrom(baseTexture);
		for(int x = 38;x<50;x++) {
			for(int y = 34;y<40;y++) {
				ni.setPixelRGBA(x, y, randomColor());
			}
		}
		for(int x = 32;x<56;x++) {
			for(int y = 40;y<43;y++) {
				ni.setPixelRGBA(x, y, randomColor());
			}
		}
		nibt = new DynamicTexture(ni);
		texId = mcc.getTextureManager().register("delivery_chest_fire", nibt);
	}

	private int randomColor() {
		if(fireYes) {
			Color r = new Color(0,0,255);
			Color y = new Color(0,128,255);
			Color o = new Color(0, 255, 255);
			r = new Color(r.getRed(), r.getGreen(), r.getBlue(), TEX_RANDOM.nextInt(256));
			y = new Color(y.getRed(), y.getGreen(), y.getBlue(), TEX_RANDOM.nextInt(256));
			o = new Color(o.getRed(), o.getGreen(), o.getBlue(), TEX_RANDOM.nextInt(256));
			int[] sel = new int[] {r.getRGB(), y.getRGB(), o.getRGB()};
			return sel[TEX_RANDOM.nextInt(sel.length)];
		}else {
			return new Color(0f,0f,0f,0f).getRGB();
		}
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
	}

	@Override
	public void renderToBuffer(PoseStack PoseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color){
		model.render(PoseStack, buffer, packedLight, packedOverlay, -1);
	}

	public void renderToBuffer(PoseStack PoseStack, MultiBufferSource provider, int packedLight, int packedOverlay){
		this.generateTexture();
		model.render(PoseStack, provider.getBuffer(RenderType.entityCutout(texId)), packedLight, packedOverlay, -1);
	}

	public void setRotationAngle(ModelPart modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}
}

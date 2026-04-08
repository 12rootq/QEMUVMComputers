package mcvmcomputers.client.entities.model;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.*;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

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
	private final MinecraftClient mcc;
	
	public static final Random TEX_RANDOM = new Random();
	
	private NativeImage ni;
	private NativeImageBackedTexture nibt;
	private Identifier texId;
	
	public boolean fireYes = true;

	public DeliveryChestModel() throws IOException {
		this(getTexturedModelData().createModel());
	}

	public DeliveryChestModel(ModelPart root) throws IOException {
		this.mcc = MinecraftClient.getInstance();
		this.baseTexture = NativeImage.read(mcc.getResourceManager().getResourceOrThrow(new Identifier("mcvmcomputers", "textures/entity/delivery_chest.png")).getInputStream());
		
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

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData root = modelData.getRoot();

		// model (root part) - main body
		ModelPartData modelData_part = root.addChild("model", ModelPartBuilder.create()
			.uv(0, 0).cuboid(-6.0F, -5.0F, -6.0F, 12.0F, 8.0F, 12.0F),
			ModelTransform.pivot(0.0F, 7.0F, 0.0F));

		// opening - child of model
		modelData_part.addChild("opening", ModelPartBuilder.create()
			.uv(0, 20).cuboid(-6.0F, -2.0F, -12.0F, 12.0F, 2.0F, 12.0F),
			ModelTransform.of(0.0F, -5.0F, 6.0F, -1.1345F, 0.0F, 0.0F));

		// upleg0 - child of model
		ModelPartData upleg0Data = modelData_part.addChild("upleg0", ModelPartBuilder.create()
			.uv(24, 34).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			ModelTransform.of(-6.0F, 3.0F, 6.0F, 0.0F, 0.7854F, 0.0F));

		// uleg0 - child of upleg0
		upleg0Data.addChild("uleg0", ModelPartBuilder.create()
			.uv(0, 46).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.uv(0, 20).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		// upleg1 - child of model
		ModelPartData upleg1Data = modelData_part.addChild("upleg1", ModelPartBuilder.create()
			.uv(0, 34).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			ModelTransform.of(-6.0F, 3.0F, -6.0F, 0.0F, -0.7854F, 0.0F));

		// uleg1 - child of upleg1
		upleg1Data.addChild("uleg1", ModelPartBuilder.create()
			.uv(44, 44).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.uv(0, 8).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		// upleg2 - child of model
		ModelPartData upleg2Data = modelData_part.addChild("upleg2", ModelPartBuilder.create()
			.uv(6, 24).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			ModelTransform.of(6.0F, 3.0F, -6.0F, 0.0F, -2.3562F, 0.0F));

		// uleg2 - child of upleg2
		upleg2Data.addChild("uleg2", ModelPartBuilder.create()
			.uv(38, 43).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.uv(0, 4).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		// upleg3 - child of model
		ModelPartData upleg3Data = modelData_part.addChild("upleg3", ModelPartBuilder.create()
			.uv(0, 24).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
			ModelTransform.of(6.0F, 3.0F, 6.0F, 0.0F, 2.3562F, 0.0F));

		// uleg3 - child of upleg3
		upleg3Data.addChild("uleg3", ModelPartBuilder.create()
			.uv(32, 43).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
			.uv(0, 0).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
			ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		// engine - child of model
		ModelPartData engineData = modelData_part.addChild("engine", ModelPartBuilder.create()
			.uv(0, 34).cuboid(-4.0F, 8.0F, -4.0F, 8.0F, 4.0F, 8.0F)
			.uv(36, 0).cuboid(-3.0F, 5.0F, -3.0F, 6.0F, 3.0F, 6.0F)
			.uv(36, 20).cuboid(-2.0F, 3.0F, -2.0F, 4.0F, 2.0F, 4.0F)
			.uv(36, 26).cuboid(-4.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
			.uv(30, 34).cuboid(3.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
			.uv(36, 0).cuboid(-4.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F)
			.uv(34, 34).cuboid(3.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F),
			ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		// fire - child of engine
		engineData.addChild("fire", ModelPartBuilder.create()
			.uv(32, 34).cuboid(-3.0F, -1.0F, -3.0F, 6.0F, 3.0F, 6.0F),
			ModelTransform.pivot(0.0F, 13.0F, 0.0F));

		return TexturedModelData.of(modelData, 64, 64);
	}
	
	private void generateTexture() {
		if(ni != null) {ni.close(); ni = null;}
		if(nibt != null) {nibt.close(); nibt = null;}
		if(texId != null) {mcc.getTextureManager().destroyTexture(texId); texId = null;};
		
		ni = new NativeImage(64, 64, true);
		ni.copyFrom(baseTexture);
		for(int x = 38;x<50;x++) {
			for(int y = 34;y<40;y++) {
				ni.setColor(x, y, randomColor());
			}
		}
		for(int x = 32;x<56;x++) {
			for(int y = 40;y<43;y++) {
				ni.setColor(x, y, randomColor());
			}
		}
		nibt = new NativeImageBackedTexture(ni);
		texId = mcc.getTextureManager().registerDynamicTexture("delivery_chest_fire", nibt);
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
	public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
	}

	@Override
	public void render(MatrixStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		model.render(matrixStack, buffer, packedLight, packedOverlay);
	}
	
	public void render(MatrixStack matrixStack, VertexConsumerProvider provider, int packedLight, int packedOverlay){
		this.generateTexture();
		model.render(matrixStack, provider.getBuffer(RenderLayer.getEntityCutout(texId)), packedLight, packedOverlay);
	}
	
	public void setRotationAngle(ModelPart modelRenderer, float x, float y, float z) {
		modelRenderer.pitch = x;
		modelRenderer.yaw = y;
		modelRenderer.roll = z;
	}
}

package newvmcomputers.client.entities.model;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
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
	public final ModelPart engine;
	public final ModelPart fire;

	
	public final ModelPart upleg0, upleg1, upleg2, upleg3;
	public final ModelPart uleg0, uleg1, uleg2, uleg3;

	private final NativeImage baseTexture;
	private final MinecraftClient mcc;

	public static final Random TEX_RANDOM = new Random();

	private NativeImage ni;
	private NativeImageBackedTexture nibt;
	private Identifier texId;

	public boolean fireYes = true;

	public DeliveryChestModel(ModelPart root) throws IOException {
		this.mcc = MinecraftClient.getInstance();
		this.baseTexture = NativeImage.read(mcc.getResourceManager().getResource(new Identifier("newvmcomputers", "textures/entity/delivery_chest.png")).get().getInputStream());

		this.model = root.getChild("model");
		this.opening = this.model.getChild("opening");
		this.engine = this.model.getChild("engine");
		this.fire = this.engine.getChild("fire");

		
		this.upleg0 = this.model.getChild("upleg0");
		this.uleg0 = this.upleg0.getChild("uleg0");

		this.upleg1 = this.model.getChild("upleg1");
		this.uleg1 = this.upleg1.getChild("uleg1");

		this.upleg2 = this.model.getChild("upleg2");
		this.uleg2 = this.upleg2.getChild("uleg2");

		this.upleg3 = this.model.getChild("upleg3");
		this.uleg3 = this.upleg3.getChild("uleg3");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();

		ModelPartData model = modelPartData.addChild("model",
				ModelPartBuilder.create().uv(0, 0).cuboid(-6.0F, -5.0F, -6.0F, 12.0F, 8.0F, 12.0F),
				ModelTransform.pivot(0.0F, 7.0F, 0.0F));

		model.addChild("opening",
				ModelPartBuilder.create().uv(0, 20).cuboid(-6.0F, -2.0F, -12.0F, 12.0F, 2.0F, 12.0F),
				ModelTransform.of(0.0F, -5.0F, 6.0F, -1.1345F, 0.0F, 0.0F));

		ModelPartData upleg0 = model.addChild("upleg0",
				ModelPartBuilder.create().uv(24, 34).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
				ModelTransform.of(-6.0F, 3.0F, 6.0F, 0.0F, 0.7854F, 0.0F));
		upleg0.addChild("uleg0",
				ModelPartBuilder.create()
						.uv(0, 46).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
						.uv(0, 20).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
				ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		ModelPartData upleg1 = model.addChild("upleg1",
				ModelPartBuilder.create().uv(0, 34).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
				ModelTransform.of(-6.0F, 3.0F, -6.0F, 0.0F, -0.7854F, 0.0F));
		upleg1.addChild("uleg1",
				ModelPartBuilder.create()
						.uv(44, 44).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
						.uv(0, 8).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
				ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		ModelPartData upleg2 = model.addChild("upleg2",
				ModelPartBuilder.create().uv(6, 24).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
				ModelTransform.of(6.0F, 3.0F, -6.0F, 0.0F, -2.3562F, 0.0F));
		upleg2.addChild("uleg2",
				ModelPartBuilder.create()
						.uv(38, 43).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
						.uv(0, 4).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
				ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		ModelPartData upleg3 = model.addChild("upleg3",
				ModelPartBuilder.create().uv(0, 24).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F),
				ModelTransform.of(6.0F, 3.0F, 6.0F, 0.0F, 2.3562F, 0.0F));
		upleg3.addChild("uleg3",
				ModelPartBuilder.create()
						.uv(32, 43).cuboid(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F)
						.uv(0, 0).cuboid(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F),
				ModelTransform.pivot(-0.9828F, 7.0F, -0.0071F));

		ModelPartData engine = model.addChild("engine",
				ModelPartBuilder.create()
						.uv(0, 34).cuboid(-4.0F, 8.0F, -4.0F, 8.0F, 4.0F, 8.0F)
						.uv(36, 0).cuboid(-3.0F, 5.0F, -3.0F, 6.0F, 3.0F, 6.0F)
						.uv(36, 20).cuboid(-2.0F, 3.0F, -2.0F, 4.0F, 2.0F, 4.0F)
						.uv(36, 26).cuboid(-4.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
						.uv(30, 34).cuboid(3.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
						.uv(36, 0).cuboid(-4.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F)
						.uv(34, 34).cuboid(3.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F),
				ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		engine.addChild("fire",
				ModelPartBuilder.create().uv(32, 34).cuboid(-3.0F, -1.0F, -3.0F, 6.0F, 3.0F, 6.0F),
				ModelTransform.pivot(0.0F, 13.0F, 0.0F));

		return TexturedModelData.of(modelData, 64, 64);
	}

	
	public void setRotationAngle(ModelPart part, float pitch, float yaw, float roll) {
		part.pitch = pitch;
		part.yaw = yaw;
		part.roll = roll;
	}

	private void generateTexture() {
		if(ni != null) {ni.close(); ni = null;}
		if(nibt != null) {nibt.close(); nibt = null;}
		if(texId != null) {mcc.getTextureManager().destroyTexture(texId); texId = null;}

		ni = new NativeImage(64, 64, true);
		ni.copyFrom(baseTexture);
		for(int x = 38; x < 50; x++) {
			for(int y = 34; y < 40; y++) {
				ni.setColor(x, y, randomColorABGR());
			}
		}
		for(int x = 32; x < 56; x++) {
			for(int y = 40; y < 43; y++) {
				ni.setColor(x, y, randomColorABGR());
			}
		}
		nibt = new NativeImageBackedTexture(ni);
		texId = mcc.getTextureManager().registerDynamicTexture("delivery_chest_fire", nibt);
	}

	private int randomColorABGR() {
		if(fireYes) {
			int a = TEX_RANDOM.nextInt(256);
			Color[] colors = new Color[] {
					new Color(0, 0, 255),    
					new Color(0, 128, 255),  
					new Color(0, 255, 255)   
			};
			Color c = colors[TEX_RANDOM.nextInt(colors.length)];
			return (a << 24) | (c.getBlue() << 16) | (c.getGreen() << 8) | c.getRed();
		} else {
			return 0;
		}
	}

	@Override
	public void setAngles(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}

	@Override
	public void render(MatrixStack matrixStack, VertexConsumer buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		model.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void render(MatrixStack matrixStack, VertexConsumerProvider provider, int packedLight, int packedOverlay) {
		this.generateTexture();
		model.render(matrixStack, provider.getBuffer(RenderLayer.getEntityTranslucent(texId)), packedLight, packedOverlay, 1.0f, 1.0f, 1.0f, 1.0f);
	}
}
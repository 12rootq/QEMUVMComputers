package newvmcomputers.client.entities.model;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class DeliveryChestModel extends EntityModel<Entity> {
    public final ModelPart model;
    public final ModelPart opening;
    public final ModelPart engine;
    public final ModelPart fire;
    public final ModelPart upleg0;
    public final ModelPart upleg1;
    public final ModelPart upleg2;
    public final ModelPart upleg3;
    public final ModelPart uleg0;
    public final ModelPart uleg1;
    public final ModelPart uleg2;
    public final ModelPart uleg3;

    private static final ResourceLocation BASE_TEXTURE_LOCATION = new ResourceLocation("newvmcomputers", "textures/entity/delivery_chest.png");

    private NativeImage baseTexture;
    private final Minecraft minecraft;

    public static final Random TEX_RANDOM = new Random();

    private NativeImage generatedTexture;
    private DynamicTexture dynamicTexture;
    private ResourceLocation textureId;

    public boolean fireYes = true;

    public DeliveryChestModel(ModelPart root) {
        this.minecraft = Minecraft.getInstance();
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

    public static LayerDefinition getLayerDefinition() {
        MeshDefinition meshDefinition = new MeshDefinition();
        PartDefinition root = meshDefinition.getRoot();
        PartDefinition model = root.addOrReplaceChild("model", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -5.0F, -6.0F, 12.0F, 8.0F, 12.0F), PartPose.offset(0.0F, 7.0F, 0.0F));
        model.addOrReplaceChild("opening", CubeListBuilder.create().texOffs(0, 20).addBox(-6.0F, -2.0F, -12.0F, 12.0F, 2.0F, 12.0F), PartPose.offsetAndRotation(0.0F, -5.0F, 6.0F, -1.1345F, 0.0F, 0.0F));

        PartDefinition upleg0 = model.addOrReplaceChild("upleg0", CubeListBuilder.create().texOffs(24, 34).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, 3.0F, 6.0F, 0.0F, 0.7854F, 0.0F));
        upleg0.addOrReplaceChild("uleg0", CubeListBuilder.create().texOffs(0, 46).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F).texOffs(0, 20).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F), PartPose.offset(-0.9828F, 7.0F, -0.0071F));
        PartDefinition upleg1 = model.addOrReplaceChild("upleg1", CubeListBuilder.create().texOffs(0, 34).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(-6.0F, 3.0F, -6.0F, 0.0F, -0.7854F, 0.0F));
        upleg1.addOrReplaceChild("uleg1", CubeListBuilder.create().texOffs(44, 44).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F).texOffs(0, 8).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F), PartPose.offset(-0.9828F, 7.0F, -0.0071F));
        PartDefinition upleg2 = model.addOrReplaceChild("upleg2", CubeListBuilder.create().texOffs(6, 24).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(6.0F, 3.0F, -6.0F, 0.0F, -2.3562F, 0.0F));
        upleg2.addOrReplaceChild("uleg2", CubeListBuilder.create().texOffs(38, 43).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F).texOffs(0, 4).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F), PartPose.offset(-0.9828F, 7.0F, -0.0071F));
        PartDefinition upleg3 = model.addOrReplaceChild("upleg3", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 7.0F, 1.0F), PartPose.offsetAndRotation(6.0F, 3.0F, 6.0F, 0.0F, 2.3562F, 0.0F));
        upleg3.addOrReplaceChild("uleg3", CubeListBuilder.create().texOffs(32, 43).addBox(-1.0F, 0.0F, -0.5F, 2.0F, 6.0F, 1.0F).texOffs(0, 0).addBox(-1.4868F, 6.0F, -1.5232F, 3.0F, 1.0F, 3.0F), PartPose.offset(-0.9828F, 7.0F, -0.0071F));
        PartDefinition engine = model.addOrReplaceChild("engine", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-4.0F, 8.0F, -4.0F, 8.0F, 4.0F, 8.0F)
                .texOffs(36, 0).addBox(-3.0F, 5.0F, -3.0F, 6.0F, 3.0F, 6.0F)
                .texOffs(36, 20).addBox(-2.0F, 3.0F, -2.0F, 4.0F, 2.0F, 4.0F)
                .texOffs(36, 26).addBox(-4.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
                .texOffs(30, 34).addBox(3.0F, 3.0F, 3.0F, 1.0F, 5.0F, 1.0F)
                .texOffs(36, 0).addBox(-4.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F)
                .texOffs(34, 34).addBox(3.0F, 3.0F, -4.0F, 1.0F, 5.0F, 1.0F), PartPose.ZERO);
        engine.addOrReplaceChild("fire", CubeListBuilder.create().texOffs(32, 34).addBox(-3.0F, -1.0F, -3.0F, 6.0F, 3.0F, 6.0F), PartPose.offset(0.0F, 13.0F, 0.0F));
        return LayerDefinition.create(meshDefinition, 64, 64);
    }

    public void setRotationAngle(ModelPart part, float xRot, float yRot, float zRot) {
        part.xRot = xRot;
        part.yRot = yRot;
        part.zRot = zRot;
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        model.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    public void render(PoseStack poseStack, MultiBufferSource provider, int packedLight, int packedOverlay) {
        if (!ensureBaseTexture()) {
            model.render(poseStack, provider.getBuffer(RenderType.entityTranslucent(MissingTextureAtlasSprite.getLocation())), packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
            return;
        }
        generateTexture();
        model.render(poseStack, provider.getBuffer(RenderType.entityTranslucent(textureId)), packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private boolean ensureBaseTexture() {
        if (baseTexture != null) {
            return true;
        }

        try {
            baseTexture = NativeImage.read(
                    minecraft.getResourceManager()
                            .getResource(BASE_TEXTURE_LOCATION)
                            .orElseThrow(() -> new IOException("Missing resource " + BASE_TEXTURE_LOCATION))
                            .open()
            );
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void generateTexture() {
        if (generatedTexture != null) {
            generatedTexture.close();
        }
        if (dynamicTexture != null) {
            dynamicTexture.close();
        }
        if (textureId != null) {
            minecraft.getTextureManager().release(textureId);
        }

        generatedTexture = new NativeImage(64, 64, true);
        generatedTexture.copyFrom(baseTexture);
        for (int x = 38; x < 50; x++) {
            for (int y = 34; y < 40; y++) {
                generatedTexture.setPixelRGBA(x, y, randomColorAbgr());
            }
        }
        for (int x = 32; x < 56; x++) {
            for (int y = 40; y < 43; y++) {
                generatedTexture.setPixelRGBA(x, y, randomColorAbgr());
            }
        }
        dynamicTexture = new DynamicTexture(generatedTexture);
        textureId = minecraft.getTextureManager().register("delivery_chest_fire", dynamicTexture);
    }

    private int randomColorAbgr() {
        if (!fireYes) {
            return 0;
        }
        int alpha = TEX_RANDOM.nextInt(256);
        Color[] colors = new Color[] {
                new Color(0, 0, 255),
                new Color(0, 128, 255),
                new Color(0, 255, 255)
        };
        Color color = colors[TEX_RANDOM.nextInt(colors.length)];
        return (alpha << 24) | (color.getBlue() << 16) | (color.getGreen() << 8) | color.getRed();
    }
}

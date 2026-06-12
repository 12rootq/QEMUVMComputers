package mcvmcomputers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Matrix4f;
import org.joml.Quaternionf;

import mcvmcomputers.item.ItemOrderingTablet;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * Renders the VM Computers tablet (model + "Tablet OS" UI) in first person through the
 * NeoForge {@link RenderHandEvent} instead of a Mixin into {@code ItemInHandRenderer}.
 *
 * <p>Other "held item" mods such as Hold My Items Reforged cancel {@link RenderHandEvent}
 * to take over first-person item rendering. When they do, vanilla
 * {@code ItemInHandRenderer.renderArmWithItem} is never called, so a HEAD Mixin into that
 * method never runs and the tablet model + UI disappear. Handling the event ourselves at
 * {@link EventPriority#HIGHEST} with {@code receiveCanceled = true} guarantees the tablet
 * is drawn regardless of whether another mod cancelled the event, while still cancelling it
 * so neither vanilla nor the other mod re-renders on top of the tablet.</p>
 */
@EventBusSubscriber(modid = "mcvmcomputers", bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class HeldTabletRenderer {

    private static final ResourceLocation TABLET_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "textures/entity/tablet.png");

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRenderHand(RenderHandEvent event) {
        ItemStack item = event.getItemStack();
        if (item.isEmpty() || !(item.getItem() instanceof ItemOrderingTablet)) {
            return;
        }
        // TabletOS is initialised lazily on the client; if it is not ready yet, fall through
        // and let the default rendering happen rather than crashing.
        if (ClientMod.tabletOS == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float equipProgress = event.getEquipProgress();

        poseStack.pushPose();
        poseStack.translate(0, -equipProgress * 2, 0);
        poseStack.pushPose();
        poseStack.translate(0, 0.1, 0.38);
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-90)));
        ClientMod.tabletOS.orderingTabletModel.renderToBuffer(
            poseStack,
            bufferSource.getBuffer(RenderType.entityCutout(TABLET_TEXTURE)),
            light, OverlayTexture.NO_OVERLAY, -1);

        if (ClientMod.tabletOS.textureIdentifier != null) {
            poseStack.pushPose();
            poseStack.scale(0.19f, 0.19f, 0.19f);
            poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-90)));
            poseStack.translate(-1.65, -1.65, 7.57);
            Matrix4f matrix4f = poseStack.last().pose();
            VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.text(ClientMod.tabletOS.textureIdentifier));
            vertexConsumer.addVertex(matrix4f, 0.0F, 3.3F, -0.01F).setColor(255, 255, 255, 255).setUv(0.0F, 1.0F).setLight(15728640);
            vertexConsumer.addVertex(matrix4f, 3.3F, 3.3F, -0.01F).setColor(255, 255, 255, 255).setUv(1.0F, 1.0F).setLight(15728640);
            vertexConsumer.addVertex(matrix4f, 3.3F, 0.0F, -0.01F).setColor(255, 255, 255, 255).setUv(1.0F, 0.0F).setLight(15728640);
            vertexConsumer.addVertex(matrix4f, 0.0F, 0.0F, -0.01F).setColor(255, 255, 255, 255).setUv(0.0F, 0.0F).setLight(15728640);
            poseStack.popPose();
        }
        poseStack.popPose();
        poseStack.popPose();

        // Prevent vanilla (and any other mod) from rendering the flat item / transforming
        // on top of the tablet model.
        event.setCanceled(true);
    }
}

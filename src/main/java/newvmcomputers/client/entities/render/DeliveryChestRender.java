package newvmcomputers.client.entities.render;

import java.awt.Color;

import newvmcomputers.client.ClientMod;
import newvmcomputers.client.entities.model.DeliveryChestModel;
import newvmcomputers.entities.EntityDeliveryChest;
import newvmcomputers.sound.SoundList;
import newvmcomputers.utils.TabletOrder.OrderStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.Level;

import static newvmcomputers.client.ClientMod.*;
import static newvmcomputers.utils.MVCUtils.*;

public class DeliveryChestRender extends EntityRenderer<EntityDeliveryChest> {
	private final DeliveryChestModel deliveryChestModel;
	private final Minecraft mcc;

	public DeliveryChestRender(EntityRendererProvider.Context ctx) {
		super(ctx);
		this.mcc = Minecraft.getInstance();
		try {
			this.deliveryChestModel = new DeliveryChestModel(ctx.bakeLayer(ClientMod.DELIVERY_CHEST_LAYER));
		} catch (Exception e) {
			throw new RuntimeException("Failed to load DeliveryChestModel", e);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(EntityDeliveryChest entity) {
		return null; 
	}

	private void applyRotations(EntityDeliveryChest entity) {
		deliveryChestModel.setRotationAngle(deliveryChestModel.upleg0, 0, 0, entity.upLeg01Rot);
		deliveryChestModel.setRotationAngle(deliveryChestModel.upleg1, 0, 0, entity.upLeg01Rot);
		deliveryChestModel.setRotationAngle(deliveryChestModel.upleg2, 0, 0, entity.upLeg23Rot);
		deliveryChestModel.setRotationAngle(deliveryChestModel.upleg3, 0, 0, entity.upLeg23Rot);

		deliveryChestModel.setRotationAngle(deliveryChestModel.uleg0, 0, 0, entity.uLeg01Rot);
		deliveryChestModel.setRotationAngle(deliveryChestModel.uleg1, 0, 0, entity.uLeg01Rot);
		deliveryChestModel.setRotationAngle(deliveryChestModel.uleg2, 0, 0, entity.uLeg23Rot);
		deliveryChestModel.setRotationAngle(deliveryChestModel.uleg3, 0, 0, entity.uLeg23Rot);

		deliveryChestModel.setRotationAngle(deliveryChestModel.opening, entity.openingRot, 0, 0);

		deliveryChestModel.fireYes = entity.fire;
	}

	private Vec3 renderPos(EntityDeliveryChest entity) {
		return new Vec3(entity.getX(), entity.getY() + entity.renderOffY, entity.getZ() + entity.renderOffZ);
	}

	private void changeRotations(EntityDeliveryChest entity) {
		if(entity.fire) {
			if(entity.rocketSound == null) {
				entity.rocketSound = new AbstractTickableSoundInstance(SoundList.ROCKET_SOUND.get(), SoundSource.MASTER, net.minecraft.util.RandomSource.create()) {
					{
						this.looping = true;
						this.delay = 0;
						this.volume = 1.0f;
					}
					@Override
					public void tick() {
						if (entity.isRemoved()) {
							this.stop();
							return;
						}
						if (mcc.player == null) return;

						Vec3 v = new Vec3(entity.getX(), entity.getY() + entity.renderOffY, entity.getZ() + entity.renderOffZ);
						double dist = Math.abs(v.distanceTo(mcc.player.position()));
						dist = Math.min(dist, 40) / 40.0;
						this.volume = (float) (1.0 - dist);
					}
				};
				mcc.getSoundManager().play(entity.rocketSound);
			}
		} else {
			if(entity.rocketSound != null) {
				mcc.getSoundManager().stop(entity.rocketSound);
				entity.rocketSound = null;
			}
		}

		if(!entity.getTakingOff()) {
			Vec3 curPos = renderPos(entity);
			double tx = lerp(curPos.x, entity.getTargetX(), deltaTime / 2f);
			double ty = lerp(curPos.y, entity.getTargetY(), deltaTime / 2f);
			double tz = lerp(curPos.z, entity.getTargetZ(), deltaTime / 2f);
			entity.updateRenderPos(tx, ty, tz);

			double dist = Math.abs(renderPos(entity).distanceTo(new Vec3(entity.getTargetX(), entity.getTargetY(), entity.getTargetZ())));
			double prog = Math.min(dist / 40f, 1f);
			entity.renderRot = (float) (90f + (45f * (2.0 - prog)));

			entity.fire = dist > 25 || (dist < 25 && dist > 0.1);

			if(dist < 3) {
				entity.upLeg01Rot = lerp(entity.upLeg01Rot, 0f, deltaTime);
				entity.upLeg23Rot = lerp(entity.upLeg23Rot, 0f, deltaTime);
				entity.uLeg01Rot = lerp(entity.uLeg01Rot, 0f, deltaTime);
				entity.uLeg23Rot = lerp(entity.uLeg23Rot, 0f, deltaTime);
			}
			if (dist < 0.1) {
				entity.openingRot = lerp(entity.openingRot, -2F, deltaTime);
				entity.fire = false;
				entity.updateRenderPos(entity.getTargetX(), entity.getTargetY(), entity.getTargetZ());
			}
		} else {
			entity.takeOffSpeed = lerp(entity.takeOffSpeed, 5f, deltaTime / 180f);
			Vec3 curPos = renderPos(entity);
			entity.updateRenderPos(curPos.x, curPos.y + entity.takeOffSpeed, curPos.z);

			entity.upLeg01Rot = lerp(entity.upLeg01Rot, 3f, deltaTime);
			entity.upLeg23Rot = lerp(entity.upLeg23Rot, 3.3f, deltaTime);
			entity.uLeg01Rot = lerp(entity.uLeg01Rot, -2.7f, deltaTime);
			entity.uLeg23Rot = lerp(entity.uLeg23Rot, -2.7f, deltaTime);
			entity.openingRot = lerp(entity.openingRot, 0f, deltaTime);
			entity.fire = true;
		}
	}

	private void smokeParticle(Level w, Vec3 pos, int amount) {
		for(int i = 0; i < amount; i++) {
			float rx = (DeliveryChestModel.TEX_RANDOM.nextFloat() * 0.5f) - .25f;
			float ry = DeliveryChestModel.TEX_RANDOM.nextFloat() * -.3F;
			float rz = (DeliveryChestModel.TEX_RANDOM.nextFloat() * 0.5f) - .25f;
			if(amount == 3) {
				w.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, rx, ry, rz);
			} else {
				w.addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, rx, ry, rz);
			}
		}
	}

	private void doParticlesForFire(EntityDeliveryChest entity) {
		Vec3 curPos = renderPos(entity);
		smokeParticle(entity.level(), curPos, 3);
		smokeParticle(entity.level(), curPos, 6);

		int groundY = entity.level().getHeight(Types.MOTION_BLOCKING, (int) curPos.x, (int) curPos.z);
		double dist = Math.abs(curPos.y - groundY);

		if(dist < 5) {
			int pAmount = (int) Math.pow(2, 4 - (int)dist);
			smokeParticle(entity.level(), new Vec3(curPos.x, groundY, curPos.z), pAmount);
		}
	}

	@Override
	public boolean shouldRender(EntityDeliveryChest entity, Frustum visibleRegion, double cameraX, double cameraY, double cameraZ) {
		return true;
	}

	@Override
	public void render(EntityDeliveryChest entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource vertexConsumers, int light) {
		this.changeRotations(entity);
		this.applyRotations(entity);
		if(entity.fire) {
			this.doParticlesForFire(entity);
		}

		matrices.pushPose();
		matrices.translate(0, entity.renderOffY, entity.renderOffZ);

		matrices.pushPose();
		matrices.mulPose(Axis.XP.rotationDegrees(entity.renderRot));
		matrices.translate(0, -1.5, 0);
		deliveryChestModel.render(matrices, vertexConsumers, light, OverlayTexture.NO_OVERLAY);

		
		matrices.pushPose();
		matrices.mulPose(Axis.XP.rotationDegrees(-90f));
		matrices.scale(0.02f, 0.02f, 0.02f);
		matrices.translate(-15.63f, -15.63f, 6.22f);
		matrices.scale(0.4f, 0.4f, 0.4f);

		if(ClientMod.myOrder != null && entity.getDeliveryUUID().equals(ClientMod.myOrder.orderUUID)) {
			OrderStatus status = ClientMod.myOrder.currentStatus;
			if(status == OrderStatus.PAYMENT_CHEST_ARRIVED || status == OrderStatus.PAYMENT_CHEST_RECEIVING) {
				drawText(matrices, vertexConsumers, "Please insert", 6, 25, -1, light);
				drawText(matrices, vertexConsumers, "" + ClientMod.myOrder.price, 39, 33, Color.HSBtoRGB(0.6f, 0.6f, 1f), light);
				drawText(matrices, vertexConsumers, "Iron Ingots", 10, 41, -1, light);
			} else if(status == OrderStatus.ORDER_CHEST_ARRIVED || status == OrderStatus.ORDER_CHEST_RECEIVED) {
				drawText(matrices, vertexConsumers, ClientMod.myOrder.items.size() + " items", 39, 20, -1, light);
				drawText(matrices, vertexConsumers, "in chest", 19, 30, -1, light);
			}
		} else {
			drawText(matrices, vertexConsumers, "This is not", 13, 30, Color.RED.getRGB(), light);
			drawText(matrices, vertexConsumers, "your chest!", 10, 40, Color.RED.getRGB(), light);
		}

		matrices.popPose();
		matrices.popPose();
		matrices.popPose();

		super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
	}

	private void drawText(PoseStack matrices, MultiBufferSource vcp, String text, float x, float y, int color, int light) {
		this.getFont().drawInBatch(text, x, y, color, false, matrices.last().pose(), vcp, Font.DisplayMode.NORMAL, 0, light);
	}
}




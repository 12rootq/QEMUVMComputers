package mcvmcomputers.client.entities.render;
import net.minecraft.world.entity.player.Player;


import java.awt.Color;
import java.io.IOException;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.client.entities.model.DeliveryChestModel;
import mcvmcomputers.entities.EntityDeliveryChest;
import mcvmcomputers.sound.SoundList;
import mcvmcomputers.utils.TabletOrder.OrderStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;



import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.gui.Font;
import org.joml.Quaternionf;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import java.util.Random;

import static mcvmcomputers.client.ClientMod.*;
import static mcvmcomputers.utils.MVCUtils.*;

public class DeliveryChestRender extends EntityRenderer<EntityDeliveryChest>{
	private DeliveryChestModel deliveryChestModel;
	private Minecraft mcc;

	public DeliveryChestRender(EntityRendererProvider.Context context) {
		super(context);
		mcc = Minecraft.getInstance();
	}

	@Override
	public ResourceLocation getTextureLocation(EntityDeliveryChest entity) {
		return null;
	}

	private void checkModel() {
		if(deliveryChestModel == null) {
			try {
				deliveryChestModel = new DeliveryChestModel();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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
		return new Vec3(entity.getX(), entity.getY()+entity.renderOffY, entity.getZ()+entity.renderOffZ);
	}

	private void changeRotations(EntityDeliveryChest entity) {
		if(entity.fire) {
			if(entity.rocketSound == null) {
				entity.rocketSound = new AbstractTickableSoundInstance(SoundList.ROCKET_SOUND, SoundSource.MASTER, net.minecraft.util.RandomSource.create()) {
					@Override
					public boolean isLooping() {
						return true;
					}

					@Override
					public boolean canStartSilent() {
						return true;
					}

					@Override
					public void tick() {
						Vec3 v = new Vec3(entity.getX(), entity.getY() + entity.renderOffY, entity.getZ() + entity.renderOffZ);
						double dist = v.distanceTo(mcc.player.position());
						dist = Math.abs(dist);
						dist = Math.min(dist, 40)/40.0;
						dist = 1 - dist;
						this.volume = (float) dist;
					}
				};
				mcc.getSoundManager().play(entity.rocketSound);
			}
		}else {
			if(entity.rocketSound != null) {
				mcc.getSoundManager().stop(entity.rocketSound);
				entity.rocketSound = null;
			}
		}
		if(!entity.getTakingOff()) {
			Vec3 curPos = renderPos(entity);
			Vec3 v = new Vec3(lerp(curPos.x, entity.getTargetX(), deltaTime/2f), lerp(curPos.y, entity.getTargetY(), deltaTime/2f), lerp(curPos.z, entity.getTargetZ(), deltaTime/2f));
			entity.updateRenderPos(v.x, v.y, v.z);
			double dist = renderPos(entity).distanceTo(new Vec3(entity.getTargetX(),entity.getTargetY(),entity.getTargetZ()));
			if(dist < 0) {
				dist = -dist;
			}
			double prog = Math.min(dist / 40f, 1f);

			prog -= 2;
			prog = -prog;

			entity.renderRot = (float) (90f + (45f * prog));

			if(dist > 25) {
				entity.fire = false;
			}else {
				entity.fire = true;
			}

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
		}else {
			entity.takeOffSpeed = lerp(entity.takeOffSpeed, 5f, deltaTime/180f);
			Vec3 curPos = renderPos(entity);
			Vec3 v = new Vec3(curPos.x,curPos.y+entity.takeOffSpeed, curPos.z);
			entity.updateRenderPos(v.x, v.y, v.z);

			entity.upLeg01Rot = lerp(entity.upLeg01Rot, 3f, deltaTime);
			entity.upLeg23Rot = lerp(entity.upLeg23Rot, 3.3f, deltaTime);
			entity.uLeg01Rot = lerp(entity.uLeg01Rot, -2.7f, deltaTime);
			entity.uLeg23Rot = lerp(entity.uLeg23Rot, -2.7f, deltaTime);
			entity.openingRot = lerp(entity.openingRot, 0f, deltaTime);
			entity.fire = true;

			if(curPos.y > 250) {
				if(entity.rocketSound != null) {
					if(mcc.getSoundManager().isActive(entity.rocketSound)) {
						mcc.getSoundManager().stop(entity.rocketSound);
						entity.rocketSound = null;
					}
				}
			}
		}
	}

	private void smokeParticle(Level w, Vec3 pos, int amount) {
		for(int i = 0;i<amount;i++) {
			if(amount == 3) {
				w.addParticle(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, (DeliveryChestModel.TEX_RANDOM.nextFloat()*0.5f)-.25f, DeliveryChestModel.TEX_RANDOM.nextFloat()*-.3F, (DeliveryChestModel.TEX_RANDOM.nextFloat()*.5f)-.25f);
			}else if(amount==6){
				w.addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, (DeliveryChestModel.TEX_RANDOM.nextFloat()*0.5f)-.25f, DeliveryChestModel.TEX_RANDOM.nextFloat()*-.3F, (DeliveryChestModel.TEX_RANDOM.nextFloat()*.5f)-.25f);
			}else {
				w.addParticle(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, (DeliveryChestModel.TEX_RANDOM.nextFloat()*2f)-1f, DeliveryChestModel.TEX_RANDOM.nextFloat()*.3F, (DeliveryChestModel.TEX_RANDOM.nextFloat()*2f)-1f);
			}
		}
	}

	private void doParticlesForFire(EntityDeliveryChest entity) {
		Vec3 curPos = renderPos(entity);

		smokeParticle(entity.level(), curPos, 3);
		smokeParticle(entity.level(), curPos, 6);

		Vec3 ground = new Vec3(curPos.x, entity.level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int)curPos.x, (int)curPos.z), curPos.z);
		double dist = ground.distanceTo(curPos);
		if(dist < 0) {
			dist = -dist;
		}

		if(dist < 5) {
			if(dist > 4 && dist < 5) {
				smokeParticle(entity.level(), ground, 1);
			}
			else if(dist > 3 && dist < 4) {
				smokeParticle(entity.level(), ground, 2);
			}
			else if(dist > 2 && dist < 3) {
				smokeParticle(entity.level(), ground, 4);
			}
			else if(dist > 1 && dist < 2) {
				smokeParticle(entity.level(), ground, 8);
			}
			else if(dist > 0 && dist < 1) {
				smokeParticle(entity.level(), ground, 16);
			}
		}
	}

	@Override
	public boolean shouldRender(EntityDeliveryChest entity, Frustum visibleRegion, double cameraX, double cameraY, double cameraZ) {
		return true;
	}

	@Override
	public void render(EntityDeliveryChest entity, float yaw, float tickDelta, PoseStack matrices, MultiBufferSource VertexConsumers, int light) {
		this.checkModel();
		this.changeRotations(entity);
		this.applyRotations(entity);
		if(entity.fire) {
			this.doParticlesForFire(entity);
		}

		matrices.pushPose();
		matrices.translate(0, entity.renderOffY, entity.renderOffZ);

		matrices.pushPose();
			matrices.mulPose(new Quaternionf().rotationX((float)Math.toRadians(entity.renderRot)));
			matrices.translate(0, -1.5, 0);
			deliveryChestModel.renderToBuffer(matrices, VertexConsumers, light, OverlayTexture.NO_OVERLAY);
			matrices.pushPose();
				matrices.mulPose(new Quaternionf().rotationX((float)Math.toRadians(-90)));
				matrices.scale(0.02f, 0.02f, 0.02f);
				matrices.translate(-15.63, -15.63, 6.22);
				matrices.pushPose();
					matrices.scale(0.4f, 0.4f, 0.4f);
					if(ClientMod.myOrder != null) {
						if(entity.getDeliveryUUID().equals(ClientMod.myOrder.orderUUID)) {
							if(ClientMod.myOrder.currentStatus == OrderStatus.PAYMENT_CHEST_ARRIVED || ClientMod.myOrder.currentStatus == OrderStatus.PAYMENT_CHEST_RECEIVING) {
								matrices.translate(0, -5, 0);
								this.getFont().drawInBatch("Please insert", 6, 25, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
								String s = ""+ClientMod.myOrder.price;
								this.getFont().drawInBatch(s, (39) - this.getFont().width(s)/2, 33, 0, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
								this.getFont().drawInBatch("Iron Ingots", 10, 41, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
								this.getFont().drawInBatch("by clicking", 13, 50, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
								this.getFont().drawInBatch("this chest", 14, 59, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
							}else if(ClientMod.myOrder.currentStatus == OrderStatus.ORDER_CHEST_ARRIVED || ClientMod.myOrder.currentStatus == OrderStatus.ORDER_CHEST_RECEIVED) {
								String s = ClientMod.myOrder.items.size() + " items";
								this.getFont().drawInBatch(s, (39) - this.getFont().width(s)/2, 20, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, 0, light);
								matrices.pushPose();
									matrices.translate(0.5, 0, 0);
									this.getFont().drawInBatch("in chest", 19, 30, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, new Color(0f,0f,0f,0f).getRGB(), light);
								matrices.popPose();
								this.getFont().drawInBatch("Collect by", 14, 44, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, new Color(0f,0f,0f,0f).getRGB(), light);
								this.getFont().drawInBatch("clicking", 21, 52, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, new Color(0f,0f,0f,0f).getRGB(), light);
							}
						}else {
							this.getFont().drawInBatch("This is not", 13, 30, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, new Color(1f,0f,0f,1f).getRGB(), light);
							this.getFont().drawInBatch("your chest!", 10, 40, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, new Color(1f,0f,0f,1f).getRGB(), light);
						}
					}else {
						this.getFont().drawInBatch("This is not", 13, 30, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, new Color(1f,0f,0f,1f).getRGB(), light);
						this.getFont().drawInBatch("your chest!", 10, 40, -1, false, matrices.last().pose(), VertexConsumers, net.minecraft.client.gui.Font.DisplayMode.NORMAL, new Color(1f,0f,0f,1f).getRGB(), light);
					}
				matrices.popPose();
			matrices.popPose();
		matrices.popPose();

		matrices.popPose();

		super.render(entity, yaw, tickDelta, matrices, VertexConsumers, light);
	}

}

package newvmcomputers.entities;

import java.util.UUID;

import newvmcomputers.MainMod;
import newvmcomputers.client.ClientMod;
import newvmcomputers.item.ItemPackage;
import newvmcomputers.utils.TabletOrder;
import newvmcomputers.utils.TabletOrder.OrderStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraftforge.registries.ForgeRegistries;

public class EntityDeliveryChest extends Entity {

	private static final EntityDataAccessor<Float> TARGET_X =
			SynchedEntityData.defineId(EntityDeliveryChest.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> TARGET_Y =
			SynchedEntityData.defineId(EntityDeliveryChest.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> TARGET_Z =
			SynchedEntityData.defineId(EntityDeliveryChest.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> TAKING_OFF =
			SynchedEntityData.defineId(EntityDeliveryChest.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> DELIVERY_UUID =
			SynchedEntityData.defineId(EntityDeliveryChest.class, EntityDataSerializers.STRING);

	public float renderRot = 90f;
	public float upLeg01Rot = 3f;
	public float uLeg01Rot = -2.7f;
	public float upLeg23Rot = 3.3f;
	public float uLeg23Rot = -2.7f;
	public float openingRot = 0f;
	public float takeOffSpeed = 0f;
	public float renderOffY = 80;
	public float renderOffZ = -80;
	public boolean fire = false;
	public SoundInstance rocketSound;

	public float takeOffTime = 0f;

	public EntityDeliveryChest(EntityType<?> type, Level level) {
		super(type, level);
	}

	public EntityDeliveryChest(Level level, Vec3 target, UUID owner) {
		super(EntityList.DELIVERY_CHEST.get(), level);
		this.getEntityData().set(TARGET_X, (float) target.x);
		this.getEntityData().set(TARGET_Y, (float) target.y);
		this.getEntityData().set(TARGET_Z, (float) target.z);
		this.getEntityData().set(DELIVERY_UUID, owner.toString());
		this.setPos(target.x, target.y, target.z);
	}

	public EntityDeliveryChest(Level level, double targetX, double targetY, double targetZ) {
		super(EntityList.DELIVERY_CHEST.get(), level);
		this.getEntityData().set(TARGET_X, (float) targetX);
		this.getEntityData().set(TARGET_Y, (float) targetY);
		this.getEntityData().set(TARGET_Z, (float) targetZ);
		this.setPos(targetX, targetY, targetZ);
	}

	@Override
	protected void defineSynchedData() {
		this.getEntityData().define(TARGET_X, 0f);
		this.getEntityData().define(TARGET_Y, 0f);
		this.getEntityData().define(TARGET_Z, 0f);
		this.getEntityData().define(DELIVERY_UUID, "");
		this.getEntityData().define(TAKING_OFF, false);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.getEntityData().set(TARGET_X, tag.getFloat("TargetX"));
		this.getEntityData().set(TARGET_Y, tag.getFloat("TargetY"));
		this.getEntityData().set(TARGET_Z, tag.getFloat("TargetZ"));
		this.getEntityData().set(DELIVERY_UUID, tag.getString("DeliveryUUID"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("TargetX", this.getEntityData().get(TARGET_X));
		tag.putFloat("TargetY", this.getEntityData().get(TARGET_Y));
		tag.putFloat("TargetZ", this.getEntityData().get(TARGET_Z));
		tag.putString("DeliveryUUID", this.getEntityData().get(DELIVERY_UUID));
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			if (this.getDeliveryUUID().isEmpty()) {
				this.discard();
			} else if (!MainMod.orders.containsKey(UUID.fromString(this.getDeliveryUUID()))) {
				this.discard();
			} else {
				TabletOrder to = MainMod.orders.get(UUID.fromString(getDeliveryUUID()));
				if (to.currentStatus == OrderStatus.PAYMENT_CHEST_RECEIVING || to.currentStatus == OrderStatus.ORDER_CHEST_RECEIVED) {
					this.getEntityData().set(TAKING_OFF, true);

					takeOffTime += 0.05f;

					if (takeOffTime > 0.5f) {
						this.discard();
						to.entitySpawned = false;
						if (to.currentStatus == OrderStatus.PAYMENT_CHEST_RECEIVING) {
							to.currentStatus = OrderStatus.ORDER_CHEST_ARRIVAL_SOON;
						}
					}
				}
			}
		}
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (player.level().isClientSide) {
			return InteractionResult.FAIL;
		}
		if (hand == InteractionHand.OFF_HAND) {
			return InteractionResult.FAIL;
		}
		if (player.getStringUUID().equals(getDeliveryUUID())) {
			TabletOrder to = MainMod.orders.get(UUID.fromString(getDeliveryUUID()));
			if (to.currentStatus == OrderStatus.PAYMENT_CHEST_ARRIVED) {
				ItemStack is = player.getMainHandItem();

				boolean flag = false;

				if (!is.isEmpty()) {
					if (is.getItem().equals(Items.IRON_INGOT)) {
						to.price -= is.getCount();
						is.shrink(is.getCount());
						flag = true;
					}
				}

				if (!flag) {
					player.sendSystemMessage(Component.translatable("newvmcomputers.click_with_ingots").withStyle(ChatFormatting.RED));
				} else {
					if (to.price < 0) {
						is.grow(to.price * -1);
						to.currentStatus = OrderStatus.PAYMENT_CHEST_RECEIVING;
					} else if (to.price == 0) {
						to.currentStatus = OrderStatus.PAYMENT_CHEST_RECEIVING;
					}
				}

				return flag ? InteractionResult.SUCCESS : InteractionResult.FAIL;
			} else if (to.currentStatus == OrderStatus.ORDER_CHEST_ARRIVED) {

				player.level().addFreshEntity(new ItemEntity(player.level(), this.getX(), this.getY() + 1.5, this.getZ(),
						ItemPackage.createPackage(ForgeRegistries.ITEMS.getKey(to.items.get(0)))));
				to.items.remove(0);
				if (to.items.isEmpty()) {
					to.currentStatus = OrderStatus.ORDER_CHEST_RECEIVED;
				}
			}
		}

		return super.interact(player, hand);
	}

	public float getTargetX() {
		return this.getEntityData().get(TARGET_X);
	}

	public float getTargetY() {
		return this.getEntityData().get(TARGET_Y);
	}

	public float getTargetZ() {
		return this.getEntityData().get(TARGET_Z);
	}

	public String getDeliveryUUID() {
		return this.getEntityData().get(DELIVERY_UUID);
	}

	public Boolean getTakingOff() {
		return this.getEntityData().get(TAKING_OFF);
	}

	public void updateRenderPos(double x, double y, double z) {
		this.renderOffY = (float) (y - this.getY());
		this.renderOffZ = (float) (z - this.getZ());
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);
		if (level().isClientSide) {
			ClientMod.currentDeliveryChest = this;
			MainMod.deliveryChestSound.run();
		}
	}
}


package mcvmcomputers.entities;

import mcvmcomputers.MainMod;
import mcvmcomputers.item.ItemList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

import net.minecraft.world.level.Level;

public class EntityWallTV extends Entity{
	private static final EntityDataAccessor<Float> LOOK_AT_POS_X =
			SynchedEntityData.defineId(EntityWallTV.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Y =
			SynchedEntityData.defineId(EntityWallTV.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Z =
			SynchedEntityData.defineId(EntityWallTV.class, EntityDataSerializers.FLOAT);

	private static final EntityDataAccessor<String> OWNER_UUID =
			SynchedEntityData.defineId(EntityWallTV.class, EntityDataSerializers.STRING);

	public EntityWallTV(EntityType<?> type, Level level) {
		super(type, level);
	}

	public EntityWallTV(Level level, double x, double y, double z) {
		this(EntityList.WALLTV, level);
		this.setPos(x, y, z);
	}

	public EntityWallTV(Level level, Double x, Double y, Double z, Vec3 lookAt, String uuid) {
		this(EntityList.WALLTV, level);
		this.setPos(x, y, z);
		this.getEntityData().set(LOOK_AT_POS_X, (float)lookAt.x);
		this.getEntityData().set(LOOK_AT_POS_Y, (float)lookAt.y);
		this.getEntityData().set(LOOK_AT_POS_Z, (float)lookAt.z);
		this.getEntityData().set(OWNER_UUID, uuid);
	}

	public Vec3 getLookAtPos() {
		return new Vec3(this.getEntityData().get(LOOK_AT_POS_X),
						 this.getEntityData().get(LOOK_AT_POS_Y),
						 this.getEntityData().get(LOOK_AT_POS_Z));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(LOOK_AT_POS_X, 0f);
		builder.define(LOOK_AT_POS_Y, 0f);
		builder.define(LOOK_AT_POS_Z, 0f);
		builder.define(OWNER_UUID, "");
	}
	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.getEntityData().set(LOOK_AT_POS_X, tag.getFloat("LookAtX"));
		this.getEntityData().set(LOOK_AT_POS_Y, tag.getFloat("LookAtY"));
		this.getEntityData().set(LOOK_AT_POS_Z, tag.getFloat("LookAtZ"));
		this.getEntityData().set(OWNER_UUID, tag.getString("Owner"));
	}
	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("LookAtX", this.getEntityData().get(LOOK_AT_POS_X));
		tag.putFloat("LookAtY", this.getEntityData().get(LOOK_AT_POS_Y));
		tag.putFloat("LookAtZ", this.getEntityData().get(LOOK_AT_POS_Z));
		tag.putString("Owner", this.getEntityData().get(OWNER_UUID));
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if(!player.level().isClientSide()) {
			if(player.isCrouching()) {
				this.kill();
				player.level().addFreshEntity(new ItemEntity(player.level(),
						this.position().x, this.position().y, this.position().z,
						new ItemStack(ItemList.ITEM_WALLTV)));
			}
		}else {
			if(!player.isCrouching()) {
				if(this.getOwnerUUID().equals(player.getStringUUID())) {
					MainMod.focus.run();
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tick() {
		if(getOwnerUUID().isEmpty()) {
			this.kill();
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	public String getOwnerUUID() {
		return this.getEntityData().get(OWNER_UUID);
	}


}

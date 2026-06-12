package mcvmcomputers.entities;

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

public class EntityKeyboard extends Entity{
	private static final EntityDataAccessor<Float> LOOK_AT_POS_X =
			SynchedEntityData.defineId(EntityKeyboard.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Y =
			SynchedEntityData.defineId(EntityKeyboard.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Z =
			SynchedEntityData.defineId(EntityKeyboard.class, EntityDataSerializers.FLOAT);

	public EntityKeyboard(EntityType<?> type, Level level) {
		super(type, level);
	}

	public EntityKeyboard(Level level, double x, double y, double z) {
		this(EntityList.KEYBOARD, level);
		this.setPos(x, y, z);
	}

	public EntityKeyboard(Level level, Double x, Double y, Double z, Vec3 lookAt, String uuid) {
		this(EntityList.KEYBOARD, level);
		this.setPos(x, y, z);
		this.getEntityData().set(LOOK_AT_POS_X, (float)lookAt.x);
		this.getEntityData().set(LOOK_AT_POS_Y, (float)lookAt.y);
		this.getEntityData().set(LOOK_AT_POS_Z, (float)lookAt.z);
	}

	public Vec3 getLookAtPos() {
		return new Vec3(this.getEntityData().get(LOOK_AT_POS_X), this.getEntityData().get(LOOK_AT_POS_Y), this.getEntityData().get(LOOK_AT_POS_Z));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(LOOK_AT_POS_X, 0f);
		builder.define(LOOK_AT_POS_Y, 0f);
		builder.define(LOOK_AT_POS_Z, 0f);
	}
	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.getEntityData().set(LOOK_AT_POS_X, tag.getFloat("LookAtX"));
		this.getEntityData().set(LOOK_AT_POS_Y, tag.getFloat("LookAtY"));
		this.getEntityData().set(LOOK_AT_POS_Z, tag.getFloat("LookAtZ"));
	}
	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("LookAtX", this.getEntityData().get(LOOK_AT_POS_X));
		tag.putFloat("LookAtY", this.getEntityData().get(LOOK_AT_POS_Y));
		tag.putFloat("LookAtZ", this.getEntityData().get(LOOK_AT_POS_Z));
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if(!player.level().isClientSide()) {
			if(player.isCrouching()) {
				this.kill();
				player.level().addFreshEntity(new ItemEntity(player.level(),
						this.position().x, this.position().y, this.position().z,
						new ItemStack(ItemList.ITEM_KEYBOARD)));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return true;
	}


}

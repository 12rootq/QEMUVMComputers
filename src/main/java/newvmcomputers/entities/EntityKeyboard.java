package newvmcomputers.entities;

import newvmcomputers.item.ItemList;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public class EntityKeyboard extends Entity {
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
		this(EntityList.KEYBOARD.get(), level);
		this.setPos(x, y, z);
	}

	public EntityKeyboard(Level level, Double x, Double y, Double z, Vec3 lookAt, String uuid) {
		this(EntityList.KEYBOARD.get(), level);
		this.setPos(x, y, z);
		this.getEntityData().set(LOOK_AT_POS_X, (float) lookAt.x);
		this.getEntityData().set(LOOK_AT_POS_Y, (float) lookAt.y);
		this.getEntityData().set(LOOK_AT_POS_Z, (float) lookAt.z);
	}

	public Vec3 getLookAtPos() {
		return new Vec3(this.getEntityData().get(LOOK_AT_POS_X), this.getEntityData().get(LOOK_AT_POS_Y), this.getEntityData().get(LOOK_AT_POS_Z));
	}

	@Override
	protected void defineSynchedData() {
		this.getEntityData().define(LOOK_AT_POS_X, 0f);
		this.getEntityData().define(LOOK_AT_POS_Y, 0f);
		this.getEntityData().define(LOOK_AT_POS_Z, 0f);
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
		if (!player.level().isClientSide) {
			if (player.isShiftKeyDown()) {
				this.discard();
				player.level().addFreshEntity(new ItemEntity(player.level(),
						this.getX(), this.getY(), this.getZ(),
						new ItemStack(ItemList.ITEM_KEYBOARD.get())));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}

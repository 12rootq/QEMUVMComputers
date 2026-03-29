package newvmcomputers.entities;

import newvmcomputers.MainMod;
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

public class EntityCRTScreen extends Entity {
	private static final EntityDataAccessor<Float> LOOK_AT_POS_X =
			SynchedEntityData.defineId(EntityCRTScreen.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Y =
			SynchedEntityData.defineId(EntityCRTScreen.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Z =
			SynchedEntityData.defineId(EntityCRTScreen.class, EntityDataSerializers.FLOAT);

	private static final EntityDataAccessor<String> OWNER_UUID =
			SynchedEntityData.defineId(EntityCRTScreen.class, EntityDataSerializers.STRING);

	public EntityCRTScreen(EntityType<?> type, Level level) {
		super(type, level);
	}

	public EntityCRTScreen(Level level, double x, double y, double z) {
		this(EntityList.CRT_SCREEN.get(), level);
		this.setPos(x, y, z);
	}

	public EntityCRTScreen(Level level, Double x, Double y, Double z, Vec3 lookAt, String uuid) {
		this(EntityList.CRT_SCREEN.get(), level);
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
	protected void defineSynchedData() {
		this.getEntityData().define(LOOK_AT_POS_X, 0f);
		this.getEntityData().define(LOOK_AT_POS_Y, 0f);
		this.getEntityData().define(LOOK_AT_POS_Z, 0f);
		this.getEntityData().define(OWNER_UUID, "");
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
		if (!player.level().isClientSide) {
			if (player.isShiftKeyDown()) {
				this.discard();
				player.level().addFreshEntity(new ItemEntity(player.level(),
						this.getX(), this.getY(), this.getZ(),
						new ItemStack(ItemList.ITEM_CRTSCREEN.get())));
			}
		} else {
			if (!player.isShiftKeyDown()) {
				if (this.getOwnerUUID().equals(player.getStringUUID())) {
					MainMod.focus.run();
				}
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void tick() {
		super.tick();
		if (getOwnerUUID().isEmpty()) {
			this.discard();
		}
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	public String getOwnerUUID() {
		return this.getEntityData().get(OWNER_UUID);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return new ClientboundAddEntityPacket(this);
	}
}
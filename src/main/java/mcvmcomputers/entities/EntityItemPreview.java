package mcvmcomputers.entities;

import mcvmcomputers.client.ClientMod;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;

import net.minecraft.world.level.Level;

public class EntityItemPreview extends Entity{
	private static final EntityDataAccessor<ItemStack> PREVIEWED_STACK =
				SynchedEntityData.defineId(EntityItemPreview.class, EntityDataSerializers.ITEM_STACK);

	public EntityItemPreview(EntityType<?> type, Level level) {
		super(type, level);
	}

	public EntityItemPreview(Level level, double x, double y, double z, ItemStack stack) {
		this(EntityList.ITEM_PREVIEW, level);
		this.setPos(x, y, z);
		this.getEntityData().set(PREVIEWED_STACK, stack);
	}

	public EntityItemPreview(Level level, double x, double y, double z) {
		this(EntityList.ITEM_PREVIEW, level);
		this.setPos(x, y, z);
	}

	@Override
	public void tick() {
		if(this.level().isClientSide()) {
			if(this != ClientMod.thePreviewEntity) {
				this.kill();
			}
		}else {
			this.kill();
		}
	}

	public void setItem(ItemStack is) {
		this.getEntityData().set(PREVIEWED_STACK, is);
	}

	public ItemStack getPreviewedItemStack() {
		return this.getEntityData().get(PREVIEWED_STACK);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(PREVIEWED_STACK, new ItemStack(Items.REDSTONE_BLOCK));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if(tag.contains("Item")) {
			this.getEntityData().set(PREVIEWED_STACK, ItemStack.parse(this.level().registryAccess(), tag.getCompound("Item")).orElse(ItemStack.EMPTY));
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.put("Item", this.getEntityData().get(PREVIEWED_STACK).save(this.level().registryAccess()));
	}


}

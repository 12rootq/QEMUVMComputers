package mcvmcomputers.entities;

import mcvmcomputers.client.ClientMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound; // Изменено
import net.minecraft.network.listener.ClientPlayPacketListener; // Добавлено
import net.minecraft.network.packet.Packet; // Обновлен импорт
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.world.World;

public class EntityItemPreview extends Entity {
	private static final TrackedData<ItemStack> PREVIEWED_STACK =
			DataTracker.registerData(EntityItemPreview.class, TrackedDataHandlerRegistry.ITEM_STACK);

	public EntityItemPreview(EntityType<?> type, World world) {
		super(type, world);
	}

	public EntityItemPreview(World world, double x, double y, double z, ItemStack stack) {
		this(EntityList.ITEM_PREVIEW, world);
		this.updatePosition(x, y, z);
		this.getDataTracker().set(PREVIEWED_STACK, stack);
	}

	public EntityItemPreview(World world, double x, double y, double z) {
		this(EntityList.ITEM_PREVIEW, world);
		this.updatePosition(x, y, z);
	}

	@Override
	public void tick() {
		super.tick(); // Рекомендуется добавлять super.tick()
		if(this.getWorld().isClient) { // Заменено world на getWorld()
			if(this != ClientMod.thePreviewEntity) {
				this.kill();
			}
		}else {
			this.kill();
		}
	}

	public void setItem(ItemStack is) {
		this.getDataTracker().set(PREVIEWED_STACK, is);
	}

	public ItemStack getPreviewedItemStack() {
		return this.getDataTracker().get(PREVIEWED_STACK);
	}

	@Override
	protected void initDataTracker() {
		this.getDataTracker().startTracking(PREVIEWED_STACK, new ItemStack(Items.REDSTONE_BLOCK));
	}

	// Переименовано под 1.20.1
	@Override
	protected void readCustomDataFromNbt(NbtCompound tag) {
		if(tag.contains("Item")) {
			// Метод fromTag заменен на fromNbt
			this.getDataTracker().set(PREVIEWED_STACK, ItemStack.fromNbt(tag.getCompound("Item")));
		}
	}

	// Переименовано под 1.20.1
	@Override
	protected void writeCustomDataToNbt(NbtCompound tag) {
		// Метод toTag заменен на writeNbt
		tag.put("Item", this.getDataTracker().get(PREVIEWED_STACK).writeNbt(new NbtCompound()));
	}

	// Обновлено под 1.20.1
	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket() {
		return new EntitySpawnS2CPacket(this);
	}

}
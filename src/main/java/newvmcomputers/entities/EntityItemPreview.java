package newvmcomputers.entities;

import newvmcomputers.client.ClientMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public class EntityItemPreview extends Entity {
    private static final EntityDataAccessor<ItemStack> PREVIEWED_STACK =
            SynchedEntityData.defineId(EntityItemPreview.class, EntityDataSerializers.ITEM_STACK);

    public EntityItemPreview(EntityType<?> type, Level level) {
        super(type, level);
    }

    public EntityItemPreview(Level level, double x, double y, double z, ItemStack stack) {
        this(EntityList.ITEM_PREVIEW.get(), level);
        this.setPos(x, y, z);
        this.getEntityData().set(PREVIEWED_STACK, stack);
    }

    public EntityItemPreview(Level level, double x, double y, double z) {
        this(EntityList.ITEM_PREVIEW.get(), level);
        this.setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this != ClientMod.thePreviewEntity) {
                this.discard();
            }
        } else {
            this.discard();
        }
    }

    public void setItem(ItemStack is) {
        this.getEntityData().set(PREVIEWED_STACK, is);
    }

    public ItemStack getPreviewedItemStack() {
        return this.getEntityData().get(PREVIEWED_STACK);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(PREVIEWED_STACK, new ItemStack(Items.REDSTONE_BLOCK));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Item")) {
            this.getEntityData().set(PREVIEWED_STACK, ItemStack.of(tag.getCompound("Item")));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("Item", this.getEntityData().get(PREVIEWED_STACK).save(new CompoundTag()));
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}

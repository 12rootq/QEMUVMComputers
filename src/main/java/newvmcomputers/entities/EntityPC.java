package newvmcomputers.entities;

import java.util.UUID;

import newvmcomputers.MainMod;
import newvmcomputers.client.ClientMod;
import newvmcomputers.item.ItemPCCase;
import newvmcomputers.item.ItemPCCaseSidepanel;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public class EntityPC extends Entity {
	private static final EntityDataAccessor<String> ISO_FILE_NAME =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> HARD_DRIVE_FILE_NAME =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.STRING);
	private static final EntityDataAccessor<String> OWNER_UUID =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.STRING);

	private static final EntityDataAccessor<Float> LOOK_AT_POS_X =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Y =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LOOK_AT_POS_Z =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.FLOAT);

	private static final EntityDataAccessor<Integer> CPU_DIVIDED_BY =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> GB_OF_RAM_IN_SLOT_0 =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> GB_OF_RAM_IN_SLOT_1 =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.INT);

	private static final EntityDataAccessor<Boolean> SIXTY_FOUR_BIT =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> GPU_IN_PCI_SLOT =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> GLASS_SIDEPANEL =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> MOTHERBOARD_INSTALLED =
			SynchedEntityData.defineId(EntityPC.class, EntityDataSerializers.BOOLEAN);

	public EntityPC(EntityType<?> type, Level level) {
		super(type, level);
	}

	public EntityPC(Level level, double x, double y, double z) {
		this(EntityList.PC.get(), level);
		this.setPos(x, y, z);
	}

	public EntityPC(Level level, double x, double y, double z, Vec3 lookAt, UUID owner, CompoundTag tag) {
		this(EntityList.PC.get(), level);
		this.setPos(x, y, z);
		this.getEntityData().set(LOOK_AT_POS_X, (float) lookAt.x);
		this.getEntityData().set(LOOK_AT_POS_Y, (float) lookAt.y);
		this.getEntityData().set(LOOK_AT_POS_Z, (float) lookAt.z);
		this.getEntityData().set(OWNER_UUID, owner.toString());

		if (tag != null) {
			if (tag.contains("x64"))
				this.getEntityData().set(SIXTY_FOUR_BIT, tag.getBoolean("x64"));
			if (tag.contains("MoboInstalled"))
				this.getEntityData().set(MOTHERBOARD_INSTALLED, tag.getBoolean("MoboInstalled"));
			if (tag.contains("GPUInstalled"))
				this.getEntityData().set(GPU_IN_PCI_SLOT, tag.getBoolean("GPUInstalled"));
			if (tag.contains("CPUDividedBy"))
				this.getEntityData().set(CPU_DIVIDED_BY, tag.getInt("CPUDividedBy"));
			if (tag.contains("RAMSlot0"))
				this.getEntityData().set(GB_OF_RAM_IN_SLOT_0, tag.getInt("RAMSlot0"));
			if (tag.contains("RAMSlot1"))
				this.getEntityData().set(GB_OF_RAM_IN_SLOT_1, tag.getInt("RAMSlot1"));
			if (tag.contains("VHDName"))
				this.getEntityData().set(HARD_DRIVE_FILE_NAME, tag.getString("VHDName"));
			if (tag.contains("ISOName"))
				this.getEntityData().set(ISO_FILE_NAME, tag.getString("ISOName"));
		}
	}

	public EntityPC(Level level, double x, double y, double z, Vec3 lookAt, UUID owner, boolean glassSidepanel, CompoundTag tag) {
		this(level, x, y, z, lookAt, owner, tag);
		this.getEntityData().set(GLASS_SIDEPANEL, glassSidepanel);
	}

	public Vec3 getLookAtPos() {
		return new Vec3(this.getEntityData().get(LOOK_AT_POS_X), this.getEntityData().get(LOOK_AT_POS_Y), this.getEntityData().get(LOOK_AT_POS_Z));
	}

	@Override
	protected void defineSynchedData() {
		this.getEntityData().define(HARD_DRIVE_FILE_NAME, "");
		this.getEntityData().define(ISO_FILE_NAME, "");
		this.getEntityData().define(OWNER_UUID, "");
		this.getEntityData().define(LOOK_AT_POS_X, 0f);
		this.getEntityData().define(LOOK_AT_POS_Y, 0f);
		this.getEntityData().define(LOOK_AT_POS_Z, 0f);
		this.getEntityData().define(GB_OF_RAM_IN_SLOT_0, 0);
		this.getEntityData().define(GB_OF_RAM_IN_SLOT_1, 0);
		this.getEntityData().define(CPU_DIVIDED_BY, 0);
		this.getEntityData().define(GPU_IN_PCI_SLOT, false);
		this.getEntityData().define(MOTHERBOARD_INSTALLED, false);
		this.getEntityData().define(GLASS_SIDEPANEL, false);
		this.getEntityData().define(SIXTY_FOUR_BIT, false);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.getEntityData().set(LOOK_AT_POS_X, tag.getFloat("LookAtX"));
		this.getEntityData().set(LOOK_AT_POS_Y, tag.getFloat("LookAtY"));
		this.getEntityData().set(LOOK_AT_POS_Z, tag.getFloat("LookAtZ"));

		if (tag.contains("Owner")) {
			this.getEntityData().set(OWNER_UUID, tag.getString("Owner"));
		}

		if (tag.contains("X64")) {
			this.getEntityData().set(SIXTY_FOUR_BIT, tag.getBoolean("X64"));
		}

		if (tag.contains("CpuDividedBy")) {
			this.getEntityData().set(CPU_DIVIDED_BY, tag.getInt("CpuDividedBy"));
		}

		if (tag.contains("IsoFileName")) {
			this.getEntityData().set(ISO_FILE_NAME, tag.getString("IsoFileName"));
		}

		if (tag.contains("GbRamSlot0")) {
			this.getEntityData().set(GB_OF_RAM_IN_SLOT_0, tag.getInt("GbRamSlot0"));
		}

		if (tag.contains("GbRamSlot1")) {
			this.getEntityData().set(GB_OF_RAM_IN_SLOT_1, tag.getInt("GbRamSlot1"));
		}

		if (tag.contains("GpuInstalled")) {
			this.getEntityData().set(GPU_IN_PCI_SLOT, tag.getBoolean("GpuInstalled"));
		}

		if (tag.contains("HardDriveFileName")) {
			this.getEntityData().set(HARD_DRIVE_FILE_NAME, tag.getString("HardDriveFileName"));
		}

		if (tag.contains("MotherboardInstalled")) {
			this.getEntityData().set(MOTHERBOARD_INSTALLED, tag.getBoolean("MotherboardInstalled"));
		}

		if (tag.contains("GlassSidepanel")) {
			this.getEntityData().set(GLASS_SIDEPANEL, tag.getBoolean("GlassSidepanel"));
		}
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putBoolean("X64", this.getEntityData().get(SIXTY_FOUR_BIT));
		tag.putFloat("LookAtX", this.getEntityData().get(LOOK_AT_POS_X));
		tag.putFloat("LookAtY", this.getEntityData().get(LOOK_AT_POS_Y));
		tag.putFloat("LookAtZ", this.getEntityData().get(LOOK_AT_POS_Z));
		tag.putInt("CpuDividedBy", this.getEntityData().get(CPU_DIVIDED_BY));
		tag.putString("IsoFileName", this.getEntityData().get(ISO_FILE_NAME));
		tag.putInt("GbRamSlot0", this.getEntityData().get(GB_OF_RAM_IN_SLOT_0));
		tag.putInt("GbRamSlot1", this.getEntityData().get(GB_OF_RAM_IN_SLOT_1));
		tag.putBoolean("GpuInstalled", this.getEntityData().get(GPU_IN_PCI_SLOT));
		tag.putString("HardDriveFileName", this.getEntityData().get(HARD_DRIVE_FILE_NAME));
		tag.putBoolean("MotherboardInstalled", this.getEntityData().get(MOTHERBOARD_INSTALLED));
		tag.putBoolean("GlassSidepanel", this.getEntityData().get(GLASS_SIDEPANEL));
		tag.putString("Owner", this.getEntityData().get(OWNER_UUID));
	}

	public String getHardDriveFileName() { return this.getEntityData().get(HARD_DRIVE_FILE_NAME); }
	public String getIsoFileName() { return this.getEntityData().get(ISO_FILE_NAME); }
	public String getOwner() { return this.getEntityData().get(OWNER_UUID); }
	public int getGigsOfRamInSlot0() { return this.getEntityData().get(GB_OF_RAM_IN_SLOT_0); }
	public int getGigsOfRamInSlot1() { return this.getEntityData().get(GB_OF_RAM_IN_SLOT_1); }
	public int getCpuDividedBy() { return this.getEntityData().get(CPU_DIVIDED_BY); }
	public boolean getGpuInstalled() { return this.getEntityData().get(GPU_IN_PCI_SLOT); }
	public boolean getMotherboardInstalled() { return this.getEntityData().get(MOTHERBOARD_INSTALLED); }
	public boolean getGlassSidepanel() { return this.getEntityData().get(GLASS_SIDEPANEL); }
	public boolean get64Bit() { return this.getEntityData().get(SIXTY_FOUR_BIT); }

	public void setOwner(String uid) { this.getEntityData().set(OWNER_UUID, uid); }
	public void setGigsOfRamInSlot0(int gb) { this.getEntityData().set(GB_OF_RAM_IN_SLOT_0, gb); }
	public void setGigsOfRamInSlot1(int gb) { this.getEntityData().set(GB_OF_RAM_IN_SLOT_1, gb); }
	public void setGpuInstalled(boolean installed) { this.getEntityData().set(GPU_IN_PCI_SLOT, installed); }
	public void setCpuDividedBy(int dividedBy) { this.getEntityData().set(CPU_DIVIDED_BY, dividedBy); }
	public void setHardDriveFileName(String fileName) { this.getEntityData().set(HARD_DRIVE_FILE_NAME, fileName); }
	public void setIsoFileName(String fileName) { this.getEntityData().set(ISO_FILE_NAME, fileName); }
	public void setMotherboardInstalled(boolean installed) { this.getEntityData().set(MOTHERBOARD_INSTALLED, installed); }
	public void set64Bit(boolean sixtyFourBit) { this.getEntityData().set(SIXTY_FOUR_BIT, sixtyFourBit); }

	@Override
	public InteractionResult interact(Player player, InteractionHand hand) {
		if (!player.level().isClientSide) {
			if (player.isShiftKeyDown() && player.getStringUUID().equals(this.getOwner())) {
				if (this.getGlassSidepanel()) {
					player.level().addFreshEntity(new ItemEntity(player.level(),
							this.getX(), this.getY(), this.getZ(),
							ItemPCCaseSidepanel.createPCStackByEntity(this)));
				} else {
					player.level().addFreshEntity(new ItemEntity(player.level(),
							this.getX(), this.getY(), this.getZ(),
							ItemPCCase.createPCStackByEntity(this)));
				}
				this.discard();
			}
		} else {
			if (!player.isShiftKeyDown()) {
				if (this.getOwner().equals(player.getStringUUID())) {
					ClientMod.currentPC = this;
					MainMod.pcOpenGui.run();
				} else {
					player.sendSystemMessage(Component.translatable("newvmcomputers.not_your_computer").withStyle(ChatFormatting.RED));
				}
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


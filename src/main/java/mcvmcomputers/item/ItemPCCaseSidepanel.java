package mcvmcomputers.item;

import java.util.List;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.entities.EntityPC;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class ItemPCCaseSidepanel extends OrderableItem{
	public ItemPCCaseSidepanel(Settings settings) {
		super(settings, 6);
	}

	private static NbtCompound getNbtSafe(ItemStack stack) {
		NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
		return comp != null ? comp.copyNbt() : null;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if(!world.isClient && hand == Hand.MAIN_HAND) {
			NbtCompound tag = getNbtSafe(user.getStackInHand(hand));
			user.getStackInHand(hand).decrement(1);
			HitResult hr = user.raycast(10, 0f, false);
			EntityPC ek = new EntityPC(world,
									hr.getPos().getX(),
									hr.getPos().getY(),
									hr.getPos().getZ(),
									new Vec3d(user.getPos().x,
												hr.getPos().getY(),
												user.getPos().z), user.getUuid(), true, tag);
			world.spawnEntity(ek);
		}

		if(world.isClient) {
			world.playSound(ClientMod.thePreviewEntity.getX(),
							ClientMod.thePreviewEntity.getY(),
							ClientMod.thePreviewEntity.getZ(),
							SoundEvents.BLOCK_METAL_PLACE,
							SoundCategory.BLOCKS, 1, 1, true);
		}

		return new TypedActionResult<ItemStack>(ActionResult.SUCCESS, user.getStackInHand(hand));
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		NbtCompound n = getNbtSafe(stack);
		if(n != null) {
			if (n.contains("MotherboardInstalled")) {
				if(n.getBoolean("MotherboardInstalled")) {
					tooltip.add(Text.translatable(n.getBoolean("X64") ? "item.mcvmcomputers.motherboard64" : "item.mcvmcomputers.motherboard").formatted(Formatting.GRAY));
					if(n.getBoolean("GpuInstalled"))
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_gpu").formatted(Formatting.GRAY));
					if(n.getInt("CpuDividedBy") > 0)
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_cpu", n.getInt("CpuDividedBy")).formatted(Formatting.GRAY));
					if(n.getInt("GbRamSlot0") > 0) {
						if((n.getInt("GbRamSlot0") / 1024) < 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot0Mb", n.getInt("GbRamSlot0")).formatted(Formatting.GRAY));
						} else if((n.getInt("GbRamSlot0") / 1024) >= 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot0", (n.getInt("GbRamSlot0") / 1024)).formatted(Formatting.GRAY));
					}}
					if(n.getInt("GbRamSlot1") > 0) {
						if((n.getInt("GbRamSlot1") / 1024) < 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot1Mb", n.getInt("GbRamSlot1")).formatted(Formatting.GRAY));
						} else if((n.getInt("GbRamSlot1") / 1024) >= 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot1", (n.getInt("GbRamSlot1") / 1024)).formatted(Formatting.GRAY));
					}}
					if(!n.getString("HardDriveFileName").isEmpty())
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_hdd", n.getString("HardDriveFileName")).formatted(Formatting.GRAY));
					if(!n.getString("IsoFileName").isEmpty())
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_iso", n.getString("IsoFileName")).formatted(Formatting.GRAY));
				}
			}
		}
	}

	@Override
	public Text getName(ItemStack stack) {
		NbtCompound n = getNbtSafe(stack);
		if(n != null) {
			if (n.contains("MotherboardInstalled")) {
				if(n.getBoolean("MotherboardInstalled")) {
					return Text.translatable("mcvmcomputers.pc_item_built");
				}
			}
		}
		return Text.translatable("item.mcvmcomputers.pc_case_sidepanel");
	}

	public static ItemStack createPCStackByEntity(EntityPC pc) {
		ItemStack is = new ItemStack(ItemList.PC_CASE_SIDEPANEL);
		if(pc.getMotherboardInstalled()) {
			NbtCompound ct = new NbtCompound();
			ct.putBoolean("X64", pc.get64Bit());
			ct.putBoolean("MotherboardInstalled", pc.getMotherboardInstalled());
			ct.putBoolean("GpuInstalled", pc.getGpuInstalled());
			ct.putInt("CpuDividedBy", pc.getCpuDividedBy());
			ct.putInt("GbRamSlot0", pc.getGigsOfRamInSlot0());
			ct.putInt("GbRamSlot1", pc.getGigsOfRamInSlot1());
			ct.putString("HardDriveFileName", pc.getHardDriveFileName());
			ct.putString("IsoFileName", pc.getIsoFileName());
			is.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(ct));
		}
		return is;
	}
}

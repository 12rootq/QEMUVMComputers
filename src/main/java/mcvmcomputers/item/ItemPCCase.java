package mcvmcomputers.item;

import java.util.List;

import mcvmcomputers.MainMod;
import mcvmcomputers.client.ClientMod;
import mcvmcomputers.entities.EntityPC;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * The PC case item. Placing it spawns an EntityPC carrying over any saved
 * hardware NBT; its tooltip and name reflect the installed components.
 */
public class ItemPCCase extends OrderableItem{
	public ItemPCCase(Settings settings) {
		super(settings, 2);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if(!world.isClient && hand == Hand.MAIN_HAND) {
			NbtCompound tag = user.getStackInHand(hand).getNbt();
			user.getStackInHand(hand).decrement(1);
			HitResult hr = user.raycast(10, 0f, false);
			EntityPC ek = new EntityPC(world,
									hr.getPos().getX(),
									hr.getPos().getY(),
									hr.getPos().getZ(),
									new Vec3d(user.getPos().x,
												hr.getPos().getY(),
												user.getPos().z), user.getUuid(), tag);
			world.spawnEntity(ek);
			MainMod.computers.put(user.getUuid(), ek);
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
	public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
		if(stack.getNbt() != null) {
			if (stack.getNbt().contains("MotherboardInstalled")) {
				if(stack.getNbt().getBoolean("MotherboardInstalled")) {
					tooltip.add(Text.translatable(stack.getNbt().getBoolean("X64") ? "item.mcvmcomputers.motherboard64" : "item.mcvmcomputers.motherboard").formatted(Formatting.GRAY));
					if(stack.getNbt().getBoolean("GpuInstalled"))
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_gpu").formatted(Formatting.GRAY));
					if(stack.getNbt().getInt("CpuDividedBy") > 0)
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_cpu", stack.getNbt().getInt("CpuDividedBy")).formatted(Formatting.GRAY));
					if(stack.getNbt().getInt("GbRamSlot0") > 0) {
						if((stack.getNbt().getInt("GbRamSlot0") / 1024) < 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot0Mb", stack.getNbt().getInt("GbRamSlot0")).formatted(Formatting.GRAY));
						} else if((stack.getNbt().getInt("GbRamSlot0") / 1024) >= 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot0", (stack.getNbt().getInt("GbRamSlot0") / 1024)).formatted(Formatting.GRAY));
					}}
					if(stack.getNbt().getInt("GbRamSlot1") > 0) {
						if((stack.getNbt().getInt("GbRamSlot1") / 1024) < 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot1Mb", stack.getNbt().getInt("GbRamSlot1")).formatted(Formatting.GRAY));
						} else if((stack.getNbt().getInt("GbRamSlot1") / 1024) >= 1) {
							tooltip.add(Text.translatable("mcvmcomputers.pc_item_ramSlot1", (stack.getNbt().getInt("GbRamSlot1") / 1024)).formatted(Formatting.GRAY));
					}}
					if(!stack.getNbt().getString("HardDriveFileName").isEmpty())
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_hdd", stack.getNbt().getString("HardDriveFileName")).formatted(Formatting.GRAY));
					if(!stack.getNbt().getString("IsoFileName").isEmpty())
						tooltip.add(Text.translatable("mcvmcomputers.pc_item_iso", stack.getNbt().getString("IsoFileName")).formatted(Formatting.GRAY));
				}
			}
		}
	}

	@Override
	public Text getName(ItemStack stack) {
		if(stack.getNbt() != null) {
			if (stack.getNbt().contains("MotherboardInstalled")) {
				if(stack.getNbt().getBoolean("MotherboardInstalled")) {
					return Text.translatable("mcvmcomputers.pc_item_built");
				}
			}
		}
		return Text.translatable("item.mcvmcomputers.pc_case");
	}

	public static ItemStack createPCStackByEntity(EntityPC pc) {
		ItemStack is = new ItemStack(ItemList.PC_CASE);
		if(pc.getMotherboardInstalled()) {
			NbtCompound ct = is.getOrCreateNbt();
			ct.putBoolean("X64", pc.get64Bit());
			ct.putBoolean("MotherboardInstalled", pc.getMotherboardInstalled());
			ct.putBoolean("GpuInstalled", pc.getGpuInstalled());
			ct.putInt("CpuDividedBy", pc.getCpuDividedBy());
			ct.putInt("GbRamSlot0", pc.getGigsOfRamInSlot0());
			ct.putInt("GbRamSlot1", pc.getGigsOfRamInSlot1());
			ct.putString("HardDriveFileName", pc.getHardDriveFileName());
			ct.putString("IsoFileName", pc.getIsoFileName());
		}
		return is;
	}
}

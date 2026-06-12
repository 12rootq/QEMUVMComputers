package mcvmcomputers.item;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;


import java.util.List;

import mcvmcomputers.client.ClientMod;
import mcvmcomputers.entities.EntityPC;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.component.DataComponents;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;


import net.minecraft.world.level.Level;

public class ItemPCCaseSidepanel extends OrderableItem{
	public ItemPCCaseSidepanel(Item.Properties settings) {
		super(settings, 6);
	}

	private static CompoundTag getNbtSafe(ItemStack stack) {
		CustomData comp = stack.get(DataComponents.CUSTOM_DATA);
		return comp != null ? comp.copyTag() : null;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
		if(!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
			CompoundTag tag = getNbtSafe(user.getItemInHand(hand));
			user.getItemInHand(hand).shrink(1);
			HitResult hr = user.pick(10, 0f, false);
			EntityPC ek = new EntityPC(level,
									hr.getLocation().x,
									hr.getLocation().y,
									hr.getLocation().z,
									new Vec3(user.position().x,
												hr.getLocation().y,
												user.position().z), user.getUUID(), true, tag);
			level.addFreshEntity(ek);
		}

		if(level.isClientSide()) {
			ClientMod.playPlaceSound(level, user, SoundEvents.METAL_PLACE, 10);
		}

		return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, user.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
		CompoundTag n = getNbtSafe(stack);
		if(n != null) {
			if (n.contains("MotherboardInstalled")) {
				if(n.getBoolean("MotherboardInstalled")) {
					tooltip.add(Component.translatable(n.getBoolean("X64") ? "item.mcvmcomputers.motherboard64" : "item.mcvmcomputers.motherboard").withStyle(ChatFormatting.GRAY));
					if(n.getBoolean("GpuInstalled"))
						tooltip.add(Component.translatable("mcvmcomputers.pc_item_gpu").withStyle(ChatFormatting.GRAY));
					if(n.getInt("CpuDividedBy") > 0)
						tooltip.add(Component.translatable("mcvmcomputers.pc_item_cpu", n.getInt("CpuDividedBy")).withStyle(ChatFormatting.GRAY));
					if(n.getInt("GbRamSlot0") > 0) {
						if((n.getInt("GbRamSlot0") / 1024) < 1) {
							tooltip.add(Component.translatable("mcvmcomputers.pc_item_ramSlot0Mb", n.getInt("GbRamSlot0")).withStyle(ChatFormatting.GRAY));
						} else if((n.getInt("GbRamSlot0") / 1024) >= 1) {
							tooltip.add(Component.translatable("mcvmcomputers.pc_item_ramSlot0", (n.getInt("GbRamSlot0") / 1024)).withStyle(ChatFormatting.GRAY));
					}}
					if(n.getInt("GbRamSlot1") > 0) {
						if((n.getInt("GbRamSlot1") / 1024) < 1) {
							tooltip.add(Component.translatable("mcvmcomputers.pc_item_ramSlot1Mb", n.getInt("GbRamSlot1")).withStyle(ChatFormatting.GRAY));
						} else if((n.getInt("GbRamSlot1") / 1024) >= 1) {
							tooltip.add(Component.translatable("mcvmcomputers.pc_item_ramSlot1", (n.getInt("GbRamSlot1") / 1024)).withStyle(ChatFormatting.GRAY));
					}}
					if(!n.getString("HardDriveFileName").isEmpty())
						tooltip.add(Component.translatable("mcvmcomputers.pc_item_hdd", n.getString("HardDriveFileName")).withStyle(ChatFormatting.GRAY));
					if(!n.getString("IsoFileName").isEmpty())
						tooltip.add(Component.translatable("mcvmcomputers.pc_item_iso", n.getString("IsoFileName")).withStyle(ChatFormatting.GRAY));
				}
			}
		}
	}

	@Override
	public Component getName(ItemStack stack) {
		CompoundTag n = getNbtSafe(stack);
		if(n != null) {
			if (n.contains("MotherboardInstalled")) {
				if(n.getBoolean("MotherboardInstalled")) {
					return Component.translatable("mcvmcomputers.pc_item_built");
				}
			}
		}
		return Component.translatable("item.mcvmcomputers.pc_case_sidepanel");
	}

	public static ItemStack createPCStackByEntity(EntityPC pc) {
		ItemStack is = new ItemStack(ItemList.PC_CASE_SIDEPANEL); if(pc.getMotherboardInstalled()) {
			CompoundTag ct = new CompoundTag();
			ct.putBoolean("X64", pc.get64Bit());
			ct.putBoolean("MotherboardInstalled", pc.getMotherboardInstalled());
			ct.putBoolean("GpuInstalled", pc.getGpuInstalled());
			ct.putInt("CpuDividedBy", pc.getCpuDividedBy());
			ct.putInt("GbRamSlot0", pc.getGigsOfRamInSlot0());
			ct.putInt("GbRamSlot1", pc.getGigsOfRamInSlot1());
			ct.putString("HardDriveFileName", pc.getHardDriveFileName());
			ct.putString("IsoFileName", pc.getIsoFileName());
			is.set(DataComponents.CUSTOM_DATA, CustomData.of(ct));
		}
		return is;
	}
}

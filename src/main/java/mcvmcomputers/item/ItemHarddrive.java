package mcvmcomputers.item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;


import net.minecraft.world.item.Item;

import mcvmcomputers.MainMod;
import net.minecraft.core.component.DataComponents;


import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.level.Level;

public class ItemHarddrive extends OrderableItem{
	public ItemHarddrive(Item.Properties settings) {
		super(settings, 6);
	}


	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
		if(level.isClientSide()) {
			MainMod.hardDriveClick.run();
		}
		return super.use(level, user, hand);
	}

	@Override
	public Component getName(ItemStack stack) {
		CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
		if (nbtComp != null) {
			CompoundTag nbt = nbtComp.copyTag();
			if (nbt.contains("vhdfile")) {
				return Component.translatable("mcvmcomputers.hdd_item_name", nbt.getString("vhdfile")).withStyle(ChatFormatting.WHITE);
			}
		}
		return Component.translatable("mcvmcomputers.hdd_item_name", Component.translatable("mcvmcomputers.hdd_right_click").getString()).withStyle(ChatFormatting.WHITE);
	}

	public static ItemStack createHardDrive(String fileName) {
		ItemStack is = new ItemStack(ItemList.ITEM_HARDDRIVE);
		CompoundTag ct = new CompoundTag();
		ct.putString("vhdfile", fileName);
		is.set(DataComponents.CUSTOM_DATA, CustomData.of(ct));
		return is;
	}

}

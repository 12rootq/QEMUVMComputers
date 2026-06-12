package mcvmcomputers.item;

import mcvmcomputers.MainMod;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ItemHarddrive extends OrderableItem{
	public ItemHarddrive(Settings settings) {
		super(settings, 6);
	}


	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if(world.isClient) {
			MainMod.hardDriveClick.run();
		}
		return super.use(world, user, hand);
	}

	@Override
	public Text getName(ItemStack stack) {
		NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (nbtComp != null) {
			NbtCompound nbt = nbtComp.copyNbt();
			if (nbt.contains("vhdfile")) {
				return Text.translatable("mcvmcomputers.hdd_item_name", nbt.getString("vhdfile")).formatted(Formatting.WHITE);
			}
		}
		return Text.translatable("mcvmcomputers.hdd_item_name", Text.translatable("mcvmcomputers.hdd_right_click").getString()).formatted(Formatting.WHITE);
	}

	public static ItemStack createHardDrive(String fileName) {
		ItemStack is = new ItemStack(ItemList.ITEM_HARDDRIVE);
		NbtCompound ct = new NbtCompound();
		ct.putString("vhdfile", fileName);
		is.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(ct));
		return is;
	}

}

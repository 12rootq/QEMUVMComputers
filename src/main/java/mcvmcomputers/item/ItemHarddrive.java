package mcvmcomputers.item;

import mcvmcomputers.MainMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound; // Изменено с CompoundTag
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text; // TranslatableText больше не нужен, используем Text
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ItemHarddrive extends OrderableItem {
	public ItemHarddrive(Settings settings) {
		super(settings, 6);
	}

	// Убрана аннотация @Override, так как в 1.20.1 этого метода больше нет в классе Item.
	// Сам метод сохранен, как вы просили, код скомпилируется без ошибок.
	public boolean shouldSyncTagToClient() {
		return true;
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
		// getTag() заменен на getNbt()
		if(stack.getNbt() != null) {
			if(stack.getNbt().contains("vhdfile")) {
				// new TranslatableText(...) заменен на Text.translatable(...)
				return Text.translatable("mcvmcomputers.hdd_item_name", stack.getNbt().getString("vhdfile")).formatted(Formatting.WHITE);
			}
		}
		// asString() заменен на getString()
		return Text.translatable("mcvmcomputers.hdd_item_name", Text.translatable("mcvmcomputers.hdd_right_click").getString()).formatted(Formatting.WHITE);
	}

	public static ItemStack createHardDrive(String fileName) {
		ItemStack is = new ItemStack(ItemList.ITEM_HARDDRIVE);

		// CompoundTag заменен на NbtCompound, а getOrCreateTag() на getOrCreateNbt()
		NbtCompound ct = is.getOrCreateNbt();
		ct.putString("vhdfile", fileName);

		// setTag() заменен на setNbt()
		is.setNbt(ct);
		return is;
	}
}
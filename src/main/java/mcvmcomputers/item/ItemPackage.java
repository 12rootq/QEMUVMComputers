package mcvmcomputers.item;

import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;

public class ItemPackage extends Item{

	public ItemPackage(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if(!world.isClient){
			ItemStack is = user.getStackInHand(hand);
			NbtComponent nbtComp = is.get(DataComponentTypes.CUSTOM_DATA);
			if (nbtComp != null) {
				NbtCompound ct = nbtComp.copyNbt();
				if (ct.contains("packaged_item")) {
					is.decrement(1);
					user.giveItemStack(new ItemStack(Registries.ITEM.get(Identifier.of(ct.getString("packaged_item")))));
				}
			}
		}
		return super.use(world, user, hand);
	}

	@Override
	public Text getName(ItemStack stack) {
		NbtComponent nbtComp = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (nbtComp != null) {
			NbtCompound ct = nbtComp.copyNbt();
			if (ct.contains("packaged_item")) {
				return Text.translatable("mcvmcomputers.packaged").formatted(Formatting.GRAY).append(Text.translatable(Registries.ITEM.get(Identifier.of(ct.getString("packaged_item"))).getTranslationKey()).formatted(Formatting.GREEN));
			}
		}
		return Text.translatable("mcvmcomputers.invalid_package").formatted(Formatting.RED);
	}

	@Override
	public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("mcvmcomputers.open_with_right_click").formatted(Formatting.GRAY));
	}

	public static ItemStack createPackage(Identifier id) {
		ItemStack is = new ItemStack(ItemList.ITEM_PACKAGE);
		NbtCompound ct = new NbtCompound();
		ct.putString("packaged_item", id.toString());
		is.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(ct));
		return is;
	}

}

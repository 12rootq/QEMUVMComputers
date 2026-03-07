package newvmcomputers.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class ItemPackage extends Item {

	public ItemPackage(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		if(!world.isClient){
			ItemStack is = user.getStackInHand(hand);
			if(is.getNbt() != null) {
				if(is.getNbt().contains("packaged_item")) {
					is.decrement(1);

					user.giveItemStack(new ItemStack(Registries.ITEM.get(new Identifier(is.getNbt().getString("packaged_item")))));
				}
			}
		}
		return super.use(world, user, hand);
	}

	@Override
	public Text getName(ItemStack stack) {
		if(stack.getNbt() != null) {
			if(stack.getNbt().contains("packaged_item")) {
				return Text.translatable("newvmcomputers.packaged")
						.formatted(Formatting.GRAY)
						.append(Text.translatable(Registries.ITEM.get(new Identifier(stack.getNbt().getString("packaged_item"))).getTranslationKey())
								.formatted(Formatting.GREEN));
			}
		}

		return Text.translatable("newvmcomputers.invalid_package").formatted(Formatting.RED);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {

		tooltip.add(Text.translatable("newvmcomputers.open_with_right_click").formatted(Formatting.GRAY));
	}

	public static ItemStack createPackage(Identifier id) {
		ItemStack is = new ItemStack(ItemList.ITEM_PACKAGE);
		NbtCompound ct = is.getOrCreateNbt();
		ct.putString("packaged_item", id.toString());

		is.setNbt(ct);
		return is;
	}

}
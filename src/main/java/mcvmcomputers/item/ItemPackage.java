package mcvmcomputers.item;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.player.Player;


import java.util.List;

import net.minecraft.core.component.DataComponents;

import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

public class ItemPackage extends Item{

	public ItemPackage(Item.Properties settings) {
		super(settings);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
		if(!level.isClientSide()){
			ItemStack is = user.getItemInHand(hand);
			CustomData nbtComp = is.get(DataComponents.CUSTOM_DATA);
			if (nbtComp != null) {
				CompoundTag ct = nbtComp.copyTag();
				if (ct.contains("packaged_item")) {
					is.shrink(1);
					String rl = ct.getString("packaged_item");
					int colonIdx = rl.indexOf(':');
					String ns = colonIdx >= 0 ? rl.substring(0, colonIdx) : "minecraft";
					String path = colonIdx >= 0 ? rl.substring(colonIdx + 1) : rl;
					user.getInventory().add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ns, path))));
				}
			}
		}
		return super.use(level, user, hand);
	}

	@Override
	public Component getName(ItemStack stack) {
		CustomData nbtComp = stack.get(DataComponents.CUSTOM_DATA);
		if (nbtComp != null) {
			CompoundTag ct = nbtComp.copyTag();
			if (ct.contains("packaged_item")) {
				String rl = ct.getString("packaged_item");
				int colonIdx = rl.indexOf(':');
				String ns = colonIdx >= 0 ? rl.substring(0, colonIdx) : "minecraft";
				String path = colonIdx >= 0 ? rl.substring(colonIdx + 1) : rl;
				return Component.translatable("mcvmcomputers.packaged").withStyle(ChatFormatting.GRAY).append(Component.translatable(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ns, path)).getDescriptionId()).withStyle(ChatFormatting.GREEN));
			}
		}
		return Component.translatable("mcvmcomputers.invalid_package").withStyle(ChatFormatting.RED);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag type) {
		tooltip.add(Component.translatable("mcvmcomputers.open_with_right_click").withStyle(ChatFormatting.GRAY));
	}

	public static ItemStack createPackage(ResourceLocation id) {
		ItemStack is = new ItemStack(ItemList.ITEM_PACKAGE);
		CompoundTag ct = new CompoundTag();
		ct.putString("packaged_item", id.toString());
		is.set(DataComponents.CUSTOM_DATA, CustomData.of(ct));
		return is;
	}

}

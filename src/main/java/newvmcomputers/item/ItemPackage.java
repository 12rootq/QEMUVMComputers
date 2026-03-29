package newvmcomputers.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemPackage extends Item {
    public ItemPackage(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) {
            ItemStack stack = user.getItemInHand(hand);
            if (stack.getTag() != null && stack.getTag().contains("packaged_item")) {
                stack.shrink(1);
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(stack.getTag().getString("packaged_item")));
                user.addItem(new ItemStack(item));
            }
        }
        return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("packaged_item")) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(stack.getTag().getString("packaged_item")));
            return Component.translatable("newvmcomputers.packaged")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.GREEN));
        }
        return Component.translatable("newvmcomputers.invalid_package").withStyle(ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("newvmcomputers.open_with_right_click").withStyle(ChatFormatting.GRAY));
    }

    public static ItemStack createPackage(ResourceLocation id) {
        ItemStack stack = new ItemStack(ItemList.ITEM_PACKAGE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("packaged_item", id.toString());
        return stack;
    }
}


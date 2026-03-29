package newvmcomputers.item;

import java.util.List;

import newvmcomputers.MainMod;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class ItemHarddrive extends OrderableItem {
    public ItemHarddrive(Properties properties) {
        super(properties, 6);
    }

    @Override
    public boolean shouldOverrideMultiplayerNbt() {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (level.isClientSide) {
            MainMod.hardDriveClick.run();
        }
        return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("vhdfile")) {
            return Component.translatable("newvmcomputers.hdd_item_name", stack.getTag().getString("vhdfile")).withStyle(ChatFormatting.WHITE);
        }
        return Component.translatable("newvmcomputers.hdd_item_name", Component.translatable("newvmcomputers.hdd_right_click").getString())
                .withStyle(ChatFormatting.WHITE);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static ItemStack createHardDrive(String fileName) {
        ItemStack stack = new ItemStack(ItemList.ITEM_HARDDRIVE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("vhdfile", fileName);
        return stack;
    }
}


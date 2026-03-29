package newvmcomputers.item;

import java.util.List;

import newvmcomputers.MainMod;
import newvmcomputers.client.ClientMod;
import newvmcomputers.entities.EntityPC;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemPCCase extends OrderableItem {
    public ItemPCCase(Properties properties) {
        super(properties, 2);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack heldStack = user.getItemInHand(hand);
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            HitResult hitResult = user.pick(10.0D, 0.0F, false);
            CompoundTag tag = heldStack.getTag() == null ? null : heldStack.getTag().copy();
            heldStack.shrink(1);

            EntityPC pc = new EntityPC(level,
                    hitResult.getLocation().x,
                    hitResult.getLocation().y,
                    hitResult.getLocation().z,
                    new Vec3(user.getX(), hitResult.getLocation().y, user.getZ()),
                    user.getUUID(),
                    tag);
            level.addFreshEntity(pc);
            MainMod.computers.put(user.getUUID(), pc);
        }

        if (level.isClientSide && ClientMod.thePreviewEntity != null) {
            level.playLocalSound(ClientMod.thePreviewEntity.getX(),
                    ClientMod.thePreviewEntity.getY(),
                    ClientMod.thePreviewEntity.getZ(),
                    SoundEvents.METAL_PLACE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F,
                    false);
        }

        return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        appendPcTooltip(stack, tooltip);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.getTag() != null && stack.getTag().contains("MoboInstalled") && stack.getTag().getBoolean("MoboInstalled")) {
            return Component.translatable("newvmcomputers.pc_item_built");
        }
        return Component.translatable("item.newvmcomputers.pc_case");
    }

    public static ItemStack createPCStackByEntity(EntityPC pc) {
        ItemStack stack = new ItemStack(ItemList.PC_CASE.get());
        if (pc.getMotherboardInstalled()) {
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean("x64", pc.get64Bit());
            tag.putBoolean("MoboInstalled", pc.getMotherboardInstalled());
            tag.putBoolean("GPUInstalled", pc.getGpuInstalled());
            tag.putInt("CPUDividedBy", pc.getCpuDividedBy());
            tag.putInt("RAMSlot0", pc.getGigsOfRamInSlot0());
            tag.putInt("RAMSlot1", pc.getGigsOfRamInSlot1());
            tag.putString("VHDName", pc.getHardDriveFileName());
            tag.putString("ISOName", pc.getIsoFileName());
        }
        return stack;
    }

    static void appendPcTooltip(ItemStack stack, List<Component> tooltip) {
        if (stack.getTag() == null || !stack.getTag().contains("MoboInstalled") || !stack.getTag().getBoolean("MoboInstalled")) {
            return;
        }

        CompoundTag tag = stack.getTag();
        tooltip.add(Component.translatable(tag.getBoolean("x64") ? "item.newvmcomputers.motherboard64" : "item.newvmcomputers.motherboard")
                .withStyle(ChatFormatting.GRAY));

        if (tag.getBoolean("GPUInstalled")) {
            tooltip.add(Component.translatable("newvmcomputers.pc_item_gpu").withStyle(ChatFormatting.GRAY));
        }
        if (tag.getInt("CPUDividedBy") > 0) {
            tooltip.add(Component.translatable("newvmcomputers.pc_item_cpu", tag.getInt("CPUDividedBy")).withStyle(ChatFormatting.GRAY));
        }
        if (tag.getInt("RAMSlot0") > 0) {
            if ((tag.getInt("RAMSlot0") / 1024) < 1) {
                tooltip.add(Component.translatable("newvmcomputers.pc_item_ramSlot0Mb", tag.getInt("RAMSlot0")).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("newvmcomputers.pc_item_ramSlot0", tag.getInt("RAMSlot0") / 1024).withStyle(ChatFormatting.GRAY));
            }
        }
        if (tag.getInt("RAMSlot1") > 0) {
            if ((tag.getInt("RAMSlot1") / 1024) < 1) {
                tooltip.add(Component.translatable("newvmcomputers.pc_item_ramSlot1Mb", tag.getInt("RAMSlot1")).withStyle(ChatFormatting.GRAY));
            } else {
                tooltip.add(Component.translatable("newvmcomputers.pc_item_ramSlot1", tag.getInt("RAMSlot1") / 1024).withStyle(ChatFormatting.GRAY));
            }
        }
        if (!tag.getString("VHDName").isEmpty()) {
            tooltip.add(Component.translatable("newvmcomputers.pc_item_hdd", tag.getString("VHDName")).withStyle(ChatFormatting.GRAY));
        }
        if (!tag.getString("ISOName").isEmpty()) {
            tooltip.add(Component.translatable("newvmcomputers.pc_item_iso", tag.getString("ISOName")).withStyle(ChatFormatting.GRAY));
        }
    }
}


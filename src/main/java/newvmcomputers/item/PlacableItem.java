package newvmcomputers.item;

import java.lang.reflect.Constructor;

import newvmcomputers.client.ClientMod;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class PlacableItem extends Item {
    private Constructor<? extends Entity> constructor;
    private final SoundEvent placeSound;

    public PlacableItem(Properties properties, Class<? extends Entity> entityPlaced, SoundEvent placeSound) {
        super(properties);
        this.placeSound = placeSound;
        try {
            constructor = entityPlaced.getConstructor(Level.class, Double.class, Double.class, Double.class, Vec3.class, String.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        ItemStack heldStack = user.getItemInHand(hand);
        if (!level.isClientSide && hand == InteractionHand.MAIN_HAND) {
            HitResult hitResult = user.pick(10.0D, 0.0F, false);
            heldStack.shrink(1);
            try {
                Entity entity = constructor.newInstance(level,
                        hitResult.getLocation().x,
                        hitResult.getLocation().y,
                        hitResult.getLocation().z,
                        new Vec3(user.getX(), hitResult.getLocation().y, user.getZ()),
                        user.getStringUUID());
                level.addFreshEntity(entity);
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        if (level.isClientSide && ClientMod.thePreviewEntity != null) {
            level.playLocalSound(ClientMod.thePreviewEntity.getX(),
                    ClientMod.thePreviewEntity.getY(),
                    ClientMod.thePreviewEntity.getZ(),
                    placeSound,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F,
                    false);
        }

        return InteractionResultHolder.sidedSuccess(user.getItemInHand(hand), level.isClientSide);
    }
}


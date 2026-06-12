package mcvmcomputers.item;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;


import java.lang.reflect.Constructor;

import mcvmcomputers.client.ClientMod;
import net.minecraft.world.entity.Entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;


import net.minecraft.world.level.Level;

public class PlacableItem extends Item{
	private Constructor<? extends Entity> constructor;
	private SoundEvent placeSound;

	public PlacableItem(Item.Properties settings, Class<? extends Entity> entityPlaced, SoundEvent placeSound) {
		super(settings);
		this.placeSound = placeSound;
		try {
			constructor = entityPlaced.getConstructor(Level.class, Double.class, Double.class, Double.class, Vec3.class, String.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
		if(!level.isClientSide() && hand == InteractionHand.MAIN_HAND) {
			user.getItemInHand(hand).shrink(1);
			HitResult hr = user.pick(10, 0f, false);
			Entity ek;
			try {
				ek = constructor.newInstance(level,
											hr.getLocation().x,
											hr.getLocation().y,
											hr.getLocation().z,
											new Vec3(user.position().x,
														hr.getLocation().y,
														user.position().z), user.getStringUUID());
				level.addFreshEntity(ek);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if(level.isClientSide()) {
			ClientMod.playPlaceSound(level, user, placeSound, 10);
		}

		return new InteractionResultHolder<ItemStack>(InteractionResult.SUCCESS, user.getItemInHand(hand));
	}

}

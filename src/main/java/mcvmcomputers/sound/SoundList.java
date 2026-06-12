package mcvmcomputers.sound;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Declares and registers the mod's custom sound events (radar ping, rocket and
 * the shop intro/outro/music tracks) using Forge's RegisterEvent.
 */
public class SoundList {
	public static SoundEvent RADAR_SOUND = SoundEvent.of(new Identifier("mcvmcomputers", "radar"));
	public static SoundEvent ROCKET_SOUND = SoundEvent.of(new Identifier("mcvmcomputers", "rocket"));
	public static SoundEvent SHOPINTRO_SOUND = SoundEvent.of(new Identifier("mcvmcomputers", "shopintro"));
	public static SoundEvent SHOPOUTRO_SOUND = SoundEvent.of(new Identifier("mcvmcomputers", "shopoutro"));
	public static SoundEvent SHOPMUSIC_SOUND = SoundEvent.of(new Identifier("mcvmcomputers", "shopmusic"));

	public static void init(RegisterEvent.RegisterHelper<SoundEvent> helper) {
		helper.register(new Identifier("mcvmcomputers", "radar"), RADAR_SOUND);
		helper.register(new Identifier("mcvmcomputers", "rocket"), ROCKET_SOUND);
		helper.register(new Identifier("mcvmcomputers", "shopintro"), SHOPINTRO_SOUND);
		helper.register(new Identifier("mcvmcomputers", "shopoutro"), SHOPOUTRO_SOUND);
		helper.register(new Identifier("mcvmcomputers", "shopmusic"), SHOPMUSIC_SOUND);
	}
}

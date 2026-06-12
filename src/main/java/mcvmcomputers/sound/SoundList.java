package mcvmcomputers.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Declares and registers the mod's custom sound events (radar ping, rocket and
 * the shop intro/outro/music tracks).
 */
public class SoundList {
	public static SoundEvent RADAR_SOUND = SoundEvent.of(Identifier.of("mcvmcomputers", "radar"));
	public static SoundEvent ROCKET_SOUND = SoundEvent.of(Identifier.of("mcvmcomputers", "rocket"));
	public static SoundEvent SHOPINTRO_SOUND = SoundEvent.of(Identifier.of("mcvmcomputers", "shopintro"));
	public static SoundEvent SHOPOUTRO_SOUND = SoundEvent.of(Identifier.of("mcvmcomputers", "shopoutro"));
	public static SoundEvent SHOPMUSIC_SOUND = SoundEvent.of(Identifier.of("mcvmcomputers", "shopmusic"));

	public static void init() {
		Registry.register(Registries.SOUND_EVENT, Identifier.of("mcvmcomputers", "radar"), RADAR_SOUND);
		Registry.register(Registries.SOUND_EVENT, Identifier.of("mcvmcomputers", "rocket"), ROCKET_SOUND);
		Registry.register(Registries.SOUND_EVENT, Identifier.of("mcvmcomputers", "shopintro"), SHOPINTRO_SOUND);
		Registry.register(Registries.SOUND_EVENT, Identifier.of("mcvmcomputers", "shopoutro"), SHOPOUTRO_SOUND);
		Registry.register(Registries.SOUND_EVENT, Identifier.of("mcvmcomputers", "shopmusic"), SHOPMUSIC_SOUND);
	}
}

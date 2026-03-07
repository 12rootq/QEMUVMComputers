package newvmcomputers.sound;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundList {
	public static final SoundEvent RADAR_SOUND = SoundEvent.of(new Identifier("newvmcomputers", "radar"));
	public static final SoundEvent ROCKET_SOUND = SoundEvent.of(new Identifier("newvmcomputers", "rocket"));
	public static final SoundEvent SHOPINTRO_SOUND = SoundEvent.of(new Identifier("newvmcomputers", "shopintro"));
	public static final SoundEvent SHOPOUTRO_SOUND = SoundEvent.of(new Identifier("newvmcomputers", "shopoutro"));
	public static final SoundEvent SHOPMUSIC_SOUND = SoundEvent.of(new Identifier("newvmcomputers", "shopmusic"));

	public static void init() {
		Registry.register(Registries.SOUND_EVENT, new Identifier("newvmcomputers", "radar"), RADAR_SOUND);
		Registry.register(Registries.SOUND_EVENT, new Identifier("newvmcomputers", "rocket"), ROCKET_SOUND);
		Registry.register(Registries.SOUND_EVENT, new Identifier("newvmcomputers", "shopintro"), SHOPINTRO_SOUND);
		Registry.register(Registries.SOUND_EVENT, new Identifier("newvmcomputers", "shopoutro"), SHOPOUTRO_SOUND);
		Registry.register(Registries.SOUND_EVENT, new Identifier("newvmcomputers", "shopmusic"), SHOPMUSIC_SOUND);
	}
}
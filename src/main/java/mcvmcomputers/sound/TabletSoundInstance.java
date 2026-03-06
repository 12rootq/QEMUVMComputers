package mcvmcomputers.sound;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.random.Random; // Импорт Random останется для конструктора

public class TabletSoundInstance extends AbstractSoundInstance {

	public TabletSoundInstance(SoundEvent soundId) {
		super(soundId, SoundCategory.MASTER, Random.create());
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public boolean shouldAlwaysPlay() {
		return true;
	}

	@Override
	public float getVolume() {
		if (this.sound == null) {
			return 0.15F;
		}
		return 0.15F;
	}
}
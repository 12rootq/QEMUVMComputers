package newvmcomputers.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource; 

public class TabletSoundInstance extends AbstractSoundInstance {

	public TabletSoundInstance(SoundEvent soundId) {
		super(soundId, SoundSource.MASTER, RandomSource.create());
		this.looping = true;
		this.delay = 0;
		this.relative = true;
		this.volume = 0.15F;
	}

	@Override
	public float getVolume() {
		return 0.15F;
	}
}


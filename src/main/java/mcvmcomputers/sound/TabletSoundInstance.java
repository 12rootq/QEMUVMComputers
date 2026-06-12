package mcvmcomputers.sound;

import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;

public class TabletSoundInstance extends AbstractSoundInstance {

	public TabletSoundInstance(SoundEvent soundId) {
		super(soundId, SoundSource.MASTER, RandomSource.create());
	}

	@Override
	public boolean isLooping() {
		return true;
	}

	@Override
	public boolean canStartSilent() {
		return true;
	}

	@Override
	public float getVolume() {
		return .15F * this.sound.getVolume().sample(this.random);
	}

}

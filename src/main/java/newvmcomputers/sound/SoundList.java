package newvmcomputers.sound;

import newvmcomputers.MainMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class SoundList {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MainMod.MODID);

    public static final RegistryObject<SoundEvent> RADAR_SOUND = register("radar");
    public static final RegistryObject<SoundEvent> ROCKET_SOUND = register("rocket");
    public static final RegistryObject<SoundEvent> SHOPINTRO_SOUND = register("shopintro");
    public static final RegistryObject<SoundEvent> SHOPOUTRO_SOUND = register("shopoutro");
    public static final RegistryObject<SoundEvent> SHOPMUSIC_SOUND = register("shopmusic");

    private SoundList() {
    }

    public static void init(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(MainMod.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }
}

package mcvmcomputers.sound;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration of the mod's sound events ({@link SoundEvent}).
 *
 * <p>Registers the tablet and delivery sounds: radar, delivery chest rocket,
 * shop intro/outro and shop music. {@link #init(IEventBus)} is called from
 * {@link mcvmcomputers.MainMod}; the ResourceLocations point to entries in
 * {@code sounds.json}.</p>
 */
/**
 * Declares and registers the mod's custom sound events (radar ping, rocket and
 * the shop intro/outro/music tracks).
 */
public class SoundList {
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, "mcvmcomputers");

    public static SoundEvent RADAR_SOUND;
    public static SoundEvent ROCKET_SOUND;
    public static SoundEvent SHOPINTRO_SOUND;
    public static SoundEvent SHOPOUTRO_SOUND;
    public static SoundEvent SHOPMUSIC_SOUND;

    public static void init(IEventBus modEventBus) {
        SOUND_EVENTS.register("radar", () -> RADAR_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "radar")));
        SOUND_EVENTS.register("rocket", () -> ROCKET_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "rocket")));
        SOUND_EVENTS.register("shopintro", () -> SHOPINTRO_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "shopintro")));
        SOUND_EVENTS.register("shopoutro", () -> SHOPOUTRO_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "shopoutro")));
        SOUND_EVENTS.register("shopmusic", () -> SHOPMUSIC_SOUND = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("mcvmcomputers", "shopmusic")));

        SOUND_EVENTS.register(modEventBus);
    }
}

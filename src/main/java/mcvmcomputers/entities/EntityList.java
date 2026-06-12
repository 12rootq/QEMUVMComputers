package mcvmcomputers.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registration of the mod's entity types ({@link EntityType}).
 *
 * <p>Via the NeoForge {@link DeferredRegister} it registers: the placement preview
 * of an item, peripherals (keyboard, mouse, CRT/flat screen, wall TV), the PC itself
 * and the delivery chest. Each type defines its hitbox size, client tracking range
 * and update interval. {@link #init(IEventBus)} is called from
 * {@link mcvmcomputers.MainMod}.</p>
 */
/**
 * Declares and registers every custom entity type (PC case, screens, keyboard,
 * mouse, item preview and the delivery chest) together with their dimensions and
 * tracking ranges.
 */
public class EntityList {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, "mcvmcomputers");

    public static EntityType<EntityItemPreview> ITEM_PREVIEW;
    public static EntityType<EntityKeyboard> KEYBOARD;
    public static EntityType<EntityMouse> MOUSE;
    public static EntityType<EntityCRTScreen> CRT_SCREEN;
    public static EntityType<EntityFlatScreen> FLATSCREEN;
    public static EntityType<EntityWallTV> WALLTV;
    public static EntityType<EntityPC> PC;
    public static EntityType<EntityDeliveryChest> DELIVERY_CHEST;

    public static void init(IEventBus modEventBus) {
        ENTITY_TYPES.register("item_preview", () -> ITEM_PREVIEW = EntityType.Builder.<EntityItemPreview>of(EntityItemPreview::new, MobCategory.MISC)
            .sized(1f, 1f).clientTrackingRange(60).updateInterval(2).build("item_preview"));
        ENTITY_TYPES.register("keyboard", () -> KEYBOARD = EntityType.Builder.<EntityKeyboard>of(EntityKeyboard::new, MobCategory.MISC)
            .sized(0.5f, 0.0625f).clientTrackingRange(60).updateInterval(2).build("keyboard"));
        ENTITY_TYPES.register("mouse", () -> MOUSE = EntityType.Builder.<EntityMouse>of(EntityMouse::new, MobCategory.MISC)
            .sized(0.25f, 0.0625f).clientTrackingRange(60).updateInterval(2).build("mouse"));
        ENTITY_TYPES.register("crt_screen", () -> CRT_SCREEN = EntityType.Builder.<EntityCRTScreen>of(EntityCRTScreen::new, MobCategory.MISC)
            .sized(0.8f, 0.8f).clientTrackingRange(60).updateInterval(2).build("crt_screen"));
        ENTITY_TYPES.register("flat_screen", () -> FLATSCREEN = EntityType.Builder.<EntityFlatScreen>of(EntityFlatScreen::new, MobCategory.MISC)
            .sized(0.8f, 0.8f).clientTrackingRange(60).updateInterval(2).build("flat_screen"));
        ENTITY_TYPES.register("walltv", () -> WALLTV = EntityType.Builder.<EntityWallTV>of(EntityWallTV::new, MobCategory.MISC)
            .sized(1f, 1.2f).clientTrackingRange(60).updateInterval(2).build("walltv"));
        ENTITY_TYPES.register("pc", () -> PC = EntityType.Builder.<EntityPC>of(EntityPC::new, MobCategory.MISC)
            .sized(0.375f, 0.6875f).clientTrackingRange(60).updateInterval(2).build("pc"));
        ENTITY_TYPES.register("delivery_chest", () -> DELIVERY_CHEST = EntityType.Builder.<EntityDeliveryChest>of(EntityDeliveryChest::new, MobCategory.MISC)
            .sized(1f, 2f).clientTrackingRange(600).updateInterval(40).build("delivery_chest"));

        ENTITY_TYPES.register(modEventBus);
    }
}

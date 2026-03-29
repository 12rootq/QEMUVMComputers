package newvmcomputers.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityList {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, "newvmcomputers");

    public static final RegistryObject<EntityType<EntityItemPreview>> ITEM_PREVIEW = ENTITY_TYPES.register("item_preview",
            () -> EntityType.Builder.<EntityItemPreview>of(EntityItemPreview::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f)
                    .clientTrackingRange(60)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("item_preview"));

    public static final RegistryObject<EntityType<EntityKeyboard>> KEYBOARD = ENTITY_TYPES.register("keyboard",
            () -> EntityType.Builder.<EntityKeyboard>of(EntityKeyboard::new, MobCategory.MISC)
                    .sized(0.5f, 0.0625f)
                    .clientTrackingRange(60)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("keyboard"));

    public static final RegistryObject<EntityType<EntityMouse>> MOUSE = ENTITY_TYPES.register("mouse",
            () -> EntityType.Builder.<EntityMouse>of(EntityMouse::new, MobCategory.MISC)
                    .sized(0.25f, 0.0625f)
                    .clientTrackingRange(60)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("mouse"));

    public static final RegistryObject<EntityType<EntityCRTScreen>> CRT_SCREEN = ENTITY_TYPES.register("crt_screen",
            () -> EntityType.Builder.<EntityCRTScreen>of(EntityCRTScreen::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .clientTrackingRange(60)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("crt_screen"));

    public static final RegistryObject<EntityType<EntityFlatScreen>> FLATSCREEN = ENTITY_TYPES.register("flat_screen",
            () -> EntityType.Builder.<EntityFlatScreen>of(EntityFlatScreen::new, MobCategory.MISC)
                    .sized(0.8f, 0.8f)
                    .clientTrackingRange(60)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("flat_screen"));

    public static final RegistryObject<EntityType<EntityWallTV>> WALLTV = ENTITY_TYPES.register("walltv",
            () -> EntityType.Builder.<EntityWallTV>of(EntityWallTV::new, MobCategory.MISC)
                    .sized(1f, 1.2f)
                    .clientTrackingRange(60)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("walltv"));

    public static final RegistryObject<EntityType<EntityPC>> PC = ENTITY_TYPES.register("pc",
            () -> EntityType.Builder.<EntityPC>of(EntityPC::new, MobCategory.MISC)
                    .sized(0.375f, 0.6875f)
                    .clientTrackingRange(60)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("pc"));

    public static final RegistryObject<EntityType<EntityDeliveryChest>> DELIVERY_CHEST = ENTITY_TYPES.register("delivery_chest",
            () -> EntityType.Builder.<EntityDeliveryChest>of(EntityDeliveryChest::new, MobCategory.MISC)
                    .sized(1f, 2f)
                    .clientTrackingRange(600)
                    .updateInterval(40)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("delivery_chest"));

    public static void init(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
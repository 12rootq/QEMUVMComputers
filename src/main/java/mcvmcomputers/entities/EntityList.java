package mcvmcomputers.entities;

import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Identifier;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Declares and registers every custom entity type (PC case, screens, keyboard,
 * mouse, item preview, mouse pad and the delivery chest) with their dimensions
 * and tracking ranges, using Forge's RegisterEvent.
 */
public class EntityList {
    public static EntityType<EntityItemPreview> ITEM_PREVIEW;
    public static EntityType<EntityKeyboard>    KEYBOARD;
    public static EntityType<EntityMouse>       MOUSE;
    public static EntityType<EntityCRTScreen>   CRT_SCREEN;
    public static EntityType<EntityFlatScreen>  FLATSCREEN;
    public static EntityType<EntityWallTV>      WALLTV;
    public static EntityType<EntityPC>          PC;
    public static EntityType<EntityDeliveryChest> DELIVERY_CHEST;
    public static EntityType<EntityMousePad>    MOUSE_PAD;

    private static RegisterEvent.RegisterHelper<EntityType<?>> HELPER;

    public static void init(RegisterEvent.RegisterHelper<EntityType<?>> helper) {
        HELPER = helper;

        ITEM_PREVIEW = reg("item_preview",
            EntityType.Builder.<EntityItemPreview>create(EntityItemPreview::new, SpawnGroup.MISC)
                .setDimensions(1f, 1f)
                .maxTrackingRange(60).trackingTickInterval(2));

        KEYBOARD = reg("keyboard",
            EntityType.Builder.<EntityKeyboard>create(EntityKeyboard::new, SpawnGroup.MISC)
                .setDimensions(0.5f, 0.0625f)
                .maxTrackingRange(60).trackingTickInterval(2));

        MOUSE = reg("mouse",
            EntityType.Builder.<EntityMouse>create(EntityMouse::new, SpawnGroup.MISC)
                .setDimensions(0.25f, 0.0625f)
                .maxTrackingRange(60).trackingTickInterval(2));

        CRT_SCREEN = reg("crt_screen",
            EntityType.Builder.<EntityCRTScreen>create(EntityCRTScreen::new, SpawnGroup.MISC)
                .setDimensions(0.8f, 0.8f)
                .maxTrackingRange(60).trackingTickInterval(2));

        FLATSCREEN = reg("flat_screen",
            EntityType.Builder.<EntityFlatScreen>create(EntityFlatScreen::new, SpawnGroup.MISC)
                .setDimensions(0.8f, 0.8f)
                .maxTrackingRange(60).trackingTickInterval(2));

        WALLTV = reg("walltv",
            EntityType.Builder.<EntityWallTV>create(EntityWallTV::new, SpawnGroup.MISC)
                .setDimensions(1f, 1.2f)
                .maxTrackingRange(60).trackingTickInterval(2));

        PC = reg("pc",
            EntityType.Builder.<EntityPC>create(EntityPC::new, SpawnGroup.MISC)
                .setDimensions(0.375f, 0.6875f)
                .maxTrackingRange(60).trackingTickInterval(2));

        DELIVERY_CHEST = reg("delivery_chest",
            EntityType.Builder.<EntityDeliveryChest>create(EntityDeliveryChest::new, SpawnGroup.MISC)
                .setDimensions(1f, 2f)
                .maxTrackingRange(600).trackingTickInterval(40));

        MOUSE_PAD = reg("mouse_pad",
            EntityType.Builder.<EntityMousePad>create(EntityMousePad::new, SpawnGroup.MISC)
                .setDimensions(1f, 0.0625f)
                .maxTrackingRange(60).trackingTickInterval(2));
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> reg(String id, EntityType.Builder<T> builder) {
        EntityType<T> type = builder.build(id);
        HELPER.register(new Identifier("mcvmcomputers", id), type);
        return type;
    }
}

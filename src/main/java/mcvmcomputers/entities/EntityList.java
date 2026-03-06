package mcvmcomputers.entities;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class EntityList {
	public static EntityType<EntityItemPreview> ITEM_PREVIEW;
	public static EntityType<EntityKeyboard> KEYBOARD;
	public static EntityType<EntityMouse> MOUSE;
	public static EntityType<EntityCRTScreen> CRT_SCREEN;
	public static EntityType<EntityFlatScreen> FLATSCREEN;
	public static EntityType<EntityWallTV> WALLTV;
	public static EntityType<EntityPC> PC;
	public static EntityType<EntityDeliveryChest> DELIVERY_CHEST;

	@SuppressWarnings("unchecked") // Подавляем предупреждение о приведении типов
	public static void init() {
		ITEM_PREVIEW = (EntityType<EntityItemPreview>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "item_preview"),
				FabricEntityTypeBuilder.<EntityItemPreview>create(SpawnGroup.MISC, EntityItemPreview::new)
						.dimensions(EntityDimensions.fixed(1f, 1f))
						.trackRangeBlocks(60).trackedUpdateRate(2).forceTrackedVelocityUpdates(true)
						.build());

		KEYBOARD = (EntityType<EntityKeyboard>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "keyboard"),
				FabricEntityTypeBuilder.<EntityKeyboard>create(SpawnGroup.MISC, EntityKeyboard::new)
						.dimensions(EntityDimensions.fixed(0.5f, 0.0625f))
						.trackRangeBlocks(60).trackedUpdateRate(2).forceTrackedVelocityUpdates(true)
						.build());

		MOUSE = (EntityType<EntityMouse>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "mouse"),
				FabricEntityTypeBuilder.<EntityMouse>create(SpawnGroup.MISC, EntityMouse::new)
						.dimensions(EntityDimensions.fixed(0.25f, 0.0625f))
						.trackRangeBlocks(60).trackedUpdateRate(2).forceTrackedVelocityUpdates(true)
						.build());

		CRT_SCREEN = (EntityType<EntityCRTScreen>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "crt_screen"),
				FabricEntityTypeBuilder.<EntityCRTScreen>create(SpawnGroup.MISC, EntityCRTScreen::new)
						.dimensions(EntityDimensions.fixed(0.8f, 0.8f))
						.trackRangeBlocks(60).trackedUpdateRate(2).forceTrackedVelocityUpdates(true)
						.build());

		FLATSCREEN = (EntityType<EntityFlatScreen>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "flat_screen"),
				FabricEntityTypeBuilder.<EntityFlatScreen>create(SpawnGroup.MISC, EntityFlatScreen::new)
						.dimensions(EntityDimensions.fixed(0.8f, 0.8f))
						.trackRangeBlocks(60).trackedUpdateRate(2).forceTrackedVelocityUpdates(true)
						.build());

		WALLTV = (EntityType<EntityWallTV>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "walltv"),
				FabricEntityTypeBuilder.<EntityWallTV>create(SpawnGroup.MISC, EntityWallTV::new)
						.dimensions(EntityDimensions.fixed(1f, 1.2f))
						.trackRangeBlocks(60).trackedUpdateRate(2).forceTrackedVelocityUpdates(true)
						.build());

		PC = (EntityType<EntityPC>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "pc"),
				FabricEntityTypeBuilder.<EntityPC>create(SpawnGroup.MISC, EntityPC::new)
						.dimensions(EntityDimensions.fixed(0.375f, 0.6875f))
						.trackRangeBlocks(60).trackedUpdateRate(2).forceTrackedVelocityUpdates(true)
						.build());

		DELIVERY_CHEST = (EntityType<EntityDeliveryChest>) (Object) Registry.register(Registries.ENTITY_TYPE,
				new Identifier("mcvmcomputers", "delivery_chest"),
				FabricEntityTypeBuilder.<EntityDeliveryChest>create(SpawnGroup.MISC, EntityDeliveryChest::new)
						.dimensions(EntityDimensions.fixed(1f, 2f))
						.trackRangeBlocks(600).trackedUpdateRate(40).forceTrackedVelocityUpdates(true)
						.build());
	}
}
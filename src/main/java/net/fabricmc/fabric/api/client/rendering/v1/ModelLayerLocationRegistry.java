package net.fabricmc.fabric.api.client.rendering.v1;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import newvmcomputers.MainMod;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ModelLayerLocationRegistry {
    private static final Map<ModelLayerLocation, Supplier<LayerDefinition>> DEFINITIONS = new LinkedHashMap<>();

    private ModelLayerLocationRegistry() {
    }

    public static void registerModelLayer(ModelLayerLocation layer, Supplier<LayerDefinition> definition) {
        DEFINITIONS.put(layer, definition);
    }

    @Mod.EventBusSubscriber(modid = MainMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ForgeHooks {
        private ForgeHooks() {
        }

        @SubscribeEvent
        public static void onRegisterLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
            for (Map.Entry<ModelLayerLocation, Supplier<LayerDefinition>> entry : DEFINITIONS.entrySet()) {
                event.registerLayerDefinition(entry.getKey(), entry.getValue());
            }
        }
    }
}

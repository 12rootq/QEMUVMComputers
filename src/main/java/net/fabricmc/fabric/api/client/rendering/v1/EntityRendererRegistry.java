package net.fabricmc.fabric.api.client.rendering.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import newvmcomputers.MainMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class EntityRendererRegistry {
    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();

    private EntityRendererRegistry() {
    }

    public static <T extends Entity> void register(Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider) {
        REGISTRATIONS.add(new Registration<>(type, provider));
    }

    @Mod.EventBusSubscriber(modid = MainMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ForgeHooks {
        private ForgeHooks() {
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            for (Registration<?> registration : REGISTRATIONS) {
                registration.register(event);
            }
        }
    }

    private record Registration<T extends Entity>(Supplier<? extends EntityType<? extends T>> type, EntityRendererProvider<T> provider) {
        private void register(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(type.get(), provider);
        }
    }
}

package net.fabricmc.fabric.api.client.networking.v1;

import java.util.ArrayList;
import java.util.List;

import newvmcomputers.MainMod;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class ClientPlayConnectionEvents {
    public static final DisconnectEvent DISCONNECT = new DisconnectEvent();

    private ClientPlayConnectionEvents() {
    }

    public static final class DisconnectEvent {
        private final List<DisconnectHandler> handlers = new ArrayList<>();

        public void register(DisconnectHandler handler) {
            handlers.add(handler);
        }
    }

    @FunctionalInterface
    public interface DisconnectHandler {
        void onDisconnect(Object handler, Minecraft client);
    }

    @Mod.EventBusSubscriber(modid = MainMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ForgeHooks {
        private ForgeHooks() {
        }

        @SubscribeEvent
        public static void onDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
            Minecraft client = Minecraft.getInstance();
            for (DisconnectHandler handler : DISCONNECT.handlers) {
                handler.onDisconnect(null, client);
            }
        }
    }
}

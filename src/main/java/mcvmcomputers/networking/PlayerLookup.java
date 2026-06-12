package mcvmcomputers.networking;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Drop-in replacement for net.fabricmc.fabric.api.networking.v1.PlayerLookup.
 *
 * Yarn mappings are kept (Architectury Loom), so all Minecraft references use the
 * Yarn names (getWorld, squaredDistanceTo, ...).
 */
public final class PlayerLookup {
    private PlayerLookup() {}

    // Margin (in blocks) used around the entity. 80 covers the 60-block tracking
    // range used by the screen entities plus headroom; the delivery chest uses a
    // larger range so we widen the search for it.
    private static final double DEFAULT_RANGE = 80.0;

    /**
     * Returns all server players currently able to see (track) the given entity.
     */
    public static Collection<ServerPlayerEntity> tracking(Entity entity) {
        if (entity == null || entity.getWorld() == null || entity.getWorld().isClient) {
            return Collections.emptyList();
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return Collections.emptyList();
        }
        double range = DEFAULT_RANGE;
        // Delivery chest is tracked from much farther away in the original mod.
        if (entity.getType() == mcvmcomputers.entities.EntityList.DELIVERY_CHEST) {
            range = 600.0;
        }
        double rangeSq = range * range;

        List<ServerPlayerEntity> result = new ArrayList<>();
        if (entity.getWorld() instanceof ServerWorld sw) {
            for (ServerPlayerEntity p : sw.getPlayers()) {
                if (p.squaredDistanceTo(entity) <= rangeSq) {
                    result.add(p);
                }
            }
        }
        return result;
    }
}

package ist.alchm.smthsHub.utility;

import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import ist.alchm.smthsHub.SmthsHubPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

public final class TeleportUtil {

    private static final PlatformScheduler scheduler = SmthsHubPlugin.scheduler();

    // Present on Paper/Folia; null on Spigot.
    private static final Method TELEPORT_ASYNC;
    private static final Method TELEPORT_ASYNC_WITH_CAUSE;

    static {
        Method m1 = null;
        Method m2 = null;
        try {
            m1 = Player.class.getMethod("teleportAsync", Location.class);
        } catch (NoSuchMethodException ex) {
            // Method not available on this server
        }
        try {
            m2 = Player.class.getMethod("teleportAsync", Location.class, TeleportCause.class);
        } catch (NoSuchMethodException ex) {
            // Method not available on this server
        }

        TELEPORT_ASYNC = m1;
        TELEPORT_ASYNC_WITH_CAUSE = m2;
    }

    private TeleportUtil() {
        // Utility class
    }

    /**
     * Cross-platform teleportation:
     * - Paper/Folia: invoke player.teleportAsync(Location)
     * - Spigot: fallback to player.teleport(Location)
     * Always scheduled on the player's thread via the wrapper.
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> teleportCompat(final Player player, final Location location) {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        if (player == null || location == null) {
            result.complete(false);
            return result;
        }

        scheduler.runAtEntity(player, task -> {
            try {
                if (TELEPORT_ASYNC != null) {
                    final CompletableFuture<Boolean> cf =
                            (CompletableFuture<Boolean>) TELEPORT_ASYNC.invoke(player, location);
                    cf.whenComplete((ok, ex) -> result.complete(ex == null && Boolean.TRUE.equals(ok)));
                } else {
                    result.complete(player.teleport(location));
                }
            } catch (Throwable t) {
                result.complete(player.teleport(location));
            }
        });

        return result;
    }

    /**
     * Cross-platform teleport with cause support:
     * - Paper/Folia: invoke player.teleportAsync(Location, TeleportCause) when available,
     *                otherwise player.teleportAsync(Location)
     * - Spigot: fallback to player.teleport(Location, TeleportCause)
     * Always scheduled on the player's thread via the wrapper.
     */
    @SuppressWarnings("unchecked")
    public static CompletableFuture<Boolean> teleportCompat(final Player player, final Location location, final TeleportCause cause) {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        if (player == null || location == null) {
            result.complete(false);
            return result;
        }

        scheduler.runAtEntity(player, task -> {
            try {
                if (TELEPORT_ASYNC_WITH_CAUSE != null) {
                    final CompletableFuture<Boolean> cf =
                            (CompletableFuture<Boolean>) TELEPORT_ASYNC_WITH_CAUSE.invoke(player, location, cause);
                    cf.whenComplete((ok, ex) -> result.complete(ex == null && Boolean.TRUE.equals(ok)));
                } else if (TELEPORT_ASYNC != null) {
                    final CompletableFuture<Boolean> cf =
                            (CompletableFuture<Boolean>) TELEPORT_ASYNC.invoke(player, location);
                    cf.whenComplete((ok, ex) -> result.complete(ex == null && Boolean.TRUE.equals(ok)));
                } else {
                    result.complete(player.teleport(location, cause));
                }
            } catch (Throwable t) {
                result.complete(player.teleport(location, cause));
            }
        });

        return result;
    }

    /**
     * Delayed cross-platform teleport scheduled at the player's entity.
     */
    public static WrappedTask teleportCompatLaterAtEntity(final Player player, final Location location,
                                                           final long delayTicks, final Runnable after) {
        return scheduler.runAtEntityLater(player, () ->
                teleportCompat(player, location).whenComplete((ignored, __) -> {
                    if (after != null) {
                        after.run();
                    }
                }), delayTicks);
    }

    /**
     * Cross-platform entity teleportation (for non-player entities like ArmorStands).
     * Always scheduled on the entity's thread via the wrapper.
     */
    public static CompletableFuture<Boolean> teleportCompat(final Entity entity, final Location location) {
        final CompletableFuture<Boolean> result = new CompletableFuture<>();

        if (entity == null || location == null) {
            result.complete(false);
            return result;
        }

        scheduler.runAtEntity(entity, task -> result.complete(entity.teleport(location)));

        return result;
    }
}

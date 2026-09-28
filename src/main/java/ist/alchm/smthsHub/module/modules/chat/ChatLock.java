package ist.alchm.smthsHub.module.modules.chat;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatLock extends Module {

    private boolean isChatLocked;

    public ChatLock(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.CHAT_LOCK);

        try {
            Class.forName("io.papermc.paper.event.player.AsyncChatEvent", false, plugin.getClass().getClassLoader());
            plugin.getServer().getPluginManager().registerEvents(new PaperHandler(plugin, this), plugin);
        } catch (ClassNotFoundException ignored) {
            plugin.getServer().getPluginManager().registerEvents(new SpigotHandler(plugin, this), plugin);
        }
    }

    @Override
    public void onEnable() {
        isChatLocked = getPlugin().getConfigManager().getFile(ConfigType.DATA).getConfig().getBoolean("chat_locked");
    }

    @Override
    public void onDisable() {
        getPlugin().getConfigManager().getFile(ConfigType.DATA).getConfig().set("chat_locked", isChatLocked);
    }

    private void handleChatLock(Player player, Runnable cancelAction) {
        if (!isChatLocked || player.hasPermission(Permissions.LOCK_CHAT_BYPASS.getPermission())) {
            return;
        }

        cancelAction.run();
        Messages.CHAT_LOCKED.send(player);
    }

    public boolean isChatLocked() {
        return isChatLocked;
    }

    public void setChatLocked(boolean chatLocked) {
        isChatLocked = chatLocked;
    }

    /**
     * Paper handler using AsyncChatEvent + Adventure.
     * Only registered when Paper's event exists.
     */
    private record PaperHandler(SmthsHubPlugin plugin, ChatLock chatLock) implements Listener {

        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerChat(AsyncChatEvent event) {
            final Player player = event.getPlayer();
            chatLock.handleChatLock(player, () -> event.setCancelled(true));
        }
    }

    /**
     * Spigot handler using AsyncPlayerChatEvent (deprecated on Paper).
     * Registered only when Paper's event is absent.
     */
    private record SpigotHandler(SmthsHubPlugin plugin, ChatLock chatLock) implements Listener {

        @SuppressWarnings("deprecation")
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onPlayerChat(AsyncPlayerChatEvent event) {
            final Player player = event.getPlayer();
            chatLock.handleChatLock(player, () -> event.setCancelled(true));
        }
    }
}

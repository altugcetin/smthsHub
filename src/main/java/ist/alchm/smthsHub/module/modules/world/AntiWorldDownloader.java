package ist.alchm.smthsHub.module.modules.world;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.reflection.XReflection;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.Permissions;
import ist.alchm.smthsHub.config.ConfigType;
import ist.alchm.smthsHub.config.Messages;
import ist.alchm.smthsHub.module.Module;
import ist.alchm.smthsHub.module.ModuleType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class AntiWorldDownloader extends Module implements PluginMessageListener {

    public AntiWorldDownloader(SmthsHubPlugin plugin) {
        super(plugin, ModuleType.ANTI_WDL);
    }

    @Override
    public void onEnable() {
            getPlugin().getServer().getMessenger().registerIncomingPluginChannel(getPlugin(), "wdl:init", this);
            getPlugin().getServer().getMessenger().registerOutgoingPluginChannel(getPlugin(), "wdl:control");
    }

    @Override
    public void onDisable() {
            getPlugin().getServer().getMessenger().unregisterIncomingPluginChannel(getPlugin(), "wdl:init");
            getPlugin().getServer().getMessenger().unregisterOutgoingPluginChannel(getPlugin(), "wdl:control");
    }

    public void onPluginMessageReceived(@NotNull String channel, Player player, byte[] data) {
        if (player.hasPermission(Permissions.ANTI_WDL_BYPASS.getPermission())) {
            return;
        }

        if (channel.equals("wdl:init")) {

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeInt(0);
            out.writeBoolean(false);
            player.sendPluginMessage(getPlugin(), "wdl:control", out.toByteArray());

            if (!getPlugin().getConfigManager().getFile(ConfigType.SETTINGS).getConfig().getBoolean("anti_wdl.admin_notify")) {
                return;
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(Permissions.ANTI_WDL_NOTIFY.getPermission())) {
                    Messages.WORLD_DOWNLOAD_NOTIFY.send(p, "%player%", player.getName());
                }
            }
        }
    }
}

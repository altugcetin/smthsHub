package ist.alchm.smthsHub.inventory;

import ist.alchm.smthsHub.SmthsHubPlugin;
import ist.alchm.smthsHub.debug.BedrockDebug;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.List;

final class FloodgateForms {

    private FloodgateForms() {
    }

    static boolean open(SmthsHubPlugin plugin, Player player, String title, List<InventoryItem> buttons) {
        FloodgateApi api;
        try {
            api = FloodgateApi.getInstance();
        } catch (Throwable ex) {
            return false;
        }
        if (api == null || !api.isFloodgatePlayer(player.getUniqueId())) {
            return false;
        }

        String formTitle = title.isEmpty() ? "Menu" : title;
        SimpleForm.Builder builder = SimpleForm.builder().title(formTitle).content("");
        for (int i = 0; i < buttons.size(); i++) {
            builder.button(BedrockMenus.buttonLabel(buttons.get(i).getItemStack(), i));
        }
        builder.validResultHandler(response -> {
            int id = response.clickedButtonId();
            if (id < 0 || id >= buttons.size() || !player.isOnline()) {
                return;
            }
            InventoryItem item = buttons.get(id);
            Runnable run = () -> {
                if (!BedrockDebug.active()) {
                    for (ClickAction action : item.getClickActions()) {
                        action.execute(player);
                    }
                    return;
                }
                BedrockDebug.messenger(player);
                BedrockDebug.decision(player, "ACT", null, null);
                try {
                    for (ClickAction action : item.getClickActions()) {
                        action.execute(player);
                    }
                    BedrockDebug.actionRan(player);
                } catch (Throwable ex) {
                    BedrockDebug.actionFailed(player, ex);
                    if (ex instanceof RuntimeException runtime) {
                        throw runtime;
                    }
                    throw new RuntimeException(ex);
                }
            };
            if (SmthsHubPlugin.scheduler().isOwnedByCurrentRegion(player)) {
                run.run();
            } else {
                SmthsHubPlugin.scheduler().runAtEntity(player, task -> run.run());
            }
        });
        return api.sendForm(player.getUniqueId(), builder.build());
    }
}

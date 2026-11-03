package dev.lap.cCWeapons.api;

import dev.lap.cCWeapons.CCWeapons;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class AddonLifecycleListener implements Listener {

    private final CCWeapons plugin;
    private final CCWeaponsApi api;

    public AddonLifecycleListener(CCWeapons plugin, CCWeaponsApi api) {
        this.plugin = plugin;
        this.api = api;
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin().equals(plugin)
                || !(event.getPlugin() instanceof JavaPlugin addon)) {
            return;
        }

        api.unregisterWeapons(addon);
    }
}
package dev.lap.standartaddon;

import dev.lap.cCWeapons.api.CCWeaponsApi;
import dev.lap.standartaddon.weapon.impl.EmberAxeWeapon;
import dev.lap.standartaddon.weapon.impl.PulseBladeWeapon;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class StandartAddon extends JavaPlugin {

    private CCWeaponsApi ccWeaponsApi;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<CCWeaponsApi> registration =
                getServer().getServicesManager().getRegistration(
                        CCWeaponsApi.class
                );

        if (registration == null) {
            getLogger().severe(
                    "CCWeapons API is unavailable. Disabling the addon."
            );
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        ccWeaponsApi = registration.getProvider();
        ccWeaponsApi.registerWeapon(this, new PulseBladeWeapon(this));
        ccWeaponsApi.registerWeapon(this, new EmberAxeWeapon());

        getLogger().info("Registered 2 example weapons in CCWeapons.");
    }

    @Override
    public void onDisable() {
        if (ccWeaponsApi != null) {
            ccWeaponsApi.unregisterWeapons(this);
        }
    }
}
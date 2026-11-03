package dev.lap.cCWeapons.api;

import dev.lap.cCWeapons.recipe.WeaponRecipeManager;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import dev.lap.cCWeapons.weapon.WeaponRegistry;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Public API for plugins that add weapons to CCWeapons.
 *
 * <p>Obtain this service through Bukkit's ServicesManager and register each
 * weapon during the addon's {@code onEnable}. Recipe and enabled settings are
 * read from the registering addon's own {@code config.yml} under
 * {@code weapons.<weapon id>}.</p>
 */
public final class CCWeaponsApi {

    private final WeaponRegistry weaponRegistry;
    private final WeaponRecipeManager recipeManager;

    public CCWeaponsApi(
            WeaponRegistry weaponRegistry,
            WeaponRecipeManager recipeManager
    ) {
        this.weaponRegistry = Objects.requireNonNull(weaponRegistry);
        this.recipeManager = Objects.requireNonNull(recipeManager);
    }

    /**
     * Registers a weapon and immediately loads its recipe from the addon's
     * configuration.
     */
    public void registerWeapon(JavaPlugin addon, CustomWeapon weapon) {
        Objects.requireNonNull(addon, "addon");
        Objects.requireNonNull(weapon, "weapon");

        if (!addon.isEnabled()) {
            throw new IllegalStateException(
                    "The addon must be enabled before registering weapons."
            );
        }

        weaponRegistry.register(addon, weapon);
        recipeManager.loadWeaponRecipe(weapon);
    }

    /**
     * Removes every weapon owned by the supplied addon, including recipes.
     */
    public void unregisterWeapons(JavaPlugin addon) {
        Objects.requireNonNull(addon, "addon");

        Collection<CustomWeapon> removed =
                weaponRegistry.unregisterOwnedBy(addon);
        removed.forEach(weapon ->
                recipeManager.removeRecipe(weapon.getId())
        );
    }

    /**
     * Reloads the addon's config and all recipes owned by it.
     */
    public void reloadAddon(JavaPlugin addon) {
        Objects.requireNonNull(addon, "addon");
        addon.reloadConfig();

        weaponRegistry.getWeapons().stream()
                .filter(weapon -> weaponRegistry.getOwner(weapon)
                        .filter(addon::equals)
                        .isPresent())
                .forEach(recipeManager::loadWeaponRecipe);
    }

    public Optional<CustomWeapon> getWeapon(String id) {
        return weaponRegistry.getWeapon(id);
    }

    public Collection<CustomWeapon> getWeapons() {
        return weaponRegistry.getWeapons();
    }
}
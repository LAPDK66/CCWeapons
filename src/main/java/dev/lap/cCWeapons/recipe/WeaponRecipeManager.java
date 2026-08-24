package dev.lap.cCWeapons.recipe;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import dev.lap.cCWeapons.weapon.WeaponRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WeaponRecipeManager {

    private final CCWeapons plugin;
    private final WeaponRegistry weaponRegistry;
    private final List<NamespacedKey> registeredRecipeKeys =
            new ArrayList<>();

    public WeaponRecipeManager(
            CCWeapons plugin,
            WeaponRegistry weaponRegistry
    ) {
        this.plugin = plugin;
        this.weaponRegistry = weaponRegistry;
    }

    public void reloadRecipes() {
        removeRecipes();

        for (CustomWeapon weapon : weaponRegistry.getWeapons()) {
            if (!weaponRegistry.isEnabled(weapon)) {
                continue;
            }

            if (!plugin.getConfig().getBoolean(
                    "weapons." + weapon.getId() + ".recipe.enabled",
                    true
            )) {
                continue;
            }

            registerRecipe(weapon);
        }
    }

    private void registerRecipe(CustomWeapon weapon) {
        String path = "weapons." + weapon.getId() + ".recipe";
        ConfigurationSection recipeSection =
                plugin.getConfig().getConfigurationSection(path);

        if (recipeSection == null) {
            plugin.getLogger().warning(
                    "Missing recipe configuration for weapon: "
                            + weapon.getId()
            );
            return;
        }

        List<String> shape = recipeSection.getStringList("shape");
        if (shape.isEmpty() || shape.size() > 3) {
            plugin.getLogger().warning(
                    "The recipe shape for '" + weapon.getId()
                            + "' must contain between one and three rows."
            );
            return;
        }

        ItemStack result = weapon.createItem(plugin);
        int amount = Math.max(
                1,
                Math.min(
                        result.getMaxStackSize(),
                        recipeSection.getInt("amount", 1)
                )
        );
        result.setAmount(amount);

        NamespacedKey recipeKey = new NamespacedKey(
                plugin,
                "weapon_" + weapon.getId().toLowerCase(Locale.ROOT)
        );

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(shape.toArray(String[]::new));

        ConfigurationSection ingredients =
                recipeSection.getConfigurationSection("ingredients");

        if (ingredients == null) {
            plugin.getLogger().warning(
                    "Missing recipe ingredients for weapon: "
                            + weapon.getId()
            );
            return;
        }

        for (String key : ingredients.getKeys(false)) {
            if (key.length() != 1) {
                plugin.getLogger().warning(
                        "Ingredient keys must contain exactly one character: "
                                + key
                );
                continue;
            }

            String materialName = ingredients.getString(key);
            Material material = Material.matchMaterial(
                    materialName == null ? "" : materialName
            );

            if (material == null) {
                plugin.getLogger().warning(
                        "Unknown material '" + materialName
                                + "' in recipe for " + weapon.getId()
                );
                return;
            }

            recipe.setIngredient(key.charAt(0), material);
        }

        if (Bukkit.addRecipe(recipe)) {
            registeredRecipeKeys.add(recipeKey);
        } else {
            plugin.getLogger().warning(
                    "Could not register recipe for weapon: "
                            + weapon.getId()
            );
        }
    }

    public void removeRecipes() {
        for (NamespacedKey key : registeredRecipeKeys) {
            Bukkit.removeRecipe(key);
        }

        registeredRecipeKeys.clear();
    }
}
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
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class WeaponRecipeManager {

    private final CCWeapons plugin;
    private final WeaponRegistry weaponRegistry;
    private final Map<String, NamespacedKey> registeredRecipeKeys =
            new LinkedHashMap<>();
    private final Map<String, WeaponRecipeDefinition> recipeDefinitions =
            new LinkedHashMap<>();

    public WeaponRecipeManager(
            CCWeapons plugin,
            WeaponRegistry weaponRegistry
    ) {
        this.plugin = plugin;
        this.weaponRegistry = weaponRegistry;
    }

    public void reloadRecipes() {
        removeRecipes();
        recipeDefinitions.clear();

        for (CustomWeapon weapon : weaponRegistry.getWeapons()) {
            loadWeaponRecipe(weapon);
        }
    }

    public void loadWeaponRecipe(CustomWeapon weapon) {
        removeRecipe(weapon.getId());
        recipeDefinitions.remove(weapon.getId());

        Optional<WeaponRecipeDefinition> definition = parseRecipe(weapon);
        definition.ifPresent(value ->
                recipeDefinitions.put(weapon.getId(), value)
        );

        if (definition.isEmpty()
                || !weaponRegistry.isEnabled(weapon)
                || !isRecipeEnabled(weapon)) {
            return;
        }

        registerRecipe(weapon, definition.get());
    }

    public Optional<WeaponRecipeDefinition> getRecipe(CustomWeapon weapon) {
        if (weapon == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(recipeDefinitions.get(weapon.getId()));
    }

    public boolean isRecipeEnabled(CustomWeapon weapon) {
        JavaPlugin owner = weaponRegistry.getOwner(weapon).orElse(plugin);

        return owner.getConfig().getBoolean(
                recipePath(weapon) + ".enabled",
                true
        );
    }

    private Optional<WeaponRecipeDefinition> parseRecipe(
            CustomWeapon weapon
    ) {
        JavaPlugin owner = weaponRegistry.getOwner(weapon).orElse(plugin);
        ConfigurationSection recipeSection = owner.getConfig()
                .getConfigurationSection(recipePath(weapon));

        if (recipeSection == null) {
            plugin.getLogger().warning(
                    "Missing recipe configuration for weapon '"
                            + weapon.getId() + "' in plugin "
                            + owner.getName() + "."
            );
            return Optional.empty();
        }

        List<String> shape = recipeSection.getStringList("shape");
        if (shape.isEmpty() || shape.size() > 3
                || shape.stream().anyMatch(row ->
                row.isEmpty() || row.length() > 3)
                || shape.stream().anyMatch(row ->
                row.length() != shape.getFirst().length())) {
            plugin.getLogger().warning(
                    "The recipe shape for '" + weapon.getId()
                            + "' must contain one to three rows with one to "
                            + "three characters each. Every row must have "
                            + "the same width."
            );
            return Optional.empty();
        }

        ConfigurationSection ingredients =
                recipeSection.getConfigurationSection("ingredients");
        if (ingredients == null) {
            plugin.getLogger().warning(
                    "Missing recipe ingredients for weapon: "
                            + weapon.getId()
            );
            return Optional.empty();
        }

        Map<Character, Material> parsedIngredients = new LinkedHashMap<>();
        for (String key : ingredients.getKeys(false)) {
            if (key.length() != 1 || key.charAt(0) == ' ') {
                plugin.getLogger().warning(
                        "Ingredient keys must contain exactly one non-space "
                                + "character in recipe for " + weapon.getId()
                );
                return Optional.empty();
            }

            String materialName = ingredients.getString(key);
            Material material = Material.matchMaterial(
                    materialName == null ? "" : materialName
            );

            if (material == null || material.isAir()) {
                plugin.getLogger().warning(
                        "Unknown material '" + materialName
                                + "' in recipe for " + weapon.getId()
                );
                return Optional.empty();
            }

            parsedIngredients.put(key.charAt(0), material);
        }

        for (String row : shape) {
            for (char symbol : row.toCharArray()) {
                if (symbol != ' ' && !parsedIngredients.containsKey(symbol)) {
                    plugin.getLogger().warning(
                            "Recipe for '" + weapon.getId()
                                    + "' uses undefined ingredient key '"
                                    + symbol + "'."
                    );
                    return Optional.empty();
                }
            }
        }

        int amount = Math.max(1, recipeSection.getInt("amount", 1));
        return Optional.of(new WeaponRecipeDefinition(
                shape,
                parsedIngredients,
                amount
        ));
    }

    private void registerRecipe(
            CustomWeapon weapon,
            WeaponRecipeDefinition definition
    ) {
        ItemStack result = weapon.createItem(plugin);
        result.setAmount(Math.min(result.getMaxStackSize(), definition.amount()));

        JavaPlugin owner = weaponRegistry.getOwner(weapon).orElse(plugin);
        NamespacedKey recipeKey = new NamespacedKey(
                owner,
                "weapon_" + weapon.getId().toLowerCase(Locale.ROOT)
        );

        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(definition.shape().toArray(String[]::new));
        definition.ingredients().forEach(recipe::setIngredient);

        if (Bukkit.addRecipe(recipe)) {
            registeredRecipeKeys.put(weapon.getId(), recipeKey);
        } else {
            plugin.getLogger().warning(
                    "Could not register recipe for weapon: "
                            + weapon.getId()
            );
        }
    }

    public void removeRecipe(String weaponId) {
        NamespacedKey key = registeredRecipeKeys.remove(weaponId);
        if (key != null) {
            Bukkit.removeRecipe(key);
        }

        recipeDefinitions.remove(weaponId);
    }

    public void removeRecipes() {
        for (NamespacedKey key : registeredRecipeKeys.values()) {
            Bukkit.removeRecipe(key);
        }

        registeredRecipeKeys.clear();
    }

    private String recipePath(CustomWeapon weapon) {
        return "weapons." + weapon.getId() + ".recipe";
    }
}
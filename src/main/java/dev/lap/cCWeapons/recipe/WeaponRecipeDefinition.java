package dev.lap.cCWeapons.recipe;

import org.bukkit.Material;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable shaped recipe data used by both Bukkit and the weapon guide.
 */
public record WeaponRecipeDefinition(
        List<String> shape,
        Map<Character, Material> ingredients,
        int amount
) {

    public WeaponRecipeDefinition {
        shape = List.copyOf(shape);
        ingredients = Map.copyOf(new LinkedHashMap<>(ingredients));
        amount = Math.max(1, amount);
    }
}
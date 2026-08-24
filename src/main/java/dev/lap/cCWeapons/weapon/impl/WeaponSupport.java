package dev.lap.cCWeapons.weapon.impl;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

final class WeaponSupport {

    private WeaponSupport() {
    }

    static List<Enchantment> findCompatibleEnchantments(
            Material material
    ) {
        ItemStack vanillaItem = new ItemStack(material);
        List<Enchantment> enchantments = new ArrayList<>();

        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            if (enchantment.canEnchantItem(vanillaItem)) {
                enchantments.add(enchantment);
            }
        }

        return List.copyOf(enchantments);
    }

    static List<String> createEnchantmentHints(
            List<Enchantment> enchantments,
            String weaponType
    ) {
        return enchantments.stream()
                .sorted(Comparator.comparing(
                        enchantment -> enchantment.getKey().getKey()
                ))
                .map(enchantment ->
                        formatName(enchantment.getKey().getKey())
                                + " I-"
                                + enchantment.getMaxLevel()
                                + " - Normal "
                                + weaponType
                                + " enchantment."
                )
                .toList();
    }

    private static String formatName(String key) {
        String[] words = key.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    word.substring(0, 1)
                            .toUpperCase(Locale.ROOT)
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }
}
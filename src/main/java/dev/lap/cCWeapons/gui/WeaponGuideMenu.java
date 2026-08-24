package dev.lap.cCWeapons.gui;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import dev.lap.cCWeapons.weapon.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WeaponGuideMenu implements Listener {

    private static final int GUIDE_SIZE = 54;
    private static final int ENCHANTMENT_SIZE = 45;

    private static final int[] GUIDE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int[] ENCHANTMENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private static final int BACK_SLOT = 40;

    private final CCWeapons plugin;
    private final WeaponRegistry weaponRegistry;

    public WeaponGuideMenu(
            CCWeapons plugin,
            WeaponRegistry weaponRegistry
    ) {
        this.plugin = plugin;
        this.weaponRegistry = weaponRegistry;
    }

    public void openGuide(Player player) {
        GuideHolder holder = new GuideHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                GUIDE_SIZE,
                Component.text("Custom Weapons Guide")
        );
        holder.setInventory(inventory);

        List<CustomWeapon> weapons = weaponRegistry.getWeapons()
                .stream()
                .filter(weaponRegistry::isEnabled)
                .sorted(Comparator.comparing(CustomWeapon::getId))
                .toList();

        for (int index = 0;
             index < weapons.size() && index < GUIDE_SLOTS.length;
             index++) {
            CustomWeapon weapon = weapons.get(index);

            ItemStack icon = weapon.createItem(plugin);
            ItemMeta meta = icon.getItemMeta();

            List<Component> lore = meta.lore() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(meta.lore());

            lore.add(Component.empty());
            lore.add(Component.text(
                    "Click to view compatible enchantments.",
                    NamedTextColor.YELLOW
            ));

            meta.lore(lore);
            icon.setItemMeta(meta);
            inventory.setItem(GUIDE_SLOTS[index], icon);
        }

        player.openInventory(inventory);
    }

    public void openEnchantments(
            Player player,
            CustomWeapon weapon
    ) {
        EnchantmentHolder holder = new EnchantmentHolder(weapon.getId());

        Inventory inventory = Bukkit.createInventory(
                holder,
                ENCHANTMENT_SIZE,
                Component.text(weapon.getDisplayName() + " Enchantments")
        );
        holder.setInventory(inventory);

        List<Enchantment> enchantments =
                weapon.getCompatibleEnchantments();

        for (int index = 0;
             index < enchantments.size()
                     && index < ENCHANTMENT_SLOTS.length;
             index++) {
            inventory.setItem(
                    ENCHANTMENT_SLOTS[index],
                    createEnchantmentIcon(enchantments.get(index))
            );
        }

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(
                Component.text("Back", NamedTextColor.YELLOW)
        );
        back.setItemMeta(backMeta);
        inventory.setItem(BACK_SLOT, back);

        player.openInventory(inventory);
    }

    private ItemStack createEnchantmentIcon(
            Enchantment enchantment
    ) {
        ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = icon.getItemMeta();

        String key = enchantment.getKey().getKey();
        String displayName = formatName(key);

        meta.displayName(
                Component.text(displayName, NamedTextColor.LIGHT_PURPLE)
        );

        meta.lore(List.of(
                Component.text(
                        "Maximum level: " + enchantment.getMaxLevel(),
                        NamedTextColor.GRAY
                ),
                Component.text(
                        "Compatible with this weapon.",
                        NamedTextColor.GREEN
                )
        ));

        icon.setItemMeta(meta);
        return icon;
    }

    private String formatName(String key) {
        String[] words = key.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    word.substring(0, 1).toUpperCase(Locale.ROOT)
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getInventory().getHolder() instanceof GuideHolder) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();

            weaponRegistry.getWeapon(clickedItem)
                    .filter(weaponRegistry::isEnabled)
                    .ifPresent(weapon ->
                            openEnchantments(player, weapon)
                    );

            return;
        }

        if (!(event.getInventory().getHolder()
                instanceof EnchantmentHolder)) {
            return;
        }

        event.setCancelled(true);

        if (event.getRawSlot() == BACK_SLOT) {
            openGuide(player);
        }
    }

    private static final class GuideHolder
            implements InventoryHolder {

        private Inventory inventory;

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class EnchantmentHolder
            implements InventoryHolder {

        private final String weaponId;
        private Inventory inventory;

        private EnchantmentHolder(String weaponId) {
            this.weaponId = weaponId;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        @SuppressWarnings("unused")
        private String getWeaponId() {
            return weaponId;
        }
    }
}